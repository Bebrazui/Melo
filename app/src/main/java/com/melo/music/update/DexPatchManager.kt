package com.melo.music.update

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Менеджер горячих обновлений байткода (.dex):
 * - Схема строго одного активного файла: `code_cache/dex_patch/active.dex`.
 * - Атомарная замена `download.tmp` -> `active.dex` (файлы не плодятся).
 * - Android 14+ совместимость: `active.dex.setReadOnly()`.
 * - Автоматическая очистка мусора при запуске.
 */
object DexPatchManager {

    private const val TAG = "DexPatchManager"
    private const val PREFS_NAME = "melo_patch_prefs"
    private const val KEY_PATCH_VERSION = "current_patch_version"
    private const val PATCH_DIR_NAME = "dex_patch"
    private const val ACTIVE_DEX_NAME = "active.dex"
    private const val TEMP_DEX_NAME = "download.tmp"

    fun getCurrentPatchVersion(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_PATCH_VERSION, 0)
    }

    /**
     * Создает ClassLoader для загрузки классов из активного DEX-патча.
     * Возвращает null, если патча нет или он поврежден.
     */
    fun loadPatchClassLoader(context: Context): ClassLoader? {
        val patchDir = File(context.codeCacheDir, PATCH_DIR_NAME)
        val activeDex = File(patchDir, ACTIVE_DEX_NAME)

        cleanupGarbage(patchDir)

        if (!activeDex.exists() || activeDex.length() == 0L) {
            return null
        }

        return try {
            DexClassLoader(
                activeDex.absolutePath,
                context.codeCacheDir.absolutePath,
                null,
                context.classLoader,
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Ошибка загрузки активного патча, откат: ${e.message}", e)
            activeDex.delete()
            null
        }
    }

    /**
     * Скачивает новый DEX во временный файл, сверяет SHA-256 и атомарно заменяет active.dex.
     * Не накапливает дубликаты файлов.
     */
    suspend fun updatePatch(
        context: Context,
        downloadUrl: String,
        newVersion: Int,
        expectedSha256: String,
        onProgress: ((Float) -> Unit)? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentPatchVersion(context)
        if (newVersion <= currentVersion && newVersion != -1) {
            Log.d(TAG, "Версия патча $newVersion не новее текущей $currentVersion")
            return@withContext false
        }

        val patchDir = File(context.codeCacheDir, PATCH_DIR_NAME).apply { mkdirs() }
        val tempFile = File(patchDir, TEMP_DEX_NAME)
        val activeFile = File(patchDir, ACTIVE_DEX_NAME)

        try {
            if (tempFile.exists()) tempFile.delete()

            downloadToFile(downloadUrl, tempFile, onProgress)

            // Проверка контрольной суммы
            if (expectedSha256.isNotBlank()) {
                val actualSha = calculateSha256(tempFile)
                if (!actualSha.equals(expectedSha256, ignoreCase = true)) {
                    Log.e(TAG, "Хэш не совпал! Ожидался: $expectedSha256, получен: $actualSha")
                    tempFile.delete()
                    return@withContext false
                }
            }

            // Атомарная замена файла
            if (activeFile.exists()) {
                activeFile.delete()
            }
            val renamed = tempFile.renameTo(activeFile)
            if (!renamed) {
                tempFile.copyTo(activeFile, overwrite = true)
                tempFile.delete()
            }

            // Требование Android 14+ (W^X): код должен быть помечен как Read-Only
            activeFile.setReadOnly()

            // Сохраняем версию патча
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_PATCH_VERSION, newVersion).apply()

            Log.i(TAG, "Патч #$newVersion успешно применен (размер: ${activeFile.length()} байт)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Сбой при установке патча: ${e.message}", e)
            tempFile.delete()
            false
        }
    }

    /**
     * Сброс патча (откат на встроенный код APK).
     */
    fun resetPatch(context: Context) {
        val patchDir = File(context.codeCacheDir, PATCH_DIR_NAME)
        val activeDex = File(patchDir, ACTIVE_DEX_NAME)
        if (activeDex.exists()) activeDex.delete()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PATCH_VERSION).apply()
        Log.i(TAG, "Патч сброшен, используется встроенный код")
    }

    private fun cleanupGarbage(patchDir: File) {
        if (!patchDir.exists()) return
        patchDir.listFiles()?.forEach { file ->
            if (file.name != ACTIVE_DEX_NAME) {
                file.delete()
            }
        }
    }

    private fun downloadToFile(urlStr: String, destination: File, onProgress: ((Float) -> Unit)? = null) {
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 20000
        connection.setRequestProperty("User-Agent", "Melo-Android-Updater")
        connection.connect()

        val totalBytes = connection.contentLength.toFloat()
        var downloadedBytes = 0L

        connection.inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        onProgress?.invoke((downloadedBytes / totalBytes).coerceIn(0f, 1f))
                    }
                }
            }
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.forEachBlock { buffer, bytesRead ->
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
