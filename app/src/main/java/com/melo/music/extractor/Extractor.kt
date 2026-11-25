package com.melo.music.extractor

import android.content.Context
import android.util.LruCache
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
    /** Является ли трек 30-секундным сниппетом (SoundCloud Go/Preview). */
    val isSnipped: Boolean = false,
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
    /** Басс-буст для трека. */
    val bassBoost: Boolean = false,
    /** Ухудшение качества (Bitcrush/Degrader) 0.0f .. 1.0f (0 = выкл). */
    val crush: Float = 0f,
    /** Сдвиг тональности в полутонах (например -12 .. +12 semitones; 0 = оригинал). */
    val pitchSemi: Float = 0f,
    /** Сид рандомизации нот / тональности (0 = выкл, >0 = уникальный рандомный сдвиг нот). */
    val noteShiftSeed: Long = 0L,
) {
    val cleanUploader: String?
        get() = cleanArtistName(uploader, title)

    val cleanTitle: String
        get() = cleanTrackTitle(title, uploader)
}

fun cleanArtistName(rawUploader: String?, rawTitle: String? = null): String? {
    var s = rawUploader?.trim() ?: ""
    if (s.startsWith("by — ", ignoreCase = true) || s.startsWith("by - ", ignoreCase = true) || s.startsWith("by: ", ignoreCase = true)) {
        s = s.substring(4).trim().trimStart('—', '-', ':').trim()
    } else if (s.startsWith("by ", ignoreCase = true)) {
        s = s.substring(3).trim()
    }

    val typePrefixes = listOf(
        "Композиция", "Песня", "Видео", "Сингл", "Альбом", "Трек", "Запись",
        "Song", "Video", "Track", "Single", "Album"
    )
    for (tp in typePrefixes) {
        if (s.startsWith(tp, ignoreCase = true)) {
            val after = s.substring(tp.length).trim()
            if (after.startsWith("•") || after.startsWith("·") || after.startsWith("-")) {
                s = after.trimStart('•', '·', '-').trim()
            }
        }
    }

    if (s.contains("•") || s.contains("·")) {
        val parts = s.split(Regex("[•·]")).map { it.trim() }.filter { it.isNotBlank() }
        val cand = parts.firstOrNull { part ->
            !typePrefixes.any { it.equals(part, ignoreCase = true) } &&
                !part.matches(Regex("""\d+:\d+""")) &&
                !part.contains("просмотр", ignoreCase = true) &&
                !part.contains("воспроизведен", ignoreCase = true) &&
                !part.contains("views", ignoreCase = true) &&
                !part.contains("plays", ignoreCase = true) &&
                !part.equals("YouTube Music", ignoreCase = true)
        }
        if (!cand.isNullOrBlank()) s = cand
    }

    if (s.endsWith(" - Topic", ignoreCase = true)) {
        s = s.substring(0, s.length - 8).trim()
    } else if (s.endsWith(" - Тема", ignoreCase = true)) {
        s = s.substring(0, s.length - 7).trim()
    }

    val genericWords = setOf(
        "исполнитель", "артист", "композиция", "песня", "видео", "трек",
        "artist", "unknown", "неизвестный исполнитель", "неизвестно", "youtube music"
    )
    if (s.lowercase() in genericWords) {
        s = ""
    }

    if (s.isBlank() || (rawTitle != null && s.equals(rawTitle.trim(), ignoreCase = true))) {
        if (rawTitle != null && (rawTitle.contains(" - ") || rawTitle.contains(" — "))) {
            val split = rawTitle.split(Regex(" [—-] "), limit = 2)
            if (split.size == 2) {
                val p0 = split[0].trim()
                val p1 = split[1].trim()
                if (p1.endsWith(" - Topic", ignoreCase = true) || p1.endsWith(" - Тема", ignoreCase = true)) {
                    val cleanP1 = p1.replace(Regex(" - (Topic|Тема)$", RegexOption.IGNORE_CASE), "").trim()
                    if (cleanP1.isNotBlank()) return cleanP1
                }
                if (p0.endsWith(" - Topic", ignoreCase = true) || p0.endsWith(" - Тема", ignoreCase = true)) {
                    val cleanP0 = p0.replace(Regex(" - (Topic|Тема)$", RegexOption.IGNORE_CASE), "").trim()
                    if (cleanP0.isNotBlank()) return cleanP0
                }
                if (p0.lowercase() in genericWords && p1.isNotBlank()) {
                    return p1
                }
                if (p0.isNotBlank()) {
                    return p0
                }
            }
        }
        return if (s.isNotBlank()) s else null
    }

    return s.ifBlank { null }
}

