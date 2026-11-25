package com.melo.music.extractor

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.db.MeloDatabase
import com.melo.music.db.StreamCacheEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Дисковый кэш прямых стрим-URL на Room Database (переживает перезапуск приложения).
 * Автоматически мигрирует старый 400-элементный JSON из SharedPreferences.
 */
object StreamCacheStore {

    private const val PREFS = "melo_stream_cache"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 400

    private val DEFAULT_TTL_MS = TimeUnit.MINUTES.toMillis(20)
    private val SAFETY_MARGIN_MS = TimeUnit.MINUTES.toMillis(5)

    private val EXPIRE_RE = Regex("[?&](?:expire|Expires)=(\\d{10})")
    private val POLICY_RE = Regex("[?&]Policy=([A-Za-z0-9_\\-~=]+)")
    private val EPOCH_RE = Regex("EpochTime\"?\\s*:\\s*(\\d{10})")

    private var database: MeloDatabase? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Быстрый in-memory кэш для мгновенного синхронного чтения get(url)
    private val inMemoryCache = ConcurrentHashMap<String, StreamCacheEntity>()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        val db = MeloDatabase.getInstance(app)
        database = db

        scope.launch {
            val dao = db.streamCacheDao()
            val now = System.currentTimeMillis()
            dao.pruneExpired(now)

            // Миграция старого SharedPreferences JSON
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val oldJson = prefs.getString(KEY, null)
            if (!oldJson.isNullOrBlank()) {
                val legacy = parseLegacyJson(oldJson)
                if (legacy.isNotEmpty()) {
                    dao.insertAll(legacy)
                }
                prefs.edit().remove(KEY).apply()
            }

            // Загрузка актуальных записей в память
            val valid = dao.getAllValid(now)
            inMemoryCache.clear()
            valid.forEach { inMemoryCache[it.pageUrl] = it }
        }
        initialized = true
    }

    /** Свежая запись из кэша или null. */
    fun get(pageUrl: String): ResolvedTrack? {
        val entry = inMemoryCache[pageUrl] ?: return null
        if (entry.expiresAt <= System.currentTimeMillis()) {
            inMemoryCache.remove(pageUrl)
            scope.launch { database?.streamCacheDao()?.delete(pageUrl) }
            return null
        }
        return ResolvedTrack(
            title = entry.title,
            audioUrl = entry.audioUrl,
            thumbnailUrl = entry.thumbnailUrl,
            artist = entry.artist,
            videoUrl = entry.videoUrl,
        )
    }

    /** Сохраняет резолв в памяти и сбрасывает в Room. */
    fun put(pageUrl: String, resolved: ResolvedTrack) {
        val exp = expiryOf(resolved.audioUrl)
        val entity = StreamCacheEntity(
            pageUrl = pageUrl,
            audioUrl = resolved.audioUrl,
            title = resolved.title,
            artist = resolved.artist,
            thumbnailUrl = resolved.thumbnailUrl,
            videoUrl = resolved.videoUrl,
            expiresAt = exp,
            addedAt = System.currentTimeMillis()
        )
        inMemoryCache[pageUrl] = entity

        scope.launch {
            val dao = database?.streamCacheDao() ?: return@launch
            dao.insert(entity)
            if (inMemoryCache.size > MAX_ENTRIES) {
                dao.trim(MAX_ENTRIES)
            }
        }
    }

    fun remove(pageUrl: String) {
        inMemoryCache.remove(pageUrl)
        scope.launch {
            database?.streamCacheDao()?.delete(pageUrl)
        }
    }

    private fun expiryOf(audioUrl: String): Long {
        val now = System.currentTimeMillis()
        EXPIRE_RE.find(audioUrl)?.let { m ->
            val sec = m.groupValues[1].toLongOrNull() ?: return@let
            return (sec * 1000L - SAFETY_MARGIN_MS).coerceAtLeast(now)
        }
        POLICY_RE.find(audioUrl)?.let { m ->
            val b64 = m.groupValues[1].replace('-', '+').replace('_', '/').replace('~', '=')
            val json = runCatching {
                String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8)
            }.getOrNull()
            if (json != null) {
                EPOCH_RE.find(json)?.let { em ->
                    val sec = em.groupValues[1].toLongOrNull() ?: return@let
                    return (sec * 1000L - SAFETY_MARGIN_MS).coerceAtLeast(now)
                }
            }
        }
        return now + DEFAULT_TTL_MS
    }

    private fun parseLegacyJson(json: String): List<StreamCacheEntity> = runCatching {
        val list = mutableListOf<StreamCacheEntity>()
        val root = JSONObject(json)
        val now = System.currentTimeMillis()
        root.keys().forEach { url ->
            val obj = root.optJSONObject(url) ?: return@forEach
            val exp = obj.optLong("exp", 0L)
            if (exp > now) {
                list.add(
                    StreamCacheEntity(
                        pageUrl = url,
                        audioUrl = obj.optString("audio", ""),
                        title = obj.optString("title", url),
                        artist = obj.optString("artist", "").ifBlank { null },
                        thumbnailUrl = obj.optString("thumb", "").ifBlank { null },
                        videoUrl = obj.optString("video", "").ifBlank { null },
                        expiresAt = exp
                    )
                )
            }
        }
        list
    }.getOrDefault(emptyList())
}
