package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.components.BrutalBadge
import com.example.ui.components.BrutalButton
import com.example.ui.components.BrutalCard
import com.example.ui.components.BrutalDivider
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerUiState

@Composable
fun NowPlayingScreen(
    playerState: PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onToggleQueue: () -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onPlayQueueItem: (Track) -> Unit,
    onMinimize: () -> Unit,
    onStateChanged: (Boolean) -> Unit,
    onTimeProgress: (Float, Float) -> Unit,
    onTrackEnded: () -> Unit,
    onError: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack
    if (track == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BrutalOffWhite)
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NO ACTIVE TRACK",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = BrutalBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SEARCH OR TAP ANY LIVE YOUTUBE STREAM TO BEGIN PLAYBACK.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(16.dp))
            BrutalButton(
                onClick = onMinimize,
                backgroundColor = BrutalYellow
            ) {
                Text(
                    text = "BACK TO DISCOVER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = BrutalBlack
                )
            }
        }
        return
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrutalOffWhite)
            .statusBarsPadding()
            .testTag("now_playing_screen")
    ) {
        // Brutalist Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrutalBlack)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMinimize,
                modifier = Modifier
                    .size(38.dp)
                    .border(2.dp, BrutalWhite)
                    .background(BrutalBlack)
                    .testTag("minimize_player_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize Player",
                    tint = BrutalYellow,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING // LIVE",
                    color = BrutalWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "POWERED BY YOUTUBE EMBED",
                    color = BrutalYellow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(
                onClick = { onToggleFavorite(track) },
                modifier = Modifier
                    .size(38.dp)
                    .border(2.dp, BrutalWhite)
                    .background(if (track.isFavorite) BrutalPink else BrutalBlack)
                    .testTag("player_fav_btn")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = BrutalWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        BrutalDivider(thickness = 3.dp)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(14.dp)
        ) {
            // Multi-Source Player Container (YouTube Embed OR Direct Audio Stream)
            BrutalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                backgroundColor = BrutalDeepBlack,
                borderColor = BrutalBlack,
                shadowColor = BrutalBlack,
                borderWidth = 3.5.dp,
                shadowOffset = 5.dp
            ) {
                if (playerState.playbackType == com.example.source.PlaybackType.DIRECT_AUDIO) {
                    // Direct Audio Brutalist Deck View
                    Box(modifier = Modifier.fillMaxSize().background(BrutalDeepBlack)) {
                        coil.compose.AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color(0xBB000000))
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    BrutalBadge(
                                        text = "DIRECT AUDIO // 320 KBPS",
                                        backgroundColor = BrutalYellow,
                                        textColor = BrutalBlack
                                    )
                                    BrutalBadge(
                                        text = playerState.resolvedMedia?.sourceName?.uppercase() ?: "JIOSAAVN",
                                        backgroundColor = BrutalCyan,
                                        textColor = BrutalDeepBlack
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Animated brutalist cassette tape indicator
                                    Text(
                                        text = if (playerState.isPlaying) "► ❚❚ AUDIO STREAM ACTIVE" else "❚❚ STREAM PAUSED",
                                        color = BrutalYellow,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ENGINE: ANDROID MEDIA",
                                        color = BrutalWhite,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "BUFFER: 100% OK",
                                        color = BrutalWhite,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // YouTube Embed View
                    val ytId = playerState.resolvedMedia?.youtubeVideoId ?: track.videoId
                    YouTubePlayerView(
                        videoId = ytId,
                        isPlaying = playerState.isPlaying,
                        volume = if (playerState.isMuted) 0f else playerState.volume,
                        seekToSeconds = playerState.seekTargetSec,
                        onStateChanged = onStateChanged,
                        onTimeProgress = onTimeProgress,
                        onTrackEnded = onTrackEnded,
                        onError = onError,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source Notice Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrutalBlack)
                    .border(2.dp, BrutalBlack)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SRC: ${playerState.resolvedMedia?.sourceName?.uppercase() ?: track.sourceId} // ${playerState.resolvedMedia?.resolutionNote ?: "RESOLVED"}",
                    color = BrutalWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                BrutalBadge(
                    text = if (playerState.playbackType == com.example.source.PlaybackType.DIRECT_AUDIO) "320K DIRECT" else "YT EMBED",
                    backgroundColor = if (playerState.playbackType == com.example.source.PlaybackType.DIRECT_AUDIO) BrutalCyan else BrutalYellow,
                    textColor = BrutalBlack,
                    borderWidth = 1.dp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track Details
            Text(
                text = track.title,
                color = BrutalBlack,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARTIST: ${track.artist.uppercase()}",
                    color = Color(0xFF333333),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                BrutalBadge(
                    text = track.genre,
                    backgroundColor = BrutalBlack,
                    textColor = BrutalWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Scrubber Section
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentSec = playerState.currentPositionSec
                val totalSec = playerState.totalDurationSec.coerceAtLeast(1f)
                val fraction = (currentSec / totalSec).coerceIn(0f, 1f)

                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentSec),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = BrutalBlack
                    )
                    Text(
                        text = formatTime(totalSec),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = BrutalBlack
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Brutalist Slider
                Slider(
                    value = fraction,
                    onValueChange = { newFraction ->
                        onSeek(newFraction * totalSec)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = BrutalYellow,
                        activeTrackColor = BrutalBlack,
                        inactiveTrackColor = BrutalGrayMedium
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Brutalist Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, BrutalBlack)
                        .background(if (playerState.isShuffling) BrutalYellow else BrutalWhite)
                        .testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = BrutalBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.5.dp, BrutalBlack)
                        .background(BrutalWhite)
                        .testTag("prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = BrutalBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Giant Play / Pause Button
                BrutalButton(
                    onClick = onPlayPause,
                    backgroundColor = if (playerState.isPlaying) BrutalOrange else BrutalYellow,
                    modifier = Modifier.height(56.dp),
                    testTag = "overlay_play_pause_button"
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = BrutalBlack,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (playerState.isPlaying) "PAUSE" else "PLAY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = BrutalBlack
                    )
                }

                // Next Button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.5.dp, BrutalBlack)
                        .background(BrutalWhite)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = BrutalBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Loop Button
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, BrutalBlack)
                        .background(if (playerState.isLooping) BrutalYellow else BrutalWhite)
                        .testTag("loop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Loop Track",
                        tint = BrutalBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            BrutalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Volume Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, BrutalBlack)
                    .background(BrutalWhite)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = BrutalBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "VOL: ${playerState.volume.toInt()}%",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = BrutalBlack
                )

                Spacer(modifier = Modifier.width(10.dp))

                Slider(
                    value = if (playerState.isMuted) 0f else playerState.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = BrutalBlack,
                        activeTrackColor = BrutalBlack,
                        inactiveTrackColor = BrutalGrayMedium
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Queue Control Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onToggleQueue)
                        .border(2.dp, BrutalBlack)
                        .background(if (playerState.isQueueVisible) BrutalYellow else BrutalWhite)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = BrutalBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLAYBACK QUEUE (${playerState.queue.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = BrutalBlack
                    )
                }

                if (playerState.queue.isNotEmpty()) {
                    Text(
                        text = "CLEAR QUEUE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCC0000),
                        modifier = Modifier
                            .clickable(onClick = onClearQueue)
                            .padding(4.dp)
                    )
                }
            }

            // Expandable Queue List
            AnimatedVisibility(visible = playerState.isQueueVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(2.5.dp, BrutalBlack)
                        .background(BrutalWhite)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "// UPCOMING TRACKS IN SEQUENCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF666666)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    playerState.queue.forEachIndexed { index, qTrack ->
                        val isCurrent = qTrack.videoId == track.videoId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isCurrent) BrutalYellow else Color.Transparent)
                                .clickable { onPlayQueueItem(qTrack) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (index < 9) "0${index + 1}." else "${index + 1}.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = BrutalBlack
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = qTrack.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.SansSerif,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = BrutalBlack
                                    )
                                    Text(
                                        text = qTrack.artist,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF444444)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onRemoveFromQueue(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = BrutalBlack,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (index < playerState.queue.lastIndex) {
                            BrutalDivider(thickness = 1.dp, color = BrutalGrayLight)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val mins = total / 60
    val secs = total % 60
    val sMins = if (mins < 10) "0$mins" else "$mins"
    val sSecs = if (secs < 10) "0$secs" else "$secs"
    return "$sMins:$sSecs"
}
