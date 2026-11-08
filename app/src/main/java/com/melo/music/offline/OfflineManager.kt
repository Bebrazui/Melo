package com.melo.music.offline

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.File

/**
 * Реестр скачанных (офлайн) треков: ссылка страницы трека → локальный URI + название.
 * Скачанные треки играются с диска (без сети/резолва) и помечаются флагом в UI.
 */
object OfflineManager {

    private const val PREFS = "melo_offline"
    private const val KEY = "map"
    private const val MELO_DIR = "Melo"

    private var prefs: SharedPreferences? = null
    // url -> (localUri, title)
    private val map = HashMap<String, Pair<String, String>>()

    /** Версия для реактивности Compose: меняется при скачивании/удалении. */
    var version by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        load()
    }

    fun isOffline(url: String): Boolean = map.containsKey(url)

    /** Локальный URI скачанного трека (content:// или file://) или null. */
    fun localUri(url: String): String? = map[url]?.first

    fun titleFor(url: String): String? = map[url]?.second

    @Synchronized
    fun add(url: String, localUri: String, title: String) {
        map[url] = localUri to title
        save()
        version++
    }

    @Synchronized
    fun remove(url: String) {
        if (map.remove(url) != null) {
            save()
            version++
        }
    }

    fun count(): Int = map.size

    fun isUriReadable(context: Context, uriStr: String): Boolean = runCatching {
        val uri = Uri.parse(uriStr)
        if (uri.scheme == "file") {
            uri.path?.let { File(it).canRead() } ?: false
        } else {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }
    }.getOrDefault(false)

    fun findUriInMusic(context: Context, filename: String): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.Audio.Media.DISPLAY_NAME}=? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
            val args = arrayOf(filename, "%${Environment.DIRECTORY_MUSIC}/$MELO_DIR%")
            val mediaStoreUri = runCatching {
                context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                        ContentUris.withAppendedId(collection, id).toString()
                    } else null
                }
            }.getOrNull()
            if (mediaStoreUri != null) return mediaStoreUri
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), MELO_DIR)
        val file = File(dir, filename)
        if (file.exists() && file.canRead()) {
            return Uri.fromFile(file).toString()
        }
        return null
    }

    private fun sanitizeFilename(s: String): String =
        s.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()

    /**
     * Возвращает локальный URI скачанного трека:
     * 1) Из сохранённого кэша map[url] (проверив читаемость).
     * 2) Поиском существующего файла в Music/Melo по имени трека/артиста.
     */
    @Synchronized
    fun getOrFindLocalUri(
        context: Context,
        url: String,
        title: String? = null,
        uploader: String? = null,
        fallbackQuery: String? = null,
    ): String? {
        // 1. Проверяем сохраненный в map URL
        map[url]?.first?.let { uriStr ->
            if (isUriReadable(context, uriStr)) return uriStr
            // URI устарел или файл удален — чистим
            map.remove(url)
            save()
        }

        // 2. Формируем список возможных имен файла на диске
        val candidates = mutableListOf<String>()
        val cleanTitle = title?.takeIf { it.isNotBlank() }?.let { sanitizeFilename(it) }
        val cleanUploader = uploader?.takeIf { it.isNotBlank() }?.let { sanitizeFilename(it) }

        if (cleanTitle != null) {
            if (cleanUploader != null) {
                candidates.add("$cleanUploader - $cleanTitle.mp3")
            }
            candidates.add("$cleanTitle.mp3")
        }

        if (fallbackQuery != null && fallbackQuery.isNotBlank()) {
            val cleanQuery = sanitizeFilename(fallbackQuery)
            candidates.add("$cleanQuery.mp3")
        }

        // 3. Ищем среди файлов на диске
        for (cand in candidates) {
            val foundUri = findUriInMusic(context, cand)
            if (foundUri != null && isUriReadable(context, foundUri)) {
                val label = listOfNotNull(cleanUploader, cleanTitle).joinToString(" — ").ifBlank { url }
                add(url, foundUri, label)
                return foundUri
            }
        }

        return null
    }

    private fun load() {
        val json = prefs?.getString(KEY, null) ?: return
        runCatching {
            val obj = JSONObject(json)
            for (k in obj.keys()) {
                val o = obj.getJSONObject(k)
                map[k] = o.getString("uri") to o.optString("title", k)
            }
        }
    }

    private fun save() {
        val obj = JSONObject()
        for ((url, v) in map) {
            obj.put(url, JSONObject().apply { put("uri", v.first); put("title", v.second) })
        }
        prefs?.edit()?.putString(KEY, obj.toString())?.apply()
    }
}
