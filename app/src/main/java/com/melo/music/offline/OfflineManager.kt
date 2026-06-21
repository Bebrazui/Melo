package com.melo.music.offline

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * Реестр скачанных (офлайн) треков: ссылка страницы трека → локальный URI + название.
 * Скачанные треки играются с диска (без сети/резолва) и помечаются флагом в UI.
 */
object OfflineManager {

    private const val PREFS = "melo_offline"
    private const val KEY = "map"

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
