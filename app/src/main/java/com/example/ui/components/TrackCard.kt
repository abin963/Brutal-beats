package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Track
import com.example.ui.theme.*

@Composable
fun TrackCard(
    track: Track,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    isPlaying: Boolean = false,
    trackIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    BrutalCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("track_card_${track.videoId}"),
        backgroundColor = if (isPlaying) BrutalYellow else BrutalWhite,
        borderColor = BrutalBlack,
        shadowColor = BrutalBlack,
        borderWidth = 3.dp,
        shadowOffset = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number label if provided
                if (trackIndex != null) {
                    BrutalTrackNumber(
                        number = trackIndex,
                        modifier = Modifier.padding(end = 8.dp),
                        color = BrutalBlack
                    )
                }

                // Thumbnail with brutalist border
                Box(
                    modifier = Modifier
                        .size(width = 86.dp, height = 56.dp)
                        .border(2.dp, BrutalBlack)
                        .background(BrutalDeepBlack)
                ) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Mini source sticker
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(if (track.sourceId == "JIOSAAVN") BrutalCyan else BrutalBlack)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (track.sourceId == "JIOSAAVN") "SAAVN" else "YT",
                            color = if (track.sourceId == "JIOSAAVN") BrutalDeepBlack else BrutalWhite,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Track Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = BrutalBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist.uppercase(),
                        color = Color(0xFF333333),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrutalBadge(
                            text = track.genre,
                            backgroundColor = BrutalBlack,
                            textColor = BrutalWhite,
                            borderWidth = 1.dp
                        )
                        if (isPlaying) {
                            BrutalBadge(
                                text = "PLAYING",
                                backgroundColor = BrutalOrange,
                                textColor = BrutalWhite,
                                borderWidth = 1.dp
                            )
                        } else {
                            Text(
                                text = track.duration,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = BrutalBlack
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            BrutalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SRC: ${track.sourceId} // ${if (track.sourceId == "JIOSAAVN") "320KBPS DIRECT" else "YOUTUBE STREAM"}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF444444)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Queue Button
                    IconButton(
                        onClick = onAddToQueue,
                        modifier = Modifier
                            .size(36.dp)
                            .border(2.dp, BrutalBlack)
                            .background(BrutalWhite)
                            .testTag("queue_btn_${track.videoId}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Add to Queue",
                            tint = BrutalBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Playlist add button
                    if (onAddToPlaylist != null) {
                        IconButton(
                            onClick = onAddToPlaylist,
                            modifier = Modifier
                                .size(36.dp)
                                .border(2.dp, BrutalBlack)
                                .background(BrutalWhite)
                                .testTag("playlist_btn_${track.videoId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = BrutalBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .border(2.dp, BrutalBlack)
                            .background(if (track.isFavorite) BrutalPink else BrutalWhite)
                            .testTag("fav_btn_${track.videoId}")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) BrutalWhite else BrutalBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Play Button
                    Box(
                        modifier = Modifier
                            .border(2.dp, BrutalBlack)
                            .background(if (isPlaying) BrutalBlack else BrutalYellow)
                            .clickable(onClick = onPlay)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("play_btn_${track.videoId}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isPlaying) BrutalYellow else BrutalBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isPlaying) "ACTIVE" else "PLAY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isPlaying) BrutalYellow else BrutalBlack
                            )
                        }
                    }
                }
            }
        }
    }
}
