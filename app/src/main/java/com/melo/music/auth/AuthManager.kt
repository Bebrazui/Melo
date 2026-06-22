package com.melo.music.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.melo.music.map.AppwriteService
import com.melo.music.profile.ProfilesRepository
import io.appwrite.ID
import io.appwrite.services.Account
import io.appwrite.services.Functions
import org.json.JSONObject

/**
 * Аккаунты через Appwrite Auth: email/пароль, Google OAuth, анонимный («локально»).
 * Состояние реактивное для Compose.
 */
object AuthManager {

    const val GOOGLE_WEB_CLIENT_ID = "133460769451-6tlvgcim6h3f67l57kes4pfgkh37v8ms.apps.googleusercontent.com"
    private const val GOOGLE_FUNCTION_ID = "6a390c96003193681d8d"

    var email by mutableStateOf<String?>(null)
        private set
    var name by mutableStateOf<String?>(null)
        private set
    var avatarUrl by mutableStateOf<String?>(null)
        private set
    var bio by mutableStateOf<String?>(null)
        private set
    var isDeveloper by mutableStateOf(false)
        private set
    var isVerified by mutableStateOf(false)
        private set

    /** Вошёл в полноценный аккаунт (а не анонимно). */
    val loggedIn: Boolean get() = !email.isNullOrBlank()

    val userId: String? get() = AppwriteService.userId

    private fun account() = Account(AppwriteService.client)

    private suspend fun loadProfile(uid: String) {
        val p = ProfilesRepository.get(uid)
        avatarUrl = p?.avatarUrl
        bio = p?.bio
        isDeveloper = p?.isDeveloper ?: false
        isVerified = p?.isVerified ?: false
        if (!p?.name.isNullOrBlank()) name = p?.name
    }

    /** Сохранить профиль (имя, аватар, описание) текущего пользователя. */
    suspend fun saveProfile(newName: String, newAvatarUrl: String?, newBio: String?): Result<Unit> = runCatching {
        val uid = AppwriteService.userId ?: error("Нет сессии")
        ProfilesRepository.upsert(uid, newName.trim().ifBlank { "Пользователь Melo" }, newAvatarUrl, newBio?.trim())
        name = newName.trim().ifBlank { "Пользователь Melo" }
        avatarUrl = newAvatarUrl
        bio = newBio?.trim()?.ifBlank { null }
    }

    /** Удаляет текущую (обычно анонимную) сессию — иначе новый вход запрещён. */
    private suspend fun clearSession() {
        runCatching { account().deleteSession("current") }
    }

    /** Подтянуть текущего пользователя (у анонима email пустой). */
    suspend fun refresh() {
        val u = runCatching { account().get() }.getOrNull()
        email = u?.email?.ifBlank { null }
        name = u?.name?.ifBlank { null }
        avatarUrl = null
        bio = null
        AppwriteService.setUserId(u?.id)
        // Профиль (аватар/описание) — только для вошедших в аккаунт.
        if (loggedIn && u != null) {
            runCatching { ProfilesRepository.ensureDefault(u.id, u.name, null) }
            runCatching { loadProfile(u.id) }
        }
        // Включаем/выключаем облачную синхронизацию библиотеки по статусу входа.
        com.melo.music.sync.LibrarySync.onAuthChanged(loggedIn)
    }

    /**
     * Шаг 1 регистрации: создаёт аккаунт и отправляет 6-значный код на почту
     * (Appwrite Email OTP). Возвращает userId для подтверждения кода.
     */
    suspend fun startEmailRegister(email: String, password: String, name: String): Result<String> = runCatching {
        clearSession()
        account().create(ID.unique(), email.trim(), password, name.trim().ifBlank { null })
        val token = account().createEmailToken(ID.unique(), email.trim())
        token.userId
    }

    /** Шаг 2 регистрации: подтверждает код из письма и входит. */
    suspend fun confirmEmailCode(userId: String, code: String): Result<Unit> = runCatching {
        clearSession()
        account().createSession(userId, code.trim())
        refresh()
    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        clearSession()
        account().createEmailPasswordSession(email.trim(), password)
        refresh()
    }

    /**
     * Меняет Google idToken (получен классическим Google Sign-In в активности)
     * на сессию Appwrite через нашу функцию (Custom Token). Без браузера.
     */
    suspend fun loginWithGoogleIdToken(
        idToken: String,
        photoUrl: String? = null,
        displayName: String? = null,
    ): Result<Unit> = runCatching {
        android.util.Log.e("MeloAuth", "got idToken len=${idToken.length}, calling function $GOOGLE_FUNCTION_ID")
        val functions = Functions(AppwriteService.client)
        val exec = functions.createExecution(
            functionId = GOOGLE_FUNCTION_ID,
            body = JSONObject().put("idToken", idToken).toString(),
        )
        android.util.Log.e(
            "MeloAuth",
            "exec status=${exec.status} code=${exec.responseStatusCode} " +
                "bodyLen=${exec.responseBody.length} errors=${exec.errors.take(300)} body=${exec.responseBody.take(300)}",
        )
        val resp = JSONObject(exec.responseBody.ifBlank { "{}" })
        if (!resp.optBoolean("ok")) error(resp.optString("error", "Ошибка функции (status=${exec.status})"))
        clearSession()
        account().createSession(resp.getString("userId"), resp.getString("secret"))
        refresh()
        // Подтягиваем аватар и имя из Google, если профиля ещё нет.
        AppwriteService.userId?.let { uid ->
            runCatching { ProfilesRepository.ensureDefault(uid, displayName, photoUrl) }
            runCatching { loadProfile(uid) }
        }
        android.util.Log.e("MeloAuth", "google login OK, email=$email")
    }

    /** Выход → возвращаемся в анонимную сессию (карта и т.п. продолжают работать). */
    suspend fun logout(): Result<Unit> = runCatching {
        runCatching { account().deleteSession("current") }
        AppwriteService.ensureSession()
        refresh()
    }
}
