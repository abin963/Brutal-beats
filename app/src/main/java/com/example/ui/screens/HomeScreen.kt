package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.*

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
    var selectedCategory by remember { mutableStateOf("For You") }
    val categories = remember {
        listOf("For You", "New Artists", "Hot Tracks", "Editor's Picks")
    }

    // Determine featured track from real tracks data
    val featuredTrack = remember(tracks, selectedCategory) {
        when (selectedCategory) {
            "New Artists" -> tracks.asReversed().firstOrNull() ?: tracks.firstOrNull()
            "Hot Tracks" -> tracks.drop(1).firstOrNull() ?: tracks.firstOrNull()
            "Editor's Picks" -> tracks.drop(2).firstOrNull() ?: tracks.firstOrNull()
            else -> tracks.firstOrNull() ?: recentTracks.firstOrNull()
        }
    }

    // Ambient Purple Glow Background Container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Soft purple ambient glow near top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NyxPurple.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
        ) {
            // SEARCH RESULTS SECTION (If user is currently searching)
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
                            Text("Clear", color = NyxPurpleLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NyxPurple)
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NyxPurple.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "NO TRACKS FOUND",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try searching for a different song, artist, or music title.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
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
                // NORMAL HOME SCREEN EXPERIENCE

                // 2. CATEGORY CHIPS: [ For You ] [ New Artists ] [ Hot Tracks ] [ Editor's Picks ]
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategory == category

                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = if (isSelected) NyxPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NyxPurpleLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, radius = 24.dp),
                                        onClick = { selectedCategory = category }
                                    )
                                    .testTag("category_chip_${category.lowercase().replace(" ", "_")}")
                            ) {
                                Text(
                                    text = category,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                // 3. FEATURED MUSIC CARD
                if (featuredTrack != null) {
                    item {
                        val isFeaturedPlaying = isPlaying && featuredTrack.videoId == currentPlayingVideoId

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onPlayTrack(featuredTrack, tracks) }
                                .testTag("featured_music_card"),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isFeaturedPlaying) NyxPurple else NyxPurple.copy(alpha = 0.35f)
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Real Song Artwork Background
                                BrutalThumbnail(
                                    imageUrl = featuredTrack.thumbnailUrl,
                                    videoId = featuredTrack.videoId,
                                    sourceId = featuredTrack.sourceId,
                                    contentDescription = featuredTrack.title,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(24.dp)
                                )

                                // Dark Gradient Scrim Overlay for crisp readability
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0x33000000),
                                                    Color(0x88000000),
                                                    Color(0xFA090812)
                                                ),
                                                startY = 0f,
                                                endY = 700f
                                            )
                                        )
                                )

                                // Content Overlay
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Top Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = NyxPurple.copy(alpha = 0.85f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NyxPurpleLight)
                                        ) {
                                            Text(
                                                text = "FOR YOU · FEATURED",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.8.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        if (isFeaturedPlaying) {
                                            Surface(
                                                shape = CircleShape,
                                                color = NyxPurple
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.GraphicEq,
                                                        contentDescription = "Playing",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "PLAYING",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Bottom Track Details & Start Listening Button
                                    Column {
                                        Text(
                                            text = featuredTrack.title,
                                            color = Color.White,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = featuredTrack.artist,
                                            color = NyxPurpleLight,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Feel the beat · Explore trending music personalized for you",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // "Start Listening" Pill Button (48dp height)
                                        Button(
                                            onClick = { onPlayTrack(featuredTrack, tracks) },
                                            shape = RoundedCornerShape(24.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NyxPurple
                                            ),
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("start_listening_button")
                                        ) {
                                            Icon(
                                                imageVector = if (isFeaturedPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isFeaturedPlaying) "Pause" else "Start Listening",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. POPULAR SECTION (Title: "Popular", "See all >")
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Popular",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { /* See all popular tracks */ }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "See all",
                                color = NyxPurpleLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "See all",
                                tint = NyxPurpleLight,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Popular Tracks List
                if (isLoadingTracks && tracks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NyxPurple)
                        }
                    }
                } else {
                    val popularList = tracks.drop(1).take(8).ifEmpty { tracks.take(8) }
                    itemsIndexed(popularList) { index, track ->
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

                // RECENTLY PLAYED / JUMP BACK IN SECTION
                if (recentTracks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Recently Played",
                                tint = NyxPurpleLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Jump Back In",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentTracks.take(8)) { track ->
                                HorizontalTrackCard(
                                    track = track,
                                    isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                                    onPlay = { onPlayTrack(track, recentTracks) }
                                )
                            }
                        }
                    }
                }

                // FAVORITES ROW (If available)
                if (favoriteTracks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Your Favorites",
                                tint = NyxPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Your Favorites",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(favoriteTracks.take(8)) { track ->
                                HorizontalTrackCard(
                                    track = track,
                                    isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                                    onPlay = { onPlayTrack(track, favoriteTracks) }
                                )
                            }
                        }
                    }
                }

                // MORE RECOMMENDED MUSIC
                if (tracks.size > 9) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recommended For You",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    itemsIndexed(tracks.drop(9)) { index, track ->
                        TrackCard(
                            track = track,
                            isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                            trackIndex = index + 10,
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
}
