package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.BrutalBeatsApp
import com.example.ui.theme.BrutalBeatsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.theme.ThemeManager.init(this)
        enableEdgeToEdge()
        setContent {
            BrutalBeatsTheme {
                BrutalBeatsApp()
            }
        }
    }
}
