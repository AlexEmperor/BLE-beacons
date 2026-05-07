package com.example.bleapp.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.BeaconSeed
import com.example.bleapp.data.PlanFloor
import com.example.bleapp.data.PlanNavGraph
import com.example.bleapp.data.planLocations
import com.example.bleapp.data.planNavGraphs
import com.example.bleapp.data.seedsForFloor
import com.example.bleapp.util.distanceToRssi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val EARTH_R = 6_371_000.0       // м
private const val FLOOR_HEIGHT_M = 4.0        // вертикальная разница между этажами

class MainViewModel : ViewModel() {

    private val allFloorSeeds: List<Pair<PlanFloor, BeaconSeed>> =
        planLocations.flatMap { loc ->
            loc.floors.flatMap { f -> seedsForFloor(f.id).map { f to it } }
        }

    val allSeeds: List<BeaconSeed> = allFloorSeeds.map { it.second }

    private val defaultFloor: PlanFloor =
        planLocations.flatMap { it.floors }.firstOrNull { seedsForFloor(it.id).isNotEmpty() }
            ?: planLocations.first().floors.first()

    private val _selectedFloor = MutableStateFlow(defaultFloor)
    val selectedFloor: StateFlow<PlanFloor> = _selectedFloor

    private val _currentSeeds = MutableStateFlow(seedsForFloor(defaultFloor.id))
    val currentSeeds: StateFlow<List<BeaconSeed>> = _currentSeeds

    private val _userPos = MutableStateFlow(Offset(0.5f, 0.5f))
    val userPos: StateFlow<Offset> = _userPos

    private val _allBeacons = MutableStateFlow<List<Beacon>>(emptyList())
    val allBeacons: StateFlow<List<Beacon>> = _allBeacons

    private val _currentBeacons = MutableStateFlow<List<Beacon>>(emptyList())
    val currentBeacons: StateFlow<List<Beacon>> = _currentBeacons

    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _trail = MutableStateFlow<List<Offset>>(emptyList())
    val trail: StateFlow<List<Offset>> = _trail

    private var targetIndex = 0
    private val fallbackWaypoints = listOf(
        Offset(0.25f, 0.30f),
        Offset(0.55f, 0.35f),
        Offset(0.70f, 0.55f),
        Offset(0.40f, 0.65f),
        Offset(0.30f, 0.45f)
    )

    // --- Состояние навигации по графу плана (если для этажа есть граф) ---
    private var navGraph: PlanNavGraph? = null
    private var navPath: List<Int> = emptyList()
    private var navPathPos: Int = 0   // индекс следующего узла в navPath

    init {
        bindNavForFloor(defaultFloor)
        refreshBeacons(_userPos.value)
        simulateMovement()
    }

    fun toggleScanning() { _isScanning.value = !_isScanning.value }

    fun selectFloor(floor: PlanFloor) {
        if (floor.id == _selectedFloor.value.id) return
        _selectedFloor.value = floor
        _currentSeeds.value = seedsForFloor(floor.id)
        _trail.value = emptyList()
        bindNavForFloor(floor)
        refreshBeacons(_userPos.value)
    }

    private fun bindNavForFloor(floor: PlanFloor) {
        val g = planNavGraphs[floor.id]
        navGraph = g
        if (g != null) {
            // Стартуем у ближайшего узла графа, чтобы не "висеть" в стене.
            val start = g.nearestNode(_userPos.value)
            _userPos.value = g.nodes[start]
            navPath = listOf(start)
            navPathPos = 0
            pickNewGraphTarget()
        } else {
            navPath = emptyList()
            navPathPos = 0
        }
    }

    private fun pickNewGraphTarget() {
        val g = navGraph ?: return
        if (g.nodes.size < 2) return
        val current = if (navPath.isNotEmpty()) navPath.last() else g.nearestNode(_userPos.value)
        var target = current
        var attempts = 0
        while ((target == current || g.neighbors(target).isEmpty()) && attempts < 16) {
            target = Random.nextInt(g.nodes.size)
            attempts++
        }
        navPath = g.shortestPath(current, target)
        navPathPos = if (navPath.size > 1) 1 else 0
    }

