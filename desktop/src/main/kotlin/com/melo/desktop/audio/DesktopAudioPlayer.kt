package com.melo.desktop.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.melo.desktop.audio.cache.AudioCacheManager
import com.melo.desktop.extractor.ResolvedTrack
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Аудиоплеер для Melo Desktop на базе JavaFX Media.
 * Поддерживает прямое воспроизведение стримов через LocalStreamServer,
 * перемотку, регулировку громкости и скорости.
 */
object DesktopAudioPlayer {

    var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private var progressJob: Job? = null

    var currentTrack by mutableStateOf<ResolvedTrack?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isBuffering by mutableStateOf(false)
        private set

    var currentPositionMs by mutableStateOf(0L)
        private set

    var durationMs by mutableStateOf(0L)
        private set

    var volume by mutableStateOf(0.8f)
        private set

    var speed by mutableStateOf(1.0f)
        private set

    const val SPEED_SLOWED = 0.93f
    const val SPEED_ORIGINAL = 1.0f
    const val SPEED_SPEED_UP = 1.15f

    enum class RepeatMode { OFF, ALL, ONE }

    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set

    var isShuffle by mutableStateOf(false)
        private set

    var onTrackEnded: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun toggleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        isShuffle = !isShuffle
    }

    fun cyclePlaybackSpeed() {
        val next = when {
            kotlin.math.abs(speed - SPEED_ORIGINAL) < 0.02f -> SPEED_SPEED_UP
            kotlin.math.abs(speed - SPEED_SPEED_UP) < 0.02f -> SPEED_SLOWED
            else -> SPEED_ORIGINAL
        }
        setPlaybackSpeed(next)
    }

    init {
        try {
            Platform.startup {}
        } catch (_: Throwable) {
            // Игнорируем повторную инициализацию JavaFX
        }
    }

    fun setPreviewState(
        track: ResolvedTrack?,
        playing: Boolean = true,
        posMs: Long = 65000L,
        durMs: Long = 210000L,
        vol: Float = 0.8f,
    ) {
        currentTrack = track
        isPlaying = playing
        currentPositionMs = posMs
        durationMs = durMs
        volume = vol
    }

    /** Начать воспроизведение трека. */
    fun play(track: ResolvedTrack, startMs: Long = 0) {
        currentTrack = track
        var dSec = track.durationSeconds
        if (dSec <= 0L) {
            dSec = Regex("[?&]dur=([0-9]+(?:\\.[0-9]+)?)").find(track.audioUrl)?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: 0L
        }
        if (dSec > 0L) {
            durationMs = dSec * 1000L
        }
        val cachedFile = AudioCacheManager.getCachedFile(track.originalUrl)
        val playUrl = if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 64 * 1024) {
            println("[DesktopAudioPlayer] Instant play from disk cache: ${cachedFile.name} (${cachedFile.length() / 1024} KB)")
            cachedFile.toURI().toString()
        } else if (track.audioUrl.startsWith("http://") || track.audioUrl.startsWith("https://")) {
            AudioCacheManager.startBackgroundDownload(track.originalUrl, track.audioUrl)
            LocalStreamServer.proxyUrl(track.audioUrl, track.originalUrl)
        } else {
            File(track.audioUrl).toURI().toString()
        }

        setupAndStartPlayer(playUrl, startMs)
    }

    private fun setupAndStartPlayer(playUrl: String, startMs: Long) {
        Platform.runLater {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.dispose()

                val media = Media(playUrl)
                val player = MediaPlayer(media)
                mediaPlayer = player

                player.volume = volume.toDouble()
                player.rate = speed.toDouble()

                media.durationProperty().addListener { _, _, newDur ->
                    if (newDur != null && !newDur.isIndefinite && !newDur.isUnknown) {
                        val d = newDur.toMillis().toLong()
                        if (d > 0 && d != Long.MAX_VALUE) {
                            durationMs = d
                        }
                    }
                }

                player.setOnReady {
                    val d = media.duration.toMillis().toLong()
                    if (d > 0 && d != Long.MAX_VALUE) durationMs = d
                    if (startMs > 0) player.seek(Duration.millis(startMs.toDouble()))
                    player.play()
                    isPlaying = true
                    isBuffering = false
                    startProgressTracker()
                }

                player.setOnEndOfMedia {
                    val curPos = player.currentTime.toMillis().toLong()
                    val totalDur = player.media?.duration?.toMillis()?.toLong()?.takeIf { it > 0 } ?: durationMs
                    // Защита от ложного срабатывания EndOfMedia при задержке сетевого буфера стрима:
                    val isPremature = curPos < 5000L || (totalDur > 5000L && curPos < (totalDur * 0.90).toLong() && curPos < totalDur - 4000L)
                    if (isPremature) {
                        println("[DesktopAudioPlayer] Premature EndOfMedia at ${curPos}ms / ${totalDur}ms — recovering playback")
                        isBuffering = true
                        scope.launch(Dispatchers.Main) {
                            delay(500)
                            if (mediaPlayer == player) {
                                player.seek(Duration.millis(curPos.toDouble()))
                                player.play()
                            }
                        }
                        return@setOnEndOfMedia
                    }
                    if (repeatMode == RepeatMode.ONE) {
                        player.seek(Duration.ZERO)
                        player.play()
                        isPlaying = true
                        currentPositionMs = 0
                        return@setOnEndOfMedia
                    }
                    isPlaying = false
                    currentPositionMs = totalDur
                    onTrackEnded?.invoke()
                }

                player.setOnError {
                    val err = player.error?.message ?: "Playback error"
                    println("[DesktopAudioPlayer] Error: $err")
                    isPlaying = false
                    isBuffering = false
                    onError?.invoke(err)
                }

                player.setOnHalted {
                    isBuffering = true
                }

                player.setOnPlaying {
                    isPlaying = true
                    isBuffering = false
                }

                player.setOnPaused {
                    isPlaying = false
                }

            } catch (e: Exception) {
                System.err.println("[DesktopAudioPlayer] Failed to load media: ${e.message}")
                onError?.invoke(e.message ?: "Unknown media error")
            }
        }
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else resume()
    }

    fun pause() {
        Platform.runLater {
            mediaPlayer?.pause()
            isPlaying = false
            com.melo.desktop.discord.DiscordRpcClient.updateNow()
        }
    }

    fun resume() {
        Platform.runLater {
            mediaPlayer?.play()
            isPlaying = true
            com.melo.desktop.discord.DiscordRpcClient.updateNow()
        }
    }

    @Volatile
    private var lastSeekTimestamp: Long = 0L

    fun seekTo(positionMs: Long) {
        val maxDur = if (durationMs > 0) durationMs else Long.MAX_VALUE
        val clamped = positionMs.coerceIn(0L, maxDur)
        currentPositionMs = clamped
        lastSeekTimestamp = System.currentTimeMillis()

        val track = currentTrack
        val currentSource = mediaPlayer?.media?.source.orEmpty()
        val isLocalFile = currentSource.startsWith("file:")

        if (isLocalFile || track == null) {
            // Уже играет локальный файл: нативная мгновенная перемотка JavaFX
            Platform.runLater {
                try {
                    mediaPlayer?.seek(Duration.millis(clamped.toDouble()))
                } catch (e: Exception) {
                    System.err.println("[DesktopAudioPlayer] Seek error: ${e.message}")
                }
            }
        } else {
            // Играет сетевой стрим: проверяем/ожидаем завершения кэширования на диск
            val directCache = AudioCacheManager.getCachedFile(track.originalUrl)
            if (directCache != null && directCache.exists() && directCache.length() > 64 * 1024) {
                println("[DesktopAudioPlayer] Seamless switch to cached file for seek: ${directCache.name} at ${clamped}ms")
                setupAndStartPlayer(directCache.toURI().toString(), clamped)
            } else {
                // Ждём завершения быстрой фоновой загрузки (2.5 МБ качаются за ~300мс)
                scope.launch {
                    val cached = AudioCacheManager.awaitCachedFile(track.originalUrl, timeoutMs = 2500L)
                    if (cached != null && cached.exists() && cached.length() > 64 * 1024) {
                        println("[DesktopAudioPlayer] Background cache ready, switching player to ${cached.name} at ${clamped}ms")
                        setupAndStartPlayer(cached.toURI().toString(), clamped)
                    } else {
                        // Фолбэк: пробуем нативный seek в стриме
                        Platform.runLater {
                            mediaPlayer?.seek(Duration.millis(clamped.toDouble()))
                        }
                    }
                }
            }
        }
    }

    fun setPlayerVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        volume = clamped
        Platform.runLater {
            mediaPlayer?.volume = clamped.toDouble()
        }
    }

    fun setPlaybackSpeed(s: Float) {
        speed = s.coerceIn(0.5f, 2.0f)
        Platform.runLater {
            mediaPlayer?.rate = speed.toDouble()
        }
    }

    fun stop() {
        Platform.runLater {
            progressJob?.cancel()
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
            mediaPlayer = null
            isPlaying = false
            currentPositionMs = 0
            durationMs = 0
            currentTrack = null
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (isPlaying) {
                    val player = mediaPlayer
                    if (player != null) {
                        Platform.runLater {
                            // Не перезаписывать позицию, если недавно (< 1000мс) был выполнен seek
                            if (System.currentTimeMillis() - lastSeekTimestamp > 1000L) {
                                val pos = player.currentTime.toMillis().toLong()
                                val dur = player.media?.duration?.toMillis()?.toLong() ?: 0L
                                if (dur > 0 && durationMs <= 0) durationMs = dur
                                currentPositionMs = pos
                            }
                        }
                    }
                }
                delay(250)
            }
        }
    }
}
