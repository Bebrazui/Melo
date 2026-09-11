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
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        cachedCookieString = prefs?.getString(KEY_COOKIES, null)
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
    fun getCookies(): String? = cachedCookieString

    /**
     * Вычисляет заголовок SAPISIDHASH для авторизации в InnerTube API.
     * Формат: SAPISIDHASH <timestamp>_<sha1(timestamp + " " + sapisid + " " + origin)>
     */
    fun getSapisidHash(origin: String = "https://music.youtube.com"): String? {
        val cookies = cachedCookieString ?: return null
        val sapisid = extractCookieValue(cookies, "SAPISID")
            ?: extractCookieValue(cookies, "__Secure-3PAPISID")
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
