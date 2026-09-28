package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.source.PreferredSourceMode
import com.example.ui.components.TrackCard
import com.example.ui.theme.NyxPurple
import com.example.ui.theme.NyxPurpleLight

@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onClearSearch: () -> Unit,
    searchResults: List<Track>,
    isSearching: Boolean,
    searchSuggestions: List<String>,
    preferredSourceMode: PreferredSourceMode,
    onSourceModeChanged: (PreferredSourceMode) -> Unit,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onStartRadio: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val quickTopics = remember {
        listOf("Trending", "Lo-Fi Beats", "Pop Hits", "EDM", "Hip Hop", "Chill Vibes", "Acoustic", "Bollywood")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
    ) {
        // Search Header Title
        item {
            Text(
                text = "Search",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }

        // Search Input Bar
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xB3110D24),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (searchQuery.isNotEmpty()) {
                        Brush.verticalGradient(listOf(NyxPurpleLight, NyxPurple, Color(0x33A855F7)))
                    } else {
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f)))
                    }
                ),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotEmpty()) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = "Search songs, artists, albums...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_tab_input"),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = NyxPurple,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                onSearchSubmit(searchQuery)
                            }
                        )
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSearching) MaterialTheme.colorScheme.surfaceVariant else NyxPurple)
                            .clickable {
                                focusManager.clearFocus()
                                onSearchSubmit(searchQuery)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("search_tab_submit_button")
                    ) {
                        Text(
                            text = if (isSearching) "..." else "Search",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Audio Source Mode row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SEARCH ENGINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PreferredSourceMode.entries.forEach { mode ->
                        val isSelected = preferredSourceMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NyxPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { onSourceModeChanged(mode) },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NyxPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        ) {
                            Text(
                                text = mode.name,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Topic Chips
        item {
            Column {
                Text(
                    text = "Explore Topics",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTopics) { topic ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NyxPurple.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    onSearchQueryChange(topic)
                                    onSearchSubmit(topic)
                                    focusManager.clearFocus()
                                }
                        ) {
                            Text(
                                text = topic,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Suggestions if any
        if (searchSuggestions.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Recent Searches",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchSuggestions) { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    onSearchQueryChange(suggestion)
                                    onSearchSubmit(suggestion)
                                    focusManager.clearFocus()
                                }
                            ) {
                                Text(
                                    text = "↗ $suggestion",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Results or Loading or Empty
        if (isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NyxPurple)
                }
            }
        } else if (searchResults.isNotEmpty()) {
            item {
                Text(
                    text = "Results (${searchResults.size})",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(searchResults) { index, track ->
                TrackCard(
                    track = track,
                    isPlaying = isPlaying && track.videoId == currentPlayingVideoId,
                    trackIndex = index + 1,
                    onPlay = { onPlayTrack(track, searchResults) },
                    onFavoriteToggle = { onFavoriteToggle(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onStartRadio = { onStartRadio(track) }
                )
            }
        } else if (searchQuery.isNotBlank()) {
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
                            text = "Try searching for a different song or artist.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
