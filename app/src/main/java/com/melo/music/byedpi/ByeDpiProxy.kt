package com.melo.music.byedpi

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Локальный SOCKS5-прокси на базе ByeDPI.
 * Работает на 127.0.0.1:1080 (по умолчанию).
 * Обход DPI: split, disorder, fake, OOB, tlsrec и т.д.
 */
object ByeDpiProxy {

    private const val TAG = "ByeDpiProxy"
    private const val PREFS = "melo_byedpi"
    private const val KEY_CMD = "cmd_line"
    private const val KEY_ENABLED = "enabled"
    const val DEFAULT_PORT = 1080
    const val DEFAULT_HOST = "127.0.0.1"

    const val DEFAULT_CMD =
        """-X -H:"youtube.com googlevideo.com ytimg.com ggpht.com youtu.be youtubei.googleapis.com yt3.googleusercontent.com googleusercontent.com accounts.youtube.com music.youtube.com bandcamp.com bcbits.com f4.bcbits.com t4.bcbits.com" -Kt,h -d1 -s1+s -s3+s -s6+s -s9+s -s12+s -s15+s -s20+s -s30+s -An -H:"a-v2.sndcdn.com cf-hls-media.sndcdn.com cf-media.sndcdn.com playback.media-streaming.soundcloud.cloud soundcloud.cloud" -f-200 -s2 -s5+hm -t6 -Qr -n wb.ru -An -H:"soundcloud.com api.soundcloud.com api-v2.soundcloud.com m.soundcloud.com" -Kt -r1+s -An -H:"i1.sndcdn.com i2.sndcdn.com i3.sndcdn.com i4.sndcdn.com" -Kt -r1+s -s2 -An -H:"discord.com discord.gg discord.media discordapp.com cdn.discordapp.com media.discordapp.net images-ext-1.discordapp.net images-ext-2.discordapp.net images.discordapp.net gateway.discord.gg status.discord.com api.discord.com discord-attachments-uploads-prd.storage.googleapis.com hcaptcha.com recaptcha.net accounts.google.com accounts.youtube.com appleid.apple.com" -Kt,h -Qorig -n "www.google.com" -f-1 -t5 -o1 -s1+s -s2+s -s5+s -d3+s -s7+s -s10+s -s15+s -An -Ku""""

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private var running = false

