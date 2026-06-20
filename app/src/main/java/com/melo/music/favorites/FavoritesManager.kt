package com.melo.music.favorites

import android.content.Context
import android.content.SharedPreferences
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

    fun init(context: Context) {
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
                )
            }.toMutableList()
        }.getOrDefault(mutableListOf())
    }

    fun isLiked(url: String): Boolean = getAll().any { it.url == url }

    fun toggle(item: TrackItem): Boolean {
        val list = getAll()
        val idx = list.indexOfFirst { it.url == item.url }
        return if (idx >= 0) {
            list.removeAt(idx)
            save(list)
            false
        } else {
            list.add(item)
            save(list)
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
            })
        }
        prefs?.edit()?.putString(KEY_TRACKS, arr.toString())?.apply()
    }
}
