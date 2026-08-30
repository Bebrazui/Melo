package com.melo.music.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.ResolvedTrack
import com.melo.music.extractor.TrackItem
import com.melo.music.net.MeloNet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Надежный загрузчик треков в память устройства с интеграцией ByeDPI и точным прогрессом.
 */
object TrackDownloader {

    private const val MELO_DIR = "Melo"
    private const val DL_CHANNEL = "melo_downloads"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isBatchDownloading = AtomicBoolean(false)

    private val httpClient = OkHttpClient.Builder()
        .proxySelector(MeloNet.byedpiSelector)
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun download(context: Context, item: TrackItem) {
        scope.launch {
            downloadSingleTrack(context.applicationContext, item)
        }
    }

    fun downloadAll(context: Context, items: List<TrackItem>) {
        val appContext = context.applicationContext
        val toDownload = items.filter { !OfflineManager.isOffline(it.url) }
        if (toDownload.isEmpty()) {
            toast(appContext, "Все треки из этого списка уже скачаны")
            return
        }

        if (!isBatchDownloading.compareAndSet(false, true)) {
            toast(appContext, "Загрузка уже идёт...")
            return
        }

        toast(appContext, "Скачивание ${toDownload.size} треков начато")
        scope.launch {
            try {
                toDownload.forEachIndexed { index, track ->
                    downloadSingleTrack(appContext, track, batchIndex = index + 1, batchTotal = toDownload.size)
                }
                toast(appContext, "Все ${toDownload.size} треков успешно скачаны!")
            } finally {
                isBatchDownloading.set(false)
            }
        }
    }

