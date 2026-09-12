package com.melo.desktop.sync

import com.melo.desktop.auth.DesktopYouTubeAuthManager
import com.melo.desktop.extractor.DesktopExtractor
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.net.DesktopMeloNet
import com.melo.desktop.storage.DesktopStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import java.util.concurrent.TimeUnit

/**
 * Синхронизация личной медиатеки из YouTube Music:
 * 1 в 1 с логикой Android версии (YouTubeSyncManager.kt).
 * - Понравившиеся треки (Liked Music / LM / VLLM)
 * - Личные плейлисты пользователя (FEmusic_liked_playlists, FEmusic_library_landing)
 */
object DesktopYouTubeSyncManager {

    data class SyncResult(
        val likedCount: Int,
        val playlistsCount: Int,
        val error: String? = null,
    )

    private val client = DesktopMeloNet.okHttpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private const val INNER_TUBE_KEY = "AIzaSyAO_FJ2SlqAE4Aq4NoXzvDYqBg55UMNy2w"
    private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?key=$INNER_TUBE_KEY&prettyPrint=false"

    suspend fun syncLibrary(
        onProgress: (String) -> Unit = {},
    ): SyncResult = withContext(Dispatchers.IO) {
        println("[DesktopYouTubeSync] Старт синхронизации...")
        if (!DesktopYouTubeAuthManager.isLoggedIn) {
            return@withContext SyncResult(0, 0, "Сначала войдите в аккаунт YouTube Music в настройках")
        }

        try {
            onProgress("Подключение к YouTube Music...")
            warmUpSession()

            onProgress("Загрузка понравившихся треков...")
            val likedTracks = fetchLikedMusic()
            println("[DesktopYouTubeSync] Найдено понравившихся треков: ${likedTracks.size}")

            var addedLikes = 0
            likedTracks.forEach { track ->
                if (!DesktopStorage.isFavorite(track.url)) {
                    DesktopStorage.toggleFavorite(track)
                    addedLikes++
                }
            }

            onProgress("Поиск ваших плейлистов...")
            val ytPlaylists = fetchUserPlaylists()
            println("[DesktopYouTubeSync] Найдено плейлистов: ${ytPlaylists.size}")

            var addedPlaylists = 0
            ytPlaylists.forEach { (name, browseId) ->
                onProgress("Синхронизация «$name»...")
                val tracks = fetchPlaylistTracks(browseId)
                if (tracks.isNotEmpty()) {
                    tracks.forEach { track ->
                        DesktopStorage.addToPlaylist(name, track)
                    }
                    addedPlaylists++
                }
            }

            println("[DesktopYouTubeSync] Синхронизация завершена! Лайков: ${likedTracks.size}, плейлистов: $addedPlaylists")
            SyncResult(likedTracks.size, addedPlaylists)
        } catch (e: Exception) {
            System.err.println("[DesktopYouTubeSync] Ошибка синхронизации: ${e.message}")
            SyncResult(0, 0, e.message ?: "Ошибка синхронизации")
        }
    }

    private fun fetchLikedMusic(): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        // 1. NewPipe LM
        val viaNewPipe = runCatching {
            DesktopExtractor.ensureInit()
            val info = PlaylistInfo.getInfo("https://music.youtube.com/playlist?list=LM")
            info.relatedItems.mapNotNull { item ->
                if (item is org.schabi.newpipe.extractor.stream.StreamInfoItem) {
                    TrackItem(
                        url = item.url,
                        title = item.name,
                        uploader = item.uploaderName,
                        durationSeconds = item.duration,
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url,
                    )
                } else null
            }
        }.getOrNull()

        if (!viaNewPipe.isNullOrEmpty() && viaNewPipe.none { it.title == "Трек" || it.title == "Трек YouTube Music" }) {
            return viaNewPipe
        }

