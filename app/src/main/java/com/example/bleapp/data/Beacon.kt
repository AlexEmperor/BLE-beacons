package com.example.bleapp.data

enum class BeaconKind {
    /** Наш расширенный iBeacon: GPS зашит в UUID. */
    OurCustom,
    /** Обычный iBeacon (Apple manufacturer data, 0x0215). */
    IBeacon,
    /** Google Eddystone (Service UUID 0xFEAA). */
    Eddystone,
    /** Любая BLE-реклама, которую не получилось распознать как маяк. */
    Unknown
}

data class Beacon(
    val id: String,           // отображаемое имя
    val mac: String,
    val rssi: Int,

    val kind: BeaconKind = BeaconKind.OurCustom,
    val uuid: String = "",    // UUID iBeacon / namespace Eddystone в hex

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
