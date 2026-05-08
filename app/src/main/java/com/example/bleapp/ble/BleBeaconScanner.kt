package com.example.bleapp.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.bleapp.data.Beacon
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
        scanner?.startScan(callback)
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
        val manufacturerData = result.scanRecord?.getManufacturerSpecificData(APPLE_COMPANY_ID) ?: return
        val beacon = parseIBeacon(
            data = manufacturerData,
            rssi = result.rssi,
            mac = result.device.address
        ) ?: return

        beaconsByMac[beacon.mac] = beacon
        onBeaconsChanged(beaconsByMac.values.sortedByDescending { it.rssi })
    }

    companion object {
        private const val APPLE_COMPANY_ID = 0x004C
    }
}
