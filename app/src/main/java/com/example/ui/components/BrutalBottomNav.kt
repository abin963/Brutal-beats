package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NyxPurple
import com.example.ui.theme.NyxPurpleLight
import com.example.ui.viewmodel.MainTab

data class NavItem(
    val tab: MainTab,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun BrutalBottomNav(
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            NavItem(MainTab.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavItem(MainTab.EXPLORE, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
            NavItem(MainTab.LIBRARY, "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
            NavItem(MainTab.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                NyxPurple.copy(alpha = 0.28f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = activeTab == item.tab
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = tween(200),
                        label = "nav_scale"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) NyxPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        animationSpec = tween(200),
                        label = "nav_icon_tint"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) NyxPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        animationSpec = tween(200),
                        label = "nav_text_tint"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 28.dp),
                                onClick = { onTabSelected(item.tab) }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("nav_tab_${item.title.lowercase()}")
                    ) {
                        // Glowing top indicator when selected
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(width = 16.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(NyxPurple)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = item.title,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }
    }
}
