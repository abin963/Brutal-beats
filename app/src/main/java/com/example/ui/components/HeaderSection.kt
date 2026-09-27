package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.source.PreferredSourceMode
import com.example.ui.theme.NyxPurple
import com.example.ui.theme.NyxPurpleLight
import com.example.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onClearSearch: () -> Unit,
    searchSuggestions: List<String>,
    isSearching: Boolean,
    preferredSourceMode: PreferredSourceMode = PreferredSourceMode.AUTO,
    onSourceModeChanged: (PreferredSourceMode) -> Unit = {},
    title: String = "Listen Now",
    showListenNowHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDark by ThemeManager.isDarkMode.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearchExpanded = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // TOP HEADER: [ Profile Avatar ]  "Listen Now"  [ Search ] [ Notification/Settings ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Profile/Avatar Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .border(1.5.dp, NyxPurple.copy(alpha = 0.6f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 22.dp),
                        onClick = { showProfileSheet = true }
                    )
                    .testTag("profile_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = NyxPurpleLight,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Center/Left: "Listen Now" (or custom title)
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            )

            // Right: [ Search button ] [ Notification/Settings button ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Toggle Button
                IconButton(
                    onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded && searchQuery.isNotBlank()) {
                            onClearSearch()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSearchExpanded || searchQuery.isNotBlank())
                                NyxPurple.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .border(
                            1.dp,
                            if (isSearchExpanded || searchQuery.isNotBlank()) NyxPurple else Color.Transparent,
                            CircleShape
                        )
                        .testTag("search_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded && searchQuery.isEmpty()) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchExpanded || searchQuery.isNotBlank()) NyxPurpleLight else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Notification / Settings Quick Button
                IconButton(
                    onClick = { showNotificationSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .testTag("notifications_button")
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        // Notification dot indicator
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NyxPurple)
                        )
                    }
                }
            }
        }

        // Animated Search Bar Area
        AnimatedVisibility(
            visible = isSearchExpanded || searchQuery.isNotBlank(),
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(12.dp))

                // Modern Translucent Search Input Field
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (searchQuery.isNotEmpty()) NyxPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
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
                                .testTag("search_input"),
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

                        // Search Submit Button (min 44dp touch target)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSearching) MaterialTheme.colorScheme.surfaceVariant else NyxPurple)
                                .clickable {
                                    focusManager.clearFocus()
                                    onSearchSubmit(searchQuery)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("search_submit_button")
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

                Spacer(modifier = Modifier.height(10.dp))

                // Engine Source Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "AUDIO SOURCE",
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
                                modifier = Modifier
                                    .clickable { onSourceModeChanged(mode) }
                                    .testTag("source_mode_${mode.name.lowercase()}"),
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

                // Suggestions row if available
                if (searchSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchSuggestions) { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NyxPurple.copy(alpha = 0.25f)),
                                modifier = Modifier.clickable {
                                    onSearchQueryChange(suggestion)
                                    onSearchSubmit(suggestion)
                                    focusManager.clearFocus()
                                }
                            ) {
                                Text(
                                    text = "↗ $suggestion",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // NYX Profile & Preferences Modal Sheet
    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar with purple glow
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NyxPurple, NyxPurpleLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "NYX Listener",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Futuristic Music Experience",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Theme Toggle Item
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ThemeManager.toggleTheme(context) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = NyxPurpleLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isDark) "Dark Theme" else "Light Theme",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Deep black with violet atmospheric glow",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = isDark,
                            onCheckedChange = { ThemeManager.toggleTheme(context) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NyxPurple
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Source Mode Item
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Playback Engine",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Current: ${preferredSourceMode.name} (YouTube & JioSaavn 320kbps)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PreferredSourceMode.entries.forEach { mode ->
                                val isSelected = preferredSourceMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSourceModeChanged(mode) },
                                    label = { Text(mode.name, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NyxPurple,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // NYX Quick Notifications / Updates Dialog
    if (showNotificationSheet) {
        AlertDialog(
            onDismissRequest = { showNotificationSheet = false },
            title = {
                Text(
                    text = "NYX Updates",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "• Welcome to the redesigned NYX interface!",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Enjoy seamless playback across YouTube and JioSaavn with our new dark ambient purple experience.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationSheet = false }) {
                    Text("Got it", color = NyxPurple, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
