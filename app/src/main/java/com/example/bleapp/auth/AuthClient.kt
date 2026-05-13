package com.example.bleapp.auth

import com.example.bleapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class LoginResult(val token: String, val userId: String, val login: String)
data class MeResult(val userId: String, val login: String)

sealed class LoginError(message: String) : Exception(message) {
    object InvalidCredentials : LoginError("Неверный логин или пароль")
    object Network : LoginError("Нет связи с сервером")
    object TokenInvalid : LoginError("Сессия истекла, войди заново")
    object UserExists : LoginError("Такой логин уже занят")
    object WeakPassword : LoginError("Пароль должен быть не короче 6 символов")
    object BadLogin : LoginError("Логин должен быть от 3 до 64 символов")
    class Server(message: String) : LoginError(message)
}

object AuthClient {
    private const val TIMEOUT_MS = 10_000

    suspend fun login(login: String, password: String): LoginResult {
        val json = request("POST", "/login", JSONObject().apply {
            put("login", login)
            put("password", password)
        })
        return parseLoginResult(json)
    }

    suspend fun register(login: String, password: String): LoginResult {
        val json = request("POST", "/register", JSONObject().apply {
            put("login", login)
            put("password", password)
        })
        return parseLoginResult(json)
    }

    suspend fun changePassword(token: String, oldPassword: String, newPassword: String): LoginResult {
        val json = request(
            method = "POST",
            path = "/password",
            body = JSONObject().apply {
                put("oldPassword", oldPassword)
                put("newPassword", newPassword)
            },
            bearer = token
        )
        return parseLoginResult(json)
    }

    suspend fun me(token: String): MeResult {
        val json = request("GET", "/me", body = null, bearer = token)
        val user = json.optJSONObject("user") ?: throw LoginError.Server("Сервер не вернул user")
        return MeResult(user.optString("id"), user.optString("login"))
    }

    private fun parseLoginResult(json: JSONObject): LoginResult {
        val token = json.optString("token").takeIf { it.isNotEmpty() }
            ?: throw LoginError.Server("Сервер не вернул токен")
        val user = json.optJSONObject("user")
        return LoginResult(
            token = token,
            userId = user?.optString("id").orEmpty(),
            login = user?.optString("login").orEmpty()
        )
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        bearer: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val url = URL("${BuildConfig.AUTH_BASE_URL}$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
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
            val parsed = if (text.isNotEmpty()) runCatching { JSONObject(text) }.getOrNull() else null

            if (code in 200..299) {
                parsed ?: throw LoginError.Server("Пустой ответ сервера")
            } else {
                val errCode = parsed?.optString("error").orEmpty()
                val message = parsed?.optString("message")?.takeIf { it.isNotEmpty() }
                throw mapError(code, errCode, message)
            }
        } catch (e: LoginError) {
            throw e
        } catch (e: IOException) {
            throw LoginError.Network
        } finally {
            conn.disconnect()
        }
    }

    private fun mapError(httpCode: Int, errCode: String, message: String?): LoginError = when {
        httpCode == 401 && errCode == "token_invalid" -> LoginError.TokenInvalid
        httpCode == 401 -> LoginError.InvalidCredentials
        httpCode == 409 && errCode == "user_exists" -> LoginError.UserExists
        httpCode == 400 && errCode == "weak_password" -> LoginError.WeakPassword
        httpCode == 400 && errCode == "bad_login" -> LoginError.BadLogin
        else -> LoginError.Server(message ?: "HTTP $httpCode")
    }
}
