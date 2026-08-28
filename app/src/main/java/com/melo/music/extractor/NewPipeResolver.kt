package com.melo.music.extractor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
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

    /** YouTube Music: параллельный поиск песен (music_songs) + исполнителей (music_artists) + альбомов (music_albums). */
    private suspend fun searchYouTube(query: String): List<TrackItem> = coroutineScope {
        val service = ServiceList.YouTube
        val songsDeferred = async {
            runCatching {
                SearchInfo.getInfo(
                    service,
                    service.searchQHFactory.fromQuery(query, listOf("music_songs"), ""),
                ).relatedItems.filterIsInstance<StreamInfoItem>().map { it.toTrackItem(Source.YOUTUBE_MUSIC) }
            }.getOrDefault(emptyList())
        }

        // Отдельный запрос за исполнителями — music_songs их не отдаёт.
        val artistsDeferred = async {
            runCatching {
                SearchInfo.getInfo(
                    service,
                    service.searchQHFactory.fromQuery(query, listOf("music_artists"), ""),
                ).relatedItems.filterIsInstance<ChannelInfoItem>().map { it.toArtistItem(Source.YOUTUBE_MUSIC) }
            }.getOrDefault(emptyList())
        }

        // Альбомы/релизы — тоже отдельный фильтр выдачи YouTube Music.
        val albumsDeferred = async {
            runCatching {
                SearchInfo.getInfo(
                    service,
                    service.searchQHFactory.fromQuery(query, listOf("music_albums"), ""),
                ).relatedItems.filterIsInstance<PlaylistInfoItem>().map { it.toAlbumItem(Source.YOUTUBE_MUSIC) }
            }.getOrDefault(emptyList())
        }

        val artists = artistsDeferred.await()
        val albums = albumsDeferred.await()
        val songs = songsDeferred.await()

        // Исполнителей и альбомов вперёд (и немного), чтобы пережили обрезку списка.
        artists.take(3) + albums.take(3) + songs
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

    /** Разрешает официальный YouTube Music канал артиста по имени или URL. */
    suspend fun resolveArtistChannelUrl(artist: TrackItem): String? = withContext(Dispatchers.IO) {
        if (isYouTube(artist.url) && (artist.url.contains("/channel/") || artist.url.contains("/@") || artist.url.contains("/user/"))) {
            return@withContext artist.url
        }
        runCatching {
            val search = SearchInfo.getInfo(
                ServiceList.YouTube,
                ServiceList.YouTube.searchQHFactory.fromQuery(artist.title, listOf("music_artists"), "")
            )
            val channels = search.relatedItems.filterIsInstance<ChannelInfoItem>()
            val target = normalizeArtistName(artist.title)
            val exact = channels.firstOrNull { ch ->
                normalizeArtistName(ch.name) == target
            }
            exact?.url ?: channels.firstOrNull()?.url
        }.getOrNull()
    }

    /**
     * Треки исполнителя: сперва официальная вкладка треков канала артиста,
     * а затем точный поиск треков с фильтрацией по исполнителю.
     */
    suspend fun artistTracks(context: Context, artist: TrackItem): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val channelUrl = if (artist.url.startsWith("http") && (artist.url.contains("/channel/") || artist.url.contains("/@") || artist.url.contains("/user/"))) {
                artist.url
            } else {
                resolveArtistChannelUrl(artist)
            }

            val viaChannel = if (channelUrl != null && channelUrl.startsWith("http")) {
                runCatching { channelTracks(channelUrl) }
                    .onFailure {
                        android.util.Log.e(
                            "MeloArtist",
                            "artistTracks viaChannel FAILED: ${it.javaClass.simpleName}: ${it.message}",
                        )
                    }
                    .getOrDefault(emptyList())
            } else {
                emptyList()
            }

            // Поиск песен артиста (music_songs на YouTube Music либо tracks на SoundCloud)
            val fromSearch = runCatching {
                val raw = when (artist.source) {
                    Source.SOUNDCLOUD -> {
                        SoundCloudFix.ensure(context)
                        searchService(ServiceList.SoundCloud, artist.title, Source.SOUNDCLOUD, listOf("tracks"))
                    }
                    Source.BANDCAMP -> BandcampSearcher.search(artist.title)
                    else -> searchService(ServiceList.YouTube, artist.title, Source.YOUTUBE_MUSIC, listOf("music_songs"))
                }
                raw.filter { artistMatches(it.uploader, artist.title, it.title) }
            }.getOrDefault(emptyList())

            android.util.Log.e(
                "MeloArtist",
                "artistTracks ${artist.title.take(24)}: channel=${viaChannel.size} search=${fromSearch.size}",
            )
            (viaChannel + fromSearch).distinctBy { it.url }
        }

    /** Альбомы/релизы исполнителя (вкладка ALBUMS канала, иначе поиск music_albums). */
    suspend fun artistAlbums(context: Context, artist: TrackItem): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val channelUrl = if (artist.url.startsWith("http") && (artist.url.contains("/channel/") || artist.url.contains("/@") || artist.url.contains("/user/"))) {
                artist.url
            } else {
                resolveArtistChannelUrl(artist)
            }

            val viaChannel = if (channelUrl != null && channelUrl.startsWith("http")) {
                runCatching {
                    val service = serviceFor(channelUrl)
                    val channel = ChannelInfo.getInfo(service, channelUrl)
                    val tab = channel.tabs.firstOrNull { lh ->
                        lh.contentFilters.any { it == ChannelTabs.ALBUMS }
                    } ?: throw IllegalStateException("no ALBUMS tab, all=${channel.tabs.map { it.contentFilters }}")
                    ChannelTabInfo.getInfo(service, tab).relatedItems
                        .filterIsInstance<PlaylistInfoItem>()
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
                }.onFailure {
                    android.util.Log.e(
                        "MeloArtist",
                        "albums viaChannel FAILED: ${it.javaClass.simpleName}: ${it.message}",
                    )
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            if (viaChannel.isNotEmpty() || !isYouTube(channelUrl ?: artist.url)) return@withContext viaChannel

            // Фолбэк для YouTube: поиск music_albums по имени
            runCatching {
                SearchInfo.getInfo(
                    ServiceList.YouTube,
                    ServiceList.YouTube.searchQHFactory.fromQuery(artist.title, listOf("music_albums"), ""),
                ).relatedItems.filterIsInstance<PlaylistInfoItem>()
                    .map { it.toAlbumItem(Source.YOUTUBE_MUSIC) }
                    .let { raw ->
                        val mine = raw.filter { albumMatches(it.uploader, artist.title) }
                        if (mine.isNotEmpty()) mine else raw.take(8)
                    }
            }.getOrDefault(emptyList())
        }

    /** Совпадение релиза с исполнителем (uploader может отсутствовать — тогда false). */
    private fun albumMatches(uploader: String?, artistName: String): Boolean {
        val u = uploader?.lowercase()?.trim().takeUnless { it.isNullOrBlank() } ?: return false
        val n = normalizeArtistName(artistName)
        if (n.isBlank()) return false
        val uClean = normalizeArtistName(u)
        if (uClean == n) return true
        val pattern = Regex("\\b" + Regex.escape(n) + "\\b", RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(uClean)
    }

    /** Извлекает video id из YouTube-ссылки (?v=ID или youtu.be/ID). */
    private fun videoId(url: String): String? =
        Regex("[?&]v=([\\w-]+)").find(url)?.groupValues?.get(1)
            ?: Regex("youtu\\.be/([\\w-]+)").find(url)?.groupValues?.get(1)

    private fun watchUrlFor(url: String): String? =
        videoId(url)?.let { "https://www.youtube.com/watch?v=$it" }

    /**
     * Статистика трека с YouTube: просмотры, лайки, число комментариев.
     * null = источник не YouTube либо данные недоступны.
     */
    suspend fun trackStats(context: Context, item: TrackItem): TrackStats? =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val watch = watchUrlFor(item.url) ?: return@withContext null
            runCatching {
                val info = StreamInfo.getInfo(ServiceList.YouTube, watch)
                val commentsCount = runCatching {
                    org.schabi.newpipe.extractor.comments.CommentsInfo
                        .getInfo(ServiceList.YouTube, watch).commentsCount
                }.getOrDefault(-1)
                TrackStats(
                    viewCount = info.viewCount,
                    likeCount = if (info.likeCount > 0) info.likeCount else -1L,
                    commentsCount = commentsCount,
                )
            }.onFailure {
                android.util.Log.e("MeloArtist", "trackStats FAILED: ${it.javaClass.simpleName}: ${it.message}")
            }.getOrNull()
        }

    /** Топ-комментарии к YouTube-треку. */
    suspend fun trackComments(context: Context, item: TrackItem, maxCount: Int = 15): List<TrackComment> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val watch = watchUrlFor(item.url) ?: return@withContext emptyList()
            runCatching {
                val info = org.schabi.newpipe.extractor.comments.CommentsInfo
                    .getInfo(ServiceList.YouTube, watch)
                info.relatedItems.take(maxCount).map { c ->
                    TrackComment(
                        author = c.uploaderName ?: "",
                        text = c.commentText?.content.orEmpty(),
                        likeCount = c.likeCount,
                        dateText = c.textualUploadDate,
                        authorAvatar = c.uploaderAvatars.lastOrNull()?.url
                            ?: c.uploaderAvatars.firstOrNull()?.url,
                    )
                }
            }.onFailure {
                android.util.Log.e("MeloArtist", "trackComments FAILED: ${it.javaClass.simpleName}: ${it.message}")
            }.getOrDefault(emptyList())
        }

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

    /**
     * Похожие исполнители: берём related-граф топ-трека исполнителя, собираем
     * чужие имена uploaders и подтягиваем на каждого настоящего артиста
     * (аватар + канал) через music_artists поиск. Максимум 4 запроса параллельно.
     *
     * [seedTracks] — уже загруженные треки исполнителя (чтобы не качать вкладку
     * канала второй раз); если пусто, загрузим сами.
     */
    suspend fun similarArtists(
        context: Context,
        artist: TrackItem,
        seedTracks: List<TrackItem>? = null,
    ): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val own = seedTracks?.takeIf { it.isNotEmpty() }
                ?: runCatching { artistTracks(context, artist) }.getOrDefault(emptyList())
            val top = own.maxByOrNull { it.viewCount } ?: own.firstOrNull()
                ?: return@withContext emptyList()
            val related = runCatching { relatedTracks(context, top) }.getOrDefault(emptyList())
            val self = normalizeArtistName(artist.title)
            val names = related.asSequence()
                .mapNotNull { it.uploader?.let(::normalizeArtistName) }
                .filter { it.isNotBlank() && it != self }
                .distinct()
                .take(4)
                .toList()
            if (names.isEmpty()) return@withContext emptyList()
            coroutineScope {
                names.map { name ->
                    async {
                        runCatching {
                            SearchInfo.getInfo(
                                ServiceList.YouTube,
                                ServiceList.YouTube.searchQHFactory.fromQuery(name, listOf("music_artists"), ""),
                            ).relatedItems.filterIsInstance<ChannelInfoItem>()
                                .firstOrNull()?.toArtistItem(Source.YOUTUBE_MUSIC)
                        }.getOrNull()
                    }
                }.awaitAll().filterNotNull()
                    .filter { normalizeArtistName(it.title) != self }
                    .distinctBy { it.url }
                    .take(6)
            }
        }

    private fun normalizeArtistName(name: String): String =
        name.lowercase()
            .removeSuffix(" - topic")
            .removeSuffix(" official")
            .removeSuffix("vevo")
            .replace(Regex("[\"']"), "")
            .trim()

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

    /** Совпадает ли исполнитель трека с именем артиста (с учётом Topic, VEVO, коллабораций и названий). */
    private fun artistMatches(uploader: String?, name: String, trackTitle: String? = null): Boolean {
        val n = normalizeArtistName(name)
        if (n.isBlank()) return false
        val u = uploader?.let(::normalizeArtistName)
        if (u != null && u.isNotBlank()) {
            if (u == n) return true
            // Поддержка коллабораций: "Artist 1, Artist 2" или "Artist 1 feat. Artist 2"
            val parts = u.split(Regex("[,&/+]|\\b(feat\\.?|ft\\.?|featuring|vs\\.?|with|x|и)\\b", RegexOption.IGNORE_CASE))
            if (parts.any { normalizeArtistName(it) == n }) return true
            // Матчинг по границам слов (чтобы "OG" не подходило под "OG Buda", "Платина" не цепляло сторонние фразы)
            val pattern = Regex("\\b" + Regex.escape(n) + "\\b", RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(u)) return true
        }

        if (trackTitle != null) {
            val titleLower = trackTitle.lowercase().trim()
            if (titleLower.startsWith("$n - ") || titleLower.startsWith("$n – ") || titleLower.startsWith("$n — ") || titleLower.startsWith("$n : ")) {
                return true
            }
        }
        return false
    }

    private fun channelTracks(channelUrl: String): List<TrackItem> {
        val service = serviceFor(channelUrl)
        val source = when {
            isSoundCloud(channelUrl) -> Source.SOUNDCLOUD
            isBandcamp(channelUrl) -> Source.BANDCAMP
            else -> Source.YOUTUBE_MUSIC
        }
        val channel = runCatching { ChannelInfo.getInfo(service, channelUrl) }
            .onFailure {
                android.util.Log.e(
                    "MeloArtist",
                    "ChannelInfo FAILED $channelUrl: ${it.javaClass.simpleName}: ${it.message}",
                )
            }
            .getOrNull() ?: return emptyList()
        val tab = channel.tabs.firstOrNull { lh ->
            lh.contentFilters.any { it == ChannelTabs.TRACKS || it == ChannelTabs.VIDEOS }
        } ?: channel.tabs.firstOrNull() ?: run {
            android.util.Log.e("MeloArtist", "no usable tab, all=${channel.tabs.map { it.contentFilters }}")
            return emptyList()
        }
        val tabInfo = ChannelTabInfo.getInfo(service, tab)
        val items = tabInfo.relatedItems.filterIsInstance<StreamInfoItem>().toMutableList()
        android.util.Log.e("MeloArtist", "tab=${tab.contentFilters} firstPage=${items.size}")
        // Пагинация вкладки канала: без этого видны только первые ~30 треков.
        var page = tabInfo.nextPage
        var guard = 0
        while (org.schabi.newpipe.extractor.Page.isValid(page) && guard < 25) {
            val result = runCatching {
                ChannelTabInfo.getMoreItems(service, tab, page)
            }.onFailure {
                android.util.Log.e("MeloArtist", "page ${guard + 1} FAILED: ${it.javaClass.simpleName}: ${it.message}")
            }.getOrNull() ?: break
            val more = result.items.filterIsInstance<StreamInfoItem>()
            if (more.isEmpty()) break
            items += more
            android.util.Log.e("MeloArtist", "page ${guard + 1}: +${more.size} (total ${items.size})")
            page = if (result.hasNextPage()) result.nextPage else null
            guard++
        }
        return items.distinctBy { it.url }.map { it.toTrackItem(source) }
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
        viewCount = runCatching { viewCount }.getOrDefault(0L),
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

    private fun PlaylistInfoItem.toAlbumItem(source: Source) = TrackItem(
        title = name,
        uploader = uploaderName?.takeIf { it.isNotBlank() },
        url = url,
        durationSeconds = 0,
        thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url
            ?: thumbnails.firstOrNull()?.url,
        source = source,
        kind = ItemKind.ALBUM,
    )
}

/** Статистика трека с YouTube. */
data class TrackStats(
    val viewCount: Long,
    /** -1 = лайки скрыты или недоступны. */
    val likeCount: Long,
    /** -1 = число комментариев неизвестно. */
    val commentsCount: Int,
)

/** Комментарий под YouTube-треком. */
data class TrackComment(
    val author: String,
    val text: String,
    val likeCount: Int,
    val dateText: String?,
    val authorAvatar: String?,
)
