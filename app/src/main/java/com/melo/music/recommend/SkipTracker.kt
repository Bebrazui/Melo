package com.melo.music.recommend

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Трекинг пропусков: если трек пропущен менее чем через N секунд
 * после начала — это негативный сигнал.
 * Данные хранятся в SharedPreferences.
 */
object SkipTracker {

    private const val PREFS = "melo_skips"
    private const val KEY_SKIPS = "skips"
    private const val KEY_LISTENS = "listens"

    /** Порог пропуска: если прослушано меньше — считаем skip. */
    const val SKIP_THRESHOLD_MS = 15_000L

    /** Порог полного прослушивания: если больше — считаем full listen. */
    const val FULL_LISTEN_THRESHOLD_MS = 30_000L

    private var prefs: SharedPreferences? = null

    /** url → количество пропусков. */
    private val skipCounts = mutableMapOf<String, Int>()

    /** url → количество полных прослушиваний. */
    private val listenCounts = mutableMapOf<String, Int>()

    /** url → суммарная длительность прослушивания (мс). */
    private val listenDurations = mutableMapOf<String, Long>()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        load()
    }

    /**
     * Записать результат прослушивания трека.
     * @param url URL трека
     * @param listenedMs сколько миллисекунд было прослушано
     * @param totalMs общая длительность трека (0 = неизвестно)
     */
    fun recordListen(url: String, listenedMs: Long, totalMs: Long = 0) {
        if (listenedMs < SKIP_THRESHOLD_MS) {
            // Skip.
            skipCounts[url] = (skipCounts[url] ?: 0) + 1
        } else if (listenedMs >= FULL_LISTEN_THRESHOLD_MS || (totalMs > 0 && listenedMs >= totalMs * 0.7)) {
            // Полное прослушивание.
            listenCounts[url] = (listenCounts[url] ?: 0) + 1
        }
        listenDurations[url] = (listenDurations[url] ?: 0) + listenedMs
        save()
    }

    /**
     * Skip-rate для трека: доля пропусков от общего числа прослушиваний.
     * 0.0 = не пропускали, 1.0 = всегда пропускают.
     */
    fun skipRate(url: String): Double {
        val skips = skipCounts[url] ?: 0
        val listens = listenCounts[url] ?: 0
        val total = skips + listens
        return if (total == 0) 0.0 else skips.toDouble() / total
    }

    /**
     * Средняя длительность прослушивания трека (мс).
     */
    fun avgListenDuration(url: String): Long {
        val total = listenDurations[url] ?: 0
        val count = (skipCounts[url] ?: 0) + (listenCounts[url] ?: 0)
        return if (count == 0) 0 else total / count
    }

    /**
     * Получить набор URL треков, которые стоит исключить (высокий skip-rate).
     */
    fun getBannedUrls(): Set<String> {
        return skipCounts.entries
            .filter { (url, skips) ->
                val listens = listenCounts[url] ?: 0
                val total = skips + listens
                total >= 2 && skips.toDouble() / total > 0.6
            }
            .map { it.key }
            .toSet()
    }

    /**
     * Получить набор артистов с высоким skip-rate.
     * Определяется по трекам из skipCounts.
     */
    fun getBannedArtists(): Set<String> {
        // Нужен доступ к TrackItem чтобы достать uploader —
        // поэтому возвращаем пустой набор, а фильтрацию делаем
        // через skipRate() в Recommender.
        return emptySet()
    }

    fun getSkipCount(url: String): Int = skipCounts[url] ?: 0

    fun getListenCount(url: String): Int = listenCounts[url] ?: 0

    // ── Persistence ──────────────────────────────────────────────────────

    private fun save() {
        val obj = JSONObject()
        for ((url, count) in skipCounts) {
            if (count > 0) obj.put("s_$url", count)
        }
        for ((url, count) in listenCounts) {
            if (count > 0) obj.put("l_$url", count)
        }
        for ((url, dur) in listenDurations) {
            if (dur > 0) obj.put("d_$url", dur)
        }
        prefs?.edit()?.putString(KEY_SKIPS, obj.toString())?.apply()
    }

    private fun load() {
        val json = prefs?.getString(KEY_SKIPS, null) ?: return
        runCatching {
            val obj = JSONObject(json)
            for (key in obj.keys()) {
                when {
                    key.startsWith("s_") -> skipCounts[key.removePrefix("s_")] = obj.getInt(key)
                    key.startsWith("l_") -> listenCounts[key.removePrefix("l_")] = obj.getInt(key)
                    key.startsWith("d_") -> listenDurations[key.removePrefix("d_")] = obj.getLong(key)
                }
            }
        }
    }
}
