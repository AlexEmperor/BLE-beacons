package com.example.bleapp.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleapp.ble.BleBeaconScanner
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.BeaconKind
import com.example.bleapp.data.BeaconSeed
import com.example.bleapp.data.PlanFloor
import com.example.bleapp.data.PlansRepository
import com.example.bleapp.util.calculateDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos

enum class ScannerFilter { OurFormat, Others }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Sentinel-этаж до первого успешного refresh PlansRepository.
     * Пользователь его никогда не видит: переход на Main блокируется
     * экраном PlansLoadingScreen, который ждёт непустого репо.
     */
    private val pendingFloor: PlanFloor = PlanFloor(
        id = "__pending__",
        name = "",
        assetPath = "",
        isSvg = false,
        widthMeters = 1f,
        heightMeters = 1f
    )
    private val allSavedSeeds: List<BeaconSeed>
        get() = PlansRepository.allSeeds()

    private val _selectedFloor = MutableStateFlow(
        PlansRepository.locations.value.firstOrNull()?.floors?.firstOrNull() ?: pendingFloor
    )
    val selectedFloor: StateFlow<PlanFloor> = _selectedFloor

    private val _userPos = MutableStateFlow(Offset(0.5f, 0.5f))
    val userPos: StateFlow<Offset> = _userPos

    private val _liveBeacons = MutableStateFlow<List<Beacon>>(emptyList())

    private val _allBeacons = MutableStateFlow<List<Beacon>>(emptyList())
    val allBeacons: StateFlow<List<Beacon>> = _allBeacons

    private val _currentBeacons = MutableStateFlow<List<Beacon>>(emptyList())
    val currentBeacons: StateFlow<List<Beacon>> = _currentBeacons

    private val _currentSeeds = MutableStateFlow<List<BeaconSeed>>(emptyList())
    val currentSeeds: StateFlow<List<BeaconSeed>> = _currentSeeds

    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _showSavedBeacons = MutableStateFlow(false)
    val showSavedBeacons: StateFlow<Boolean> = _showSavedBeacons

    private val _scannerFilter = MutableStateFlow(ScannerFilter.OurFormat)
    val scannerFilter: StateFlow<ScannerFilter> = _scannerFilter

    private val _scanList = MutableStateFlow<List<Beacon>>(emptyList())
    /** Маяки для вкладки «Сканер» с применённым фильтром. */
    val scanList: StateFlow<List<Beacon>> = _scanList

    private val _geoBeacons = MutableStateFlow<List<Beacon>>(emptyList())
    /** Все маяки (live + saved when toggle on) с осмысленными GPS-координатами. */
    val geoBeacons: StateFlow<List<Beacon>> = _geoBeacons

    val allSeeds: List<BeaconSeed>
        get() = if (_showSavedBeacons.value) {
            mergeSeeds(allSavedSeeds, liveSeedsForAllFloors(_liveBeacons.value))
        } else {
            liveSeedsForAllFloors(_liveBeacons.value)
        }

    private val scanner = BleBeaconScanner(application.applicationContext) { beacons ->
        _liveBeacons.value = beacons
        updateCurrentFloorData()
    }

    init {
        scanner.start()
        // Когда набор локаций меняется (логин/смена юзера/logout), убеждаемся
        // что выбранный этаж всё ещё существует. Иначе берём первый доступный.
        viewModelScope.launch {
            PlansRepository.locations.collect { locations ->
                val current = _selectedFloor.value
                val stillVisible = locations.any { loc -> loc.floors.any { it.id == current.id } }
                if (!stillVisible) {
                    locations.firstOrNull()?.floors?.firstOrNull()?.let { fallback ->
                        _selectedFloor.value = fallback
                    }
                }
                updateCurrentFloorData()
            }
        }
    }

    fun toggleScanning() {
        val next = !_isScanning.value
        _isScanning.value = next
        if (next) {
            scanner.start()
        } else {
            scanner.stop()
            scanner.clear()
        }
        updateCurrentFloorData()
    }

    fun toggleSavedBeacons() {
        _showSavedBeacons.value = !_showSavedBeacons.value
        updateCurrentFloorData()
    }

    fun setScannerFilter(filter: ScannerFilter) {
        if (filter == _scannerFilter.value) return
        _scannerFilter.value = filter
        updateCurrentFloorData()
    }

    fun selectFloor(floor: PlanFloor) {
        if (floor.id == _selectedFloor.value.id) return
        _selectedFloor.value = floor
        updateCurrentFloorData()
    }

    private fun updateCurrentFloorData() {
        val floor = _selectedFloor.value

        val seeds: List<BeaconSeed>
        val currentBeacons: List<Beacon>
        if (floor.isWorldMap) {
            // Город-режим: сохранённые маяки показываем только если включён тоггл
            // «Отобразить существующие» в Сканере.
            if (!_showSavedBeacons.value) {
                seeds = emptyList()
                currentBeacons = emptyList()
            } else {
                val saved = allSavedSeeds
                val liveByMac = _liveBeacons.value.associateBy { it.mac }
                seeds = saved.mapNotNull { s ->
                    val pos = latLonToFloorOffset(floor, s.lat, s.lon) ?: return@mapNotNull null
                    s.copy(x = pos.x, y = pos.y)
                }
                val seedMacs = seeds.map { it.mac }.toHashSet()
                currentBeacons = saved
                    .filter { it.mac in seedMacs }
                    .map { s ->
                        val base = savedBeaconFromSeed(s)
                        liveByMac[s.mac]?.let { base.copy(rssi = it.rssi) } ?: base
                    }
                    .sortedByDescending { it.rssi }
            }
        } else {
            val liveWithPositions = _liveBeacons.value.filter { it.hasCoordinates() }
            val liveSeeds = liveSeedsForFloor(floor, liveWithPositions)
            val savedSeeds = if (_showSavedBeacons.value) PlansRepository.seedsFor(floor.id) else emptyList()
            seeds = mergeSeeds(savedSeeds, liveSeeds)
            val seedMacs = seeds.map { it.mac }.toHashSet()
            currentBeacons = mergeBeacons(
                saved = savedSeeds.map(::savedBeaconFromSeed),
                live = liveWithPositions.filter { it.mac in seedMacs }
            )
        }

        _currentSeeds.value = seeds
        _currentBeacons.value = currentBeacons
        val display = allDisplayBeacons()
        _allBeacons.value = display
        _scanList.value = applyScannerFilter(display)
        _geoBeacons.value = currentBeacons
        estimateUserPosition(currentBeacons, seeds)?.let { _userPos.value = it }
    }

    private fun applyScannerFilter(beacons: List<Beacon>): List<Beacon> =
        when (_scannerFilter.value) {
            ScannerFilter.OurFormat -> beacons.filter { it.kind == BeaconKind.OurCustom }
            ScannerFilter.Others -> beacons.filter { it.kind != BeaconKind.OurCustom }
        }

    private fun allDisplayBeacons(): List<Beacon> {
        val saved = if (_showSavedBeacons.value) {
            allSavedSeeds.map(::savedBeaconFromSeed)
        } else {
            emptyList()
        }
        return mergeBeacons(saved = saved, live = _liveBeacons.value)
    }

    private fun liveSeedsForAllFloors(beacons: List<Beacon>): List<BeaconSeed> {
        return PlansRepository.locations.value
            .asSequence()
            .flatMap { it.floors.asSequence() }
            .flatMap { floor -> liveSeedsForFloor(floor, beacons).asSequence() }
            .distinctBy { it.mac }
            .toList()
    }

    private fun mergeSeeds(saved: List<BeaconSeed>, live: List<BeaconSeed>): List<BeaconSeed> {
        val byMac = linkedMapOf<String, BeaconSeed>()
        saved.forEach { byMac[it.mac] = it }
        live.forEach { byMac.putIfAbsent(it.mac, it) }
        return byMac.values.toList()
    }

    private fun mergeBeacons(saved: List<Beacon>, live: List<Beacon>): List<Beacon> {
        val byMac = linkedMapOf<String, Beacon>()
        saved.forEach { byMac[it.mac] = it }
        live.forEach { byMac[it.mac] = it }
        return byMac.values.sortedByDescending { it.rssi }
    }

    private fun savedBeaconFromSeed(seed: BeaconSeed): Beacon =
        Beacon(
            id = seed.id,
            mac = seed.mac,
            rssi = -90,
            beaconId = seed.beaconId,
            latitude = seed.lat.toFloat(),
            longitude = seed.lon.toFloat(),
            major = seed.major,
            minor = seed.minor,
            txPower = seed.txPower
        )

    private fun liveSeedsForFloor(floor: PlanFloor, beacons: List<Beacon>): List<BeaconSeed> {
        return beacons.mapNotNull { beacon ->
            val pos = latLonToFloorOffset(floor, beacon.latitude.toDouble(), beacon.longitude.toDouble())
                ?: return@mapNotNull null

            BeaconSeed(
                id = beacon.id,
                mac = beacon.mac,
                x = pos.x,
                y = pos.y,
                txPower = beacon.txPower,
                lat = beacon.latitude.toDouble(),
                lon = beacon.longitude.toDouble(),
                level = beacon.major,
                major = beacon.major,
                minor = beacon.minor,
                beaconId = beacon.beaconId
            )
        }
    }

    private fun latLonToFloorOffset(floor: PlanFloor, lat: Double, lon: Double): Offset? {
        if (lat == 0.0 && lon == 0.0) return null

        val metersPerDegree = 111_320.0
        val dxM = (lon - floor.refLon) * metersPerDegree * cos(Math.toRadians(floor.refLat))
        val dyM = (floor.refLat - lat) * metersPerDegree
        val x = (0.5 + dxM / floor.widthMeters).toFloat()
        val y = (0.5 + dyM / floor.heightMeters).toFloat()

        if (x !in -0.15f..1.15f || y !in -0.15f..1.15f) return null
        return Offset(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
    }

    private fun estimateUserPosition(beacons: List<Beacon>, seeds: List<BeaconSeed>): Offset? {
        if (beacons.isEmpty() || seeds.isEmpty()) return null

        val seedsByMac = seeds.associateBy { it.mac }
        var sumW = 0.0
        var sumX = 0.0
        var sumY = 0.0

        beacons.forEach { beacon ->
            val seed = seedsByMac[beacon.mac] ?: return@forEach
            val distance = calculateDistance(beacon.rssi, beacon.txPower).coerceAtLeast(0.2)
            val weight = 1.0 / (distance * distance)
            sumW += weight
            sumX += seed.x * weight
            sumY += seed.y * weight
        }

        if (sumW == 0.0) return null
        return Offset(
            x = (sumX / sumW).toFloat().coerceIn(0f, 1f),
            y = (sumY / sumW).toFloat().coerceIn(0f, 1f)
        )
    }

    private fun Beacon.hasCoordinates(): Boolean =
        !(latitude == 0f && longitude == 0f)

    fun currentFloorMaxMeters(): Float =
        maxOf(_selectedFloor.value.widthMeters, _selectedFloor.value.heightMeters)

    override fun onCleared() {
        scanner.stop()
        super.onCleared()
    }
}
