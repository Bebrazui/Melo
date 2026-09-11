package com.melo.music.sync

import android.content.Context
import com.melo.music.auth.YouTubeAccountManager
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager
import com.melo.music.playlists.Playlist
import com.melo.music.playlists.PlaylistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Синхронизация личной медиатеки из YouTube Music:
 * - Понравившиеся треки (Liked Music / LM)
 * - Личные плейлисты пользователя
 */
object YouTubeSyncManager {

    data class SyncResult(
        val likedCount: Int,
        val playlistsCount: Int,
        val error: String? = null
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .proxySelector(com.melo.music.net.MeloNet.byedpiSelector)
        .build()

    private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"

    /**
     * Выполняет полный синк лайков и плейлистов из YouTube Music.
     */
    suspend fun syncLibrary(
        context: Context,
        onProgress: (String) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!YouTubeAccountManager.isLoggedIn) {
            return@withContext SyncResult(0, 0, "Сначала выполните вход в Google аккаунт")
        }

        try {
            onProgress("Загрузка понравившихся треков...")
            val likedTracks = fetchLikedMusic()
            var addedLikes = 0
            likedTracks.forEach { track ->
                if (!FavoritesManager.isLiked(track.url)) {
                    FavoritesManager.toggle(track)
                    addedLikes++
                }
            }

            onProgress("Загрузка ваших плейлистов...")
            val ytPlaylists = fetchUserPlaylists()
            var addedPlaylists = 0

            ytPlaylists.forEach { (name, browseId) ->
                onProgress("Синхронизация «$name»...")
                val tracks = fetchPlaylistTracks(browseId)
                if (tracks.isNotEmpty()) {
                    val existing = PlaylistManager.getAll().find { it.name.equals(name, ignoreCase = true) }
                    if (existing != null) {
                        tracks.forEach { PlaylistManager.addTrack(existing.id, it) }
                    } else {
                        val newPl = PlaylistManager.create(name)
                        tracks.forEach { PlaylistManager.addTrack(newPl.id, it) }
                    }
                    addedPlaylists++
                }
            }

            SyncResult(likedTracks.size, addedPlaylists)
        } catch (e: Exception) {
            SyncResult(0, 0, e.message ?: "Ошибка синхронизации")
        }
    }

    /**
     * Тянет треки из автоплейлиста "LM" (Liked Music).
     */
    private fun fetchLikedMusic(): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val bodyJson = createInnerTubeContext().apply {
            put("browseId", "FEmusic_liked_videos")
        }
        val responseJson = postInnerTube(BROWSE_URL, bodyJson) ?: return emptyList()

        parseTracksFromBrowse(responseJson, tracks)
        return tracks
    }

    /**
     * Тянет список личных плейлистов (имя -> browseId).
     */
    private fun fetchUserPlaylists(): List<Pair<String, String>> {
        val playlists = mutableListOf<Pair<String, String>>()
        val bodyJson = createInnerTubeContext().apply {
            put("browseId", "FEmusic_library_playlists")
        }
        val responseJson = postInnerTube(BROWSE_URL, bodyJson) ?: return emptyList()

        val jsonStr = responseJson.toString()
        // Ищем все вхождения плейлистов пользователя VLPL...
        val regex = Regex(""""title":\{"runs":\[\{"text":"([^"]+)"\}\]\}.*?"browseId":"(VLPL[^"]+)"""")
        regex.findAll(jsonStr).forEach { match ->
            val title = match.groupValues[1]
            val browseId = match.groupValues[2]
            if (playlists.none { it.second == browseId }) {
                playlists.add(title to browseId)
            }
        }
        return playlists
    }

    /**
     * Загружает треки конкретного плейлиста.
     */
    private fun fetchPlaylistTracks(browseId: String): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val bodyJson = createInnerTubeContext().apply {
            put("browseId", browseId)
        }
        val responseJson = postInnerTube(BROWSE_URL, bodyJson) ?: return emptyList()
        parseTracksFromBrowse(responseJson, tracks)
        return tracks
    }

    private fun parseTracksFromBrowse(root: JSONObject, out: MutableList<TrackItem>) {
        val rootStr = root.toString()
        // Извлекаем videoId, title, author
        val videoIdRegex = Regex(""""videoId":"([a-zA-Z0-9_-]{11})"""")
        val ids = videoIdRegex.findAll(rootStr).map { it.groupValues[1] }.distinct().toList()

        ids.forEach { vid ->
            if (vid.length == 11 && !vid.startsWith("FEmusic") && !vid.startsWith("VLPL")) {
                // Ищем title рядом с videoId если возможно
                out.add(
                    TrackItem(
                        title = "YouTube Music Track",
                        uploader = "YouTube Music",
                        url = "https://music.youtube.com/watch?v=$vid",
                        durationSeconds = 0,
                        thumbnailUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                        source = Source.YOUTUBE_MUSIC,
                        kind = ItemKind.TRACK
                    )
                )
            }
        }
    }

    private fun createInnerTubeContext(): JSONObject {
        return JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240101.01.00")
                    put("hl", "ru")
                    put("gl", "RU")
                })
            })
        }
    }

    private fun postInnerTube(url: String, json: JSONObject): JSONObject? {
        val reqBuilder = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
            .addHeader("Referer", "https://music.youtube.com/")
            .addHeader("X-Origin", "https://music.youtube.com")

        YouTubeAccountManager.getCookies()?.let { reqBuilder.addHeader("Cookie", it) }
        YouTubeAccountManager.getSapisidHash()?.let { reqBuilder.addHeader("Authorization", it) }

        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bodyStr = resp.body?.string() ?: return null
                JSONObject(bodyStr)
            }
        } catch (e: Exception) {
            null
        }
    }
}
