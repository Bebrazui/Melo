package com.melo.desktop.audio.cache

import com.melo.desktop.extractor.DesktopExtractor
import com.melo.desktop.extractor.Source
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.net.DesktopMeloNet
import com.melo.desktop.recommend.DesktopRecommender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Фоновый предзагрузчик следующего трека в очереди и персональной волны.
 * Гарантирует, что при завершении трека или нажатии кнопки "Дальше"
 * следующая композиция уже закэширована на диске и стартует мгновенно (0 мс).
 */
object AudioPreloader {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var preloadJob: Job? = null

    /**
     * Запуск предзагрузки для следующего трека.
     * Если [nextTrack] не передан (очередь закончилась), запрашивает трек из бесконечной волны.
     */
    fun preloadNext(currentTrack: TrackItem?, nextTrack: TrackItem?) {
        preloadJob?.cancel()

        preloadJob = scope.launch {
            // Даем текущему треку 10 секунд на то, чтобы спокойно начать воспроизведение и забить первичный буфер
            kotlinx.coroutines.delay(10000)
            if (!isActive) return@launch

            val target = nextTrack ?: run {
                if (currentTrack != null) {
                    DesktopRecommender.getNextWaveTracks(currentTrack).firstOrNull()
                } else null
            } ?: return@launch

            // Если трек уже на диске, ничего качать не нужно
            if (AudioCacheManager.isCached(target.url)) {
                println("[AudioPreloader] Next track is already cached: '${target.title}'")
                return@launch
            }

            println("[AudioPreloader] Pre-buffering next track: '${target.title}' (${target.url})")

            runCatching {
                // 1. Извлекаем прямую ссылку на поток заранее (сохраняется в памяти streamCache)
                val resolved = DesktopExtractor.resolveAudioUrl(target.url)
                if (!isActive) return@launch

                val ext = if (resolved.audioUrl.contains(".mp3") || resolved.source == Source.SOUNDCLOUD) ".mp3" else ".m4a"
                val partFile = AudioCacheManager.getPartFile(target.url, ext)

                val req = Request.Builder()
                    .url(resolved.audioUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()

                DesktopMeloNet.okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@launch
                    val body = resp.body ?: return@launch

                    body.byteStream().use { input: InputStream ->
                        FileOutputStream(partFile).use { output: FileOutputStream ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                if (!isActive) {
                                    partFile.delete()
                                    return@launch
                                }
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }

                    if (isActive) {
                        AudioCacheManager.commitPartFile(target.url, ext)
                        println("[AudioPreloader] Successfully preloaded: '${target.title}' -> instant playback ready!")
                    }
                }
            }.onFailure { err ->
                if (err !is kotlinx.coroutines.CancellationException) {
                    System.err.println("[AudioPreloader] Failed to preload next track: ${err.message}")
                }
            }
        }
    }

    fun cancel() {
        preloadJob?.cancel()
        preloadJob = null
    }
}
