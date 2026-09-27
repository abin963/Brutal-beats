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
    primary = NyxPurple,
    onPrimary = Color.White,
    primaryContainer = NeonLimeContainer,
    onPrimaryContainer = NyxPurpleLight,
    secondary = NyxCyan,
    onSecondary = DarkBackground,
    tertiary = NyxPink,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF382F5A)
)

private val LightColorScheme = lightColorScheme(
    primary = NyxPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF581C87),
    secondary = NyxCyan,
    onSecondary = Color.White,
    tertiary = NyxPink,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFC7BEDF)
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
