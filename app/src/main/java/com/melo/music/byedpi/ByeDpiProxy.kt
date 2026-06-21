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

    /** Стратегия обхода DPI по умолчанию (универсальная, авто-подбор). */
    const val DEFAULT_CMD = "--auto=torst --tlsrec 1+s --split 1+s --disorder 1"

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
