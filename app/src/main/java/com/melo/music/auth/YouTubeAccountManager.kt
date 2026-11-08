package com.melo.music.auth

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.melo.music.net.MeloNet
import com.melo.music.sync.InnerTubeConfig
import com.melo.music.util.MeloLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Управляет сессией Google / YouTube Music.
 * Хранит авторизационные cookies (SAPISID, __Secure-3PAPISID и др.)
 * и генерирует заголовок SAPISIDHASH для доступа к возрастной музыке и приватным данным.
 */
object YouTubeAccountManager {

    private const val PREFS = "melo_yt_account"
    private const val KEY_COOKIES = "yt_cookies"
    private const val KEY_ACCOUNT_NAME = "yt_account_name"
    private const val KEY_ACCOUNT_EMAIL = "yt_account_email"
    private const val KEY_ACCOUNT_AVATAR = "yt_account_avatar"
    private const val KEY_IS_LOGGED_IN = "yt_is_logged_in"

    private var prefs: SharedPreferences? = null

    var isLoggedIn by mutableStateOf(false)
        private set

    var accountName by mutableStateOf<String?>(null)
        private set

    var accountEmail by mutableStateOf<String?>(null)
        private set

    var accountAvatarUrl by mutableStateOf<String?>(null)
        private set

    @Volatile
    private var cachedCookieString: String? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
        if (cachedCookieString.isNullOrBlank()) {
            cachedCookieString = prefs?.getString(KEY_COOKIES, null)
        }
        isLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
        accountName = prefs?.getString(KEY_ACCOUNT_NAME, null)
        accountEmail = prefs?.getString(KEY_ACCOUNT_EMAIL, null)
        accountAvatarUrl = prefs?.getString(KEY_ACCOUNT_AVATAR, null)
    }

    /**
     * Сохраняет cookies сессии YouTube Music.
     */
    fun saveSession(
        cookies: String,
        name: String? = null,
        email: String? = null,
        avatarUrl: String? = null
    ) {
        cachedCookieString = cookies
        isLoggedIn = true
        accountName = name ?: accountName ?: "Google Аккаунт"
        accountEmail = email ?: accountEmail
        accountAvatarUrl = avatarUrl ?: accountAvatarUrl

        prefs?.edit()
            ?.putString(KEY_COOKIES, cookies)
            ?.putString(KEY_ACCOUNT_NAME, accountName)
            ?.putString(KEY_ACCOUNT_EMAIL, accountEmail)
            ?.putString(KEY_ACCOUNT_AVATAR, accountAvatarUrl)
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.apply()
    }

    fun updateAccountDetails(name: String?, email: String?, avatarUrl: String?) {
        if (!name.isNullOrBlank()) accountName = name
        if (!email.isNullOrBlank()) accountEmail = email
        if (!avatarUrl.isNullOrBlank()) accountAvatarUrl = avatarUrl

        prefs?.edit()
            ?.putString(KEY_ACCOUNT_NAME, accountName)
            ?.putString(KEY_ACCOUNT_EMAIL, accountEmail)
            ?.putString(KEY_ACCOUNT_AVATAR, accountAvatarUrl)
            ?.apply()
    }

    /**
     * Загружает данные пользователя (имя, email, аватарку) через InnerTube account_menu.
     */
    suspend fun fetchAccountDetails(): Boolean = withContext(Dispatchers.IO) {
        val cookies = getCookies() ?: return@withContext false
        val auth = getSapisidHash()
        val apiKey = InnerTubeConfig.getApiKey()
        val clientVersion = InnerTubeConfig.getClientVersion()

        val attempts = listOf(
            Triple(
                "https://music.youtube.com/youtubei/v1/account/account_menu?key=$apiKey&prettyPrint=false",
                "WEB_REMIX",
                "https://music.youtube.com"
            ),
            Triple(
                "https://www.youtube.com/youtubei/v1/account/account_menu?key=$apiKey&prettyPrint=false",
                "WEB",
                "https://www.youtube.com"
            )
        )

        for ((url, clientName, origin) in attempts) {
            try {
                val jsonBody = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", clientName)
                            put("clientVersion", if (clientName == "WEB") "2.20260915.00.00" else clientVersion)
                            put("hl", "ru")
                            put("gl", "RU")
                        })
                    })
                }

                val reqBuilder = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .addHeader("Referer", "$origin/")
                    .addHeader("Origin", origin)
                    .addHeader("X-Origin", origin)
                    .addHeader("Cookie", cookies)

                val specificAuth = getSapisidHash(origin) ?: auth
                if (!specificAuth.isNullOrBlank()) {
                    reqBuilder.addHeader("Authorization", specificAuth)
                }
                if (clientName == "WEB_REMIX") {
                    reqBuilder.addHeader("X-YouTube-Client-Name", "67")
                    reqBuilder.addHeader("X-YouTube-Client-Version", clientVersion)
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .dns(MeloNet.dns)
                    .proxySelector(MeloNet.byedpiSelector)
                    .build()

                client.newCall(reqBuilder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val bodyStr = resp.body?.string() ?: return@use
                    val json = JSONObject(bodyStr)

                    var parsedName: String? = null
                    var parsedEmail: String? = null
                    var parsedAvatar: String? = null

                    // 1. Поиск через activeAccountHeaderRenderer
                    val actions = json.optJSONArray("actions")
                    if (actions != null) {
                        for (i in 0 until actions.length()) {
                            val action = actions.optJSONObject(i)
                            val header = action?.optJSONObject("openPopupAction")
                                ?.optJSONObject("popup")
                                ?.optJSONObject("multiPageMenuRenderer")
                                ?.optJSONObject("header")
                                ?.optJSONObject("activeAccountHeaderRenderer")
                            if (header != null) {
                                parsedName = header.optJSONObject("accountName")?.let {
                                    it.optString("simpleText").takeIf { s -> s.isNotBlank() }
                                        ?: it.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                }
                                parsedEmail = header.optJSONObject("email")?.let {
                                    it.optString("simpleText").takeIf { s -> s.isNotBlank() }
                                        ?: it.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                } ?: header.optJSONObject("channelHandle")?.let {
                                    it.optString("simpleText").takeIf { s -> s.isNotBlank() }
                                        ?: it.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                }
                                val thumbs = header.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
                                if (thumbs != null && thumbs.length() > 0) {
                                    parsedAvatar = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
                                }
                            }
                        }
                    }

                    // 2. Фолбэк на регулярные выражения
                    if (parsedName.isNullOrBlank()) {
                        parsedName = Regex(""""accountName":\s*\{[^}]*"text"\s*:\s*"([^"]+)"""").find(bodyStr)?.groupValues?.get(1)
                            ?: Regex(""""accountName":\s*\{[^}]*"simpleText"\s*:\s*"([^"]+)"""").find(bodyStr)?.groupValues?.get(1)
                    }
                    if (parsedEmail.isNullOrBlank()) {
                        parsedEmail = Regex(""""email":\s*\{[^}]*"text"\s*:\s*"([^"]+)"""").find(bodyStr)?.groupValues?.get(1)
                            ?: Regex(""""email":\s*\{[^}]*"simpleText"\s*:\s*"([^"]+)"""").find(bodyStr)?.groupValues?.get(1)
                            ?: Regex("""([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})""").find(bodyStr)?.groupValues?.get(1)
                    }
                    if (parsedAvatar.isNullOrBlank()) {
                        parsedAvatar = Regex(""""accountPhoto":\s*\{[^}]*"url"\s*:\s*"([^"]+)"""").find(bodyStr)?.groupValues?.get(1)
                            ?: Regex("""(https://lh3\.googleusercontent\.com/[a-zA-Z0-9_\-=]+)""").find(bodyStr)?.groupValues?.get(1)
                    }

                    if (!parsedName.isNullOrBlank() || !parsedEmail.isNullOrBlank() || !parsedAvatar.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            updateAccountDetails(parsedName, parsedEmail, parsedAvatar)
                        }
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                MeloLog.e("YouTubeAccount", "Ошибка загрузки данных аккаунта: ${e.message}")
            }
        }
        false
    }

    /**
     * Возвращает строку cookies для отправки в HTTP-запросах.
     */
    fun getCookies(): String? {
        val stored = cachedCookieString
        if (!stored.isNullOrBlank()) return stored
        val fromPrefs = prefs?.getString(KEY_COOKIES, null)
        if (!fromPrefs.isNullOrBlank()) {
            cachedCookieString = fromPrefs
            return fromPrefs
        }
        return runCatching {
            val cm = CookieManager.getInstance()
            val c1 = cm.getCookie("https://music.youtube.com")
            val c2 = cm.getCookie("https://www.youtube.com")
            val merged = listOfNotNull(c1, c2).flatMap { it.split(";") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.substringBefore("=") }
                .joinToString("; ")
            merged.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /**
     * Экспортирует сохранённые cookies в формате Netscape cookie file
     * для использования в yt-dlp (--cookies).
     */
    fun getNetscapeCookieFile(context: Context): java.io.File? {
        init(context)
        val raw = getCookies()?.takeIf { it.isNotBlank() } ?: return null
        return try {
            val file = java.io.File(context.cacheDir, "yt_cookies.txt")
            if (raw.trimStart().startsWith("# Netscape") || raw.trimStart().startsWith("# HTTP Cookie File")) {
                file.writeText(raw)
                return file
            }

            val sb = java.lang.StringBuilder()
            sb.append("# Netscape HTTP Cookie File\n")
            sb.append("# http://curl.haxx.se/rfc/cookie_spec.html\n")
            sb.append("# This is a generated file! Do not edit.\n\n")

            // .youtube.com покрывает youtube.com, www.youtube.com, music.youtube.com, m.youtube.com
            val pairs = raw.split(";").mapNotNull {
                val trimmed = it.trim()
                if (trimmed.isEmpty() || !trimmed.contains('=')) null
                else {
                    val name = trimmed.substringBefore('=').trim()
                    val value = trimmed.substringAfter('=').trim()
                    if (name.isNotEmpty()) name to value else null
                }
            }.distinctBy { it.first }

            val expires = "2147483647"
            for ((name, value) in pairs) {
                sb.append(".youtube.com\tTRUE\t/\tTRUE\t$expires\t$name\t$value\n")
            }
            // Также добавляем для .google.com куки авторизации сессии (SID, HSID, SSID, APISID, SAPISID)
            val googleAuthCookieNames = setOf(
                "SID", "HSID", "SSID", "APISID", "SAPISID",
                "__Secure-1PSID", "__Secure-3PSID", "__Secure-1PAPISID", "__Secure-3PAPISID"
            )
            for ((name, value) in pairs) {
                if (googleAuthCookieNames.contains(name)) {
                    sb.append(".google.com\tTRUE\t/\tTRUE\t$expires\t$name\t$value\n")
                }
            }
            file.writeText(sb.toString())
            file
        } catch (e: Exception) {
            android.util.Log.e("YouTubeAccount", "Failed to write netscape cookie file: ${e.message}")
            null
        }
    }

    /**
     * Вычисляет заголовок SAPISIDHASH для авторизации в InnerTube API.
     * Формат: SAPISIDHASH <timestamp>_<sha1(timestamp + " " + sapisid + " " + origin)>
     */
    fun getSapisidHash(origin: String = "https://music.youtube.com"): String? {
        val cookies = getCookies() ?: return null
        val sapisid = extractCookieValue(cookies, "SAPISID")
            ?: extractCookieValue(cookies, "__Secure-3PAPISID")
            ?: extractCookieValue(cookies, "__Secure-1PAPISID")
            ?: return null

        val timestamp = System.currentTimeMillis() / 1000
        val payload = "$timestamp $sapisid $origin"
        val sha1 = sha1(payload)
        return "SAPISIDHASH ${timestamp}_${sha1}"
    }

    /**
     * Выход из аккаунта с очисткой сохранённых данных и CookieManager.
     */
    fun logout() {
        cachedCookieString = null
        isLoggedIn = false
        accountName = null
        accountEmail = null
        accountAvatarUrl = null

        prefs?.edit()?.clear()?.apply()

        runCatching {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
        }
    }

    private fun extractCookieValue(cookieHeader: String, cookieName: String): String? {
        val cookies = cookieHeader.split(";")
        for (c in cookies) {
            val trimmed = c.trim()
            if (trimmed.startsWith("$cookieName=")) {
                return trimmed.substringAfter("$cookieName=")
            }
        }
        return null
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
