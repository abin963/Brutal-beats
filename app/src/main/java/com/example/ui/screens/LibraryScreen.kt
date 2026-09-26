package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlaylistEntity
import com.example.data.model.Track
import com.example.ui.components.*
import com.example.ui.theme.*

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
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableStateOf(LibrarySubTab.FAVORITES) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // If viewing a playlist, BackHandler closes it
    BackHandler(enabled = selectedPlaylist != null) {
        onClosePlaylist()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BrutalOffWhite)
            .padding(14.dp)
    ) {
        // If a playlist is opened, show playlist view
        if (selectedPlaylist != null) {
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
                }
            )
            return
        }

        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOCAL LIBRARY",
                color = BrutalBlack,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-1).sp
            )

            BrutalBadge(
                text = "ROOM DB PERSISTENCE",
                backgroundColor = BrutalYellow,
                textColor = BrutalBlack
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Subtabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.5.dp, BrutalBlack)
                    .background(if (subTab == LibrarySubTab.FAVORITES) BrutalBlack else BrutalWhite)
                    .clickable { subTab = LibrarySubTab.FAVORITES }
                    .padding(vertical = 8.dp)
                    .testTag("subtab_favorites"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FAVORITES (${favorites.size})",
                    color = if (subTab == LibrarySubTab.FAVORITES) BrutalYellow else BrutalBlack,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.5.dp, BrutalBlack)
                    .background(if (subTab == LibrarySubTab.PLAYLISTS) BrutalBlack else BrutalWhite)
                    .clickable { subTab = LibrarySubTab.PLAYLISTS }
                    .padding(vertical = 8.dp)
                    .testTag("subtab_playlists"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PLAYLISTS (${playlists.size})",
                    color = if (subTab == LibrarySubTab.PLAYLISTS) BrutalYellow else BrutalBlack,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (subTab) {
            LibrarySubTab.FAVORITES -> {
                if (favorites.isEmpty()) {
                    BrutalCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BrutalWhite,
                        borderWidth = 3.dp,
                        shadowOffset = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NO FAVORITES LOGGED",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                color = BrutalBlack
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "TAP THE HEART ICON ON ANY TRACK CARD TO STORE IT LOCALLY IN ROOM DB.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        favorites.forEachIndexed { index, track ->
                            TrackCard(
                                track = track,
                                onPlay = { onPlayTrack(track, favorites) },
                                onFavoriteToggle = { onFavoriteToggle(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                isPlaying = currentPlayingVideoId == track.videoId && isPlaying,
                                trackIndex = index + 1
                            )
                        }
                    }
                }
            }

            LibrarySubTab.PLAYLISTS -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Create Playlist Button
                    BrutalButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = BrutalYellow,
                        testTag = "create_playlist_btn"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create",
                            tint = BrutalBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CREATE NEW PLAYLIST",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = BrutalBlack
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (playlists.isEmpty()) {
                        BrutalCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = BrutalWhite,
                            borderWidth = 3.dp,
                            shadowOffset = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "NO PLAYLISTS YET",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.SansSerif,
                                    color = BrutalBlack
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "CREATE CUSTOM INDUSTRIAL MIXES & RECTANGULAR SOUND BOARDS.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            playlists.forEach { playlist ->
                                BrutalCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenPlaylist(playlist) },
                                    backgroundColor = BrutalWhite,
                                    borderWidth = 3.dp,
                                    shadowOffset = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                BrutalBadge(
                                                    text = "PLAYLIST",
                                                    backgroundColor = BrutalBlack,
                                                    textColor = BrutalWhite
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = playlist.name,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.SansSerif,
                                                    color = BrutalBlack
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "TAP TO VIEW TRACKS & PLAY",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF555555)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeletePlaylist(playlist.id) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .border(2.dp, BrutalBlack)
                                                .background(BrutalWhite)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFCC0000),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = BrutalWhite,
            shape = androidx.compose.ui.graphics.RectangleShape,
            modifier = Modifier.border(3.5.dp, BrutalBlack),
            title = {
                Text(
                    text = "NEW BRUTAL PLAYLIST",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = BrutalBlack
                )
            },
            text = {
                Column {
                    Text(
                        text = "ENTER PLAYLIST TITLE:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF444444)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, BrutalBlack)
                            .testTag("playlist_name_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BrutalOffWhite,
                            unfocusedContainerColor = BrutalOffWhite,
                            cursorColor = BrutalBlack,
                            focusedTextColor = BrutalBlack,
                            unfocusedTextColor = BrutalBlack
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newPlaylistName.isNotBlank()) {
                                    onCreatePlaylist(newPlaylistName)
                                    newPlaylistName = ""
                                    showCreateDialog = false
                                }
                            }
                        )
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, BrutalBlack)
                        .background(BrutalYellow)
                        .clickable {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylist(newPlaylistName)
                                newPlaylistName = ""
                                showCreateDialog = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CREATE",
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = BrutalBlack
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .border(2.dp, BrutalBlack)
                        .background(BrutalWhite)
                        .clickable { showCreateDialog = false }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = BrutalBlack
                    )
                }
            }
        )
    }
}

@Composable
fun PlaylistDetailView(
    playlist: PlaylistEntity,
    tracks: List<Track>,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onRemoveTrack: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Back header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, BrutalBlack)
                    .background(BrutalWhite)
                    .testTag("playlist_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BrutalBlack
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                BrutalBadge(
                    text = "CUSTOM PLAYLIST",
                    backgroundColor = BrutalBlack,
                    textColor = BrutalWhite
                )
                Text(
                    text = playlist.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = BrutalBlack
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (tracks.isNotEmpty()) {
            BrutalButton(
                onClick = { onPlayTrack(tracks.first(), tracks) },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalYellow
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play All",
                    tint = BrutalBlack,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PLAY ALL TRACKS (${tracks.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = BrutalBlack
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (tracks.isEmpty()) {
            BrutalCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalWhite,
                borderWidth = 3.dp,
                shadowOffset = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYLIST IS EMPTY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = BrutalBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ADD TRACKS FROM THE DISCOVER OR FAVORITES SECTIONS USING THE '+' ICON.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF666666)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tracks.forEachIndexed { index, track ->
                    TrackCard(
                        track = track,
                        onPlay = { onPlayTrack(track, tracks) },
                        onFavoriteToggle = { onFavoriteToggle(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        isPlaying = currentPlayingVideoId == track.videoId && isPlaying,
                        trackIndex = index + 1
                    )
                }
            }
        }
    }
}
