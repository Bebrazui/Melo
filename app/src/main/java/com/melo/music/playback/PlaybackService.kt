package com.melo.music.playback

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.melo.music.audio.EqualizerManager
import com.melo.music.byedpi.ByeDpiProxy
import com.melo.music.extractor.TrackItem
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

class PlaybackService : MediaSessionService() {

    companion object {
        var audioSessionId: Int = 0
            private set

        /** Текущая очередь. */
        var queue: List<TrackItem> = emptyList()
        var queueIndex: Int = 0
            private set

        var onQueueChanged: ((index: Int) -> Unit)? = null

        /** Вызывается при окончании трека без кроссфейда (фолбэк). */
        var onTrackEnded: (() -> Unit)? = null

        /** Переключение треков из наушников / уведомления (резолвит и играет UI). */
        var onSkipNext: (() -> Unit)? = null
        var onSkipPrev: (() -> Unit)? = null

        /** Кроссфейд: длительность наложения (мс). 0 = выключен. */
        var crossfadeMs: Int = 4000

        // Следующий трек, заранее зарезолвленный в UI (для кроссфейда).
        @Volatile var nextUrl: String? = null
        @Volatile var nextTitle: String? = null
        @Volatile var nextArtwork: String? = null
        @Volatile var nextIndexPending: Int = -1
        @Volatile var nextSpeed: Float = 1f

        /** Текущая скорость/тон (1.0 = оригинал). Чтобы сервис применял её сам. */
        @Volatile var playbackSpeed: Float = 1f

        /** Вызывается, когда кроссфейд переключил на следующий трек. */
        var onCrossfadeAdvance: ((index: Int) -> Unit)? = null

        fun setNext(url: String?, title: String?, index: Int, speed: Float = 1f, artwork: String? = null) {
            nextUrl = url
            nextTitle = title
            nextArtwork = artwork
            nextIndexPending = index
            nextSpeed = speed
        }

        fun clearNext() {
            nextUrl = null
            nextTitle = null
            nextArtwork = null
            nextIndexPending = -1
            nextSpeed = 1f
        }

        fun setQueue(list: List<TrackItem>, index: Int) {
            queue = list
            queueIndex = index
        }

        /** Команда сервису: отменить текущий кроссфейд (при ручном переключении). */
        var cancelCrossfadeCmd: (() -> Unit)? = null
        fun cancelCrossfade() = cancelCrossfadeCmd?.invoke()

        // ── Таймер сна ──────────────────────────────────────────────────────
        /** Момент засыпания (elapsedRealtime, мс). 0 = таймер выключен. */
        @Volatile var sleepEndAt: Long = 0L
            private set
        /** Режим «до конца трека»: пауза по окончании текущего трека. */
        @Volatile var sleepEndOfTrack: Boolean = false
            private set
        /** Длительность плавного затухания перед паузой (мс). */
        const val SLEEP_FADE_MS = 8_000L

        fun setSleepTimerMinutes(minutes: Int) {
            sleepEndOfTrack = false
            sleepEndAt = if (minutes > 0) android.os.SystemClock.elapsedRealtime() + minutes * 60_000L else 0L
        }
        fun setSleepEndOfTrack() { sleepEndOfTrack = true; sleepEndAt = 0L }
        fun cancelSleepTimer() { sleepEndAt = 0L; sleepEndOfTrack = false }
        fun sleepActive(): Boolean = sleepEndAt > 0L || sleepEndOfTrack
        /** Остаток до засыпания (мс), 0 если по минутам не задан. */
        fun sleepRemainingMs(): Long =
            if (sleepEndAt > 0L) (sleepEndAt - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L) else 0L
    }

    private var mediaSession: MediaSession? = null
    private lateinit var playerA: ExoPlayer
    private lateinit var playerB: ExoPlayer
    private var usingA = true
    private val active: ExoPlayer get() = if (usingA) playerA else playerB
    private val standby: ExoPlayer get() = if (usingA) playerB else playerA

    private var crossfading = false
    private val handler = Handler(Looper.getMainLooper())

    // Подсчёт нажатий на кнопку гарнитуры: 1 = play/pause, 2 = next, 3+ = prev.
    private var tapCount = 0
    private var tapRunnable: Runnable? = null
    private val tapWindowMs = 320L

