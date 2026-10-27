package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Unified Media3 MediaSessionService & ExoPlayer architecture adapted from Metrolist.
 * Provides authoritative background playback, system MediaSession, lock screen controls,
 * automatic audio focus management, headphone disconnect handling, and gapless playback.
 */
class MusicPlayerService : MediaSessionService() {

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
        const val EXTRA_SEEK_MS = "extra_seek_ms"

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

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private var currentTrack: Track? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0f)
    val currentPositionSec: StateFlow<Float> = _currentPositionSec.asStateFlow()

    private val _durationSec = MutableStateFlow(0f)
    val durationSec: StateFlow<Float> = _durationSec.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var isUpdatingProgress = false
    private val progressRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                try {
                    val posMs = player.currentPosition
                    val durMs = player.duration
                    _currentPositionSec.value = posMs / 1000f
                    if (durMs > 0) {
                        _durationSec.value = durMs / 1000f
                    }
                } catch (_: Exception) {}
            }
            if (isUpdatingProgress) {
                mainHandler.postDelayed(this, 300)
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return binder
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _isBuffering.value = true
                    }
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        val dur = player.duration
                        if (dur > 0) {
                            _durationSec.value = dur / 1000f
                        }
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        onCompletionCallback?.invoke()
                    }
                    Player.STATE_IDLE -> {
                        _isBuffering.value = false
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressLoop()
                } else {
                    stopProgressLoop()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _isPlaying.value = false
                _isBuffering.value = false
                stopProgressLoop()
                onErrorCallback?.invoke(error.errorCode)
            }

            override fun onRepeatModeChanged(mode: Int) {
                _repeatMode.value = mode
            }

            override fun onShuffleModeEnabledChanged(shuffleEnabled: Boolean) {
                _isShuffleEnabled.value = shuffleEnabled
            }
        })

        exoPlayer = player

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {
                // Handled natively by Media3
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    fun playDirectStream(track: Track, url: String) {
        currentTrack = track
        _isBuffering.value = true
        _currentPositionSec.value = 0f
        _durationSec.value = 0f

        val player = exoPlayer ?: return

        try {
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle("NYX Music")
                .setArtworkUri(Uri.parse(track.thumbnailUrl))
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(track.videoId)
                .setUri(url)
                .setMediaMetadata(mediaMetadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _isPlaying.value = true
            startProgressLoop()
        } catch (_: Exception) {
            _isPlaying.value = false
            _isBuffering.value = false
            onErrorCallback?.invoke(-1)
        }
    }

    fun pause() {
        exoPlayer?.pause()
        _isPlaying.value = false
        stopProgressLoop()
    }

    fun resume() {
        exoPlayer?.play()
        _isPlaying.value = true
        startProgressLoop()
    }

    fun seekTo(seconds: Float) {
        val ms = (seconds * 1000).toLong()
        exoPlayer?.seekTo(ms)
        _currentPositionSec.value = seconds
    }

    fun seekBy(deltaSeconds: Float) {
        val cur = _currentPositionSec.value
        val dur = _durationSec.value
        val maxBound = if (dur > 0f) dur else Float.MAX_VALUE
        val target = (cur + deltaSeconds).coerceIn(0f, maxBound)
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        _playbackSpeed.value = clamped
        exoPlayer?.playbackParameters = androidx.media3.common.PlaybackParameters(clamped)
    }

    fun setVolume(vol0to100: Float) {
        val scalar = (vol0to100 / 100f).coerceIn(0f, 1f)
        exoPlayer?.volume = scalar
    }

    fun setRepeatMode(mode: Int) {
        exoPlayer?.repeatMode = mode
        _repeatMode.value = mode
    }

    fun toggleRepeatMode(): Int {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        setRepeatMode(nextMode)
        return nextMode
    }

    fun toggleShuffle(): Boolean {
        val nextShuffle = !_isShuffleEnabled.value
        exoPlayer?.shuffleModeEnabled = nextShuffle
        _isShuffleEnabled.value = nextShuffle
        return nextShuffle
    }

    fun stopPlayback() {
        stopProgressLoop()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _isPlaying.value = false
        _currentPositionSec.value = 0f
    }

    fun syncForegroundStreamState(track: Track, isPlaying: Boolean, isBuffering: Boolean, currentSec: Float, durationSec: Float) {
        currentTrack = track
        _isPlaying.value = isPlaying
        _isBuffering.value = isBuffering
        _currentPositionSec.value = currentSec
        if (durationSec > 0) _durationSec.value = durationSec
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

    override fun onDestroy() {
        super.onDestroy()
        stopProgressLoop()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        if (instance == this) instance = null
    }
}
