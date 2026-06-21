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
        @Volatile var nextIndexPending: Int = -1

        /** Вызывается, когда кроссфейд переключил на следующий трек. */
        var onCrossfadeAdvance: ((index: Int) -> Unit)? = null

        fun setNext(url: String?, title: String?, index: Int) {
            nextUrl = url
            nextTitle = title
            nextIndexPending = index
        }

        fun clearNext() {
            nextUrl = null
            nextTitle = null
            nextIndexPending = -1
        }

        fun setQueue(list: List<TrackItem>, index: Int) {
            queue = list
            queueIndex = index
        }

        /** Команда сервису: отменить текущий кроссфейд (при ручном переключении). */
        var cancelCrossfadeCmd: (() -> Unit)? = null
        fun cancelCrossfade() = cancelCrossfadeCmd?.invoke()
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

        // Аудио через ByeDPI (когда включён и запущен), иначе напрямую.
        val okClient = OkHttpClient.Builder()
            .proxySelector(object : ProxySelector() {
                override fun select(uri: URI): List<Proxy> {
                    return if (ByeDpiProxy.isEnabled() && ByeDpiProxy.isRunning()) {
                        listOf(ByeDpiProxy.getProxy())
                    } else {
                        listOf(Proxy.NO_PROXY)
                    }
                }
                override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) {}
            })
            .build()
        val httpFactory = OkHttpDataSource.Factory(okClient)
            .setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) " +
                    "Gecko/20100101 Firefox/91.0",
            )
        // DefaultDataSource: локальные офлайн-файлы (content://, file://) играем напрямую,
        // сетевые ссылки уходят в httpFactory (через ByeDPI).
        val dsFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpFactory)
        // Важно: у каждого плеера СВОЙ LoadControl (общий требует общий поток).
        fun buildPlayer() = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dsFactory))
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
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED && !crossfading) {
                Log.d("MeloService", "STATE_ENDED (no crossfade)")
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
                if (!crossfading &&
                    crossfadeMs > 0 &&
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
        val advanceIndex = nextIndexPending
        crossfading = true
        clearNext()

        val fromP = active
        val toP = standby

        toP.setMediaItem(
            MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title ?: "").build())
                .build(),
        )
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
