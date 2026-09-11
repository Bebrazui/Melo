package com.melo.desktop.audio

import com.melo.desktop.audio.cache.AudioCacheManager
import com.melo.desktop.net.DesktopMeloNet
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/**
 * Локальный HTTP-стриминг прокси на 127.0.0.1:45455.
 * Позволяет системному плееру (JavaFX Media) воспроизводить аудиопотоки
 * YouTube/SoundCloud через ByeDPI / Zapret со сквозным кэшированием на диск.
 */
object LocalStreamServer {

    const val PORT = 45455
    private var server: HttpServer? = null
    private val remoteMetaCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

    @Synchronized
    fun start() {
        if (server != null) return
        try {
            val s = HttpServer.create(InetSocketAddress("127.0.0.1", PORT), 0)
            val handler = AudioStreamHandler()
            s.createContext("/", handler)
            s.executor = Executors.newFixedThreadPool(8)
            s.start()
            server = s
            println("[LocalStreamServer] Running on http://127.0.0.1:$PORT/")
        } catch (e: Exception) {
            System.err.println("[LocalStreamServer] Failed to start on port $PORT: ${e.message}")
        }
    }

    /** Формирует локальный URL с расширением (.m4a/.mp3) для JavaFX Media. */
    fun proxyUrl(remoteUrl: String, originalUrl: String? = null): String {
        start()
        val ext = when {
            remoteUrl.contains(".mp3", ignoreCase = true) || remoteUrl.contains("mime=audio%2Fmpeg", ignoreCase = true) -> ".mp3"
            remoteUrl.contains(".ogg", ignoreCase = true) || remoteUrl.contains(".opus", ignoreCase = true) -> ".ogg"
            else -> ".m4a"
        }
        val encodedRemote = URLEncoder.encode(remoteUrl, StandardCharsets.UTF_8.toString())
        val encodedOriginal = originalUrl?.let { "&orig=" + URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }.orEmpty()
        return "http://127.0.0.1:$PORT/stream$ext?url=$encodedRemote$encodedOriginal"
    }

    private class AudioStreamHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                val rawQuery = exchange.requestURI.rawQuery.orEmpty()
                var targetUrl: String? = null
                var originalUrl: String? = null

                val params = rawQuery.split("&")
                for (p in params) {
                    when {
                        p.startsWith("url=") -> targetUrl = URLDecoder.decode(p.removePrefix("url="), StandardCharsets.UTF_8.toString())
                        p.startsWith("orig=") -> originalUrl = URLDecoder.decode(p.removePrefix("orig="), StandardCharsets.UTF_8.toString())
                    }
                }

                if (targetUrl.isNullOrBlank()) {
                    exchange.sendResponseHeaders(400, -1)
                    return
                }

                val method = exchange.requestMethod.uppercase()
                val isHead = method == "HEAD"
                val clientRange = exchange.requestHeaders.getFirst("Range")
                println("[LocalStreamServer] REQ: $method range=$clientRange path=${exchange.requestURI.path}")

                // Определяем тип контента по расширению пути
                val path = exchange.requestURI.path.lowercase()
                val ext = when {
                    path.endsWith(".mp3") -> ".mp3"
                    path.endsWith(".ogg") || path.endsWith(".opus") -> ".ogg"
                    else -> ".m4a"
                }
                val defaultContentType = when (ext) {
                    ".mp3" -> "audio/mpeg"
                    ".ogg" -> "audio/ogg"
                    else -> "audio/mp4"
                }

