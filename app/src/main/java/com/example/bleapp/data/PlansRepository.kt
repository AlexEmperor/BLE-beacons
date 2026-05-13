package com.example.bleapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Источник данных о планах/этажах/маяках. Стартует пустым — все consumers
 * получают пустые списки до первого успешного [refresh] из API.
 *
 * Стартовый flow приложения построен так, что юзер не попадает на Main
 * без успешного refresh (см. PlansLoadingScreen в UI), поэтому пустой
 * стартовый стейт никогда не доходит до экранов с этажами.
 */
object PlansRepository {

    private val _locations = MutableStateFlow<List<PlanLocation>>(emptyList())
    val locations: StateFlow<List<PlanLocation>> = _locations

    private val _seedsByFloor = MutableStateFlow<Map<String, List<BeaconSeed>>>(emptyMap())
    val seedsByFloor: StateFlow<Map<String, List<BeaconSeed>>> = _seedsByFloor

    suspend fun refresh(token: String) {
        val data = PlansApi.fetch(token)
        _locations.value = data.locations
        _seedsByFloor.value = data.seedsByFloor
    }

    fun reset() {
        _locations.value = emptyList()
        _seedsByFloor.value = emptyMap()
    }

    fun seedsFor(floorId: String): List<BeaconSeed> =
        _seedsByFloor.value[floorId].orEmpty()

    fun allSeeds(): List<BeaconSeed> =
        _seedsByFloor.value.values.flatten()

    val isLoaded: Boolean
        get() = _locations.value.isNotEmpty()
}
