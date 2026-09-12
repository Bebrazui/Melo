package com.melo.desktop.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    @Volatile
    private var cachedCookieString: String? = null

    init {
        loadSession()
    }

    private fun loadSession() {
        if (!sessionFile.exists()) return
        runCatching {
            val json = sessionFile.readText()
            val obj = JSONObject(json)
            cachedCookieString = obj.optString("cookies").takeIf { it.isNotBlank() }
            accountName = obj.optString("name").takeIf { it.isNotBlank() }
            accountEmail = obj.optString("email").takeIf { it.isNotBlank() }
            isLoggedIn = !cachedCookieString.isNullOrBlank()
        }
    }

    /**
     * Сохраняет сессионные cookies YouTube Music
     */
    fun saveSession(cookies: String, name: String? = null, email: String? = null) {
        cachedCookieString = cookies.trim()
        isLoggedIn = true
        accountName = name ?: accountName ?: "YouTube Music Аккаунт"
        accountEmail = email ?: accountEmail

        runCatching {
            sessionFile.parentFile?.mkdirs()
            val obj = JSONObject().apply {
                put("cookies", cachedCookieString)
                put("name", accountName.orEmpty())
                put("email", accountEmail.orEmpty())
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

    fun logout() {
        cachedCookieString = null
        isLoggedIn = false
        accountName = null
        accountEmail = null
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
