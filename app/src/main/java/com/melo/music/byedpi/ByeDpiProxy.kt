package com.melo.music.byedpi

import android.content.Context
import android.content.SharedPreferences
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

    /** Стратегия обхода DPI по умолчанию (профили для YouTube/SoundCloud/Discord). */
    const val DEFAULT_CMD =
        """-H:"youtube.com googlevideo.com ytimg.com ggpht.com youtu.be youtubei.googleapis.com yt3.googleusercontent.com googleusercontent.com" -Kt,h -d1 -s1+s -s3+s -s6+s -s9+s -s12+s -s15+s -s20+s -s30+s -a1 -An -H:"soundcloud.com api.soundcloud.com api-v2.soundcloud.com m.soundcloud.com eventgateway.soundcloud.com api-partners.soundcloud.com api-mobile.soundcloud.com wis.sndcdn.com va.sndcdn.com invite.soundcloud.com events.soundcloud.com" -Kt,h -d1 -s1+s -s3+s -s6+s -s9+s -s12+s -s15+s -s20+s -s30+s -a1 -An -H:"sndcdn.com a-v2.sndcdn.com cf-hls-media.sndcdn.com cf-media.sndcdn.com cf-preview-media.sndcdn.com cf-hls-opus-media.sndcdn.com i1.sndcdn.com i2.sndcdn.com i3.sndcdn.com i4.sndcdn.com assets.soundcloud.com playback.media-streaming.soundcloud.cloud" -Kt,h -d1 -s1+s -s3+s -s6+s -s9+s -s12+s -s15+s -s20+s -s30+s -a1 -An -H:"discord.com discord.gg discord.media discordapp.com cdn.discordapp.com media.discordapp.net images-ext-1.discordapp.net images-ext-2.discordapp.net images.discordapp.net gateway.discord.gg status.discord.com api.discord.com discord-attachments-uploads-prd.storage.googleapis.com hcaptcha.com recaptcha.net accounts.google.com appleid.apple.com" -Kth -Qorig -n "www.google.com" -f-1 -t5 -o1 -s1+s -s2+s -s5+s -d3+s -s7+s -s10+s -s15+s -An -Ku"""

    private var prefs: SharedPreferences? = null
    private var running = false

    init {
        System.loadLibrary("byedpi")
    }

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** Текущий прокси для OkHttp. */
    fun getProxy(): Proxy {
        return Proxy(Proxy.Type.SOCKS, InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT))
    }

    fun isRunning(): Boolean = running

    fun isEnabled(): Boolean = prefs?.getBoolean(KEY_ENABLED, true) ?: true

    fun setEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }

    fun getCommandLine(): String {
        val saved = prefs?.getString(KEY_CMD, null)
        return if (saved.isNullOrBlank()) DEFAULT_CMD else saved
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
            Log.w(TAG, "Already running")
            return true
        }

        val cmdLine = customArgs ?: getCommandLine()
        if (cmdLine.isBlank()) {
            Log.e(TAG, "No command line arguments")
            return false
        }

        val args = shellSplit(cmdLine)
        if (args.isEmpty()) {
            Log.e(TAG, "Empty args after split")
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

        Log.i(TAG, "Starting with args: $finalArgs")
        val result = jniStartProxy(finalArgs.toTypedArray())
        running = result == 0
        if (!running) {
            Log.e(TAG, "Failed to start, result=$result")
        }
        return running
    }

    fun stop() {
        if (!running) return
        Log.i(TAG, "Stopping...")
        jniStopProxy()
        running = false
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
}
