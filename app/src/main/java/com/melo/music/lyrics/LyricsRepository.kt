package com.melo.music.lyrics

import android.content.Context
import android.content.SharedPreferences
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Одна строка LRC: момент времени (мс) + текст. */
data class LrcLine(val timeMs: Long, val text: String)

/** Результат: синхронизированные строки (если есть) и/или обычный текст. */
data class Lyrics(val lines: List<LrcLine>?, val plain: String?) {
    val isSynced: Boolean get() = !lines.isNullOrEmpty()
    val isEmpty: Boolean get() = lines.isNullOrEmpty() && plain.isNullOrBlank()
}

/**
 * Тексты песен через бесплатный API lrclib.net.
 * Кэширует результаты в памяти (LruCache) и на диске (SharedPreferences).
 */
object LyricsRepository {

    private const val PREFS = "melo_lyrics"
    private const val KEY = "cache"
    private const val MAX_CACHE = 100

    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val TS_RE = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val memCache = LruCache<String, Lyrics>(MAX_CACHE)
    private var prefs: SharedPreferences? = null

    // LruCache не принимает null — для «текст не найден» кладём этот пустой объект.
    private val EMPTY = Lyrics(null, null)

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Загружаем дисковый кэш в память.
        loadDiskCache()
    }

    suspend fun fetch(title: String, artist: String?): Lyrics? = withContext(Dispatchers.IO) {
        val key = cacheKey(title, artist)

        // 1) Память.
        memCache.get(key)?.let { return@withContext it }

        // 2) Диск.
        loadFromDisk(key)?.let {
            memCache.put(key, it)
            return@withContext it
        }

        // 3) Сеть.
        val cleanTitle = title.substringBefore(" (").substringBefore(" [").trim().ifBlank { title }
        val cleanArtist = artist
            ?.removeSuffix(" - Topic")
            ?.substringBefore(" - Topic")
            ?.trim()

        val url = HttpUrl.Builder()
            .scheme("https")
            .host("lrclib.net")
            .addPathSegment("api")
            .addPathSegment("search")
            .apply {
                if (!cleanArtist.isNullOrBlank()) {
                    addQueryParameter("track_name", cleanTitle)
                    addQueryParameter("artist_name", cleanArtist)
                } else {
                    addQueryParameter("q", cleanTitle)
                }
            }
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Melo/0.1 (music app)")
            .build()

        val result = runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val arr = JSONArray(body)
                var synced: List<LrcLine>? = null
                var plain: String? = null
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (synced == null) {
                        val raw = obj.optString("syncedLyrics", "")
                        if (raw.isNotBlank()) parseLrc(raw).takeIf { it.isNotEmpty() }?.let { synced = it }
                    }
                    if (plain == null) {
                        obj.optString("plainLyrics", "").takeIf { it.isNotBlank() }?.let { plain = it }
                    }
                    if (synced != null && plain != null) break
                }
                if (synced == null && plain == null) null else Lyrics(synced, plain)
            }
        }.getOrNull()

        // Сохраняем в кэш (включая «не найдено» — чтобы не запрашивать повторно).
        memCache.put(key, result ?: EMPTY)
        saveToDisk(key, result)
        result
    }

    private fun cacheKey(title: String, artist: String?): String =
        "${artist.orEmpty().lowercase()}|${title.lowercase()}".trim()

    // ── Дисковый кэш ────────────────────────────────────────────────────────

    private fun loadDiskCache() {
        val json = prefs?.getString(KEY, null) ?: return
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val k = obj.getString("k")
                val synced = if (obj.has("s")) parseLrc(obj.getString("s")) else null
                val plain = obj.optString("p", null)
                val lyrics = if (synced == null && plain.isNullOrBlank()) null else Lyrics(synced, plain)
                memCache.put(k, lyrics ?: EMPTY)
            }
        }
    }

    private fun loadFromDisk(key: String): Lyrics? {
        val json = prefs?.getString(KEY, null) ?: return null
        return runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("k") == key) {
                    val synced = if (obj.has("s")) parseLrc(obj.getString("s")) else null
                    val plain = obj.optString("p", null)
                    return@runCatching if (synced == null && plain.isNullOrBlank()) null else Lyrics(synced, plain)
                }
            }
            null
        }.getOrNull()
    }

    private fun saveToDisk(key: String, lyrics: Lyrics?) {
        val json = prefs?.getString(KEY, null) ?: "[]"
        val arr = runCatching { JSONArray(json) }.getOrDefault(JSONArray())

        // Удаляем старую запись с тем же ключом.
        for (i in arr.length() - 1 downTo 0) {
            if (arr.getJSONObject(i).optString("k") == key) {
                arr.remove(i)
            }
        }

        // Добавляем новую в конец.
        if (lyrics != null) {
            arr.put(JSONObject().apply {
                put("k", key)
                lyrics.lines?.let { put("s", linesToLrc(it)) }
                lyrics.plain?.let { put("p", it) }
            })
        } else {
            arr.put(JSONObject().apply { put("k", key) })
        }

        // Ограничиваем размер кэша.
        while (arr.length() > MAX_CACHE) arr.remove(0)

        prefs?.edit()?.putString(KEY, arr.toString())?.apply()
    }

    private fun linesToLrc(lines: List<LrcLine>): String = buildString {
        for (line in lines) {
            val min = line.timeMs / 60_000
            val sec = (line.timeMs % 60_000) / 1_000
            val ms = line.timeMs % 1_000
            append("[%02d:%02d.%03d]%s".format(min, sec, ms, line.text))
            append("\n")
        }
    }

    /** Парсит LRC-текст в отсортированный список строк. */
    private fun parseLrc(raw: String): List<LrcLine> {
        val out = ArrayList<LrcLine>()
        for (line in raw.lineSequence()) {
            val stamps = TS_RE.findAll(line).toList()
            if (stamps.isEmpty()) continue
            val text = line.substring(stamps.last().range.last + 1).trim()
            for (m in stamps) {
                val mm = m.groupValues[1].toLong()
                val ss = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    3 -> frac.toLong()
                    else -> 0
                }
                out.add(LrcLine(mm * 60_000 + ss * 1_000 + ms, text))
            }
        }
        return out.sortedBy { it.timeMs }
    }
}
