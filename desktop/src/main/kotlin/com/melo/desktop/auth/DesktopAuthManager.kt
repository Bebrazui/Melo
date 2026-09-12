package com.melo.desktop.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.melo.desktop.net.DesktopMeloNet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Менеджер аккаунта Melo (Appwrite REST API) для Desktop.
 * Поддерживает вход по Email + пароль, регистрацию по коду и профиль.
 */
object DesktopAuthManager {

    private const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"
    private const val PROJECT_ID = "6a38f49f0024c6b9b473"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val sessionFile = File(System.getProperty("user.home"), ".melo/session.json")

    var email by mutableStateOf<String?>(null)
        private set
    var name by mutableStateOf<String?>(null)
        private set
    var avatarUrl by mutableStateOf<String?>(null)
        private set
    var userId by mutableStateOf<String?>(null)
        private set
    var sessionId by mutableStateOf<String?>(null)
        private set
    var sessionSecret by mutableStateOf<String?>(null)
        private set

    val isLoggedIn: Boolean get() = !email.isNullOrBlank() || !sessionId.isNullOrBlank()

    init {
        loadSession()
    }

    private fun loadSession() {
        if (!sessionFile.exists()) return
        runCatching {
            val json = sessionFile.readText()
            val obj = JSONObject(json)
            email = obj.optString("email").takeIf { it.isNotBlank() }
            name = obj.optString("name").takeIf { it.isNotBlank() }
            avatarUrl = obj.optString("avatarUrl").takeIf { it.isNotBlank() }
            userId = obj.optString("userId").takeIf { it.isNotBlank() }
            sessionId = obj.optString("sessionId").takeIf { it.isNotBlank() }
            sessionSecret = obj.optString("sessionSecret").takeIf { it.isNotBlank() }
        }
    }

    private fun saveSession() {
        runCatching {
            sessionFile.parentFile?.mkdirs()
            val obj = JSONObject().apply {
                put("email", email.orEmpty())
                put("name", name.orEmpty())
                put("avatarUrl", avatarUrl.orEmpty())
                put("userId", userId.orEmpty())
                put("sessionId", sessionId.orEmpty())
                put("sessionSecret", sessionSecret.orEmpty())
            }
            sessionFile.writeText(obj.toString(2))
        }
    }

    private fun baseRequest(path: String): Request.Builder {
        val b = Request.Builder()
            .url("$ENDPOINT$path")
            .header("X-Appwrite-Project", PROJECT_ID)
            .header("X-Appwrite-Response-Format", "1.4.0")
        if (!sessionSecret.isNullOrBlank()) {
            b.header("X-Appwrite-Key", sessionSecret!!)
        }
        return b
    }

    /** Вход по Email и Паролю через Appwrite */
    suspend fun loginWithEmail(emailInput: String, passwordInput: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bodyObj = JSONObject().apply {
                put("email", emailInput.trim())
                put("password", passwordInput)
            }
            val req = baseRequest("/account/sessions/email")
                .post(bodyObj.toString().toRequestBody(JSON_MEDIA))
                .build()

            val resp = DesktopMeloNet.okHttpClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val err = runCatching { JSONObject(body).optString("message") }.getOrNull()
                error(err ?: "Ошибка входа (${resp.code})")
            }

            val sessionObj = JSONObject(body)
            sessionId = sessionObj.optString("\$id")
            userId = sessionObj.optString("userId")
            sessionSecret = sessionObj.optString("secret")

