package com.melo.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.NewPipeResolver
import com.melo.music.extractor.SoundCloudFix
import okhttp3.OkHttpClient
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
        Thread {
            runCatching { Extractor.ensureInit(this) }
            runCatching { NewPipeResolver.ensureInit(this) }
            // Добываем SoundCloud client_id с рабочих хостов (минуя soundcloud.com).
            runCatching { SoundCloudFix.ensure(this) }
        }.start()
    }

    /** Загрузчик обложек: браузерный UA + таймауты (чтобы запрос не висел вечно). */
    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .build()
    }
}
