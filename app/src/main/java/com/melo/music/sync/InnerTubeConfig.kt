package com.melo.music.sync

import android.content.Context
import android.content.SharedPreferences
import com.melo.music.auth.YouTubeAccountManager
import com.melo.music.net.MeloNet
import com.melo.music.util.MeloLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Динамическое получение актуального INNERTUBE_API_KEY и INNERTUBE_CLIENT_VERSION
 * напрямую с music.youtube.com, чтобы избежать поломки при ротации ключей Google.
 */
object InnerTubeConfig {
    private const val TAG = "InnerTubeConfig"
    private const val PREFS_NAME = "melo_innertube_config"
    private const val KEY_API_KEY = "innertube_api_key"
    private const val KEY_CLIENT_VERSION = "innertube_client_version"
    private const val KEY_LAST_FETCH = "innertube_last_fetch_ms"

    // Актуальные фолбэки на случай отсутствия сети при первом запуске
    const val DEFAULT_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
    const val DEFAULT_CLIENT_VERSION = "1.20260915.14.00"

    // Периодичность обновления из сети (раз в 3 дня)
    private const val REFRESH_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefs: SharedPreferences? = null

    @Volatile
    private var cachedApiKey: String = DEFAULT_API_KEY

    @Volatile
    private var cachedClientVersion: String = DEFAULT_CLIENT_VERSION

    private val cookieJar = object : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = store.getOrPut(url.host) { mutableListOf() }
            synchronized(list) {
                list.removeAll { old -> cookies.any { it.name == old.name } }
                list.addAll(cookies)
            }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return store[url.host] ?: emptyList()
        }
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .dns(MeloNet.dns)
            .proxySelector(MeloNet.byedpiSelector)
            .cookieJar(cookieJar)
            .followRedirects(true)
            .build()
    }

    private val apiKeyRegex = Regex(""""INNERTUBE_API_KEY"\s*:\s*"([a-zA-Z0-9_-]+)"""")
    private val clientVerRegex = Regex(""""INNERTUBE_CLIENT_VERSION"\s*:\s*"([^"]+)"""")

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        cachedApiKey = p.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        cachedClientVersion = p.getString(KEY_CLIENT_VERSION, DEFAULT_CLIENT_VERSION) ?: DEFAULT_CLIENT_VERSION

        val lastFetch = p.getLong(KEY_LAST_FETCH, 0L)
        val now = System.currentTimeMillis()
        if (now - lastFetch > REFRESH_INTERVAL_MS || cachedApiKey == DEFAULT_API_KEY) {
            MeloLog.d(TAG, "Кэш InnerTube устарел или дефолтный, запускаем фоновое обновление...")
            scope.launch {
                refresh(force = false)
            }
        } else {
            MeloLog.d(TAG, "Используем кэш InnerTube: key=${cachedApiKey.take(10)}..., ver=$cachedClientVersion")
        }
    }

    fun getApiKey(): String = cachedApiKey

    fun getClientVersion(): String = cachedClientVersion

    fun getBrowseUrl(): String =
        "https://music.youtube.com/youtubei/v1/browse?key=$cachedApiKey&prettyPrint=false"

    fun triggerRefreshAsync() {
        scope.launch {
            refresh(force = true)
        }
    }

    suspend fun refresh(force: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val p = prefs
        val lastFetch = p?.getLong(KEY_LAST_FETCH, 0L) ?: 0L
        val now = System.currentTimeMillis()
        if (!force && (now - lastFetch < REFRESH_INTERVAL_MS) && cachedApiKey != DEFAULT_API_KEY) {
            return@withContext true
        }

        // 1. Сначала пробуем получить с music.youtube.com
        if (fetchFromUrl("https://music.youtube.com/", now, p)) {
            return@withContext true
        }

        // 2. Фолбэк на www.youtube.com
        MeloLog.d(TAG, "Пробуем запасной URL https://www.youtube.com/...")
        fetchFromUrl("https://www.youtube.com/", now, p)
    }

    private fun fetchFromUrl(url: String, now: Long, p: SharedPreferences?): Boolean {
        MeloLog.d(TAG, "Запрос $url для извлечения актуального InnerTube ключа...")
        val cookieHeader = buildString {
            append("SOCS=CAESEwgDEgk2OTU3NTg0MDcaAmVuIAEaBgiA_LyaBg; CONSENT=YES+1")
            val userCookies = runCatching { YouTubeAccountManager.getCookies() }.getOrNull()
            if (!userCookies.isNullOrBlank()) {
                append("; ")
                append(userCookies)
            }
        }

        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cookie", cookieHeader)
            .build()

        try {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    MeloLog.e(TAG, "Не удалось получить страницу $url: HTTP ${resp.code}")
                    return false
                }
                val html = resp.body?.string() ?: return false
                val keyMatch = apiKeyRegex.find(html)?.groupValues?.getOrNull(1)
                val verMatch = clientVerRegex.find(html)?.groupValues?.getOrNull(1)

                var updated = false
                if (!keyMatch.isNullOrBlank()) {
                    cachedApiKey = keyMatch
                    p?.edit()?.putString(KEY_API_KEY, keyMatch)?.apply()
                    MeloLog.d(TAG, "Успешно извлечён INNERTUBE_API_KEY: $keyMatch")
                    updated = true
                } else {
                    MeloLog.d(TAG, "INNERTUBE_API_KEY не найден регуляркой в $url")
                }

                if (!verMatch.isNullOrBlank()) {
                    cachedClientVersion = verMatch
                    p?.edit()?.putString(KEY_CLIENT_VERSION, verMatch)?.apply()
                    MeloLog.d(TAG, "Успешно извлечён INNERTUBE_CLIENT_VERSION: $verMatch")
                    updated = true
                } else {
                    MeloLog.d(TAG, "INNERTUBE_CLIENT_VERSION не найден регуляркой в $url")
                }

                if (updated) {
                    p?.edit()?.putLong(KEY_LAST_FETCH, now)?.apply()
                    return true
                }
                return false
            }
        } catch (e: Exception) {
            MeloLog.e(TAG, "Ошибка при получении данных с $url: ${e.message}", e)
            return false
        }
    }
}
