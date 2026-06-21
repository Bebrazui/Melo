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
        HistoryManager.init(this)
        EqualizerManager.init(this)
        LyricsRepository.init(this)
        Recommender.init(this)
        ByeDpiProxy.init(this)
        if (ByeDpiProxy.isEnabled() && ByeDpiProxy.getCommandLine().isNotBlank()) {
            Thread { ByeDpiProxy.start() }.start()
        }
        Thread {
            runCatching { Extractor.ensureInit(this) }
            runCatching { NewPipeResolver.ensureInit(this) }
            // Добываем SoundCloud client_id с рабочих хостов (минуя soundcloud.com).
            runCatching { SoundCloudFix.ensure(this) }
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
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) " +
                            "Gecko/20100101 Firefox/91.0",
                    )
                    .build()
                chain.proceed(request)
            }
        if (ByeDpiProxy.isEnabled()) {
            clientBuilder.proxySelector(object : ProxySelector() {
                override fun select(uri: URI): List<Proxy> {
                    return if (ByeDpiProxy.isRunning()) {
                        listOf(ByeDpiProxy.getProxy())
                    } else {
                        listOf(Proxy.NO_PROXY)
                    }
                }
                override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) {}
            })
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
