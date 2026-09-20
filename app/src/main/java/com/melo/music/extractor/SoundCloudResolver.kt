package com.melo.music.extractor

import android.content.Context
import com.melo.music.net.MeloNet
import com.melo.music.util.FileLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Нативный резолвер SoundCloud треков.
 * Напрямую обращается к SoundCloud API v2, минуя баги NewPipe и тяжеловесный yt-dlp.
 * Извлекает валидный HLS m3u8 поток и метаданные за < 300мс.
 */
object SoundCloudResolver {

    private const val TAG = "MeloSC"
    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .dns(MeloNet.dns)
            .proxySelector(MeloNet.byedpiSelector)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .followRedirects(false) // Ручная обработка 302: SoundCloud при редиректе теряет client_id
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Connection", "close")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    /**
     * Резолвит track URL в [ResolvedTrack] с прямым HLS плейлистом.
     */
    suspend fun resolve(context: Context, url: String): ResolvedTrack = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        val appContext = context.applicationContext
        FileLog.i(TAG, "== START resolve: $url ==")

        // 1. Прямой m3u8 стрим уже передан
        if (url.contains(".m3u8") || url.contains("cf-hls-media.sndcdn.com") || url.contains("playback.media-streaming")) {
            FileLog.i(TAG, "Already direct stream: $url")
            return@withContext ResolvedTrack(
                title = "SoundCloud",
                audioUrl = url,
            )
        }

        // 2. Получаем актуальный client_id
        var clientId = SoundCloudFix.ensure(appContext)
            ?: run {
                FileLog.e(TAG, "SoundCloud client_id unavailable")
                throw IllegalStateException("SoundCloud client_id недоступен")
            }
        FileLog.i(TAG, "Using client_id: $clientId")

        // 3. Если передан transcoding API URL (например, из кэша NewPipe)
        if (url.contains("api-v2.soundcloud.com/media/")) {
            val finalUrl = resolveTranscodingUrl(url, clientId, null)
                ?: run {
                    FileLog.e(TAG, "Failed to resolve transcoding url: $url")
                    throw IllegalStateException("Не удалось получить поток из transcoding URL")
                }
            FileLog.i(TAG, "Transcoding direct resolve OK: ${finalUrl.take(60)}")
            return@withContext ResolvedTrack(
                title = "SoundCloud",
                audioUrl = finalUrl,
            )
        }

        // 4. Запрашиваем данные о треке через resolve
        var trackJson: JSONObject? = null
        for (attempt in 0..2) {
            when (val r = fetchTrackJson(url, clientId)) {
                is FetchTrackResult.Success -> {
                    trackJson = r.json
                    break
                }
                is FetchTrackResult.Unauthorized -> {
                    FileLog.w(TAG, "SoundCloud resolve returned 401/403 with $clientId, refreshing...")
                    SoundCloudFix.invalidate(appContext)
                    clientId = SoundCloudFix.ensure(appContext)
                        ?: throw IllegalStateException("SoundCloud client_id недоступен")
                }
                is FetchTrackResult.NetworkError -> {
                    FileLog.w(TAG, "SoundCloud resolve network error (attempt $attempt): ${r.message}")
                    Thread.sleep(250)
                }
            }
        }

        if (trackJson == null) {
            FileLog.e(TAG, "fetchTrackJson failed for $url")
            throw IllegalStateException("SoundCloud: не удалось получить данные трека")
        }

        val trackAuth = trackJson.optString("track_authorization", "")
        val media = trackJson.optJSONObject("media")
            ?: run {
                FileLog.e(TAG, "No media object in trackJson")
                throw IllegalStateException("SoundCloud трек не содержит медиа информации")
            }
        val transcodings = media.optJSONArray("transcodings")
            ?: run {
                FileLog.e(TAG, "No transcodings in trackJson")
                throw IllegalStateException("SoundCloud трек не содержит аудио потоков")
            }

        FileLog.i(TAG, "Found ${transcodings.length()} transcodings, trackAuth=${trackAuth.isNotBlank()}")

