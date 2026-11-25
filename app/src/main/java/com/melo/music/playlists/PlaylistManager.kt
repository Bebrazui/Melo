package com.melo.music.playlists

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.db.MeloDatabase
import com.melo.music.db.PlaylistEntity
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

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
 * Плейлисты на Room Database с in-memory кэшем.
 * Автоматически мигрирует данные из SharedPreferences.
 */
object PlaylistManager {

    private const val PREFS = "melo_playlists"
    private const val KEY = "playlists"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private var database: MeloDatabase? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val inMemoryPlaylists = CopyOnWriteArrayList<Playlist>()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        appContext = app
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        val db = MeloDatabase.getInstance(app)
        database = db

        scope.launch {
            val dao = db.playlistDao()

            // 1. Миграция старых плейлистов из SharedPreferences
            val oldJson = p.getString(KEY, null)
            if (!oldJson.isNullOrBlank()) {
                val legacy = parseLegacyPlaylists(oldJson)
                if (legacy.isNotEmpty()) {
                    dao.insertAll(legacy.map {
                        PlaylistEntity(
                            id = it.id,
                            name = it.name,
                            isPublic = it.isPublic,
                            tracksJson = serializeTracks(it.tracks)
                        )
                    })
                }
                p.edit().remove(KEY).apply()
            }

            // 2. Загрузка в память
            val fromDb = dao.getAll().map {
                Playlist(
                    id = it.id,
                    name = it.name,
                    tracks = parseTracks(it.tracksJson),
                    isPublic = it.isPublic
                )
            }
            inMemoryPlaylists.clear()
            inMemoryPlaylists.addAll(fromDb)
        }
        initialized = true
    }

    fun getAll(): MutableList<Playlist> = ArrayList(inMemoryPlaylists)

    fun create(name: String): Playlist {
        val playlist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name.trim().ifBlank { "Без названия" },
            tracks = emptyList(),
        )
        inMemoryPlaylists.add(playlist)
        saveDb(playlist)
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
        return playlist
    }

    fun delete(id: String) {
        inMemoryPlaylists.removeAll { it.id == id }
        scope.launch {
            database?.playlistDao()?.deleteById(id)
        }
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
    }

    /** Полная замена локального списка (используется облачным синком). */
    fun replaceAll(list: List<Playlist>) {
        inMemoryPlaylists.clear()
        inMemoryPlaylists.addAll(list)
        scope.launch {
            val dao = database?.playlistDao() ?: return@launch
            dao.clearAll()
            dao.insertAll(list.map {
                PlaylistEntity(
                    id = it.id,
                    name = it.name,
                    isPublic = it.isPublic,
                    tracksJson = serializeTracks(it.tracks)
                )
            })
        }
    }

    fun setPublic(id: String, value: Boolean) {
        val idx = inMemoryPlaylists.indexOfFirst { it.id == id }
        if (idx < 0) return
        val updated = inMemoryPlaylists[idx].copy(isPublic = value)
        inMemoryPlaylists[idx] = updated
        saveDb(updated)
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
    }

    fun addTrack(playlistId: String, track: TrackItem): Boolean {
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

        val updated = pl.copy(tracks = pl.tracks + track)
        inMemoryPlaylists[idx] = updated
        saveDb(updated)
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
        appContext?.let { ctx -> Extractor.prefetch(ctx, track.url) }
        return true
    }

    fun removeTrack(playlistId: String, trackUrl: String) {
        val idx = inMemoryPlaylists.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val pl = inMemoryPlaylists[idx]
        val updated = pl.copy(tracks = pl.tracks.filterNot { it.url == trackUrl })
        inMemoryPlaylists[idx] = updated
        saveDb(updated)
        com.melo.music.sync.LibrarySync.onPlaylistsChanged()
    }

    private fun saveDb(playlist: Playlist) {
        scope.launch {
            database?.playlistDao()?.insert(
                PlaylistEntity(
                    id = playlist.id,
                    name = playlist.name,
                    isPublic = playlist.isPublic,
                    tracksJson = serializeTracks(playlist.tracks)
                )
            )
        }
    }

    private fun serializeTracks(tracks: List<TrackItem>): String {
        val arr = JSONArray()
        for (t in tracks) {
            arr.put(JSONObject().apply {
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
            })
        }
        return arr.toString()
    }

    private fun parseTracks(json: String): List<TrackItem> = runCatching {
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
                speed = obj.optDouble("speed", 1.0).toFloat(),
                bassBoost = obj.optBoolean("bassBoost", false),
                crush = obj.optDouble("crush", 0.0).toFloat(),
                pitchSemi = obj.optDouble("pitchSemi", 0.0).toFloat(),
                noteShiftSeed = obj.optLong("noteShiftSeed", 0L),
            )
        }
    }.getOrDefault(emptyList())

    private fun parseLegacyPlaylists(json: String): List<Playlist> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            Playlist(
                id = obj.getString("id"),
                name = obj.getString("name"),
                tracks = parseTracks(obj.getJSONArray("tracks").toString()),
                isPublic = obj.optBoolean("isPublic", true),
            )
        }
    }.getOrDefault(emptyList())
}
