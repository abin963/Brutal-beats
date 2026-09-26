package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrutalYellow,
    onPrimary = BrutalDeepBlack,
    primaryContainer = BrutalGrayDark,
    onPrimaryContainer = BrutalYellow,
    secondary = BrutalCyan,
    onSecondary = BrutalDeepBlack,
    tertiary = BrutalOrange,
    background = BrutalBlack,
    onBackground = BrutalWhite,
    surface = BrutalGrayDark,
    onSurface = BrutalWhite,
    surfaceVariant = Color(0xFF282828),
    onSurfaceVariant = BrutalGrayLight,
    outline = BrutalWhite
)

private val LightColorScheme = lightColorScheme(
    primary = BrutalBlack,
    onPrimary = BrutalWhite,
    primaryContainer = BrutalYellow,
    onPrimaryContainer = BrutalBlack,
    secondary = BrutalOrange,
    onSecondary = BrutalWhite,
    tertiary = BrutalCyan,
    background = BrutalOffWhite,
    onBackground = BrutalBlack,
    surface = BrutalWhite,
    onSurface = BrutalBlack,
    surfaceVariant = BrutalGrayLight,
    onSurfaceVariant = BrutalBlack,
    outline = BrutalBlack
)

@Composable
fun BrutalBeatsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
