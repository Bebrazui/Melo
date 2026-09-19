package com.melo.music.extractor

import android.content.Context
import android.util.LruCache
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Результат резолва: что играть и как подписать. */
data class ResolvedTrack(
    val title: String,
    val audioUrl: String,
    /** Обложка трека (для уведомления/кроссфейда). Может быть null. */
    val thumbnailUrl: String? = null,
    /** Автор/исполнитель трека (для системного уведомления и Dynamic Island). */
    val artist: String? = null,
    /** Прямой URL видеоклипа (MP4), если у трека есть видеопоток. */
    val videoUrl: String? = null,
)

/** Определяет, является ли аудио-URL HLS-потоком (.m3u8, CloudFront HLS или SoundCloud media-streaming). */
fun isHlsUrl(url: String): Boolean {
    val u = url.lowercase()
    return u.contains("m3u8") ||
        u.contains("/hls") ||
        u.contains("cf-hls-media.sndcdn.com") ||
        u.contains("media-streaming.soundcloud.cloud") ||
        u.contains("manifest")
}

/** Источник трека (для индикатора в UI). */
enum class Source { YOUTUBE_MUSIC, SOUNDCLOUD, BANDCAMP, DEEZER, TIDAL, LOCAL }

/** Тип элемента списка: трек, исполнитель/канал или альбом/плейлист. */
enum class ItemKind { TRACK, ARTIST, ALBUM }

/** Элемент списка (поиск / рекомендации) — ещё без прямого аудио-URL. */
data class TrackItem(
    val title: String,
    val uploader: String?,
    val url: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val source: Source,
    val kind: ItemKind = ItemKind.TRACK,
    /** Скорость/тон воспроизведения (1.0 = оригинал). Сохранённые «slowed/sped up» версии. */
    val speed: Float = 1f,
    /** Просмотры (ранжирование «Популярного» на экране исполнителя); 0 если неизвестно. */
    val viewCount: Long = 0,
)

class TrackCopyrightException(
    message: String = "Трек удалён правообладателем",
    cause: Throwable? = null,
) : Exception(message, cause)

/** Определяет источник по URL (для Deezer/Tidal, которые идут через yt-dlp). */
fun sourceForUrl(url: String): Source = when {
    url.contains("deezer.com", ignoreCase = true) ||
        url.contains("deezer.page.link", ignoreCase = true) -> Source.DEEZER
    url.contains("tidal.com", ignoreCase = true) -> Source.TIDAL
    url.contains("soundcloud.com", ignoreCase = true) -> Source.SOUNDCLOUD
    url.contains("bandcamp.com", ignoreCase = true) -> Source.BANDCAMP
    else -> Source.YOUTUBE_MUSIC
}

/**
 * Обёртка над yt-dlp, работающим прямо на устройстве (youtubedl-android).
 *
 * [ensureInit] распаковывает Python + yt-dlp (тяжёлая операция, делается один раз
 * за процесс). [resolveAudioUrl] по ссылке достаёт прямой URL лучшего аудио-потока,
 * который затем играет тот же Media3-плеер.
 */
object Extractor {

    @Volatile
    private var initialized = false

    /** Кэш готовых стрим-URL на сессию (один резолв на трек). */
    private val streamCache = LruCache<String, ResolvedTrack>(50)

    /** Резолвы «в полёте» — чтобы один URL не резолвился дважды параллельно. */
    private val inFlight = ConcurrentHashMap<String, Deferred<ResolvedTrack>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Очередь фонового prefetch: 1 воркер — prefetch не должен мешать тапу пользователя. */
    @Volatile
    private var appCtx: Context? = null
    private val prefetchQueue = Channel<String>(Channel.UNLIMITED)

    init {
        repeat(1) { worker ->
            scope.launch {
                for (url in prefetchQueue) {
                    val ctx = appCtx ?: continue
                    if (!isCached(url)) {
                        // android.util.Log.e("MeloPerf", "prefetch[$worker] resolve $url")
                        runCatching { resolveAudioUrl(ctx, url) }
                    }
                }
            }
        }
    }