    override fun onCreate() {
        super.onCreate()

        // Аудио через ByeDPI + системный DNS. HTTP/1.1 — десинк ломает HTTP/2-потоки.
        // Длинные таймауты + переиспользование соединений: десинк капризен на НОВЫХ
        // коннектах, поэтому держим установленные дольше и гоняем по ним все сегменты.
        val okClient = OkHttpClient.Builder()
            .proxySelector(com.melo.music.net.MeloNet.byedpiSelector)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(okhttp3.ConnectionPool(12, 5, java.util.concurrent.TimeUnit.MINUTES))
            .addInterceptor { chain ->
                val req = chain.request()
                val host = req.url.host
                try {
                    val resp = chain.proceed(req)
                    val tag = if (resp.code !in 200..299) req.url.toString().take(180) else host
                    // android.util.Log.e("MeloPlay", "${resp.code} <- $tag")
                    resp
                } catch (e: Exception) {
                    // android.util.Log.e("MeloPlay", "$host FAILED: ${e.javaClass.simpleName}: ${e.message}")
                    throw e
                }
            }
            .build()
        val httpFactory = OkHttpDataSource.Factory(okClient)
            .setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) " +
                    "Gecko/20100101 Firefox/91.0",
            )
        // DefaultDataSource: локальные офлайн-файлы (content://, file://) играем напрямую,
        // сетевые ссылки уходят в httpFactory (через ByeDPI).
        val dsFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpFactory)
        // 4xx (403/401/404) = протухшая/невалидная ссылка — НЕ транзиентно: не долбим,
        // сразу отдаём ошибку вверх → UI инвалидирует кэш и пере-резолвит свежую ссылку.
        // Транзиентные сетевые сбои (таймаут/RST) повторяем пару раз с короткой паузой.
        val loadErrorPolicy = object : androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy() {
            override fun getRetryDelayMsFor(
                info: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo,
            ): Long {
                val ex = info.exception
                if (ex is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException &&
                    ex.responseCode in 400..499
                ) {
                    return androidx.media3.common.C.TIME_UNSET // не повторять — пусть пере-резолвит
                }
                return minOf(info.errorCount * 500L, 2_000L)
            }
        }
        // Важно: у каждого плеера СВОЙ LoadControl (общий требует общий поток).
        fun buildPlayer() = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dsFactory).setLoadErrorHandlingPolicy(loadErrorPolicy),
            )
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15_000, 50_000, 500, 1_000)
                    .build(),
            )
            .build()

        playerA = buildPlayer()
        playerB = buildPlayer()

        active.addListener(endListener)

        audioSessionId = active.audioSessionId
        EqualizerManager.attach(audioSessionId)
        applyReverb(active)
        mediaSession = MediaSession.Builder(this, active)
            .setCallback(mediaButtonCallback)
            .setBitmapLoader(CoilBitmapLoader(this))
            .build()

        // Изменили реверб в настройках → переустановить aux-send на активном плеере.
        EqualizerManager.onReverbChanged = { handler.post { applyReverb(active) } }

        cancelCrossfadeCmd = { handler.post { abortCrossfade() } }
        handler.post(crossfadeTick)
    }

    /**
     * Управление с гарнитуры: ловим кнопочные события и сами считаем тапы,
     * т.к. система не везде раскладывает мульти-тап на NEXT/PREVIOUS.
     */
    private val mediaButtonCallback = object : MediaSession.Callback {
        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent,
        ): Boolean {
            val key: KeyEvent? = if (android.os.Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }
            if (key == null || key.action != KeyEvent.ACTION_DOWN || key.repeatCount != 0) {
                return true
            }
            when (key.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> handler.post { onSkipNext?.invoke() }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> handler.post { onSkipPrev?.invoke() }
                KeyEvent.KEYCODE_MEDIA_PLAY -> handler.post { active.play() }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> handler.post { active.pause() }
                KeyEvent.KEYCODE_MEDIA_STOP -> handler.post { active.pause() }
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                -> handler.post { onHeadsetTap() }
                else -> return false
            }
            return true
        }
    }

    /** Накопление тапов в окне [tapWindowMs] и реакция по их числу. */
    private fun onHeadsetTap() {
        tapCount++
        tapRunnable?.let { handler.removeCallbacks(it) }
        val r = Runnable {
            when (tapCount) {
                1 -> if (active.isPlaying) active.pause() else active.play()
                2 -> onSkipNext?.invoke()
                else -> onSkipPrev?.invoke()
            }
            tapCount = 0
        }
        tapRunnable = r
        handler.postDelayed(r, tapWindowMs)
    }

    /** Посыл звука плеера в глобальный aux-реверб (или отключение). */
    private fun applyReverb(player: ExoPlayer) {
        runCatching {
            player.setAuxEffectInfo(
                AuxEffectInfo(EqualizerManager.reverbEffectId(), EqualizerManager.reverbSendLevel()),
            )
        }
    }

    private val endListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            com.melo.music.widget.WidgetUpdater.setPlaying(applicationContext, playing)
        }
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED && !crossfading) {
                // Таймер сна «до конца трека»: останавливаемся, а не идём дальше.
                if (sleepEndOfTrack) {
                    sleepEndOfTrack = false
                    active.pause()
                    return
                }
                // Log.d("MeloService", "STATE_ENDED (no crossfade)")
                onTrackEnded?.invoke()
            }
        }
    }

    /** Каждые 200мс проверяем, не пора ли начинать кроссфейд. */
    private val crossfadeTick = object : Runnable {
        override fun run() {
            try {
                val p = active
                val dur = p.duration
                val pos = p.currentPosition
                // Таймер сна по минутам: плавно гасим в конце и ставим паузу.
                if (sleepEndAt > 0L && p.isPlaying) {
                    val remain = sleepEndAt - android.os.SystemClock.elapsedRealtime()
                    when {
                        remain <= 0L -> {
                            p.pause()
                            p.volume = 1f
                            sleepEndAt = 0L
                        }
                        remain <= SLEEP_FADE_MS -> p.volume = (remain.toFloat() / SLEEP_FADE_MS).coerceIn(0.05f, 1f)
                    }
                }
                if (!crossfading &&
                    crossfadeMs > 0 &&
                    !sleepEndOfTrack &&
                    sleepEndAt == 0L &&
                    nextUrl != null &&
                    p.isPlaying &&
                    dur > 0 &&
                    dur - pos <= crossfadeMs &&
                    dur - pos > 0
                ) {
                    startCrossfade()
                }
            } catch (_: Exception) {
            }
            handler.postDelayed(this, 200)
        }
    }

    private fun startCrossfade() {
        val url = nextUrl ?: return
        val title = nextTitle
        val artwork = nextArtwork
        val advanceIndex = nextIndexPending
        val speed = nextSpeed
        crossfading = true
        clearNext()

        val fromP = active
        val toP = standby

        val meta = MediaMetadata.Builder().setTitle(title ?: "")
        artwork?.let { meta.setArtworkUri(android.net.Uri.parse(it)) }
        toP.setMediaItem(
            MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(meta.build())
                .build(),
        )
        // Скорость/тон следующего трека применяем на уровне сервиса — работает и в фоне.
        toP.playbackParameters = androidx.media3.common.PlaybackParameters(speed, speed)
        playbackSpeed = speed
        toP.volume = 0f
        toP.prepare()
        toP.play()

        // Переключаем сессию/UI на новый трек СРАЗУ (меняется заранее, во время наложения).
        usingA = !usingA
        fromP.removeListener(endListener)
        toP.addListener(endListener)
        mediaSession?.player = toP
        audioSessionId = toP.audioSessionId
        EqualizerManager.attach(audioSessionId)
        applyReverb(toP)
        if (advanceIndex >= 0) onCrossfadeAdvance?.invoke(advanceIndex)

        // Плавно гасим старый, проявляем новый.
        val duration = crossfadeMs.toLong()
        val start = System.currentTimeMillis()
        val fade = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - start
                val frac = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                fromP.volume = 1f - frac
                toP.volume = frac
                if (frac < 1f && crossfading) {
                    handler.postDelayed(this, 60)
                } else {
                    fromP.stop()
                    fromP.clearMediaItems()
                    fromP.volume = 1f
                    toP.volume = 1f
                    crossfading = false
                }
            }
        }
        handler.post(fade)
    }

    /** Прерывает кроссфейд (ручное переключение трека). */
    private fun abortCrossfade() {
        if (!crossfading) return
        crossfading = false
        standby.stop()
        standby.clearMediaItems()
        standby.volume = 1f
        active.volume = 1f
        clearNext()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        onTrackEnded = null
        onSkipNext = null
        onSkipPrev = null
        onQueueChanged = null
        onCrossfadeAdvance = null
        cancelCrossfadeCmd = null
        EqualizerManager.onReverbChanged = null
        clearNext()
        queue = emptyList()
        queueIndex = 0
        EqualizerManager.release()
        mediaSession?.release()
        runCatching { playerA.release() }
        runCatching { playerB.release() }
        mediaSession = null
        super.onDestroy()
    }
}
