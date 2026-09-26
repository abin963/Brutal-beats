package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.source.PreferredSourceMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainTab

@Composable
fun HeaderSection(
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onClearSearch: () -> Unit,
    searchSuggestions: List<String>,
    isSearching: Boolean,
    preferredSourceMode: PreferredSourceMode = PreferredSourceMode.AUTO,
    onSourceModeChanged: (PreferredSourceMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BrutalOffWhite)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Top row: Brand & Live Source Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(BrutalBlack)
                    .border(2.5.dp, BrutalBlack)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "BRUTAL",
                    color = BrutalWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .background(BrutalYellow)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "BEATS",
                        color = BrutalBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Source Mode Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "SRC:",
                    color = BrutalBlack,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                PreferredSourceMode.entries.forEach { mode ->
                    val isSelected = preferredSourceMode == mode
                    Box(
                        modifier = Modifier
                            .border(1.5.dp, BrutalBlack)
                            .background(if (isSelected) BrutalBlack else BrutalWhite)
                            .clickable { onSourceModeChanged(mode) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("source_mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            color = if (isSelected) BrutalYellow else BrutalBlack,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Brutalist Search Bar
        BrutalCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalWhite,
            borderColor = BrutalBlack,
            shadowColor = BrutalBlack,
            borderWidth = 3.dp,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = BrutalBlack,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "SEARCH YOUTUBE + JIOSAAVN...",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF777777)
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
                        cursorColor = BrutalBlack,
                        focusedTextColor = BrutalBlack,
                        unfocusedTextColor = BrutalBlack
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
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
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = BrutalBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Brutalist search button
                Box(
                    modifier = Modifier
                        .background(if (isSearching) BrutalOrange else BrutalYellow)
                        .border(2.dp, BrutalBlack)
                        .clickable {
                            focusManager.clearFocus()
                            onSearchSubmit(searchQuery)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("search_submit_button")
                ) {
                    Text(
                        text = if (isSearching) "..." else "FIND",
                        color = BrutalBlack,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Search Suggestions chips if active
        if (searchSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(searchSuggestions) { suggestion ->
                    Box(
                        modifier = Modifier
                            .background(BrutalWhite)
                            .border(1.5.dp, BrutalBlack)
                            .clickable {
                                onSearchQueryChange(suggestion)
                                onSearchSubmit(suggestion)
                                focusManager.clearFocus()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "↗ $suggestion",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = BrutalBlack
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Tabs: [DISCOVER] [LIBRARY] [HISTORY]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(2.5.dp, BrutalBlack)
                        .background(if (isSelected) BrutalBlack else BrutalWhite)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 10.dp)
                        .testTag("tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[ ${tab.name} ]",
                        color = if (isSelected) BrutalYellow else BrutalBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
