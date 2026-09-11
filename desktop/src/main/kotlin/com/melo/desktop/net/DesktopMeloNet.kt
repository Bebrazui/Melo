package com.melo.desktop.net

import com.melo.desktop.byedpi.ByeDpiManager
import com.melo.desktop.storage.DesktopStorage
import com.melo.desktop.storage.DpiEngine
import com.melo.desktop.zapret.ZapretManager
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Сетевой слой Melo Desktop:
 * Умная маршрутизация Zapret (WinDivert) / SOCKS5 ByeDPI + DoH (Cloudflare 1.1.1.1).
 */
object DesktopMeloNet {

    @Volatile
    private var isZapretActive: Boolean = false
    @Volatile
    private var isByeDpiActive: Boolean = false

    init {
        val daemon = Thread({
            while (true) {
                try {
                    isZapretActive = ZapretManager.isRunning()
                    isByeDpiActive = ByeDpiManager.isEnabled && ByeDpiManager.isRunning()
                    Thread.sleep(2500)
                } catch (_: InterruptedException) {
                    break
                } catch (_: Exception) {
                    try { Thread.sleep(2500) } catch (_: InterruptedException) { break }
                }
            }
        }, "Melo-NetStateWatcher")
        daemon.isDaemon = true
        daemon.start()
    }

    val byedpiSelector: ProxySelector = object : ProxySelector() {
        override fun select(uri: URI): List<Proxy> {
            val engine = DesktopStorage.dpiEngine.value
            return when (engine) {
                DpiEngine.DISABLED -> listOf(Proxy.NO_PROXY)
                DpiEngine.ZAPRET -> listOf(Proxy.NO_PROXY)
                DpiEngine.BYEDPI -> {
                    if (isByeDpiActive) {
                        listOf(
                            Proxy(Proxy.Type.SOCKS, InetSocketAddress(ByeDpiManager.DEFAULT_HOST, ByeDpiManager.DEFAULT_PORT)),
                            Proxy.NO_PROXY,
                        )
                    } else {
                        listOf(Proxy.NO_PROXY)
                    }
                }
                DpiEngine.AUTO -> {
                    if (isZapretActive) {
                        listOf(Proxy.NO_PROXY)
                    } else if (isByeDpiActive) {
                        listOf(
                            Proxy(Proxy.Type.SOCKS, InetSocketAddress(ByeDpiManager.DEFAULT_HOST, ByeDpiManager.DEFAULT_PORT)),
                            Proxy.NO_PROXY,
                        )
                    } else {
                        listOf(Proxy.NO_PROXY)
                    }
                }
            }
        }

        override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) {
            // Фолбэк на прямой доступ при ошибке прокси
        }
    }

    private val bootstrapClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(15, TimeUnit.SECONDS)
            .proxySelector(byedpiSelector)
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .build()
    }

    /** DNS-over-HTTPS через 1.1.1.1 без утечек системного DNS. */
    val doh: Dns by lazy {
        try {
            DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url("https://1.1.1.1/dns-query".toHttpUrl())
                .bootstrapDnsHosts(InetAddress.getByName("1.1.1.1"), InetAddress.getByName("1.0.0.1"))
                .build()
        } catch (_: Exception) {
            Dns.SYSTEM
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 8
        }
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .proxySelector(byedpiSelector)
            .dns(doh)
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
