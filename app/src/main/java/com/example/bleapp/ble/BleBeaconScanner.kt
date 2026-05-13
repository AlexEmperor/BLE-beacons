package com.example.bleapp.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.BeaconKind
import com.example.bleapp.util.parseEddystone
import com.example.bleapp.util.parseIBeacon

class BleBeaconScanner(
    private val context: Context,
    private val onBeaconsChanged: (List<Beacon>) -> Unit
) {
    private val beaconsByMac = linkedMapOf<String, Beacon>()
    private var started = false

    private val bluetoothAdapter by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val scanner
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleResult)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (started || !hasRequiredPermissions()) return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()
        scanner?.startScan(emptyList(), settings, callback)
        started = true
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!started) return
        scanner?.stopScan(callback)
        started = false
    }

    fun clear() {
        beaconsByMac.clear()
        onBeaconsChanged(emptyList())
    }

    fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun handleResult(result: ScanResult) {
        val mac = result.device.address ?: return
        val rssi = result.rssi
        val scanRecord = result.scanRecord

        val parsed = scanRecord?.let { record ->
            // 1) Apple manufacturer data → iBeacon (наш или стандартный).
            record.getManufacturerSpecificData(APPLE_COMPANY_ID)?.let { data ->
                parseIBeacon(data = data, rssi = rssi, mac = mac)
            }
            // 2) Eddystone (Service UUID 0xFEAA).
                ?: record.serviceData?.get(EDDYSTONE_SERVICE_UUID)?.let { data ->
                    parseEddystone(data = data, rssi = rssi, mac = mac)
                }
        }

        val beacon = parsed ?: run {
            val name = scanRecord?.deviceName?.takeIf { it.isNotBlank() } ?: "BLE-устройство"
            Beacon(
                id = name,
                mac = mac,
                rssi = rssi,
                kind = BeaconKind.Unknown
            )
        }

        beaconsByMac[beacon.mac] = beacon
        onBeaconsChanged(beaconsByMac.values.sortedByDescending { it.rssi })
    }

    companion object {
        private const val APPLE_COMPANY_ID = 0x004C
        private val EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
    }
}
