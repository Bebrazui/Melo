package com.melo.music.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Поиск треков через Bandcamp API (fuzzysearch/autocomplete).
 * Возвращает результаты по мере загрузки (один batch за запрос).
 */
object BandcampSearcher {

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Ищет треки на Bandcamp. Возвращает список TrackItem.
     * URL-ы в ответе API бывают «удвоенными» — исправляем.
     */
    suspend fun search(query: String): List<TrackItem> = withContext(Dispatchers.IO) {
        val url = "https://bandcamp.com/api/fuzzysearch/2/app_autocomplete?q=${
            java.net.URLEncoder.encode(query, "UTF-8")
        }"
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101 Firefox/91.0",
            )
            .build()

        runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val body = resp.body?.string() ?: return@use emptyList()
                val json = JSONObject(body)
                val results = json.getJSONArray("results")
                val items = mutableListOf<TrackItem>()
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val type = obj.optString("type", "")
                    if (type != "t" && type != "a") continue

                    val name = obj.optString("name", "")
                    if (name.isBlank()) continue

                    val bandName = obj.optString("band_name", "")
                    val rawUrl = obj.optString("url", "")
                    val trackUrl = fixUrl(rawUrl)
                    if (trackUrl.isBlank()) continue

                    // API отдаёт art_id — добавляем префикс `a` для CDN.
                    val rawThumb = obj.optString("img", "").ifBlank { null }
                    val thumb = rawThumb?.let { fixThumb(it) }

                    items.add(
                        TrackItem(
                            title = name,
                            uploader = bandName.ifBlank { null },
                            url = trackUrl,
                            durationSeconds = 0,
                            thumbnailUrl = thumb,
                            source = Source.BANDCAMP,
                            kind = ItemKind.TRACK,
                        ),
                    )
                }
                items
            }
        }.getOrDefault(emptyList())
    }

    /** Исправляет удвоенные URL от Bandcamp API. */
    private fun fixUrl(raw: String): String {
        if (raw.isBlank()) return ""
        val secondProto = raw.indexOf("https://", 8)
        return if (secondProto > 0) raw.substring(secondProto) else raw
    }

    /** Добавляет префикс `a` к art_id: /img/3207173692_3.jpg → /img/a3207173692_10.jpg */
    private fun fixThumb(url: String): String {
        val bcbits = "f4.bcbits.com/img/"
        val idx = url.indexOf(bcbits)
        if (idx < 0) return url
        val after = url.substring(idx + bcbits.length)
        val withA = if (after.startsWith("a")) after else "a$after"
        // Заменяем размер _N на _10.
        val us = withA.lastIndexOf('_')
        val dot = withA.lastIndexOf(".jpg")
        return if (us > 0 && dot > us) {
            url.substring(0, idx + bcbits.length) + withA.substring(0, us + 1) + "10.jpg"
        } else {
            url.substring(0, idx + bcbits.length) + withA
        }
    }
}