                // ── 1. Проверяем локальный дисковый кэш ──────────────────────
                val cachedFile = originalUrl?.let { AudioCacheManager.getCachedFile(it) }
                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 64 * 1024) {
                    serveFromLocalFile(exchange, cachedFile, defaultContentType, isHead, clientRange)
                    return
                }

                // ── 2. HEAD запрос: отдаем метаданные с корректным Content-Length ───────────
                if (isHead) {
                    val headers = exchange.responseHeaders
                    var cachedMeta = remoteMetaCache[targetUrl]
                    if (cachedMeta == null) {
                        runCatching {
                            val probeReq = Request.Builder()
                                .url(targetUrl)
                                .head()
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                .build()
                            DesktopMeloNet.okHttpClient.newCall(probeReq).execute().use { probeResp ->
                                val ct = probeResp.header("Content-Type")?.takeIf { it.isNotBlank() } ?: defaultContentType
                                val cl = probeResp.header("Content-Length")?.toLongOrNull() ?: -1L
                                if (cl > 0) {
                                    val pair = Pair(ct, cl)
                                    cachedMeta = pair
                                    remoteMetaCache[targetUrl] = pair
                                }
                            }
                        }
                    }
                    val cType = cachedMeta?.first ?: defaultContentType
                    val cLength = cachedMeta?.second ?: -1L
                    headers.set("Content-Type", cType)
                    headers.set("Accept-Ranges", "bytes")
                    if (cLength > 0) {
                        headers.set("Content-Length", cLength.toString())
                    }
                    exchange.sendResponseHeaders(200, -1)
                    return
                }

                // ── 3. GET запрос со сквозным кэшированием (Tee-Stream) ────────
                val getReqBuilder = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

                if (!clientRange.isNullOrBlank()) {
                    getReqBuilder.header("Range", clientRange)
                }

                val response = DesktopMeloNet.okHttpClient.newCall(getReqBuilder.build()).execute()
                val code = response.code
                val contentType = response.header("Content-Type")?.takeIf { it.isNotBlank() } ?: defaultContentType
                val rawCl = response.header("Content-Length")?.toLongOrNull() ?: -1L
                val bodyCl = response.body?.contentLength() ?: -1L
                var contentLength = if (rawCl > 0) rawCl else bodyCl
                val contentRange = response.header("Content-Range")
                val acceptRanges = response.header("Accept-Ranges") ?: "bytes"

                val headers = exchange.responseHeaders
                headers.set("Content-Type", contentType)
                headers.set("Accept-Ranges", acceptRanges)
                if (!contentRange.isNullOrBlank()) {
                    headers.set("Content-Range", contentRange)
                }

                // Вычисляем точную длину диапазона для 206 Partial Content
                if (contentLength <= 0L && !contentRange.isNullOrBlank()) {
                    val m = Regex("""bytes\s+(\d+)-(\d+)/(\d+|\*)""").find(contentRange)
                    if (m != null) {
                        val s = m.groupValues[1].toLongOrNull() ?: 0L
                        val e = m.groupValues[2].toLongOrNull() ?: 0L
                        if (e >= s) {
                            contentLength = e - s + 1L
                        }
                    }
                }

                // ВНИМАНИЕ: Для 206 Partial Content responseLength ОБЯЗАН быть точным числом байт!
                // Значение 0L в sendResponseHeaders заставляет JDK HttpServer слать Transfer-Encoding: chunked,
                // что ломает парсинг стрима в Windows Media Foundation / GStreamer и делает перемотку невозможной.
                if (code == 206 && contentLength <= 0L) {
                    val bytes = response.body?.bytes() ?: ByteArray(0)
                    exchange.sendResponseHeaders(206, bytes.size.toLong())
                    exchange.responseBody.use { out ->
                        out.write(bytes)
                        out.flush()
                    }
                    response.close()
                    return
                }

                val responseLength = if (contentLength > 0) contentLength else 0L
                if (contentLength > 0) {
                    remoteMetaCache[targetUrl] = Pair(contentType, contentLength)
                }
                println("[LocalStreamServer] RES: code=$code length=$responseLength (cl=$contentLength) range=$contentRange accept=$acceptRanges")
                exchange.sendResponseHeaders(code, responseLength)

                try {
                    response.body?.byteStream()?.use { input: InputStream ->
                        exchange.responseBody.use { output: OutputStream ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }
                } finally {
                    try { response.close() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (!msg.contains("Connection reset") && !msg.contains("Broken pipe") && !msg.contains("aborted")) {
                    println("[LocalStreamServer] Stream notice: $msg")
                }
            } finally {
                try { exchange.close() } catch (_: Exception) {}
            }
        }

        private fun serveFromLocalFile(
            exchange: HttpExchange,
            file: File,
            contentType: String,
            isHead: Boolean,
            clientRange: String?,
        ) {
            val fileLength = file.length()
            val headers = exchange.responseHeaders
            headers.set("Content-Type", contentType)
            headers.set("Accept-Ranges", "bytes")

            if (isHead) {
                headers.set("Content-Length", fileLength.toString())
                exchange.sendResponseHeaders(200, -1)
                return
            }

            if (!clientRange.isNullOrBlank() && clientRange.startsWith("bytes=")) {
                val rangeSpec = clientRange.removePrefix("bytes=").trim()
                val parts = rangeSpec.split("-")
                val start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                val end = parts.getOrNull(1)?.toLongOrNull() ?: (fileLength - 1L)
                val clampedEnd = end.coerceAtMost(fileLength - 1L)
                val rangeLen = (clampedEnd - start + 1L).coerceAtLeast(0L)

                headers.set("Content-Range", "bytes $start-$clampedEnd/$fileLength")
                exchange.sendResponseHeaders(206, rangeLen)

                RandomAccessFile(file, "r").use { raf ->
                    raf.seek(start)
                    exchange.responseBody.use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var remaining = rangeLen
                        while (remaining > 0) {
                            val toRead = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                            val read = raf.read(buffer, 0, toRead)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            remaining -= read
                        }
                        output.flush()
                    }
                }
            } else {
                exchange.sendResponseHeaders(200, fileLength)
                file.inputStream().use { input ->
                    exchange.responseBody.use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                        output.flush()
                    }
                }
            }
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            try { server?.stop(0) } catch (_: Exception) {}
        })
    }
}
