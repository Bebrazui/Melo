package com.melo.desktop.auth

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI

/**
 * Сервер авторизации YouTube Music через системный браузер по умолчанию.
 * Открывает в дефолтном браузере специальную страницу авторизации,
 * которая берет сессию из music.youtube.com и отправляет токен/cookie в Melo.
 */
object BrowserYouTubeAuthHelper {

    private var server: HttpServer? = null

    fun openBrowserLogin(
        onSuccess: () -> Unit,
    ) {
        // Останавливаем предыдущий сервер, если был открыт
        stopServer()

        try {
            val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            val port = s.address.port
            server = s

            s.createContext("/callback", CallbackHandler(onSuccess))
            s.createContext("/login", LoginLandingHandler(port))
            s.executor = null
            s.start()

            val loginUrl = "http://127.0.0.1:$port/login"
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(loginUrl))
            }
        } catch (e: Exception) {
            System.err.println("[BrowserYouTubeAuthHelper] Error starting local server: ${e.message}")
            // Fallback: просто открываем music.youtube.com
            runCatching {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI("https://music.youtube.com"))
                }
            }
        }
    }

    private fun stopServer() {
        try {
            server?.stop(0)
            server = null
        } catch (_: Exception) {}
    }

    private class LoginLandingHandler(private val port: Int) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val html = """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                    <meta charset="utf-8">
                    <title>Вход в YouTube Music для Melo</title>
                    <style>
                        body {
                            background-color: #0b0f0c;
                            color: #ffffff;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            height: 100vh;
                            margin: 0;
                        }
                        .card {
                            background: #161e19;
                            border: 1px solid rgba(255, 255, 255, 0.12);
                            border-radius: 20px;
                            padding: 32px 40px;
                            max-width: 520px;
                            width: 100%;
                            box-shadow: 0 20px 40px rgba(0,0,0,0.6);
                            text-align: center;
                        }
                        h2 { margin-top: 0; color: #10B981; }
                        p { font-size: 14px; color: rgba(255, 255, 255, 0.7); line-height: 1.6; }
                        .btn {
                            display: inline-block;
                            background: #FF0000;
                            color: #fff;
                            font-weight: bold;
                            padding: 12px 24px;
                            border-radius: 12px;
                            text-decoration: none;
                            margin-top: 15px;
                            cursor: pointer;
                        }
                        textarea {
                            width: 100%;
                            height: 70px;
                            background: #0d1310;
                            border: 1px solid rgba(255,255,255,0.15);
                            border-radius: 10px;
                            color: #fff;
                            padding: 10px;
                            box-sizing: border-box;
                            margin-top: 12px;
                            font-family: monospace;
                            font-size: 12px;
                        }
                        .code-box {
                            background: #0d1310;
                            border: 1px solid rgba(255,255,255,0.1);
                            padding: 10px;
                            border-radius: 8px;
                            font-family: monospace;
                            font-size: 12px;
                            text-align: left;
                            overflow-x: auto;
                            color: #34D399;
                        }
                        .btn-send {
                            background: #10B981;
                            color: #0b0f0c;
                            border: none;
                            font-weight: bold;
                            padding: 12px 20px;
                            border-radius: 12px;
                            width: 100%;
                            cursor: pointer;
                            margin-top: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>Melo & YouTube Music</h2>
                        <p>1. Откройте в новой вкладке <a href="https://music.youtube.com" target="_blank" style="color: #60A5FA;">music.youtube.com</a> (убедитесь, что вы вошли в свой Google аккаунт).</p>
                        <p>2. Нажмите F12 (Консоль) и вставьте эту команду, чтобы передать сессию в Melo:</p>
                        <div class="code-box" id="scriptCode">
                            fetch('http://127.0.0.1:$port/callback?cookie=' + encodeURIComponent(document.cookie))
                        </div>
                        <p>3. Либо просто скопируйте cookies и нажмите отправить ниже:</p>
                        <form action="/callback" method="GET">
                            <textarea name="cookie" placeholder="Вставьте Cookie или SAPISID..."></textarea>
                            <button type="submit" class="btn-send">Подключить к Melo</button>
                        </form>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val bytes = html.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html; charset=UTF-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            val os: OutputStream = exchange.responseBody
            os.write(bytes)
            os.close()
        }
    }

    private class CallbackHandler(private val onSuccess: () -> Unit) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            // Поддерживаем как CORS для fetch, так и стандартный GET
            exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
            exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, OPTIONS")
            exchange.responseHeaders.add("Access-Control-Allow-Headers", "*")

            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return
            }

            val query = exchange.requestURI.query ?: ""
            val cookieParam = query.split("&")
                .firstOrNull { it.startsWith("cookie=") }
                ?.substringAfter("cookie=")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

            if (!cookieParam.isNullOrBlank()) {
                DesktopYouTubeAuthManager.saveSession(
                    cookies = cookieParam,
                    name = "YouTube Music",
                )
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    DesktopYouTubeAuthManager.fetchUserProfile()
                }
                onSuccess()
            }

            val successHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>Успешно!</title>
                    <style>
                        body {
                            background-color: #0b0f0c;
                            color: #10B981;
                            font-family: sans-serif;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            height: 100vh;
                            margin: 0;
                        }
                        .box {
                            background: #161e19;
                            border: 1px solid rgba(255,255,255,0.1);
                            padding: 40px;
                            border-radius: 20px;
                            text-align: center;
                        }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <h1>Вход выполнен!</h1>
                        <p style="color: #fff;">YouTube Music успешно подключен к Melo. Можете закрыть эту вкладку.</p>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val bytes = successHtml.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html; charset=UTF-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            val os: OutputStream = exchange.responseBody
            os.write(bytes)
            os.close()

            stopServer()
        }
    }
}
