package com.melo.music.favorites

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Хранилище избранных треков в SharedPreferences.
 * Выживает перезаход приложения.
 */
object FavoritesManager {

    private const val PREFS = "melo_favorites"
    private const val KEY_TRACKS = "liked_tracks"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getAll(): MutableList<TrackItem> {
        val json = prefs?.getString(KEY_TRACKS, null) ?: return mutableListOf()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TrackItem(
                    title = obj.getString("title"),
                    uploader = obj.optString("uploader", null),
                    url = obj.getString("url"),
                    durationSeconds = obj.optLong("duration", 0),
                    thumbnailUrl = obj.optString("thumbnail", null),
                    source = Source.valueOf(obj.optString("source", "YOUTUBE_MUSIC")),
                    kind = ItemKind.TRACK,
                    speed = obj.optDouble("speed", 1.0).toFloat(),
                )
            }.toMutableList()
        }.getOrDefault(mutableListOf())
    }

    fun isLiked(url: String): Boolean = getAll().any { it.url == url }

    /** Совпадение с учётом скорости — slowed/sped up версии хранятся отдельно. */
    fun isLiked(item: TrackItem): Boolean =
        getAll().any { it.url == item.url && sameSpeed(it.speed, item.speed) }

    fun toggle(item: TrackItem): Boolean {
        val list = getAll()
        val idx = list.indexOfFirst { it.url == item.url && sameSpeed(it.speed, item.speed) }
        return if (idx >= 0) {
            list.removeAt(idx)
            save(list)
            false
        } else {
            list.add(item)
            save(list)
            // Заранее резолвим прямую ссылку → следующее воспроизведение мгновенное.
            appContext?.let { ctx -> Extractor.prefetch(ctx, item.url) }
            true
        }
    }

    private fun save(list: List<TrackItem>) {
        val arr = JSONArray()
        for (t in list) {
            arr.put(JSONObject().apply {
                put("title", t.title)
                put("uploader", t.uploader ?: "")
                put("url", t.url)
                put("duration", t.durationSeconds)
                put("thumbnail", t.thumbnailUrl ?: "")
                put("source", t.source.name)
                put("speed", t.speed.toDouble())
            })
        }
        prefs?.edit()?.putString(KEY_TRACKS, arr.toString())?.apply()
    }

    private fun sameSpeed(a: Float, b: Float) = kotlin.math.abs(a - b) < 0.01f
}
