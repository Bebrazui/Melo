package com.melo.music.extractor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.melo.music.byedpi.ByeDpiProxy
import com.melo.music.offline.OfflineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Импорт плейлистов/лайков из YouTube Music и SoundCloud.
 *
 * Стратегия:
 *  - YouTube/SoundCloud плейлист по ссылке → NewPipe (треки сразу играбельны).
 *  - SoundCloud лайки по ссылке профиля → api-v2 (надёжный хост на сети с DPI-обходом).
 *  - Фолбэк для SoundCloud: скрейп публичной страницы (__sc_hydration JSON);
 *    треки с permalink играем напрямую, треки только с названием — матчим через поиск.
 */
object PlaylistImporter {

    enum class Service { YOUTUBE, SOUNDCLOUD, LOCAL }
    enum class Mode { PLAYLIST, FAVORITES }

    data class Progress(val done: Int, val total: Int, val phase: String)
    data class Result(val name: String, val tracks: List<TrackItem>)

    /**
     * Локальный импорт: пользователь выбрал папку (SAF tree URI).
     * Берём все аудиофайлы, регистрируем как офлайн (играются прямо с диска).
     */
    suspend fun importLocalFolder(context: Context, treeUri: Uri): Result =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            // Сохраняем доступ к папке между перезапусками приложения.
            runCatching {
                resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val folderName = treeDocId.substringAfterLast(':').substringAfterLast('/')
                .ifBlank { "Папка" }
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)