    init {
        System.loadLibrary("byedpi")
        // ByeDPI при остановке делает двойной close() fd → fdsan роняет процесс (SIGABRT).
        // Отключаем fdsan, чтобы рестарт прокси не крашил приложение.
        runCatching { jniDisableFdsan() }
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val cm = appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null && networkCallback == null) {
            val cb = object : ConnectivityManager.NetworkCallback() {
                private var lastNetwork: android.net.Network? = null
                override fun onAvailable(network: android.net.Network) {
                    if (lastNetwork != null && lastNetwork != network) {
                        if (isEnabled() && running) {
                            Thread {
                                runCatching {
                                    Thread.sleep(400)
                                    restart()
                                }
                            }.start()
                        }
                    }
                    lastNetwork = network
                }
            }
            cm.registerDefaultNetworkCallback(cb)
            networkCallback = cb
        }
    }

    /** Активен ли системный VPN. */
    fun isVpnActive(): Boolean {
        val ctx = appContext ?: return false
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /** Нужно ли маршрутизировать через ByeDPI: включён, запущен и нет активного VPN. */
    fun shouldRoute(): Boolean = isEnabled() && isRunning() && !isVpnActive()

    /** Текущий прокси для OkHttp. */
    fun getProxy(): Proxy {
        return Proxy(Proxy.Type.SOCKS, InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT))
    }

    fun isRunning(): Boolean = running

    /** Гарантирует, что ByeDPI запущен, если он включен пользователем. */
    @Synchronized
    fun ensureRunning(): Boolean {
        if (!isEnabled()) return false
        if (!running) {
            Log.w(TAG, "ByeDPI stopped, restarting...")
            return start()
        }
        return true
    }

    fun isEnabled(): Boolean = prefs?.getBoolean(KEY_ENABLED, true) ?: true

    fun setEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }

    fun getCommandLine(): String {
        val saved = prefs?.getString(KEY_CMD, null)
        if (saved.isNullOrBlank() ||
            !saved.contains("-H:\"soundcloud.com api.soundcloud.com api-v2.soundcloud.com m.soundcloud.com\" -Kt -r1+s -An") ||
            !saved.contains("-H:\"i1.sndcdn.com i2.sndcdn.com i3.sndcdn.com i4.sndcdn.com\" -Kt -r1+s -s2") ||
            !saved.contains("-H:\"a-v2.sndcdn.com cf-hls-media.sndcdn.com")
        ) {
            prefs?.edit()?.putString(KEY_CMD, DEFAULT_CMD)?.apply()
            return DEFAULT_CMD
        }
        return saved
    }

    fun setCommandLine(cmd: String) {
        prefs?.edit()?.putString(KEY_CMD, cmd)?.apply()
    }

    /**
     * Запускает ByeDPI прокси с указанными аргументами командной строки.
     * Аргументы парсятся так же, как в CLI: "--disorder 1 --auto=torst" и т.д.
     */
    fun start(customArgs: String? = null): Boolean {
        if (running) {
            return true
        }

        val cmdLine = customArgs ?: getCommandLine()
        if (cmdLine.isBlank()) {
            return false
        }

        com.melo.music.util.FileLog.i(TAG, "Starting ByeDPI with cmd: $cmdLine")

        val args = shellSplit(cmdLine)
        if (args.isEmpty()) {
            return false
        }

        // Принудительно слушать на localhost
        val finalArgs = mutableListOf<String>()
        finalArgs.addAll(args)

        // Если нет -i, добавляем localhost
        if (!finalArgs.contains("-i") && !finalArgs.contains("--ip")) {
            finalArgs.addAll(listOf("-i", DEFAULT_HOST))
        }
        // Если нет -p, добавляем порт
        if (!finalArgs.contains("-p") && !finalArgs.contains("--port")) {
            finalArgs.addAll(listOf("-p", DEFAULT_PORT.toString()))
        }
        // Включаем отладку по умолчанию
        if (!finalArgs.contains("-x") && !finalArgs.contains("--debug")) {
            finalArgs.addAll(listOf("-x", "1"))
        }

        val result = jniStartProxy(finalArgs.toTypedArray())
        running = result == 0
        return running
    }

    fun stop() {
        if (!running) return
        jniStopProxy()
        running = false
    }

    /** Перезапуск с заданной стратегией (для подбора/тюнинга или смены сети). */
    @Synchronized
    fun restart(args: String? = null): Boolean {
        stop()
        Thread.sleep(250)
        return start(args)
    }

    /**
     * Разбивает строку на аргументы (аналог shell split).
     * Поддерживает кавычки: --hosts ":example.com"
     */
    fun shellSplit(cmd: String): List<String> {
        val args = mutableListOf<String>()
        val current = StringBuilder()
        var inSingle = false
        var inDouble = false
        var escaped = false

        for (c in cmd) {
            when {
                escaped -> {
                    current.append(c)
                    escaped = false
                }
                c == '\\' -> escaped = true
                c == '\'' && !inDouble -> inSingle = !inSingle
                c == '"' && !inSingle -> inDouble = !inDouble
                c.isWhitespace() && !inSingle && !inDouble -> {
                    if (current.isNotEmpty()) {
                        args.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(c)
            }
        }
        if (current.isNotEmpty()) {
            args.add(current.toString())
        }
        return args
    }

    // JNI methods
    private external fun jniStartProxy(args: Array<String>): Int
    private external fun jniStopProxy(): Int
    private external fun jniIsRunning(): Boolean
    private external fun jniDisableFdsan()
}