        // 5. Собираем потоки:
        // 1) Progressive MP3 (cf-media.sndcdn.com) — мгновенный файл, доступен
        // 2) HLS AAC/MP4/OGG (playback.media-streaming.soundcloud.cloud) — быстрый HLS, доступен
        // 3) HLS MP3 (cf-hls-media.sndcdn.com) — в РФ часто заблокирован на уровне CloudFront IP
        // 5. Собираем потоки:
        // 1) HLS AAC/OPUS (playback.media-streaming.soundcloud.cloud) — CDN Google Cloud, не режется РКН/ТСПУ
        // 2) Progressive MP3 (cf-media.sndcdn.com) — CloudFront
        // 3) HLS MP3 (cf-hls-media.sndcdn.com)
        // 4) Сниппеты (30-сек превью), если полный трек недоступен в регионе
        val hlsAac = mutableListOf<String>()
        val progMp3 = mutableListOf<String>()
        val hlsMp3 = mutableListOf<String>()
        val snippedList = mutableListOf<String>()

        for (i in 0 until transcodings.length()) {
            val t = transcodings.optJSONObject(i) ?: continue
            val format = t.optJSONObject("format") ?: continue
            val protocol = format.optString("protocol")
            val mime = format.optString("mime_type")
            val isSnipped = t.optBoolean("snipped", false)

            val u = t.optString("url")
            if (u.isBlank()) continue

            if (isSnipped) {
                snippedList.add(u)
                continue
            }

            if (protocol == "progressive") {
                progMp3.add(u)
            } else if (protocol == "hls") {
                if (mime.contains("mp4") || mime.contains("aac") || mime.contains("ogg") || mime.contains("opus")) {
                    hlsAac.add(u)
                } else if (mime.contains("mpeg")) {
                    hlsMp3.add(u)
                } else {
                    hlsAac.add(u)
                }
            }
        }

        val fullStreams = hlsAac + progMp3 + hlsMp3
        val candidateTranscodings = fullStreams + snippedList

        if (candidateTranscodings.isEmpty()) {
            FileLog.e(TAG, "No available stream found in transcodings")
            throw IllegalStateException("SoundCloud: не найден доступный аудио поток для этого трека")
        }

        FileLog.i(TAG, "Testing ${candidateTranscodings.size} transcodings (hlsAac=${hlsAac.size}, prog=${progMp3.size}, hlsMp3=${hlsMp3.size}, snipped=${snippedList.size})...")
        var finalAudioStreamUrl: String? = null
        var fallbackBlockedUrl: String? = null
        var isFinalSnipped = false

        for (tUrl in candidateTranscodings) {
            val res = resolveTranscodingUrl(tUrl, clientId, trackAuth)
            if (!res.isNullOrBlank()) {
                if (res.contains("cf-hls-media.sndcdn.com") && candidateTranscodings.size > 1) {
                    FileLog.w(TAG, "Skipping cf-hls-media (IP blocked in RU), keeping as last resort: ${res.take(60)}")
                    fallbackBlockedUrl = res
                    continue
                }
                finalAudioStreamUrl = res
                isFinalSnipped = snippedList.contains(tUrl)
                break
            }
        }

        if (finalAudioStreamUrl.isNullOrBlank()) {
            finalAudioStreamUrl = fallbackBlockedUrl
        }

        if (finalAudioStreamUrl.isNullOrBlank()) {
            FileLog.e(TAG, "Failed to get audio stream from all transcoding urls")
            throw IllegalStateException("Не удалось получить аудио поток от SoundCloud CDN")
        }

        // 7. Метаданные
        val rawTitle = trackJson.optString("title").ifBlank { "SoundCloud Track" }
        val userObj = trackJson.optJSONObject("user")
        val artist = userObj?.optString("username")?.takeIf { it.isNotBlank() }
        val rawArtwork = trackJson.optString("artwork_url").takeIf { it.isNotBlank() }
            ?: userObj?.optString("avatar_url")?.takeIf { it.isNotBlank() }
        val artwork = rawArtwork?.let {
            if (it.contains("/avatars-")) {
                it.replace(Regex("-(?:large|badge|small|mini)\\.(jpg|jpeg|png)"), "-t300x300.$1")
            } else {
                it.replace(Regex("-(?:large|badge|small|mini)\\.(jpg|jpeg|png)"), "-t500x500.$1")
            }
        }

        val title = if (artist != null && !rawTitle.contains(artist, ignoreCase = true)) {
            "$rawTitle — $artist"
        } else {
            rawTitle
        }

        val elapsed = System.currentTimeMillis() - t0
        FileLog.i(TAG, "== SUCCESS resolve (${elapsed}ms, snipped=$isFinalSnipped): '$title' -> stream: ${finalAudioStreamUrl.take(70)} ==")

