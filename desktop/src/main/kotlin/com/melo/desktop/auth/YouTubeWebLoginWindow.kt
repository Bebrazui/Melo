package com.melo.desktop.auth

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Окно авторизации в YouTube Music на базе системного Chromium (Microsoft Edge / Google Chrome).
 * Поскольку встроенный в JavaFX WebKit блокируется системой безопасности Google (disallowed_useragent),
 * вход запускается через системный Chromium в режиме автономного веб-окна (--app) с изолированным профилем.
 * Куки сессии автоматически перехватываются через безопасный локальный порт отладки Chrome DevTools Protocol (CDP).
 */
object YouTubeWebLoginWindow {

    private var monitoringJob: Job? = null
    private var browserProcess: Process? = null

    private fun findChromiumExecutable(): String? {
        val candidates = listOf(
            "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            System.getenv("LOCALAPPDATA") + "\\Microsoft\\Edge\\Application\\msedge.exe",
            System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
        )
        return candidates.firstOrNull { File(it).exists() }
    }

    fun open(
        onSuccess: (cookies: String) -> Unit,
        onClose: () -> Unit = {},
    ) {
        val exePath = findChromiumExecutable()
        if (exePath == null) {
            SwingUtilities.invokeLater {
                JOptionPane.showMessageDialog(
                    null,
                    "На вашем компьютере не найден Edge или Chrome для безопасного входа Google.\nПожалуйста, вставьте cookies вручную.",
                    "Вход в YouTube Music",
                    JOptionPane.WARNING_MESSAGE
                )
            }
            return
        }

        // Останавливаем предыдущую сессию, если была
        monitoringJob?.cancel()
        browserProcess?.destroy()

        val port = 9333
        val profileDir = File(System.getProperty("user.home"), ".melo/edge_auth").apply { mkdirs() }
        val loginUrl = "https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F"

        val command = listOf(
            exePath,
            "--app=$loginUrl",
            "--user-data-dir=${profileDir.absolutePath}",
            "--remote-debugging-port=$port",
            "--window-size=600,750",
            "--no-first-run",
            "--no-default-browser-check"
        )

        try {
            val process = ProcessBuilder(command).start()
            browserProcess = process

            monitoringJob = CoroutineScope(Dispatchers.IO).launch {
                // Ожидаем запуск браузера и поднятие порта CDP
                delay(1500)

                while (isActive && process.isAlive) {
                    delay(1200)

                    val (currentUrl, cookies) = fetchCurrentUrlAndCookiesViaCdp(port)
                    val hasSapisid = cookies.contains("SAPISID=") || cookies.contains("__Secure-3PAPISID=") || cookies.contains("__Secure-1PAPISID=")
                    val hasLoginInfo = cookies.contains("LOGIN_INFO=")
                    val hasYtSid = cookies.contains("SID=") && (currentUrl.contains("youtube.com") || currentUrl.contains("music.youtube.com"))

                    // Ждем, пока браузер действительно перейдет на YouTube Music
                    if (hasSapisid && (hasLoginInfo || hasYtSid)) {
                        delay(1500)
                        val finalCookies = fetchCookiesViaCdp(port)
                        val toSave = if (finalCookies.isNotBlank()) finalCookies else cookies
                        DesktopYouTubeAuthManager.saveSession(
                            cookies = toSave,
                            name = "YouTube Music",
                        )
                        runCatching { process.destroy() }
                        SwingUtilities.invokeLater {
                            onSuccess(toSave)
                        }
                        // Автоматический перенос медиатеки 1 в 1 сразу после авторизации
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                com.melo.desktop.sync.DesktopYouTubeSyncManager.syncLibrary()
                            } catch (_: Exception) {}
                        }
                        break
                    }
                }

                if (!process.isAlive) {
                    SwingUtilities.invokeLater {
                        onClose()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SwingUtilities.invokeLater {
                JOptionPane.showMessageDialog(
                    null,
                    "Не удалось запустить окно авторизации: ${e.message}",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun fetchCookiesViaCdp(port: Int): String {
        return fetchCurrentUrlAndCookiesViaCdp(port).second
    }

    private fun fetchCurrentUrlAndCookiesViaCdp(port: Int): Pair<String, String> {
        return runCatching {
            // 1. Получаем список открытых страниц через HTTP CDP endpoint
            val listUrl = URL("http://127.0.0.1:$port/json")
            val conn = (listUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 1000
                readTimeout = 1000
            }
            if (conn.responseCode != 200) return "" to ""

            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val targets = JSONArray(jsonText)

            var bestUrl = ""
            var bestCookies = ""

            for (i in 0 until targets.length()) {
                val target = targets.getJSONObject(i)
                val targetUrl = target.optString("url", "")
                val wsUrl = target.optString("webSocketDebuggerUrl", "")

                // Ищем сессию, где есть youtube.com или google.com
                if (wsUrl.isNotBlank() && (targetUrl.contains("youtube.com") || targetUrl.contains("google.com"))) {
                    bestUrl = targetUrl
                    val cookies = requestCookiesFromWebSocket(wsUrl)
                    if (cookies.isNotBlank()) {
                        bestCookies = cookies
                        if (targetUrl.contains("music.youtube.com") || targetUrl.contains("youtube.com")) {
                            break
                        }
                    }
                }
            }
            bestUrl to bestCookies
        }.getOrDefault("" to "")
    }

    private fun requestCookiesFromWebSocket(wsUrl: String): String {
        var resultCookies = ""
        val latch = java.util.concurrent.CountDownLatch(1)

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val request = okhttp3.Request.Builder().url(wsUrl).build()

        val ws = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                // Запрашиваем cookies со всех доменов YouTube и Google
                val msg = JSONObject().apply {
                    put("id", 1001)
                    put("method", "Network.getCookies")
                    put("params", JSONObject().apply {
                        put("urls", JSONArray().apply {
                            put("https://music.youtube.com")
                            put("https://www.youtube.com")
                            put("https://youtube.com")
                            put("https://accounts.google.com")
                            put("https://google.com")
                        })
                    })
                }
                webSocket.send(msg.toString())
            }

            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    if (json.optInt("id") == 1001) {
                        val result = json.optJSONObject("result")
                        val cookieList = result?.optJSONArray("cookies")
                        if (cookieList != null) {
                            val list = mutableListOf<String>()
                            for (i in 0 until cookieList.length()) {
                                val c = cookieList.getJSONObject(i)
                                val name = c.optString("name")
                                val value = c.optString("value")
                                if (name.isNotBlank() && value.isNotBlank()) {
                                    list.add("$name=$value")
                                }
                            }
                            resultCookies = list.distinctBy { it.substringBefore("=") }.joinToString("; ")
                        }
                        webSocket.close(1000, "done")
                        latch.countDown()
                    }
                }
            }

            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                latch.countDown()
            }

            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                latch.countDown()
            }
        })

        runCatching {
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
            ws.close(1000, "close")
        }

        return resultCookies
    }
}
