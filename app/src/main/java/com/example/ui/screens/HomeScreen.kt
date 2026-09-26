package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.components.BrutalThumbnail
import com.example.ui.components.HorizontalTrackCard
import com.example.ui.components.TrackCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonPink

@Composable
fun HomeScreen(
    tracks: List<Track>,
    isLoadingTracks: Boolean,
    recentTracks: List<Track>,
    favoriteTracks: List<Track>,
    searchResults: List<Track>,
    isSearching: Boolean,
    searchQuery: String,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        // Search Results Section if active
        if (searchQuery.isNotBlank() || isSearching) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEARCH RESULTS: \"$searchQuery\"",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )

                    TextButton(onClick = onClearSearch) {
                        Text("Clear", color = NeonLime, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonLime)
                    }
                }
            } else if (searchResults.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NO TRACKS FOUND",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try searching for a different song, artist, or YouTube link.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(searchResults) { index, track ->
                    TrackCard(
                        track = track,
                        isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                        trackIndex = index + 1,
                        onPlay = { onPlayTrack(track, searchResults) },
                        onFavoriteToggle = { onFavoriteToggle(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    )
                }
            }
        } else {
            // Standard Home View Content

            // Quick Access Recently Played Grid (top 4-6 songs)
            if (recentTracks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Jump Back In",
                            tint = NeonLime,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Jump Back In",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val quickGrid = recentTracks.take(4)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in quickGrid.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (j in i until (i + 2).coerceAtMost(quickGrid.size)) {
                                    val track = quickGrid[j]
                                    val isTrackPlaying = isPlaying && track.videoId == currentPlayingVideoId

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onPlayTrack(track, recentTracks) },
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isTrackPlaying) NeonLime else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BrutalThumbnail(
                                                imageUrl = track.thumbnailUrl,
                                                videoId = track.videoId,
                                                sourceId = track.sourceId,
                                                contentDescription = track.title,
                                                modifier = Modifier.size(42.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = track.title,
                                                color = if (isTrackPlaying) NeonLime else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                if (i + 1 >= quickGrid.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Trending / Recommended Horizontal Carousel
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Trending",
                        tint = NeonLime,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trending Beats",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoadingTracks && tracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonLime)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(tracks.take(10)) { track ->
                            HorizontalTrackCard(
                                track = track,
                                isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                                onPlay = { onPlayTrack(track, tracks) }
                            )
                        }
                    }
                }
            }

            // Favorites section preview (if user has favorites)
            if (favoriteTracks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorites",
                            tint = NeonPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Your Favorites",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(favoriteTracks) { track ->
                            HorizontalTrackCard(
                                track = track,
                                isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                                onPlay = { onPlayTrack(track, favoriteTracks) }
                            )
                        }
                    }
                }
            }

            // Recommended Feed list
            item {
                Text(
                    text = "Recommended For You",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (tracks.isEmpty() && isLoadingTracks) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonLime)
                    }
                }
            } else {
                itemsIndexed(tracks.drop(10).ifEmpty { tracks }) { index, track ->
                    TrackCard(
                        track = track,
                        isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                        trackIndex = index + 1,
                        onPlay = { onPlayTrack(track, tracks) },
                        onFavoriteToggle = { onFavoriteToggle(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    )
                }
            }
        }
    }
}
