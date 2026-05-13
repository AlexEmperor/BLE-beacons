package com.example.bleapp.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = openEncrypted(appContext)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
        }.apply()

    var login: String?
        get() = prefs.getString(KEY_LOGIN, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_LOGIN) else putString(KEY_LOGIN, value)
        }.apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isTokenAlive(): Boolean = token?.let { Jwt.isAlive(it) } == true

    private companion object {
        const val PREFS = "auth_secure"
        const val KEY_TOKEN = "token"
        const val KEY_LOGIN = "login"

        fun openEncrypted(ctx: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return runCatching {
                EncryptedSharedPreferences.create(
                    ctx,
                    PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.getOrElse {
                // Если файл повреждён или сменился ключ (переустановка / clear data
                // на устройстве с keystore-проблемой) — пересоздаём.
                ctx.deleteSharedPreferences(PREFS)
                EncryptedSharedPreferences.create(
                    ctx,
                    PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }
        }
    }
}
