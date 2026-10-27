package com.example.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AccentColorOption(
    val title: String,
    val primary: Color,
    val light: Color,
    val glow: Color
) {
    PURPLE("Vibrant Violet", Color(0xFFA855F7), Color(0xFFC084FC), Color(0x66A855F7)),
    LIME("Liquid Lime", Color(0xFF84CC16), Color(0xFFA3E635), Color(0x6684CC16)),
    CYAN("Electric Cyan", Color(0xFF06B6D4), Color(0xFF22D3EE), Color(0x6606B6D4)),
    PINK("Neon Pink", Color(0xFFF43F5E), Color(0xFFFB7185), Color(0x66F43F5E))
}

object ThemeManager {
    private const val PREFS_NAME = "brutal_beats_preferences"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private const val KEY_AMOLED_MODE = "is_amoled_mode"
    private const val KEY_ACCENT_COLOR = "accent_color"

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isAmoledMode = MutableStateFlow(false)
    val isAmoledMode: StateFlow<Boolean> = _isAmoledMode.asStateFlow()

    private val _accentColor = MutableStateFlow(AccentColorOption.PURPLE)
    val accentColor: StateFlow<AccentColorOption> = _accentColor.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, true)
        _isAmoledMode.value = prefs.getBoolean(KEY_AMOLED_MODE, false)
        val savedAccent = prefs.getString(KEY_ACCENT_COLOR, AccentColorOption.PURPLE.name)
        _accentColor.value = try {
            AccentColorOption.valueOf(savedAccent ?: AccentColorOption.PURPLE.name)
        } catch (_: Exception) {
            AccentColorOption.PURPLE
        }
    }

    fun toggleTheme(context: Context) {
        setDarkMode(context, !_isDarkMode.value)
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        _isDarkMode.value = isDark
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun setAmoledMode(context: Context, isAmoled: Boolean) {
        _isAmoledMode.value = isAmoled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AMOLED_MODE, isAmoled).apply()
    }

    fun setAccentColor(context: Context, option: AccentColorOption) {
        _accentColor.value = option
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACCENT_COLOR, option.name).apply()
    }
}
