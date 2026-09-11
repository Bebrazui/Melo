package com.melo.desktop.audio.cache

import com.melo.desktop.net.DesktopMeloNet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Менеджер дискового кэширования аудиофайлов Melo Desktop.
 * Сохраняет прослушанные треки в ~/.melo/cache/audio/ для мгновенного повторного
 * воспроизведения (< 50 мс) без сетевых запросов.
 */
object AudioCacheManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = ConcurrentHashMap<String, CompletableDeferred<File?>>()

    private val baseCacheDir: File by lazy {
        val dir = File(System.getProperty("user.home"), ".melo/cache/audio")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /** Максимальный размер кэша: 2 ГБ. */
    const val MAX_CACHE_BYTES: Long = 2L * 1024 * 1024 * 1024

    /**
     * Получение уникального ключа кэша по URL трека (Video ID или SHA-256).
     */
    fun cacheKey(url: String): String {
        val ytId = Regex("[?&]v=([\\w-]+)").find(url)?.groupValues?.get(1)
            ?: Regex("youtu\\.be/([\\w-]+)").find(url)?.groupValues?.get(1)
        if (!ytId.isNullOrBlank()) return ytId

        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(url.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(24)
    }

    /**
     * Возвращает полностью скачанный файл трека из кэша, если он существует.
     */
    fun getCachedFile(originalUrl: String): File? {
        val key = cacheKey(originalUrl)
        val candidates = listOf(
            File(baseCacheDir, "$key.m4a"),
            File(baseCacheDir, "$key.mp3"),
            File(baseCacheDir, "$key.ogg"),
        )
        return candidates.firstOrNull { it.exists() && it.isFile && it.length() > 64 * 1024 }
    }

    /**
     * Проверяет, закэширован ли трек.
     */
    fun isCached(originalUrl: String): Boolean = getCachedFile(originalUrl) != null

    /**
     * Возвращает временный файл для записи (.part).
     */
    fun getPartFile(originalUrl: String, ext: String = ".m4a"): File {
        val key = cacheKey(originalUrl)
        val extension = if (ext.startsWith(".")) ext else ".$ext"
        return File(baseCacheDir, "$key$extension.part")
    }

    /**
     * Завершение записи: атомарное переименование .part в целевой файл.
     */
    fun commitPartFile(originalUrl: String, ext: String = ".m4a"): File? {
        val part = getPartFile(originalUrl, ext)
        if (!part.exists() || part.length() < 64 * 1024) {
            part.delete()
            return null
        }

        val key = cacheKey(originalUrl)
        val extension = if (ext.startsWith(".")) ext else ".$ext"
        val target = File(baseCacheDir, "$key$extension")

        if (target.exists()) target.delete()
        val success = part.renameTo(target)
        if (success) {
            println("[AudioCacheManager] Cached track ($key$extension, ${target.length() / 1024} KB)")
            trimCacheIfNeeded()
            return target
        }
        return null
    }

    /**
     * Запускает фоновую загрузку трека в дисковый кэш на полной скорости сети.
     * Возвращает CompletableDeferred с файлом кэша или null при ошибке.
     */
    fun startBackgroundDownload(originalUrl: String, audioUrl: String): CompletableDeferred<File?> {
        val key = cacheKey(originalUrl)
        val existingFile = getCachedFile(originalUrl)
        if (existingFile != null) {
            return CompletableDeferred(existingFile)
        }

        val existingJob = activeDownloads[key]
        if (existingJob != null) {
            return existingJob
        }

        val deferred = CompletableDeferred<File?>()
        val prev = activeDownloads.putIfAbsent(key, deferred)
        if (prev != null) {
            return prev
        }

        scope.launch {
            println("[AudioCacheManager] Starting background download for $key...")
            var resultFile: File? = null
            try {
                val ext = when {
                    audioUrl.contains(".mp3") -> ".mp3"
                    audioUrl.contains(".ogg") || audioUrl.contains(".opus") -> ".ogg"
                    else -> ".m4a"
                }
                val partFile = getPartFile(originalUrl, ext)
                val request = Request.Builder()
                    .url(audioUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()

                DesktopMeloNet.okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            FileOutputStream(partFile).use { out ->
                                body.byteStream().use { input ->
                                    input.copyTo(out, bufferSize = 64 * 1024)
                                }
                                out.flush()
                            }
                            resultFile = commitPartFile(originalUrl, ext)
                            println("[AudioCacheManager] Background download committed for $key: ${resultFile?.name}")
                        }
                    } else {
                        println("[AudioCacheManager] Background download HTTP error ${response.code} for $key")
                    }
                }
            } catch (e: Exception) {
                println("[AudioCacheManager] Background cache error for $key: ${e.message}")
            } finally {
                activeDownloads.remove(key)
                deferred.complete(resultFile ?: getCachedFile(originalUrl))
            }
        }

        return deferred
    }

    /**
     * Ожидает завершения кэширования трека с таймаутом (по умолчанию 3.5 секунды).
     */
    suspend fun awaitCachedFile(originalUrl: String, timeoutMs: Long = 3500L): File? {
        val direct = getCachedFile(originalUrl)
        if (direct != null) return direct

        val key = cacheKey(originalUrl)
        val job = activeDownloads[key] ?: return getCachedFile(originalUrl)
        return withTimeoutOrNull(timeoutMs) { job.await() } ?: getCachedFile(originalUrl)
    }

    /**
     * Вычисляет суммарный размер дискового кэша аудио в байтах.
     */
    fun getCacheSizeBytes(): Long {
        if (!baseCacheDir.exists()) return 0L
        return baseCacheDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    /**
     * Полная очистка кэша аудио.
     */
    fun clearCache() {
        if (!baseCacheDir.exists()) return
        baseCacheDir.listFiles()?.forEach { file ->
            runCatching { file.delete() }
        }
        println("[AudioCacheManager] Cache cleared.")
    }

    /**
     * LRU-очистка: удаляет самые старые файлы, если размер кэша превышает лимит.
     */
    fun trimCacheIfNeeded(maxBytes: Long = MAX_CACHE_BYTES) {
        val files = baseCacheDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") } ?: return
        var currentSize = files.sumOf { it.length() }
        if (currentSize <= maxBytes) return

        val sorted = files.sortedBy { it.lastModified() }
        for (file in sorted) {
            if (currentSize <= maxBytes * 0.85) break
            val len = file.length()
            if (file.delete()) {
                currentSize -= len
                println("[AudioCacheManager] Evicted old cache file: ${file.name}")
            }
        }
    }
}
