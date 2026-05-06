package com.example.bleapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 🎨 Нейтральная тёмная палитра (как на макете)
val BgPrimary = Color(0xFF0A0A0F)    // фон экранов — почти чёрный
val BgSecondary = Color(0xFF161821)  // карточки, плашки
val BgTertiary = Color(0xFF0E0F14)   // TabRow, AppBar

private val DarkColors = darkColorScheme(
    background = BgPrimary,
    surface = BgSecondary,
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFF00FFA3),
    onBackground = Color.White
)

@Composable
fun BleAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}