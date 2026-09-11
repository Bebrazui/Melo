package com.melo.desktop.extractor

import com.melo.desktop.byedpi.ByeDpiManager
import com.melo.desktop.net.DesktopMeloNet
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Загрузчик NewPipe на ПК с поддержкой ByeDPI и переповторов для SoundCloud.
 */
class DesktopOkHttpDownloader(
    private val client: OkHttpClient = DesktopMeloNet.okHttpClient,
) : Downloader() {

    private val scClient: OkHttpClient by lazy {
        client.newBuilder()
            .addInterceptor { chain ->
                val req = chain.request()
                val host = req.url.host
                if (!(host.contains("soundcloud") || host.contains("sndcdn"))) {
                    return@addInterceptor chain.proceed(req)
                }
                val fresh = req.newBuilder().header("Connection", "close").build()
                var last: IOException? = null
                repeat(3) {
                    try {
                        return@addInterceptor chain
                            .withConnectTimeout(4, TimeUnit.SECONDS)
                            .withReadTimeout(6, TimeUnit.SECONDS)
                            .proceed(fresh)
                    } catch (e: IOException) {
                        last = e
                    }
                }
                throw last ?: IOException("SC retry failed")
            }
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

        val activeClient = if (url.contains("soundcloud") || url.contains("sndcdn")) scClient else client
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
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0"
    }
}
