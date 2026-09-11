package com.melo.desktop.zapret

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Пресеты командной строки из репозитория flowseal/zapret-discord-youtube.
 */
enum class ZapretPreset(
    val title: String,
    val description: String,
    val args: String,
) {
    GENERAL(
        title = "General (YouTube + Discord)",
        description = "Основной пресет: fake + split2 с md5sig и фейками UDP. Рекомендуется по умолчанию.",
        args = "--wf-tcp=80,443 --wf-udp=443,50000-65535 --filter-udp=443 --dpi-desync=fake --dpi-desync-repeats=11 --filter-tcp=80,443 --dpi-desync=fake,split2 --dpi-desync-autottl=2 --dpi-desync-fooling=md5sig",
    ),
    GENERAL_ALT(
        title = "General ALT (Мультисплит)",
        description = "Альтернативный режим с multisplit для провайдеров с глубокой фильтрацией.",
        args = "--wf-tcp=80,443 --wf-udp=443,50000-65535 --filter-udp=443 --dpi-desync=fake --filter-tcp=80,443 --dpi-desync=fake,multisplit --dpi-desync-split-pos=1,midsld",
    ),
    GENERAL_FAKE(
        title = "General Fake",
        description = "Упрощенный режим fake с повторами для сетей без поддержки split.",
        args = "--wf-tcp=80,443 --wf-udp=443,50000-65535 --filter-udp=443 --dpi-desync=fake --filter-tcp=80,443 --dpi-desync=fake --dpi-desync-repeats=6",
    ),
    DISCORD_YOUTUBE(
        title = "Discord + YouTube (Badseq)",
        description = "Стратегия десинхронизации с искажением порядковых номеров пакетов (badseq).",
        args = "--wf-tcp=80,443 --wf-udp=443,50000-65535 --filter-udp=443 --dpi-desync=fake --dpi-desync-repeats=11 --filter-tcp=80,443 --dpi-desync=fake,split --dpi-desync-autottl=2 --dpi-desync-fooling=badseq",
    ),
    CUSTOM(
        title = "Пользовательский",
        description = "Ручные флаги командной строки для winws.exe.",
        args = "",
    );
}

/**
 * Менеджер управления Zapret (winws.exe + WinDivert) на ПК.
 * Позволяет запускать проверенные стратегии flowseal/zapret-discord-youtube
 * с повышением привилегий Windows (UAC) и отслеживать статус фильтрации.
 */
object ZapretManager {

    val isWindows: Boolean =
        System.getProperty("os.name", "").lowercase().contains("win")

    @Volatile
    private var lastCheckTime: Long = 0
    @Volatile
    private var cachedIsRunning: Boolean = false
    private const val CACHE_TTL_MS = 4000L

    val isStarting = AtomicBoolean(false)

    /**
     * Быстрая проверка, запущен ли winws.exe в системе (как запущенный из Melo, так и фоновый сервис).
     */
    fun isRunning(forceRefresh: Boolean = false): Boolean {
        if (!isWindows) return false
        val now = System.currentTimeMillis()
        if (!forceRefresh && (now - lastCheckTime < CACHE_TTL_MS)) {
            return cachedIsRunning
        }

        val running = checkProcessRunning(allowProcessSpawn = forceRefresh)
        cachedIsRunning = running
        lastCheckTime = now
        return running
    }