    /** Поставить трек в фоновую очередь предзагрузки стрим-URL. */
    fun prefetch(context: Context, url: String) {
        if (isCached(url)) return
        appCtx = context.applicationContext
        prefetchQueue.trySend(url)
    }

    /** Уже есть в кэше? (память или свежая запись на диске) */
    fun isCached(url: String): Boolean =
        streamCache.get(url) != null || StreamCacheStore.get(url) != null

    /**
     * Выбрасывает трек из всех кэшей — для авто-перерезолва, когда плеер словил
     * ошибку (например, протухшую/привязанную к IP ссылку → 403).
     */
    fun invalidate(url: String) {
        streamCache.remove(url)
        StreamCacheStore.remove(url)
        inFlight.remove(url)
    }

    @Synchronized
    fun ensureInit(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        YoutubeDL.getInstance().init(app)
        FFmpeg.getInstance().init(app)
        initialized = true
    }

    /** Обновляет бинарь yt-dlp до свежего (требует сети). Вызывать периодически. */
    suspend fun updateYtDlp(context: Context): YoutubeDL.UpdateStatus? =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext)
        }

    suspend fun resolveAudioUrl(context: Context, url: String, fallbackQuery: String? = null): ResolvedTrack {
        // 0) Офлайн: скачанный трек играем прямо с диска, без сети.
        com.melo.music.offline.OfflineManager.localUri(url)?.let { local ->
            // android.util.Log.e("MeloPerf", "OFFLINE HIT $url")
            return ResolvedTrack(
                title = com.melo.music.offline.OfflineManager.titleFor(url) ?: url,
                audioUrl = local,
            )
        }
        // 1) Память.
        streamCache.get(url)?.let {
            // android.util.Log.e("MeloPerf", "CACHE HIT (mem) $url")
            return it
        }
        // 2) Диск — мгновенно, если ссылка ещё не протухла.
        StreamCacheStore.get(url)?.let {
            // android.util.Log.e("MeloPerf", "CACHE HIT (disk) $url")
            streamCache.put(url, it)
            return it
        }
        val app = context.applicationContext
        val flightKey = if (!fallbackQuery.isNullOrBlank()) "$url#$fallbackQuery" else url
        // Дедуп: если этот URL уже резолвится — ждём тот же результат.
        val deferred = inFlight.getOrPut(flightKey) {
            // android.util.Log.e("MeloPerf", "CACHE MISS → resolve $url")
            scope.async {
                try {
                    // SoundCloud — прямой нативный HLS-резолвер (<300мс)
                    if (NewPipeResolver.isSoundCloud(url)) {
                        com.melo.music.util.FileLog.i("MeloExtract", "Resolving via SoundCloudResolver: $url")
                        val scResolved = SoundCloudResolver.resolve(app, url)
                        streamCache.put(url, scResolved)
                        StreamCacheStore.put(url, scResolved)
                        return@async scResolved
                    }

                    // YouTube и SoundCloud (fallback) — через NewPipe, с надёжным авто-фолбэком на yt-dlp при ошибках.
                    val resolved = if (NewPipeResolver.isSupported(url)) {
                        try {
                            NewPipeResolver.resolve(app, url)
                        } catch (e: Exception) {
                            val msg = e.message.orEmpty().lowercase()
                            val isExplicitCopyright = msg.contains("copyright") ||
                                msg.contains("removed following a copyright") ||
                                msg.contains("interscope") ||
                                msg.contains("dmca")
                            if (isExplicitCopyright) {
                                android.util.Log.e("MeloExtract", "Track blocked by copyright: $url")
                                throw TrackCopyrightException("Трек удалён правообладателем", e)
                            }
                            android.util.Log.e("MeloExtract", "NewPipe resolve failed for $url: ${e.message}, falling back to yt-dlp")
                            try {
                                resolveWithYtDlp(app, url)
                            } catch (e2: Exception) {
                                val msg2 = e2.message.orEmpty().lowercase()
                                if (msg2.contains("copyright") || msg2.contains("removed") || msg2.contains("interscope") || msg2.contains("dmca")) {
                                    throw TrackCopyrightException("Трек удалён правообладателем", e2)
                                }
                                throw e2
                            }
                        }
                    } else {
                        resolveWithYtDlp(app, url)
                    }
                    streamCache.put(url, resolved)
                    StreamCacheStore.put(url, resolved)
                    resolved
                } finally {
                    inFlight.remove(flightKey)
                }
            }
        }
        return deferred.await()
    }

    /** Метаданные трека по ссылке (без скачивания) — для подсказки по URL. */
    suspend fun fetchMeta(context: Context, url: String): TrackItem? =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            runCatching {
                val cleanUrl = if (url.contains("music.youtube.com")) url.replace("music.youtube.com", "www.youtube.com") else url
                val request = YoutubeDLRequest(cleanUrl).apply {
                    addOption("--no-playlist")
                    addOption("--skip-download")
                    addOption("--no-update")
                    addOption("--no-warnings")
                    if (com.melo.music.byedpi.ByeDpiProxy.shouldRoute()) {
                        addOption("--proxy", "socks5://${com.melo.music.byedpi.ByeDpiProxy.DEFAULT_HOST}:${com.melo.music.byedpi.ByeDpiProxy.DEFAULT_PORT}")
                    }
                    if (com.melo.music.auth.YouTubeAccountManager.isLoggedIn) {
                        val cookieFile = com.melo.music.auth.YouTubeAccountManager.getNetscapeCookieFile(context)
                        if (cookieFile != null && cookieFile.exists()) {
                            addOption("--cookies", cookieFile.absolutePath)
                        }
                    }
                }
                val info = YoutubeDL.getInstance().getInfo(request)
                TrackItem(
                    title = info.title ?: url,
                    uploader = info.uploader,
                    url = url,
                    durationSeconds = runCatching { info.duration.toLong() }.getOrDefault(0L),
                    thumbnailUrl = info.thumbnail,
                    source = sourceForUrl(url),
                    kind = ItemKind.TRACK,
                )
            }.getOrNull()
        }

    private suspend fun resolveWithYtDlp(context: Context, url: String): ResolvedTrack =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val cookieFile = if (com.melo.music.auth.YouTubeAccountManager.isLoggedIn) {
                com.melo.music.auth.YouTubeAccountManager.getNetscapeCookieFile(context)
            } else null

            fun buildRequest(
                targetUrl: String,
                client: String? = null,
                useCookies: Boolean = true,
                format: String? = "ba/b/bestaudio/best",
            ): YoutubeDLRequest {
                return YoutubeDLRequest(targetUrl).apply {
                    if (format != null) {
                        addOption("-f", format)
                    }
                    addOption("--no-playlist")
                    addOption("--no-update")
                    addOption("--no-warnings")
                    if (com.melo.music.byedpi.ByeDpiProxy.shouldRoute()) {
                        addOption("--proxy", "socks5://${com.melo.music.byedpi.ByeDpiProxy.DEFAULT_HOST}:${com.melo.music.byedpi.ByeDpiProxy.DEFAULT_PORT}")
                    }
                    if (useCookies && cookieFile != null && cookieFile.exists()) {
                        addOption("--cookies", cookieFile.absolutePath)
                    }
                    if (client != null) {
                        addOption("--extractor-args", "youtube:player_client=$client")
                    }
                }
            }

            data class Strategy(
                val targetUrl: String,
                val client: String?,
                val useCookies: Boolean,
                val format: String?,
                val label: String,
            )

            val isYtMusic = url.contains("music.youtube.com")
            val ytWatchUrl = if (isYtMusic) url.replace("music.youtube.com", "www.youtube.com") else url

            val strategies = mutableListOf<Strategy>()
            // 1) Если трек из YouTube Music — в первую очередь запрашиваем специализированные клиенты YouTube Music
            if (isYtMusic) {
                strategies.add(Strategy(url, "web_remix", true, "ba/b/bestaudio/best", "music.youtube.com web_remix with cookies"))
                strategies.add(Strategy(url, "android_music", true, "ba/b/bestaudio/best", "music.youtube.com android_music with cookies"))
                strategies.add(Strategy(url, "ios_music", true, "ba/b/bestaudio/best", "music.youtube.com ios_music with cookies"))
                strategies.add(Strategy(url, "web_remix", true, null, "music.youtube.com web_remix any format"))
                strategies.add(Strategy(url, "android_music", true, null, "music.youtube.com android_music any format"))
            }

            // 2) Клиенты YouTube с обходом ограничений
            strategies.add(Strategy(ytWatchUrl, "web_creator", true, "ba/b/bestaudio/best", "web_creator with cookies"))
            strategies.add(Strategy(ytWatchUrl, "ios", true, "ba/b/bestaudio/best", "ios with cookies"))
            strategies.add(Strategy(ytWatchUrl, "mweb", true, "ba/b/bestaudio/best", "mweb with cookies"))
            strategies.add(Strategy(ytWatchUrl, "web", true, "ba/b/bestaudio/best", "web with cookies"))
            strategies.add(Strategy(ytWatchUrl, "tv_embedded", false, "ba/b/bestaudio/best", "tv_embedded anonymous"))
            strategies.add(Strategy(ytWatchUrl, "tv", false, "ba/b/bestaudio/best", "tv anonymous"))
            strategies.add(Strategy(ytWatchUrl, null, true, "ba/b/bestaudio/best", "default yt-dlp with cookies"))
            strategies.add(Strategy(ytWatchUrl, null, false, "ba/b/bestaudio/best", "default yt-dlp anonymous"))
            strategies.add(Strategy(ytWatchUrl, "ios", true, null, "ios any format"))
            strategies.add(Strategy(ytWatchUrl, "tv_embedded", false, null, "tv_embedded any format"))
            strategies.add(Strategy(ytWatchUrl, null, true, null, "default any format"))

            fun appendLog(msg: String) {
                android.util.Log.e("MeloExtract", msg)
                runCatching {
                    val logFile = java.io.File(context.cacheDir, "resolve.log")
                    if (logFile.length() > 200_000) logFile.delete()
                    val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
                    logFile.appendText("$time: $msg\n")
                }
            }

            var lastError: Throwable? = null
            for (strat in strategies) {
                try {
                    appendLog("Trying yt-dlp: ${strat.label} for ${strat.targetUrl}")
                    val req = buildRequest(strat.targetUrl, client = strat.client, useCookies = strat.useCookies, format = strat.format)
                    val result = YoutubeDL.getInstance().getInfo(req)
                    val audioUrl = result.url
                    if (!audioUrl.isNullOrBlank()) {
                        appendLog("yt-dlp SUCCESS with ${strat.label} for ${strat.targetUrl}: ${audioUrl.take(60)}")
                        return@withContext ResolvedTrack(
                            title = result.title ?: url,
                            audioUrl = audioUrl,
                            thumbnailUrl = result.thumbnail,
                            artist = result.uploader,
                        )
                    }
                } catch (e: Exception) {
                    appendLog("yt-dlp ${strat.label} failed: ${e.message}")
                    if (strat.label.contains("music") || strat.label.contains("creator")) {
                        runCatching {
                            val listReq = buildRequest(strat.targetUrl, client = strat.client, useCookies = strat.useCookies, format = null).apply {
                                addOption("--list-formats")
                            }
                            val out = YoutubeDL.getInstance().execute(listReq).out
                            appendLog("FORMATS for ${strat.label}:\n$out")
                        }
                    }
                    lastError = e
                }
            }

            appendLog("ALL STRATEGIES FAILED. Last error: ${lastError?.message}")
            throw lastError ?: IllegalStateException("yt-dlp не смог извлечь аудио-поток ни одной из стратегий")
        }
}
