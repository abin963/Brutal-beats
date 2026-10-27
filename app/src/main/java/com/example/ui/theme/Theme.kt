package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun BrutalBeatsTheme(
    darkTheme: Boolean = run {
        val darkState by ThemeManager.isDarkMode.collectAsState()
        darkState
    },
    content: @Composable () -> Unit
) {
    val isAmoled by ThemeManager.isAmoledMode.collectAsState()
    val accent by ThemeManager.accentColor.collectAsState()

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent.primary,
            onPrimary = Color.White,
            primaryContainer = accent.light.copy(alpha = 0.2f),
            onPrimaryContainer = accent.light,
            secondary = NyxCyan,
            onSecondary = if (isAmoled) Color.Black else DarkBackground,
            tertiary = NyxPink,
            background = if (isAmoled) Color.Black else DarkBackground,
            onBackground = DarkTextPrimary,
            surface = if (isAmoled) Color(0xFF07050E) else DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = if (isAmoled) Color(0xFF100C1F) else DarkSurfaceVariant,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkBorder,
            outlineVariant = Color(0xFF382F5A)
        )
    } else {
        lightColorScheme(
            primary = accent.primary,
            onPrimary = Color.White,
            primaryContainer = accent.light.copy(alpha = 0.15f),
            onPrimaryContainer = accent.primary,
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
    }

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