            // Первый проход: собираем аудио и .lrc-файлы (по базовому имени).
            data class Audio(val docId: String, val name: String)
            val audios = mutableListOf<Audio>()
            val lrcByBase = HashMap<String, String>() // baseNameLower -> lrc docUri
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null, null, null,
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (c.moveToNext()) {
                    val docId = c.getString(idIdx) ?: continue
                    val name = c.getString(nameIdx) ?: continue
                    val mime = c.getString(mimeIdx) ?: ""
                    when {
                        name.endsWith(".lrc", ignoreCase = true) ->
                            lrcByBase[name.substringBeforeLast('.').lowercase().trim()] =
                                DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                        mime.startsWith("audio") -> audios.add(Audio(docId, name))
                    }
                }
            }
            if (audios.isEmpty()) error("В папке нет аудиофайлов")

            val coverDir = java.io.File(context.cacheDir, "local_covers").apply { mkdirs() }
            val tracks = audios.map { a ->
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, a.docId).toString()
                val base = a.name.substringBeforeLast('.')
                val (pArtist, pTitle) = parseArtistTitle(a.name)

                var title = pTitle
                var artist = pArtist
                var durationSec = 0L
                var thumb: String? = null

                // Метаданные + встроенная обложка из самого файла.
                val mmr = android.media.MediaMetadataRetriever()
                runCatching {
                    mmr.setDataSource(context, Uri.parse(docUri))
                    mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() }?.let { title = it }
                    mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() }?.let { artist = it }
                    mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()?.let { durationSec = it / 1000 }
                    mmr.embeddedPicture?.let { pic ->
                        val f = java.io.File(coverDir, a.docId.hashCode().toString() + ".jpg")
                        runCatching { f.writeBytes(pic); thumb = Uri.fromFile(f).toString() }
                    }
                }
                runCatching { mmr.release() }

                // Сопутствующий .lrc → в кэш текстов под ключ трека.
                lrcByBase[base.lowercase().trim()]?.let { lrcUri ->
                    runCatching {
                        resolver.openInputStream(Uri.parse(lrcUri))?.bufferedReader()
                            ?.use { it.readText() }
                    }.getOrNull()?.takeIf { it.isNotBlank() }?.let { raw ->
                        com.melo.music.lyrics.LyricsRepository.putLocal(context, title, artist, raw)
                    }
                }

                OfflineManager.add(docUri, docUri, if (artist != null) "$artist — $title" else title)
                TrackItem(
                    title = title,
                    uploader = artist,
                    url = docUri,
                    durationSeconds = durationSec,
                    thumbnailUrl = thumb,
                    source = Source.LOCAL,
                    kind = ItemKind.TRACK,
                )
            }
            Result(folderName, tracks.sortedBy { it.title })
        }

    /** «Artist - Title.mp3» → (Artist, Title); иначе (null, имя без расширения). */
    private fun parseArtistTitle(filename: String): Pair<String?, String> {
        val base = filename.substringBeforeLast('.')
        val parts = base.split(" - ", limit = 2)
        return if (parts.size == 2) parts[0].trim() to parts[1].trim()
        else null to base.trim()
    }

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(25, TimeUnit.SECONDS)
        .proxySelector(object : ProxySelector() {
            override fun select(uri: URI): List<Proxy> =
                if (ByeDpiProxy.isEnabled() && ByeDpiProxy.isRunning()) {
                    listOf(ByeDpiProxy.getProxy())
                } else {
                    listOf(Proxy.NO_PROXY)
                }
            override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) {}
        })
        .build()

    suspend fun import(
        context: Context,
        service: Service,
        mode: Mode,
        input: String,
        onProgress: (Progress) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        val url = input.trim()
        require(url.startsWith("http", ignoreCase = true)) { "Вставь ссылку (http…)" }

        when (service) {
            Service.YOUTUBE -> {
                onProgress(Progress(0, 0, "Читаю плейлист YouTube…"))
                val (name, tracks) = NewPipeResolver.importPlaylist(context, url)
                if (tracks.isEmpty()) error("Не нашёл треков. Для лайков вставь ссылку на ПУБЛИЧНЫЙ плейлист (лайки в YouTube приватны).")
                Result(name, tracks.distinctBy { it.url })
            }

            Service.SOUNDCLOUD -> when (mode) {
                Mode.PLAYLIST -> {
                    onProgress(Progress(0, 0, "Читаю сет SoundCloud…"))
                    val viaNewPipe = runCatching { NewPipeResolver.importPlaylist(context, url) }
                        .getOrNull()
                    if (viaNewPipe != null && viaNewPipe.second.isNotEmpty()) {
                        Result(viaNewPipe.first, viaNewPipe.second.distinctBy { it.url })
                    } else {
                        onProgress(Progress(0, 0, "Фолбэк: публичная страница…"))
                        scrapeFallback(context, url, "Импорт SoundCloud", onProgress)
                    }
                }
                Mode.FAVORITES -> {
                    onProgress(Progress(0, 0, "Читаю лайки SoundCloud…"))
                    val viaApi = runCatching { importScLikes(context, url) }.getOrNull()
                    if (viaApi != null && viaApi.tracks.isNotEmpty()) {
                        viaApi
                    } else {
                        onProgress(Progress(0, 0, "Фолбэк: публичная страница…"))
                        scrapeFallback(context, url.trimEnd('/') + "/likes", "Лайки SoundCloud", onProgress)
                    }
                }
            }

            // Локальный импорт идёт отдельным путём (importLocalFolder).
            Service.LOCAL -> error("Локальный импорт через выбор папки")
        }
    }

    // ── SoundCloud лайки через api-v2 ───────────────────────────────────────────

    private fun importScLikes(context: Context, profileUrl: String): Result {
        val id = SoundCloudFix.ensure(context) ?: error("Нет client_id SoundCloud")
        val c = client()
        // 1) Резолвим профиль → числовой id пользователя.
        val resolveUrl = "https://api-v2.soundcloud.com/resolve?url=" +
            URLEncoder.encode(profileUrl.trim(), "UTF-8") + "&client_id=$id"
        val userJson = httpJson(c, resolveUrl) ?: error("Не удалось открыть профиль")
        val userId = userJson.optLong("id", -1L)
        if (userId <= 0) error("Это не профиль пользователя")
        val username = userJson.optString("username", "SoundCloud")

        // 2) Тянем лайки постранично.
        val tracks = mutableListOf<TrackItem>()
        var next: String? = "https://api-v2.soundcloud.com/users/$userId/likes" +
            "?client_id=$id&limit=200&offset=0&linked_partitioning=1"
        var pages = 0
        while (next != null && pages < 5) {
            val page = httpJson(c, next) ?: break
            val collection = page.optJSONArray("collection") ?: JSONArray()
            for (i in 0 until collection.length()) {
                val track = collection.optJSONObject(i)?.optJSONObject("track") ?: continue
                tracks.add(track.toScTrack())
            }
            next = page.optString("next_href", "").ifBlank { null }
                ?.let { if (it.contains("client_id")) it else "$it&client_id=$id" }
            pages++
        }
        return Result("Лайки $username", tracks.distinctBy { it.url })
    }

    private fun JSONObject.toScTrack(): TrackItem = TrackItem(
        title = optString("title", "Без названия"),
        uploader = optJSONObject("user")?.optString("username")?.takeIf { it.isNotBlank() },
        url = optString("permalink_url", ""),
        durationSeconds = optLong("duration", 0L) / 1000,
        thumbnailUrl = optString("artwork_url", "").ifBlank { null },
        source = Source.SOUNDCLOUD,
        kind = ItemKind.TRACK,
    )

    // ── Фолбэк: скрейп публичной страницы (__sc_hydration) ──────────────────────

    private suspend fun scrapeFallback(
        context: Context,
        pageUrl: String,
        defaultName: String,
        onProgress: (Progress) -> Unit,
    ): Result {
        val html = httpGet(client(), pageUrl) ?: error("Страница недоступна")
        val raw = parseScHydration(html)
        if (raw.isEmpty()) error("Не удалось разобрать страницу")

        // Треки с прямой ссылкой играем сразу; остальные матчим поиском.
        val out = mutableListOf<TrackItem>()
        raw.forEachIndexed { idx, r ->
            onProgress(Progress(idx + 1, raw.size, "Сопоставляю треки…"))
            if (!r.permalink.isNullOrBlank()) {
                out.add(
                    TrackItem(
                        title = r.title,
                        uploader = r.artist,
                        url = r.permalink,
                        durationSeconds = 0,
                        thumbnailUrl = r.artwork,
                        source = Source.SOUNDCLOUD,
                        kind = ItemKind.TRACK,
                    ),
                )
            } else {
                val q = listOfNotNull(r.artist, r.title).joinToString(" ")
                NewPipeResolver.searchOne(context, q)?.let { out.add(it) }
            }
        }
        return Result(defaultName, out.distinctBy { it.url })
    }

    private data class RawTrack(
        val title: String,
        val artist: String?,
        val permalink: String?,
        val artwork: String?,
    )

    private fun parseScHydration(html: String): List<RawTrack> {
        val marker = "__sc_hydration ="
        val start = html.indexOf(marker).takeIf { it >= 0 } ?: return emptyList()
        val jsonStart = html.indexOf('[', start)
        val jsonEnd = html.indexOf("};", jsonStart).takeIf { it >= 0 }
            ?: html.indexOf("</script>", jsonStart)
        if (jsonStart < 0 || jsonEnd < 0) return emptyList()
        val arr = runCatching { JSONArray(html.substring(jsonStart, jsonEnd).trimEnd(';', ' ', '\n')) }
            .getOrNull() ?: return emptyList()

        val out = mutableListOf<RawTrack>()
        fun addTrack(t: JSONObject) {
            val title = t.optString("title", "").ifBlank { return }
            out.add(
                RawTrack(
                    title = title,
                    artist = t.optJSONObject("user")?.optString("username")?.takeIf { it.isNotBlank() },
                    permalink = t.optString("permalink_url", "").ifBlank { null },
                    artwork = t.optString("artwork_url", "").ifBlank { null },
                ),
            )
        }
        for (i in 0 until arr.length()) {
            val data = arr.optJSONObject(i)?.optJSONObject("data") ?: continue
            // Одиночный трек.
            if (data.has("title") && data.has("permalink_url")) addTrack(data)
            // Плейлист/сет.
            data.optJSONArray("tracks")?.let { ts ->
                for (j in 0 until ts.length()) ts.optJSONObject(j)?.let { addTrack(it) }
            }
        }
        return out
    }

    // ── HTTP ────────────────────────────────────────────────────────────────────

    private fun httpJson(c: OkHttpClient, url: String): JSONObject? = runCatching {
        c.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            resp.body?.string()?.let { JSONObject(it) }
        }
    }.getOrNull()

    private fun httpGet(c: OkHttpClient, url: String): String? = runCatching {
        val req = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0 Safari/537.36",
            )
            .build()
        c.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
    }.getOrNull()
}
