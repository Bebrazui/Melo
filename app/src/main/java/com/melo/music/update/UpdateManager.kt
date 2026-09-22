package com.melo.music.update

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.melo.music.BuildConfig
import com.melo.music.net.MeloNet
import com.melo.music.util.MeloLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val changelog: String,
    val sha256: String = "",
)

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val PREFS_NAME = "melo_update_prefs"
    private const val KEY_IGNORED_VERSION_CODE = "ignored_version_code"
    private const val KEY_IGNORED_VERSION_NAME = "ignored_version_name"

    private val VERSION_URLS = listOf(
        "https://raw.githubusercontent.com/Bebrazui/Melo/master/version.json",
        "https://cdn.jsdelivr.net/gh/Bebrazui/Melo@master/version.json",
        "https://raw.githubusercontent.com/Bebrazui/Melo-Releases/main/version.json",
        "https://raw.githubusercontent.com/Bebrazui/Melo-Releases/master/version.json",
    )

    private const val FALLBACK_APK_URL =
        "https://github.com/Bebrazui/Melo-Releases/releases/download/ALPHA/app-release.apk"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .dns(MeloNet.dns)
            .proxySelector(MeloNet.byedpiSelector)
            .followRedirects(true)
            .build()
    }

    var availableUpdate by mutableStateOf<UpdateInfo?>(null)
    var isChecking by mutableStateOf(false)
    var lastCheckStatus by mutableStateOf<String?>(null)

    var isDownloading by mutableStateOf(false)
    var downloadProgress by mutableFloatStateOf(0f)
    var downloadStatusText by mutableStateOf<String?>(null)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isVersionIgnored(context: Context, versionCode: Int, versionName: String): Boolean {
        val prefs = getPrefs(context)
        val ignoredCode = prefs.getInt(KEY_IGNORED_VERSION_CODE, -1)
        val ignoredName = prefs.getString(KEY_IGNORED_VERSION_NAME, null)
        return (versionCode > 0 && ignoredCode >= versionCode) || (ignoredName != null && ignoredName == versionName)
    }

    fun ignoreVersion(context: Context, versionCode: Int, versionName: String) {
        getPrefs(context).edit()
            .putInt(KEY_IGNORED_VERSION_CODE, versionCode)
            .putString(KEY_IGNORED_VERSION_NAME, versionName)
            .apply()
        availableUpdate = null
        MeloLog.d(TAG, "Версия $versionName ($versionCode) добавлена в игнорируемые")
    }

    fun clearIgnoredVersion(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_IGNORED_VERSION_CODE)
            .remove(KEY_IGNORED_VERSION_NAME)
            .apply()
    }

    /**
     * Проверяет наличие новой версии через GitHub rawusercontent / cdn.jsdelivr.net.
     */
    suspend fun checkForUpdates(context: Context, manual: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        if (isChecking) return@withContext availableUpdate
        isChecking = true
        lastCheckStatus = "Проверка обновлений..."
        MeloLog.d(TAG, "Запуск проверки обновлений (manual=$manual)...")

        var jsonBody: String? = null
        for (urlStr in VERSION_URLS) {
            try {
                val req = Request.Builder()
                    .url(urlStr)
                    .header("User-Agent", "Melo-Android/${BuildConfig.VERSION_NAME}")
                    .header("Cache-Control", "no-cache")
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            jsonBody = body
                            MeloLog.d(TAG, "Успешно получен version.json из $urlStr")
                            return@use
                        }
                    } else {
                        MeloLog.d(TAG, "Запрос к $urlStr вернул HTTP ${resp.code}")
                    }
                }
                if (jsonBody != null) break
            } catch (e: Exception) {
                MeloLog.d(TAG, "Не удалось подключиться к $urlStr: ${e.message}")
            }
        }

        if (jsonBody == null) {
            // Фолбэк на GitHub Releases API (на случай, если version.json ещё не запушен в ветку)
            jsonBody = fetchFromGithubApi()
        }

        if (jsonBody == null) {
            lastCheckStatus = "Не удалось проверить обновления"
            isChecking = false
            return@withContext null
        }

        try {
            val json = JSONObject(jsonBody!!)
            val remoteCode = json.optInt("versionCode", 0)
            val remoteName = json.optString("versionName", "").removePrefix("v")
            val apkUrl = json.optString("apkUrl", FALLBACK_APK_URL).ifBlank { FALLBACK_APK_URL }
            val changelog = json.optString("changelog", "Улучшения стабильности и исправления ошибок")
            val sha256 = json.optString("sha256", "")

            val currentCode = BuildConfig.VERSION_CODE
            val currentName = BuildConfig.VERSION_NAME

            val isNewer = when {
                remoteCode > currentCode -> true
                remoteCode == currentCode && remoteName.isNotBlank() && remoteName != currentName -> {
                    isSemVerNewer(remoteName, currentName)
                }
                remoteCode == 0 && remoteName.isNotBlank() && remoteName != currentName -> {
                    isSemVerNewer(remoteName, currentName)
                }
                else -> false
            }

            if (!isNewer) {
                lastCheckStatus = "У вас последняя версия ($currentName)"
                availableUpdate = null
                isChecking = false
                MeloLog.d(TAG, "Текущая версия $currentName ($currentCode) актуальна. На сервере: $remoteName ($remoteCode)")
                return@withContext null
            }

            val info = UpdateInfo(
                versionName = if (remoteName.isNotBlank()) remoteName else "v$remoteCode",
                versionCode = remoteCode,
                apkUrl = apkUrl,
                changelog = changelog,
                sha256 = sha256,
            )

            // Если версия была скрыта пользователем и это не ручная проверка — не навязываем
            if (!manual && isVersionIgnored(context, remoteCode, info.versionName)) {
                MeloLog.d(TAG, "Обновление ${info.versionName} пропущено, так как пользователь выбрал 'Не показывать больше'")
                availableUpdate = null
                lastCheckStatus = "Доступно обновление ${info.versionName} (скрыто)"
                isChecking = false
                return@withContext null
            }

            availableUpdate = info
            lastCheckStatus = "Доступна версия ${info.versionName}"
            MeloLog.d(TAG, "Найдено обновление: ${info.versionName} ($remoteCode)")
            isChecking = false
            info
        } catch (e: Exception) {
            MeloLog.e(TAG, "Ошибка парсинга version.json: ${e.message}", e)
            lastCheckStatus = "Ошибка проверки обновлений"
            isChecking = false
            null
        }
    }

    /**
     * Скачивает APK и запускает системный диалог установки.
     */
    suspend fun downloadAndInstall(
        context: Context,
        info: UpdateInfo,
        onProgress: (step: String, progress: Float) -> Unit = { _, _ -> },
    ): Boolean = withContext(Dispatchers.IO) {
        if (isDownloading) return@withContext false
        isDownloading = true
        downloadProgress = 0f
        downloadStatusText = "Подключение к серверу..."
        onProgress("Подключение к серверу...", 0.05f)

        val apkFile = File(context.cacheDir, "melo_update.apk")
        if (apkFile.exists()) apkFile.delete()

        try {
            val req = Request.Builder()
                .url(info.apkUrl)
                .header("User-Agent", "Melo-Android-Updater")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = "Ошибка загрузки: HTTP ${resp.code}"
                    downloadStatusText = err
                    onProgress(err, 0f)
                    isDownloading = false
                    return@withContext false
                }

                val body = resp.body ?: run {
                    downloadStatusText = "Пустой ответ сервера"
                    isDownloading = false
                    return@withContext false
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            val frac = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0.5f
                            downloadProgress = frac
                            val pct = (frac * 100).toInt()
                            val msg = "Скачивание: $pct%"
                            downloadStatusText = msg
                            onProgress(msg, frac)
                        }
                    }
                }
            }

            if (!apkFile.exists() || apkFile.length() == 0L) {
                downloadStatusText = "Файл APK пуст или не сохранён"
                isDownloading = false
                return@withContext false
            }

            // Проверка SHA256 если предоставлен
            if (info.sha256.isNotBlank()) {
                downloadStatusText = "Проверка целостности..."
                onProgress("Проверка целостности...", 0.98f)
                val calculatedSha = calculateSha256(apkFile)
                if (!calculatedSha.equals(info.sha256, ignoreCase = true)) {
                    MeloLog.e(TAG, "SHA256 не совпал! Ожидался: ${info.sha256}, вычислен: $calculatedSha")
                    apkFile.delete()
                    downloadStatusText = "Ошибка контрольной суммы"
                    isDownloading = false
                    return@withContext false
                }
            }

            downloadStatusText = "Запуск установки..."
            onProgress("Запуск установки...", 1f)
            val installed = withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }

            if (!installed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallApk(context)) {
                    downloadStatusText = "Требуется разрешение на установку приложений"
                } else {
                    downloadStatusText = "Не удалось запустить установщик"
                }
            }

            isDownloading = false
            installed
        } catch (e: Exception) {
            MeloLog.e(TAG, "Ошибка скачивания APK: ${e.message}", e)
            downloadStatusText = "Ошибка скачивания: ${e.message}"
            apkFile.delete()
            isDownloading = false
            false
        }
    }

    fun canInstallApk(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                MeloLog.e(TAG, "Не удалось открыть настройки неизвестных источников: ${e.message}", e)
            }
        }
    }

    fun installApk(context: Context, apkFile: File = File(context.cacheDir, "melo_update.apk")): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            MeloLog.e(TAG, "Файл APK не найден или пуст")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallApk(context)) {
            MeloLog.d(TAG, "Нет разрешения REQUEST_INSTALL_PACKAGES, открытие настроек...")
            openInstallPermissionSettings(context)
            return false
        }

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            MeloLog.e(TAG, "Не удалось запустить установщик APK: ${e.message}", e)
            false
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.forEachBlock { buffer, bytesRead ->
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isSemVerNewer(remote: String, current: String): Boolean {
        return try {
            val rParts = remote.split('.').mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
            val cParts = current.split('.').mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
            val maxLen = maxOf(rParts.size, cParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            false
        } catch (_: Exception) {
            remote != current
        }
    }

    private fun fetchFromGithubApi(): String? {
        val apiUrls = listOf(
            "https://api.github.com/repos/Bebrazui/Melo-Releases/releases/latest",
            "https://api.github.com/repos/Bebrazui/Melo/releases/latest",
        )
        for (apiUrl in apiUrls) {
            try {
                val req = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "Melo-Android/${BuildConfig.VERSION_NAME}")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string() ?: return@use
                        val json = JSONObject(bodyStr)
                        val tag = json.optString("tag_name", "").removePrefix("v")
                        val bodyText = json.optString("body", "Новая версия на GitHub")
                        val assets = json.optJSONArray("assets")
                        var apkUrl: String? = null
                        if (assets != null) {
                            for (i in 0 until assets.length()) {
                                val a = assets.getJSONObject(i)
                                val name = a.optString("name", "")
                                if (name.endsWith(".apk", ignoreCase = true)) {
                                    apkUrl = a.optString("browser_download_url", "")
                                    break
                                }
                            }
                        }
                        val synthetic = JSONObject().apply {
                            put("versionCode", 0)
                            put("versionName", tag)
                            put("apkUrl", apkUrl ?: FALLBACK_APK_URL)
                            put("changelog", bodyText)
                        }
                        return synthetic.toString()
                    }
                }
            } catch (e: Exception) {
                MeloLog.d(TAG, "GitHub API fallback failed for $apiUrl: ${e.message}")
            }
        }
        return null
    }
}
