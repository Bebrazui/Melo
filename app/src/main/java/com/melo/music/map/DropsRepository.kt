package com.melo.music.map

import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Музыкальный «пин» на карте. */
data class MapDrop(
    val id: String,
    val lat: Double,
    val lng: Double,
    val title: String,
    val artist: String?,
    val thumbnailUrl: String?,
    val sourceUrl: String,
    val source: Source,
    val caption: String,
    val ownerId: String,
) {
    /** Превращает пин в обычный трек для воспроизведения нашим резолвом. */
    fun toTrackItem() = TrackItem(
        title = title,
        uploader = artist,
        url = sourceUrl,
        durationSeconds = 0,
        thumbnailUrl = thumbnailUrl,
        source = source,
        kind = ItemKind.TRACK,
    )
}

object DropsRepository {

    // Короткий кэш ответов карты в памяти: пины меняются нечасто, а карту открывают/
    // двигают часто → не дёргаем сервер на каждое движение. По истечении TTL — обновляем.
    private const val CACHE_TTL_MS = 90_000L
    private val areaCache = java.util.concurrent.ConcurrentHashMap<String, Pair<List<MapDrop>, Long>>()
    @Volatile private var recentCache: Pair<List<MapDrop>, Long>? = null
    val knownDrops = java.util.concurrent.ConcurrentHashMap<String, MapDrop>()

    private fun fresh(stamp: Long): Boolean = System.currentTimeMillis() - stamp < CACHE_TTL_MS

    /** Сбросить кэш карты (после создания/удаления своего пина). */
    fun invalidate() { areaCache.clear(); recentCache = null; knownDrops.clear() }

    /** Пины в видимой области карты (bounding box). Быстрый локальный фильтр + фоновая синхронизация. */
    suspend fun listInArea(
        minLat: Double, maxLat: Double, minLng: Double, maxLng: Double,
    ): List<MapDrop> = withContext(Dispatchers.IO) {
        val actualMinLat = minOf(minLat, maxLat)
        val actualMaxLat = maxOf(minLat, maxLat)
        val wrap = minLng > maxLng

        fun filterLocal(collection: Collection<MapDrop>): List<MapDrop> =
            collection.filter { d ->
                d.lat in actualMinLat..actualMaxLat &&
                    (if (wrap) d.lng >= minLng || d.lng <= maxLng else d.lng in minLng..maxLng)
            }.filterNot { MapModeration.isHidden(it.id) }

        // Если кэш пуст — загружаем свежие со всей карты
        if (knownDrops.isEmpty()) {
            runCatching { recent(500) }
        } else {
            // В фоне обновляем кэш свежих дропов без блокировки
            if (recentCache == null || !fresh(recentCache?.second ?: 0L)) {
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    runCatching { recent(500) }
                }
            }
        }

        // Мгновенная выдача из локального кэша — 0 мс задержки
        val localHits = filterLocal(knownDrops.values)
        if (localHits.isNotEmpty()) {
            return@withContext localHits
        }

        // Если локально пока ничего нет — безопасный точечный запрос
        val key = "%.2f,%.2f,%.2f,%.2f".format(actualMinLat, actualMaxLat, minLng, maxLng)
        areaCache[key]?.takeIf { fresh(it.second) }?.let { return@withContext it.first }

        runCatching {
            val res = AppwriteService.databases.listDocuments(
                databaseId = AppwriteService.DATABASE_ID,
                collectionId = AppwriteService.COLLECTION_DROPS,
                queries = listOf(
                    Query.greaterThan("lat", actualMinLat),
                    Query.lessThan("lat", actualMaxLat),
                    Query.orderDesc("\$createdAt"),
                    Query.limit(200),
                ),
            )
            res.documents.mapNotNull { toDrop(it.id, it.data) }.forEach { knownDrops[it.id] = it }
        }

