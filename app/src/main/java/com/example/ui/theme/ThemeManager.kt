package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "brutal_beats_preferences"
    private const val KEY_DARK_MODE = "is_dark_mode"

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Default to dark mode for high-contrast neon brutal aesthetic
        _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun toggleTheme(context: Context) {
        setDarkMode(context, !_isDarkMode.value)
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        _isDarkMode.value = isDark
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }
}
