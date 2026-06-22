package com.melo.music.profile

import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.map.AppwriteService
import io.appwrite.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** Публичный плейлист другого пользователя. */
data class RemotePlaylist(
    val id: String,
    val name: String,
    val tracks: List<TrackItem>,
)

/**
 * Чтение публичной библиотеки другого пользователя (открытые плейлисты + лайки).
 * Закрытые документы Appwrite не отдаёт по правам — поэтому видно только открытое.
 */
object PublicLibrary {

    suspend fun favorites(userId: String): List<TrackItem> = withContext(Dispatchers.IO) {
        runCatching {
            val res = AppwriteService.databases.listDocuments(
                databaseId = AppwriteService.DATABASE_ID,
                collectionId = AppwriteService.COLLECTION_FAVORITES,
                queries = listOf(Query.equal("ownerId", userId), Query.limit(1)),
            )
            val doc = res.documents.firstOrNull() ?: return@runCatching emptyList()
            if (doc.data["isPublic"] as? Boolean == false) return@runCatching emptyList()
            parseTracks(doc.data["tracks"] as? String ?: "[]")
        }.getOrDefault(emptyList())
    }

    suspend fun playlists(userId: String): List<RemotePlaylist> = withContext(Dispatchers.IO) {
        runCatching {
            val res = AppwriteService.databases.listDocuments(
                databaseId = AppwriteService.DATABASE_ID,
                collectionId = AppwriteService.COLLECTION_PLAYLISTS,
                queries = listOf(
                    Query.equal("ownerId", userId),
                    Query.equal("isPublic", true),
                    Query.limit(50),
                ),
            )
            res.documents.mapNotNull { doc ->
                runCatching {
                    val tracks = parseTracks(doc.data["tracks"] as? String ?: "[]")
                    if (tracks.isEmpty()) return@runCatching null
                    RemotePlaylist(
                        id = doc.id,
                        name = doc.data["name"] as? String ?: "Плейлист",
                        tracks = tracks,
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    private fun parseTracks(json: String): List<TrackItem> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            TrackItem(
                title = obj.getString("title"),
                uploader = obj.optString("uploader", "").ifBlank { null },
                url = obj.getString("url"),
                durationSeconds = obj.optLong("duration", 0),
                thumbnailUrl = obj.optString("thumbnail", "").ifBlank { null },
                source = runCatching { Source.valueOf(obj.optString("source", "YOUTUBE_MUSIC")) }
                    .getOrDefault(Source.YOUTUBE_MUSIC),
                kind = ItemKind.TRACK,
                speed = obj.optDouble("speed", 1.0).toFloat(),
            )
        }
    }.getOrDefault(emptyList())
}
