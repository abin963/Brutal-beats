package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Track
import com.example.ui.components.*
import com.example.ui.player.NowPlayingBottomBar
import com.example.ui.player.NowPlayingScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.NeonLime
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun BrutalBeatsApp(
    viewModel: MusicViewModel = viewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    var showNewPlaylistInput by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }

    // Handle back press when player is expanded
    BackHandler(enabled = playerState.isPlayerExpanded) {
        viewModel.togglePlayerExpanded()
    }

    // Handle back press when searching or in sub-screen
    BackHandler(enabled = !playerState.isPlayerExpanded && (uiState.searchQuery.isNotBlank() || uiState.activeTab != MainTab.HOME)) {
        if (uiState.searchQuery.isNotBlank()) {
            viewModel.clearSearch()
        } else if (uiState.activeTab != MainTab.HOME) {
            viewModel.selectTab(MainTab.HOME)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (playerState.isPlayerExpanded) {
            // Fullscreen Now Playing screen
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
                onMinimize = { viewModel.togglePlayerExpanded() },
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
        } else {
            // Main App Layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    HeaderSection(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onSearchSubmit = { viewModel.performSearch(it) },
                        onClearSearch = { viewModel.clearSearch() },
                        searchSuggestions = uiState.searchSuggestions,
                        isSearching = uiState.isSearching,
                        preferredSourceMode = uiState.preferredSourceMode,
                        onSourceModeChanged = { viewModel.setPreferredSource(it) },
                        modifier = Modifier.statusBarsPadding()
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // Persistent Mini-Player (visible when a track is loaded)
                        NowPlayingBottomBar(
                            currentTrack = playerState.currentTrack,
                            isPlaying = playerState.isPlaying,
                            onPlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.nextTrack() },
                            onPrevious = { viewModel.previousTrack() },
                            onExpand = { viewModel.togglePlayerExpanded() },
                            onFavoriteToggle = {
                                playerState.currentTrack?.let { viewModel.toggleFavorite(it) }
                            },
                            progressFraction = if (playerState.totalDurationSec > 0f) {
                                playerState.currentPositionSec / playerState.totalDurationSec
                            } else 0f
                        )

                        // Bottom Navigation: HOME / EXPLORE / LIBRARY / HISTORY
                        BrutalBottomNav(
                            activeTab = uiState.activeTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (uiState.activeTab) {
                        MainTab.HOME -> {
                            HomeScreen(
                                tracks = uiState.genreTracks,
                                isLoadingTracks = uiState.isLoadingGenre,
                                recentTracks = history,
                                favoriteTracks = favorites,
                                searchResults = uiState.searchResults,
                                isSearching = uiState.isSearching,
                                searchQuery = uiState.searchQuery,
                                currentPlayingVideoId = playerState.currentTrack?.videoId,
                                isPlaying = playerState.isPlaying,
                                onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylist = { trackToAddToPlaylist = it },
                                onClearSearch = { viewModel.clearSearch() }
                            )
                        }

                        MainTab.EXPLORE -> {
                            ExploreScreen(
                                tracks = uiState.genreTracks,
                                isLoadingTracks = uiState.isLoadingGenre,
                                selectedGenre = uiState.selectedGenre,
                                directUrlInput = uiState.directUrlInput,
                                isFetchingDirectLink = uiState.isFetchingDirectLink,
                                currentPlayingVideoId = playerState.currentTrack?.videoId,
                                isPlaying = playerState.isPlaying,
                                onSelectGenre = { viewModel.selectGenre(it) },
                                onDirectUrlChange = { viewModel.updateDirectUrlInput(it) },
                                onPlayDirectUrl = { viewModel.playDirectLink(it) },
                                onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylist = { trackToAddToPlaylist = it }
                            )
                        }

                        MainTab.LIBRARY -> {
                            LibraryScreen(
                                favorites = favorites,
                                playlists = playlists,
                                selectedPlaylist = uiState.selectedPlaylist,
                                selectedPlaylistTracks = uiState.selectedPlaylistTracks,
                                currentPlayingVideoId = playerState.currentTrack?.videoId,
                                isPlaying = playerState.isPlaying,
                                onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onCreatePlaylist = { viewModel.createPlaylist(it) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onOpenPlaylist = { viewModel.openPlaylist(it) },
                                onClosePlaylist = { viewModel.selectTab(MainTab.LIBRARY) },
                                onRemoveTrackFromPlaylist = { pId, vId -> viewModel.removeTrackFromPlaylist(pId, vId) }
                            )
                        }

                        MainTab.HISTORY -> {
                            HistoryScreen(
                                history = history,
                                currentPlayingVideoId = playerState.currentTrack?.videoId,
                                isPlaying = playerState.isPlaying,
                                onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylist = { trackToAddToPlaylist = it },
                                onClearHistory = { viewModel.clearHistory() }
                            )
                        }
                    }

                    // Floating Toast / Status Message Banner
                    uiState.statusMessage?.let { msg ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = NeonLime,
                            tonalElevation = 6.dp
                        ) {
                            Text(
                                text = msg,
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Add to Playlist Dialog
        trackToAddToPlaylist?.let { track ->
            AlertDialog(
                onDismissRequest = {
                    trackToAddToPlaylist = null
                    showNewPlaylistInput = false
                },
                title = {
                    Text(
                        text = "Add to Playlist",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = track.title,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        if (playlists.isEmpty()) {
                            Text(
                                text = "No playlists created yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        } else {
                            playlists.forEach { playlist ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.addTrackToPlaylist(playlist.id, track)
                                            trackToAddToPlaylist = null
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = playlist.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        if (showNewPlaylistInput) {
                            OutlinedTextField(
                                value = newPlaylistTitle,
                                onValueChange = { newPlaylistTitle = it },
                                label = { Text("New Playlist Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (newPlaylistTitle.isNotBlank()) {
                                        viewModel.createPlaylist(newPlaylistTitle.trim())
                                        newPlaylistTitle = ""
                                        showNewPlaylistInput = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonLime),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Create & Add", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(
                                onClick = { showNewPlaylistInput = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = NeonLime)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create New Playlist", color = NeonLime, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { trackToAddToPlaylist = null }) {
                        Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
