package com.melo.music.profile

import android.content.Context
import android.net.Uri
import com.melo.music.map.AppwriteService
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Публичный профиль пользователя Melo. */
data class MeloProfile(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val bio: String?,
    val isDeveloper: Boolean = false,
    val isVerified: Boolean = false,
)

/**
 * Профили в Appwrite: документ на пользователя (documentId == userId), читаемый всеми,
 * редактируемый только владельцем. Аватарки — в Storage-бакете.
 */
object ProfilesRepository {

    private const val DEFAULT_NAME = "Пользователь Melo"

    // Кэш профилей в памяти: профиль меняется редко, а читается часто (ленты, бейджи,
    // экраны профилей) → не дёргаем сервер по кругу. TTL небольшой, чтобы видеть правки.
    private const val CACHE_TTL_MS = 10 * 60 * 1000L
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<MeloProfile, Long>>()

    private fun cached(userId: String): MeloProfile? =
        cache[userId]?.takeIf { System.currentTimeMillis() - it.second < CACHE_TTL_MS }?.first

    private fun putCache(p: MeloProfile) { cache[p.userId] = p to System.currentTimeMillis() }

    /** Инвалидация (после правки своего профиля). */
    fun invalidate(userId: String) { cache.remove(userId) }

    suspend fun get(userId: String, forceRefresh: Boolean = false): MeloProfile? = withContext(Dispatchers.IO) {
        if (!forceRefresh) cached(userId)?.let { return@withContext it }
        runCatching {
            val doc = AppwriteService.databases.getDocument(
                databaseId = AppwriteService.DATABASE_ID,
                collectionId = AppwriteService.COLLECTION_PROFILES,
                documentId = userId,
            )
            doc.data.toProfile(userId)
        }.getOrNull()?.also { putCache(it) }
    }

    /** Создаёт профиль, только если его ещё нет (не затирает кастомные данные). */
    suspend fun ensureDefault(userId: String, name: String?, avatarUrl: String?) {
        if (get(userId) != null) return
        runCatching { upsert(userId, name?.ifBlank { null } ?: DEFAULT_NAME, avatarUrl, null) }
    }

    suspend fun upsert(userId: String, name: String, avatarUrl: String?, bio: String?): MeloProfile =
        withContext(Dispatchers.IO) {
            val data = mapOf(
                "userId" to userId,
                "name" to name,
                "nameLower" to name.lowercase(),
                "avatarUrl" to (avatarUrl ?: ""),
                "bio" to (bio ?: ""),
            )
            runCatching {
                AppwriteService.databases.updateDocument(
                    databaseId = AppwriteService.DATABASE_ID,
                    collectionId = AppwriteService.COLLECTION_PROFILES,
                    documentId = userId,
                    data = data,
                )
            }.getOrElse {
                AppwriteService.databases.createDocument(
                    databaseId = AppwriteService.DATABASE_ID,
                    collectionId = AppwriteService.COLLECTION_PROFILES,
                    documentId = userId,
                    data = data,
                    permissions = listOf(
                        Permission.read(Role.any()),
                        Permission.update(Role.user(userId)),
                        Permission.delete(Role.user(userId)),
                    ),
                )
            }
            // Бейджи (dev/verified) живут отдельно — сохраняем их из кэша, не затираем.
            val badges = cached(userId)
            MeloProfile(
                userId, name, avatarUrl?.ifBlank { null }, bio?.ifBlank { null },
                isDeveloper = badges?.isDeveloper ?: false,
                isVerified = badges?.isVerified ?: false,
            ).also { putCache(it) }
        }

    suspend fun search(query: String, limit: Int = 20): List<MeloProfile> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        runCatching {
            val res = AppwriteService.databases.listDocuments(
                databaseId = AppwriteService.DATABASE_ID,
                collectionId = AppwriteService.COLLECTION_PROFILES,
                queries = listOf(Query.search("nameLower", q.lowercase()), Query.limit(limit)),
            )
            res.documents.mapNotNull { it.data.toProfile(it.id) }
        }.getOrDefault(emptyList())
    }

    /** Загружает аватарку в Storage и возвращает публичный URL. */
    suspend fun uploadAvatar(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Не удалось прочитать файл")
            val ext = when (context.contentResolver.getType(uri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val file = AppwriteService.storage.createFile(
                bucketId = AppwriteService.BUCKET_AVATARS,
                fileId = ID.unique(),
                file = InputFile.fromBytes(bytes, "avatar.$ext", "image/$ext"),
                permissions = listOf(Permission.read(Role.any())),
            )
            AppwriteService.fileUrl(AppwriteService.BUCKET_AVATARS, file.id)
        }.getOrNull()
    }

    private fun Map<String, Any>.toProfile(userId: String): MeloProfile? = runCatching {
        MeloProfile(
            userId = this["userId"] as? String ?: userId,
            name = (this["name"] as? String)?.ifBlank { null } ?: DEFAULT_NAME,
            avatarUrl = (this["avatarUrl"] as? String)?.ifBlank { null },
            bio = (this["bio"] as? String)?.ifBlank { null },
            isDeveloper = this["isDeveloper"] as? Boolean ?: false,
            isVerified = this["isVerified"] as? Boolean ?: false,
        )
    }.getOrNull()
}
