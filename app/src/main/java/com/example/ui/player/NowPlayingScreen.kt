package com.example.ui.player

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.source.PlaybackType
import com.example.ui.components.AmbientBackgroundLayer
import com.example.ui.components.BrutalThumbnail
import com.example.ui.components.TrackCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerUiState

@Composable
fun NowPlayingScreen(
    playerState: PlayerUiState,
    recommendations: List<Track>,
    isLoadingRecommendations: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleAutoplay: () -> Unit = {},
    onToggleFavorite: (Track) -> Unit,
    onToggleQueue: () -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onPlayQueueItem: (Track) -> Unit,
    onPlayRecommendedTrack: (Track) -> Unit,
    onToggleVideoMode: () -> Unit,
    onMinimize: () -> Unit,
    onStateChanged: (Boolean) -> Unit,
    onBufferingChanged: (Boolean) -> Unit,
    onTimeProgress: (Float, Float) -> Unit,
    onTrackEnded: () -> Unit,
    onError: (Int) -> Unit,
    radioState: com.example.ui.viewmodel.RadioState = com.example.ui.viewmodel.RadioState(),
    onStartRadio: ((Track) -> Unit)? = null,
    onStopRadio: (() -> Unit)? = null,
    onRetryRadio: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.92f else 1f,
        label = "big_play_scale"
    )

    // Dedicated local dragging and seek hold state to prevent progress bar from shaking/fighting with touch
    var isUserDragging by remember { mutableStateOf(false) }
    var dragProgressSec by remember { mutableFloatStateOf(0f) }
    var seekHoldSec by remember { mutableFloatStateOf(-1f) }
    var seekHoldTimestamp by remember { mutableLongStateOf(0L) }

    // When new song starts, reset seekHold and dragging cleanly
    LaunchedEffect(track.videoId) {
        seekHoldSec = -1f
        isUserDragging = false
    }

    // Release seek hold when playback position catches up or times out
    if (seekHoldSec >= 0f) {
        val elapsed = System.currentTimeMillis() - seekHoldTimestamp
        if (elapsed > 1000L || kotlin.math.abs(playerState.currentPositionSec - seekHoldSec) < 1.5f) {
            seekHoldSec = -1f
        }
    }

    val totalSec = playerState.totalDurationSec.coerceAtLeast(1f)
    val displayCurrentSec = when {
        isUserDragging -> dragProgressSec
        seekHoldSec >= 0f -> seekHoldSec
        else -> playerState.currentPositionSec
    }.coerceIn(0f, totalSec)

    val sliderFraction = (displayCurrentSec / totalSec).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Immersive Ambient Blurred Artwork Background
        AmbientBackgroundLayer(artworkUrl = track.thumbnailUrl)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
        // Top Action Bar: [Minimize] [NOW PLAYING & Source Info] [Video/Cover Toggle] [Favorite]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMinimize,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("minimize_player_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize Player",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
            ) {
                if (playerState.isRadioActive || radioState.isRadioActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NyxPurpleLight)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RADIO",
                            color = NyxPurpleLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    val seedTitle = radioState.seedTrack?.title ?: playerState.radioSeedTrack?.title ?: track.title
                    Text(
                        text = "Based on: $seedTitle",
                        color = NyxPurpleLight.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "NOW PLAYING",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (playerState.playbackType == PlaybackType.DIRECT_AUDIO) "JIOSAAVN 320KBPS" else "YOUTUBE STREAM",
                        color = if (playerState.playbackType == PlaybackType.DIRECT_AUDIO) NeonCyan else NeonLime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Video Mode Toggle (for YouTube playback)
                if (playerState.playbackType == PlaybackType.YOUTUBE_EMBED) {
                    IconButton(
                        onClick = onToggleVideoMode,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (playerState.isVideoMode) NeonLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (playerState.isVideoMode) Icons.Default.Image else Icons.Default.Videocam,
                            contentDescription = if (playerState.isVideoMode) "Show Artwork" else "Show Video",
                            tint = if (playerState.isVideoMode) NeonLime else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Favorite Toggle
                IconButton(
                    onClick = { onToggleFavorite(track) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (track.isFavorite) NeonPink.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("player_fav_btn")
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) NeonPink else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Main scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Artwork or Video Area: ALWAYS displays prominent high-res artwork without black rectangle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Artwork is ALWAYS loaded as the primary base layer filling the container
                    BrutalThumbnail(
                        imageUrl = track.thumbnailUrl,
                        videoId = track.videoId,
                        sourceId = track.sourceId,
                        contentDescription = "${track.title} by ${track.artist}",
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(22.dp),
                        showSourceBadge = false
                    )

                    // If user activated Video Mode on YouTube, overlay the video view
                    if (playerState.playbackType == PlaybackType.YOUTUBE_EMBED && playerState.isVideoMode) {
                        val ytId = playerState.resolvedMedia?.youtubeVideoId ?: track.videoId
                        YouTubePlayerView(
                            videoId = ytId,
                            isPlaying = playerState.isPlaying,
                            volume = if (playerState.isMuted) 0f else playerState.volume,
                            seekToSeconds = playerState.seekTargetSec,
                            trackTitle = track.title,
                            trackArtist = track.artist,
                            artworkUrl = track.thumbnailUrl,
                            onStateChanged = onStateChanged,
                            onBufferingChanged = onBufferingChanged,
                            onTimeProgress = onTimeProgress,
                            onTrackEnded = onTrackEnded,
                            onError = onError,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom gradient & status pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isDirect = playerState.playbackType == PlaybackType.DIRECT_AUDIO
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDirect) NeonCyan else NeonLime)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isDirect) "320 KBPS DIRECT" else "YT AUDIO STREAM",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = when {
                                playerState.isBuffering -> "⏳ BUFFERING..."
                                playerState.isPlaying -> "● ACTIVE PLAYBACK"
                                else -> "❚❚ PAUSED"
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title & Artist Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Share Button
                IconButton(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Listening to \"${track.title}\" by ${track.artist} on Brutal Beats!\nhttps://youtube.com/watch?v=${track.videoId}"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smooth Liquid Glass Progress Bar & Timestamps
            Slider(
                value = sliderFraction,
                onValueChange = { frac ->
                    isUserDragging = true
                    dragProgressSec = frac * totalSec
                },
                onValueChangeFinished = {
                    val target = dragProgressSec
                    seekHoldSec = target
                    seekHoldTimestamp = System.currentTimeMillis()
                    isUserDragging = false
                    onSeek(target)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_progress_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = NyxPurpleLight,
                    activeTrackColor = NyxPurple,
                    inactiveTrackColor = Color(0x33FFFFFF)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(displayCurrentSec),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (playerState.isBuffering) {
                    Text(
                        text = "BUFFERING",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NyxPurpleLight
                    )
                }
                Text(
                    text = formatTime(totalSec),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Central Playback Controls: [Shuffle] [Prev] [GIANT LIQUID GLASS PLAY/PAUSE] [Next] [Repeat]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (playerState.isShuffling) NyxPurple.copy(alpha = 0.25f) else Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.isShuffling) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Giant 68dp Circular Liquid Glass Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(playButtonScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NyxPurple, NyxPurpleLight)
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.15f))
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = playInteractionSource,
                            indication = ripple(bounded = true, radius = 34.dp),
                            onClick = onPlayPause
                        )
                        .testTag("overlay_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Next Button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Loop Button
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (playerState.isLooping) NyxPurple.copy(alpha = 0.25f) else Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (playerState.isLooping) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Autoplay & Radio Mode Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radio Button
                val isRadioOn = playerState.isRadioActive || radioState.isRadioActive
                Surface(
                    onClick = {
                        if (isRadioOn) {
                            onStopRadio?.invoke()
                        } else {
                            onStartRadio?.invoke(track)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isRadioOn) NyxPurple.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (isRadioOn) NyxPurpleLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("now_playing_radio_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Radio",
                            tint = if (isRadioOn) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRadioOn) "RADIO ON" else "RADIO",
                            color = if (isRadioOn) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Autoplay Toggle
                Surface(
                    onClick = onToggleAutoplay,
                    shape = RoundedCornerShape(20.dp),
                    color = if (playerState.isAutoplayEnabled) NyxPurple.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (playerState.isAutoplayEnabled) NyxPurpleLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("autoplay_toggle_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Autoplay",
                            tint = if (playerState.isAutoplayEnabled) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playerState.isAutoplayEnabled) "AUTOPLAY ON" else "AUTOPLAY OFF",
                            color = if (playerState.isAutoplayEnabled) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Volume & Queue Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleMute) {
                    Icon(
                        imageVector = if (playerState.isMuted || playerState.volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Slider(
                    value = if (playerState.isMuted) 0f else playerState.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                IconButton(
                    onClick = onToggleQueue,
                    modifier = Modifier.testTag("queue_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (playerState.isQueueVisible) NeonLime else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Queue Section if toggled
            AnimatedVisibility(visible = playerState.isQueueVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Header indicating Radio Queue or Regular Queue
                    if (playerState.isRadioActive || radioState.isRadioActive) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NyxPurple.copy(alpha = 0.14f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NyxPurple.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Radio,
                                            contentDescription = null,
                                            tint = NyxPurpleLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "RADIO QUEUE",
                                            color = NyxPurpleLight,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (onStopRadio != null) {
                                            TextButton(onClick = onStopRadio) {
                                                Text("Stop Radio", color = NyxPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        TextButton(onClick = onClearQueue) {
                                            Text("Clear", color = NyxPurpleLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                val seed = radioState.seedTrack?.title ?: playerState.radioSeedTrack?.title ?: track.title
                                Text(
                                    text = "Based on: $seed",
                                    color = NyxPurpleLight.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "NOW PLAYING: ${track.title}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UP NEXT (${playerState.queue.size})",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        if (!playerState.isRadioActive && !radioState.isRadioActive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = onToggleAutoplay) {
                                    Text(
                                        text = if (playerState.isAutoplayEnabled) "⚡ AUTOPLAY ON" else "AUTOPLAY OFF",
                                        color = if (playerState.isAutoplayEnabled) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(onClick = onClearQueue) {
                                    Text("Clear", color = NyxPurpleLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Radio loading or error status in queue
                    if (radioState.isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = NyxPurple, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Finding similar music...",
                                color = NyxPurpleLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (radioState.error != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = radioState.error,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            if (onRetryRadio != null) {
                                TextButton(onClick = onRetryRadio) {
                                    Text("Try Again", color = NyxPurpleLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    playerState.queue.forEachIndexed { idx, qTrack ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPlayQueueItem(qTrack) },
                            color = if (qTrack.videoId == track.videoId) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BrutalThumbnail(
                                    imageUrl = qTrack.thumbnailUrl,
                                    videoId = qTrack.videoId,
                                    sourceId = qTrack.sourceId,
                                    contentDescription = qTrack.title,
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = qTrack.title,
                                        color = if (qTrack.videoId == track.videoId) NeonLime else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = qTrack.artist,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveFromQueue(idx) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AUTOMATIC "RECOMMENDED FOR YOU" SECTION (Based on currently playing song)
            Spacer(modifier = Modifier.height(28.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Recommend,
                                contentDescription = null,
                                tint = NeonLime,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECOMMENDED FOR YOU",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Based on \"${track.title.take(30)}\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isLoadingRecommendations) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = NeonLime
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingRecommendations && recommendations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonLime)
                    }
                } else if (recommendations.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Loading related music...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    recommendations.forEach { recTrack ->
                        TrackCard(
                            track = recTrack,
                            isPlaying = false,
                            onPlay = { onPlayRecommendedTrack(recTrack) },
                            onFavoriteToggle = { onToggleFavorite(recTrack) },
                            onAddToQueue = { /* Queued directly */ },
                            onAddToPlaylist = {},
                            onStartRadio = onStartRadio?.let { sr -> { sr(recTrack) } },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

private fun formatTime(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val mins = total / 60
    val secs = total % 60
    return String.format("%02d:%02d", mins, secs)
}
