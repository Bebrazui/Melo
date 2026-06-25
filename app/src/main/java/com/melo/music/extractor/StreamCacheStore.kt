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
    private const val KEY_VER = "cache_ver"
    // 2: корректный расчёт срока для SoundCloud CloudFront Policy (раньше SC жил 4ч и протухал → 403).
    private const val CACHE_VER = 2
    private const val MAX_ENTRIES = 400

    /** TTL по умолчанию, если в самой ссылке вообще нет срока. Коротко: лучше пере-резолв, чем 403. */
    private val DEFAULT_TTL_MS = TimeUnit.MINUTES.toMillis(20)

    /** Запас: считаем ссылку протухшей чуть раньше реального срока. */
    private val SAFETY_MARGIN_MS = TimeUnit.MINUTES.toMillis(5)

    private val EXPIRE_RE = Regex("[?&](?:expire|Expires)=(\\d{10})")
    // SoundCloud HLS — подписанный CloudFront: срок ВНУТРИ base64-JSON параметра Policy.
    private val POLICY_RE = Regex("[?&]Policy=([A-Za-z0-9_\\-~=]+)")
    private val EPOCH_RE = Regex("EpochTime\"?\\s*:\\s*(\\d{10})")

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        // Миграция: старые записи могли быть сохранены с неверным (слишком долгим) сроком → чистим разом.
        if (p.getInt(KEY_VER, 1) != CACHE_VER) {
            p.edit().remove(KEY).putInt(KEY_VER, CACHE_VER).apply()
        }
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

    /** Срок годности: из параметра ссылки (expire/Expires или CloudFront Policy), иначе now + TTL. */
    private fun expiryOf(audioUrl: String): Long {
        val now = System.currentTimeMillis()
        val floor = now + TimeUnit.MINUTES.toMillis(1)
        // 1) Прямой параметр expire/Expires (YouTube googlevideo, SC progressive).
        EXPIRE_RE.find(audioUrl)?.groupValues?.get(1)?.toLongOrNull()?.let {
            return maxOf(it * 1000L - SAFETY_MARGIN_MS, floor)
        }
        // 2) CloudFront signed Policy (SoundCloud HLS): реальный срок в base64-JSON.
        POLICY_RE.find(audioUrl)?.groupValues?.get(1)?.let { policy ->
            EPOCH_RE.find(decodeCfPolicy(policy))?.groupValues?.get(1)?.toLongOrNull()?.let {
                return maxOf(it * 1000L - SAFETY_MARGIN_MS, floor)
            }
        }
        // 3) Срока в ссылке нет — короткий TTL (длинный TTL отдавал протухшую SC-ссылку → 403).
        return now + DEFAULT_TTL_MS
    }

    /** Декодирует CloudFront base64url (replaced: +→- =→_ /→~) в JSON политики. */
    private fun decodeCfPolicy(p: String): String = runCatching {
        val b64 = p.replace('-', '+').replace('_', '=').replace('~', '/')
        String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT))
    }.getOrDefault("")

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
