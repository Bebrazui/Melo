package com.melo.music.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.ResolvedTrack
import com.melo.music.extractor.TrackItem
import com.melo.music.playlists.PlaylistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextMenu(
    item: TrackItem?,
    onDismiss: () -> Unit,
) {
    if (item == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPlaylistPicker by remember { mutableStateOf(false) }

    if (showPlaylistPicker) {
        val playlists = remember { PlaylistManager.getAll() }
        Dialog(onDismissRequest = { showPlaylistPicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "В плейлист",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(14.dp))
                    if (playlists.isEmpty()) {
                        Text(
                            "Нет плейлистов. Создай во вкладке «Аккаунт».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        playlists.forEach { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        val added = PlaylistManager.addTrack(pl.id, item)
                                        Toast.makeText(
                                            context,
                                            if (added) "Добавлено в «${pl.name}»" else "Уже в плейлисте",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        showPlaylistPicker = false
                                        onDismiss()
                                    }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF26262C)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (pl.coverUrl != null) {
                                        AsyncImage(
                                            model = pl.coverUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.LibraryMusic,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        pl.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                    Text(
                                        "${pl.tracks.size} треков",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showPlaylistPicker = false }) { Text("Закрыть") }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    if (!item.uploader.isNullOrBlank()) {
                        Text(
                            text = item.uploader,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            ContextMenuItem(
                icon = Icons.Filled.CloudDownload,
                label = "Скачать",
                subtitle = "Лучшее качество + обложка",
                onClick = {
                    onDismiss()
                    scope.launch(Dispatchers.IO) { downloadTrack(context, item) }
                },
            )
            ContextMenuItem(
                icon = Icons.Filled.Share,
                label = "Поделиться",
                subtitle = "Ссылка на трек",
                onClick = {
                    onDismiss()
                    shareTrack(context, item)
                },
            )
            ContextMenuItem(
                icon = Icons.Filled.Add,
                label = "В плейлист",
                subtitle = "Добавить в один из плейлистов",
                onClick = { showPlaylistPicker = true },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Скачивание MP3 + ID3 ─────────────────────────────────────────────────────

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

private fun sanitizeFilename(s: String): String =
    s.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()

// ── Уведомления о загрузке ───────────────────────────────────────────────────

private const val DL_CHANNEL = "melo_downloads"

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

private fun notifyProgress(context: Context, id: Int, label: String, percent: Int, indeterminate: Boolean) {
    val n = NotificationCompat.Builder(context, DL_CHANNEL)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Скачивание")
        .setContentText(label)
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

private fun toast(context: Context, text: String) {
    android.os.Handler(context.mainLooper).post {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}

private fun downloadTrack(context: Context, item: TrackItem) {
    val notifId = item.url.hashCode()
    val label = "${item.uploader?.let { "$it — " } ?: ""}${item.title}"
    ensureChannel(context)
    try {
        val filename = sanitizeFilename("${item.uploader ?: "Unknown"} - ${item.title}") + ".mp3"

        if (existsInMusic(context, filename)) {
            toast(context, "Уже скачано")
            return
        }

        notifyProgress(context, notifId, label, 0, indeterminate = true)

        // 1. Резолв прямого аудио-URL (мы на IO-потоке).
        val resolved: ResolvedTrack = kotlinx.coroutines.runBlocking {
            Extractor.resolveAudioUrl(context, item.url)
        }

        // 2. Обложка (необязательно).
        val artworkBytes: ByteArray? = item.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { url ->
            runCatching {
                httpClient.newCall(Request.Builder().url(url).build()).execute().use {
                    if (it.isSuccessful) it.body?.bytes() else null
                }
            }.getOrNull()
        }

        // 3. ID3v2-тег.
        val id3 = runCatching { buildId3v2(item.title, item.uploader, artworkBytes) }
            .getOrDefault(ByteArray(0))

        // 4. Качаем аудио потоком с прогрессом в уведомлении.
        val mp3Request = Request.Builder().url(resolved.audioUrl).build()
        val mp3Bytes = httpClient.newCall(mp3Request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("Аудио: HTTP ${resp.code}")
            val body = resp.body ?: throw Exception("Пустой ответ аудио")
            val total = body.contentLength()
            val input = body.byteStream()
            val out = ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            var downloaded = 0L
            var lastPct = -1
            while (true) {
                val r = input.read(buf)
                if (r < 0) break
                out.write(buf, 0, r)
                downloaded += r
                if (total > 0) {
                    val pct = (downloaded * 100 / total).toInt()
                    if (pct >= lastPct + 3) {
                        notifyProgress(context, notifId, label, pct, false)
                        lastPct = pct
                    }
                }
            }
            out.toByteArray()
        }

        // 5. Сохраняем (MediaStore на Android 10+, иначе обычный файл).
        saveToMusic(context, filename, id3 + mp3Bytes)

        notifyDone(context, notifId, label, ok = true, error = null)
    } catch (e: Exception) {
        android.util.Log.e("MeloDownload", "Download failed", e)
        notifyDone(context, notifId, label, ok = false, error = e.message ?: e.javaClass.simpleName)
    }
}

private const val MELO_DIR = "Melo"

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

private fun saveToMusic(context: Context, filename: String, bytes: ByteArray) {
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
            ?: throw Exception("MediaStore не дал URI")
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw Exception("Не открыть поток записи")
        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
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
    }
}

/**
 * Строит ID3v2.3 тег (байты), который кладётся в начало MP3.
 * Поддерживает: TIT2 (title), TPE1 (artist), APIC (cover art).
 */
private fun buildId3v2(title: String, artist: String?, artwork: ByteArray?): ByteArray {
    val frames = mutableListOf<ByteArray>()
    frames.add(textFrame("TIT2", title))
    if (!artist.isNullOrBlank()) frames.add(textFrame("TPE1", artist))
    if (artwork != null && artwork.isNotEmpty()) frames.add(coverFrame(artwork))

    val frameBytes = frames.fold(ByteArray(0)) { acc, f -> acc + f }

    val size = synchsafe(frameBytes.size + 10) // +10 for header
    val header = ByteArray(10)
    header[0] = 'I'.code.toByte()
    header[1] = 'D'.code.toByte()
    header[2] = '3'.code.toByte()
    header[3] = 0x03 // version 2.3
    header[4] = 0x00
    header[5] = 0x00 // flags
    header[6] = ((size shr 21) and 0x7F).toByte()
    header[7] = ((size shr 14) and 0x7F).toByte()
    header[8] = ((size shr 7) and 0x7F).toByte()
    header[9] = (size and 0x7F).toByte()

    return header + frameBytes
}

private fun textFrame(frameId: String, value: String): ByteArray {
    val text = value.toByteArray(Charsets.UTF_8)
    // Frame: ID(4) + size(4) + flags(2) + encoding(1) + text
    val frameData = ByteArray(1 + text.size)
    frameData[0] = 0x03 // UTF-8
    System.arraycopy(text, 0, frameData, 1, text.size)
    return frameHeader(frameId, frameData.size) + frameData
}

private fun coverFrame(jpegData: ByteArray): ByteArray {
    // APIC: encoding(1) + mime("image/jpeg"\0) + pictureType(1) + description("\0") + data
    val mime = "image/jpeg".toByteArray(Charsets.US_ASCII)
    val desc = ByteArray(1) // empty description
    val frameData = ByteArray(1 + mime.size + 1 + desc.size + jpegData.size)
    var pos = 0
    frameData[pos++] = 0x00 // ISO-8859-1
    System.arraycopy(mime, 0, frameData, pos, mime.size)
    pos += mime.size
    frameData[pos++] = 0x00 // null terminator
    frameData[pos++] = 0x03 // FRONT_COVER
    frameData[pos++] = 0x00 // empty description terminator
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
    header[8] = 0x00 // flags
    header[9] = 0x00
    return header
}

private fun synchsafe(value: Int): Int {
    return ((value and 0x0FE00000) shl 3) or
        ((value and 0x001FC000) shl 2) or
        ((value and 0x00003F80) shl 1) or
        (value and 0x0000007F)
}

// ── Поделиться ────────────────────────────────────────────────────────────────

private fun shareTrack(context: Context, item: TrackItem) {
    val text = buildString {
        append(item.title)
        if (!item.uploader.isNullOrBlank()) append(" — ").append(item.uploader)
        append("\nСлушай в Melo: ${item.url}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Поделиться треком"))
}
