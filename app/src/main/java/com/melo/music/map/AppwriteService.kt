package com.melo.music.map

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage

/**
 * Подключение к Appwrite Cloud (бэкенд «Карты музыки»).
 * Хранит только координаты, описание и ссылку на трек — без самой музыки.
 */
object AppwriteService {

    const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"
    const val PROJECT_ID = "6a38f49f0024c6b9b473"
    // ВАЖНО: реальные ID из Appwrite (не отображаемые имена).
    const val DATABASE_ID = "6a38fc430015b7804515"
    const val COLLECTION_DROPS = "6a38fc75002e786fae6c"
    // Облачная библиотека (синк плейлистов/избранного по аккаунту). Кастомные ID.
    const val COLLECTION_PLAYLISTS = "playlists"
    const val COLLECTION_FAVORITES = "favorites"
    // Публичные профили пользователей + хранилище аватарок.
    const val COLLECTION_PROFILES = "profiles"
    const val BUCKET_AVATARS = "avatars"

    /** Публичный URL файла из хранилища (если у файла есть read("any")). */
    fun fileUrl(bucketId: String, fileId: String): String =
        "$ENDPOINT/storage/buckets/$bucketId/files/$fileId/view?project=$PROJECT_ID"

    @Volatile private var clientRef: Client? = null
    @Volatile var userId: String? = null
        private set

    fun setUserId(id: String?) { userId = id }

    val client: Client get() = clientRef!!
    val databases: Databases get() = Databases(client)
    val storage: Storage get() = Storage(client)

    fun init(context: Context) {
        if (clientRef != null) return
        clientRef = Client(context.applicationContext)
            .setEndpoint(ENDPOINT)
            .setProject(PROJECT_ID)
    }

    /** Анонимная сессия (без регистрации). Возвращает userId или null. */
    suspend fun ensureSession(): String? {
        val account = Account(client)
        userId = runCatching { account.get().id }.getOrNull()
        if (userId == null) {
            runCatching { account.createAnonymousSession() }
            userId = runCatching { account.get().id }.getOrNull()
        }
        return userId
    }
}
