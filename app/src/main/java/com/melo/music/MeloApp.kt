package com.melo.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.melo.music.audio.EqualizerManager
import com.melo.music.byedpi.ByeDpiProxy
import com.melo.music.crash.CrashHandler
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.NewPipeResolver
import com.melo.music.extractor.SoundCloudFix
import com.melo.music.extractor.StreamCacheStore
import com.melo.music.favorites.FavoritesManager
import com.melo.music.history.HistoryManager
import com.melo.music.lyrics.LyricsRepository
import com.melo.music.playlists.PlaylistManager
import com.melo.music.recommend.Recommender
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Точка входа приложения. Прогревает yt-dlp (распаковку Python) в фоне
 * и настраивает загрузчик обложек Coil.
 *
 * Позже здесь же инициализируем Hilt-граф и Room.
 */
class MeloApp : Application(), ImageLoaderFactory {

    /** Лимит одновременных запросов обложек SoundCloud (i1-i4): при шторме ByeDPI рвёт коннекты. */
    private val scImgSemaphore = java.util.concurrent.Semaphore(4)

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        FavoritesManager.init(this)
        PlaylistManager.init(this)
        com.melo.music.auth.LoginGuard.init(this)
        com.melo.music.auth.AuthManager.init(this)
        HistoryManager.init(this)
        EqualizerManager.init(this)
        LyricsRepository.init(this)
        StreamCacheStore.init(this)
        com.melo.music.offline.OfflineManager.init(this)
        com.melo.music.settings.AppSettings.init(this)
        Recommender.init(this)
        // Карта музыки: бэкенд Appwrite + osmdroid.
        com.melo.music.map.AppwriteService.init(this)
        com.melo.music.map.MapModeration.init(this)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
        Thread {
            kotlinx.coroutines.runBlocking {
                runCatching { com.melo.music.map.AppwriteService.ensureSession() }
                runCatching { com.melo.music.auth.AuthManager.refresh() }
            }
        }.start()
        ByeDpiProxy.init(this)
        if (ByeDpiProxy.isEnabled() && ByeDpiProxy.getCommandLine().isNotBlank()) {
            Thread { ByeDpiProxy.start() }.start()
        }
        Thread {
            runCatching { Extractor.ensureInit(this) }
            runCatching { NewPipeResolver.ensureInit(this) }
            // Добываем SoundCloud client_id с рабочих хостов (минуя soundcloud.com).
            runCatching { SoundCloudFix.ensure(this) }
            // Прогреваем SC-медиахосты: ByeDPI заранее подберёт стратегию, иначе
            // первый трек ~15с ловит source error пока подбор идёт на лету.
            runCatching { SoundCloudFix.warmUp() }
        }.start()
    }

    /**
     * Загрузчик обложек. Все обложки с одного хоста (googleusercontent), поэтому
     * поднимаем лимит запросов на хост и НЕ ставим callTimeout (он считает время
     * в очереди → массовые таймауты). Память + диск кэш, игнор cache-headers,
     * чтобы при скролле картинки не грузились заново.
     */
    override fun newImageLoader(): ImageLoader {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 8
        }
        val clientBuilder = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            // ByeDPI + системный DNS (DoH давал недостижимые IP для sndcdn).
            // HTTP/1.1: десинк ByeDPI ломает HTTP/2 (обложки виснут).
            .proxySelector(com.melo.music.net.MeloNet.byedpiSelector)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val host = chain.request().url.host
                val sc = host.contains("sndcdn") || host.contains("soundcloud")
                // SoundCloud-обложки t500x500 (~73КБ) захлёбываются на ByeDPI; t200x200
                // (~10КБ) пролезает надёжно и для мелких превью списка более чем хватает.
                val origUrl = chain.request().url.toString()
                val url = if (sc) {
                    origUrl.replace(Regex("-(?:large|t\\d+x\\d+|original)\\.(jpg|jpeg|png)"), "-t200x200.$1")
                } else {
                    origUrl
                }
                val request = chain.request().newBuilder().url(url).header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101 Firefox/91.0",
                ).build()
                if (!sc) return@addInterceptor chain.proceed(request)
                // SoundCloud-обложки: ограничиваем параллельность + повторяем при reset
                // (Socket closed падает за ~100мс на свежем коннекте, повтор дёшев).
                // Coil сам не ретраит — без этого упавшая обложка остаётся серой.
                scImgSemaphore.acquire()
                try {
                    var last: Exception? = null
                    repeat(4) {
                        try {
                            val resp = chain
                                .withConnectTimeout(5, TimeUnit.SECONDS)
                                .withReadTimeout(4, TimeUnit.SECONDS)
                                .proceed(request)
                            if (resp.code in 400..499) return@addInterceptor resp
                            if (!resp.isSuccessful) { resp.close(); last = java.io.IOException("HTTP ${resp.code}"); return@repeat }
                            // Читаем ВСЁ тело здесь — под таймаут+ретрай (иначе Coil
                            // дочитывает тело при декоде и ловит таймаут через ByeDPI).
                            val ct = resp.body?.contentType()
                            val bytes = resp.body!!.bytes()
                            return@addInterceptor resp.newBuilder()
                                .body(bytes.toResponseBody(ct))
                                .build()
                        } catch (e: Exception) {
                            last = e
                        }
                    }
                    // android.util.Log.e("MeloImg", "FAILx4 ${last?.javaClass?.simpleName} ${request.url}")
                    throw last ?: java.io.IOException("img fail")
                } finally {
                    scImgSemaphore.release()
                }
            }
        val client = clientBuilder.build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
