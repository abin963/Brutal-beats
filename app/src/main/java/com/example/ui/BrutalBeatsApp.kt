package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.*
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

    val mainScrollState = rememberScrollState()

    // Handle back press when expanded
    BackHandler(enabled = playerState.isPlayerExpanded) {
        viewModel.togglePlayerExpanded()
    }

    // Handle back press when searching or in sub-screen
    BackHandler(enabled = !playerState.isPlayerExpanded && (uiState.searchQuery.isNotBlank() || uiState.activeTab != MainTab.DISCOVER)) {
        if (uiState.searchQuery.isNotBlank()) {
            viewModel.clearSearch()
        } else if (uiState.activeTab != MainTab.DISCOVER) {
            viewModel.selectTab(MainTab.DISCOVER)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrutalOffWhite)
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
                    viewModel.showMessage("YOUTUBE PLAYER ERROR (CODE $it)")
                }
            )
        } else {
            // Main App Layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = BrutalOffWhite,
                topBar = {
                    HeaderSection(
                        activeTab = uiState.activeTab,
                        onTabSelected = { viewModel.selectTab(it) },
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
                    Column(modifier = Modifier.navigationBarsPadding()) {
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
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(mainScrollState)
                ) {
                    when (uiState.activeTab) {
                        MainTab.DISCOVER -> {
                            DiscoverScreen(
                                tracks = uiState.genreTracks,
                                isLoadingTracks = uiState.isLoadingGenre,
                                searchResults = uiState.searchResults,
                                isSearching = uiState.isSearching,
                                searchQuery = uiState.searchQuery,
                                selectedGenre = uiState.selectedGenre,
                                directUrlInput = uiState.directUrlInput,
                                isFetchingDirectLink = uiState.isFetchingDirectLink,
                                currentPlayingVideoId = playerState.currentTrack?.videoId,
                                isPlaying = playerState.isPlaying,
                                onSelectGenre = { viewModel.selectGenre(it) },
                                onDirectUrlChange = { viewModel.updateDirectUrlInput(it) },
                                onPlayDirectUrl = { viewModel.playDirectLink(it) },
                                onPlayTrack = { track, queue ->
                                    viewModel.playTrack(track, queue)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylist = { trackToAddToPlaylist = it },
                                onClearSearch = { viewModel.clearSearch() }
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
                                onPlayTrack = { track, queue ->
                                    viewModel.playTrack(track, queue)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onCreatePlaylist = { viewModel.createPlaylist(it) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onOpenPlaylist = { viewModel.openPlaylist(it) },
                                onClosePlaylist = { viewModel.selectTab(MainTab.LIBRARY) },
                                onRemoveTrackFromPlaylist = { pid, vid ->
                                    viewModel.removeTrackFromPlaylist(pid, vid)
                                }
                            )
                        }

                        MainTab.HISTORY -> {
                            HistoryScreen(
                                history = history,
                                currentPlayingVideoId = playerState.currentTrack?.videoId,
                                isPlaying = playerState.isPlaying,
                                onPlayTrack = { track, queue ->
                                    viewModel.playTrack(track, queue)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylist = { trackToAddToPlaylist = it },
                                onClearHistory = { viewModel.clearHistory() }
                            )
                        }
                    }
                }
            }
        }

        // Action Status Toast / Toast notification banner
        uiState.statusMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .background(BrutalYellow)
                    .border(2.5.dp, BrutalBlack)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("status_toast")
            ) {
                Text(
                    text = "► $msg",
                    color = BrutalBlack,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Add to Playlist Modal Dialog
        trackToAddToPlaylist?.let { track ->
            AlertDialog(
                onDismissRequest = {
                    trackToAddToPlaylist = null
                    showNewPlaylistInput = false
                },
                containerColor = BrutalWhite,
                shape = androidx.compose.ui.graphics.RectangleShape,
                modifier = Modifier.border(3.5.dp, BrutalBlack),
                title = {
                    Text(
                        text = "ADD TO PLAYLIST",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = BrutalBlack
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "TRACK: ${track.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            color = BrutalBlack
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (playlists.isEmpty() && !showNewPlaylistInput) {
                            Text(
                                text = "NO PLAYLISTS CREATED YET.",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                        } else {
                            playlists.forEach { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addTrackToPlaylist(pl.id, track)
                                            trackToAddToPlaylist = null
                                        }
                                        .border(1.5.dp, BrutalBlack)
                                        .background(BrutalOffWhite)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pl.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = BrutalBlack
                                    )
                                    Text(
                                        text = "[ADD]",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = BrutalYellow
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (showNewPlaylistInput) {
                            TextField(
                                value = newPlaylistTitle,
                                onValueChange = { newPlaylistTitle = it },
                                placeholder = {
                                    Text(
                                        "PLAYLIST NAME...",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(2.dp, BrutalBlack),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = BrutalOffWhite,
                                    unfocusedContainerColor = BrutalOffWhite,
                                    cursorColor = BrutalBlack
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            BrutalButton(
                                onClick = {
                                    if (newPlaylistTitle.isNotBlank()) {
                                        viewModel.createPlaylist(newPlaylistTitle)
                                        newPlaylistTitle = ""
                                        showNewPlaylistInput = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "SAVE & DONE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNewPlaylistInput = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New",
                                    tint = BrutalBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ CREATE NEW PLAYLIST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = BrutalBlack
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Box(
                        modifier = Modifier
                            .border(2.dp, BrutalBlack)
                            .background(BrutalBlack)
                            .clickable {
                                trackToAddToPlaylist = null
                                showNewPlaylistInput = false
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "CLOSE",
                            color = BrutalWhite,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            )
        }
    }
}
