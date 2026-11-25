package com.melo.music.favorites

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.db.FavoriteEntity
import com.melo.music.db.MeloDatabase
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Хранилище избранных треков на Room Database.
 * Автоматически мигрирует старые данные из SharedPreferences.
 */
object FavoritesManager {

    private const val PREFS = "melo_favorites"
    private const val KEY_TRACKS = "liked_tracks"
    private const val KEY_PUBLIC = "liked_public"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private var database: MeloDatabase? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Быстрый кэш в памяти для мгновенного синхронного доступа в UI (getAll(), isLiked())
    private val inMemoryCache = CopyOnWriteArrayList<TrackItem>()
    private val _favoritesFlow = MutableStateFlow<List<TrackItem>>(emptyList())
    val favoritesFlow: StateFlow<List<TrackItem>> = _favoritesFlow.asStateFlow()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val db = MeloDatabase.getInstance(app)
        database = db

        // Загрузка кэша и фоновая миграция
        scope.launch {
            val dao = db.favoritesDao()
            // Проверяем, есть ли старые данные в SharedPreferences для миграции
            val oldJson = prefs?.getString(KEY_TRACKS, null)
            if (!oldJson.isNullOrBlank()) {
                val migrated = parseLegacyJson(oldJson)
                if (migrated.isNotEmpty()) {
                    dao.insertAll(migrated.map { FavoriteEntity.fromTrackItem(it) })
                }
                prefs?.edit()?.remove(KEY_TRACKS)?.apply()
            }

            // Инициализация кэша из Room
            val dbTracks = dao.getAll().map { it.toTrackItem() }
            inMemoryCache.clear()
            inMemoryCache.addAll(dbTracks)
            _favoritesFlow.value = inMemoryCache.toList()

            // Наблюдаем за базой для реактивных обновлений
            dao.observeAll().collect { list ->
                val items = list.map { it.toTrackItem() }
                inMemoryCache.clear()
                inMemoryCache.addAll(items)
                _favoritesFlow.value = items
            }
        }
        initialized = true
    }

    /** Открыты ли лайкнутые песни для других. */
    fun isPublic(): Boolean = prefs?.getBoolean(KEY_PUBLIC, true) ?: true

    fun setPublic(value: Boolean) {
        prefs?.edit()?.putBoolean(KEY_PUBLIC, value)?.apply()
        com.melo.music.sync.LibrarySync.onFavoritesChanged()
    }

    fun getAll(): MutableList<TrackItem> = ArrayList(inMemoryCache)

    fun isLiked(url: String): Boolean = inMemoryCache.any { it.url == url }

    /** Совпадение с учётом скорости, басс-буста и эффектов. */
    fun isLiked(item: TrackItem): Boolean = inMemoryCache.any { matches(it, item) }

    fun toggle(item: TrackItem): Boolean {
        val existing = inMemoryCache.firstOrNull { matches(it, item) }
        val dao = database?.favoritesDao()
        return if (existing != null) {
            inMemoryCache.remove(existing)
            _favoritesFlow.value = inMemoryCache.toList()
            scope.launch {
                dao?.deleteById(FavoriteEntity.fromTrackItem(item).id)
            }
            com.melo.music.sync.LibrarySync.onFavoritesChanged()
            false
        } else {
            inMemoryCache.add(0, item)
            _favoritesFlow.value = inMemoryCache.toList()
            scope.launch {
                dao?.insert(FavoriteEntity.fromTrackItem(item))
            }
            com.melo.music.sync.LibrarySync.onFavoritesChanged()
            appContext?.let { ctx -> Extractor.prefetch(ctx, item.url) }
            true
        }
    }

    /** Полная замена списка (облачный синк). */
    fun replaceAll(list: List<TrackItem>) {
        inMemoryCache.clear()
        inMemoryCache.addAll(list)
        _favoritesFlow.value = list
        scope.launch {
            val dao = database?.favoritesDao() ?: return@launch
            dao.clearAll()
            dao.insertAll(list.map { FavoriteEntity.fromTrackItem(it) })
        }
    }

    private fun parseLegacyJson(json: String): List<TrackItem> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            TrackItem(
                title = obj.getString("title"),
                uploader = obj.optString("uploader", "").ifBlank { null },
                url = obj.getString("url"),
                durationSeconds = obj.optLong("duration", 0),
                thumbnailUrl = obj.optString("thumbnail", "").ifBlank { null },
                source = runCatching { Source.valueOf(obj.optString("source", "YOUTUBE_MUSIC")) }.getOrDefault(Source.YOUTUBE_MUSIC),
                kind = ItemKind.TRACK,
                speed = obj.optDouble("speed", 1.0).toFloat(),
                bassBoost = obj.optBoolean("bassBoost", false),
                crush = obj.optDouble("crush", 0.0).toFloat(),
                pitchSemi = obj.optDouble("pitchSemi", 0.0).toFloat(),
                noteShiftSeed = obj.optLong("noteShiftSeed", 0L),
            )
        }
    }.getOrDefault(emptyList())

    private fun sameSpeed(a: Float, b: Float) = kotlin.math.abs(a - b) < 0.01f

    private fun matches(a: TrackItem, b: TrackItem): Boolean =
        a.url == b.url &&
            sameSpeed(a.speed, b.speed) &&
            a.bassBoost == b.bassBoost &&
            kotlin.math.abs(a.crush - b.crush) < 0.01f &&
            kotlin.math.abs(a.pitchSemi - b.pitchSemi) < 0.01f &&
            a.noteShiftSeed == b.noteShiftSeed
}
