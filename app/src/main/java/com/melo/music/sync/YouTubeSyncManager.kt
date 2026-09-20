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
        .dns(com.melo.music.net.MeloNet.dns)
        .proxySelector(com.melo.music.net.MeloNet.byedpiSelector)
        .build()

    private fun getBrowseUrl(): String = InnerTubeConfig.getBrowseUrl()

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
            InnerTubeConfig.refresh(force = false)

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
        // 1. Сначала пробуем NewPipe LM
        val viaNewPipe = runCatching {
            val res = kotlinx.coroutines.runBlocking {
                NewPipeResolver.importPlaylist(context, "https://music.youtube.com/playlist?list=LM")
            }
            res.second
        }.getOrNull()

        if (!viaNewPipe.isNullOrEmpty() && viaNewPipe.none { it.title == "Трек" || it.title == "Трек YouTube Music" }) {
            MeloLog.d("YouTubeSync", "Liked Music успешно получены через NewPipe: ${viaNewPipe.size}")
            return viaNewPipe
        }

        // 2. Запрос через InnerTube browse (VLLM или FEmusic_liked_videos)
        for (browseId in listOf("VLLM", "FEmusic_liked_videos")) {
            val bodyJson = createInnerTubeContext().apply {
                put("browseId", browseId)
            }
            val responseJson = postInnerTube(getBrowseUrl(), bodyJson) ?: continue
            parseTracksFromJson(context, responseJson, tracks)
            if (tracks.isNotEmpty()) {
                MeloLog.d("YouTubeSync", "Liked Music успешно получены через $browseId: ${tracks.size}")
                break
            }
        }
        return tracks
    }

    private fun fetchUserPlaylists(context: Context): List<Pair<String, String>> {
        val playlists = mutableListOf<Pair<String, String>>()
        val browseIdsToTry = listOf(
            "FEmusic_library_landing",
            "FEmusic_liked_playlists"
        )

        for (bId in browseIdsToTry) {
            val bodyJson = createInnerTubeContext().apply {
                put("browseId", bId)
            }
            val responseJson = postInnerTube(getBrowseUrl(), bodyJson) ?: continue
            val jsonStr = responseJson.toString()
            MeloLog.d("YouTubeSync", "Playlists ($bId) raw json length: ${jsonStr.length}")

            // 1. Парсинг через musicTwoRowItemRenderer
            val twoRowMarker = "\"musicTwoRowItemRenderer\":"
            var idx = 0
            while (true) {
                val start = jsonStr.indexOf(twoRowMarker, idx)
                if (start == -1) break
                val chunk = jsonStr.substring(start + twoRowMarker.length).take(3000)

                val browseIdMatch = Regex(""""browseId":"(VLPL[a-zA-Z0-9_-]+|PL[a-zA-Z0-9_-]+|FEmusic_library_privately_owned_playlist[a-zA-Z0-9_-]*)"""").find(chunk)
                val titleMatch = Regex(""""title":\{"runs":\[\{"text":"([^"]+)"""").find(chunk)
                    ?: Regex(""""text":"([^"]+)"""").find(chunk)

                if (browseIdMatch != null && titleMatch != null) {
                    val rawBrowseId = browseIdMatch.groupValues[1]
                    val title = titleMatch.groupValues[1]
                    if (title != "Плейлист" && title != "Альбом" && title.isNotBlank()) {
                        val fixedBrowseId = if (rawBrowseId.startsWith("VL")) rawBrowseId else "VL$rawBrowseId"
                        if (playlists.none { it.second == fixedBrowseId }) {
                            playlists.add(title to fixedBrowseId)
                            MeloLog.d("YouTubeSync", "Найден плейлист: «$title» ($fixedBrowseId)")
                        }
                    }
                }
                idx = start + twoRowMarker.length
            }

            // 2. Дополнительный regex поиск любых плейлистов VLPL / PL
            val regex = Regex(""""title":\{"runs":\[\{"text":"([^"]+)"\}\]\}.*?"browseId":"(VLPL[a-zA-Z0-9_-]+|PL[a-zA-Z0-9_-]+)"""")
            regex.findAll(jsonStr).forEach { match ->
                val title = match.groupValues[1]
                val browseId = match.groupValues[2]
                val fixedBrowseId = if (browseId.startsWith("VL")) browseId else "VL$browseId"
                if (playlists.none { it.second == fixedBrowseId }) {
                    playlists.add(title to fixedBrowseId)
                    MeloLog.d("YouTubeSync", "Найден плейлист (regex): «$title» ($fixedBrowseId)")
                }
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

        if (!viaNewPipe.isNullOrEmpty() && viaNewPipe.none { it.title == "Трек" || it.title == "Трек YouTube Music" }) {
            return viaNewPipe
        }

        val bodyJson = createInnerTubeContext().apply {
            put("browseId", browseId)
        }
        val responseJson = postInnerTube(getBrowseUrl(), bodyJson) ?: return emptyList()
        parseTracksFromJson(context, responseJson, tracks)
        return tracks
    }

    private fun parseTracksFromJson(context: Context, root: JSONObject, out: MutableList<TrackItem>) {
        val rootStr = root.toString()
        val marker = "\"musicResponsiveListItemRenderer\":"
        var idx = 0
        while (true) {
            val start = rootStr.indexOf(marker, idx)
            if (start == -1) break
            val sub = rootStr.substring(start + marker.length)
            val chunk = sub.take(4000)

            val videoIdMatch = Regex(""""videoId":"([a-zA-Z0-9_-]{11})"""").find(chunk)
            if (videoIdMatch != null) {
                val videoId = videoIdMatch.groupValues[1]

                // Извлекаем все flexColumns
                val colMarker = "\"musicResponsiveListItemFlexColumnRenderer\":"
                val cols = mutableListOf<String>()
                var colIdx = 0
                while (true) {
                    val cStart = chunk.indexOf(colMarker, colIdx)
                    if (cStart == -1) break
                    val cSub = chunk.substring(cStart + colMarker.length).take(1500)
                    cols.add(cSub)
                    colIdx = cStart + colMarker.length
                }

                var trackTitle: String? = null
                var trackAuthor: String? = null

                if (cols.isNotEmpty()) {
                    // Первая колонка — ВСЕГДА название песни
                    val titleRuns = Regex(""""text":"([^"]+)"""").findAll(cols[0]).map { it.groupValues[1] }.toList()
                    trackTitle = titleRuns.firstOrNull { it.isNotBlank() && it != "•" }
                }

                if (cols.size > 1) {
                    // Вторая колонка — исполнитель, альбом, просмотры, время
                    val authorRuns = Regex(""""text":"([^"]+)"""").findAll(cols[1]).map { it.groupValues[1] }.toList()
                    trackAuthor = authorRuns.firstOrNull { text ->
                        text.isNotBlank() &&
                            text != "•" &&
                            text != "." &&
                            text != trackTitle &&
                            text != "YouTube Music" &&
                            !text.matches(Regex("""\d+:\d+""")) &&
                            !text.contains("просмотр", ignoreCase = true) &&
                            !text.contains("воспроизведен", ignoreCase = true) &&
                            !text.contains("views", ignoreCase = true) &&
                            !text.contains("plays", ignoreCase = true)
                    }
                }

                // Фолбэк на общий поиск runs
                if (trackTitle.isNullOrBlank()) {
                    val allTexts = Regex(""""text":"([^"]+)"""").findAll(chunk).map { it.groupValues[1] }.toList()
                    val filtered = allTexts.filter { text ->
                        text.isNotBlank() &&
                            text != "•" &&
                            text != "." &&
                            text != "YouTube Music" &&
                            !text.matches(Regex("""\d+:\d+""")) &&
                            !text.contains("просмотр", ignoreCase = true) &&
                            !text.contains("воспроизведен", ignoreCase = true)
                    }
                    trackTitle = filtered.firstOrNull() ?: "Трек"
                    trackAuthor = filtered.drop(1).firstOrNull()
                }

                val finalTitle = trackTitle ?: "Трек"
                val finalAuthor = trackAuthor?.takeIf { it.isNotBlank() && it != "." && it != "•" }

                if (out.none { it.url == "https://music.youtube.com/watch?v=$videoId" }) {
                    out.add(
                        TrackItem(
                            title = finalTitle,
                            uploader = finalAuthor,
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

        // Если InnerTube отдал только videoId или названия все еще "Трек",
        // подтягиваем реальные метаданные через NewPipe для первых треков или недостающих
        for (i in out.indices) {
            val item = out[i]
            if (item.title == "Трек" || item.title == "Трек YouTube Music" || item.uploader.isNullOrBlank()) {
                val resolvedMeta = runCatching {
                    kotlinx.coroutines.runBlocking {
                        NewPipeResolver.resolveSingleTrack(context, item.url)
                    }
                }.getOrNull()
                if (resolvedMeta != null && resolvedMeta.title.isNotBlank()) {
                    out[i] = item.copy(
                        title = resolvedMeta.title,
                        uploader = resolvedMeta.uploader ?: item.uploader,
                        durationSeconds = resolvedMeta.durationSeconds,
                        thumbnailUrl = resolvedMeta.thumbnailUrl ?: item.thumbnailUrl
                    )
                }
            }
        }

        if (out.isEmpty()) {
            val videoIdRegex = Regex(""""videoId":"([a-zA-Z0-9_-]{11})"""")
            val ids = videoIdRegex.findAll(rootStr).map { it.groupValues[1] }.distinct().toList()
            ids.take(50).forEach { vid ->
                val trackUrl = "https://music.youtube.com/watch?v=$vid"
                val meta = runCatching {
                    kotlinx.coroutines.runBlocking {
                        NewPipeResolver.resolveSingleTrack(context, trackUrl)
                    }
                }.getOrNull()

                out.add(
                    TrackItem(
                        title = meta?.title ?: "Трек YouTube Music",
                        uploader = meta?.uploader,
                        url = trackUrl,
                        durationSeconds = meta?.durationSeconds ?: 0,
                        thumbnailUrl = meta?.thumbnailUrl ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
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
                    put("clientVersion", InnerTubeConfig.getClientVersion())
                    put("hl", "ru")
                    put("gl", "RU")
                })
            })
        }
    }

    private fun postInnerTube(url: String, json: JSONObject): JSONObject? {
        val cookies = YouTubeAccountManager.getCookies()
        val auth = YouTubeAccountManager.getSapisidHash()
        val clientVer = InnerTubeConfig.getClientVersion()
        MeloLog.d("YouTubeSync", "postInnerTube: url=$url, clientVer=$clientVer, cookiesLen=${cookies?.length ?: 0}, hasAuth=${!auth.isNullOrBlank()}")

        val reqBuilder = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            .addHeader("Referer", "https://music.youtube.com/")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("X-Origin", "https://music.youtube.com")
            .addHeader("X-YouTube-Client-Name", "67")
            .addHeader("X-YouTube-Client-Version", clientVer)

        cookies?.let { reqBuilder.addHeader("Cookie", it) }
        auth?.let { reqBuilder.addHeader("Authorization", it) }

        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                MeloLog.d("YouTubeSync", "postInnerTube ответ код=${resp.code}")
                val bodyStr = resp.body?.string()
                if (!resp.isSuccessful) {
                    MeloLog.e("YouTubeSync", "postInnerTube ошибка HTTP ${resp.code}: ${resp.message} | Тело: $bodyStr")
                    if (resp.code == 401 || resp.code == 403 || resp.code == 404 || (resp.code == 400 && bodyStr?.contains("API_KEY") == true)) {
                        InnerTubeConfig.resetToDefault()
                        InnerTubeConfig.triggerRefreshAsync()
                    }
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
