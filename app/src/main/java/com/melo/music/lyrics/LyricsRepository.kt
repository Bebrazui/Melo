package com.melo.music.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Тексты песен через бесплатный API lrclib.net.
 * Возвращает обычный (не синхронизированный) текст или null.
 */
object LyricsRepository {

    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(title: String, artist: String?): String? = withContext(Dispatchers.IO) {
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

        runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val plain = arr.getJSONObject(i).optString("plainLyrics", "")
                    if (plain.isNotBlank()) return@use plain
                }
                null
            }
        }.getOrNull()
    }
}
