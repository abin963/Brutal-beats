package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonLime,
    onPrimary = DarkBackground,
    primaryContainer = NeonLimeContainer,
    onPrimaryContainer = NeonLime,
    secondary = NeonCyan,
    onSecondary = DarkBackground,
    tertiary = NeonPink,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF383844)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00B347),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4FBEA),
    onPrimaryContainer = Color(0xFF006626),
    secondary = Color(0xFF007A8A),
    onSecondary = Color.White,
    tertiary = NeonPink,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFC0C3CE)
)

@Composable
fun BrutalBeatsTheme(
    darkTheme: Boolean = run {
        val darkState by ThemeManager.isDarkMode.collectAsState()
        darkState
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep alias for compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BrutalBeatsTheme(darkTheme = darkTheme, content = content)
}
