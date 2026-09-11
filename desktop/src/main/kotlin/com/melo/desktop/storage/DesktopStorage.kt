package com.melo.desktop.storage

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.melo.desktop.byedpi.ByeDpiManager
import com.melo.desktop.extractor.ItemKind
import com.melo.desktop.extractor.Source
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.zapret.ZapretPreset
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Режимы обхода блокировок и замедления (DPI).
 */
enum class DpiEngine(val title: String, val subtitle: String) {
    AUTO("Автоматически", "Zapret (если запущен) → ByeDPI → Прямое соединение"),
    ZAPRET("Zapret (Flowseal)", "Системная фильтрация через winws.exe и WinDivert (UAC)"),
    BYEDPI("ByeDPI", "Локальный SOCKS5 прокси ciadpi без прав администратора"),
    DISABLED("Отключено", "Прямое соединение (для работы через собственный VPN)"),
}

/**
 * Локальное хранилище данных пользователя в ~/.melo/.
 */
object DesktopStorage {

    private val baseDir = File(System.getProperty("user.home"), ".melo").apply { mkdirs() }
    private val favoritesFile = File(baseDir, "favorites.json")
    private val historyFile = File(baseDir, "history.json")
    private val playlistsFile = File(baseDir, "playlists.json")
    private val settingsFile = File(baseDir, "settings.json")

    val favorites = mutableStateListOf<TrackItem>()
    val history = mutableStateListOf<TrackItem>()
    val playlists = mutableStateOf<Map<String, List<TrackItem>>>(emptyMap())

    var dpiEngine = mutableStateOf(DpiEngine.AUTO)
    var zapretPreset = mutableStateOf(ZapretPreset.GENERAL)
    var zapretCustomArgs = mutableStateOf("")
    var zapretCustomPath = mutableStateOf("")

    var byedpiEnabled = mutableStateOf(true)
    var byedpiCmd = mutableStateOf(ByeDpiManager.DEFAULT_CMD)

    init {
        loadSettings()
        loadFavorites()
        loadHistory()
        loadPlaylists()
    }

    // ── Избранное ────────────────────────────────────────────────────────────

    fun isFavorite(url: String): Boolean = favorites.any { it.url == url }

    fun toggleFavorite(track: TrackItem) {
        if (isFavorite(track.url)) {
            favorites.removeAll { it.url == track.url }
        } else {
            favorites.add(0, track)
        }
        saveFavorites()
    }

