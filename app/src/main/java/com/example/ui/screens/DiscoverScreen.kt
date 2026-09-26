package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.data.remote.GenrePresets
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DiscoverScreen(
    tracks: List<Track>,
    isLoadingTracks: Boolean,
    searchResults: List<Track>,
    isSearching: Boolean,
    searchQuery: String,
    selectedGenre: String,
    directUrlInput: String,
    isFetchingDirectLink: Boolean,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onSelectGenre: (String) -> Unit,
    onDirectUrlChange: (String) -> Unit,
    onPlayDirectUrl: (String) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BrutalOffWhite)
    ) {
        // Direct Link / Video ID Player Card
        BrutalCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("direct_link_card"),
            backgroundColor = BrutalWhite,
            borderColor = BrutalBlack,
            shadowColor = BrutalBlack,
            borderWidth = 3.dp,
            shadowOffset = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DIRECT YOUTUBE STREAM",
                        color = BrutalBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    BrutalBadge(
                        text = "ANY URL / ID",
                        backgroundColor = BrutalYellow,
                        textColor = BrutalBlack
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, BrutalBlack)
                        .background(BrutalOffWhite),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = directUrlInput,
                        onValueChange = onDirectUrlChange,
                        placeholder = {
                            Text(
                                text = "PASTE HTTPS://YOUTU.BE/... OR 11-CHAR ID",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF777777)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("direct_url_input"),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = BrutalBlack,
                            focusedTextColor = BrutalBlack,
                            unfocusedTextColor = BrutalBlack
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { onPlayDirectUrl(directUrlInput) }
                        )
                    )

                    Box(
                        modifier = Modifier
                            .background(if (isFetchingDirectLink) BrutalOrange else BrutalYellow)
                            .border(1.5.dp, BrutalBlack)
                            .clickable(enabled = !isFetchingDirectLink && directUrlInput.isNotBlank()) {
                                onPlayDirectUrl(directUrlInput)
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .testTag("direct_play_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFetchingDirectLink) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = BrutalBlack
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = BrutalBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PLAY",
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = BrutalBlack
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Genre Navigation Pills
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(GenrePresets.GENRES) { genre ->
                val isSelected = selectedGenre.equals(genre.name, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .border(2.dp, BrutalBlack)
                        .background(if (isSelected) BrutalBlack else BrutalWhite)
                        .clickable { onSelectGenre(genre.name) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("genre_filter_${genre.name}")
                ) {
                    Text(
                        text = genre.name,
                        color = if (isSelected) BrutalYellow else BrutalBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Results Section if query is present
        if (searchQuery.isNotBlank() || isSearching) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrutalBadge(
                            text = "SEARCH RESULTS",
                            backgroundColor = BrutalYellow,
                            textColor = BrutalBlack
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "\"$searchQuery\"",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = BrutalBlack
                        )
                    }

                    Text(
                        text = "[CLEAR]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = BrutalOrange,
                        modifier = Modifier
                            .clickable(onClick = onClearSearch)
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(2.dp, BrutalBlack)
                            .background(BrutalWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = BrutalBlack,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "QUERYING YOUTUBE SERVERS...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = BrutalBlack
                            )
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, BrutalBlack)
                            .background(BrutalWhite)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "NO TRACKS RETURNED FOR \"$searchQuery\". TRY ANOTHER QUERY OR PASTE A DIRECT YOUTUBE LINK.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = BrutalBlack
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        searchResults.forEachIndexed { index, track ->
                            TrackCard(
                                track = track,
                                onPlay = { onPlayTrack(track, searchResults) },
                                onFavoriteToggle = { onFavoriteToggle(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onAddToPlaylist = { onAddToPlaylist(track) },
                                isPlaying = currentPlayingVideoId == track.videoId && isPlaying,
                                trackIndex = index + 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                BrutalDivider(thickness = 3.dp)
            }
        }

        // Live Feed Section (Genre or Trending)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$selectedGenre FEED",
                        color = BrutalBlack,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BrutalBadge(
                        text = "${tracks.size} TRACKS",
                        backgroundColor = BrutalBlack,
                        textColor = BrutalWhite
                    )
                }

                BrutalBadge(
                    text = "LIVE YT",
                    backgroundColor = BrutalYellow,
                    textColor = BrutalBlack
                )
            }

            Text(
                text = "REAL-TIME YOUTUBE AUDIO // TAP ANY CARD TO STREAM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            if (isLoadingTracks) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .border(2.5.dp, BrutalBlack)
                        .background(BrutalWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = BrutalBlack
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "FETCHING LIVE $selectedGenre STREAMS...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = BrutalBlack
                        )
                    }
                }
            } else if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, BrutalBlack)
                        .background(BrutalWhite)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "NO TRACKS DISCOVERED YET. PLEASE CHECK NETWORK OR PASTE A DIRECT YOUTUBE LINK.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = BrutalBlack
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tracks.forEachIndexed { index, track ->
                        TrackCard(
                            track = track,
                            onPlay = { onPlayTrack(track, tracks) },
                            onFavoriteToggle = { onFavoriteToggle(track) },
                            onAddToQueue = { onAddToQueue(track) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            isPlaying = currentPlayingVideoId == track.videoId && isPlaying,
                            trackIndex = index + 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Footer Section
        FooterSection()
    }
}
