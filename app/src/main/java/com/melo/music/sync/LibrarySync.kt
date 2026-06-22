package com.melo.music.sync

import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager
import com.melo.music.map.AppwriteService
import com.melo.music.playlists.Playlist
import com.melo.music.playlists.PlaylistManager
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Облачная синхронизация библиотеки (плейлисты + избранное) по аккаунту Appwrite.
 *
 * Правила:
 *  - Синк работает только когда пользователь вошёл в аккаунт (не аноним).
 *  - Локальные треки (импортированные файлы, content://) НЕ уходят на сервер —
 *    они остаются только на устройстве.
 *  - При входе локальные плейлисты/избранное ДОЛИВАЮТСЯ к серверным (объединение),
 *    ничего не теряется.
 */
object LibrarySync {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile var enabled: Boolean = false
        private set

    private var plPush: Job? = null
    private var favPush: Job? = null

    /** Локальный трек (файл) — на сервер не выгружается. */
    private fun isLocal(t: TrackItem): Boolean =
        t.source == Source.LOCAL ||
            t.url.startsWith("content://") ||
            t.url.startsWith("file://")

    /** Вызывается из AuthManager.refresh(): включает/выключает синк. */
    fun onAuthChanged(loggedIn: Boolean) {
        enabled = loggedIn
        if (loggedIn) scope.launch { runCatching { fullSync() } }
    }

    /** Изменились плейлисты локально → отложенно зеркалим на сервер. */
    fun onPlaylistsChanged() {
        if (!enabled) return
        plPush?.cancel()
        plPush = scope.launch {
            delay(1500)
            runCatching { pushPlaylists() }
        }
    }

    /** Изменилось избранное локально → отложенно зеркалим на сервер. */
    fun onFavoritesChanged() {
        if (!enabled) return
        favPush?.cancel()
        favPush = scope.launch {
            delay(1500)
            runCatching { pushFavorites() }
        }
    }

    // ---- Полный синк: тянем сервер, объединяем с локальным, отправляем обратно ----

    private suspend fun fullSync() {
        val uid = AppwriteService.userId ?: return

        // Плейлисты: объединяем по localId.
        val serverPls = listServerPlaylists(uid)
        val localPls = PlaylistManager.getAll()
        val ids = (localPls.map { it.id } + serverPls.map { it.localId }).distinct()
        val mergedPls = ids.map { id ->
            val l = localPls.firstOrNull { it.id == id }
            val s = serverPls.firstOrNull { it.localId == id }
            Playlist(
                id = id,
                name = l?.name ?: s?.name ?: "Плейлист",
                tracks = unionTracks((l?.tracks ?: emptyList()) + (s?.tracks ?: emptyList())),
                isPublic = l?.isPublic ?: s?.isPublic ?: true,
            )
        }
        PlaylistManager.replaceAll(mergedPls)

        // Избранное: объединяем треки.
        val serverFav = getServerFavorites(uid)?.second ?: emptyList()
        val mergedFav = unionTracks(FavoritesManager.getAll() + serverFav)
        FavoritesManager.replaceAll(mergedFav)

        // Отправляем объединённое обратно на сервер (без локальных файлов).
        pushPlaylists()
        pushFavorites()
    }

    // ---- Зеркалирование локального состояния на сервер ----

    private suspend fun pushPlaylists() {
        val uid = AppwriteService.userId ?: return
        val local = PlaylistManager.getAll()
        val server = listServerPlaylists(uid).associateBy { it.localId }

        for (pl in local) {
            val tracksJson = serializeTracks(pl.tracks.filterNot { isLocal(it) })
            val data = mapOf(
                "ownerId" to uid,
                "localId" to pl.id,
                "name" to pl.name,
                "tracks" to tracksJson,
                "isPublic" to pl.isPublic,
            )
            val docId = server[pl.id]?.docId
            if (docId != null) {
                AppwriteService.databases.updateDocument(
                    databaseId = AppwriteService.DATABASE_ID,
                    collectionId = AppwriteService.COLLECTION_PLAYLISTS,
                    documentId = docId,
                    data = data,
                    permissions = perms(uid, pl.isPublic),
                )
            } else {
                AppwriteService.databases.createDocument(
                    databaseId = AppwriteService.DATABASE_ID,
                    collectionId = AppwriteService.COLLECTION_PLAYLISTS,
                    documentId = ID.unique(),
                    data = data,
                    permissions = perms(uid, pl.isPublic),
                )
            }
        }
        // Удаляем на сервере плейлисты, которых больше нет локально.
        val localIds = local.map { it.id }.toSet()
        for ((localId, sp) in server) {
            if (localId !in localIds) {
                runCatching {
                    AppwriteService.databases.deleteDocument(
                        databaseId = AppwriteService.DATABASE_ID,
                        collectionId = AppwriteService.COLLECTION_PLAYLISTS,
                        documentId = sp.docId,
                    )
                }
            }
        }
    }

    private suspend fun pushFavorites() {
        val uid = AppwriteService.userId ?: return
        val pub = FavoritesManager.isPublic()
        val tracksJson = serializeTracks(FavoritesManager.getAll().filterNot { isLocal(it) })
        val data = mapOf("ownerId" to uid, "tracks" to tracksJson, "isPublic" to pub)
        val existing = getServerFavorites(uid)?.first
        if (existing != null) {
            AppwriteService.databases.updateDocument(
                databaseId = AppwriteService.DATABASE_ID,
                collectionId = AppwriteService.COLLECTION_FAVORITES,
                documentId = existing,
                data = data,
                permissions = perms(uid, pub),
            )
        } else {
            AppwriteService.databases.createDocument(
                databaseId = AppwriteService.DATABASE_ID,
                collectionId = AppwriteService.COLLECTION_FAVORITES,
                documentId = ID.unique(),
                data = data,
                permissions = perms(uid, pub),
            )
        }
    }

    // ---- Чтение с сервера ----

    private data class ServerPlaylist(
        val docId: String,
        val localId: String,
        val name: String,
        val tracks: List<TrackItem>,
        val isPublic: Boolean,
    )

    private suspend fun listServerPlaylists(uid: String): List<ServerPlaylist> {
        val res = AppwriteService.databases.listDocuments(
            databaseId = AppwriteService.DATABASE_ID,
            collectionId = AppwriteService.COLLECTION_PLAYLISTS,
            queries = listOf(Query.equal("ownerId", uid), Query.limit(200)),
        )
        return res.documents.mapNotNull { doc ->
            val d = doc.data
            runCatching {
                ServerPlaylist(
                    docId = doc.id,
                    localId = d["localId"] as? String ?: return@runCatching null,
                    name = d["name"] as? String ?: "Плейлист",
                    tracks = parseTracks(d["tracks"] as? String ?: "[]"),
                    isPublic = d["isPublic"] as? Boolean ?: true,
                )
            }.getOrNull()
        }
    }

    /** (docId, треки) документа избранного пользователя или null. */
    private suspend fun getServerFavorites(uid: String): Pair<String, List<TrackItem>>? {
        val res = AppwriteService.databases.listDocuments(
            databaseId = AppwriteService.DATABASE_ID,
            collectionId = AppwriteService.COLLECTION_FAVORITES,
            queries = listOf(Query.equal("ownerId", uid), Query.limit(1)),
        )
        val doc = res.documents.firstOrNull() ?: return null
        return doc.id to parseTracks(doc.data["tracks"] as? String ?: "[]")
    }

    /** Открытым документам — публичное чтение, закрытым — только владелец. */
    private fun perms(uid: String, isPublic: Boolean) = listOf(
        if (isPublic) Permission.read(Role.any()) else Permission.read(Role.user(uid)),
        Permission.update(Role.user(uid)),
        Permission.delete(Role.user(uid)),
    )

    // ---- Объединение / (де)сериализация треков ----

    /** Объединяет треки без дублей (по url + скорости). Первые вхождения в приоритете. */
    private fun unionTracks(tracks: List<TrackItem>): List<TrackItem> {
        val seen = LinkedHashMap<String, TrackItem>()
        for (t in tracks) {
            val key = t.url + "@" + Math.round(t.speed * 100)
            if (!seen.containsKey(key)) seen[key] = t
        }
        return seen.values.toList()
    }

    private fun serializeTracks(tracks: List<TrackItem>): String {
        val arr = JSONArray()
        for (t in tracks) {
            arr.put(
                JSONObject().apply {
                    put("title", t.title)
                    put("uploader", t.uploader ?: "")
                    put("url", t.url)
                    put("duration", t.durationSeconds)
                    put("thumbnail", t.thumbnailUrl ?: "")
                    put("source", t.source.name)
                    put("speed", t.speed.toDouble())
                },
            )
        }
        return arr.toString()
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