        ResolvedTrack(
            title = title,
            audioUrl = finalAudioStreamUrl,
            thumbnailUrl = artwork,
            artist = artist,
            videoUrl = null,
            isSnipped = isFinalSnipped,
        )
    }

    private sealed class FetchTrackResult {
        data class Success(val json: JSONObject) : FetchTrackResult()
        object Unauthorized : FetchTrackResult()
        data class NetworkError(val message: String) : FetchTrackResult()
    }

    private fun fetchTrackJson(url: String, clientId: String): FetchTrackResult {
        val cleanUrl = url.trim()
        val initialUrl = if (cleanUrl.contains("/tracks/") && (cleanUrl.contains("api.") || cleanUrl.contains("api-v2."))) {
            val trackId = cleanUrl.substringAfterLast("/").substringBefore("?").filter { it.isDigit() }
            "https://api-v2.soundcloud.com/tracks/$trackId?client_id=$clientId"
        } else {
            "https://api-v2.soundcloud.com/resolve?url=${URLEncoder.encode(cleanUrl, "UTF-8")}&client_id=$clientId"
        }

        FileLog.i(TAG, "fetchTrackJson request: $initialUrl")

        return try {
            var currentUrl = initialUrl
            var hops = 0
            while (hops < 4) {
                val request = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", UA)
                    .header("Accept", "application/json")
                    .build()

                val resp = httpClient.newCall(request).execute()
                val code = resp.code
                FileLog.i(TAG, "fetchTrackJson hop $hops -> HTTP $code ($currentUrl)")

                if (code == 401 || code == 403) {
                    resp.close()
                    return FetchTrackResult.Unauthorized
                }

                if (code in 300..399) {
                    val location = resp.header("Location")
                    resp.close()
                    if (location.isNullOrBlank()) {
                        FileLog.e(TAG, "Redirect without Location header")
                        return FetchTrackResult.NetworkError("Redirect without Location header")
                    }
                    val targetUrl = if (!location.contains("client_id=")) {
                        location + (if (location.contains("?")) "&" else "?") + "client_id=$clientId"
                    } else {
                        location
                    }
                    FileLog.i(TAG, "Following redirect to: $targetUrl")
                    currentUrl = targetUrl
                    hops++
                    continue
                }

                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return FetchTrackResult.NetworkError("Empty body")
                    return FetchTrackResult.Success(JSONObject(body))
                } else {
                    val errBody = runCatching { resp.body?.string() }.getOrNull()
                    resp.close()
                    FileLog.w(TAG, "fetchTrackJson error HTTP $code: $errBody")
                    return FetchTrackResult.NetworkError("HTTP $code: $errBody")
                }
            }
            FetchTrackResult.NetworkError("Too many redirects")
        } catch (e: Exception) {
            FileLog.e(TAG, "fetchTrackJson exception", e)
            FetchTrackResult.NetworkError(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun resolveTranscodingUrl(transcodingUrl: String, clientId: String, trackAuth: String?): String? {
        val sep = if (transcodingUrl.contains("?")) "&" else "?"
        var targetUrl = "$transcodingUrl${sep}client_id=$clientId"
        if (!trackAuth.isNullOrBlank()) {
            targetUrl += "&track_authorization=$trackAuth"
        }

        FileLog.i(TAG, "resolveTranscodingUrl request: $targetUrl")

        return runCatching {
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", UA)
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val code = resp.code
                val body = resp.body?.string()
                FileLog.i(TAG, "resolveTranscodingUrl -> HTTP $code (body: ${body?.take(80)})")
                if (resp.isSuccessful && !body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val streamUrl = json.optString("url").takeIf { it.isNotBlank() }
                    FileLog.i(TAG, "Got stream url: ${streamUrl?.take(60)}")
                    streamUrl
                } else {
                    if (!trackAuth.isNullOrBlank()) {
                        FileLog.w(TAG, "Retrying transcoding without track_authorization...")
                        val retryUrl = "$transcodingUrl${sep}client_id=$clientId"
                        val retryReq = Request.Builder().url(retryUrl).header("User-Agent", UA).build()
                        httpClient.newCall(retryReq).execute().use { retryResp ->
                            val rCode = retryResp.code
                            val rBody = retryResp.body?.string()
                            FileLog.i(TAG, "resolveTranscodingUrl (no auth) -> HTTP $rCode (body: ${rBody?.take(80)})")
                            if (retryResp.isSuccessful && !rBody.isNullOrBlank()) {
                                return@use JSONObject(rBody).optString("url").takeIf { it.isNotBlank() }
                            }
                        }
                    }
                    null
                }
            }
        }.onFailure {
            FileLog.e(TAG, "resolveTranscodingUrl exception", it)
        }.getOrNull()
    }
}
