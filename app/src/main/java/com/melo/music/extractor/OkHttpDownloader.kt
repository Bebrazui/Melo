package com.melo.music.extractor

import com.melo.music.byedpi.ByeDpiProxy
import com.melo.music.net.MeloNet
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
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
            .proxySelector(MeloNet.byedpiSelector)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .addInterceptor(ScRetryInterceptor)
            .build()
    }

    /**
     * Через ByeDPI отдельные коннекты к SoundCloud иногда «немеют» — TLS встаёт,
     * запрос уходит, ответа нет → read timeout ~10с убивает резолв (а соседние
     * запросы летят за <1с). Для SC-хостов: короткий таймаут (4с) + до 3 попыток;
     * каждый повтор берёт СВЕЖИЙ коннект, который обычно отвечает мгновенно.
     */
    private object ScRetryInterceptor : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val req = chain.request()
            val host = req.url.host
            if (!(host.contains("soundcloud") || host.contains("sndcdn"))) {
                return chain.proceed(req)
            }
            // Connection: close — не оставляем коннект в пуле. Переиспользованные
            // keep-alive соединения через ByeDPI «немеют»; свежее рукопожатие ByeDPI
            // обходит надёжно. Каждый запрос = свежий коннект.
            val fresh = req.newBuilder().header("Connection", "close").build()
            var last: java.io.IOException? = null
            repeat(3) {
                try {
                    return chain
                        .withConnectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                        .withReadTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                        .proceed(fresh)
                } catch (e: java.io.IOException) {
                    last = e
                }
            }
            throw last ?: java.io.IOException("SC retry failed")
        }
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

        val host = runCatching { URI(url).host }.getOrNull() ?: url
        val sc = host.contains("soundcloud") || host.contains("sndcdn")
        val proxied = ByeDpiProxy.shouldRoute()
        val started = System.currentTimeMillis()
        val response = try {
            activeClient.newCall(requestBuilder.build()).execute()
        } catch (e: Exception) {
            if (sc || LOG_ALL) {
                android.util.Log.e("MeloNet", "$httpMethod $host FAILED proxy=$proxied: ${e.javaClass.simpleName}: ${e.message}")
            }
            throw e
        }
        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val body = response.body?.string()
        val ms = System.currentTimeMillis() - started
        if (sc || LOG_ALL) {
            android.util.Log.e("MeloNet", "$httpMethod $host -> ${response.code} ${body?.length ?: 0}b ${ms}ms proxy=$proxied")
        }
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
        // Поставь true, чтобы видеть ВСЕ запросы NewPipe (не только SoundCloud).
        const val LOG_ALL = false
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101 Firefox/91.0"
    }
}
