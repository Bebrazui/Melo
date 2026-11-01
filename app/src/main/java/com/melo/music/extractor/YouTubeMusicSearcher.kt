package com.melo.music.extractor

import android.content.Context
import com.melo.music.auth.YouTubeAccountManager
import com.melo.music.sync.InnerTubeConfig
import com.melo.music.util.MeloLog
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
 * Нативный поиск YouTube Music через InnerTube API.
 * Использует официальные ранжирование и рекомендации Google.
 */
object YouTubeMusicSearcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .dns(com.melo.music.net.MeloNet.dns)
        .proxySelector(com.melo.music.net.MeloNet.byedpiSelector)
        .build()

    suspend fun search(query: String): List<TrackItem> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        try {
            val url = InnerTubeConfig.getSearchUrl()
            val clientVer = InnerTubeConfig.getClientVersion()
            val cookies = YouTubeAccountManager.getCookies()
            val auth = YouTubeAccountManager.getSapisidHash()

            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", clientVer)
                        put("hl", "ru")
                        put("gl", "RU")
                    })
                })
                put("query", q)
            }

            val reqBuilder = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .addHeader("Referer", "https://music.youtube.com/")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("X-Origin", "https://music.youtube.com")
                .addHeader("X-YouTube-Client-Name", "67")
                .addHeader("X-YouTube-Client-Version", clientVer)

            cookies?.let { reqBuilder.addHeader("Cookie", it) }
            auth?.let { reqBuilder.addHeader("Authorization", it) }

            val response = client.newCall(reqBuilder.build()).execute()
            val bodyStr = response.body?.string()
            if (!response.isSuccessful || bodyStr.isNullOrEmpty()) {
                MeloLog.d("YTSearch", "InnerTube search failed with code ${response.code}")
                return@withContext emptyList()
            }

            parseSearchResults(JSONObject(bodyStr))
        } catch (e: Exception) {
            MeloLog.e("YTSearch", "Search exception: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseSearchResults(root: JSONObject): List<TrackItem> {
        val results = mutableListOf<TrackItem>()
        val seenUrls = mutableSetOf<String>()

        fun addTrack(item: TrackItem) {
            if (item.url.isNotBlank() && seenUrls.add(item.url)) {
                results.add(item)
            }
        }

        fun traverse(element: Any) {
            when (element) {
                is JSONObject -> {
                    // 1. Лучший результат (Top result card)
                    if (element.has("musicCardShelfRenderer")) {
                        parseTopResultCard(element.getJSONObject("musicCardShelfRenderer"))?.let { addTrack(it) }
                    }

                    // 2. Стандартная строка (Песня / Видео / Альбом / Артист)
                    if (element.has("musicResponsiveListItemRenderer")) {
                        parseListItem(element.getJSONObject("musicResponsiveListItemRenderer"))?.let { addTrack(it) }
                    }

                    val keys = element.keys()
                    while (keys.hasNext()) {
                        traverse(element.get(keys.next()))
                    }
                }
                is JSONArray -> {
                    for (i in 0 until element.length()) {
                        traverse(element.get(i))
                    }
                }
            }
        }

        traverse(root)
        return results
    }

    /** Парсинг карточки "Лучший результат" (Top Result). */
    private fun parseTopResultCard(card: JSONObject): TrackItem? {
        val title = extractText(card.optJSONObject("title")) ?: return null

        // Проверяем videoId для трека
        val videoId = card.optJSONObject("onTap")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")?.takeIf { it.isNotBlank() }

        // Проверяем browseId для артиста или альбома
        val browseEndpoint = card.optJSONObject("onTap")?.optJSONObject("browseEndpoint")
        val browseId = browseEndpoint?.optString("browseId")
        val pageType = browseEndpoint?.optJSONObject("browseEndpointContextSupportedConfigs")
            ?.optJSONObject("browseEndpointContextMusicConfig")
            ?.optString("pageType")

        val subtitle = extractText(card.optJSONObject("subtitle"))

        val thumb = extractThumbnail(card.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer"))

        return when {
            !videoId.isNullOrBlank() -> {
                val url = "https://music.youtube.com/watch?v=$videoId"
                TrackItem(
                    title = title,
                    uploader = subtitle ?: "YouTube Music",
                    url = url,
                    durationSeconds = 0,
                    thumbnailUrl = thumb ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                    source = Source.YOUTUBE_MUSIC,
                    kind = ItemKind.TRACK
                )
            }
            pageType == "MUSIC_PAGE_TYPE_ARTIST" && !browseId.isNullOrBlank() -> {
                TrackItem(
                    title = title,
                    uploader = "Исполнитель",
                    url = "https://music.youtube.com/channel/$browseId",
                    durationSeconds = 0,
                    thumbnailUrl = thumb,
                    source = Source.YOUTUBE_MUSIC,
                    kind = ItemKind.ARTIST
                )
            }
            pageType == "MUSIC_PAGE_TYPE_ALBUM" && !browseId.isNullOrBlank() -> {
                TrackItem(
                    title = title,
                    uploader = subtitle ?: "Альбом",
                    url = "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}",
                    durationSeconds = 0,
                    thumbnailUrl = thumb,
                    source = Source.YOUTUBE_MUSIC,
                    kind = ItemKind.ALBUM
                )
            }
            else -> null
        }
    }

    /** Парсинг строки musicResponsiveListItemRenderer. */
    private fun parseListItem(item: JSONObject): TrackItem? {
        val playlistItemData = item.optJSONObject("playlistItemData")
        var videoId = playlistItemData?.optString("videoId")?.takeIf { it.isNotBlank() }

        val flexColumns = item.optJSONArray("flexColumns") ?: return null
        if (flexColumns.length() == 0) return null

        val col0 = flexColumns.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
        val title = extractText(col0?.optJSONObject("text")) ?: return null

        // Если videoId не было в playlistItemData, смотрим watchEndpoint в первой колонке
        if (videoId.isNullOrBlank()) {
            videoId = col0?.optJSONObject("text")
                ?.optJSONArray("runs")?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")?.takeIf { it.isNotBlank() }
        }

        // Вторая колонка: артист, длительность, тип
        val col1 = flexColumns.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
        val runs = col1?.optJSONObject("text")?.optJSONArray("runs")

        var uploader: String? = null
        var isArtist = false
        var isAlbum = false
        var browseId: String? = null
        var durationSeconds = 0L

        if (runs != null) {
            for (i in 0 until runs.length()) {
                val r = runs.optJSONObject(i) ?: continue
                val text = r.optString("text").trim()
                if (text.isBlank() || text == "•" || text == ".") continue

                val nav = r.optJSONObject("navigationEndpoint")
                val bId = nav?.optJSONObject("browseEndpoint")?.optString("browseId")
                val pageType = nav?.optJSONObject("browseEndpoint")
                    ?.optJSONObject("browseEndpointContextSupportedConfigs")
                    ?.optJSONObject("browseEndpointContextMusicConfig")
                    ?.optString("pageType")

                if (pageType == "MUSIC_PAGE_TYPE_ARTIST" || (text.contains("Исполнитель", ignoreCase = true) || text.contains("Artist", ignoreCase = true))) {
                    isArtist = true
                    if (!bId.isNullOrBlank()) browseId = bId
                } else if (pageType == "MUSIC_PAGE_TYPE_ALBUM" || (text.contains("Альбом", ignoreCase = true) || text.contains("Сингл", ignoreCase = true) || text.contains("Album", ignoreCase = true))) {
                    isAlbum = true
                    if (!bId.isNullOrBlank()) browseId = bId
                }

                if (text.matches(Regex("""\d+:\d+"""))) {
                    val parts = text.split(":")
                    if (parts.size == 2) {
                        durationSeconds = (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
                    }
                } else if (uploader == null && !text.contains("просмотр", ignoreCase = true) && !text.contains("воспроизведен", ignoreCase = true)) {
                    uploader = text
                }
            }
        }

        val thumb = extractThumbnail(item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer"))

        return when {
            isArtist && !browseId.isNullOrBlank() -> {
                TrackItem(
                    title = title,
                    uploader = "Исполнитель",
                    url = "https://music.youtube.com/channel/$browseId",
                    durationSeconds = 0,
                    thumbnailUrl = thumb,
                    source = Source.YOUTUBE_MUSIC,
                    kind = ItemKind.ARTIST
                )
            }
            isAlbum && !browseId.isNullOrBlank() -> {
                TrackItem(
                    title = title,
                    uploader = uploader ?: "Альбом",
                    url = "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}",
                    durationSeconds = 0,
                    thumbnailUrl = thumb,
                    source = Source.YOUTUBE_MUSIC,
                    kind = ItemKind.ALBUM
                )
            }
            !videoId.isNullOrBlank() -> {
                val url = "https://music.youtube.com/watch?v=$videoId"
                TrackItem(
                    title = title,
                    uploader = uploader,
                    url = url,
                    durationSeconds = durationSeconds,
                    thumbnailUrl = thumb ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                    source = Source.YOUTUBE_MUSIC,
                    kind = ItemKind.TRACK
                )
            }
            else -> null
        }
    }

    private fun extractText(obj: JSONObject?): String? {
        if (obj == null) return null
        val runs = obj.optJSONArray("runs")
        if (runs != null && runs.length() > 0) {
            val sb = StringBuilder()
            for (i in 0 until runs.length()) {
                sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
            }
            val text = sb.toString().trim()
            if (text.isNotBlank()) return text
        }
        return obj.optString("simpleText").takeIf { it.isNotBlank() }
    }

    private fun extractThumbnail(renderer: JSONObject?): String? {
        if (renderer == null) return null
        val thumbs = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails") ?: return null
        if (thumbs.length() == 0) return null
        return thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
    }
}
