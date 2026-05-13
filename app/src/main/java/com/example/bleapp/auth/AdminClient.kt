package com.example.bleapp.auth

import com.example.bleapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AdminUser(val id: Long, val login: String, val role: String)
data class AdminLocation(val code: String, val name: String, val isPublic: Boolean)
data class AdminGrant(val userId: Long, val locationCode: String)

data class AdminState(
    val users: List<AdminUser>,
    val locations: List<AdminLocation>,
    val grants: List<AdminGrant>
)

object AdminClient {
    private const val TIMEOUT_MS = 15_000

    suspend fun state(token: String): AdminState =
        withContext(Dispatchers.IO) {
            val json = request("GET", "/admin/state", null, token)
            parseState(json)
        }

    suspend fun grant(token: String, userId: Long, locationCode: String) =
        withContext(Dispatchers.IO) {
            request("POST", "/admin/grant", JSONObject().apply {
                put("userId", userId)
                put("locationCode", locationCode)
            }, token)
            Unit
        }

    suspend fun revoke(token: String, userId: Long, locationCode: String) =
        withContext(Dispatchers.IO) {
            request("POST", "/admin/revoke", JSONObject().apply {
                put("userId", userId)
                put("locationCode", locationCode)
            }, token)
            Unit
        }

    private fun parseState(json: JSONObject): AdminState {
        val usersArr = json.optJSONArray("users") ?: JSONArray()
        val locsArr = json.optJSONArray("locations") ?: JSONArray()
        val grantsArr = json.optJSONArray("grants") ?: JSONArray()

        val users = (0 until usersArr.length()).map { i ->
            val u = usersArr.getJSONObject(i)
            AdminUser(
                id = u.optLong("id"),
                login = u.optString("login"),
                role = u.optString("role", "user")
            )
        }
        val locations = (0 until locsArr.length()).map { i ->
            val l = locsArr.getJSONObject(i)
            AdminLocation(
                code = l.optString("code"),
                name = l.optString("name"),
                isPublic = l.optBoolean("isPublic", false)
            )
        }
        val grants = (0 until grantsArr.length()).map { i ->
            val g = grantsArr.getJSONObject(i)
            AdminGrant(
                userId = g.optLong("userId"),
                locationCode = g.optString("locationCode")
            )
        }
        return AdminState(users, locations, grants)
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject?,
        bearer: String
    ): JSONObject {
        val url = URL("${BuildConfig.AUTH_BASE_URL}$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $bearer")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException("HTTP $code: $text")
            }
            return if (text.isEmpty()) JSONObject() else JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
}
