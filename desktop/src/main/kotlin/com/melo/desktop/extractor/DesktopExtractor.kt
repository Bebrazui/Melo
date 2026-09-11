package com.melo.desktop.extractor

import com.melo.desktop.net.DesktopMeloNet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Главный экстрактор каталога для Melo Desktop.
 * Работает через pure JVM NewPipe Extractor.
 */
object DesktopExtractor {

    @Volatile
    private var initialized = false

    private val streamCache = ConcurrentHashMap<String, ResolvedTrack>()

    val countryCode: String
        get() = Locale.getDefault().country.ifBlank { "RU" }

    @Synchronized
    fun ensureInit() {
        if (initialized) return
        val cacheDir = File(System.getProperty("user.home"), ".melo/cache/http")
        cacheDir.mkdirs()

        val client = DesktopMeloNet.okHttpClient.newBuilder()
            .cache(Cache(cacheDir, 64L * 1024 * 1024))
            .build()

        val locale = Locale.getDefault()
        val localization = Localization(
            locale.language.ifBlank { "ru" },
            locale.country.ifBlank { "RU" },
        )
        NewPipe.init(DesktopOkHttpDownloader(client), localization, ContentCountry(countryCode))
        initialized = true
    }

    fun isYouTube(url: String): Boolean =
        url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true)

    fun isSoundCloud(url: String): Boolean =
        url.contains("soundcloud.com", ignoreCase = true)

    fun isBandcamp(url: String): Boolean =
        url.contains("bandcamp.com", ignoreCase = true)

    private fun serviceFor(url: String): StreamingService = when {
        isSoundCloud(url) -> ServiceList.SoundCloud
        isBandcamp(url) -> ServiceList.Bandcamp
        else -> ServiceList.YouTube
    }

    /** Резолвит прямой аудиопоток для трека. */
    suspend fun resolveAudioUrl(url: String): ResolvedTrack = withContext(Dispatchers.IO) {
        streamCache[url]?.let { return@withContext it }
        com.melo.desktop.storage.DesktopStreamCacheStore.get(url)?.let {
            streamCache[url] = it
            return@withContext it
        }
        ensureInit()

        val service = serviceFor(url)
        val info = StreamInfo.getInfo(service, url)

        val candidates = info.audioStreams.filter { it.content.isNotBlank() }

        val audio = if (isSoundCloud(url)) {
            // SoundCloud: берём HLS или прогрессивный MP3
            val hls = candidates.filter { it.deliveryMethod == DeliveryMethod.HLS }
            hls.filter { it.averageBitrate in 1 until 160 }.maxByOrNull { it.averageBitrate }
                ?: hls.firstOrNull()
                ?: candidates.maxByOrNull { it.averageBitrate }
        } else {
            // YouTube: предпочитаем M4A (AAC), так как JavaFX Media нативно поддерживает AAC
            val m4aStreams = candidates.filter { it.format?.suffix.equals("m4a", ignoreCase = true) }
            m4aStreams.maxByOrNull { it.averageBitrate }
                ?: candidates.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }.maxByOrNull { it.averageBitrate }
                ?: candidates.maxByOrNull { it.averageBitrate }
        } ?: throw IllegalStateException("Не найден аудиопоток для: $url")

        val author = info.uploaderName?.takeIf { it.isNotBlank() }
        val source = when {
            isSoundCloud(url) -> Source.SOUNDCLOUD
            isBandcamp(url) -> Source.BANDCAMP
            else -> Source.YOUTUBE_MUSIC
        }

        val video = if (isYouTube(url)) {
            info.videoStreams.filter { it.content.isNotBlank() && !it.isVideoOnly }
                .maxByOrNull { it.height }?.content
        } else null

        var trackDur = info.duration.takeIf { it > 0 } ?: 0L
        if (trackDur <= 0L) {
            trackDur = Regex("[?&]dur=([0-9]+(?:\\.[0-9]+)?)").find(audio.content)?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: 0L
        }

        val resolved = ResolvedTrack(
            title = info.name,
            audioUrl = audio.content,
            thumbnailUrl = info.thumbnails.maxByOrNull { it.height }?.url ?: info.thumbnails.firstOrNull()?.url,
            artist = author,
            videoUrl = video,
            source = source,
            originalUrl = url,
            durationSeconds = trackDur,
        )

        streamCache[url] = resolved
        com.melo.desktop.storage.DesktopStreamCacheStore.put(url, resolved)
        resolved
    }

    /** Поиск треков с прогрессивной выдачей. */
    fun search(query: String): Flow<List<TrackItem>> = channelFlow {
        ensureInit()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            send(emptyList())
            return@channelFlow
        }

        val mutex = Mutex()
        val allItems = mutableListOf<TrackItem>()

        suspend fun emitBatch(block: suspend () -> List<TrackItem>) {
            val part = runCatching { block() }.getOrDefault(emptyList())
            if (part.isEmpty()) return
            mutex.withLock {
                allItems.addAll(part)
                send(allItems.distinctBy { it.url })
            }
        }

        launch(Dispatchers.IO) {
            emitBatch { searchYouTube(trimmed) }
        }
        launch(Dispatchers.IO) {
            emitBatch { searchSoundCloud(trimmed) }
        }
    }

    private suspend fun searchYouTube(query: String): List<TrackItem> = coroutineScope {
        val service = ServiceList.YouTube

        val songsDef = async {
            runCatching {
                SearchInfo.getInfo(
                    service,
                    service.searchQHFactory.fromQuery(query, listOf("music_songs"), ""),
                ).relatedItems.filterIsInstance<StreamInfoItem>().map { it.toTrackItem(Source.YOUTUBE_MUSIC) }
            }.getOrDefault(emptyList())
        }

        val artistsDef = async {
            runCatching {
                SearchInfo.getInfo(
                    service,
                    service.searchQHFactory.fromQuery(query, listOf("music_artists"), ""),
                ).relatedItems.filterIsInstance<ChannelInfoItem>().map { it.toArtistItem(Source.YOUTUBE_MUSIC) }
            }.getOrDefault(emptyList())
        }

        val albumsDef = async {
            runCatching {
                SearchInfo.getInfo(
                    service,
                    service.searchQHFactory.fromQuery(query, listOf("music_albums"), ""),
                ).relatedItems.filterIsInstance<PlaylistInfoItem>().map { it.toAlbumItem(Source.YOUTUBE_MUSIC) }
            }.getOrDefault(emptyList())
        }

        val artists = artistsDef.await()
        val albums = albumsDef.await()
        val songs = songsDef.await()

        artists.take(2) + albums.take(2) + songs
    }

    private fun searchSoundCloud(query: String): List<TrackItem> {
        return runCatching {
            val service = ServiceList.SoundCloud
            val handler = service.searchQHFactory.fromQuery(query, listOf("tracks"), "")
            SearchInfo.getInfo(service, handler).relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toTrackItem(Source.SOUNDCLOUD) }
        }.getOrDefault(emptyList())
    }

    /** Подсказки в строке поиска. */
    fun getSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return try {
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=" +
                URLEncoder.encode(query.trim(), "UTF-8")
            val request = Request.Builder().url(url).build()
            DesktopMeloNet.okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val json = JSONArray(body)
                if (json.length() < 2) return emptyList()
                val arr = json.getJSONArray(1)
                (0 until arr.length()).mapNotNull { arr.optString(it)?.takeIf { s -> s.isNotBlank() } }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Популярная музыка для региона. */
    suspend fun recommendations(): List<TrackItem> = withContext(Dispatchers.IO) {
        ensureInit()
        val service = ServiceList.YouTube
        val seed = if (Locale.getDefault().language == "ru") "популярная музыка" else "popular music"
        val handler = service.searchQHFactory.fromQuery(seed, listOf("music_songs"), "")
        SearchInfo.getInfo(service, handler).relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { it.toTrackItem(Source.YOUTUBE_MUSIC) }
    }

    /** Полка треков по теме / жанру. */
    suspend fun shelf(seed: String): List<TrackItem> = withContext(Dispatchers.IO) {
        ensureInit()
        val service = ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(seed, listOf("music_songs"), "")
        SearchInfo.getInfo(service, handler).relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { it.toTrackItem(Source.YOUTUBE_MUSIC) }
    }

    /** Радио трека (YouTube Music mix RDAMVM) для «Моей волны». */
    suspend fun relatedTracks(seed: TrackItem): List<TrackItem> = withContext(Dispatchers.IO) {
        ensureInit()
        val vid = videoId(seed.url)
        if (vid != null) {
            val mix = runCatching {
                albumTracks("https://music.youtube.com/playlist?list=RDAMVM$vid")
            }.getOrDefault(emptyList()).filter { videoId(it.url) != vid }
            if (mix.isNotEmpty()) return@withContext mix
        }

        runCatching {
            StreamInfo.getInfo(serviceFor(seed.url), seed.url).relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toTrackItem(seed.source) }
                .filter { it.url != seed.url }
        }.getOrDefault(emptyList())
    }

    /** Треки альбома или плейлиста. */
    suspend fun albumTracks(url: String): List<TrackItem> = withContext(Dispatchers.IO) {
        ensureInit()
        val source = if (isSoundCloud(url)) Source.SOUNDCLOUD else Source.YOUTUBE_MUSIC
        runCatching {
            PlaylistInfo.getInfo(serviceFor(url), url).relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toTrackItem(source) }
        }.getOrDefault(emptyList())
    }

    /** Официальные треки исполнителя. */
    suspend fun artistTracks(artist: TrackItem): List<TrackItem> = withContext(Dispatchers.IO) {
        ensureInit()
        runCatching {
            val service = ServiceList.YouTube
            val channel = ChannelInfo.getInfo(service, artist.url)
            val tab = channel.tabs.firstOrNull { lh ->
                lh.contentFilters.any { it == ChannelTabs.TRACKS || it == ChannelTabs.VIDEOS }
            } ?: channel.tabs.firstOrNull() ?: return@runCatching emptyList()

            ChannelTabInfo.getInfo(service, tab).relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toTrackItem(artist.source) }
        }.getOrDefault(emptyList())
    }

    private fun videoId(url: String): String? =
        Regex("[?&]v=([\\w-]+)").find(url)?.groupValues?.get(1)
            ?: Regex("youtu\\.be/([\\w-]+)").find(url)?.groupValues?.get(1)

    private fun StreamInfoItem.toTrackItem(src: Source) = TrackItem(
        title = name,
        uploader = uploaderName?.takeIf { it.isNotBlank() },
        url = url,
        durationSeconds = duration,
        thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url ?: thumbnails.firstOrNull()?.url,
        source = src,
        kind = ItemKind.TRACK,
        viewCount = runCatching { viewCount }.getOrDefault(0L),
    )

    private fun ChannelInfoItem.toArtistItem(src: Source) = TrackItem(
        title = name,
        uploader = "Исполнитель",
        url = url,
        durationSeconds = 0,
        thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url ?: thumbnails.firstOrNull()?.url,
        source = src,
        kind = ItemKind.ARTIST,
    )

    private fun PlaylistInfoItem.toAlbumItem(src: Source) = TrackItem(
        title = name,
        uploader = uploaderName?.takeIf { it.isNotBlank() },
        url = url,
        durationSeconds = 0,
        thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url ?: thumbnails.firstOrNull()?.url,
        source = src,
        kind = ItemKind.ALBUM,
    )
}
