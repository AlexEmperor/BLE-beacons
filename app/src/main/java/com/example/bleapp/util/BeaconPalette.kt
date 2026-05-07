package com.example.bleapp.util

import androidx.compose.ui.graphics.Color

fun beaconColor(index: Int, total: Int): Color {
    val n = if (total <= 0) 1 else total
    val hue = (360f * index / n) % 360f
    return Color.hsv(hue, 0.65f, 0.95f)
}
