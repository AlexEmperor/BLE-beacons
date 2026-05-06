package com.example.bleapp.util

import androidx.compose.ui.graphics.Color

// Цвет по качеству сигнала RSSI (как в легенде на макете)
fun rssiColor(rssi: Int): Color = when {
    rssi >= -60 -> Color(0xFF00FFA3)   // отлично
    rssi >= -70 -> Color(0xFF7CFF6B)   // хорошо
    rssi >= -80 -> Color(0xFFFFC107)   // удовл.
    rssi >= -100 -> Color(0xFFFF5252)  // слабо
    else -> Color(0xFF888888)          // нет сигнала
}
