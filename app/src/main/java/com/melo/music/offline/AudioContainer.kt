package com.melo.music.offline

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream

/**
 * Реальный аудио-контейнер файла. Определяется по сигнатуре файла, а НЕ по расширению:
 * приложение годами сохраняло поток SoundCloud/YouTube в «.mp3» с MIME audio/mpeg,
 * хотя внутри лежал WebM/Opus или фрагментированный MP4 — Media3 такой файл не читает
 * (ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED), и трек «залипал» на ошибке по кругу.
 */
enum class AudioFormat(val mimeType: String, val extension: String) {
    MP3("audio/mpeg", "mp3"),
    MP4("audio/mp4", "m4a"),
    WEBM("audio/webm", "webm"),
    OGG("audio/ogg", "ogg"),
    WAV("audio/wav", "wav"),
    UNKNOWN("", "");

    val isPlayable: Boolean get() = this != UNKNOWN
}

/** Определение и проверка контейнера аудиофайла. */
object AudioContainer {

    private const val PROBE_BYTES = 16

    /** Определяет формат по первым байтам файла (в т.ч. с ID3-префиксом). */
    fun detect(bytes: ByteArray): AudioFormat {
        if (bytes.size < 4) return AudioFormat.UNKNOWN
        if (hasId3(bytes)) {
            if (bytes.size < 10) return AudioFormat.UNKNOWN
            val bodyOffset = 10 + id3Size(bytes)
            if (bodyOffset <= 0 || bodyOffset >= bytes.size) return AudioFormat.UNKNOWN
            val body = detectRaw(bytes, bodyOffset)
            // ID3-префикс читается Media3 только перед MP3; для WebM/MP4/OGG он ломает файл.
            return if (body == AudioFormat.MP3) body else AudioFormat.UNKNOWN
        }
        return detectRaw(bytes, 0)
    }

    /** Определяет формат локального файла по URI (content:// или file://). */
    fun probeUri(context: Context, uriStr: String): AudioFormat = runCatching {
        val uri = Uri.parse(uriStr)
        val stream: InputStream = if (uri.scheme == "file") {
            val path = uri.path ?: return@runCatching AudioFormat.UNKNOWN
            File(path).inputStream()
        } else {
            context.contentResolver.openInputStream(uri) ?: return@runCatching AudioFormat.UNKNOWN
        }
        stream.use { probe(it) }
    }.getOrDefault(AudioFormat.UNKNOWN)

    /** Определяет формат по потоку (читает только начало файла). */
    fun probe(stream: InputStream): AudioFormat {
        val head = ByteArray(PROBE_BYTES)
        if (readFully(stream, head) < 4) return AudioFormat.UNKNOWN
        if (hasId3(head)) {
            val bodyOffset = 10L + id3Size(head)
            if (bodyOffset < head.size) {
                // Тег помещается в уже прочитанный буфер.
                val f = detectRaw(head, bodyOffset.toInt())
                return if (f == AudioFormat.MP3) f else AudioFormat.UNKNOWN
            }
            var skipped = 0L
            val toSkip = bodyOffset - head.size
            while (skipped < toSkip) {
                val s = stream.skip(toSkip - skipped)
                if (s <= 0) break
                skipped += s
            }
            if (skipped < toSkip) return AudioFormat.UNKNOWN
            val body = ByteArray(PROBE_BYTES)
            if (readFully(stream, body) < 4) return AudioFormat.UNKNOWN
            val f = detectRaw(body, 0)
            return if (f == AudioFormat.MP3) f else AudioFormat.UNKNOWN
        }
        return detectRaw(head, 0)
    }

    private fun detectRaw(b: ByteArray, off: Int): AudioFormat {
        val n = b.size - off
        if (n < 2) return AudioFormat.UNKNOWN
        // ISO-BMFF (MP4/M4A): 'ftyp' на смещении 4.
        if (n >= 8 && matches(b, off + 4, "ftyp")) return AudioFormat.MP4
        // Matroska / WebM (Opus/Vorbis внутри).
        if ((b[off].toInt() and 0xFF) == 0x1A && (b[off + 1].toInt() and 0xFF) == 0x45 &&
            (b[off + 2].toInt() and 0xFF) == 0xDF && (b[off + 3].toInt() and 0xFF) == 0xA3
        ) return AudioFormat.WEBM
        // Ogg (Vorbis/Opus).
        if (matches(b, off, "OggS")) return AudioFormat.OGG
        // WAV (RIFF....WAVE).
        if (n >= 12 && matches(b, off, "RIFF") && matches(b, off + 8, "WAVE")) return AudioFormat.WAV
        // MP3 frame sync.
        if ((b[off].toInt() and 0xFF) == 0xFF && (b[off + 1].toInt() and 0xE0) == 0xE0) {
            return AudioFormat.MP3
        }
        return AudioFormat.UNKNOWN
    }

    private fun hasId3(b: ByteArray): Boolean =
        b.size >= 3 && b[0] == 'I'.code.toByte() && b[1] == 'D'.code.toByte() && b[2] == '3'.code.toByte()

    /** Размер ID3v2-тега (байты после 10-байтного заголовка). */
    private fun id3Size(b: ByteArray): Int {
        val version = b[3].toInt() and 0xFF
        return if (version == 2) {
            ((b[6].toInt() and 0xFF) shl 16) or ((b[7].toInt() and 0xFF) shl 8) or (b[8].toInt() and 0xFF)
        } else {
            ((b[6].toInt() and 0x7F) shl 21) or ((b[7].toInt() and 0x7F) shl 14) or
                ((b[8].toInt() and 0x7F) shl 7) or (b[9].toInt() and 0x7F)
        }
    }

    private fun matches(b: ByteArray, off: Int, s: String): Boolean {
        if (off < 0 || off + s.length > b.size) return false
        for (i in s.indices) {
            if (b[off + i].toInt() and 0xFF != s[i].code) return false
        }
        return true
    }

    private fun readFully(stream: InputStream, buf: ByteArray): Int {
        var read = 0
        while (read < buf.size) {
            val r = stream.read(buf, read, buf.size - read)
            if (r <= 0) break
            read += r
        }
        return read
    }
}