        // 2. InnerTube browse: пробуем FEmusic_liked_videos, затем VLLM
        val browseIds = listOf("FEmusic_liked_videos", "VLLM")
        for (bId in browseIds) {
            val bodyJson = createInnerTubeContext().apply {
                put("browseId", bId)
            }
            val responseJson = postInnerTube(BROWSE_URL, bodyJson)
            if (responseJson != null) {
                parseTracksFromJson(responseJson, tracks)
                if (tracks.isNotEmpty()) break
            }
        }
        return tracks
    }

    private fun fetchUserPlaylists(): List<Pair<String, String>> {
        val playlists = mutableListOf<Pair<String, String>>()
        val browseIdsToTry = listOf(
            "FEmusic_liked_playlists",
            "FEmusic_library_landing",
        )

        for (bId in browseIdsToTry) {
            val bodyJson = createInnerTubeContext().apply {
                put("browseId", bId)
            }
            val responseJson = postInnerTube(BROWSE_URL, bodyJson) ?: continue
            val jsonStr = responseJson.toString()

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
                        }
                    }
                }
                idx = start + twoRowMarker.length
            }

            val regex = Regex(""""title":\{"runs":\[\{"text":"([^"]+)"\}\]\}.*?"browseId":"(VLPL[a-zA-Z0-9_-]+|PL[a-zA-Z0-9_-]+)"""")
            regex.findAll(jsonStr).forEach { match ->
                val title = match.groupValues[1]
                val browseId = match.groupValues[2]
                val fixedBrowseId = if (browseId.startsWith("VL")) browseId else "VL$browseId"
                if (playlists.none { it.second == fixedBrowseId }) {
                    playlists.add(title to fixedBrowseId)
                }
            }
        }
        return playlists
    }

    private fun fetchPlaylistTracks(browseId: String): List<TrackItem> {
        val tracks = mutableListOf<TrackItem>()
        val playlistId = if (browseId.startsWith("VL")) browseId.removePrefix("VL") else browseId
        val viaNewPipe = runCatching {
            DesktopExtractor.ensureInit()
            val info = PlaylistInfo.getInfo("https://music.youtube.com/playlist?list=$playlistId")
            info.relatedItems.mapNotNull { item ->
                if (item is org.schabi.newpipe.extractor.stream.StreamInfoItem) {
                    TrackItem(
                        url = item.url,
                        title = item.name,
                        uploader = item.uploaderName,
                        durationSeconds = item.duration,
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url,
                    )
                } else null
            }
        }.getOrNull()

        if (!viaNewPipe.isNullOrEmpty() && viaNewPipe.none { it.title == "Трек" || it.title == "Трек YouTube Music" }) {
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
        fun searchRenderers(obj: Any) {
            when (obj) {
                is JSONObject -> {
                    if (obj.has("musicResponsiveListItemRenderer")) {
                        val item = obj.optJSONObject("musicResponsiveListItemRenderer")
                        if (item != null) parseSingleTrack(item, out)
                    } else {
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            searchRenderers(obj.get(k))
                        }
                    }
                }
                is org.json.JSONArray -> {
                    for (i in 0 until obj.length()) {
                        searchRenderers(obj.get(i))
                    }
                }
            }
        }

        searchRenderers(root)
    }

    private fun parseSingleTrack(item: JSONObject, out: MutableList<TrackItem>) {
        // 1. videoId
        var videoId: String? = null
        if (item.has("playlistItemData")) {
            videoId = item.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }
        }
        if (videoId.isNullOrBlank()) {
            videoId = item.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")?.takeIf { it.isNotBlank() }
        }
        if (videoId.isNullOrBlank()) {
            videoId = item.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")?.takeIf { it.isNotBlank() }
        }

        // Если это кнопка "Перемешать все" или нет videoId — пропускаем
        if (videoId.isNullOrBlank()) return

        // 2. Название трека и исполнитель
        var title: String? = null
        var author: String? = null
        var durationSeconds = 0

        val flexColumns = item.optJSONArray("flexColumns")
        if (flexColumns != null && flexColumns.length() > 0) {
            // Колонка 0 — название трека
            val col0Runs = flexColumns.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
            if (col0Runs != null && col0Runs.length() > 0) {
                title = col0Runs.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() && it != "•" }
            }

            // Колонка 1 — исполнитель и метаданные
            if (flexColumns.length() > 1) {
                val col1Runs = flexColumns.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                if (col1Runs != null) {
                    for (c in 0 until col1Runs.length()) {
                        val t = col1Runs.optJSONObject(c)?.optString("text")?.trim().orEmpty()
                        if (t.isNotBlank() && t != "•" && t != "." && t != "YouTube Music" && !t.contains("просмотр", ignoreCase = true)) {
                            if (t.matches(Regex("""\d+:\d+"""))) {
                                val parts = t.split(":")
                                if (parts.size == 2) {
                                    durationSeconds = (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
                                }
                            } else if (author == null) {
                                author = t
                            }
                        }
                    }
                }
            }
        }

        // Резервный источник: accessibilityPlayData ("Title - Artist - Duration")
        if (title.isNullOrBlank() || author.isNullOrBlank()) {
            val label = item.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("accessibilityPlayData")
                ?.optJSONObject("accessibilityData")
                ?.optString("label")
            if (!label.isNullOrBlank()) {
                val parts = label.split(" - ")
                if (parts.isNotEmpty() && title.isNullOrBlank()) {
                    title = parts[0].trim()
                }
                if (parts.size > 1 && author.isNullOrBlank()) {
                    author = parts[1].trim()
                }
            }
        }

        // Обложка трека
        var thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        val thumbnails = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        if (thumbnails != null && thumbnails.length() > 0) {
            val lastUrl = thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
            if (!lastUrl.isNullOrBlank()) thumb = lastUrl
        }

        val finalTitle = title?.takeIf { it.isNotBlank() } ?: "Трек"
        val finalAuthor = author?.takeIf { it.isNotBlank() } ?: "Исполнитель"
        val trackUrl = "https://www.youtube.com/watch?v=$videoId"

        if (out.none { it.url == trackUrl }) {
            out.add(
                TrackItem(
                    url = trackUrl,
                    title = finalTitle,
                    uploader = finalAuthor,
                    durationSeconds = durationSeconds.toLong(),
                    thumbnailUrl = thumb,
                )
            )
        }
    }

    private fun createInnerTubeContext(): JSONObject {
        return JSONObject().apply {
            put(
                "context",
                JSONObject().apply {
                    put(
                        "client",
                        JSONObject().apply {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", "1.20240101.01.00")
                            put("hl", "ru")
                            put("gl", "RU")
                        }
                    )
                }
            )
        }
    }

    private fun postInnerTube(url: String, body: JSONObject): JSONObject? {
        return try {
            val reqBuilder = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
                .header("Referer", "https://music.youtube.com/")
                .header("Origin", "https://music.youtube.com")
                .header("X-Origin", "https://music.youtube.com")
                .header("X-YouTube-Client-Name", "67")
                .header("X-YouTube-Client-Version", "1.20240101.01.00")
                .header("X-Goog-AuthUser", "0")

            DesktopYouTubeAuthManager.getCookies()?.let {
                reqBuilder.header("Cookie", it)
            }
            DesktopYouTubeAuthManager.getSapisidHash()?.let {
                reqBuilder.header("Authorization", it)
            }

            val resp = client.newCall(reqBuilder.build()).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string().orEmpty()
                JSONObject(bodyStr)
            } else {
                val errBody = resp.body?.string().orEmpty().take(500)
                System.err.println("[DesktopYouTubeSync] InnerTube response error: HTTP ${resp.code} for URL: $url, details: $errBody")
                null
            }
        } catch (e: Exception) {
            System.err.println("[DesktopYouTubeSync] InnerTube network error: ${e.message}")
            null
        }
    }

    private fun warmUpSession() {
        val currentCookies = DesktopYouTubeAuthManager.getCookies() ?: return
        try {
            val req = Request.Builder()
                .url("https://music.youtube.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
                .header("Cookie", currentCookies)
                .build()

            val resp = client.newCall(req).execute()
            val setCookies = resp.headers("Set-Cookie")
            if (setCookies.isNotEmpty()) {
                val cookieMap = mutableMapOf<String, String>()
                currentCookies.split(";").forEach { c ->
                    if (c.contains("=")) {
                        val k = c.substringBefore("=").trim()
                        val v = c.substringAfter("=").trim()
                        if (k.isNotBlank()) cookieMap[k] = v
                    }
                }
                setCookies.forEach { sc ->
                    val pair = sc.substringBefore(";")
                    if (pair.contains("=")) {
                        val k = pair.substringBefore("=").trim()
                        val v = pair.substringAfter("=").trim()
                        if (k.isNotBlank()) cookieMap[k] = v
                    }
                }
                val merged = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
                DesktopYouTubeAuthManager.saveSession(merged)
            }
            resp.close()
        } catch (e: Exception) {
            System.err.println("[DesktopYouTubeSync] Warmup error: ${e.message}")
        }
    }
}
