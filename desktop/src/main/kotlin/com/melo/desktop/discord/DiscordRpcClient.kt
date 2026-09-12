package com.melo.desktop.discord

import com.melo.desktop.audio.DesktopAudioPlayer
import com.melo.desktop.extractor.ResolvedTrack
import com.melo.desktop.storage.DesktopStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Встроенный легковесный клиент Discord Rich Presence (IPC).
 * Работает напрямую через Named Pipe в Windows (\\.\pipe\discord-ipc-0..9)
 * без сторонних тяжелых библиотек и нативных DLL.
 */
object DiscordRpcClient {

    private const val CLIENT_ID = "1548247229524082860" // Discord Application ID для Melo
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var workerJob: Job? = null

    @Volatile
    private var pipeFile: RandomAccessFile? = null
    @Volatile
    private var isConnected = false

    private var lastTrackUrl: String? = null
    private var lastPlayingState: Boolean = false
    private var lastTrackStartMs: Long = 0L

    fun start() {
        if (workerJob != null) return
        workerJob = scope.launch {
            while (isActive) {
                try {
                    if (!DesktopStorage.discordRpcEnabled.value) {
                        closeConnection()
                        delay(2000)
                        continue
                    }

                    if (!isConnected) {
                        connect()
                    }

                    if (isConnected) {
                        updatePresenceIfNeeded()
                    }
                } catch (e: Exception) {
                    closeConnection()
                }
                delay(1500)
            }
        }
    }

    fun stop() {
        workerJob?.cancel()
        workerJob = null
        closeConnection()
    }

    private fun connect(): Boolean {
        closeConnection()
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        
        for (i in 0..9) {
            try {
                val pipePath = if (isWindows) {
                    "\\\\.\\pipe\\discord-ipc-$i"
                } else {
                    val xdg = System.getenv("XDG_RUNTIME_DIR")
                        ?: System.getenv("TMPDIR")
                        ?: System.getenv("TMP")
                        ?: System.getenv("TEMP")
                        ?: "/tmp"
                    "$xdg/discord-ipc-$i"
                }

                val file = File(pipePath)
                if (isWindows || file.exists()) {
                    val raf = RandomAccessFile(pipePath, "rw")
                    pipeFile = raf
                    
                    // Отправка Handshake (Opcode 0)
                    val handshake = JSONObject().apply {
                        put("v", 1)
                        put("client_id", CLIENT_ID)
                    }
                    sendFrame(0, handshake.toString())

                    // Чтение ответа Handshake
                    val response = readFrame()
                    if (response != null) {
                        isConnected = true
                        lastTrackUrl = null // Сбросить, чтобы обновить активность
                        println("[DiscordRpc] Connected to Discord successfully via pipe $i!")
                        return true
                    } else {
                        raf.close()
                    }
                }
            } catch (_: Exception) {
                // Пробуем следующий пайп
            }
        }
        return false
    }

    private fun closeConnection() {
        if (isConnected) {
            runCatching {
                // Очистить присутствие перед выходом
                sendClearPresence()
            }
        }
        isConnected = false
        runCatching { pipeFile?.close() }
        pipeFile = null
    }

    fun updateNow() {
        scope.launch {
            if (isConnected) {
                lastTrackUrl = null
                updatePresenceIfNeeded()
            }
        }
    }

    private fun updatePresenceIfNeeded() {
        val track = DesktopAudioPlayer.currentTrack
        val isPlaying = DesktopAudioPlayer.isPlaying

        if (track == null) {
            if (lastTrackUrl != null) {
                sendClearPresence()
                lastTrackUrl = null
                lastPlayingState = false
            }
            return
        }

        val trackChanged = track.originalUrl != lastTrackUrl
        val stateChanged = isPlaying != lastPlayingState

        if (trackChanged || stateChanged) {
            lastTrackUrl = track.originalUrl
            lastPlayingState = isPlaying
            if (trackChanged) {
                lastTrackStartMs = System.currentTimeMillis() - DesktopAudioPlayer.currentPositionMs
            }

            sendPresence(track, isPlaying, lastTrackStartMs)
        }
    }

    private fun sendPresence(track: ResolvedTrack, isPlaying: Boolean, startTimestampMs: Long) {
        val now = System.currentTimeMillis()
        val currentPos = DesktopAudioPlayer.currentPositionMs
        val durationMs = DesktopAudioPlayer.durationMs.takeIf { it > 0 } ?: (track.durationSeconds * 1000L)

        val activity = JSONObject().apply {
            put("type", 2) // 2 = LISTENING (в профиле Discord отображается "Слушает Melo", а не "Играет")
            put("details", track.title.take(128))
            put("state", (track.artist?.takeIf { it.isNotBlank() } ?: "Melo Music").take(128))

            val assets = JSONObject().apply {
                val thumb = track.thumbnailUrl
                if (!thumb.isNullOrBlank() && (thumb.startsWith("http://") || thumb.startsWith("https://"))) {
                    put("large_image", thumb)
                    put("large_text", track.title.take(128))
                    put("small_image", thumb)
                    put("small_text", if (isPlaying) "Воспроизведение" else "Пауза")
                } else {
                    put("large_image", "melo_logo")
                    put("large_text", track.title.take(128))
                }
            }
            put("assets", assets)

            if (isPlaying && durationMs > 0L) {
                val timestamps = JSONObject().apply {
                    val startSec = (now - currentPos) / 1000L
                    val endSec = startSec + (durationMs / 1000L)
                    put("start", startSec)
                    put("end", endSec)
                }
                put("timestamps", timestamps)
            }
        }

        val payload = JSONObject().apply {
            put("cmd", "SET_ACTIVITY")
            put("nonce", UUID.randomUUID().toString())
            put("args", JSONObject().apply {
                put("pid", ProcessHandle.current().pid().toInt())
                put("activity", activity)
            })
        }

        println("[DiscordRpc] Sending presence: ${track.title} by ${track.artist}")
        sendFrame(1, payload.toString())
    }

    private fun sendClearPresence() {
        val payload = JSONObject().apply {
            put("cmd", "SET_ACTIVITY")
            put("nonce", UUID.randomUUID().toString())
            put("args", JSONObject().apply {
                put("pid", ProcessHandle.current().pid().toInt())
                put("activity", JSONObject.NULL)
            })
        }
        sendFrame(1, payload.toString())
    }

    private fun sendFrame(opcode: Int, jsonStr: String) {
        val raf = pipeFile ?: return
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(opcode)
            putInt(bytes.size)
        }.array()

        synchronized(this) {
            raf.write(header)
            raf.write(bytes)
        }

        // Вычитываем ответ Discord на команду, чтобы буфер пайпа не забивался
        scope.launch {
            synchronized(this@DiscordRpcClient) {
                runCatching {
                    readFrame()
                }
            }
        }
    }

    private fun readFrame(): String? {
        val raf = pipeFile ?: return null
        val header = ByteArray(8)
        raf.readFully(header)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val opcode = buf.int
        val length = buf.int

        if (length < 0 || length > 64 * 1024) return null
        val body = ByteArray(length)
        raf.readFully(body)
        return String(body, Charsets.UTF_8)
    }
}

