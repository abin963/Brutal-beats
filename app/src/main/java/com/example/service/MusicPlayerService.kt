package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import coil.Coil
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.data.model.Track
import com.example.ui.components.ThumbnailUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground Service that handles authoritative background audio playback,
 * gapless track transitions, MediaSession, lock screen/notification media controls,
 * audio focus, and headphone disconnect.
 */
class MusicPlayerService : Service(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val CHANNEL_ID = "nyx_music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.service.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_TOGGLE_PLAY = "com.example.service.ACTION_TOGGLE_PLAY"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_NEXT = "com.example.service.ACTION_NEXT"
        const val ACTION_PREV = "com.example.service.ACTION_PREV"
        const val ACTION_SEEK_TO = "com.example.service.ACTION_SEEK_TO"
        const val ACTION_PLAY_DIRECT = "com.example.service.ACTION_PLAY_DIRECT"
        const val EXTRA_SEEK_MS = "extra_seek_ms"
        const val EXTRA_STREAM_URL = "extra_stream_url"

        // Global singleton accessor for single-source-of-truth player state & controls
        @Volatile
        private var instance: MusicPlayerService? = null
        fun getInstance(): MusicPlayerService? = instance

        var onNextCallback: (() -> Unit)? = null
        var onPreviousCallback: (() -> Unit)? = null
        var onPlayPauseCallback: (() -> Unit)? = null
        var onSeekCallback: ((Float) -> Unit)? = null
        var onCompletionCallback: (() -> Unit)? = null
        var onErrorCallback: ((Int) -> Unit)? = null
        var onTrackChangedCallback: ((Track) -> Unit)? = null
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null
    private var isPlayerPrepared = false
    private var nextMediaPlayer: MediaPlayer? = null
    private var nextTrack: Track? = null
    private var isGaplessAttached = false

    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var currentTrack: Track? = null
    private var currentArtworkBitmap: Bitmap? = null
    private var artworkJob: Job? = null

    private var isPausedByTransientLoss = false
    private var userInitiatedPause = false
    private var isNoisyReceiverRegistered = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0f)
    val currentPositionSec: StateFlow<Float> = _currentPositionSec.asStateFlow()

    private val _durationSec = MutableStateFlow(0f)
    val durationSec: StateFlow<Float> = _durationSec.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var isUpdatingProgress = false
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        val posSec = mp.currentPosition / 1000f
                        val durSec = mp.duration / 1000f
                        _currentPositionSec.value = posSec
                        if (durSec > 0) _durationSec.value = durSec
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, mp.currentPosition.toLong(), 1.0f)
                    }
                } catch (_: Exception) {}
            }
            if (isUpdatingProgress) {
                mainHandler.postDelayed(this, 300)
            }
        }
    }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Pause automatically when headphones / Bluetooth disconnects
                pause()
                onPlayPauseCallback?.invoke()
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        createNotificationChannel()
        initMediaSession()

        // Immediate initial notification to satisfy Android Foreground Service contract
        try {
            val initialNotif = buildNotification(currentTrack, isPlaying = false, isBuffering = false)
            startForeground(NOTIFICATION_ID, initialNotif)
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        // Ensure foreground status is active immediately to prevent OS timeout exceptions
        try {
            val notif = buildNotification(currentTrack, _isPlaying.value, _isBuffering.value)
            startForeground(NOTIFICATION_ID, notif)
        } catch (_: Exception) {}

        when (intent?.action) {
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_TOGGLE_PLAY -> {
                if (_isPlaying.value) pause() else resume()
                onPlayPauseCallback?.invoke()
            }
            ACTION_STOP -> stopPlayback()
            ACTION_NEXT -> onNextCallback?.invoke()
            ACTION_PREV -> onPreviousCallback?.invoke()
            ACTION_SEEK_TO -> {
                val ms = intent.getLongExtra(EXTRA_SEEK_MS, 0L)
                seekTo(ms / 1000f)
                onSeekCallback?.invoke(ms / 1000f)
            }
        }
        return START_NOT_STICKY
    }

    private fun initMediaSession() {
        val mediaButtonReceiverComponent = android.content.ComponentName(this, MediaButtonReceiver::class.java)
        mediaSession = MediaSessionCompat(this, "NyxMediaSession", mediaButtonReceiverComponent, null).apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resume()
                    onPlayPauseCallback?.invoke()
                }

                override fun onPause() {
                    pause()
                    onPlayPauseCallback?.invoke()
                }

                override fun onSkipToNext() {
                    onNextCallback?.invoke()
                }

                override fun onSkipToPrevious() {
                    onPreviousCallback?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos / 1000f)
                    onSeekCallback?.invoke(pos / 1000f)
                }

                override fun onStop() {
                    stopPlayback()
                }
            })
            isActive = true
        }
    }

    fun playDirectStream(track: Track, url: String) {
        currentTrack = track
        userInitiatedPause = false
        isPlayerPrepared = false
        _isBuffering.value = true
        _durationSec.value = 0f
        _currentPositionSec.value = 0f

        clearNextPlayer()
        stopCurrentPlayer()
        requestAudioFocus()
        registerNoisyReceiver()

        updateMediaMetadata(track, null)
        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING, 0L, 0.0f)
        try {
            startForeground(NOTIFICATION_ID, buildNotification(track, isPlaying = true, isBuffering = true))
        } catch (_: Exception) {}
        loadArtworkBitmap(track)

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { player ->
                    try {
                        isPlayerPrepared = true
                        player.start()
                        _isPlaying.value = true
                        _isBuffering.value = false
                        _durationSec.value = player.duration / 1000f
                        startProgressLoop()
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, player.currentPosition.toLong(), 1.0f)
                        updateNotification()

                        // Check if we already have next player prepared to attach gaplessly
                        attachGaplessNextPlayerIfReady()
                    } catch (e: Exception) {
                        isPlayerPrepared = false
                        onErrorCallback?.invoke(-1)
                    }
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, _ ->
                    isPlayerPrepared = false
                    _isPlaying.value = false
                    _isBuffering.value = false
                    stopProgressLoop()
                    updatePlaybackState(PlaybackStateCompat.STATE_ERROR, 0L, 0.0f)
                    updateNotification()
                    onErrorCallback?.invoke(what)
                    true // Return true to signal error was handled and prevent erroneous completion callback cascade!
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            isPlayerPrepared = false
            _isPlaying.value = false
            _isBuffering.value = false
            onErrorCallback?.invoke(-1)
        }
    }

    /**
     * Preload next track for seamless zero-gap transitions via Android MediaPlayer.setNextMediaPlayer
     */
    fun preloadNextStream(track: Track, url: String) {
        if (url.isBlank()) return
        clearNextPlayer()
        nextTrack = track
        serviceScope.launch(Dispatchers.IO) {
            try {
                val nextMp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(url)
                    setOnPreparedListener {
                        isGaplessAttached = false
                        mainHandler.post {
                            attachGaplessNextPlayerIfReady()
                        }
                    }
                    setOnCompletionListener {
                        handleTrackCompletion()
                    }
                    setOnErrorListener { _, _, _ ->
                        clearNextPlayer()
                        true
                    }
                    prepareAsync()
                }
                nextMediaPlayer = nextMp
            } catch (_: Exception) {
                clearNextPlayer()
            }
        }
    }

    private fun attachGaplessNextPlayerIfReady() {
        val current = mediaPlayer ?: return
        val next = nextMediaPlayer ?: return
        if (!isGaplessAttached && _isPlaying.value) {
            try {
                current.setNextMediaPlayer(next)
                isGaplessAttached = true
            } catch (_: Exception) {}
        }
    }

    private fun handleTrackCompletion() {
        val upcoming = nextTrack
        val nextMp = nextMediaPlayer

        if (upcoming != null && nextMp != null && isGaplessAttached) {
            // Seamless gapless handoff
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {}

            mediaPlayer = nextMp
            isPlayerPrepared = true
            currentTrack = upcoming
            nextMediaPlayer = null
            nextTrack = null
            isGaplessAttached = false

            _isPlaying.value = true
            _isBuffering.value = false
            _currentPositionSec.value = 0f
            _durationSec.value = (nextMp.duration / 1000f).coerceAtLeast(0f)

            updateMediaMetadata(upcoming, null)
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, 0L, 1.0f)
            updateNotification()
            loadArtworkBitmap(upcoming)
            startProgressLoop()

            onTrackChangedCallback?.invoke(upcoming)
            onCompletionCallback?.invoke()
        } else {
            isPlayerPrepared = false
            _isPlaying.value = false
            _isBuffering.value = false
            stopProgressLoop()
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED, 0L, 0.0f)
            updateNotification()
            onCompletionCallback?.invoke()
        }
    }

    private fun clearNextPlayer() {
        try {
            mediaPlayer?.setNextMediaPlayer(null)
        } catch (_: Exception) {}
        try {
            nextMediaPlayer?.reset()
            nextMediaPlayer?.release()
        } catch (_: Exception) {}
        nextMediaPlayer = null
        nextTrack = null
        isGaplessAttached = false
    }

    fun syncForegroundStreamState(track: Track, isPlaying: Boolean, isBuffering: Boolean, currentSec: Float, durationSec: Float) {
        currentTrack = track
        _isPlaying.value = isPlaying
        _isBuffering.value = isBuffering
        _currentPositionSec.value = currentSec
        if (durationSec > 0) _durationSec.value = durationSec

        val state = when {
            isBuffering -> PlaybackStateCompat.STATE_BUFFERING
            isPlaying -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        val speed = if (isPlaying && !isBuffering) 1.0f else 0.0f
        updatePlaybackState(state, (currentSec * 1000).toLong(), speed)
        updateMediaMetadata(track, currentArtworkBitmap)
        updateNotification()
        if (currentArtworkBitmap == null) {
            loadArtworkBitmap(track)
        }
    }

    fun pause() {
        userInitiatedPause = true
        if (isPlayerPrepared) {
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        it.pause()
                    }
                } catch (_: Exception) {}
            }
        }
        _isPlaying.value = false
        stopProgressLoop()
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED, (_currentPositionSec.value * 1000).toLong(), 0.0f)
        updateNotification()
    }

    fun resume() {
        userInitiatedPause = false
        requestAudioFocus()
        registerNoisyReceiver()
        if (isPlayerPrepared) {
            mediaPlayer?.let {
                try {
                    if (!it.isPlaying) {
                        it.start()
                        startProgressLoop()
                    }
                } catch (_: Exception) {}
            }
        }
        _isPlaying.value = true
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, (_currentPositionSec.value * 1000).toLong(), 1.0f)
        updateNotification()
    }

    fun seekTo(seconds: Float) {
        if (isPlayerPrepared) {
            mediaPlayer?.let {
                try {
                    val ms = (seconds * 1000).toInt().coerceIn(0, it.duration)
                    it.seekTo(ms)
                } catch (_: Exception) {}
            }
        }
        _currentPositionSec.value = seconds
        val state = if (_isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val speed = if (_isPlaying.value) 1.0f else 0.0f
        updatePlaybackState(state, (seconds * 1000).toLong(), speed)
    }

    fun setVolume(vol0to100: Float) {
        val scalar = (vol0to100 / 100f).coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(scalar, scalar)
            nextMediaPlayer?.setVolume(scalar, scalar)
        } catch (_: Exception) {}
    }

    fun stopPlayback() {
        isPlayerPrepared = false
        stopProgressLoop()
        clearNextPlayer()
        stopCurrentPlayer()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        _isPlaying.value = false
        _currentPositionSec.value = 0f
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L, 0.0f)
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun startProgressLoop() {
        isUpdatingProgress = true
        mainHandler.removeCallbacks(progressRunnable)
        mainHandler.post(progressRunnable)
    }

    private fun stopProgressLoop() {
        isUpdatingProgress = false
        mainHandler.removeCallbacks(progressRunnable)
    }

    private fun stopCurrentPlayer() {
        isPlayerPrepared = false
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
    }

    private fun loadArtworkBitmap(track: Track) {
        artworkJob?.cancel()
        artworkJob = serviceScope.launch {
            try {
                val url = ThumbnailUtils.resolveOptimizedUrl(track.thumbnailUrl, track.videoId)
                if (url.isNotBlank()) {
                    val request = ImageRequest.Builder(this@MusicPlayerService)
                        .data(url)
                        .allowHardware(false)
                        .build()
                    val result = Coil.imageLoader(this@MusicPlayerService).execute(request)
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        currentArtworkBitmap = drawable.bitmap
                        updateMediaMetadata(track, drawable.bitmap)
                        updateNotification()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateMediaMetadata(track: Track, bitmap: Bitmap?) {
        val durMs = if (_durationSec.value > 0) (_durationSec.value * 1000).toLong() else -1L
        val metaBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "NYX Music")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durMs)

        if (bitmap != null) {
            metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
        }
        mediaSession?.setMetadata(metaBuilder.build())
    }

    private fun updatePlaybackState(state: Int, positionMs: Long, speed: Float) {
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, positionMs, speed)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun buildNotification(track: Track?, isPlaying: Boolean, isBuffering: Boolean): Notification {
        val title = track?.title ?: "NYX Music Player"
        val artist = track?.artist ?: "Playing high quality sound"

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Actions: Prev, Play/Pause, Next
        val prevIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(
            this, 10, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_TOGGLE_PLAY }
        val togglePendingIntent = PendingIntent.getService(
            this, 11, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, MusicPlayerService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(
            this, 12, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("NYX")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(currentArtworkBitmap)
            .setContentIntent(contentPendingIntent)
            .setStyle(mediaStyle)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                togglePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)

        return builder.build()
    }

    private fun updateNotification() {
        val track = currentTrack ?: return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            manager?.notify(NOTIFICATION_ID, buildNotification(track, _isPlaying.value, _isBuffering.value))
        } catch (_: Exception) {}
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build()
            }
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss: pause playback
                pause()
                onPlayPauseCallback?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Transient loss (phone call): pause playback
                if (_isPlaying.value) {
                    isPausedByTransientLoss = true
                    pause()
                    onPlayPauseCallback?.invoke()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Duck volume down to 20%
                setVolume(20f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Restore volume and resume if paused by transient loss
                setVolume(100f)
                if (isPausedByTransientLoss && !userInitiatedPause) {
                    isPausedByTransientLoss = false
                    resume()
                    onPlayPauseCallback?.invoke()
                }
            }
        }
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            try {
                registerReceiver(becomingNoisyReceiver, filter)
                isNoisyReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {}
            isNoisyReceiverRegistered = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NYX Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background audio playback and system media controls for NYX"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressLoop()
        clearNextPlayer()
        stopCurrentPlayer()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        mediaSession?.release()
        mediaSession = null
        if (instance == this) instance = null
    }
}