        filterLocal(knownDrops.values).also {
            areaCache[key] = it to System.currentTimeMillis()
        }
    }

    /** Свежие пины со всей карты — для глобального поиска песен по карте. */
    suspend fun recent(limit: Int = 300): List<MapDrop> = withContext(Dispatchers.IO) {
        recentCache?.takeIf { fresh(it.second) }?.let { return@withContext it.first }
        val res = AppwriteService.databases.listDocuments(
            databaseId = AppwriteService.DATABASE_ID,
            collectionId = AppwriteService.COLLECTION_DROPS,
            queries = listOf(Query.orderDesc("\$createdAt"), Query.limit(limit)),
        )
        res.documents.mapNotNull { toDrop(it.id, it.data) }
            .filterNot { MapModeration.isHidden(it.id) }
            .onEach { knownDrops[it.id] = it }
            .also { recentCache = it to System.currentTimeMillis() }
    }

    /** Пожаловаться на пин: создаёт запись в reports и локально скрывает его. */
    suspend fun report(drop: MapDrop, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = AppwriteService.userId ?: AppwriteService.ensureSession() ?: error("Нет сессии")
            AppwriteService.databases.createDocument(
                databaseId = AppwriteService.DATABASE_ID,
                collectionId = AppwriteService.COLLECTION_REPORTS,
                documentId = ID.unique(),
                data = mapOf(
                    "reporterId" to uid,
                    "targetType" to "drop",
                    "targetId" to drop.id,
                    "reason" to reason,
                    "title" to drop.title,
                    "sourceUrl" to drop.sourceUrl,
                ),
                permissions = listOf(Permission.update(Role.user(uid)), Permission.delete(Role.user(uid))),
            )
            MapModeration.hide(drop.id)
            invalidate()
        }
    }

    /** Удалить свой пин. */
    suspend fun delete(dropId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            AppwriteService.databases.deleteDocument(
                databaseId = AppwriteService.DATABASE_ID,
                collectionId = AppwriteService.COLLECTION_DROPS,
                documentId = dropId,
            )
            MapModeration.hide(dropId)
            invalidate()
        }
    }

    private fun toDrop(id: String, d: Map<String, Any>): MapDrop? = runCatching {
        MapDrop(
            id = id,
            lat = (d["lat"] as Number).toDouble(),
            lng = (d["lng"] as Number).toDouble(),
            title = d["title"] as? String ?: "Трек",
            artist = (d["artist"] as? String)?.ifBlank { null },
            thumbnailUrl = (d["thumbnailUrl"] as? String)?.ifBlank { null },
            sourceUrl = d["sourceUrl"] as? String ?: return@runCatching null,
            source = runCatching { Source.valueOf(d["source"] as? String ?: "YOUTUBE_MUSIC") }
                .getOrDefault(Source.YOUTUBE_MUSIC),
            caption = d["caption"] as? String ?: "",
            ownerId = d["ownerId"] as? String ?: "",
        )
    }.getOrNull()

    /** Оставить пин в точке (lat,lng) с треком и описанием. */
    suspend fun create(
        lat: Double, lng: Double, track: TrackItem, caption: String,
    ): MapDrop = withContext(Dispatchers.IO) {
        val uid = AppwriteService.userId ?: AppwriteService.ensureSession()
            ?: error("Нет сессии")
        val data = mapOf(
            "lat" to lat,
            "lng" to lng,
            "title" to track.title,
            "artist" to (track.uploader ?: ""),
            "thumbnailUrl" to (track.thumbnailUrl ?: ""),
            "sourceUrl" to track.url,
            "source" to track.source.name,
            "caption" to caption,
            "ownerId" to uid,
            "likes" to 0,
        )
        val doc = AppwriteService.databases.createDocument(
            databaseId = AppwriteService.DATABASE_ID,
            collectionId = AppwriteService.COLLECTION_DROPS,
            documentId = ID.unique(),
            data = data,
            permissions = listOf(
                Permission.read(Role.any()),
                Permission.update(Role.user(uid)),
                Permission.delete(Role.user(uid)),
            ),
        )
        MapDrop(
            id = doc.id, lat = lat, lng = lng, title = track.title,
            artist = track.uploader, thumbnailUrl = track.thumbnailUrl,
            sourceUrl = track.url, source = track.source, caption = caption, ownerId = uid,
        ).also { invalidate() }
    }
}
