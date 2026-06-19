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
        val client = OkHttpClient.Builder()
            .cache(Cache(cacheDir, 64L * 1024 * 1024))
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

    /** Поддерживается ли URL движком NewPipe (иначе — yt-dlp). */
    fun isSupported(url: String): Boolean = isYouTube(url) || isSoundCloud(url)

    private fun serviceFor(url: String): StreamingService =
        if (isSoundCloud(url)) ServiceList.SoundCloud else ServiceList.YouTube

    suspend fun resolve(context: Context, url: String): ResolvedTrack = withContext(Dispatchers.IO) {
        val t0 = android.os.SystemClock.elapsedRealtime()
        ensureInit(context)
        if (isSoundCloud(url)) SoundCloudFix.ensure(context)
        val t1 = android.os.SystemClock.elapsedRealtime()
        val info = StreamInfo.getInfo(serviceFor(url), url)
        val t2 = android.os.SystemClock.elapsedRealtime()
        val candidates = info.audioStreams.filter { it.content.isNotBlank() }
        // Предпочитаем прогрессивный поток (проще для ExoPlayer), иначе любой (HLS).
        val audio = candidates.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxByOrNull { it.averageBitrate }
            ?: candidates.maxByOrNull { it.averageBitrate }
            ?: throw IllegalStateException("NewPipe не нашёл аудио-потоков")
        val t3 = android.os.SystemClock.elapsedRealtime()
        android.util.Log.e(
            "MeloPerf",
            "resolve init=${t1 - t0}ms getInfo=${t2 - t1}ms pick=${t3 - t2}ms TOTAL=${t3 - t0}ms",
        )

        val author = info.uploaderName?.takeIf { it.isNotBlank() }
        ResolvedTrack(
            title = if (author != null) "${info.name} — $author" else info.name,
            audioUrl = audio.content,
        )
    }

    /**
     * Потоковый поиск: результаты приходят по мере готовности источника, а не «махом».
     * YouTube обычно быстрее (~0.4с), SoundCloud — позже (~2с) и дописывается снизу.
     * Каждый emit — это накопленный список (исполнители сверху, затем треки).
     */
    fun search(context: Context, query: String): Flow<List<TrackItem>> = channelFlow {
        ensureInit(context)
        val acc = mutableListOf<TrackItem>()
        val mutex = Mutex()

        suspend fun emitFrom(tag: String, block: () -> List<TrackItem>) {
            val part = runCatching { block() }
                .onFailure { android.util.Log.e("MeloSearch", "$tag failed: $it", it) }
                .getOrDefault(emptyList())
            android.util.Log.e("MeloSearch", "$tag → ${part.size} items")
            if (part.isEmpty()) return
            mutex.withLock {
                acc.addAll(part)
                // Исполнители — наверх, треки — ниже (стабильно).
                send(acc.sortedBy { if (it.kind == ItemKind.ARTIST) 0 else 1 })
            }
        }

        launch(Dispatchers.IO) {
            emitFrom("YouTube") { searchYouTube(query) }
        }
        launch(Dispatchers.IO) {
            emitFrom("SoundCloud") {
                SoundCloudFix.ensure(context) // client_id с рабочих хостов, минуя soundcloud.com
                searchService(ServiceList.SoundCloud, query, Source.SOUNDCLOUD, listOf("tracks"))
            }
        }
    }

    /** YouTube: треки + исполнители (каналы) в одной выдаче. */
    private fun searchYouTube(query: String): List<TrackItem> {
        val service = ServiceList.YouTube
        val info = SearchInfo.getInfo(service, service.searchQHFactory.fromQuery(query))
        return info.relatedItems.mapNotNull { item ->
            when (item) {
                is StreamInfoItem -> item.toTrackItem(Source.YOUTUBE_MUSIC)
                is ChannelInfoItem -> item.toArtistItem(Source.YOUTUBE_MUSIC)
                else -> null
            }
        }
    }

    /** Треки исполнителя/канала: открываем вкладку Tracks/Videos канала. */
    suspend fun artistTracks(context: Context, channelUrl: String): List<TrackItem> =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val service = serviceFor(channelUrl)
            val source = if (isSoundCloud(channelUrl)) Source.SOUNDCLOUD else Source.YOUTUBE_MUSIC
            val channel = ChannelInfo.getInfo(service, channelUrl)
            val tab = channel.tabs.firstOrNull { lh ->
                lh.contentFilters.any {
                    it == ChannelTabs.TRACKS || it == ChannelTabs.VIDEOS
                }
            } ?: channel.tabs.firstOrNull()
            ?: return@withContext emptyList()
            val tabInfo = ChannelTabInfo.getInfo(service, tab)
            tabInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toTrackItem(source) }
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
