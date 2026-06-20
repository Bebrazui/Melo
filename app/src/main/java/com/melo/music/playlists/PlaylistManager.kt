package com.melo.music.playlists

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import org.json.JSONArray
import org.json.JSONObject

/** Плейлист: id, имя и список треков. */
data class Playlist(
    val id: String,
    val name: String,
    val tracks: List<TrackItem>,
) {
    /** Обложка плейлиста — обложка первого трека. */
    val coverUrl: String? get() = tracks.firstOrNull()?.thumbnailUrl
}

/**
 * Плейлисты в SharedPreferences (переживают перезапуск).
 */
object PlaylistManager {

    private const val PREFS = "melo_playlists"
    private const val KEY = "playlists"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getAll(): MutableList<Playlist> {
        val json = prefs?.getString(KEY, null) ?: return mutableListOf()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Playlist(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    tracks = parseTracks(obj.getJSONArray("tracks")),
                )
            }.toMutableList()
        }.getOrDefault(mutableListOf())
    }

    fun create(name: String): Playlist {
        val list = getAll()
        val playlist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name.trim().ifBlank { "Без названия" },
            tracks = emptyList(),
        )
        list.add(playlist)
        save(list)
        return playlist
    }

    fun delete(id: String) {
        save(getAll().filterNot { it.id == id })
    }

    /** Добавляет трек в плейлист (без дублей). Возвращает true, если добавлен. */
    fun addTrack(playlistId: String, track: TrackItem): Boolean {
        val list = getAll()
        val idx = list.indexOfFirst { it.id == playlistId }
        if (idx < 0) return false
        val pl = list[idx]
        if (pl.tracks.any { it.url == track.url }) return false
        list[idx] = pl.copy(tracks = pl.tracks + track)
        save(list)
        return true
    }

    fun removeTrack(playlistId: String, trackUrl: String) {
        val list = getAll()
        val idx = list.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val pl = list[idx]
        list[idx] = pl.copy(tracks = pl.tracks.filterNot { it.url == trackUrl })
        save(list)
    }

    private fun save(list: List<Playlist>) {
        val arr = JSONArray()
        for (pl in list) {
            arr.put(
                JSONObject().apply {
                    put("id", pl.id)
                    put("name", pl.name)
                    put("tracks", serializeTracks(pl.tracks))
                },
            )
        }
        prefs?.edit()?.putString(KEY, arr.toString())?.apply()
    }

    private fun serializeTracks(tracks: List<TrackItem>): JSONArray {
        val arr = JSONArray()
        for (t in tracks) {
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
        return arr
    }

    private fun parseTracks(arr: JSONArray): List<TrackItem> =
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
}
