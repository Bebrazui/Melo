package com.melo.music.auth

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest

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
    private const val KEY_IS_LOGGED_IN = "yt_is_logged_in"

    private var prefs: SharedPreferences? = null

    var isLoggedIn by mutableStateOf(false)
        private set

    var accountName by mutableStateOf<String?>(null)
        private set

    var accountEmail by mutableStateOf<String?>(null)
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
    }

    /**
     * Сохраняет cookies сессии YouTube Music.
     */
    fun saveSession(cookies: String, name: String? = null, email: String? = null) {
        cachedCookieString = cookies
        isLoggedIn = true
        accountName = name ?: accountName ?: "Google Аккаунт"
        accountEmail = email ?: accountEmail

        prefs?.edit()
            ?.putString(KEY_COOKIES, cookies)
            ?.putString(KEY_ACCOUNT_NAME, accountName)
            ?.putString(KEY_ACCOUNT_EMAIL, accountEmail)
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.apply()
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
