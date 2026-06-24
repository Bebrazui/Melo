package com.melo.music.extractor

import android.content.Context
import com.melo.music.byedpi.ByeDpiProxy
import com.melo.music.net.MeloNet
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
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

    // Анти-шторм: не дёргать добычу client_id чаще, чем раз в COOLDOWN_MS.
    @Volatile
    private var lastDiscover = 0L
    private const val COOLDOWN_MS = 5 * 60 * 1000L

    private const val PREFS = "melo_sc"
    private const val KEY_ID = "client_id"

    // К SoundCloud — через ByeDPI + честный DNS (DoH), как и все остальные клиенты.
    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .proxySelector(MeloNet.byedpiSelector)
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .build()

    /** Ждёт запуск ByeDPI, иначе первые запросы к SC уходят напрямую в DPI-блок. */
    private fun awaitProxy() {
        if (!ByeDpiProxy.isEnabled()) return
        var waited = 0
        while (!ByeDpiProxy.isRunning() && waited < 24) {
            Thread.sleep(250)
            waited++
        }
        android.util.Log.e("MeloSC", "byedpi running=${ByeDpiProxy.isRunning()} (ждали ${waited * 250}мс)")
    }

    /**
     * Прогрев SC-медиахостов. ByeDPI в auto-режиме (-A) на ПЕРВОМ коннекте к хосту
     * перебирает стратегии (~15с) и кэширует рабочую. Без прогрева этот перебор
     * случается при первом воспроизведении → ExoPlayer ловит "source error".
     * Дёргаем хосты в фоне сразу после старта, чтобы перебор прошёл незаметно.
     */
    fun warmUp() {
        if (!ByeDpiProxy.isEnabled()) return
        awaitProxy()
        val hosts = listOf(
            "https://cf-hls-media.sndcdn.com/",
            "https://cf-media.sndcdn.com/",
            "https://playback.media-streaming.soundcloud.cloud/",
            "https://i1.sndcdn.com/",
        )
        // Параллельно: каждый коннект на холодную может занять до ~15с.
        val threads = hosts.map { h ->
            Thread {
                val r = runCatching {
                    client.newCall(Request.Builder().url(h).header("User-Agent", UA).build())
                        .execute().use { it.code }
                }.getOrElse { it.javaClass.simpleName }
                android.util.Log.e("MeloSC", "warmup $h -> $r")
            }.also { it.start() }
        }
        threads.forEach { runCatching { it.join(20_000) } }
        android.util.Log.e("MeloSC", "warmup done")
    }

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

        // 1) Есть сохранённый id — доверяем ему без сетевой проверки (не штормим).
        //    Если он реально протух, SC-запросы вернут 401 и мы добудем заново (с кулдауном).
        prefs.getString(KEY_ID, null)?.let { saved ->
            cachedId = saved
            inject(saved)
            return saved
        }

        // 2) Нет id — добываем, но не чаще раза в COOLDOWN_MS (анти-шторм).
        val now = System.currentTimeMillis()
        if (now - lastDiscover < COOLDOWN_MS) return null
        lastDiscover = now
        awaitProxy()
        val id = discover()
        if (id != null) {
            android.util.Log.e("MeloSC", "client_id добыт: $id")
            cachedId = id
            prefs.edit().putString(KEY_ID, id).apply()
            inject(id)
            return id
        }
        android.util.Log.e("MeloSC", "не удалось добыть client_id (повтор не раньше чем через 5 мин)")
        return null
    }

    /** Принудительно сбросить кэшированный id (например, при 401 от SoundCloud). */
    @Synchronized
    fun invalidate(context: Context) {
        cachedId = null
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_ID).apply()
    }

    /** Проверка client_id через рабочий api-v2 (200 = валиден). */
    private fun isValid(id: String): Boolean = runCatching {
        val url = "https://api-v2.soundcloud.com/search/tracks?q=test&limit=1&client_id=$id"
        client.newCall(Request.Builder().url(url).build()).execute().use { it.code == 200 }
    }.getOrDefault(false)

    private val HTML_SOURCES = listOf(
        "https://soundcloud.com/",
        "https://soundcloud.com/discover",
        "https://assets.web.soundcloud.cloud/",
        "https://m.soundcloud.com/",
    )

    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    /** Быстрая проверка достижимости ключевых хостов SoundCloud через текущий маршрут. */
    private fun probe(url: String): String = runCatching {
        client.newCall(Request.Builder().url(url).header("User-Agent", UA).build())
            .execute().use { "HTTP ${it.code}" }
    }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }

    /** Проба через ByeDPI: системный DNS (без DoH), HTTP/1.1, свежий клиент. Любой HTTP-код = DPI пробит. */
    private fun probeCode(url: String): String = runCatching {
        OkHttpClient.Builder()
            .callTimeout(8, TimeUnit.SECONDS)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress(ByeDpiProxy.DEFAULT_HOST, ByeDpiProxy.DEFAULT_PORT)))
            .build()
            .newCall(Request.Builder().url(url).header("User-Agent", UA).build())
            .execute().use { it.code.toString() }
    }.getOrElse { it.javaClass.simpleName }

    /**
     * ВРЕМЕННО: перебор стратегий ByeDPI против проблемных хостов SoundCloud.
     * Любой числовой код = хост достижим (DPI пробит). RST/timeout/unreachable = нет.
     * Логи в теге MeloTune. VPN должен быть ВЫКЛЮЧЕН.
     */
    fun tuneHosts() {
        val hosts = listOf("i1.sndcdn.com", "cf-media.sndcdn.com", "a-v2.sndcdn.com", "api-v2.soundcloud.com")
        val bases = listOf("-Kt", "-Kt,h", "-Kth")
        val descs = listOf(
            "-s1+s", "-d1+s", "-o1+s", "-r1+s",
            "-s1+h", "-o1+h", "-r1+h",
            "-s2+s", "-d2+s", "-o2+s", "-r2+s",
            "-o1+s -r1+s", "-s1+s -d1+s", "-o1+s -d1+s", "-r1+s -o1+s",
            "-f1+s -t3", "-f1+s -t5 -o1+s", "-f1+s -t2 -r1+s",
        )
        val strategies = bases.flatMap { b -> descs.map { d -> "$b $d -An" } }
        android.util.Log.e("MeloTune", "=== START host tuning: ${strategies.size} стратегий (VPN OFF!) ===")
        android.util.Log.e("MeloTune", "hosts: i1 / cf-media / a-v2 / api-v2  (число=ok, иначе ошибка)")
        for ((i, args) in strategies.withIndex()) {
            val ok = runCatching { ByeDpiProxy.restart(args) }.getOrDefault(false)
            Thread.sleep(500)
            if (!ok) { android.util.Log.e("MeloTune", "[$i] START FAILED: $args"); continue }
            val res = hosts.joinToString("  ") { probeCode("https://$it/") }
            val hit = res.split("  ").take(2).all { it.toIntOrNull() != null } // i1 и cf-media оба числовые
            android.util.Log.e("MeloTune", "[$i]${if (hit) " ★" else ""} $args -> $res")
        }
        runCatching { ByeDpiProxy.restart(ByeDpiProxy.DEFAULT_CMD) }
        android.util.Log.e("MeloTune", "=== DONE (★ = i1 и cf-media пробились) ===")
    }

    fun diagnose() {
        val mode = when {
            ByeDpiProxy.shouldRoute() -> "ByeDPI"
            ByeDpiProxy.isVpnActive() -> "VPN/direct"
            else -> "direct"
        }
        android.util.Log.e("MeloSC", "=== РЕЖИМ: $mode (vpn=${ByeDpiProxy.isVpnActive()}, byedpi=${ByeDpiProxy.isRunning()}) ===")
        android.util.Log.e("MeloSC", "probe soundcloud.com    = ${probe("https://soundcloud.com/")}")
        android.util.Log.e("MeloSC", "probe api-v2            = ${probe("https://api-v2.soundcloud.com/")}")
        android.util.Log.e("MeloSC", "probe a-v2.sndcdn.com   = ${probe("https://a-v2.sndcdn.com/")}")
        android.util.Log.e("MeloSC", "probe assets.web.sc     = ${probe("https://assets.web.soundcloud.cloud/")}")
    }

    private const val OK_BYTES = 256 * 1024L // успех: реально прокачали ≥256 КБ

    /**
     * Качает последовательность HLS-сегментов (как реальный плеер) и меряет суммарную
     * скорость. Это ловит «volumetric»-троттлинг: одиночный сегмент мелкий (~30 КБ),
     * поэтому смотрим на устойчивую отдачу нескольких сегментов подряд.
     * Префикс "OK"=успех (≥256 КБ за окно), иначе "slow".
     */
    private fun measureSegments(segs: List<String>, seconds: Int): String = runCatching {
        client.connectionPool.evictAll()
        val start = System.currentTimeMillis()
        var total = 0L
        var i = 0
        var lastErr: String? = null
        while (i < segs.size && System.currentTimeMillis() - start < seconds * 1000L) {
            total += runCatching {
                client.newCall(Request.Builder().url(segs[i]).header("User-Agent", UA).build())
                    .execute().use { resp ->
                        if (resp.code !in 200..206) { lastErr = "HTTP ${resp.code}"; return@use 0L }
                        val stream = resp.body!!.byteStream()
                        val buf = ByteArray(32 * 1024)
                        var seg = 0L
                        while (true) { val n = stream.read(buf); if (n < 0) break; seg += n }
                        seg
                    }
            }.getOrElse { lastErr = it.javaClass.simpleName; 0L }
            i++
        }
        val secs = (System.currentTimeMillis() - start) / 1000.0
        val kbps = if (secs > 0) total / 1024.0 / secs else 0.0
        val mark = if (total >= OK_BYTES) "OK" else "slow"
        "%s %.0f kB/s (%d KB, %d сегм%s)".format(mark, kbps, total / 1024, i, lastErr?.let { ", $it" } ?: "")
    }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }

    /** Резолвит живой HLS-плейлист (через коннект-стратегию). Возвращает список URL аудио-сегментов. */
    private fun resolveSegments(cid: String): List<String> {
        runCatching { ByeDpiProxy.restart("-Kt -f1+s -t2 -r1+s") }
        Thread.sleep(800)
        val search = httpGet(client, "https://api-v2.soundcloud.com/search/tracks?q=lofi&limit=10&client_id=$cid") ?: return emptyList()
        val cands = mutableListOf<Pair<String, String>>()
        runCatching {
            val coll = org.json.JSONObject(search).getJSONArray("collection")
            for (i in 0 until coll.length()) {
                val tr = coll.optJSONObject(i) ?: continue
                val trans = tr.optJSONObject("media")?.optJSONArray("transcodings") ?: continue
                for (j in 0 until trans.length()) {
                    val t = trans.getJSONObject(j)
                    val fmt = t.getJSONObject("format")
                    if (fmt.optString("protocol") == "hls" && fmt.optString("mime_type").contains("mpeg")) {
                        cands.add(t.getString("url") to tr.optString("track_authorization"))
                    }
                }
            }
        }
        for ((mu, auth) in cands) {
            val resolved = httpGet(client, "$mu?client_id=$cid&track_authorization=$auth")
            val playlistUrl = runCatching { org.json.JSONObject(resolved!!).getString("url") }.getOrNull() ?: continue
            val m3u8 = httpGet(client, playlistUrl) ?: continue
            val segs = m3u8.lineSequence().filter { it.startsWith("http") }.toList()
            if (segs.isNotEmpty()) return segs
        }
        return emptyList()
    }

    /**
     * Стратегии под SoundCloud от добровольцев (TG-канал bbd). Тестируем по одной
     * в фоне, пока одна не даст реальную скорость на аудио-сегменте. VPN ВЫКЛЮЧИТЬ.
     */
    private fun scStrategies(): List<String> = listOf(
        "-f-1 -T0.5 -Ars -d0+sm -At -r1+s",
        "-f-200 -s2 -s5+hm -t6 -Qr -n wb.ru",
        "-f1+nme -t6 -As -Qr -s1:6+sm -a1 -As -s5:12+sm -a1 -As -d3 -q7 -r6 -Mh",
        "-s1 -q2 -a3 -Y -q1 -s1+s -L1 -o2 -At,r -f-2 -n www.ya.ru",
    )

    /**
     * Тестер: по одной прогоняет SC-стратегии добровольцев и качает живой
     * аудио-сегмент SoundCloud, пока одна не даст реальную скорость. VPN ВЫКЛЮЧИТЬ.
     */
    fun tuneThroughput(context: Context) {
        val tag = "MeloTune"
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cid = cachedId ?: prefs.getString(KEY_ID, null)
        if (cid == null) { android.util.Log.e(tag, "нет client_id — пропуск"); return }

        var segs = resolveSegments(cid)
        if (segs.isEmpty()) { android.util.Log.e(tag, "не удалось резолвить сегменты"); ByeDpiProxy.restart(ByeDpiProxy.DEFAULT_CMD); return }
        val segHost = segs.first().substringAfter("://").substringBefore("/")
        val strategies = scStrategies()
        android.util.Log.e(tag, "=== SC-СТРАТЕГИИ (добровольцы): ${strategies.size}, сегментов=${segs.size}, хост=$segHost (VPN OFF) ===")

        var found: String? = null
        for ((idx, s) in strategies.withIndex()) {
            // Плейлист живёт ~минуты — освежаем периодически.
            if (idx > 0 && idx % 2 == 0) resolveSegments(cid).takeIf { it.isNotEmpty() }?.let { segs = it }
            val ok = runCatching { ByeDpiProxy.restart(s) }.getOrDefault(false)
            Thread.sleep(600)
            val res = if (!ok) "byedpi FAIL" else measureSegments(segs, 12)
            android.util.Log.e(tag, "[$idx] $s -> $res")
            if (res.startsWith("OK") && found == null) {
                found = s
                android.util.Log.e(tag, "★★★ РАБОЧАЯ СТРАТЕГИЯ: $s ($res) ★★★")
            }
        }
        if (found == null) android.util.Log.e(tag, "из ${strategies.size} рабочих не нашлось")
        runCatching { ByeDpiProxy.restart(ByeDpiProxy.DEFAULT_CMD) }
        android.util.Log.e(tag, "=== DONE ===")
    }

    private fun discover(): String? {
        repeat(2) { attempt ->
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
