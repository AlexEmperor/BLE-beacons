package com.example.bleapp.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.bleapp.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionName: String,
    val releaseUrl: String,
    val apkUrl: String,
    val message: String
)

sealed interface UpdateInstallResult {
    data object Started : UpdateInstallResult
    data object InstallPermissionRequired : UpdateInstallResult
}

class UpdateManager(private val context: Context) {

    suspend fun checkForUpdate(): AppUpdate? {
        val json = httpGet(GITHUB_LATEST_RELEASE_URL)
        val release = JSONObject(json)
        val tag = release.optString("tag_name")
        val latestVersion = tag.removePrefix("v").trim()
        if (latestVersion.isBlank() || compareVersions(latestVersion, BuildConfig.VERSION_NAME) <= 0) {
            return null
        }

        val assets = release.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name") == APK_ASSET_NAME) {
                apkUrl = asset.optString("browser_download_url")
                break
            }
        }

        return AppUpdate(
            versionName = latestVersion,
            releaseUrl = release.optString("html_url"),
            apkUrl = apkUrl ?: "$GITHUB_LATEST_DOWNLOAD_BASE/$APK_ASSET_NAME",
            message = "Хорошие новости! Вышла новая версия приложения."
        )
    }

    suspend fun downloadAndInstall(update: AppUpdate): UpdateInstallResult {
        if (!context.packageManager.canRequestPackageInstalls()) {
            openInstallPermissionSettings()
            return UpdateInstallResult.InstallPermissionRequired
        }

        val apk = downloadApk(update.apkUrl)
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return UpdateInstallResult.Started
    }

    fun openRelease(update: AppUpdate) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun downloadApk(url: String): File {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outFile = File(updatesDir, APK_ASSET_NAME)
        val connection = openConnection(url)
        connection.inputStream.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return outFile
    }

    private fun httpGet(url: String): String {
        val connection = openConnection(url)
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun openConnection(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 60_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "BLEapp/${BuildConfig.VERSION_NAME}")
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val l = left.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
        val r = right.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
        val max = maxOf(l.size, r.size)
        for (i in 0 until max) {
            val lv = l.getOrElse(i) { 0 }
            val rv = r.getOrElse(i) { 0 }
            if (lv != rv) return lv.compareTo(rv)
        }
        return 0
    }

    companion object {
        private const val APK_ASSET_NAME = "app-release.apk"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val GITHUB_LATEST_RELEASE_URL =
            "https://api.github.com/repos/AlexEmperor/BLE-beacons/releases/latest"
        private const val GITHUB_LATEST_DOWNLOAD_BASE =
            "https://github.com/AlexEmperor/BLE-beacons/releases/latest/download"
    }
}
