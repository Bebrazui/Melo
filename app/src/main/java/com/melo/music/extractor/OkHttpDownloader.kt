package com.melo.music.extractor

import com.melo.music.byedpi.ByeDpiProxy
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI

/**
 * Реализация загрузчика NewPipe поверх OkHttp.
 * Динамически проверяет ByeDPI прокси на каждый запрос через ProxySelector.
 */
class OkHttpDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder().build(),
) : Downloader() {

    private val dynamicProxyClient: OkHttpClient by lazy {
        client.newBuilder()
            .proxySelector(object : ProxySelector() {
                override fun select(uri: URI): List<Proxy> {
                    return if (ByeDpiProxy.isEnabled() && ByeDpiProxy.isRunning()) {
                        listOf(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", ByeDpiProxy.DEFAULT_PORT)))
                    } else {
                        listOf(Proxy.NO_PROXY)
                    }
                }
                override fun connectFailed(uri: URI, sa: java.net.SocketAddress, ioe: java.io.IOException) {}
            })
            .build()
    }

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = dataToSend?.toRequestBody(null, 0, dataToSend.size)

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        headers.forEach { (name, values) ->
            when {
                values.size > 1 -> {
                    requestBuilder.removeHeader(name)
                    values.forEach { requestBuilder.addHeader(name, it) }
                }
                values.size == 1 -> requestBuilder.header(name, values[0])
            }
        }

        val activeClient = if (ByeDpiProxy.isEnabled()) {
            dynamicProxyClient
        } else {
            client
        }

        val response = activeClient.newCall(requestBuilder.build()).execute()
        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val body = response.body?.string()
        val latestUrl = response.request.url.toString()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            body,
            latestUrl,
        )
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101 Firefox/91.0"
    }
}
