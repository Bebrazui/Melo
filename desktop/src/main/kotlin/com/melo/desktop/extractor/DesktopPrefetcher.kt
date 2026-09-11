package com.melo.desktop.extractor

import com.melo.desktop.storage.DesktopStreamCacheStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Фоновый префетчер аудио-потоков для Melo Desktop.
 * Аналогично мобильной версии, когда на экране появляются карточки треков
 * (рекомендации, поиск, очередь плейлиста), префетчер в фоне заранее получает
 * прямые ссылки на аудио и кэширует их в DesktopStreamCacheStore.
 * Когда пользователь нажимает плей — трек готов мгновенно.
 */
object DesktopPrefetcher {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val queue = Channel<String>(Channel.UNLIMITED)
    private val queuedUrls = ConcurrentHashMap.newKeySet<String>()

    init {
        scope.launch {
            for (url in queue) {
                if (!isActive) break
                try {
                    // Если уже есть в дисковом кэше, пропускаем
                    if (DesktopStreamCacheStore.get(url) != null) {
                        queuedUrls.remove(url)
                        continue
                    }

                    // Резолвим аудиопоток в фоне
                    DesktopExtractor.resolveAudioUrl(url)

                    // Небольшая пауза между запросами, чтобы не забивать канал
                    delay(300)
                } catch (_: Exception) {
                    // Фоновая ошибка не должна прерывать очередь
                } finally {
                    queuedUrls.remove(url)
                }
            }
        }
    }

    /** Добавить URL трека в очередь фонового резолва. */
    fun prefetch(url: String?) {
        if (url.isNullOrBlank()) return
        if (DesktopStreamCacheStore.get(url) != null) return
        if (queuedUrls.add(url)) {
            queue.trySend(url)
        }
    }

    /** Добавить несколько URL в фоновый резолв (например, топ-3 из выдачи). */
    fun prefetchAll(urls: List<String>) {
        for (url in urls) {
            prefetch(url)
        }
    }
}
