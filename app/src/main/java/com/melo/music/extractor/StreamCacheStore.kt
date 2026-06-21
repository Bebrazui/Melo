package com.melo.music.extractor

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Дисковый кэш прямых стрим-URL (переживает перезапуск приложения).
 *
 * Прямые ссылки на аудио протухают: у YouTube (`googlevideo.com`) в URL зашит
 * `expire=<unix-секунды>`, у SoundCloud — подписанный CloudFront с `Expires=`.
 * Поэтому храним ссылку ВМЕСТЕ со сроком годности и отдаём её только пока свежая —
 * иначе протухшая ссылка ломала бы воспроизведение (403).
 *
 * Ключ — URL страницы трека (как в [TrackItem.url]). Значение — прямой аудио-URL,
 * заголовок и момент протухания. Это ускоряет повторное воспроизведение
 * избранного / плейлистов: один раз резолвим, дальше играем мгновенно.
 */
object StreamCacheStore {

    private const val PREFS = "melo_stream_cache"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 400

    /** TTL по умолчанию, если в самой ссылке нет срока (SoundCloud HLS и т.п.). */
    private val DEFAULT_TTL_MS = TimeUnit.HOURS.toMillis(4)

    /** Запас: считаем ссылку протухшей чуть раньше реального срока. */
    private val SAFETY_MARGIN_MS = TimeUnit.MINUTES.toMillis(5)

    private val EXPIRE_RE = Regex("[?&](?:expire|Expires)=(\\d{10})")

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        pruneExpired()
    }

    /** Свежая запись из кэша или null (нет / протухла). */
    @Synchronized
    fun get(pageUrl: String): ResolvedTrack? {
        val root = readRoot() ?: return null
        val obj = root.optJSONObject(pageUrl) ?: return null
        val expiresAt = obj.optLong("exp", 0L)
        if (expiresAt <= System.currentTimeMillis()) return null
        val audioUrl = obj.optString("audio", "").ifBlank { return null }
        return ResolvedTrack(
            title = obj.optString("title", pageUrl),
            audioUrl = audioUrl,
        )
    }

    /** Сохраняет резолв; срок годности вычисляется из самой ссылки. */
    @Synchronized
    fun put(pageUrl: String, resolved: ResolvedTrack) {
        val root = readRoot() ?: JSONObject()
        root.put(
            pageUrl,
            JSONObject().apply {
                put("audio", resolved.audioUrl)
                put("title", resolved.title)
                put("exp", expiryOf(resolved.audioUrl))
            },
        )
        trim(root)
        prefs?.edit()?.putString(KEY, root.toString())?.apply()
    }

    @Synchronized
    fun remove(pageUrl: String) {
        val root = readRoot() ?: return
        if (root.has(pageUrl)) {
            root.remove(pageUrl)
            prefs?.edit()?.putString(KEY, root.toString())?.apply()
        }
    }

    // ── Внутреннее ──────────────────────────────────────────────────────────

    private fun readRoot(): JSONObject? {
        val json = prefs?.getString(KEY, null) ?: return null
        return runCatching { JSONObject(json) }.getOrNull()
    }

    /** Срок годности: из параметра ссылки, иначе now + TTL. */
    private fun expiryOf(audioUrl: String): Long {
        val now = System.currentTimeMillis()
        val parsed = EXPIRE_RE.find(audioUrl)?.groupValues?.get(1)?.toLongOrNull()
        val expMs = if (parsed != null) parsed * 1000L - SAFETY_MARGIN_MS else now + DEFAULT_TTL_MS
        return maxOf(expMs, now + TimeUnit.MINUTES.toMillis(1))
    }

    private fun pruneExpired() {
        val root = readRoot() ?: return
        val now = System.currentTimeMillis()
        val dead = root.keys().asSequence().filter { root.optJSONObject(it)?.optLong("exp", 0L)
            ?.let { e -> e <= now } ?: true }.toList()
        if (dead.isEmpty()) return
        dead.forEach { root.remove(it) }
        prefs?.edit()?.putString(KEY, root.toString())?.apply()
    }

    /** Ограничивает размер: при переполнении выкидываем записи с ближайшим протуханием. */
    private fun trim(root: JSONObject) {
        if (root.length() <= MAX_ENTRIES) return
        val byExpiry = root.keys().asSequence()
            .map { it to (root.optJSONObject(it)?.optLong("exp", 0L) ?: 0L) }
            .sortedBy { it.second }
            .toList()
        val toRemove = byExpiry.take(root.length() - MAX_ENTRIES)
        toRemove.forEach { root.remove(it.first) }
    }
}
