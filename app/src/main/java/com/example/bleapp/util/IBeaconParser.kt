package com.example.bleapp.util

import com.example.bleapp.data.Beacon
import com.example.bleapp.data.BeaconKind
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Парсер iBeacon manufacturer data (0x02 0x15 …).
 *
 * Внутри одного 25-байтного envelope-а может прилететь:
 *  - наш расширенный формат (UUID = beaconId | reserved | latitude | longitude),
 *  - обычный iBeacon со случайным UUID.
 *
 * Различаем эвристикой: если из UUID получается осмысленная пара
 * (latitude ∈ [-90, 90], longitude ∈ [-180, 180], не одновременно нули)
 * — считаем «наш». Иначе — стандартный iBeacon.
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

    val uuidHex = data.copyOfRange(offset, offset + 16)
        .joinToString("") { "%02x".format(it) }

    val beaconId = ByteBuffer.wrap(data, offset, 4)
        .order(ByteOrder.BIG_ENDIAN)
        .int.toLong() and 0xFFFFFFFFL

    val latitude = ByteBuffer.wrap(data, offset + 8, 4)
        .order(ByteOrder.BIG_ENDIAN)
        .float

    val longitude = ByteBuffer.wrap(data, offset + 12, 4)
        .order(ByteOrder.BIG_ENDIAN)
        .float

    val major = ((data[offset + 16].toInt() and 0xFF) shl 8) or (data[offset + 17].toInt() and 0xFF)
    val minor = ((data[offset + 18].toInt() and 0xFF) shl 8) or (data[offset + 19].toInt() and 0xFF)
    val txPower = data[offset + 20].toInt()

    val isOurFormat = latitude.isFinite() && longitude.isFinite() &&
            latitude in -90f..90f && longitude in -180f..180f &&
            !(latitude == 0f && longitude == 0f)

    return if (isOurFormat) {
        Beacon(
            id = "Beacon $minor",
            mac = mac,
            rssi = rssi,
            kind = BeaconKind.OurCustom,
            uuid = uuidHex,
            beaconId = beaconId,
            latitude = latitude,
            longitude = longitude,
            major = major,
            minor = minor,
            txPower = txPower
        )
    } else {
        Beacon(
            id = "iBeacon $major:$minor",
            mac = mac,
            rssi = rssi,
            kind = BeaconKind.IBeacon,
            uuid = uuidHex,
            major = major,
            minor = minor,
            txPower = txPower
        )
    }
}

/**
 * Минимальный парсер Eddystone (Service UUID 0xFEAA).
 * Поддерживаем UID-фрейм (тип 0x00) и URL-фрейм (тип 0x10) — на остальные просто
 * вешаем имя «Eddystone».
 */
fun parseEddystone(
    data: ByteArray,
    rssi: Int,
    mac: String
): Beacon? {
    if (data.isEmpty()) return null
    val frameType = data[0].toInt() and 0xFF
    val txPower = if (data.size >= 2) data[1].toInt() else -59

    return when (frameType) {
        0x00 -> {
            // UID: [0]=type [1]=txPower [2-11]=namespace (10 байт) [12-17]=instance (6 байт)
            if (data.size < 18) return null
            val namespace = data.copyOfRange(2, 12).joinToString("") { "%02x".format(it) }
            val instance = data.copyOfRange(12, 18).joinToString("") { "%02x".format(it) }
            Beacon(
                id = "Eddystone-UID ${instance.takeLast(6)}",
                mac = mac,
                rssi = rssi,
                kind = BeaconKind.Eddystone,
                uuid = namespace,
                txPower = txPower
            )
        }
        0x10 -> {
            val url = decodeEddystoneUrl(data) ?: "Eddystone-URL"
            Beacon(
                id = url.take(32),
                mac = mac,
                rssi = rssi,
                kind = BeaconKind.Eddystone,
                txPower = txPower
            )
        }
        else -> Beacon(
            id = "Eddystone (frame 0x%02X)".format(frameType),
            mac = mac,
            rssi = rssi,
            kind = BeaconKind.Eddystone,
            txPower = txPower
        )
    }
}

private fun decodeEddystoneUrl(data: ByteArray): String? {
    if (data.size < 3) return null
    val schemePrefix = when (data[2].toInt() and 0xFF) {
        0x00 -> "http://www."
        0x01 -> "https://www."
        0x02 -> "http://"
        0x03 -> "https://"
        else -> return null
    }
    val tail = StringBuilder()
    for (i in 3 until data.size) {
        val b = data[i].toInt() and 0xFF
        tail.append(when (b) {
            0x00 -> ".com/"
            0x01 -> ".org/"
            0x02 -> ".edu/"
            0x03 -> ".net/"
            0x04 -> ".info/"
            0x05 -> ".biz/"
            0x06 -> ".gov/"
            0x07 -> ".com"
            0x08 -> ".org"
            0x09 -> ".edu"
            0x0A -> ".net"
            0x0B -> ".info"
            0x0C -> ".biz"
            0x0D -> ".gov"
            else -> b.toChar()
        })
    }
    return schemePrefix + tail
}
