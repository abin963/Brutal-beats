package com.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.player.NowPlayingScreen
import com.example.ui.theme.BrutalBeatsTheme
import com.example.ui.viewmodel.MusicViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

class PlayerActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BrutalBeatsTheme {
                val playerState by viewModel.playerState.collectAsStateWithLifecycle()

                NowPlayingScreen(
                    playerState = playerState,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextTrack() },
                    onPrevious = { viewModel.previousTrack() },
                    onSeek = { viewModel.seekTo(it) },
                    onVolumeChange = { viewModel.setVolume(it) },
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleLoop = { viewModel.toggleLoop() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onToggleQueue = { viewModel.toggleQueueVisibility() },
                    onRemoveFromQueue = { viewModel.removeFromQueue(it) },
                    onClearQueue = { viewModel.clearQueue() },
                    onPlayQueueItem = { viewModel.playTrack(it) },
                    onMinimize = { finish() },
                    onStateChanged = { isPlaying ->
                        if (isPlaying != playerState.isPlaying) {
                            viewModel.togglePlayPause()
                        }
                    },
                    onTimeProgress = { cur, dur ->
                        viewModel.onPlayerProgress(cur, dur)
                    },
                    onTrackEnded = {
                        viewModel.onTrackFinished()
                    },
                    onError = {
                        viewModel.showMessage("PLAYBACK ERROR (CODE $it)")
                    }
                )
            }
        }
    }
}
