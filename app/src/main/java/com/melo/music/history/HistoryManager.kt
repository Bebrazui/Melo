package com.melo.music.history

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * История прослушивания в SharedPreferences (последние первыми, без дублей).
 */
object HistoryManager {

    private const val PREFS = "melo_history"
    private const val KEY = "recent"
    private const val KEY_PLAYS = "plays"
    private const val CAP = 50

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getAll(): List<TrackItem> {
        val json = prefs?.getString(KEY, null) ?: return emptyList()
        return runCatching {
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
    }

    /** Добавляет трек в начало истории (убирая прежнее вхождение) и инкрементирует счётчик. */
    fun add(item: TrackItem) {
        if (item.kind != ItemKind.TRACK) return
        val list = getAll().filterNot { it.url == item.url }.toMutableList()
        list.add(0, item)
        while (list.size > CAP) list.removeAt(list.size - 1)
        save(list)
        bumpPlays(item.url)
    }

    // ── Персональная статистика прослушиваний ───────────────────────────────

    private fun plays(): MutableMap<String, Pair<Int, Long>> {
        val json = prefs?.getString(KEY_PLAYS, null) ?: return mutableMapOf()
        return runCatching {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Pair<Int, Long>>()
            obj.keys().forEach { url ->
                val o = obj.getJSONObject(url)
                map[url] = o.getInt("count") to o.optLong("ts")
            }
            map
        }.getOrDefault(mutableMapOf())
    }

    private fun bumpPlays(url: String) {
        val map = plays()
        val (count, _) = map[url] ?: (0 to 0L)
        map[url] = count + 1 to System.currentTimeMillis()
        // Ограничиваем размер: оставляем самые свежие/частые.
        while (map.size > 500) map.remove(map.keys.first())
        val obj = JSONObject()
        map.forEach { (u, v) ->
            obj.put(u, JSONObject().apply { put("count", v.first); put("ts", v.second) })
        }
        prefs?.edit()?.putString(KEY_PLAYS, obj.toString())?.apply()
    }

    /** Сколько раз пользователь слушал трек. */
    fun playCount(url: String): Int = plays()[url]?.first ?: 0

    /** Момент последнего прослушивания (epoch мс), 0 если не слушал. */
    fun lastPlayedAt(url: String): Long = plays()[url]?.second ?: 0L

    private fun save(list: List<TrackItem>) {
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
