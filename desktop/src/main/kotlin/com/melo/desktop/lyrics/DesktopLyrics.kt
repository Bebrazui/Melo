package com.melo.desktop.lyrics

import com.melo.desktop.net.DesktopMeloNet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

data class LrcLine(val timeMs: Long, val text: String)

data class Lyrics(val lines: List<LrcLine>?, val plain: String?) {
    val isSynced: Boolean get() = !lines.isNullOrEmpty()
    val isEmpty: Boolean get() = lines.isNullOrEmpty() && plain.isNullOrBlank()
}

/**
 * Получение синхронизированных текстов песен (LRC) через lrclib.net.
 */
object DesktopLyrics {

    private val cache = ConcurrentHashMap<String, Lyrics>()
    private val TS_RE = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    suspend fun fetch(title: String, artist: String?): Lyrics? = withContext(Dispatchers.IO) {
        val key = "${artist.orEmpty().lowercase()}|${title.lowercase()}".trim()
        cache[key]?.let { return@withContext it }

        val cleanTitle = title.substringBefore(" (").substringBefore(" [").trim().ifBlank { title }
        val cleanArtist = artist?.removeSuffix(" - Topic")?.substringBefore(" - Topic")?.trim()

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
            .header("User-Agent", "MeloDesktop/1.0 (music player)")
            .build()

        val result = runCatching {
            DesktopMeloNet.okHttpClient.newCall(request).execute().use { resp ->
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
                    if (synced != null) break
                }
                if (synced == null && plain == null) null else Lyrics(synced, plain)
            }
        }.getOrNull()

        if (result != null) {
            cache[key] = result
        }
        result
    }

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
