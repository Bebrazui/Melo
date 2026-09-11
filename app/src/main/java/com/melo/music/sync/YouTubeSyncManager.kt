package com.melo.music.sync

import android.content.Context
import com.melo.music.auth.YouTubeAccountManager
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.NewPipeResolver
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager
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

    suspend fun syncLibrary(
        context: Context,
        onProgress: (String) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!YouTubeAccountManager.isLoggedIn) {
            return@withContext SyncResult(0, 0, "Сначала войдите в Google аккаунт в настройках")
        }

        try {
            onProgress("Загрузка понравившихся треков...")
            val likedTracks = fetchLikedMusic(context)
            var addedLikes = 0
            likedTracks.forEach { track ->
                if (!FavoritesManager.isLiked(track.url)) {
                    FavoritesManager.toggle(track)
                    addedLikes++
                }
            }

            onProgress("Поиск ваших плейлистов...")
            val ytPlaylists = fetchUserPlaylists(context)
            var addedPlaylists = 0

            ytPlaylists.forEach { (name, browseId) ->
                onProgress("Синхронизация «$name»...")
                val tracks = fetchPlaylistTracks(context, browseId)
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
            android.util.Log.e("MeloSync", "syncLibrary error: ${e.message}", e)
            SyncResult(0, 0, e.message ?: "Ошибка синхронизации")
        }
    }

    private fun fetchLikedMusic(context: Context): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        // 1. Попытка через NewPipe плейлист LM / LL (Likes)
        val viaNewPipe = runCatching {
            val res = kotlinx.coroutines.runBlocking {
                NewPipeResolver.importPlaylist(context, "https://music.youtube.com/playlist?list=LM")
            }
            res.second
        }.getOrNull()

        if (!viaNewPipe.isNullOrEmpty()) {
            return viaNewPipe
        }

        // 2. Запрос через InnerTube browse
        val bodyJson = createInnerTubeContext().apply {
            put("browseId", "FEmusic_liked_videos")
        }
        val responseJson = postInnerTube(BROWSE_URL, bodyJson) ?: return emptyList()
        parseTracksFromJson(responseJson, tracks)
        return tracks
    }

    private fun fetchUserPlaylists(context: Context): List<Pair<String, String>> {
        val playlists = mutableListOf<Pair<String, String>>()
        val bodyJson = createInnerTubeContext().apply {
            put("browseId", "FEmusic_library_playlists")
        }
        val responseJson = postInnerTube(BROWSE_URL, bodyJson) ?: return emptyList()

        val jsonStr = responseJson.toString()
        // Регулярка для извлечения названия и browseId плейлистов
        val regex = Regex(""""title":\{"runs":\[\{"text":"([^"]+)"\}\]\}.*?"browseId":"(VLPL[^"]+|FEmusic_library_privately_owned_playlist[^"]+|PL[^"]+)"""")
        regex.findAll(jsonStr).forEach { match ->
            val title = match.groupValues[1]
            val browseId = match.groupValues[2]
            if (playlists.none { it.second == browseId }) {
                playlists.add(title to browseId)
            }
        }
        return playlists
    }

    private fun fetchPlaylistTracks(context: Context, browseId: String): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val playlistId = if (browseId.startsWith("VL")) browseId.removePrefix("VL") else browseId
        val viaNewPipe = runCatching {
            val res = kotlinx.coroutines.runBlocking {
                NewPipeResolver.importPlaylist(context, "https://music.youtube.com/playlist?list=$playlistId")
            }
            res.second
        }.getOrNull()

        if (!viaNewPipe.isNullOrEmpty()) {
            return viaNewPipe
        }

        val bodyJson = createInnerTubeContext().apply {
            put("browseId", browseId)
        }
        val responseJson = postInnerTube(BROWSE_URL, bodyJson) ?: return emptyList()
        parseTracksFromJson(responseJson, tracks)
        return tracks
    }

    private fun parseTracksFromJson(root: JSONObject, out: MutableList<TrackItem>) {
        val rootStr = root.toString()
        val musicResponsiveItemRegex = Regex(""""musicResponsiveListItemRenderer":\{(.*?)\}\}\}\}""")
        val items = musicResponsiveItemRegex.findAll(rootStr).map { it.groupValues[1] }.toList()

        if (items.isNotEmpty()) {
            for (item in items) {
                val videoIdMatch = Regex(""""videoId":"([a-zA-Z0-9_-]{11})"""").find(item)
                val videoId = videoIdMatch?.groupValues?.get(1) ?: continue
                val titleMatch = Regex(""""text":"([^"]+)"""").find(item)
                val title = titleMatch?.groupValues?.get(1) ?: "Трек"

                out.add(
                    TrackItem(
                        title = title,
                        uploader = "YouTube Music",
                        url = "https://music.youtube.com/watch?v=$videoId",
                        durationSeconds = 0,
                        thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                        source = Source.YOUTUBE_MUSIC,
                        kind = ItemKind.TRACK
                    )
                )
            }
        } else {
            val videoIdRegex = Regex(""""videoId":"([a-zA-Z0-9_-]{11})"""")
            val ids = videoIdRegex.findAll(rootStr).map { it.groupValues[1] }.distinct().toList()
            ids.forEach { vid ->
                out.add(
                    TrackItem(
                        title = "Трек YouTube Music",
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
        val cookies = YouTubeAccountManager.getCookies()
        val auth = YouTubeAccountManager.getSapisidHash()
        android.util.Log.e("MeloSync", "postInnerTube: url=$url, cookiesLen=${cookies?.length ?: 0}, hasAuth=${!auth.isNullOrBlank()}")

        val reqBuilder = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
            .addHeader("Referer", "https://music.youtube.com/")
            .addHeader("X-Origin", "https://music.youtube.com")

        cookies?.let { reqBuilder.addHeader("Cookie", it) }
        auth?.let { reqBuilder.addHeader("Authorization", it) }

        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                android.util.Log.e("MeloSync", "postInnerTube response code=${resp.code}")
                if (!resp.isSuccessful) return null
                val bodyStr = resp.body?.string() ?: return null
                android.util.Log.e("MeloSync", "postInnerTube body len=${bodyStr.length}")
                JSONObject(bodyStr)
            }
        } catch (e: Exception) {
            android.util.Log.e("MeloSync", "postInnerTube error: ${e.message}", e)
            null
        }
    }
}
