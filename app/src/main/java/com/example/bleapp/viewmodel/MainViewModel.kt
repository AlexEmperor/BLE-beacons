package com.example.bleapp.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bleapp.data.Beacon
import com.example.bleapp.data.roomBeacons
import com.example.bleapp.util.distanceToRssi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class MainViewModel : ViewModel() {

    private var targetIndex = 0

    private val _userPos = MutableStateFlow(Offset(0.5f, 0.5f))
    val userPos: StateFlow<Offset> = _userPos

    private val _beacons = MutableStateFlow<List<Beacon>>(emptyList())
    val beacons: StateFlow<List<Beacon>> = _beacons

    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning

    // 📜 история позиций для траектории (понадобится для карты)
    private val _trail = MutableStateFlow<List<Offset>>(emptyList())
    val trail: StateFlow<List<Offset>> = _trail

    init {
        simulateMovement()
    }

    fun toggleScanning() {
        _isScanning.value = !_isScanning.value
    }

    private fun simulateMovement() {
        viewModelScope.launch {
            while (true) {
                val target = roomBeacons[targetIndex]
                val current = _userPos.value

                val dx = target.x - current.x
                val dy = target.y - current.y
                val step = 0.02f

                val newPos = Offset(
                    current.x + dx * step,
                    current.y + dy * step
                )

                _userPos.value = newPos
                updateBeacons(newPos)
                appendTrail(newPos)

                if (abs(dx) < 0.02f && abs(dy) < 0.02f) {
                    targetIndex = (targetIndex + 1) % roomBeacons.size
                }

                delay(200)
            }
        }
    }

    private fun appendTrail(pos: Offset) {
        val list = _trail.value.toMutableList()
        list.add(pos)
        if (list.size > 60) list.removeAt(0)
        _trail.value = list
    }

    private fun updateBeacons(user: Offset) {
        val updated = roomBeacons.mapIndexed { i, beaconPos ->
            val dx = user.x - beaconPos.x
            val dy = user.y - beaconPos.y
            // 🔑 множитель = "размер мира в метрах".
            // Маяки с координатами вне [0..1] окажутся в МЕТРАХ далеко от пользователя.
            // Например (-0.5, 0.5) при множителе 6 = -3 метра по X = маяк на 5+ метрах.
            val distance = sqrt((dx * dx + dy * dy).toDouble()) * 6
            val rssi = distanceToRssi(distance.coerceAtLeast(0.1))

            Beacon(
                id = "Beacon_${i + 1}",
                mac = "A1:B2:C3:D4:E5:0${(i + 1).toString().padStart(2, '0')}",
                rssi = rssi
            )
        }
        _beacons.value = updated
    }
}