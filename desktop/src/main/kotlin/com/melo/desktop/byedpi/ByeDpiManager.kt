package com.melo.desktop.byedpi

import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Менеджер обхода DPI (ByeDPI) на ПК.
 * По умолчанию работает на 127.0.0.1:1080.
 */
object ByeDpiManager {

    const val DEFAULT_PORT = 1080
    const val DEFAULT_HOST = "127.0.0.1"

    /** Проверенная стратегия обхода DPI для YouTube / SoundCloud в РФ. */
    const val DEFAULT_CMD =
        """-H:"youtube.com googlevideo.com ytimg.com ggpht.com youtu.be youtubei.googleapis.com yt3.googleusercontent.com googleusercontent.com" -Kt,h -d1 -s1+s -s3+s -s6+s -s9+s -s12+s -s15+s -s20+s -s30+s -a1 -An -H:"soundcloud.com api.soundcloud.com api-v2.soundcloud.com m.soundcloud.com eventgateway.soundcloud.com api-partners.soundcloud.com api-mobile.soundcloud.com wis.sndcdn.com va.sndcdn.com invite.soundcloud.com events.soundcloud.com" -Kt -r1+s -An -H:"a-v2.sndcdn.com cf-hls-media.sndcdn.com cf-media.sndcdn.com cf-preview-media.sndcdn.com cf-hls-opus-media.sndcdn.com assets.soundcloud.com playback.media-streaming.soundcloud.cloud" -f-200 -s2 -s5+hm -t6 -Qr -n wb.ru -An -H:"i1.sndcdn.com i2.sndcdn.com i3.sndcdn.com i4.sndcdn.com" -Kt -r1+s -An -H:"bandcamp.com bcbits.com f4.bcbits.com t4.bcbits.com" -Kt,h -d1 -s1+s -s3+s -s6+s -s9+s -s12+s -s15+s -s20+s -s30+s -a1 -An"""

    @Volatile
    private var process: Process? = null

    @Volatile
    var isEnabled: Boolean = true

    @Volatile
    private var lastPortCheckTime: Long = 0L
    @Volatile
    private var cachedPortOpen: Boolean = false
    private const val PORT_CACHE_TTL_MS = 3000L

    /** Проверяет, слушает ли локальный SOCKS5 порт 1080. */
    fun isPortOpen(host: String = DEFAULT_HOST, port: Int = DEFAULT_PORT, force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        if (!force && (now - lastPortCheckTime < PORT_CACHE_TTL_MS)) {
            return cachedPortOpen
        }
        val open = try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 100)
                true
            }
        } catch (_: Exception) {
            false
        }
        cachedPortOpen = open
        lastPortCheckTime = now
        return open
    }

    /** Проверяет, активен ли ByeDPI (либо наш процесс, либо внешний демон). */
    fun isRunning(force: Boolean = false): Boolean {
        if (process != null && process!!.isAlive) return true
        return isPortOpen(force = force)
    }

    /** Запускает локальный бинарник ciadpi.exe, если доступен. */
    fun start(customCmd: String? = null): Boolean {
        if (!isEnabled) return false
        if (isPortOpen()) {
            println("[ByeDpiManager] Port $DEFAULT_PORT is already active, reusing existing proxy.")
            return true
        }

        val binary = findCiadpiBinary() ?: run {
            println("[ByeDpiManager] ciadpi binary not found in ~/.melo/bin. Network requests will proceed via direct / system connection.")
            return false
        }

        return try {
            val cmdLine = customCmd ?: DEFAULT_CMD
            val args = mutableListOf(binary.absolutePath, "-i", DEFAULT_HOST, "-p", DEFAULT_PORT.toString())
            args.addAll(shellSplit(cmdLine))

            val pb = ProcessBuilder(args)
            pb.redirectErrorStream(true)
            process = pb.start()

            // Ждём 500мс для старта порта
            Thread.sleep(500)
            val started = isPortOpen()
            println("[ByeDpiManager] Started ciadpi (PID=${process?.pid()}): success=$started")
            started
        } catch (e: Exception) {
            System.err.println("[ByeDpiManager] Failed to start ciadpi: ${e.message}")
            false
        }
    }

    fun stop() {
        process?.let {
            try {
                it.destroyForcibly()
            } catch (_: Exception) {}
        }
        process = null
    }

    private fun findCiadpiBinary(): File? {
        val userHome = System.getProperty("user.home")
        val meloBin = File(userHome, ".melo/bin")
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val exeName = if (isWindows) "ciadpi.exe" else "ciadpi"

        val candidates = listOf(
            File(meloBin, exeName),
            File("tools/$exeName"),
            File("bin/$exeName"),
        )
        return candidates.firstOrNull { it.exists() && it.canExecute() }
    }

    private fun shellSplit(cmd: String): List<String> {
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

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
    }
}
