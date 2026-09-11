package com.melo.desktop.storage

import com.melo.desktop.extractor.ResolvedTrack
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Постоянный дисковый кэш прямых аудио-ссылок YouTube / SoundCloud.
 * Хранит прямые ссылки с вычисленным сроком годности (expire/Policy) в ~/.melo/cache/stream_cache.json.
 * Позволяет начинать воспроизведение ранее открытых треков мгновенно (< 5 мс) без NewPipe-резолва.
 */
object DesktopStreamCacheStore {

    private const val MAX_ENTRIES = 500
    private val DEFAULT_TTL_MS = TimeUnit.MINUTES.toMillis(20)
    private val SAFETY_MARGIN_MS = TimeUnit.MINUTES.toMillis(5)

    private val EXPIRE_RE = Regex("[?&](?:expire|Expires)=(\\d{10})")
    private val POLICY_RE = Regex("[?&]Policy=([A-Za-z0-9_\\-~=]+)")
    private val EPOCH_RE = Regex("EpochTime\"?\\s*:\\s*(\\d{10})")

    private val cacheDir = File(System.getProperty("user.home"), ".melo/cache").apply { mkdirs() }
    private val cacheFile = File(cacheDir, "stream_cache.json")

    private val memoryMap = ConcurrentHashMap<String, CacheEntry>()

    data class CacheEntry(
        val resolved: ResolvedTrack,
        val expiresAt: Long,
    )

    init {
        loadFromDisk()
    }

    /** Получить свежий трек из кэша (если срок действия не истёк). */
    fun get(pageUrl: String): ResolvedTrack? {
        val entry = memoryMap[pageUrl] ?: return null
        if (entry.expiresAt <= System.currentTimeMillis()) {
            memoryMap.remove(pageUrl)
            return null
        }
        return entry.resolved
    }

    /** Сохранить трек в кэш с автоматическим вычислением срока годности. */
    fun put(pageUrl: String, resolved: ResolvedTrack) {
        val dur = if (resolved.durationSeconds > 0) {
            resolved.durationSeconds
        } else {
            Regex("[?&]dur=([0-9]+(?:\\.[0-9]+)?)").find(resolved.audioUrl)?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: 0L
        }
        val finalResolved = if (dur > 0 && resolved.durationSeconds <= 0) resolved.copy(durationSeconds = dur) else resolved
        val expiresAt = expiryOf(finalResolved.audioUrl)
        memoryMap[pageUrl] = CacheEntry(finalResolved, expiresAt)
        trimAndSaveAsync()
    }

    fun remove(pageUrl: String) {
        if (memoryMap.remove(pageUrl) != null) {
            trimAndSaveAsync()
        }
    }

    private fun expiryOf(audioUrl: String): Long {
        val now = System.currentTimeMillis()
        val floor = now + TimeUnit.MINUTES.toMillis(1)

        // 1. YouTube googlevideo
        EXPIRE_RE.find(audioUrl)?.groupValues?.get(1)?.toLongOrNull()?.let {
            return maxOf(it * 1000L - SAFETY_MARGIN_MS, floor)
        }

        // 2. SoundCloud CloudFront Policy
        POLICY_RE.find(audioUrl)?.groupValues?.get(1)?.let { policy ->
            val decoded = decodeCfPolicy(policy)
            EPOCH_RE.find(decoded)?.groupValues?.get(1)?.toLongOrNull()?.let {
                return maxOf(it * 1000L - SAFETY_MARGIN_MS, floor)
            }
        }

        // 3. Fallback TTL
        return now + DEFAULT_TTL_MS
    }

    private fun decodeCfPolicy(p: String): String = runCatching {
        val b64 = p.replace('-', '+').replace('_', '=').replace('~', '/')
        val pad = (4 - b64.length % 4) % 4
        val padded = b64 + "=".repeat(pad)
        String(Base64.getDecoder().decode(padded), StandardCharsets.UTF_8)
    }.getOrDefault("")

    private fun loadFromDisk() {
        if (!cacheFile.exists()) return
        try {
            val content = cacheFile.readText(StandardCharsets.UTF_8)
            val json = JSONObject(content)
            val now = System.currentTimeMillis()

            json.keys().forEach { key ->
                val obj = json.optJSONObject(key) ?: return@forEach
                val exp = obj.optLong("exp", 0L)
                val audio = obj.optString("audio", "")
                if (exp > now && audio.isNotBlank()) {
                    var dur = obj.optLong("dur", 0L)
                    if (dur <= 0L) {
                        dur = Regex("[?&]dur=([0-9]+(?:\\.[0-9]+)?)").find(audio)?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: 0L
                    }
                    val resolved = ResolvedTrack(
                        title = obj.optString("title", key),
                        audioUrl = audio,
                        thumbnailUrl = obj.optString("thumb", "").ifBlank { null },
                        artist = obj.optString("artist", "").ifBlank { null },
                        videoUrl = obj.optString("video", "").ifBlank { null },
                        originalUrl = key,
                        durationSeconds = dur,
                    )
                    memoryMap[key] = CacheEntry(resolved, exp)
                }
            }
            println("[DesktopStreamCacheStore] Loaded ${memoryMap.size} valid cached stream URLs")
        } catch (e: Exception) {
            System.err.println("[DesktopStreamCacheStore] Failed to read stream cache: ${e.message}")
        }
    }

    @Volatile
    private var saveScheduled = false

    private fun trimAndSaveAsync() {
        if (saveScheduled) return
        saveScheduled = true

        Thread({
            try {
                Thread.sleep(800) // Debounce writes
                saveScheduled = false
                saveToDisk()
            } catch (_: Exception) {}
        }, "Melo-StreamCacheSaver").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    private fun saveToDisk() {
        try {
            val now = System.currentTimeMillis()
            // Убираем протухшие
            val validEntries = memoryMap.entries
                .filter { it.value.expiresAt > now }
                .sortedByDescending { it.value.expiresAt }
                .take(MAX_ENTRIES)

            val root = JSONObject()
            for ((key, entry) in validEntries) {
                val entryDur = if (entry.resolved.durationSeconds > 0) {
                    entry.resolved.durationSeconds
                } else {
                    Regex("[?&]dur=([0-9]+(?:\\.[0-9]+)?)").find(entry.resolved.audioUrl)?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: 0L
                }
                val obj = JSONObject().apply {
                    put("audio", entry.resolved.audioUrl)
                    put("title", entry.resolved.title)
                    put("thumb", entry.resolved.thumbnailUrl ?: "")
                    put("artist", entry.resolved.artist ?: "")
                    put("video", entry.resolved.videoUrl ?: "")
                    put("dur", entryDur)
                    put("exp", entry.expiresAt)
                }
                root.put(key, obj)
            }
            cacheFile.writeText(root.toString(2), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            System.err.println("[DesktopStreamCacheStore] Failed to write stream cache: ${e.message}")
        }
    }
}
