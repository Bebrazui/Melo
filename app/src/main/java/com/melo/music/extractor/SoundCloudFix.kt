package com.melo.music.extractor

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Добывает SoundCloud client_id, НЕ обращаясь к заблокированному soundcloud.com.
 *
 * На сети пользователя (DPI-обход byebyedpi) надёжно работают `api-v2.soundcloud.com`
 * и `a-v2.sndcdn.com`, а `m.soundcloud.com`/`soundcloud.com` проходят нестабильно.
 * Поэтому:
 *  1. Если есть сохранённый client_id — проверяем его через api-v2 и используем.
 *  2. Иначе с ретраями тащим HTML c m.soundcloud.com → ссылки на бандлы a-v2.sndcdn.com
 *     → регулярка client_id. Найденный id СОХРАНЯЕМ навсегда (до протухания).
 *  3. Внедряем id в NewPipe рефлексией → поиск/резолв идут через api-v2 (рабочий).
 */
object SoundCloudFix {

    @Volatile
    private var cachedId: String? = null

    private const val PREFS = "melo_sc"
    private const val KEY_ID = "client_id"

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val ID_RE = Regex("client_id\\s*[:=]\\s*\"([0-9a-zA-Z]{20,40})\"")
    private val ASSET_RE = Regex("https://[a-z0-9\\-]+\\.sndcdn\\.com/assets/[^\"'<> )]+\\.js")

    /** Текущий client_id (из памяти или сохранённый) — для отображения статуса. */
    fun currentId(context: Context): String? =
        cachedId ?: context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ID, null)

    /** Ручная установка client_id: проверяем через api-v2, сохраняем, внедряем. */
    @Synchronized
    fun setManual(context: Context, id: String): Boolean {
        val clean = id.trim()
        if (!isValid(clean)) return false
        cachedId = clean
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ID, clean).apply()
        inject(clean)
        return true
    }

    /** Сброс и повторная авто-добыча. */
    @Synchronized
    fun refresh(context: Context): String? {
        cachedId = null
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_ID).apply()
        return ensure(context)
    }

    /** Гарантирует валидный client_id и внедряет его в NewPipe. Идемпотентно. */
    @Synchronized
    fun ensure(context: Context): String? {
        cachedId?.let { return it }
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // 1) Сохранённый id — проверяем и используем.
        prefs.getString(KEY_ID, null)?.let { saved ->
            if (isValid(saved)) {
                android.util.Log.e("MeloSC", "сохранённый client_id валиден")
                cachedId = saved
                inject(saved)
                return saved
            }
            android.util.Log.e("MeloSC", "сохранённый client_id протух — добываем заново")
        }

        // 2) Добываем заново.
        val id = discover()
        if (id != null && isValid(id)) {
            android.util.Log.e("MeloSC", "client_id добыт и валиден: $id")
            cachedId = id
            prefs.edit().putString(KEY_ID, id).apply()
            inject(id)
            return id
        }
        android.util.Log.e("MeloSC", "не удалось добыть валидный client_id")
        return null
    }

    /** Проверка client_id через рабочий api-v2 (200 = валиден). */
    private fun isValid(id: String): Boolean = runCatching {
        val url = "https://api-v2.soundcloud.com/search/tracks?q=test&limit=1&client_id=$id"
        client.newCall(Request.Builder().url(url).build()).execute().use { it.code == 200 }
    }.getOrDefault(false)

    private val HTML_SOURCES = listOf(
        "https://assets.web.soundcloud.cloud/",
        "https://m.soundcloud.com/",
    )

    private fun discover(): String? {
        repeat(8) { attempt ->
            for (src in HTML_SOURCES) {
                val html = httpGet(client, src) ?: continue
                android.util.Log.e("MeloSC", "попытка $attempt: $src → HTML ${html.length} символов")

                ID_RE.find(html)?.let {
                    android.util.Log.e("MeloSC", "client_id прямо в HTML $src")
                    return it.groupValues[1]
                }
                val assets = ASSET_RE.findAll(html).map { it.value }.distinct().toList()
                android.util.Log.e("MeloSC", "$src → бандлов: ${assets.size}")
                for (asset in assets.reversed()) {
                    val js = httpGet(client, asset) ?: continue
                    ID_RE.find(js)?.let {
                        android.util.Log.e("MeloSC", "client_id из $asset")
                        return it.groupValues[1]
                    }
                }
            }
        }
        return null
    }

    private fun httpGet(c: OkHttpClient, url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0 Safari/537.36",
            )
            .build()
        c.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) {
                resp.body?.string()
            } else {
                android.util.Log.e("MeloSC", "GET $url → HTTP ${resp.code}")
                null
            }
        }
    }.onFailure { android.util.Log.e("MeloSC", "GET $url → ${it.javaClass.simpleName}: ${it.message}") }
        .getOrNull()

    private fun inject(id: String) {
        runCatching {
            val cls = Class.forName(
                "org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper",
            )
            val field = cls.getDeclaredField("clientId")
            field.isAccessible = true
            field.set(null, id)
        }.onFailure { android.util.Log.e("MeloSC", "inject failed: $it") }
    }
}
