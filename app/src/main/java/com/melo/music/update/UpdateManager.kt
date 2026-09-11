package com.melo.music.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.melo.music.BuildConfig
import com.melo.music.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val patchVersion: Int,
    val patchUrl: String?,
    val patchSha256: String?,
    val apkUrl: String?,
    val apkSha256: String?,
    val changelog: String,
    val isPatchOnly: Boolean,
)

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val GITHUB_API_LATEST = "https://api.github.com/repos/Bebrazui/Melo/releases/latest"

    var availableUpdate by mutableStateOf<UpdateInfo?>(null)
    var isChecking by mutableStateOf(false)
    var lastCheckStatus by mutableStateOf<String?>(null)

    /**
     * Проверяет наличие обновлений (релизного APK и/или горячего DEX-патча).
     */
    suspend fun checkForUpdates(context: Context, manual: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        if (isChecking) return@withContext availableUpdate
        isChecking = true
        lastCheckStatus = "Проверка обновлений..."

        try {
            val url = URL(GITHUB_API_LATEST)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("User-Agent", "Melo-Android")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (conn.responseCode != 200) {
                lastCheckStatus = "Сервер недоступен (${conn.responseCode})"
                isChecking = false
                return@withContext null
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val tag = json.optString("tag_name", "").removePrefix("v")
            val bodyText = json.optString("body", "Улучшения стабильности и исправления")
            val assets = json.optJSONArray("assets")

            var apkUrl: String? = null
            var patchUrl: String? = null
            var patchVersion = 0
            var newVersionCode = 0

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")

                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = downloadUrl
                    } else if (name.endsWith(".dex", ignoreCase = true)) {
                        patchUrl = downloadUrl
                        // Парсим номер патча из имени (например, patch-v14.dex -> 14)
                        val numMatch = Regex("""\d+""").find(name)
                        patchVersion = numMatch?.value?.toIntOrNull() ?: 1
                    }
                }
            }

            val currentCode = BuildConfig.VERSION_CODE
            val currentPatch = DexPatchManager.getCurrentPatchVersion(context)

            // Проверяем, есть ли что обновлять
            val hasNewPatch = patchUrl != null && patchVersion > currentPatch
            val hasNewApk = apkUrl != null && tag.isNotBlank() && tag != BuildConfig.VERSION_NAME

            if (!hasNewPatch && !hasNewApk) {
                lastCheckStatus = "У вас установлена последняя версия"
                availableUpdate = null
                isChecking = false
                return@withContext null
            }

            val isPatchOnly = hasNewPatch && !hasNewApk
            val info = UpdateInfo(
                versionName = if (hasNewApk) tag else BuildConfig.VERSION_NAME,
                versionCode = newVersionCode,
                patchVersion = patchVersion,
                patchUrl = patchUrl,
                patchSha256 = "",
                apkUrl = apkUrl,
                apkSha256 = "",
                changelog = bodyText,
                isPatchOnly = isPatchOnly,
            )

            availableUpdate = info
            lastCheckStatus = if (isPatchOnly) "Доступен патч парсера #$patchVersion" else "Доступна версия $tag"

            // Если включено автообновление — применяем тихо в фоне
            if (AppSettings.autoUpdate) {
                if (isPatchOnly && patchUrl != null) {
                    Log.i(TAG, "Автообновление: тихо качаем патч #$patchVersion в фоне")
                    DexPatchManager.updatePatch(
                        context = context,
                        downloadUrl = patchUrl,
                        newVersion = patchVersion,
                        expectedSha256 = "",
                    )
                    availableUpdate = null
                    lastCheckStatus = "Патч #$patchVersion успешно применен в фоне"
                }
            }

            isChecking = false
            info
        } catch (e: Exception) {
            Log.w(TAG, "Ошибка проверки обновлений: ${e.message}")
            lastCheckStatus = "Не удалось проверить обновления"
            isChecking = false
            null
        }
    }

    /**
     * Ручное применение обновления из UpdateActivity:
     * скачивает и устанавливает патч или запускает установщик APK.
     */
    suspend fun applyUpdate(
        context: Context,
        info: UpdateInfo,
        onProgress: (step: String, progress: Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        if (info.isPatchOnly && info.patchUrl != null) {
            onProgress("Загрузка микро-патча...", 0.2f)
            val success = DexPatchManager.updatePatch(
                context = context,
                downloadUrl = info.patchUrl,
                newVersion = info.patchVersion,
                expectedSha256 = info.patchSha256 ?: "",
                onProgress = { p -> onProgress("Загрузка компонентов (${(p * 100).toInt()}%)...", 0.2f + p * 0.6f) },
            )
            if (success) {
                onProgress("Применение патча...", 0.9f)
                availableUpdate = null
                onProgress("Обновление завершено!", 1.0f)
            }
            return@withContext success
        } else if (info.apkUrl != null) {
            onProgress("Загрузка установочного пакета...", 0.1f)
            val apkFile = File(context.cacheDir, "melo_update.apk")
            if (apkFile.exists()) apkFile.delete()

            downloadApk(info.apkUrl, apkFile) { p ->
                onProgress("Скачивание APK (${(p * 100).toInt()}%)...", 0.1f + p * 0.75f)
            }

            onProgress("Подготовка к установке...", 0.95f)

            // Установка APK
            installApk(context, apkFile)
            return@withContext true
        }
        false
    }

    private fun downloadApk(urlStr: String, destination: File, onProgress: (Float) -> Unit) {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.connect()

        val total = conn.contentLength.toFloat()
        var read = 0L

        conn.inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(16 * 1024)
                var len: Int
                while (input.read(buffer).also { len = it } != -1) {
                    output.write(buffer, 0, len)
                    read += len
                    if (total > 0) onProgress((read / total).coerceIn(0f, 1f))
                }
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
