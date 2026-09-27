package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.data.local.PlaylistEntity
import com.example.data.model.Track
import com.example.ui.components.TrackCard
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonPink

enum class LibrarySubTab {
    FAVORITES,
    PLAYLISTS
}

@Composable
fun LibraryScreen(
    favorites: List<Track>,
    playlists: List<PlaylistEntity>,
    selectedPlaylist: PlaylistEntity?,
    selectedPlaylistTracks: List<Track>,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onOpenPlaylist: (PlaylistEntity) -> Unit,
    onClosePlaylist: () -> Unit,
    onRemoveTrackFromPlaylist: (Long, String) -> Unit,
    onStartRadio: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableStateOf(LibrarySubTab.FAVORITES) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    BackHandler(enabled = selectedPlaylist != null) {
        onClosePlaylist()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        if (selectedPlaylist != null) {
            // Playlist Detail View
            PlaylistDetailView(
                playlist = selectedPlaylist,
                tracks = selectedPlaylistTracks,
                currentPlayingVideoId = currentPlayingVideoId,
                isPlaying = isPlaying,
                onBack = onClosePlaylist,
                onPlayTrack = onPlayTrack,
                onFavoriteToggle = onFavoriteToggle,
                onAddToQueue = onAddToQueue,
                onRemoveTrack = { videoId ->
                    onRemoveTrackFromPlaylist(selectedPlaylist.id, videoId)
                },
                onStartRadio = onStartRadio
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))

            // Sub-Tabs: [Favorites] [Playlists]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                LibrarySubTab.entries.forEach { tab ->
                    val isSelected = subTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonLime else Color.Transparent)
                            .clickable { subTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tab == LibrarySubTab.FAVORITES) "Favorites (${favorites.size})" else "Playlists (${playlists.size})",
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (subTab) {
                LibrarySubTab.FAVORITES -> {
                    FavoritesList(
                        favorites = favorites,
                        currentPlayingVideoId = currentPlayingVideoId,
                        isPlaying = isPlaying,
                        onPlayTrack = onPlayTrack,
                        onFavoriteToggle = onFavoriteToggle,
                        onAddToQueue = onAddToQueue,
                        onStartRadio = onStartRadio
                    )
                }
                LibrarySubTab.PLAYLISTS -> {
                    PlaylistsList(
                        playlists = playlists,
                        onOpenPlaylist = onOpenPlaylist,
                        onCreatePlaylistClick = { showCreateDialog = true },
                        onDeletePlaylist = onDeletePlaylist
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newPlaylistName = ""
            },
            title = {
                Text(
                    text = "New Playlist",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonLime,
                        cursorColor = NeonLime
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                ) {
                    Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newPlaylistName = ""
                    }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun FavoritesList(
    favorites: List<Track>,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onStartRadio: (Track) -> Unit = {}
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Favorites Yet",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap the heart icon on any song to save it here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${favorites.size} SAVED TRACKS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = { onPlayTrack(favorites.first(), favorites) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play all",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            itemsIndexed(favorites) { index, track ->
                TrackCard(
                    track = track,
                    isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                    trackIndex = index + 1,
                    onPlay = { onPlayTrack(track, favorites) },
                    onFavoriteToggle = { onFavoriteToggle(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = {},
                    onStartRadio = { onStartRadio(track) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsList(
    playlists: List<PlaylistEntity>,
    onOpenPlaylist: (PlaylistEntity) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Create Playlist Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onCreatePlaylistClick)
                    .testTag("create_playlist_btn"),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NeonLime),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Playlist",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Create Playlist",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Organize your favorite jams",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Custom Playlists",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(playlists) { playlist ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenPlaylist(playlist) },
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = NeonLime,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = playlist.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to view tracks",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: PlaylistEntity,
    tracks: List<Track>,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onRemoveTrack: (String) -> Unit,
    onStartRadio: (Track) -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${tracks.size} tracks",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                if (tracks.isNotEmpty()) {
                    Button(
                        onClick = { onPlayTrack(tracks.first(), tracks) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play all",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Playlist is Empty\nAdd tracks using the + button on any song",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            itemsIndexed(tracks) { index, track ->
                TrackCard(
                    track = track,
                    isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                    trackIndex = index + 1,
                    onPlay = { onPlayTrack(track, tracks) },
                    onFavoriteToggle = { onFavoriteToggle(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onRemoveTrack(track.videoId) },
                    onStartRadio = { onStartRadio(track) }
                )
            }
        }
    }
}
