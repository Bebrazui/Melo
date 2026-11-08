package com.melo.music.artists

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.extractor.cleanArtistName
import org.json.JSONArray
import org.json.JSONObject

/**
 * Хранилище подписок на исполнителей.
 * Сохраняется в SharedPreferences, переживает перезапуск приложения.
 */
object ArtistSubscriptionManager {

    private const val PREFS = "melo_artist_subscriptions"
    private const val KEY_ARTISTS = "subscribed_artists"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private var inMemoryCache: MutableList<TrackItem>? = null

    var version by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    @Synchronized
    private fun loadFromPrefs(): MutableList<TrackItem> {
        val cached = inMemoryCache
        if (cached != null) return cached
        val json = prefs?.getString(KEY_ARTISTS, null) ?: return mutableListOf<TrackItem>().also { inMemoryCache = it }
        val list = runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TrackItem(
                    title = obj.getString("title"),
                    uploader = obj.optString("uploader", "Исполнитель").ifBlank { "Исполнитель" },
                    url = obj.getString("url"),
                    durationSeconds = 0L,
                    thumbnailUrl = obj.optString("thumbnail", "").ifBlank { null },
                    source = Source.valueOf(obj.optString("source", "YOUTUBE_MUSIC")),
                    kind = ItemKind.ARTIST,
                )
            }.toMutableList()
        }.getOrDefault(mutableListOf())
        inMemoryCache = list
        return list
    }

    @Synchronized
    fun getAll(): List<TrackItem> = ArrayList(inMemoryCache ?: loadFromPrefs())

    private fun normalize(name: String): String =
        (cleanArtistName(name) ?: name).trim().lowercase()

    @Synchronized
    fun isSubscribed(artist: TrackItem): Boolean {
        val list = inMemoryCache ?: loadFromPrefs()
        val targetName = normalize(artist.title)
        return list.any {
            it.url == artist.url || normalize(it.title) == targetName
        }
    }

    @Synchronized
    fun isSubscribed(name: String): Boolean {
        val targetName = normalize(name)
        val list = inMemoryCache ?: loadFromPrefs()
        return list.any { normalize(it.title) == targetName }
    }

    @Synchronized
    fun toggle(artist: TrackItem): Boolean {
        val list = inMemoryCache ?: loadFromPrefs()
        val targetName = normalize(artist.title)
        val idx = list.indexOfFirst {
            it.url == artist.url || normalize(it.title) == targetName
        }
        return if (idx >= 0) {
            list.removeAt(idx)
            save(list)
            version++
            false
        } else {
            val cleanTitle = cleanArtistName(artist.title) ?: artist.title
            val itemToAdd = artist.copy(
                title = cleanTitle,
                uploader = "Исполнитель",
                kind = ItemKind.ARTIST,
            )
            list.add(0, itemToAdd)
            save(list)
            version++
            true
        }
    }

    @Synchronized
    private fun save(list: List<TrackItem>) {
        inMemoryCache = list.toMutableList()
        val arr = JSONArray()
        for (a in list) {
            arr.put(
                JSONObject().apply {
                    put("title", a.title)
                    put("uploader", a.uploader ?: "Исполнитель")
                    put("url", a.url)
                    put("thumbnail", a.thumbnailUrl ?: "")
                    put("source", a.source.name)
                }
            )
        }
        prefs?.edit()?.putString(KEY_ARTISTS, arr.toString())?.apply()
    }
}