    private fun loadFavorites() {
        if (!favoritesFile.exists()) return
        runCatching {
            val json = favoritesFile.readText()
            val arr = JSONArray(json)
            favorites.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                favorites.add(trackFromJson(obj))
            }
        }
    }

    private fun saveFavorites() {
        runCatching {
            val arr = JSONArray()
            favorites.forEach { arr.put(trackToJson(it)) }
            favoritesFile.writeText(arr.toString(2))
        }
    }

    // ── История ──────────────────────────────────────────────────────────────

    fun addToHistory(track: TrackItem) {
        history.removeAll { it.url == track.url }
        history.add(0, track)
        while (history.size > 200) history.removeAt(history.lastIndex)
        saveHistory()
    }

    fun clearHistory() {
        history.clear()
        saveHistory()
    }

    private fun loadHistory() {
        if (!historyFile.exists()) return
        runCatching {
            val json = historyFile.readText()
            val arr = JSONArray(json)
            history.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                history.add(trackFromJson(obj))
            }
        }
    }

    private fun saveHistory() {
        runCatching {
            val arr = JSONArray()
            history.forEach { arr.put(trackToJson(it)) }
            historyFile.writeText(arr.toString(2))
        }
    }

    // ── Плейлисты ────────────────────────────────────────────────────────────

    fun createPlaylist(name: String) {
        val cur = playlists.value.toMutableMap()
        if (!cur.containsKey(name)) {
            cur[name] = emptyList()
            playlists.value = cur
            savePlaylists()
        }
    }

    fun addToPlaylist(name: String, track: TrackItem) {
        val cur = playlists.value.toMutableMap()
        val list = cur[name]?.toMutableList() ?: mutableListOf()
        if (list.none { it.url == track.url }) {
            list.add(track)
            cur[name] = list
            playlists.value = cur
            savePlaylists()
        }
    }

    fun removeFromPlaylist(name: String, url: String) {
        val cur = playlists.value.toMutableMap()
        val list = cur[name]?.toMutableList() ?: return
        list.removeAll { it.url == url }
        cur[name] = list
        playlists.value = cur
        savePlaylists()
    }

    fun deletePlaylist(name: String) {
        val cur = playlists.value.toMutableMap()
        cur.remove(name)
        playlists.value = cur
        savePlaylists()
    }

    private fun loadPlaylists() {
        if (!playlistsFile.exists()) return
        runCatching {
            val json = playlistsFile.readText()
            val obj = JSONObject(json)
            val map = mutableMapOf<String, List<TrackItem>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = obj.getJSONArray(key)
                val tracks = (0 until arr.length()).map { trackFromJson(arr.getJSONObject(it)) }
                map[key] = tracks
            }
            playlists.value = map
        }
    }

    private fun savePlaylists() {
        runCatching {
            val obj = JSONObject()
            playlists.value.forEach { (k, list) ->
                val arr = JSONArray()
                list.forEach { arr.put(trackToJson(it)) }
                obj.put(k, arr)
            }
            playlistsFile.writeText(obj.toString(2))
        }
    }

    // ── Настройки ────────────────────────────────────────────────────────────

    private fun loadSettings() {
        if (!settingsFile.exists()) return
        runCatching {
            val obj = JSONObject(settingsFile.readText())
            val engineName = obj.optString("dpiEngine", DpiEngine.AUTO.name)
            dpiEngine.value = runCatching { DpiEngine.valueOf(engineName) }.getOrDefault(DpiEngine.AUTO)
            val presetName = obj.optString("zapretPreset", ZapretPreset.GENERAL.name)
            zapretPreset.value = runCatching { ZapretPreset.valueOf(presetName) }.getOrDefault(ZapretPreset.GENERAL)
            zapretCustomArgs.value = obj.optString("zapretCustomArgs", "")
            zapretCustomPath.value = obj.optString("zapretCustomPath", "")
            byedpiEnabled.value = obj.optBoolean("byedpiEnabled", true)
            byedpiCmd.value = obj.optString("byedpiCmd", ByeDpiManager.DEFAULT_CMD)
            ByeDpiManager.isEnabled = byedpiEnabled.value
        }
    }

    fun saveSettings() {
        runCatching {
            val obj = JSONObject().apply {
                put("dpiEngine", dpiEngine.value.name)
                put("zapretPreset", zapretPreset.value.name)
                put("zapretCustomArgs", zapretCustomArgs.value)
                put("zapretCustomPath", zapretCustomPath.value)
                put("byedpiEnabled", byedpiEnabled.value)
                put("byedpiCmd", byedpiCmd.value)
            }
            settingsFile.writeText(obj.toString(2))
        }
    }

    // ── Сериализация TrackItem ────────────────────────────────────────────────

    private fun trackToJson(track: TrackItem): JSONObject = JSONObject().apply {
        put("title", track.title)
        put("uploader", track.uploader)
        put("url", track.url)
        put("duration", track.durationSeconds)
        put("thumbnail", track.thumbnailUrl)
        put("source", track.source.name)
        put("kind", track.kind.name)
        put("speed", track.speed.toDouble())
        put("views", track.viewCount)
    }

    private fun trackFromJson(obj: JSONObject): TrackItem = TrackItem(
        title = obj.getString("title"),
        uploader = obj.optString("uploader", null),
        url = obj.getString("url"),
        durationSeconds = obj.optLong("duration", 0),
        thumbnailUrl = obj.optString("thumbnail", null),
        source = Source.valueOf(obj.optString("source", "YOUTUBE_MUSIC")),
        kind = ItemKind.valueOf(obj.optString("kind", "TRACK")),
        speed = obj.optDouble("speed", 1.0).toFloat(),
        viewCount = obj.optLong("views", 0),
    )
}
