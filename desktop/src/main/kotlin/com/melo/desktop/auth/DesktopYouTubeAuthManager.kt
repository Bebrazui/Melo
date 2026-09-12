package com.melo.desktop.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Управляет сессией Google / YouTube Music для Melo Desktop.
 * Хранит авторизационные cookies и генерирует заголовок SAPISIDHASH
 * для доступа к персональной библиотеке, плейлистам и трекам с возрастными ограничениями.
 */
object DesktopYouTubeAuthManager {

    private val sessionFile = File(System.getProperty("user.home"), ".melo/yt_session.json")

    var isLoggedIn by mutableStateOf(false)
        private set

    var accountName by mutableStateOf<String?>(null)
        private set

    var accountEmail by mutableStateOf<String?>(null)
        private set

    var accountAvatarUrl by mutableStateOf<String?>(null)
        private set

    var accountHandle by mutableStateOf<String?>(null)
        private set

    @Volatile
    private var cachedCookieString: String? = null

    init {
        loadSession()
        if (isLoggedIn && (accountAvatarUrl == null || accountName == "YouTube Music")) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                fetchUserProfile()
            }
        }
    }

    private fun loadSession() {
        if (!sessionFile.exists()) return
        runCatching {
            val json = sessionFile.readText()
            val obj = JSONObject(json)
            cachedCookieString = obj.optString("cookies").takeIf { it.isNotBlank() }
            accountName = obj.optString("name").takeIf { it.isNotBlank() }
            accountEmail = obj.optString("email").takeIf { it.isNotBlank() }
            accountAvatarUrl = obj.optString("avatar").takeIf { it.isNotBlank() }
            accountHandle = obj.optString("handle").takeIf { it.isNotBlank() }
            isLoggedIn = !cachedCookieString.isNullOrBlank()
        }
    }

    /**
     * Сохраняет сессионные cookies YouTube Music и профиль пользователя
     */
    fun saveSession(
        cookies: String,
        name: String? = null,
        email: String? = null,
        avatar: String? = null,
        handle: String? = null,
    ) {
        cachedCookieString = cookies.trim()
        isLoggedIn = true
        accountName = name ?: accountName ?: "YouTube Music"
        accountEmail = email ?: accountEmail
        accountAvatarUrl = avatar ?: accountAvatarUrl
        accountHandle = handle ?: accountHandle

        runCatching {
            sessionFile.parentFile?.mkdirs()
            val obj = JSONObject().apply {
                put("cookies", cachedCookieString)
                put("name", accountName.orEmpty())
                put("email", accountEmail.orEmpty())
                put("avatar", accountAvatarUrl.orEmpty())
                put("handle", accountHandle.orEmpty())
            }
            sessionFile.writeText(obj.toString(2))
        }
    }

    fun getCookies(): String? = cachedCookieString

    /**
     * Вычисляет заголовок SAPISIDHASH для авторизации в InnerTube API.
     */
    fun getSapisidHash(origin: String = "https://music.youtube.com"): String? {
        val cookies = cachedCookieString ?: return null
        val sapisid = extractCookieValue(cookies, "SAPISID")
            ?: extractCookieValue(cookies, "__Secure-3PAPISID")
            ?: extractCookieValue(cookies, "__Secure-1PAPISID")
            ?: return null

        val timestamp = System.currentTimeMillis() / 1000
        val payload = "$timestamp $sapisid $origin"
        val sha1 = sha1(payload)
        return "SAPISIDHASH ${timestamp}_${sha1}"
    }

    suspend fun fetchUserProfile(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cookies = cachedCookieString ?: return@withContext false
        val authHash = getSapisidHash("https://www.youtube.com") ?: return@withContext false

        try {
            val key = "AIzaSyAO_FJ2SlqAE4Aq4NoXzvDYqBg55UMNy2w"
            val url = "https://www.youtube.com/youtubei/v1/account/account_menu?key=$key&prettyPrint=false"

            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.01.00")
                        put("hl", "ru")
                        put("gl", "RU")
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val req = okhttp3.Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody(mediaType))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
                .header("Referer", "https://www.youtube.com/")
                .header("Origin", "https://www.youtube.com")
                .header("X-Origin", "https://www.youtube.com")
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", "2.20240101.01.00")
                .header("X-Goog-AuthUser", "0")
                .header("Authorization", authHash)
                .header("Cookie", cookies)
                .build()

            val client = com.melo.desktop.net.DesktopMeloNet.okHttpClient
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val str = resp.body?.string().orEmpty()
                    val json = JSONObject(str)

                    var foundName: String? = null
                    var foundAvatar: String? = null
                    var foundHandle: String? = null

                    fun search(obj: Any) {
                        when (obj) {
                            is JSONObject -> {
                                if (obj.has("activeAccountHeaderRenderer")) {
                                    val h = obj.getJSONObject("activeAccountHeaderRenderer")
                                    foundName = h.optJSONObject("accountName")?.optString("simpleText")
                                    foundHandle = h.optJSONObject("channelHandle")?.optString("simpleText")
                                    val thumbs = h.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
                                    if (thumbs != null && thumbs.length() > 0) {
                                        foundAvatar = thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                                    }
                                } else {
                                    val it = obj.keys()
                                    while (it.hasNext()) {
                                        search(obj.get(it.next()))
                                    }
                                }
                            }
                            is org.json.JSONArray -> {
                                for (i in 0 until obj.length()) {
                                    search(obj.get(i))
                                }
                            }
                        }
                    }

                    search(json)

                    if (!foundName.isNullOrBlank() || !foundAvatar.isNullOrBlank()) {
                        saveSession(
                            cookies = cookies,
                            name = foundName ?: accountName,
                            email = accountEmail,
                            avatar = foundAvatar ?: accountAvatarUrl,
                            handle = foundHandle ?: accountHandle,
                        )
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("[DesktopYouTubeAuth] Error fetching profile: ${e.message}")
        }
        false
    }

    fun logout() {
        cachedCookieString = null
        isLoggedIn = false
        accountName = null
        accountEmail = null
        accountAvatarUrl = null
        accountHandle = null
        sessionFile.delete()
    }

    private fun extractCookieValue(cookieHeader: String, name: String): String? {
        return cookieHeader.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$name=") }
            ?.substringAfter("$name=")
            ?.trim()
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
