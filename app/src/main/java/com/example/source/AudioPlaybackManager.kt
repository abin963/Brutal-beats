package com.example.source

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPlaybackManager(context: Context) {

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isDirectPlaying = MutableStateFlow(false)
    val isDirectPlaying: StateFlow<Boolean> = _isDirectPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private var onCompletionCallback: (() -> Unit)? = null
    private var isUpdatingProgress = false

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    val posSec = mp.currentPosition / 1000f
                    val durSec = mp.duration / 1000f
                    _currentPosition.value = posSec
                    if (durSec > 0) _duration.value = durSec
                }
            }
            if (isUpdatingProgress) {
                mainHandler.postDelayed(this, 500)
            }
        }
    }

    fun playDirectStream(url: String, onCompletion: () -> Unit) {
        stop()
        onCompletionCallback = onCompletion
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
                    player.start()
                    _isDirectPlaying.value = true
                    _duration.value = player.duration / 1000f
                    startProgressLoop()
                }
                setOnCompletionListener {
                    _isDirectPlaying.value = false
                    stopProgressLoop()
                    onCompletionCallback?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    _isDirectPlaying.value = false
                    stopProgressLoop()
                    false
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            _isDirectPlaying.value = false
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isDirectPlaying.value = false
            }
        }
        stopProgressLoop()
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isDirectPlaying.value = true
            startProgressLoop()
        }
    }

    fun seekTo(seconds: Float) {
        mediaPlayer?.let {
            val ms = (seconds * 1000).toInt().coerceIn(0, it.duration)
            it.seekTo(ms)
            _currentPosition.value = seconds
        }
    }

    fun setVolume(volume0to100: Float) {
        val scalar = (volume0to100 / 100f).coerceIn(0f, 1f)
        mediaPlayer?.setVolume(scalar, scalar)
    }

    fun stop() {
        stopProgressLoop()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
        _isDirectPlaying.value = false
        _currentPosition.value = 0f
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
}
