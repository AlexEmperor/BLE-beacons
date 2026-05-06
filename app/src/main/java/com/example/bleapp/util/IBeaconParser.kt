package com.example.bleapp.util

import com.example.bleapp.data.Beacon
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Парсер iBeacon manufacturer data.
 *
 * Ожидаемый формат (всего 25 байт):
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
    if (data.size < 25) return null
    if (data[0] != 0x4C.toByte() || data[1] != 0x00.toByte()) return null
    if (data[2] != 0x02.toByte() || data[3] != 0x15.toByte()) return null

    // Beacon ID (uint32, big-endian — как обычно в BLE-payload)
    val beaconId = ByteBuffer.wrap(data, 4, 4)
        .order(ByteOrder.BIG_ENDIAN)
        .int.toLong() and 0xFFFFFFFFL

    // Latitude / Longitude (float, big-endian)
    val latitude = ByteBuffer.wrap(data, 12, 4)
        .order(ByteOrder.BIG_ENDIAN)
        .float

    val longitude = ByteBuffer.wrap(data, 16, 4)
        .order(ByteOrder.BIG_ENDIAN)
        .float

    // Major / Minor (uint16, big-endian)
    val major = ((data[20].toInt() and 0xFF) shl 8) or (data[21].toInt() and 0xFF)
    val minor = ((data[22].toInt() and 0xFF) shl 8) or (data[23].toInt() and 0xFF)

    val txPower = data[24].toInt() // signed

    return Beacon(
        id = "Beacon_$minor",
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