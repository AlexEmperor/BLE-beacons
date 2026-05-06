package com.example.bleapp.data

data class Beacon(
    val id: String,           // отображаемое имя
    val mac: String,
    val rssi: Int,

    // 📡 iBeacon поля
    val beaconId: Long = 0,   // [0-3] uint32 — уникальный ID маяка
    val latitude: Float = 0f, // [8-11] float — широта установки
    val longitude: Float = 0f,// [12-15] float — долгота установки
    val major: Int = 0,       // номер этажа/зоны
    val minor: Int = 0,       // номер маяка внутри зоны
    val txPower: Int = -59    // мощность передатчика
)

data class BeaconPosition(
    val id: String,
    val x: Float,
    val y: Float
)