package com.example.bleapp.data

import com.example.bleapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class PlansData(
    val locations: List<PlanLocation>,
    val seedsByFloor: Map<String, List<BeaconSeed>>
)

object PlansApi {
    private const val TIMEOUT_MS = 15_000

    suspend fun fetch(token: String): PlansData = withContext(Dispatchers.IO) {
        val url = URL("${BuildConfig.AUTH_BASE_URL}/plans")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("HTTP $code: $text")
            parse(JSONObject(text))
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(root: JSONObject): PlansData {
        val locationsArr = root.optJSONArray("locations") ?: JSONArray()
        val locations = mutableListOf<PlanLocation>()
        val seeds = mutableMapOf<String, List<BeaconSeed>>()

        for (i in 0 until locationsArr.length()) {
            val locJson = locationsArr.getJSONObject(i)
            val locCode = locJson.optString("code")
            val locName = locJson.optString("name")
            val floorsArr = locJson.optJSONArray("floors") ?: JSONArray()

            val floors = mutableListOf<PlanFloor>()
            for (j in 0 until floorsArr.length()) {
                val f = floorsArr.getJSONObject(j)
                val floorCode = f.optString("code")
                val level = f.optInt("level", 1)

                floors.add(
                    PlanFloor(
                        id = floorCode,
                        name = f.optString("name"),
                        assetPath = f.optString("assetPath"),
                        isSvg = f.optBoolean("isSvg"),
                        widthMeters = f.optDouble("widthMeters", 0.0).toFloat(),
                        heightMeters = f.optDouble("heightMeters", 0.0).toFloat(),
                        level = level,
                        refLat = f.optDouble("refLat", 0.0),
                        refLon = f.optDouble("refLon", 0.0),
                        bearingDeg = f.optDouble("bearingDeg", 0.0).toFloat(),
                        isWorldMap = f.optBoolean("isWorldMap")
                    )
                )

                val beaconsArr = f.optJSONArray("beacons") ?: JSONArray()
                val floorSeeds = mutableListOf<BeaconSeed>()
                for (k in 0 until beaconsArr.length()) {
                    val b = beaconsArr.getJSONObject(k)
                    floorSeeds.add(
                        BeaconSeed(
                            id = b.optString("id"),
                            mac = b.optString("mac"),
                            x = b.optDouble("xNorm", 0.0).toFloat(),
                            y = b.optDouble("yNorm", 0.0).toFloat(),
                            txPower = b.optInt("txPower", -65),
                            lat = b.optDouble("latitude", 0.0),
                            lon = b.optDouble("longitude", 0.0),
                            level = level,
                            major = b.optInt("major", 0),
                            minor = b.optInt("minor", 0),
                            beaconId = b.optLong("beaconId", 0L)
                        )
                    )
                }
                seeds[floorCode] = floorSeeds
            }
            locations.add(PlanLocation(id = locCode, name = locName, floors = floors))
        }
        return PlansData(locations, seeds)
    }
}
