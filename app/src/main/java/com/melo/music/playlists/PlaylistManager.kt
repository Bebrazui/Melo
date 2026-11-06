package com.melo.music.playlists

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.extractor.Extractor
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
    /** Открытый плейлист виден другим пользователям. По умолчанию открыт. */
    val isPublic: Boolean = true,
) {
    /** Обложка плейлиста — обложка первого трека. */
    val coverUrl: String? get() = tracks.firstOrNull()?.thumbnailUrl
}

/**
 * Плейлисты в SharedPreferences с in-memory кэшем (переживают перезапуск).
 */
object PlaylistManager {

    private const val PREFS = "melo_playlists"
    private const val KEY = "playlists"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private val inMemoryPlaylists = mutableListOf<Playlist>()
    private var initialized = false

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        loadAll(p)
    }

    @Synchronized
    private fun loadAll(p: SharedPreferences) {
        if (initialized) return
        inMemoryPlaylists.clear()
        val json = p.getString(KEY, null)
        if (!json.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    inMemoryPlaylists.add(
                        Playlist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            tracks = parseTracks(obj.getJSONArray("tracks")),
                            isPublic = obj.optBoolean("isPublic", true),
                        ),
                    )
                }
            }
        }
        initialized = true
    }

    private fun ensureLoaded() {
        if (!initialized) {
            prefs?.let { loadAll(it) }
        }
    }

    @Synchronized
    fun getAll(): MutableList<Playlist> {
        ensureLoaded()
        return inMemoryPlaylists.map { it.copy() }.toMutableList()
    }

    @Synchronized
    fun create(name: String): Playlist {
        ensureLoaded()
        val playlist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name.trim().ifBlank { "Без названия" },
            tracks = emptyList(),
        )
        inMemoryPlaylists.add(playlist)
        save(inMemoryPlaylists)
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
        return playlist
    }

    @Synchronized
    fun delete(id: String) {
        ensureLoaded()
        inMemoryPlaylists.removeAll { it.id == id }
        save(inMemoryPlaylists)
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
    }

    /** Полная замена локального списка (используется облачным синком — без обратной выгрузки). */
    @Synchronized
    fun replaceAll(list: List<Playlist>) {
        ensureLoaded()
        inMemoryPlaylists.clear()
        inMemoryPlaylists.addAll(list)
        save(inMemoryPlaylists)
    }

    /** Открыть/закрыть плейлист для других пользователей. */
    @Synchronized
    fun setPublic(id: String, value: Boolean) {
        ensureLoaded()
        val idx = inMemoryPlaylists.indexOfFirst { it.id == id }
        if (idx < 0) return
        inMemoryPlaylists[idx] = inMemoryPlaylists[idx].copy(isPublic = value)
        save(inMemoryPlaylists)
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
    }

    /** Добавляет трек в плейлист (без дублей). Возвращает true, если добавлен. */
    @Synchronized
    fun addTrack(playlistId: String, track: TrackItem): Boolean {
        ensureLoaded()
        val idx = inMemoryPlaylists.indexOfFirst { it.id == playlistId }
        if (idx < 0) return false
        val pl = inMemoryPlaylists[idx]
        if (pl.tracks.any {
            it.url == track.url &&
                kotlin.math.abs(it.speed - track.speed) < 0.01f &&
                it.bassBoost == track.bassBoost &&
                kotlin.math.abs(it.crush - track.crush) < 0.01f &&
                kotlin.math.abs(it.pitchSemi - track.pitchSemi) < 0.01f &&
                it.noteShiftSeed == track.noteShiftSeed
        }) return false
        inMemoryPlaylists[idx] = pl.copy(tracks = pl.tracks + track)
        save(inMemoryPlaylists)
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
        // Заранее резолвим прямую ссылку → воспроизведение из плейлиста мгновенное.
        appContext?.let { ctx -> Extractor.prefetch(ctx, track.url) }
        return true
    }

    @Synchronized
    fun removeTrack(playlistId: String, trackUrl: String) {
        ensureLoaded()
        val idx = inMemoryPlaylists.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val pl = inMemoryPlaylists[idx]
        inMemoryPlaylists[idx] = pl.copy(tracks = pl.tracks.filterNot { it.url == trackUrl })
        save(inMemoryPlaylists)
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
    }

    private fun save(list: List<Playlist>) {
        val arr = JSONArray()
        for (pl in list) {
            arr.put(
                JSONObject().apply {
                    put("id", pl.id)
                    put("name", pl.name)
                    put("tracks", serializeTracks(pl.tracks))
                    put("isPublic", pl.isPublic)
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
                    put("speed", t.speed.toDouble())
                    put("bassBoost", t.bassBoost)
                    put("crush", t.crush.toDouble())
                    put("pitchSemi", t.pitchSemi.toDouble())
                    put("noteShiftSeed", t.noteShiftSeed)
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
                speed = obj.optDouble("speed", 1.0).toFloat(),
                bassBoost = obj.optBoolean("bassBoost", false),
                crush = obj.optDouble("crush", 0.0).toFloat(),
                pitchSemi = obj.optDouble("pitchSemi", 0.0).toFloat(),
                noteShiftSeed = obj.optLong("noteShiftSeed", 0L),
            )
        }
}
