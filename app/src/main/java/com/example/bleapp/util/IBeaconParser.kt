package com.example.bleapp.util

import com.example.bleapp.data.Beacon
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Парсер iBeacon manufacturer data.
 *
 * Ожидаемый формат полного manufacturer data (всего 25 байт):
 *  [0-1]   Apple Company ID (0x4C 0x00)
 *  [2-3]   iBeacon type (0x02 0x15)
 *  [4-19]  UUID 16 байт:
 *            [4-7]   Beacon ID (uint32)
 *            [8-11]  reserved
 *            [12-15] Latitude (float)
 *            [16-19] Longitude (float)
 *  [20-21] Major (uint16, big-endian)
 *  [22-23] Minor (uint16, big-endian)
 *  [24]    TX Power (int8)
 */
fun parseIBeacon(
    data: ByteArray,
    rssi: Int,
    mac: String = "00:00:00:00:00:00"
): Beacon? {
    val offset = when {
        data.size >= 25 &&
                data[0] == 0x4C.toByte() &&
                data[1] == 0x00.toByte() &&
                data[2] == 0x02.toByte() &&
                data[3] == 0x15.toByte() -> 4

        data.size >= 23 &&
                data[0] == 0x02.toByte() &&
                data[1] == 0x15.toByte() -> 2

        else -> return null
    }

    // Beacon ID (uint32, big-endian — как обычно в BLE-payload)
    val beaconId = ByteBuffer.wrap(data, offset, 4)
        .order(ByteOrder.BIG_ENDIAN)
        .int.toLong() and 0xFFFFFFFFL

    // Latitude / Longitude (float, big-endian)
    val latitude = ByteBuffer.wrap(data, offset + 8, 4)
        .order(ByteOrder.BIG_ENDIAN)
        .float

    val longitude = ByteBuffer.wrap(data, offset + 12, 4)
        .order(ByteOrder.BIG_ENDIAN)
        .float

    // Major / Minor (uint16, big-endian)
    val major = ((data[offset + 16].toInt() and 0xFF) shl 8) or (data[offset + 17].toInt() and 0xFF)
    val minor = ((data[offset + 18].toInt() and 0xFF) shl 8) or (data[offset + 19].toInt() and 0xFF)

    val txPower = data[offset + 20].toInt() // signed

    return Beacon(
        id = "Beacon $minor",
        mac = mac,
        rssi = rssi,
        beaconId = beaconId,
        latitude = latitude,
        longitude = longitude,
        major = major,
        minor = minor,
        txPower = txPower
    )
}