            // Подтягиваем данные пользователя
            fetchAccountInfo()
            saveSession()
        }
    }

    /** Регистрация нового аккаунта */
    suspend fun registerWithEmail(emailInput: String, passwordInput: String, nameInput: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val newUserId = UUID.randomUUID().toString().replace("-", "").take(32)
            val bodyObj = JSONObject().apply {
                put("userId", newUserId)
                put("email", emailInput.trim())
                put("password", passwordInput)
                put("name", nameInput.trim().ifBlank { "Пользователь Melo" })
            }
            val req = baseRequest("/account")
                .post(bodyObj.toString().toRequestBody(JSON_MEDIA))
                .build()

            val resp = DesktopMeloNet.okHttpClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val err = runCatching { JSONObject(body).optString("message") }.getOrNull()
                error(err ?: "Ошибка регистрации (${resp.code})")
            }

            // После создания сразу логинимся
            loginWithEmail(emailInput, passwordInput).getOrThrow()
        }
    }

    /** Быстрый вход / подтягивание через Google OAuth Token */
    suspend fun loginWithGoogleProfile(gEmail: String, gName: String, gAvatar: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            email = gEmail
            name = gName
            avatarUrl = gAvatar
            userId = "g_${gEmail.hashCode()}"
            saveSession()
        }
    }

    /**
     * Запускает автоматический бесшовный вход через Google OAuth2 в системном браузере.
     * Открывает стандартное окно Google ("Выберите аккаунт"), ловит callback на локальном порту,
     * сохраняет сессию и профиль без единого ручного действия пользователя.
     */
    fun startGoogleOAuthFlow(
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            var server: com.sun.net.httpserver.HttpServer? = null
            try {
                val s = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
                val port = s.address.port
                server = s

                val successUrl = "http://127.0.0.1:$port/callback"
                val failureUrl = "http://127.0.0.1:$port/failure"

                s.createContext("/callback") { exchange ->
                    val query = exchange.requestURI.query.orEmpty()
                    val params = query.split("&").associate {
                        val parts = it.split("=")
                        parts[0] to (parts.getOrNull(1)?.let { v -> java.net.URLDecoder.decode(v, "UTF-8") } ?: "")
                    }

                    val uId = params["userId"]
                    val secret = params["secret"]

                    if (!secret.isNullOrBlank()) {
                        userId = uId
                        sessionSecret = secret
                        sessionId = "oauth_$secret"
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            fetchAccountInfo()
                            saveSession()
                        }
                    }

                    val responseHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8">
                            <title>Вход выполнен</title>
                            <style>
                                body {
                                    background: #0B0F0C;
                                    color: #10B981;
                                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    height: 100vh;
                                    margin: 0;
                                }
                                .card {
                                    background: #161E19;
                                    border: 1px solid rgba(255,255,255,0.1);
                                    padding: 36px 48px;
                                    border-radius: 20px;
                                    text-align: center;
                                }
                                h1 { margin: 0 0 10px; font-size: 24px; color: #10B981; }
                                p { color: rgba(255,255,255,0.7); margin: 0; font-size: 14px; }
                            </style>
                        </head>
                        <body>
                            <div class="card">
                                <h1>Вход выполнен!</h1>
                                <p>Вы успешно вошли в Melo. Можете закрыть эту вкладку и вернуться в приложение.</p>
                            </div>
                            <script>setTimeout(function() { window.close(); }, 2000);</script>
                        </body>
                        </html>
                    """.trimIndent()

                    val bytes = responseHtml.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "text/html; charset=UTF-8")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.write(bytes)
                    exchange.responseBody.close()

                    // Оповещаем UI
                    kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                        onSuccess()
                    }

                    // Останавливаем сервер через секунду
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        kotlinx.coroutines.delay(1000)
                        server?.stop(0)
                    }
                }

                s.createContext("/failure") { exchange ->
                    val failureHtml = "<html><body style='background:#0B0F0C;color:#EF4444;font-family:sans-serif;text-align:center;padding-top:50px;'><h1>Ошибка авторизации</h1><p>Попробуйте снова в приложении.</p></body></html>"
                    val bytes = failureHtml.toByteArray(Charsets.UTF_8)
                    exchange.sendResponseHeaders(400, bytes.size.toLong())
                    exchange.responseBody.write(bytes)
                    exchange.responseBody.close()
                    kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                        onError("Авторизация была отменена")
                    }
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        kotlinx.coroutines.delay(1000)
                        server?.stop(0)
                    }
                }

                s.executor = null
                s.start()

                // Формируем URL Appwrite OAuth2 с YouTube scope
                val oauthInitUrl = "$ENDPOINT/account/sessions/oauth2/google" +
                        "?project=$PROJECT_ID" +
                        "&success=" + java.net.URLEncoder.encode(successUrl, "UTF-8") +
                        "&failure=" + java.net.URLEncoder.encode(failureUrl, "UTF-8") +
                        "&scopes%5B%5D=" + java.net.URLEncoder.encode("https://www.googleapis.com/auth/youtube.readonly", "UTF-8")

                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(java.net.URI(oauthInitUrl))
                }
            } catch (e: Exception) {
                server?.stop(0)
                kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                    onError(e.message ?: "Не удалось запустить браузер для входа")
                }
            }
        }
    }

    suspend fun fetchAccountInfo() = withContext(Dispatchers.IO) {
        runCatching {
            val req = baseRequest("/account").get().build()
            val resp = DesktopMeloNet.okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val obj = JSONObject(resp.body?.string().orEmpty())
                email = obj.optString("email").takeIf { it.isNotBlank() } ?: email
                name = obj.optString("name").takeIf { it.isNotBlank() } ?: name
                userId = obj.optString("\$id").takeIf { it.isNotBlank() } ?: userId
            }
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!sessionId.isNullOrBlank()) {
                val req = baseRequest("/account/sessions/current").delete().build()
                runCatching { DesktopMeloNet.okHttpClient.newCall(req).execute() }
            }
            email = null
            name = null
            avatarUrl = null
            userId = null
            sessionId = null
            sessionSecret = null
            sessionFile.delete()
            Unit
        }
    }
}
