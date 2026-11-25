package com.melo.music.history

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.db.HistoryEntity
import com.melo.music.db.MeloDatabase
import com.melo.music.db.PlayCountEntity
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * История прослушивания на Room Database с in-memory кэшем.
 * Автоматически мигрирует данные из SharedPreferences.
 */
object HistoryManager {

    private const val PREFS = "melo_history"
    private const val KEY = "recent"
    private const val KEY_PLAYS = "plays"
    private const val CAP = 50

    private var prefs: SharedPreferences? = null
    private var database: MeloDatabase? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val inMemoryRecent = CopyOnWriteArrayList<TrackItem>()
    private val inMemoryPlays = ConcurrentHashMap<String, Pair<Int, Long>>()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        val db = MeloDatabase.getInstance(app)
        database = db

        scope.launch {
            val dao = db.historyDao()

            // 1. Миграция старой истории из SharedPreferences
            val oldJson = p.getString(KEY, null)
            if (!oldJson.isNullOrBlank()) {
                val legacyRecent = parseLegacyRecent(oldJson)
                if (legacyRecent.isNotEmpty()) {
                    legacyRecent.forEach { dao.insert(HistoryEntity.fromTrackItem(it)) }
                }
                p.edit().remove(KEY).apply()
            }

            // 2. Миграция статистики прослушиваний
            val oldPlaysJson = p.getString(KEY_PLAYS, null)
            if (!oldPlaysJson.isNullOrBlank()) {
                val legacyPlays = parseLegacyPlays(oldPlaysJson)
                if (legacyPlays.isNotEmpty()) {
                    dao.insertAllPlayCounts(legacyPlays.map { (url, pair) ->
                        PlayCountEntity(url = url, count = pair.first, lastPlayedAt = pair.second)
                    })
                }
                p.edit().remove(KEY_PLAYS).apply()
            }

            // 3. Загрузка в память
            val fromDb = dao.getRecent(CAP).map { it.toTrackItem() }
            inMemoryRecent.clear()
            inMemoryRecent.addAll(fromDb)

            val playsFromDb = dao.getAllPlayCounts()
            inMemoryPlays.clear()
            playsFromDb.forEach {
                inMemoryPlays[it.url] = it.count to it.lastPlayedAt
            }
        }
        initialized = true
    }

    fun getAll(): List<TrackItem> = inMemoryRecent.toList()

    /** Добавляет трек в начало истории и инкрементирует счётчик. */
    fun add(item: TrackItem) {
        if (item.kind != ItemKind.TRACK) return
        inMemoryRecent.removeAll { it.url == item.url }
        inMemoryRecent.add(0, item)
        while (inMemoryRecent.size > CAP) {
            inMemoryRecent.removeAt(inMemoryRecent.size - 1)
        }

        scope.launch {
            val dao = database?.historyDao() ?: return@launch
            dao.insert(HistoryEntity.fromTrackItem(item))
            dao.trim(CAP)
        }
        bumpPlays(item.url)
    }

    private fun bumpPlays(url: String) {
        val (count, _) = inMemoryPlays[url] ?: (0 to 0L)
        val newCount = count + 1
        val now = System.currentTimeMillis()
        inMemoryPlays[url] = newCount to now

        scope.launch {
            database?.historyDao()?.insertPlayCount(
                PlayCountEntity(url = url, count = newCount, lastPlayedAt = now)
            )
        }
    }

    fun playCount(url: String): Int = inMemoryPlays[url]?.first ?: 0

    fun lastPlayedAt(url: String): Long = inMemoryPlays[url]?.second ?: 0L

    private fun parseLegacyRecent(json: String): List<TrackItem> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            TrackItem(
                title = obj.getString("title"),
                uploader = obj.optString("uploader", "").ifBlank { null },
                url = obj.getString("url"),
                durationSeconds = obj.optLong("duration", 0),
                thumbnailUrl = obj.optString("thumbnail", "").ifBlank { null },
                source = runCatching { Source.valueOf(obj.optString("source", "YOUTUBE_MUSIC")) }
                    .getOrDefault(Source.YOUTUBE_MUSIC),
                kind = ItemKind.TRACK,
            )
        }
    }.getOrDefault(emptyList())

    private fun parseLegacyPlays(json: String): Map<String, Pair<Int, Long>> = runCatching {
        val result = mutableMapOf<String, Pair<Int, Long>>()
        val obj = JSONObject(json)
        obj.keys().forEach { url ->
            val o = obj.getJSONObject(url)
            result[url] = o.getInt("count") to o.optLong("ts")
        }
        result
    }.getOrDefault(emptyMap())
}
