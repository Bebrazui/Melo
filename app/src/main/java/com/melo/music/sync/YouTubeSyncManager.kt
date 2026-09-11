package com.melo.music.sync

import android.content.Context
import com.melo.music.auth.YouTubeAccountManager
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.NewPipeResolver
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager
import com.melo.music.playlists.PlaylistManager
import com.melo.music.util.MeloLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    private const val INNER_TUBE_KEY = "AIzaSyAO_FJ2SlqAE4Aq4NoXzvDYqBg55UMNy2w"
    private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?key=$INNER_TUBE_KEY&prettyPrint=false"

    suspend fun syncLibrary(
        context: Context,
        onProgress: (String) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        MeloLog.d("YouTubeSync", "Старт синхронизации...")
        if (!YouTubeAccountManager.isLoggedIn) {
            MeloLog.e("YouTubeSync", "Пользователь не авторизован в YouTubeAccountManager")
            return@withContext SyncResult(0, 0, "Сначала войдите в Google аккаунт в настройках")
        }

        val cookies = YouTubeAccountManager.getCookies()
        MeloLog.d("YouTubeSync", "Куки найдены: длина=${cookies?.length ?: 0}")

        try {
            onProgress("Загрузка понравившихся треков...")
            MeloLog.d("YouTubeSync", "Запрос понравившихся треков...")
            val likedTracks = fetchLikedMusic(context)
            MeloLog.d("YouTubeSync", "Найдено понравившихся треков: ${likedTracks.size}")

            var addedLikes = 0
            likedTracks.forEach { track ->
                if (!FavoritesManager.isLiked(track.url)) {
                    FavoritesManager.toggle(track)
                    addedLikes++
                }
            }

            onProgress("Поиск ваших плейлистов...")
            MeloLog.d("YouTubeSync", "Запрос списка плейлистов библиотеки...")
            val ytPlaylists = fetchUserPlaylists(context)
            MeloLog.d("YouTubeSync", "Найдено плейлистов: ${ytPlaylists.size}")

            var addedPlaylists = 0
            ytPlaylists.forEach { (name, browseId) ->
                onProgress("Синхронизация «$name»...")
                MeloLog.d("YouTubeSync", "Загрузка плейлиста «$name» (id=$browseId)...")
                val tracks = fetchPlaylistTracks(context, browseId)
                MeloLog.d("YouTubeSync", "Плейлист «$name»: треков=${tracks.size}")
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

            MeloLog.d("YouTubeSync", "Синхронизация завершена успешно! Лайков: ${likedTracks.size}, плейлистов: $addedPlaylists")
            SyncResult(likedTracks.size, addedPlaylists)
        } catch (e: Exception) {
            MeloLog.e("YouTubeSync", "Сбой синхронизации: ${e.message}", e)
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
            MeloLog.d("YouTubeSync", "Liked Music успешно получены через NewPipe: ${viaNewPipe.size}")
            return viaNewPipe
        }

        // 2. Запрос через InnerTube browse (VLLM = full Liked Music playlist)
        val bodyJson = createInnerTubeContext().apply {
            put("browseId", "VLLM")
        }
        val responseJson = postInnerTube(BROWSE_URL, bodyJson) ?: return emptyList()
        parseTracksFromJson(responseJson, tracks)
        return tracks
    }

    private fun fetchUserPlaylists(context: Context): List<Pair<String, String>> {
        val playlists = mutableListOf<Pair<String, String>>()
        val bodyJson = createInnerTubeContext().apply {
            put("browseId", "FEmusic_liked_playlists")
        }
        val responseJson = postInnerTube(BROWSE_URL, bodyJson) ?: return emptyList()

        val jsonStr = responseJson.toString()
        MeloLog.d("YouTubeSync", "Playlists raw json length: ${jsonStr.length}, sample: ${jsonStr.take(300)}")
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
        // Ищем каждый блок musicResponsiveListItemRenderer
        val marker = "\"musicResponsiveListItemRenderer\":"
        var idx = 0
        while (true) {
            val start = rootStr.indexOf(marker, idx)
            if (start == -1) break
            val sub = rootStr.substring(start + marker.length)
            val chunk = sub.take(2000)

            val videoIdMatch = Regex(""""videoId":"([a-zA-Z0-9_-]{11})"""").find(chunk)
            if (videoIdMatch != null) {
                val videoId = videoIdMatch.groupValues[1]

                // Название трека: первое поле text в flexColumns[0]
                val titleMatch = Regex(""""text":"([^"]+)"""").find(chunk)
                val rawTitle = titleMatch?.groupValues?.get(1) ?: "Трек"

                // Исполнитель: обычно в flexColumns[1] или последующих text
                val allTexts = Regex(""""text":"([^"]+)"""").findAll(chunk)
                    .map { it.groupValues[1] }
                    .filter { text ->
                        text != rawTitle &&
                            text != "•" &&
                            text != "YouTube Music" &&
                            !text.matches(Regex("""\d+:\d+""")) &&
                            !text.contains("просмотр", ignoreCase = true) &&
                            !text.contains("воспроизведен", ignoreCase = true)
                    }
                    .toList()

                val author = allTexts.firstOrNull()?.takeIf { it.isNotBlank() }

                if (out.none { it.url == "https://music.youtube.com/watch?v=$videoId" }) {
                    out.add(
                        TrackItem(
                            title = rawTitle,
                            uploader = author,
                            url = "https://music.youtube.com/watch?v=$videoId",
                            durationSeconds = 0,
                            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                            source = Source.YOUTUBE_MUSIC,
                            kind = ItemKind.TRACK
                        )
                    )
                }
            }
            idx = start + marker.length
        }

        if (out.isEmpty()) {
            val videoIdRegex = Regex(""""videoId":"([a-zA-Z0-9_-]{11})"""")
            val ids = videoIdRegex.findAll(rootStr).map { it.groupValues[1] }.distinct().toList()
            ids.forEach { vid ->
                out.add(
                    TrackItem(
                        title = "Трек YouTube Music",
                        uploader = null,
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
        MeloLog.d("YouTubeSync", "postInnerTube: url=$url, cookiesLen=${cookies?.length ?: 0}, hasAuth=${!auth.isNullOrBlank()}")

        val reqBuilder = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
            .addHeader("Referer", "https://music.youtube.com/")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("X-Origin", "https://music.youtube.com")
            .addHeader("X-YouTube-Client-Name", "67")
            .addHeader("X-YouTube-Client-Version", "1.20240101.01.00")

        cookies?.let { reqBuilder.addHeader("Cookie", it) }
        auth?.let { reqBuilder.addHeader("Authorization", it) }

        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                MeloLog.d("YouTubeSync", "postInnerTube ответ код=${resp.code}")
                val bodyStr = resp.body?.string()
                if (!resp.isSuccessful) {
                    MeloLog.e("YouTubeSync", "postInnerTube ошибка HTTP ${resp.code}: ${resp.message} | Тело: $bodyStr")
                    return null
                }
                if (bodyStr.isNullOrEmpty()) return null
                MeloLog.d("YouTubeSync", "postInnerTube ответ body=${bodyStr.take(150)}...")
                JSONObject(bodyStr)
            }
        } catch (e: Exception) {
            MeloLog.e("YouTubeSync", "postInnerTube сетевое исключение: ${e.message}", e)
            null
        }
    }
}