    private fun checkProcessRunning(allowProcessSpawn: Boolean): Boolean {
        // Сначала пробуем встроенный ProcessHandle API Java (быстро, ~1мс, без спавна подпроцессов)
        try {
            val foundViaJava = ProcessHandle.allProcesses().anyMatch { ph ->
                val info = ph.info()
                val cmd = info.command().orElse("")
                val cmdLine = info.commandLine().orElse("")
                cmd.endsWith("winws.exe", ignoreCase = true) ||
                    cmd.equals("winws", ignoreCase = true) ||
                    cmdLine.contains("winws.exe", ignoreCase = true)
            }
            if (foundViaJava) return true
        } catch (_: Exception) {}

        if (!allowProcessSpawn) return cachedIsRunning

        // Запасной опрос через tasklist только при явном forceRefresh
        return try {
            val pb = ProcessBuilder("tasklist", "/FI", "IMAGENAME eq winws.exe", "/NH")
            pb.redirectErrorStream(true)
            val p = pb.start()
            val output = p.inputStream.bufferedReader().readText()
            p.waitFor()
            output.contains("winws.exe", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Поиск бинарника winws.exe.
     * Проверяет пользовательский путь, папку ~/.melo/bin/zapret/, а также стандартные директории.
     */
    fun findWinwsBinary(customPath: String? = null): File? {
        if (!isWindows) return null

        if (!customPath.isNullOrBlank()) {
            val custom = File(customPath.trim())
            if (custom.isFile && custom.name.equals("winws.exe", ignoreCase = true) && custom.exists()) {
                return custom
            }
            if (custom.isDirectory) {
                val candidate = File(custom, "winws.exe")
                if (candidate.exists()) return candidate
            }
        }

        val userHome = System.getProperty("user.home")
        val candidates = listOf(
            File(userHome, ".melo/bin/zapret/winws.exe"),
            File(userHome, ".melo/bin/winws.exe"),
            File("tools/zapret/winws.exe"),
            File("tools/winws.exe"),
            File("bin/winws.exe"),
            File("winws.exe"),
            File(userHome, "zapret-discord-youtube/winws.exe"),
            File(userHome, "Downloads/zapret-discord-youtube/winws.exe"),
        )

        return candidates.firstOrNull { it.exists() && it.isFile }
    }

    /**
     * Запуск winws.exe с повышением прав UAC через PowerShell.
     * Возвращает true, если запуск был инициирован успешно.
     */
    fun start(
        preset: ZapretPreset = ZapretPreset.GENERAL,
        customArgs: String? = null,
        customPath: String? = null,
    ): Boolean {
        if (!isWindows) return false
        if (isRunning(forceRefresh = true)) {
            println("[ZapretManager] winws.exe is already active.")
            return true
        }

        val binary = findWinwsBinary(customPath)
        if (binary == null) {
            System.err.println("[ZapretManager] winws.exe not found. Specify custom path in settings or place into ~/.melo/bin/zapret/")
            return false
        }

        val rawArgs = if (preset == ZapretPreset.CUSTOM) {
            customArgs?.trim().orEmpty()
        } else {
            preset.args
        }

        if (rawArgs.isBlank()) {
            System.err.println("[ZapretManager] Empty arguments for Zapret.")
            return false
        }

        return try {
            isStarting.set(true)
            val workDir = binary.parentFile?.absolutePath ?: ""
            val exePath = binary.absolutePath

            // Экранируем аргументы для PowerShell Start-Process
            val escapedArgs = rawArgs.replace("\"", "`\"")

            val psCommand = "Start-Process -FilePath \"$exePath\" -ArgumentList \"$escapedArgs\" -WorkingDirectory \"$workDir\" -Verb RunAs -WindowStyle Hidden"

            val pb = ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command", psCommand)
            val proc = pb.start()
            proc.waitFor()

            // Небольшая пауза на UAC подтверждение и старт WinDivert драйвера
            Thread.sleep(1200)

            val started = isRunning(forceRefresh = true)
            println("[ZapretManager] Started winws ($preset): running=$started")
            started
        } catch (e: Exception) {
            System.err.println("[ZapretManager] Failed to start winws: ${e.message}")
            false
        } finally {
            isStarting.set(false)
        }
    }

    /**
     * Остановка winws.exe и освобождение WinDivert драйвера.
     */
    fun stop(): Boolean {
        if (!isWindows) return false
        return try {
            val pb = ProcessBuilder("taskkill", "/F", "/IM", "winws.exe")
            val proc = pb.start()
            proc.waitFor()

            // Также освобождаем драйвер WinDivert если требуется
            runCatching {
                ProcessBuilder("net", "stop", "WinDivert").start().waitFor()
            }

            Thread.sleep(300)
            cachedIsRunning = false
            lastCheckTime = 0
            !isRunning(forceRefresh = true)
        } catch (e: Exception) {
            System.err.println("[ZapretManager] Failed to stop winws: ${e.message}")
            false
        }
    }
}
