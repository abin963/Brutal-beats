package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.ui.theme.NyxCyan
import com.example.ui.theme.NyxPink
import com.example.ui.theme.NyxPurple
import com.example.ui.theme.NyxPurpleLight

@Composable
fun TrackCard(
    track: Track,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    trackIndex: Int? = null,
    onStartRadio: (() -> Unit)? = null
) {
    val isSaavn = track.sourceId.contains("SAAVN", ignoreCase = true)
    val cardBorderColor by animateColorAsState(
        targetValue = if (isPlaying) NyxPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        animationSpec = tween(200),
        label = "border_color"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onPlay
            )
            .testTag("track_card_${track.videoId}"),
        shape = RoundedCornerShape(18.dp),
        color = if (isPlaying) Color(0xD91D1638) else Color(0x99130F24),
        tonalElevation = if (isPlaying) 6.dp else 2.dp,
        shadowElevation = if (isPlaying) 8.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) {
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(NyxPurpleLight, NyxPurple, Color(0x33A855F7))
                )
            } else {
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                )
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional Index number
            if (trackIndex != null) {
                Text(
                    text = String.format("%02d", trackIndex),
                    color = if (isPlaying) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.width(26.dp)
                )
            }

            // Real Thumbnail Artwork
            BrutalThumbnail(
                imageUrl = track.thumbnailUrl,
                videoId = track.videoId,
                sourceId = track.sourceId,
                contentDescription = track.title,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Track details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isPlaying) NyxPurpleLight else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quality pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSaavn) NyxCyan.copy(alpha = 0.15f) else NyxPurple.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isSaavn) "320K" else "YT",
                            color = if (isSaavn) NyxCyan else NyxPurpleLight,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (track.duration.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = track.duration,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Radio Button (44dp target)
                if (onStartRadio != null) {
                    IconButton(
                        onClick = onStartRadio,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("track_radio_btn_${track.videoId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Start Radio",
                            tint = NyxPurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Favorite Button (44dp target)
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) NyxPink else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Add to Playlist Button (44dp target)
                IconButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Large 48dp Play / Pause circular action button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) NyxPurple else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 24.dp),
                            onClick = onPlay
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
