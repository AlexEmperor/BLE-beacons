package com.example.bleapp.data

// 📡 Внутреннее представление маяка для рендера/симуляции.
// x, y — нормализованные [0..1] в "image-space" (y=0 — верх плана).
// lat, lon, level — для расчёта реальной дистанции даже между этажами/зданиями.
data class BeaconSeed(
    val id: String,
    val mac: String,
    val x: Float,
    val y: Float,
    val txPower: Int = -65,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val level: Int = 1,
    val major: Int = 0,
    val minor: Int = 0,
    val beaconId: Long = 0
)

/** Маяки выбранного этажа в image-space (с инверсией Y от Navigine). */
fun seedsForFloor(floorId: String): List<BeaconSeed> =
    realBeaconsByFloor[floorId].orEmpty().map { rb ->
        BeaconSeed(
            id = rb.id,
            mac = rb.mac,
            x = rb.kx,
            y = 1f - rb.ky,
            txPower = rb.txPower,
            lat = rb.lat,
            lon = rb.lon,
            level = rb.level,
            major = rb.major,
            minor = rb.minor,
            beaconId = rb.beaconId
        )
    }
