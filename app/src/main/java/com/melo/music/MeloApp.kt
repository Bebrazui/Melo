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
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        FavoritesManager.init(this)
        PlaylistManager.init(this)
        com.melo.music.auth.LoginGuard.init(this)
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
        // Авто-тюнер SC-стратегий отключён: победитель ([2]) зашит в ByeDpiProxy.DEFAULT_CMD.
        // SoundCloudFix.tuneThroughput(...) остаётся для ручной перепроверки при необходимости.
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
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) " +
                            "Gecko/20100101 Firefox/91.0",
                    )
                    .build()
                val host = request.url.host
                try {
                    val resp = chain.proceed(request)
                    if (host.contains("sndcdn") || host.contains("bcbits") || host.contains("soundcloud")) {
                        android.util.Log.e("MeloImg", "$host -> ${resp.code}")
                    }
                    resp
                } catch (e: Exception) {
                    android.util.Log.e("MeloImg", "$host FAILED: ${e.javaClass.simpleName}: ${e.message}")
                    throw e
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
