package com.example.bleapp.auth

import android.util.Base64
import org.json.JSONObject

object Jwt {
    fun expSeconds(token: String): Long? = runCatching {
        val parts = token.split('.')
        if (parts.size < 2) return null
        val payload = String(
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
            Charsets.UTF_8
        )
        JSONObject(payload).optLong("exp", -1L).takeIf { it > 0 }
    }.getOrNull()

    fun isAlive(token: String, skewSeconds: Long = 30): Boolean {
        val exp = expSeconds(token) ?: return false
        return exp - skewSeconds > System.currentTimeMillis() / 1000
    }

    fun role(token: String): String? = runCatching {
        val parts = token.split('.')
        if (parts.size < 2) return null
        val payload = String(
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
            Charsets.UTF_8
        )
        JSONObject(payload).optString("role").takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun isAdmin(token: String?): Boolean = token != null && role(token) == "admin"
}