    private suspend fun downloadSingleTrack(
        context: Context,
        item: TrackItem,
        batchIndex: Int? = null,
        batchTotal: Int? = null,
    ) {
        val notifId = item.url.hashCode()
        val label = "${item.uploader?.let { "$it — " } ?: ""}${item.title}"
        ensureChannel(context)

        try {
            val filename = sanitizeFilename("${item.uploader ?: "Unknown"} - ${item.title}") + ".mp3"

            if (existsInMusic(context, filename)) {
                if (batchIndex == null) toast(context, "Уже скачано")
                return
            }

            val prefix = if (batchIndex != null && batchTotal != null) "[$batchIndex/$batchTotal] " else ""
            notifyProgress(context, notifId, "$prefix$label", "Получение ссылки...", 0, indeterminate = true)

            // 1. Резолв аудио-ссылки (через NewPipe/yt-dlp)
            val resolved: ResolvedTrack = Extractor.resolveAudioUrl(context, item.url)

            // 2. Скачивание обложки
            val artworkBytes: ByteArray? = item.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { url ->
                runCatching {
                    httpClient.newCall(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).execute().use {
                        if (it.isSuccessful) it.body?.bytes() else null
                    }
                }.getOrNull()
            }

            // 3. Сборка ID3-тегов
            val id3 = runCatching { buildId3v2(item.title, item.uploader, artworkBytes) }
                .getOrDefault(ByteArray(0))

            notifyProgress(context, notifId, "$prefix$label", "Скачивание аудио...", 5, indeterminate = false)

            // 4. Скачивание MP3-потока
            val mp3Request = Request.Builder()
                .url(resolved.audioUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val mp3Bytes = httpClient.newCall(mp3Request).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("Сервер вернул ошибку: HTTP ${resp.code}")
                val body = resp.body ?: throw Exception("Пустой ответ от сервера")
                val total = body.contentLength()
                val estimatedTotal = if (total > 0) total else (item.durationSeconds.coerceAtLeast(60) * 16_000L)
                val input = body.byteStream()
                val out = ByteArrayOutputStream()
                val buf = ByteArray(32 * 1024)
                var downloaded = 0L
                var lastPct = -1

                while (true) {
                    val r = input.read(buf)
                    if (r < 0) break
                    out.write(buf, 0, r)
                    downloaded += r

                    val pct = ((downloaded * 100) / estimatedTotal).toInt().coerceIn(5, 98)
                    if (pct >= lastPct + 4) {
                        val mbText = if (total > 0) {
                            String.format(Locale.US, "%.1f / %.1f МБ", downloaded / 1048576f, total / 1048576f)
                        } else {
                            String.format(Locale.US, "%.1f МБ", downloaded / 1048576f)
                        }
                        notifyProgress(context, notifId, "$prefix$label", mbText, pct, indeterminate = false)
                        lastPct = pct
                    }
                }
                out.toByteArray()
            }

            // 5. Сохранение в общую папку Music/Melo
            val savedUri = saveToMusic(context, filename, id3 + mp3Bytes)

            // 6. Добавление в OfflineManager
            if (savedUri != null) {
                OfflineManager.add(item.url, savedUri, label)
            }

            notifyDone(context, notifId, label, ok = true, error = null)
        } catch (e: Exception) {
            notifyDone(context, notifId, label, ok = false, error = e.message ?: e.javaClass.simpleName)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(DL_CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(DL_CHANNEL, "Загрузки", NotificationManager.IMPORTANCE_LOW),
                )
            }
        }
    }

    private fun notifyProgress(
        context: Context,
        id: Int,
        title: String,
        statusText: String,
        percent: Int,
        indeterminate: Boolean,
    ) {
        val n = NotificationCompat.Builder(context, DL_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(statusText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, indeterminate)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    private fun notifyDone(context: Context, id: Int, label: String, ok: Boolean, error: String?) {
        val n = NotificationCompat.Builder(context, DL_CHANNEL)
            .setSmallIcon(
                if (ok) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error,
            )
            .setContentTitle(if (ok) "Скачано ✓" else "Ошибка скачивания")
            .setContentText(if (ok) label else (error ?: "неизвестная ошибка"))
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    private fun sanitizeFilename(s: String): String =
        s.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()

    private fun existsInMusic(context: Context, filename: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.Audio.Media.DISPLAY_NAME}=? AND " +
                "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
            val args = arrayOf(filename, "%${Environment.DIRECTORY_MUSIC}/$MELO_DIR%")
            return runCatching {
                context.contentResolver.query(collection, projection, selection, args, null)
                    ?.use { it.count > 0 } ?: false
            }.getOrDefault(false)
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), MELO_DIR)
        return File(dir, filename).exists()
    }

    private fun saveToMusic(context: Context, filename: String, bytes: ByteArray): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$MELO_DIR")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values)
                ?: throw Exception("MediaStore не вернул URI")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw Exception("Не удалось открыть поток записи MediaStore")
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri.toString()
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                MELO_DIR,
            )
            dir.mkdirs()
            val file = File(dir, filename)
            file.writeBytes(bytes)
            android.media.MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf("audio/mpeg"), null,
            )
            return android.net.Uri.fromFile(file).toString()
        }
    }

    private fun buildId3v2(title: String, artist: String?, artwork: ByteArray?): ByteArray {
        val frames = mutableListOf<ByteArray>()
        frames.add(textFrame("TIT2", title))
        if (!artist.isNullOrBlank()) frames.add(textFrame("TPE1", artist))
        if (artwork != null && artwork.isNotEmpty()) frames.add(coverFrame(artwork))

        val frameBytes = frames.fold(ByteArray(0)) { acc, f -> acc + f }

        val size = synchsafe(frameBytes.size + 10)
        val header = ByteArray(10)
        header[0] = 'I'.code.toByte()
        header[1] = 'D'.code.toByte()
        header[2] = '3'.code.toByte()
        header[3] = 0x03
        header[4] = 0x00
        header[5] = 0x00
        header[6] = ((size shr 21) and 0x7F).toByte()
        header[7] = ((size shr 14) and 0x7F).toByte()
        header[8] = ((size shr 7) and 0x7F).toByte()
        header[9] = (size and 0x7F).toByte()

        return header + frameBytes
    }

    private fun textFrame(frameId: String, value: String): ByteArray {
        val text = value.toByteArray(Charsets.UTF_8)
        val frameData = ByteArray(1 + text.size)
        frameData[0] = 0x03
        System.arraycopy(text, 0, frameData, 1, text.size)
        return frameHeader(frameId, frameData.size) + frameData
    }

    private fun coverFrame(jpegData: ByteArray): ByteArray {
        val mime = "image/jpeg".toByteArray(Charsets.US_ASCII)
        val desc = ByteArray(1)
        val frameData = ByteArray(1 + mime.size + 1 + desc.size + jpegData.size)
        var pos = 0
        frameData[pos++] = 0x00
        System.arraycopy(mime, 0, frameData, pos, mime.size)
        pos += mime.size
        frameData[pos++] = 0x00
        frameData[pos++] = 0x03
        frameData[pos++] = 0x00
        System.arraycopy(jpegData, 0, frameData, pos, jpegData.size)
        return frameHeader("APIC", frameData.size) + frameData
    }

    private fun frameHeader(frameId: String, dataSize: Int): ByteArray {
        val header = ByteArray(10)
        header[0] = frameId[0].code.toByte()
        header[1] = frameId[1].code.toByte()
        header[2] = frameId[2].code.toByte()
        header[3] = frameId[3].code.toByte()
        header[4] = ((dataSize shr 24) and 0xFF).toByte()
        header[5] = ((dataSize shr 16) and 0xFF).toByte()
        header[6] = ((dataSize shr 8) and 0xFF).toByte()
        header[7] = (dataSize and 0xFF).toByte()
        header[8] = 0x00
        header[9] = 0x00
        return header
    }

    private fun synchsafe(value: Int): Int {
        return ((value and 0x0FE00000) shl 3) or
            ((value and 0x001FC000) shl 2) or
            ((value and 0x00003F80) shl 1) or
            (value and 0x0000007F)
    }

    private fun toast(context: Context, text: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}
