package com.example.bleapp.util

import kotlin.math.pow

// RSSI → расстояние в метрах
fun calculateDistance(rssi: Int, txPower: Int = -59): Double {
    return 10.0.pow((txPower - rssi) / (10 * 2.0))
}
