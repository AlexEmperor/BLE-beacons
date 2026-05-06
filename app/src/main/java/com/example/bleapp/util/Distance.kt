package com.example.bleapp.util

import kotlin.math.log10
import kotlin.math.pow

// RSSI → расстояние в метрах
fun calculateDistance(rssi: Int, txPower: Int = -59): Double {
    return 10.0.pow((txPower - rssi) / (10 * 2.0))
}

// Расстояние → RSSI (для симуляции)
fun distanceToRssi(distance: Double, txPower: Int = -59, n: Double = 2.0): Int {
    return (txPower - 10 * n * log10(distance)).toInt()
}

// RSSI → радиус окружности на радаре
fun rssiToRadius(rssi: Int): Float {
    val norm = ((rssi + 100) / 60f).coerceIn(0f, 1f)
    val inverted = 1f - norm
    return 80f + inverted * 220f
}
