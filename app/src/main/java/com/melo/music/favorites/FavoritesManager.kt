package com.melo.music.favorites

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Хранилище избранных треков в SharedPreferences.
 * Выживает перезаход приложения.
 */
object FavoritesManager {

    private const val PREFS = "melo_favorites"
    private const val KEY_TRACKS = "liked_tracks"
    private const val KEY_PUBLIC = "liked_public"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private var inMemoryCache: MutableList<TrackItem>? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    /** Открыты ли лайкнутые песни для других. По умолчанию открыты. */
    fun isPublic(): Boolean = prefs?.getBoolean(KEY_PUBLIC, true) ?: true

    fun setPublic(value: Boolean) {
        prefs?.edit()?.putBoolean(KEY_PUBLIC, value)?.apply()
        com.melo.music.sync.LibrarySync.onFavoritesChanged()
    }

    @Synchronized
    private fun loadFromPrefs(): MutableList<TrackItem> {
        val cached = inMemoryCache
        if (cached != null) return cached
        val json = prefs?.getString(KEY_TRACKS, null) ?: return mutableListOf<TrackItem>().also { inMemoryCache = it }
        val list = runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TrackItem(
                    title = obj.getString("title"),
                    uploader = obj.optString("uploader", "").ifBlank { null },
                    url = obj.getString("url"),
                    durationSeconds = obj.optLong("duration", 0),
                    thumbnailUrl = obj.optString("thumbnail", "").ifBlank { null },
                    source = Source.valueOf(obj.optString("source", "YOUTUBE_MUSIC")),
                    kind = ItemKind.TRACK,
                    speed = obj.optDouble("speed", 1.0).toFloat(),
                    bassBoost = obj.optBoolean("bassBoost", false),
                    crush = obj.optDouble("crush", 0.0).toFloat(),
                    pitchSemi = obj.optDouble("pitchSemi", 0.0).toFloat(),
                    noteShiftSeed = obj.optLong("noteShiftSeed", 0L),
                )
            }.toMutableList()
        }.getOrDefault(mutableListOf())
        inMemoryCache = list
        return list
    }

    @Synchronized
    fun getAll(): MutableList<TrackItem> = ArrayList(inMemoryCache ?: loadFromPrefs())

    @Synchronized
    fun isLiked(url: String): Boolean = (inMemoryCache ?: loadFromPrefs()).any { it.url == url }

    /** Совпадение с учётом скорости, басс-буста и эффектов. */
    @Synchronized
    fun isLiked(item: TrackItem): Boolean =
        (inMemoryCache ?: loadFromPrefs()).any { matches(it, item) }

    @Synchronized
    fun toggle(item: TrackItem): Boolean {
        val list = inMemoryCache ?: loadFromPrefs()
        val idx = list.indexOfFirst { matches(it, item) }
        return if (idx >= 0) {
            list.removeAt(idx)
            save(list)
            com.melo.music.sync.LibrarySync.onFavoritesChanged()
            false
        } else {
            list.add(item)
            save(list)
            com.melo.music.sync.LibrarySync.onFavoritesChanged()
            // Заранее резолвим прямую ссылку → следующее воспроизведение мгновенное.
            appContext?.let { ctx -> Extractor.prefetch(ctx, item.url) }
            true
        }
    }

    /** Полная замена локального списка (используется облачным синком — без обратной выгрузки). */
    @Synchronized
    fun replaceAll(list: List<TrackItem>) = save(list)

    @Synchronized
    private fun save(list: List<TrackItem>) {
        inMemoryCache = list.toMutableList()
        val arr = JSONArray()
        for (t in list) {
            arr.put(JSONObject().apply {
                put("title", t.title)
                put("uploader", t.uploader ?: "")
                put("url", t.url)
                put("duration", t.durationSeconds)
                put("thumbnail", t.thumbnailUrl ?: "")
                put("source", t.source.name)
                put("speed", t.speed.toDouble())
                put("bassBoost", t.bassBoost)
                put("crush", t.crush.toDouble())
                put("pitchSemi", t.pitchSemi.toDouble())
                put("noteShiftSeed", t.noteShiftSeed)
            })
        }
        prefs?.edit()?.putString(KEY_TRACKS, arr.toString())?.apply()
    }

    private fun sameSpeed(a: Float, b: Float) = kotlin.math.abs(a - b) < 0.01f

    private fun matches(a: TrackItem, b: TrackItem): Boolean =
        a.url == b.url &&
            sameSpeed(a.speed, b.speed) &&
            a.bassBoost == b.bassBoost &&
            kotlin.math.abs(a.crush - b.crush) < 0.01f &&
            kotlin.math.abs(a.pitchSemi - b.pitchSemi) < 0.01f &&
            a.noteShiftSeed == b.noteShiftSeed
}
