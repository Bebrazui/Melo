package com.melo.music.net

import com.melo.music.byedpi.ByeDpiProxy
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Общий сетевой слой: маршрутизация через ByeDPI + честный DNS (DoH).
 *
 * Системный DNS в РФ подменяет IP заблокированных доменов → ByeDPI коннектится
 * на «мёртвый» адрес. DoH (Cloudflare по IP, без палевного SNI) отдаёт реальные IP,
 * а ByeDPI уже применяет десинк по SNI. Используется ВЕЗДЕ: обложки, резолв, плеер.
 */
object MeloNet {

    /** Прокси-селектор ByeDPI (отходит в сторону при активном VPN). */
    val byedpiSelector: ProxySelector = object : ProxySelector() {
        override fun select(uri: URI): List<Proxy> {
            return if (ByeDpiProxy.shouldRoute()) {
                listOf(
                    Proxy(Proxy.Type.SOCKS, InetSocketAddress(ByeDpiProxy.DEFAULT_HOST, ByeDpiProxy.DEFAULT_PORT)),
                )
            } else {
                listOf(Proxy.NO_PROXY)
            }
        }
        override fun connectFailed(uri: URI, sa: java.net.SocketAddress, ioe: java.io.IOException) {
            if (sa is InetSocketAddress && sa.port == ByeDpiProxy.DEFAULT_PORT) {
                ByeDpiProxy.ensureRunning()
            }
        }
    }

    // Бутстрап для DoH: прямой коннект без проксирования и без задержек.
    private val bootstrap: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(3, TimeUnit.SECONDS)
            .connectTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    /** DNS-over-HTTPS (Cloudflare 1.1.1.1 по IP). Кэширует ответы. */
    val doh: Dns by lazy {
        DnsOverHttps.Builder()
            .client(bootstrap)
            .url("https://1.1.1.1/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
                InetAddress.getByName("8.8.8.8")
            )
            .build()
    }

    /**
     * Безопасный резолвер: для заблокированных сервисов (SoundCloud, YouTube, Bandcamp)
     * сразу опрашивает DoH (Cloudflare 1.1.1.1), чтобы избежать подмены IP системным DNS провайдера в РФ.
     * Для остальных хостов сначала пробует быстрый системный DNS.
     */
    val dns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val isBlocked = hostname.contains("sndcdn") ||
                hostname.contains("soundcloud") ||
                hostname.contains("youtube") ||
                hostname.contains("googlevideo") ||
                hostname.contains("ytimg") ||
                hostname.contains("ggpht") ||
                hostname.contains("bcbits") ||
                hostname.contains("bandcamp") ||
                hostname.contains("discord")

            if (isBlocked) {
                return try {
                    doh.lookup(hostname)
                } catch (_: Exception) {
                    Dns.SYSTEM.lookup(hostname)
                }
            }

            return try {
                Dns.SYSTEM.lookup(hostname)
            } catch (_: Exception) {
                try {
                    doh.lookup(hostname)
                } catch (e: Exception) {
                    throw e
                }
            }
        }
    }
}
