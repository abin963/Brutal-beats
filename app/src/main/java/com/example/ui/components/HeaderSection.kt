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
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.*

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
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // TOP HEADER: [ Profile Avatar ]  "Listen Now"  [ Search ] [ Notification/Settings ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Glass Profile/Avatar Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0x1CFFFFFF) else Color(0xD8FFFFFF)
                    )
                    .border(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(NyxPurpleLight.copy(alpha = 0.8f), NyxPurple.copy(alpha = 0.3f))
                        ),
                        CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 23.dp),
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
                // Glass Search Toggle Button
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
                                NyxPurple.copy(alpha = 0.25f)
                            else if (isDark) Color(0x16FFFFFF) else Color(0x99FFFFFF)
                        )
                        .border(
                            1.dp,
                            if (isSearchExpanded || searchQuery.isNotBlank())
                                Brush.verticalGradient(listOf(NyxPurpleLight, NyxPurple))
                            else Brush.verticalGradient(listOf(Color(0x35FFFFFF), Color(0x10FFFFFF))),
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

                // Glass Notification / Settings Quick Button
                IconButton(
                    onClick = { showNotificationSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x16FFFFFF) else Color(0x99FFFFFF))
                        .border(
                            1.dp,
                            Brush.verticalGradient(listOf(Color(0x35FFFFFF), Color(0x10FFFFFF))),
                            CircleShape
                        )
                        .testTag("notifications_button")
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        // Glowing notification dot indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NyxPurple)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }
                }
            }
        }

        // Animated Floating Glass Search Bar Area
        AnimatedVisibility(
            visible = isSearchExpanded || searchQuery.isNotBlank(),
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(12.dp))

                // Modern Floating Glass Search Input Field
                val searchShape = RoundedCornerShape(20.dp)
                val searchBg = if (isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xE0181432),
                            Color(0xF00F0C22)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF5FFFFFF),
                            Color(0xE5F3EFFE)
                        )
                    )
                }

                val searchBorder = if (searchQuery.isNotEmpty()) {
                    Brush.verticalGradient(
                        listOf(NyxPurpleLight, NyxPurple.copy(alpha = 0.6f))
                    )
                } else {
                    if (isDark) {
                        Brush.verticalGradient(
                            listOf(Color(0x35FFFFFF), NyxPurple.copy(alpha = 0.25f))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(NyxPurple.copy(alpha = 0.4f), Color(0x18DCD6EF))
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = searchShape,
                            spotColor = if (searchQuery.isNotEmpty()) NyxPurple else Color(0x18A855F7),
                            ambientColor = Color(0x22000000)
                        )
                        .clip(searchShape)
                        .background(searchBg, searchShape)
                        .border(1.dp, searchBorder, searchShape)
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
                        val submitShape = RoundedCornerShape(12.dp)
                        val submitBgColor = MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            modifier = Modifier
                                .clip(submitShape)
                                .then(
                                    if (isSearching) {
                                        Modifier.background(submitBgColor, submitShape)
                                    } else {
                                        Modifier.background(Brush.linearGradient(listOf(NyxPurple, NyxPurpleLight)), submitShape)
                                    }
                                )
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

                // Engine Source Selector Glass Pills
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
                            val chipShape = RoundedCornerShape(12.dp)
                            val chipUnselectedColor = if (isDark) Color(0x18FFFFFF) else Color(0x99EDE9FE)
                            Box(
                                modifier = Modifier
                                    .clip(chipShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(
                                                Brush.horizontalGradient(listOf(NyxPurple, NyxPurpleLight)),
                                                chipShape
                                            )
                                        } else {
                                            Modifier.background(chipUnselectedColor, chipShape)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NyxPurpleLight else Color(0x25A855F7),
                                        chipShape
                                    )
                                    .clickable { onSourceModeChanged(mode) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("source_mode_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.name,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
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
                            val sugShape = RoundedCornerShape(20.dp)
                            Box(
                                modifier = Modifier
                                    .clip(sugShape)
                                    .background(
                                        if (isDark) Color(0x16FFFFFF) else Color(0x99EDE9FE),
                                        sugShape
                                    )
                                    .border(1.dp, NyxPurple.copy(alpha = 0.3f), sugShape)
                                    .clickable {
                                        onSearchQueryChange(suggestion)
                                        onSearchSubmit(suggestion)
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "↗ $suggestion",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
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
