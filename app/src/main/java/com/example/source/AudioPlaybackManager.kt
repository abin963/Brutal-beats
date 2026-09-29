package com.example.source

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.Track
import com.example.service.MusicPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controller bridging MusicViewModel and MusicPlayerService, ensuring ONE authoritative playback instance.
 */
class AudioPlaybackManager(private val context: Context) {

    private val appContext = context.applicationContext

    private val _isDirectPlaying = MutableStateFlow(false)
    val isDirectPlaying: StateFlow<Boolean> = _isDirectPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private fun ensureServiceStarted(): MusicPlayerService? {
        var service = MusicPlayerService.getInstance()
        if (service == null) {
            val intent = Intent(appContext, MusicPlayerService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(appContext, intent)
                } else {
                    appContext.startService(intent)
                }
            } catch (_: Exception) {}
            service = MusicPlayerService.getInstance()
        }
        return service
    }

    fun playDirectStream(
        track: Track,
        url: String,
        onCompletion: () -> Unit,
        onError: ((Int) -> Unit)? = null,
        onTrackChanged: ((Track) -> Unit)? = null
    ) {
        val service = ensureServiceStarted()
        MusicPlayerService.onCompletionCallback = onCompletion
        MusicPlayerService.onErrorCallback = onError
        MusicPlayerService.onTrackChangedCallback = onTrackChanged

        service?.playDirectStream(track, url)
    }

    fun preloadNextStream(track: Track, url: String) {
        MusicPlayerService.getInstance()?.preloadNextStream(track, url)
    }

    fun pause() {
        MusicPlayerService.getInstance()?.pause()
    }

    fun resume() {
        MusicPlayerService.getInstance()?.resume()
    }

    fun seekTo(seconds: Float) {
        MusicPlayerService.getInstance()?.seekTo(seconds)
    }

    fun setVolume(volume0to100: Float) {
        MusicPlayerService.getInstance()?.setVolume(volume0to100)
    }

    fun stop() {
        MusicPlayerService.getInstance()?.stopPlayback()
    }

    fun syncForegroundStreamState(track: Track, isPlaying: Boolean, isBuffering: Boolean, currentSec: Float, durationSec: Float) {
        ensureServiceStarted()
        MusicPlayerService.getInstance()?.syncForegroundStreamState(track, isPlaying, isBuffering, currentSec, durationSec)
    }
}
