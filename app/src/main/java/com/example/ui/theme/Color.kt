package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// NYX Violet / Purple Accent Palette
val NyxPurple = Color(0xFFA855F7)          // Primary Vibrant Purple Accent
val NyxPurpleLight = Color(0xFFC084FC)     // Light Purple Accent
val NyxPurpleDark = Color(0xFF7E22CE)      // Deep Purple Accent
val NyxPurpleGlow = Color(0x66A855F7)      // Purple Atmospheric Glow
val NyxPink = Color(0xFFF43F5E)            // Neon Pink / Favorite Accent
val NyxCyan = Color(0xFF06B6D4)            // Cyan indicator for high-res / JioSaavn
val NyxSurfaceGlass = Color(0xDD120E24)    // Translucent glass surface
val NyxSurfaceBorder = Color(0x40A855F7)   // Subtle glowing purple border

// Liquid Glass Design System Tokens
val GlassSurface = Color(0x1AFFFFFF)         // 10% translucent white for glass
val GlassSurfaceDark = Color(0xB30F0B1E)     // Dark tinted liquid glass
val GlassSurfaceElevated = Color(0xD917122C) // Elevated glass card
val GlassBorder = Color(0x2EFFFFFF)          // Crisp specular glass border
val GlassBorderSubtle = Color(0x1FFFFFFF)    // Subtle glass border
val GlassHighlight = Color(0x4DFFFFFF)       // Specular top reflection
val GlassGlow = Color(0x33A855F7)            // Ambient purple glow

// Backward-compatibility aliases mapped to NYX palette
val NeonLime = NyxPurple
val NeonLimeHover = NyxPurpleLight
val NeonLimePressed = NyxPurpleDark
val NeonLimeContainer = Color(0xFF291345)
val NeonLimeGlow = NyxPurpleGlow

val NeonPink = NyxPink
val NeonCyan = NyxCyan
val NeonOrange = Color(0xFFFF5500)
val BrutalYellow = Color(0xFFEAB308)

// Base NYX Dark Scheme (Deep black with subtle purple undertone)
val DarkBackground = Color(0xFF08070F)
val DarkSurface = Color(0xFF131022)
val DarkSurfaceVariant = Color(0xFF1C1733)
val DarkBorder = Color(0xFF2C2448)
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFB4B1C9)
val DarkTextMuted = Color(0xFF767290)

// Base NYX Light Scheme
val LightBackground = Color(0xFFF7F6FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDE9F8)
val LightBorder = Color(0xFFDCD6EF)
val LightTextPrimary = Color(0xFF120E25)
val LightTextSecondary = Color(0xFF5E5878)
val LightTextMuted = Color(0xFF8B85A4)

// Legacy compatibility aliases
val BrutalBlack = DarkBackground
val BrutalDeepBlack = Color(0xFF000000)
val BrutalWhite = Color(0xFFFFFFFF)
val BrutalOffWhite = LightBackground
val BrutalOrange = NeonOrange
val BrutalCyan = NeonCyan
val BrutalPink = NeonPink
val BrutalGrayLight = Color(0xFFE8E8EE)
val BrutalGrayMedium = Color(0xFFB0B0BE)
val BrutalGrayDark = Color(0xFF1A162B)
val BrutalBorder = DarkBorder
val BrutalShadow = Color(0xFF000000)
