package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.source.PreferredSourceMode
import com.example.ui.theme.AccentColorOption
import com.example.ui.theme.NyxPurple
import com.example.ui.theme.NyxPurpleLight
import com.example.ui.theme.ThemeManager
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.PlayerUiState

@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    playerState: PlayerUiState,
    preferredSourceMode: PreferredSourceMode,
    onSourceModeChanged: (PreferredSourceMode) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark by ThemeManager.isDarkMode.collectAsState()
    val isAmoled by ThemeManager.isAmoledMode.collectAsState()
    val currentAccent by ThemeManager.accentColor.collectAsState()

    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Appearance & Theming
        item {
            SettingsSectionHeader(title = "APPEARANCE & THEME", icon = Icons.Default.Palette)
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Dark Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dark Mode",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "High-contrast dark aesthetic with ambient glow",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { ThemeManager.setDarkMode(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = currentAccent.primary
                            )
                        )
                    }

                    if (isDark) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Pure AMOLED Black Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pure AMOLED Black",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Deep pitch-black for OLED displays and battery savings",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = isAmoled,
                                onCheckedChange = { ThemeManager.setAmoledMode(context, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = currentAccent.primary
                                )
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Accent Color Selection
                    Text(
                        text = "Accent Color",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active: ${currentAccent.title}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccentColorOption.entries.forEach { option ->
                            val isSelected = currentAccent == option
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { ThemeManager.setAccentColor(context, option) }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(option.primary, option.light))
                                        )
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = option.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) option.light else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Playback & Controls
        item {
            SettingsSectionHeader(title = "PLAYBACK & ENGINE", icon = Icons.Default.Tune)
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Default Playback Speed
                    Text(
                        text = "Default Playback Speed",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Current: ${playerState.playbackSpeed}×",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { sp ->
                            val isSelected = kotlin.math.abs(playerState.playbackSpeed - sp) < 0.01f
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setPlaybackSpeed(sp) },
                                label = { Text("${sp}×", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = currentAccent.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Seek Jump Interval
                    Text(
                        text = "Seek Jump Interval",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Interval for forward and rewind buttons: ${playerState.seekIntervalSec}s",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { sec ->
                            val isSelected = playerState.seekIntervalSec == sec
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSeekInterval(sec) },
                                label = { Text("${sec}s", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = currentAccent.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Default Mode (Audio vs Video)
                    Text(
                        text = "Default Playback Mode",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (playerState.isVideoMode) "Video Mode (YouTube Player active)" else "Audio Mode (Ambient Artwork active)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !playerState.isVideoMode,
                            onClick = { viewModel.setDefaultPlaybackMode("AUDIO") },
                            label = { Text("🎧 Audio Mode", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentAccent.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = playerState.isVideoMode,
                            onClick = { viewModel.setDefaultPlaybackMode("VIDEO") },
                            label = { Text("🎬 Video Mode", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentAccent.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Autoplay Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Autoplay Similar Tracks",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically play recommendations when the current queue finishes",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = playerState.isAutoplayEnabled,
                            onCheckedChange = { viewModel.toggleAutoplay() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = currentAccent.primary
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Preferred Source Mode
                    Text(
                        text = "Preferred Music Source",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active: ${preferredSourceMode.name} (Direct 320kbps streams where available)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PreferredSourceMode.entries.forEach { mode ->
                            val isSelected = preferredSourceMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSourceModeChanged(mode) },
                                label = { Text(mode.name, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = currentAccent.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: Storage & Downloads
        item {
            SettingsSectionHeader(title = "STORAGE & DOWNLOADS", icon = Icons.Default.Download)
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Downloaded Audio Size",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${viewModel.getTotalDownloadsSize()} used • ${viewModel.getFreeStorage()} free on device",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = { showClearDownloadsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                        ) {
                            Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Downloaded songs are stored locally in app storage for offline listening without internet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Section: About & Licensing
        item {
            SettingsSectionHeader(title = "ABOUT NYX", icon = Icons.Default.Info)
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NYX Music v2.4.0",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Futuristic Android music player built with Jetpack Compose & AndroidX Media3 ExoPlayer.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Features adapted from Metrolist & YTMusicUltimate licensed under GPL-3.0.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (showClearDownloadsDialog) {
        AlertDialog(
            onDismissRequest = { showClearDownloadsDialog = false },
            title = {
                Text(
                    text = "Clear All Downloads?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This will delete all offline downloaded songs from your device storage. You can re-download them anytime.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllDownloads()
                        showClearDownloadsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDownloadsDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = Color(0xFF140F26)
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NyxPurpleLight,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = NyxPurpleLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
