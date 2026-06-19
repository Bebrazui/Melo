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
)

/** Источник трека (для индикатора в UI). */
enum class Source { YOUTUBE_MUSIC, SOUNDCLOUD }

/** Тип элемента списка: воспроизводимый трек или исполнитель/канал. */
enum class ItemKind { TRACK, ARTIST }

/** Элемент списка (поиск / рекомендации) — ещё без прямого аудио-URL. */
data class TrackItem(
    val title: String,
    val uploader: String?,
    val url: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val source: Source,
    val kind: ItemKind = ItemKind.TRACK,
)

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

    /** Очередь фонового prefetch: один воркер, чтобы не конкурировать с тапом. */
    @Volatile
    private var appCtx: Context? = null
    private val prefetchQueue = Channel<String>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (url in prefetchQueue) {
                val ctx = appCtx ?: continue
                if (!isCached(url)) runCatching { resolveAudioUrl(ctx, url) }
            }
        }
    }

    /** Поставить трек в фоновую очередь предзагрузки стрим-URL. */
    fun prefetch(context: Context, url: String) {
        if (isCached(url)) return
        appCtx = context.applicationContext
        prefetchQueue.trySend(url)
    }

    /** Уже есть в кэше? (чтобы не дёргать prefetch повторно) */
    fun isCached(url: String): Boolean = streamCache.get(url) != null

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

    suspend fun resolveAudioUrl(context: Context, url: String): ResolvedTrack {
        streamCache.get(url)?.let {
            android.util.Log.e("MeloPerf", "CACHE HIT $url")
            return it
        }
        val app = context.applicationContext
        // Дедуп: если этот URL уже резолвится — ждём тот же результат.
        val deferred = inFlight.getOrPut(url) {
            android.util.Log.e("MeloPerf", "CACHE MISS → resolve $url")
            scope.async {
                try {
                    // YouTube и SoundCloud — через NewPipe, остальное — через yt-dlp.
                    val resolved = if (NewPipeResolver.isSupported(url)) {
                        NewPipeResolver.resolve(app, url)
                    } else {
                        resolveWithYtDlp(app, url)
                    }
                    streamCache.put(url, resolved)
                    resolved
                } finally {
                    inFlight.remove(url)
                }
            }
        }
        return deferred.await()
    }

    private suspend fun resolveWithYtDlp(context: Context, url: String): ResolvedTrack =
        withContext(Dispatchers.IO) {
            ensureInit(context)
            val request = YoutubeDLRequest(url).apply {
                addOption("-f", "bestaudio/best")
                // Не лезем в плейлисты, берём один трек.
                addOption("--no-playlist")
            }
            val info = YoutubeDL.getInstance().getInfo(request)
            val audioUrl = info.url
                ?: throw IllegalStateException("yt-dlp не вернул прямой аудио-URL")
            ResolvedTrack(
                title = info.title ?: url,
                audioUrl = audioUrl,
            )
        }
}
