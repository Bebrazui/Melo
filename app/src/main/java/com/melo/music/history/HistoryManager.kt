package com.melo.music.history

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * История прослушивания в SharedPreferences с in-memory кэшем (последние первыми, без дублей).
 */
object HistoryManager {

    private const val PREFS = "melo_history"
    private const val KEY = "recent"
    private const val KEY_PLAYS = "plays"
    private const val CAP = 50

    private var prefs: SharedPreferences? = null
    private val inMemoryRecent = mutableListOf<TrackItem>()
    private val inMemoryPlays = mutableMapOf<String, Pair<Int, Long>>()
    private var initialized = false

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        loadAll(p)
    }

    @Synchronized
    private fun loadAll(p: SharedPreferences) {
        if (initialized) return
        inMemoryRecent.clear()
        inMemoryPlays.clear()

        val json = p.getString(KEY, null)
        if (!json.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    inMemoryRecent.add(
                        TrackItem(
                            title = obj.getString("title"),
                            uploader = obj.optString("uploader", "").ifBlank { null },
                            url = obj.getString("url"),
                            durationSeconds = obj.optLong("duration", 0),
                            thumbnailUrl = obj.optString("thumbnail", "").ifBlank { null },
                            source = runCatching { Source.valueOf(obj.optString("source", "YOUTUBE_MUSIC")) }
                                .getOrDefault(Source.YOUTUBE_MUSIC),
                            kind = ItemKind.TRACK,
                        ),
                    )
                }
            }
        }

        val jsonPlays = p.getString(KEY_PLAYS, null)
        if (!jsonPlays.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(jsonPlays)
                obj.keys().forEach { url ->
                    val o = obj.getJSONObject(url)
                    inMemoryPlays[url] = o.getInt("count") to o.optLong("ts")
                }
            }
        }
        initialized = true
    }

    @Synchronized
    fun getAll(): List<TrackItem> {
        ensureLoaded()
        return inMemoryRecent.toList()
    }

    /** Добавляет трек в начало истории (убирая прежнее вхождение) и инкрементирует счётчик. */
    @Synchronized
    fun add(item: TrackItem) {
        if (item.kind != ItemKind.TRACK) return
        ensureLoaded()
        inMemoryRecent.removeAll { it.url == item.url }
        inMemoryRecent.add(0, item)
        while (inMemoryRecent.size > CAP) inMemoryRecent.removeAt(inMemoryRecent.size - 1)
        saveRecent(inMemoryRecent)
        bumpPlays(item.url)
    }

    // ── Персональная статистика прослушиваний ───────────────────────────────

    @Synchronized
    private fun bumpPlays(url: String) {
        ensureLoaded()
        val (count, _) = inMemoryPlays[url] ?: (0 to 0L)
        inMemoryPlays[url] = count + 1 to System.currentTimeMillis()
        while (inMemoryPlays.size > 500) inMemoryPlays.remove(inMemoryPlays.keys.first())
        val obj = JSONObject()
        inMemoryPlays.forEach { (u, v) ->
            obj.put(u, JSONObject().apply { put("count", v.first); put("ts", v.second) })
        }
        prefs?.edit()?.putString(KEY_PLAYS, obj.toString())?.apply()
    }

    /** Сколько раз пользователь слушал трек. */
    @Synchronized
    fun playCount(url: String): Int {
        ensureLoaded()
        return inMemoryPlays[url]?.first ?: 0
    }

    /** Момент последнего прослушивания (epoch мс), 0 если не слушал. */
    @Synchronized
    fun lastPlayedAt(url: String): Long {
        ensureLoaded()
        return inMemoryPlays[url]?.second ?: 0L
    }

    private fun ensureLoaded() {
        if (!initialized) {
            prefs?.let { loadAll(it) }
        }
    }

    private fun saveRecent(list: List<TrackItem>) {
        val arr = JSONArray()
        for (t in list) {
            arr.put(
                JSONObject().apply {
                    put("title", t.title)
                    put("uploader", t.uploader ?: "")
                    put("url", t.url)
                    put("duration", t.durationSeconds)
                    put("thumbnail", t.thumbnailUrl ?: "")
                    put("source", t.source.name)
                },
            )
        }
        prefs?.edit()?.putString(KEY, arr.toString())?.apply()
    }
}
