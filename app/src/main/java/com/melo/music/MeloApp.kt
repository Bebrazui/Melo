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

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        com.melo.music.util.FileLog.init(this)
        com.melo.music.util.FileLog.i("MeloApp", "Application started")
        StreamCacheStore.init(this)
        FavoritesManager.init(this)
        PlaylistManager.init(this)
        com.melo.music.auth.LoginGuard.init(this)
        com.melo.music.auth.AuthManager.init(this)
        com.melo.music.auth.YouTubeAccountManager.init(this)
        com.melo.music.sync.InnerTubeConfig.init(this)
        HistoryManager.init(this)
        EqualizerManager.init(this)
        LyricsRepository.init(this)
        StreamCacheStore.init(this)
        com.melo.music.offline.OfflineManager.init(this)
        com.melo.music.settings.AppSettings.init(this)
        com.melo.music.ui.sound.ClickFeedback.init(this)
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
            maxRequests = 10
            maxRequestsPerHost = 4
        }
        val clientBuilder = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .retryOnConnectionFailure(true)
            .dns(com.melo.music.net.MeloNet.dns)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            // ByeDPI + честный DNS.
            // HTTP/1.1: десинк ByeDPI ломает HTTP/2 (обложки виснут).
            .proxySelector(com.melo.music.net.MeloNet.byedpiSelector)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val origReq = chain.request()
                val host = origReq.url.host
                val sc = host.contains("sndcdn") || host.contains("soundcloud")
                val origUrl = origReq.url.toString()

                val isAvatar = origUrl.contains("/avatars-")
                val is500 = origUrl.contains("-t500x500.")
                val is300 = origUrl.contains("-t300x300.")
                val primaryUrl = if (sc) {
                    if (isAvatar) {
                        if (!is300) {
                            origUrl.replace(Regex("-(?:large|badge|small|mini|t500x500)\\.(jpg|jpeg|png)"), "-t300x300.$1")
                        } else {
                            origUrl
                        }
                    } else {
                        if (!is500) {
                            origUrl.replace(Regex("-(?:large|badge|small|mini|t300x300)\\.(jpg|jpeg|png)"), "-t500x500.$1")
                        } else {
                            origUrl
                        }
                    }
                } else {
                    origUrl
                }
                val fallbackUrl = if (sc) {
                    primaryUrl.replace("-t500x500.", "-large.").replace("-t300x300.", "-large.")
                } else null

                com.melo.music.util.FileLog.d("MeloImg", "--> GET $primaryUrl (sc=$sc, avatar=$isAvatar)")

                val primaryRequest = origReq.newBuilder()
                    .url(primaryUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                    )
                    .build()

                var resp: okhttp3.Response? = null
                try {
                    resp = chain.proceed(primaryRequest)
                    com.melo.music.util.FileLog.d("MeloImg", "<-- HTTP ${resp.code} for $primaryUrl")
                    if (sc && resp.code == 404 && fallbackUrl != null && fallbackUrl != primaryUrl) {
                        com.melo.music.util.FileLog.w("MeloImg", "404 on high-res -> fallback to: $fallbackUrl")
                        resp.close()
                        val fallbackReq = origReq.newBuilder()
                            .url(fallbackUrl)
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                            )
                            .build()
                        resp = chain.withConnectTimeout(chain.connectTimeoutMillis(), TimeUnit.MILLISECONDS)
                            .proceed(fallbackReq)
                        com.melo.music.util.FileLog.d("MeloImg", "<-- HTTP ${resp.code} for fallback $fallbackUrl")
                    }
                } catch (e: Exception) {
                    com.melo.music.util.FileLog.e("MeloImg", "Primary FAIL $primaryUrl: ${e.javaClass.simpleName} ${e.message}")
                    if (sc && fallbackUrl != null && fallbackUrl != primaryUrl) {
                        com.melo.music.util.FileLog.w("MeloImg", "Exception on high-res -> trying fallback $fallbackUrl")
                        resp = runCatching {
                            val fallbackReq = origReq.newBuilder()
                                .url(fallbackUrl)
                                .header(
                                    "User-Agent",
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
                                )
                                .build()
                            chain.withConnectTimeout(chain.connectTimeoutMillis(), TimeUnit.MILLISECONDS)
                                .proceed(fallbackReq)
                        }.getOrNull()
                        if (resp != null) {
                            com.melo.music.util.FileLog.d("MeloImg", "<-- HTTP ${resp.code} for fallback $fallbackUrl")
                        }
                    }
                    if (resp == null) throw e
                }
                resp
            }
        val client = clientBuilder.build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .components {
                add(com.melo.music.ui.SquareCropInterceptor())
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
