package com.melo.music.ui

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.ResolvedTrack
import com.melo.music.extractor.TrackItem
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextMenu(
    item: TrackItem?,
    onDismiss: () -> Unit,
) {
    if (item == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                    scope.launch { downloadTrack(context, item) }
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
                subtitle = "Скоро будет",
                onClick = {
                    onDismiss()
                    Toast.makeText(context, "Скоро будет", Toast.LENGTH_SHORT).show()
                },
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

private val httpClient = OkHttpClient.Builder().build()

private fun sanitizeFilename(s: String): String =
    s.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()

private fun downloadTrack(context: Context, item: TrackItem) {
    try {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melo",
        )
        dir.mkdirs()

        val filename = sanitizeFilename(
            "${item.uploader ?: "Unknown"} - ${item.title}",
        ) + ".mp3"
        val mp3File = File(dir, filename)
        if (mp3File.exists()) {
            android.os.Handler(context.mainLooper).post {
                Toast.makeText(context, "Уже скачано", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // 1. Resolve best URL
        val resolved: ResolvedTrack = kotlinx.coroutines.runBlocking {
            Extractor.resolveAudioUrl(context, item.url)
        }

        // 2. Download MP3
        val mp3Request = Request.Builder().url(resolved.audioUrl).build()
        val mp3Response = httpClient.newCall(mp3Request).execute()
        if (!mp3Response.isSuccessful) throw Exception("HTTP ${mp3Response.code}")
        mp3File.writeBytes(mp3Response.body!!.bytes())

        // 3. Download artwork
        var artworkBytes: ByteArray? = null
        if (!item.thumbnailUrl.isNullOrBlank()) {
            try {
                val artRequest = Request.Builder().url(item.thumbnailUrl).build()
                val artResponse = httpClient.newCall(artRequest).execute()
                if (artResponse.isSuccessful) {
                    artworkBytes = artResponse.body!!.bytes()
                }
            } catch (_: Exception) { }
        }

        // 4. Write ID3v2 tag (title + artist + cover art)
        try {
            writeId3v2(mp3File, item.title, item.uploader, artworkBytes)
        } catch (e: Exception) {
            android.util.Log.e("MeloDownload", "ID3 error: ${e.message}")
        }

        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, "Скачано: $filename", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("MeloDownload", "Download failed: ${e.message}", e)
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Записывает ID3v2.3 тег в начало MP3-файла.
 * Поддерживает: TIT2 (title), TPE1 (artist), APIC (cover art).
 */
private fun writeId3v2(file: File, title: String, artist: String?, artwork: ByteArray?) {
    val frames = mutableListOf<ByteArray>()

    // TIT2 — title
    frames.add(textFrame("TIT2", title))

    // TPE1 — artist
    if (!artist.isNullOrBlank()) {
        frames.add(textFrame("TPE1", artist))
    }

    // APIC — cover art (JPEG)
    if (artwork != null && artwork.isNotEmpty()) {
        frames.add(coverFrame(artwork))
    }

    // Собираем все фреймы
    val frameBytes = frames.fold(ByteArray(0)) { acc, f -> acc + f }

    // ID3v2.3 header: "ID3" + version 2.3 + flags + size (synchsafe)
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

    val id3tag = header + frameBytes

    // Вставляем в начало файла
    val tempFile = File(file.parent, file.name + ".tmp")
    file.renameTo(tempFile)
    file.outputStream().use { out ->
        out.write(id3tag)
        tempFile.inputStream().use { inp ->
            inp.copyTo(out)
        }
    }
    tempFile.delete()
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
