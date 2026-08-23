package com.melo.music.extractor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.File
import java.util.Locale

/**
 * Резолвер YouTube на базе NewPipe Extractor — без yt-dlp.
 * NewPipe сам подбирает аудио-формат и расшифровывает подписи,
 * его прямые URL обычно играются ExoPlayer-ом напрямую.
 *
 * Дисковый кэш OkHttp хранит плеерный base.js между запусками приложения —
 * это снимает основную часть задержки первого резолва.
 */
object NewPipeResolver {

    @Volatile
    private var initialized = false

    /** Код страны (ISO, напр. "RU"), по которому строятся рекомендации. */
    val countryCode: String
        get() = Locale.getDefault().country.ifBlank { "US" }

    @Synchronized
    fun ensureInit(context: Context) {
        if (initialized) return
        val cacheDir = File(context.applicationContext.cacheDir, "newpipe_http")
        // Через ОДИН ByeDPI-прокси нельзя гнать лавину запросов: при шторме (prefetch
        // множества SoundCloud-треков, каждый = 6-8 запросов к api-v2) прокси
        // захлёбывается и нужный треку запрос ловит таймаут ~12с. Жёстко ограничиваем
        // одновременные запросы (особенно на хост) — тогда всё отвечает за ~150мс.
        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 8
            maxRequestsPerHost = 4
        }
        val client = OkHttpClient.Builder()
            .cache(Cache(cacheDir, 64L * 1024 * 1024))
            .dispatcher(dispatcher)
            // База под YouTube (base.js крупный). SoundCloud-зависания лечит
            // ScRetryInterceptor в OkHttpDownloader (короткий таймаут + повтор).
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        // Локаль и страна устройства → региональные результаты (RU → русские песни).
        val locale = Locale.getDefault()
        val localization = Localization(
            locale.language.ifBlank { "en" },
            locale.country.ifBlank { "US" },
        )
        NewPipe.init(OkHttpDownloader(client), localization, ContentCountry(countryCode))
        initialized = true
    }

    fun isYouTube(url: String): Boolean =
        url.contains("youtube.com", ignoreCase = true) ||
            url.contains("youtu.be", ignoreCase = true)

    fun isSoundCloud(url: String): Boolean =
        url.contains("soundcloud.com", ignoreCase = true)

    fun isBandcamp(url: String): Boolean =
        url.contains("bandcamp.com", ignoreCase = true)

    /** Поддерживается ли URL движком NewPipe (иначе — yt-dlp). */
    fun isSupported(url: String): Boolean =
        isYouTube(url) || isSoundCloud(url) || isBandcamp(url)

    private fun serviceFor(url: String): StreamingService = when {
        isSoundCloud(url) -> ServiceList.SoundCloud
        isBandcamp(url) -> ServiceList.Bandcamp
        else -> ServiceList.YouTube
    }

    suspend fun resolve(context: Context, url: String): ResolvedTrack = withContext(Dispatchers.IO) {
        val t0 = android.os.SystemClock.elapsedRealtime()
        ensureInit(context)
        if (isSoundCloud(url)) SoundCloudFix.ensure(context)
        val t1 = android.os.SystemClock.elapsedRealtime()
        val info = StreamInfo.getInfo(serviceFor(url), url)
        val t2 = android.os.SystemClock.elapsedRealtime()
        val candidates = info.audioStreams.filter { it.content.isNotBlank() }
        val audio = if (isSoundCloud(url)) {
            // SoundCloud: прогрессив (cf-media) и aac_160k (Go+) отдают 403 для анонима.
            // Берём бесплатный HLS < 160 кбит/с (mp3 128 / opus).
            val hls = candidates.filter { it.deliveryMethod == DeliveryMethod.HLS }
            hls.forEach {
                // android.util.Log.e("MeloSC", "hls br=${it.averageBitrate} url=${it.content.take(60)}")
            }
            hls.filter { it.averageBitrate in 1 until 160 }.maxByOrNull { it.averageBitrate }
                ?: hls.filter { it.averageBitrate <= 0 }.firstOrNull()
                ?: hls.minByOrNull { it.averageBitrate }
                ?: candidates.maxByOrNull { it.averageBitrate }
        } else {
            // YouTube/Bandcamp: прогрессив проще для ExoPlayer.
            candidates.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }.maxByOrNull { it.averageBitrate }
                ?: candidates.maxByOrNull { it.averageBitrate }
        } ?: throw IllegalStateException("NewPipe не нашёл аудио-потоков")
        val t3 = android.os.SystemClock.elapsedRealtime()
        val source = when {
            isSoundCloud(url) -> Source.SOUNDCLOUD
            isBandcamp(url) -> Source.BANDCAMP
            else -> Source.YOUTUBE_MUSIC
        }
        // android.util.Log.e(
        //     "MeloPerf",
        //     "resolve init=${t1 - t0}ms getInfo=${t2 - t1}ms pick=${t3 - t2}ms TOTAL=${t3 - t0}ms",
        // )

        val author = info.uploaderName?.takeIf { it.isNotBlank() }
        ResolvedTrack(
            title = if (author != null) "${info.name} — $author" else info.name,
            audioUrl = audio.content,
            thumbnailUrl = info.thumbnails.firstOrNull()?.url,
        )
    }

    /**
     * Потоковый поиск: результаты приходят по мере готовности источника, а не «махом».
     * YouTube обычно быстрее (~0.4с), SoundCloud — позже (~2с) и дописывается снизу.
     * Каждый emit — это накопленный список (исполнители сверху, затем треки).
     */
    /** Похоже ли на ссылку трека одного из источников. */
    fun isTrackUrl(q: String): Boolean {
        val s = q.trim()
        if (!s.startsWith("http", ignoreCase = true)) return false
        return isYouTube(s) || isSoundCloud(s) || isBandcamp(s)
    }

    /** Один трек по ссылке (для поля поиска): метаданные без воспроизведения. */
    suspend fun resolveSingleTrack(context: Context, url: String): TrackItem? =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            runCatching {
                when {
                    isYouTube(url) || isSoundCloud(url) -> {
                        if (isSoundCloud(url)) SoundCloudFix.ensure(context)
                        val info = StreamInfo.getInfo(serviceFor(url), url)
                        TrackItem(
                            title = info.name,
                            uploader = info.uploaderName?.takeIf { it.isNotBlank() },
                            url = info.url ?: url,
                            durationSeconds = info.duration,
                            thumbnailUrl = info.thumbnails.maxByOrNull { it.height }?.url
                                ?: info.thumbnails.firstOrNull()?.url,
                            source = if (isSoundCloud(url)) Source.SOUNDCLOUD else Source.YOUTUBE_MUSIC,
                            kind = ItemKind.TRACK,
                        )
                    }
                    isBandcamp(url) -> Extractor.fetchMeta(context, url)
                    else -> null
                }
            }.getOrNull()
        }

    fun search(context: Context, query: String): Flow<List<TrackItem>> = channelFlow {
        ensureInit(context)

        // Если в поле вставили ссылку — предлагаем только этот трек.
        if (isTrackUrl(query)) {
            val item = resolveSingleTrack(context, query.trim())
            if (item != null) send(listOf(item))
            return@channelFlow
        }

        val mutex = Mutex()
        val allItems = mutableListOf<TrackItem>()

        suspend fun emitAll(tag: String, source: Source, block: suspend () -> List<TrackItem>) {
            val part = runCatching { block() }
                .onFailure { /* android.util.Log.e("MeloSearch", "$tag failed: $it", it) */ }
                .getOrDefault(emptyList())
                .take(15)
            // android.util.Log.e("MeloSearch", "$tag → ${part.size} items")
            if (part.isEmpty()) return
            mutex.withLock {
                allItems.addAll(part)
                // Сортируем ВСЕ результаты по релевантности, невзирая на источник.
                send(RelevanceScorer.rank(query, allItems))
            }
        }

        launch(Dispatchers.IO) {
            emitAll("YouTube", Source.YOUTUBE_MUSIC) { searchYouTube(query) }
        }
        launch(Dispatchers.IO) {
            emitAll("SoundCloud", Source.SOUNDCLOUD) {
                SoundCloudFix.ensure(context)
                searchService(ServiceList.SoundCloud, query, Source.SOUNDCLOUD, listOf("tracks"))
            }
        }
        launch(Dispatchers.IO) {
            emitAll("Bandcamp", Source.BANDCAMP) { BandcampSearcher.search(query) }
        }
    }

    /** YouTube Music: песни (music_songs) + исполнители (music_artists). */
    private fun searchYouTube(query: String): List<TrackItem> {
        val service = ServiceList.YouTube
        val songs = SearchInfo.getInfo(
            service,
            service.searchQHFactory.fromQuery(query, listOf("music_songs"), ""),
        ).relatedItems.filterIsInstance<StreamInfoItem>().map { it.toTrackItem(Source.YOUTUBE_MUSIC) }

        // Отдельный запрос за исполнителями — music_songs их не отдаёт.
        val artists = runCatching {
            SearchInfo.getInfo(
                service,
                service.searchQHFactory.fromQuery(query, listOf("music_artists"), ""),
            ).relatedItems.filterIsInstance<ChannelInfoItem>().map { it.toArtistItem(Source.YOUTUBE_MUSIC) }
        }.getOrDefault(emptyList())

        // Исполнителей вперёд (и немного), чтобы пережили обрезку списка.
        return artists.take(3) + songs
    }

    /**
     * Подсказки для поиска через YouTube suggest API.
     * Возвращает список строк — кандидатов для autocomplete.
     */
    fun getSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return try {
            val url = "https://suggestqueries.google.com/complete/search" +
                "?client=firefox&ds=yt&q=${
                    java.net.URLEncoder.encode(query.trim(), "UTF-8")
                }"
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val client = okhttp3.OkHttpClient.Builder()
                .callTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                // Ответ — JSON array: [query, [suggestion1, suggestion2, ...]]
                val json = org.json.JSONArray(body)
                if (json.length() < 2) return emptyList()
                val arr = json.getJSONArray(1)
                (0 until arr.length()).mapNotNull { arr.optString(it)?.takeIf { s -> s.isNotBlank() } }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Треки исполнителя: сперва вкладка канала, иначе фолбэк — поиск по имени.
     * Для YouTube-исполнителей вкладка часто пустая, поэтому фолбэк надёжнее.
     */
    suspend fun artistTracks(context: Context, artist: TrackItem): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val viaChannel = runCatching { channelTracks(artist.url) }.getOrDefault(emptyList())
            if (viaChannel.isNotEmpty()) return@withContext viaChannel

            // Фолбэк: ищем песни по имени исполнителя в его источнике
            // и оставляем только его треки (иначе подмешиваются чужие).
            runCatching {
                val raw = when (artist.source) {
                    Source.SOUNDCLOUD -> {
                        SoundCloudFix.ensure(context)
                        searchService(ServiceList.SoundCloud, artist.title, Source.SOUNDCLOUD, listOf("tracks"))
                    }
                    Source.BANDCAMP -> BandcampSearcher.search(artist.title)
                    else -> searchYouTube(artist.title).filter { it.kind == ItemKind.TRACK }
                }
                val mine = raw.filter { artistMatches(it.uploader, artist.title) }
                mine.ifEmpty { raw }
            }.getOrDefault(emptyList())
        }

    /** Альбомы/релизы исполнителя (вкладка ALBUMS канала). */
    suspend fun artistAlbums(context: Context, artist: TrackItem): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            runCatching {
                val service = serviceFor(artist.url)
                val channel = ChannelInfo.getInfo(service, artist.url)
                val tab = channel.tabs.firstOrNull { lh ->
                    lh.contentFilters.any { it == ChannelTabs.ALBUMS }
                } ?: return@runCatching emptyList()
                ChannelTabInfo.getInfo(service, tab).relatedItems
                    .filterIsInstance<org.schabi.newpipe.extractor.playlist.PlaylistInfoItem>()
                    .map { pl ->
                        TrackItem(
                            title = pl.name,
                            uploader = artist.title,
                            url = pl.url,
                            durationSeconds = 0,
                            thumbnailUrl = pl.thumbnails.maxByOrNull { it.height }?.url
                                ?: pl.thumbnails.firstOrNull()?.url,
                            source = artist.source,
                            kind = ItemKind.ALBUM,
                        )
                    }
            }.getOrDefault(emptyList())
        }

    /** Извлекает video id из YouTube-ссылки (?v=ID). */
    private fun videoId(url: String): String? =
        Regex("[?&]v=([\\w-]+)").find(url)?.groupValues?.get(1)

    /**
     * НАСТОЯЩИЕ рекомендации: «похожее» к треку — основа волны «Sea».
     * Для YouTube берём чистое радио YouTube Music (RDAMVM<id>) — связные песни
     * без мусора (обычный related-граф youtube.com отдаёт роблокс/клипы/реапы).
     * Для SoundCloud/Bandcamp — related/autoplay их сервиса.
     */
    suspend fun relatedTracks(context: Context, seed: TrackItem): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            // YouTube → радио YouTube Music.
            if (!isSoundCloud(seed.url) && !isBandcamp(seed.url)) {
                val vid = videoId(seed.url)
                if (vid != null) {
                    val mix = runCatching {
                        albumTracks(context, "https://music.youtube.com/playlist?list=RDAMVM$vid")
                    }.getOrDefault(emptyList()).filter { videoId(it.url) != vid }
                    if (mix.isNotEmpty()) return@withContext mix
                }
            }
            // SoundCloud/Bandcamp или фолбэк: related-граф сервиса.
            if (isSoundCloud(seed.url)) SoundCloudFix.ensure(context)
            val source = when {
                isSoundCloud(seed.url) -> Source.SOUNDCLOUD
                isBandcamp(seed.url) -> Source.BANDCAMP
                else -> Source.YOUTUBE_MUSIC
            }
            runCatching {
                StreamInfo.getInfo(serviceFor(seed.url), seed.url).relatedItems
                    .filterIsInstance<StreamInfoItem>()
                    .map { it.toTrackItem(source) }
                    .filter { it.url != seed.url }
            }.getOrDefault(emptyList())
        }

    /** Треки альбома/плейлиста по ссылке. */
    suspend fun albumTracks(context: Context, albumUrl: String): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val source = when {
                isSoundCloud(albumUrl) -> Source.SOUNDCLOUD
                isBandcamp(albumUrl) -> Source.BANDCAMP
                else -> Source.YOUTUBE_MUSIC
            }
            runCatching {
                org.schabi.newpipe.extractor.playlist.PlaylistInfo
                    .getInfo(serviceFor(albumUrl), albumUrl)
                    .relatedItems
                    .filterIsInstance<StreamInfoItem>()
                    .map { it.toTrackItem(source) }
            }.getOrDefault(emptyList())
        }

    /** Плейлист по ссылке: имя + играбельные треки (для импорта). */
    suspend fun importPlaylist(context: Context, url: String): Pair<String, List<TrackItem>> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            if (isSoundCloud(url)) SoundCloudFix.ensure(context)
            val source = when {
                isSoundCloud(url) -> Source.SOUNDCLOUD
                isBandcamp(url) -> Source.BANDCAMP
                else -> Source.YOUTUBE_MUSIC
            }
            val info = org.schabi.newpipe.extractor.playlist.PlaylistInfo
                .getInfo(serviceFor(url), url)
            val tracks = info.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toTrackItem(source) }
            (info.name ?: "Импорт") to tracks
        }

    /** Лучшее совпадение по тексту (для матчинга импортируемых треков). */
    suspend fun searchOne(context: Context, query: String): TrackItem? =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            runCatching { searchYouTube(query) }.getOrDefault(emptyList())
                .firstOrNull { it.kind == ItemKind.TRACK }
        }

    /** Совпадает ли исполнитель трека с именем артиста (с учётом " - Topic"). */
    private fun artistMatches(uploader: String?, name: String): Boolean {
        val u = uploader?.lowercase()?.removeSuffix(" - topic")?.trim() ?: return false
        val n = name.lowercase().removeSuffix(" - topic").trim()
        if (u.isBlank() || n.isBlank()) return false
        // Только точное совпадение или uploader, содержащий ПОЛНОЕ имя
        // (иначе «Dazey» подходит под «Dazey and the Scouts»).
        return u == n || u.contains(n)
    }

    private fun channelTracks(channelUrl: String): List<TrackItem> {
        val service = serviceFor(channelUrl)
        val source = when {
            isSoundCloud(channelUrl) -> Source.SOUNDCLOUD
            isBandcamp(channelUrl) -> Source.BANDCAMP
            else -> Source.YOUTUBE_MUSIC
        }
        val channel = ChannelInfo.getInfo(service, channelUrl)
        val tab = channel.tabs.firstOrNull { lh ->
            lh.contentFilters.any { it == ChannelTabs.TRACKS || it == ChannelTabs.VIDEOS }
        } ?: channel.tabs.firstOrNull() ?: return emptyList()
        val tabInfo = ChannelTabInfo.getInfo(service, tab)
        return tabInfo.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toTrackItem(source) }
    }

    private fun searchService(
        service: StreamingService,
        query: String,
        source: Source,
        contentFilters: List<String>?,
    ): List<TrackItem> {
        val handler = if (contentFilters != null) {
            service.searchQHFactory.fromQuery(query, contentFilters, "")
        } else {
            service.searchQHFactory.fromQuery(query)
        }
        val info = SearchInfo.getInfo(service, handler)
        return info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { it.toTrackItem(source) }
    }

    /**
     * «Рекомендации» — популярная музыка с YouTube Music по региону устройства.
     * Фильтр music_songs уводит запрос на music.youtube.com, а ContentCountry/
     * Localization делают выдачу локальной (RU → русские песни).
     */
    suspend fun recommendations(context: Context): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val service = ServiceList.YouTube
            val handler = service.searchQHFactory.fromQuery(
                popularMusicSeed(),
                listOf("music_songs"),
                "",
            )
            val info = SearchInfo.getInfo(service, handler)
            info.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toTrackItem(Source.YOUTUBE_MUSIC) }
        }

    /** Полка главной: YouTube Music по произвольному seed-запросу (региональная выдача). */
    suspend fun shelf(context: Context, seed: String): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val service = ServiceList.YouTube
            val handler = service.searchQHFactory.fromQuery(seed, listOf("music_songs"), "")
            val info = SearchInfo.getInfo(service, handler)
            info.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toTrackItem(Source.YOUTUBE_MUSIC) }
        }

    /** Локализованный «затравочный» запрос под популярную музыку. */
    private fun popularMusicSeed(): String = when (Locale.getDefault().language) {
        "ru" -> "популярная музыка"
        "uk" -> "популярна музика"
        "es" -> "música popular"
        "fr" -> "musique populaire"
        "de" -> "beliebte musik"
        else -> "popular music"
    }

    private fun StreamInfoItem.toTrackItem(source: Source) = TrackItem(
        title = name,
        uploader = uploaderName?.takeIf { it.isNotBlank() },
        url = url,
        durationSeconds = duration,
        thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url
            ?: thumbnails.firstOrNull()?.url,
        source = source,
        kind = ItemKind.TRACK,
    )

    private fun ChannelInfoItem.toArtistItem(source: Source) = TrackItem(
        title = name,
        uploader = "Исполнитель",
        url = url,
        durationSeconds = 0,
        thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url
            ?: thumbnails.firstOrNull()?.url,
        source = source,
        kind = ItemKind.ARTIST,
    )
}