    private fun simulateMovement() {
        viewModelScope.launch {
            while (true) {
                val newPos = navGraph?.let { stepAlongGraph(it) } ?: stepAlongWaypoints()
                _userPos.value = newPos
                appendTrail(newPos)
                if (_isScanning.value) refreshBeacons(newPos)
                delay(200)
            }
        }
    }

    private fun stepAlongWaypoints(): Offset {
        val target = fallbackWaypoints[targetIndex]
        val current = _userPos.value
        val dx = target.x - current.x
        val dy = target.y - current.y
        val step = 0.02f
        val newPos = Offset(
            (current.x + dx * step).coerceIn(0f, 1f),
            (current.y + dy * step).coerceIn(0f, 1f)
        )
        if (abs(dx) < 0.02f && abs(dy) < 0.02f) {
            targetIndex = (targetIndex + 1) % fallbackWaypoints.size
        }
        return newPos
    }

    /** Скорость движения по рёбрам графа в долях нормализованных координат за тик. */
    private val graphStep = 0.012f

    private fun stepAlongGraph(g: PlanNavGraph): Offset {
        if (navPath.size < 2) {
            pickNewGraphTarget()
            if (navPath.size < 2) return _userPos.value
        }
        val current = _userPos.value
        val target = g.nodes[navPath[navPathPos]]
        val dx = target.x - current.x
        val dy = target.y - current.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= graphStep) {
            // Достигли промежуточного узла — переходим к следующему или выбираем новую цель.
            if (navPathPos >= navPath.lastIndex) {
                pickNewGraphTarget()
                return target
            } else {
                navPathPos++
                return target
            }
        }
        val k = graphStep / dist
        return Offset(current.x + dx * k, current.y + dy * k)
    }

    /**
     * Текущая GPS-позиция пользователя:
     * центр выбранного этажа + смещение от userPos в метрах,
     * без учёта поворота плана (hor_deg) — для симуляции достаточно.
     */
    private fun userLatLon(user: Offset): Pair<Double, Double> {
        val f = _selectedFloor.value
        val dxM = (user.x - 0.5f) * f.widthMeters             // ось X — на восток
        val dyM = (0.5f - user.y) * f.heightMeters            // ось Y — на север (image-space инвертирован)
        val dLat = dyM / 111_320.0
        val dLon = dxM / (111_320.0 * cos(Math.toRadians(f.refLat)))
        return f.refLat + dLat to f.refLon + dLon
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val dφ = Math.toRadians(lat2 - lat1)
        val dλ = Math.toRadians(lon2 - lon1)
        val a = sin(dφ / 2).let { it * it } +
                cos(φ1) * cos(φ2) * sin(dλ / 2).let { it * it }
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_R * c
    }

    private fun refreshBeacons(user: Offset) {
        val (uLat, uLon) = userLatLon(user)
        val curLevel = _selectedFloor.value.level
        val all = allFloorSeeds.map { (_, seed) ->
            val horiz = haversine(uLat, uLon, seed.lat, seed.lon)
            val vert = (seed.level - curLevel) * FLOOR_HEIGHT_M
            val distance = sqrt(horiz * horiz + vert * vert).coerceAtLeast(0.1)
            val rssi = distanceToRssi(distance, seed.txPower) + Random.nextInt(-1, 2)
            Beacon(
                id = seed.id,
                mac = seed.mac,
                rssi = rssi,
                txPower = seed.txPower,
                beaconId = seed.beaconId,
                major = seed.major,
                minor = seed.minor,
                latitude = seed.lat.toFloat(),
                longitude = seed.lon.toFloat()
            )
        }
        _allBeacons.value = all
        val curMacs = _currentSeeds.value.map { it.mac }.toHashSet()
        _currentBeacons.value = all.filter { it.mac in curMacs }
    }

    private fun appendTrail(pos: Offset) {
        val list = _trail.value.toMutableList()
        list.add(pos)
        if (list.size > 60) list.removeAt(0)
        _trail.value = list
    }

    fun currentFloorMaxMeters(): Float =
        max(_selectedFloor.value.widthMeters, _selectedFloor.value.heightMeters)
}
