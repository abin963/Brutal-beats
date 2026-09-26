package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HistoryScreen(
    history: List<Track>,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BrutalOffWhite)
            .padding(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PLAYBACK HISTORY",
                    color = BrutalBlack,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "LOCAL PERSISTENCE // SESSION TIMELINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF666666)
                )
            }

            BrutalBadge(
                text = "${history.size} PLAYED",
                backgroundColor = BrutalYellow,
                textColor = BrutalBlack
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (history.isNotEmpty()) {
            BrutalButton(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalWhite,
                testTag = "purge_history_btn"
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear History",
                    tint = Color(0xFFCC0000),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PURGE COMPLETE PLAYBACK HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCC0000)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (history.isEmpty()) {
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
                        text = "HISTORY LOG EMPTY",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = BrutalBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ANY TRACK YOU PLAY IS AUTOMATICALLY PRESERVED HERE IN ROOM DB.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF666666)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                history.forEachIndexed { index, track ->
                    TrackCard(
                        track = track,
                        onPlay = { onPlayTrack(track, history) },
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
}