fun cleanTrackTitle(rawTitle: String, rawUploader: String? = null): String {
    var t = rawTitle.trim()
    val typePrefixes = listOf("Композиция", "Песня", "Видео", "Сингл", "Альбом", "Трек", "Song", "Video", "Track")
    for (tp in typePrefixes) {
        if (t.startsWith(tp, ignoreCase = true)) {
            val after = t.substring(tp.length).trim()
            if (after.startsWith("•") || after.startsWith("·")) {
                val rem = after.trimStart('•', '·').trim()
                if (rem.isNotBlank()) t = rem
            }
        }
    }
    val artist = cleanArtistName(rawUploader, rawTitle)
    if (artist != null) {
        if (t.startsWith("$artist - ", ignoreCase = true) || t.startsWith("$artist — ", ignoreCase = true)) {
            t = t.substring(artist.length + 3).trim()
        } else if (t.endsWith(" - $artist - Topic", ignoreCase = true) || t.endsWith(" — $artist - Topic", ignoreCase = true)) {
            t = t.substring(0, t.length - artist.length - 11).trim()
        } else if (t.endsWith(" - $artist - Тема", ignoreCase = true) || t.endsWith(" — $artist - Тема", ignoreCase = true)) {
            t = t.substring(0, t.length - artist.length - 10).trim()
        } else if (t.endsWith(" - $artist", ignoreCase = true) || t.endsWith(" — $artist", ignoreCase = true)) {
            t = t.substring(0, t.length - artist.length - 3).trim()
        }
    }
    if (t.endsWith(" - Topic", ignoreCase = true)) {
        t = t.substring(0, t.length - 8).trim()
    } else if (t.endsWith(" - Тема", ignoreCase = true)) {
        t = t.substring(0, t.length - 7).trim()
    }
    return t.ifBlank { rawTitle }
}

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
                        com.melo.music.util.FileLog.d("MeloPerf", "prefetch[$worker] starting resolve for $url")
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
     * Дополнительно вычищает офлайн-запись, если локальный файл оказался битым
     * (иначе трек снова подхватит тот же файл и уйдёт в бесконечный цикл 3003).
     */
    fun invalidate(url: String) {
        streamCache.remove(url)
        StreamCacheStore.remove(url)
        inFlight.remove(url)
        com.melo.music.offline.OfflineManager.invalidateBroken(url)
    }

    fun ensureInit(context: Context) {
        if (initialized) return
        initialized = true
    }

    suspend fun resolveAudioUrl(
        context: Context,
        url: String,
        fallbackQuery: String? = null,
        title: String? = null,
        uploader: String? = null,
        preferProgressive: Boolean = false,
    ): ResolvedTrack {
        // 0) Офлайн: скачанный трек играем прямо с диска, без сети.
        com.melo.music.offline.OfflineManager.getOrFindLocalUri(context, url, title, uploader, fallbackQuery)?.let { local ->
            // android.util.Log.e("MeloPerf", "OFFLINE HIT $url")
            val trackTitle = com.melo.music.offline.OfflineManager.titleFor(url) ?: (title ?: url)
            val thumb = com.melo.music.favorites.FavoritesManager.getAll().find { it.url == url }?.thumbnailUrl
            return ResolvedTrack(
                title = trackTitle,
                audioUrl = local,
                thumbnailUrl = thumb,
                artist = uploader,
            )
        }
        // Для офлайн-загрузки нужен свежий резолв (HLS-кэш не подходит: из HLS-сегментов
        // SoundCloud получается нечитаемая склейка), поэтому кэш потока обходим.
        if (!preferProgressive) {
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
        }
        val app = context.applicationContext
        val flightKey = buildString {
            append(url)
            if (!fallbackQuery.isNullOrBlank()) append('#').append(fallbackQuery)
            if (preferProgressive) append("#prog")
        }
        // Дедуп: если этот URL уже резолвится — ждём тот же результат.
        val deferred = inFlight.getOrPut(flightKey) {
            // android.util.Log.e("MeloPerf", "CACHE MISS → resolve $url")
            scope.async {
                try {
                    // SoundCloud — прямой нативный HLS-резолвер (<300мс)
                    if (NewPipeResolver.isSoundCloud(url)) {
                        com.melo.music.util.FileLog.i("MeloExtract", "Resolving via SoundCloudResolver: $url (progressive=$preferProgressive)")
                        val scResolved = runCatching { SoundCloudResolver.resolve(app, url, preferProgressive) }.getOrNull()
                        if (scResolved != null) {
                            val ytQuery = fallbackQuery ?: listOfNotNull(scResolved.title, scResolved.artist).joinToString(" ").takeIf { it.isNotBlank() }
                            if (scResolved.isSnipped && !ytQuery.isNullOrBlank()) {
                                com.melo.music.util.FileLog.i("MeloExtract", "SoundCloud track is 30s preview snippet, searching full track on YouTube: $ytQuery")
                                val ytMatch = runCatching { NewPipeResolver.searchOne(app, ytQuery) }.getOrNull()
                                if (ytMatch != null && ytMatch.url.isNotBlank()) {
                                    val ytResolved = runCatching { NewPipeResolver.resolve(app, ytMatch.url) }.getOrNull()
                                    if (ytResolved != null) {
                                        if (!preferProgressive) {
                                            streamCache.put(url, ytResolved)
                                            StreamCacheStore.put(url, ytResolved)
                                        }
                                        return@async ytResolved
                                    }
                                }
                            }
                            if (!preferProgressive) {
                                streamCache.put(url, scResolved)
                                StreamCacheStore.put(url, scResolved)
                            }
                            return@async scResolved
                        } else {
                            com.melo.music.util.FileLog.w("MeloExtract", "SoundCloudResolver failed for $url, attempting fallback to YouTube: $fallbackQuery")
                            if (!fallbackQuery.isNullOrBlank()) {
                                val ytMatch = runCatching { NewPipeResolver.searchOne(app, fallbackQuery) }.getOrNull()
                                if (ytMatch != null && ytMatch.url.isNotBlank()) {
                                    val ytResolved = runCatching { NewPipeResolver.resolve(app, ytMatch.url) }.getOrNull()
                                    if (ytResolved != null) {
                                        if (!preferProgressive) {
                                            streamCache.put(url, ytResolved)
                                            StreamCacheStore.put(url, ytResolved)
                                        }
                                        return@async ytResolved
                                    }
                                }
                            }
                            throw IllegalStateException("SoundCloud: аудио поток трека недоступен")
                        }
                    }

                    // YouTube и SoundCloud — через нативный NewPipeResolver.
                    val resolved = if (NewPipeResolver.isSupported(url)) {
                        try {
                            com.melo.music.util.FileLog.i("MeloExtract", "Resolving via NewPipe: $url")
                            NewPipeResolver.resolve(app, url)
                        } catch (e: Exception) {
                            val msg = e.message.orEmpty().lowercase()
                            val isExplicitCopyright = msg.contains("copyright") ||
                                msg.contains("removed following a copyright") ||
                                msg.contains("interscope") ||
                                msg.contains("dmca")
                            if (isExplicitCopyright) {
                                com.melo.music.util.FileLog.e("MeloExtract", "Track blocked by copyright: $url")
                                throw TrackCopyrightException("Трек удалён правообладателем", e)
                            }
                            com.melo.music.util.FileLog.e("MeloExtract", "NewPipe resolve failed for $url: ${e.message}")
                            throw e
                        }
                    } else {
                        throw IllegalStateException("Неподдерживаемый URL источника: $url")
                    }
                    if (!preferProgressive) {
                        streamCache.put(url, resolved)
                        StreamCacheStore.put(url, resolved)
                    }
                    resolved
                } finally {
                    inFlight.remove(flightKey)
                }
            }
        }
        return deferred.await()
    }

    /** Метаданные трека по ссылке через NewPipeResolver. */
    suspend fun fetchMeta(context: Context, url: String): TrackItem? =
        withContext(Dispatchers.IO) {
            NewPipeResolver.resolveSingleTrack(context, url)
        }
}
