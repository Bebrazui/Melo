package com.melo.music.playback

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
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
import java.net.URI
import okio.buffer

class PlaybackService : MediaSessionService() {

    companion object {
        var audioSessionId: Int = 0
            private set

        /** Текущая очередь. */
        var queue: List<TrackItem> = emptyList()
        var queueIndex: Int = 0
            set
        @Volatile var currentTrackItem: TrackItem? = null

        var onQueueChanged: ((index: Int) -> Unit)? = null

        /** Вызывается при окончании трека без кроссфейда (фолбэк). */
        var onTrackEnded: (() -> Unit)? = null

        var onToggleFavorite: (() -> Unit)? = null

        /** Переключение треков из наушников / уведомления (резолвит и играет UI). */
        var onSkipNext: (() -> Unit)? = null
        var onSkipPrev: (() -> Unit)? = null

        /** Кроссфейд: длительность наложения (мс). 0 = выключен. */
        var crossfadeMs: Int = 4000

        // Следующий трек, заранее зарезолвленный в UI (для кроссфейда).
        @Volatile var nextUrl: String? = null
        @Volatile var nextTitle: String? = null
        @Volatile var nextArtwork: String? = null
        @Volatile var nextArtist: String? = null
        @Volatile var nextIndexPending: Int = -1
        @Volatile var nextSpeed: Float = 1f

        /** Текущая скорость/тон (1.0 = оригинал). Чтобы сервис применял её сам. */
        @Volatile var playbackSpeed: Float = 1f

        /** Вызывается, когда кроссфейд переключил на следующий трек. */
        var onCrossfadeAdvance: ((index: Int) -> Unit)? = null

        fun setNext(url: String?, title: String?, index: Int, speed: Float = 1f, artwork: String? = null, artist: String? = null) {
            nextUrl = url
            nextTitle = title
            nextArtwork = artwork
            nextArtist = artist
            nextIndexPending = index
            nextSpeed = speed
        }

        fun clearNext() {
            nextUrl = null
            nextTitle = null
            nextArtwork = null
            nextArtist = null
            nextIndexPending = -1
            nextSpeed = 1f
        }

        fun setQueue(list: List<TrackItem>, index: Int) {
            queue = list
            queueIndex = index
            currentTrackItem = list.getOrNull(index)
            EqualizerManager.setBassBoostEnabled(currentTrackItem?.bassBoost ?: false)
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

        @Volatile var activePlayerProvider: (() -> ExoPlayer?)? = null
        val activePlayer: ExoPlayer? get() = activePlayerProvider?.invoke()

        @Volatile var currentAudioStreamUrl: String? = null

        var switchStreamUrlCmd: ((url: String, positionMs: Long) -> Unit)? = null
        fun switchStream(url: String, positionMs: Long) {
            switchStreamUrlCmd?.invoke(url, positionMs)
        }

        fun switchToVideo(videoUrl: String) {
            val player = activePlayer ?: return
            val pos = player.currentPosition
            switchStream(videoUrl, pos)
        }

        fun switchToAudio() {
            val player = activePlayer ?: return
            val audio = currentAudioStreamUrl ?: return
            val pos = player.currentPosition
            switchStream(audio, pos)
        }
    }

    private var mediaSession: MediaSession? = null
    private lateinit var playerA: ExoPlayer
    private lateinit var playerB: ExoPlayer
    private var usingA = true
    private val active: ExoPlayer get() = if (usingA) playerA else playerB
    private val standby: ExoPlayer get() = if (usingA) playerB else playerA

    private var crossfading = false
    private val handler = Handler(Looper.getMainLooper())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Подсчёт нажатий на кнопку гарнитуры: 1 = play/pause, 2 = next, 3+ = prev.
    private var tapCount = 0
    private var tapRunnable: Runnable? = null
    // Защита от дребезга / двойного переключения (debounce 350ms)
    private var lastSkipTimeMs = 0L
    private fun safeSkipNext() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSkipTimeMs < 350L) return
        lastSkipTimeMs = now
        onSkipNext?.invoke()
    }
    private fun safeSkipPrev() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSkipTimeMs < 350L) return
        lastSkipTimeMs = now
        onSkipPrev?.invoke()
    }

    private inner class MeloForwardingPlayer(player: Player) : androidx.media3.common.ForwardingPlayer(player) {
        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands().buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_PLAY_PAUSE)
                .build()
        }
        override fun isCommandAvailable(command: Int): Boolean {
            if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) return true
            return super.isCommandAvailable(command)
        }
        override fun seekToNext() {
            handler.post { safeSkipNext() }
        }
        override fun seekToPrevious() {
            handler.post { safeSkipPrev() }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Аудио через ByeDPI + системный DNS. HTTP/1.1 — десинк ломает HTTP/2-потоки.
        // Длинные таймауты + переиспользование соединений: десинк капризен на НОВЫХ
        // коннектах, поэтому держим установленные дольше и гоняем по ним все сегменты.
        val okClient = OkHttpClient.Builder()
            .dns(com.melo.music.net.MeloNet.dns)
            .proxySelector(com.melo.music.net.MeloNet.byedpiSelector)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(okhttp3.ConnectionPool(16, 25, java.util.concurrent.TimeUnit.SECONDS))
            .addInterceptor { chain ->
                var req = chain.request()
                val host = req.url.host
                if (com.melo.music.auth.YouTubeAccountManager.isLoggedIn) {
                    if (host.contains("youtube.com") && !host.contains("googlevideo.com")) {
                        com.melo.music.auth.YouTubeAccountManager.getCookies()?.let { cookies ->
                            val curCookie = req.header("Cookie")
                            val newCookie = if (!curCookie.isNullOrBlank()) "$curCookie; $cookies" else cookies
                            req = req.newBuilder().header("Cookie", newCookie).build()
                        }
                    }
                }
                val t0 = System.currentTimeMillis()
                try {
                    val resp = chain.proceed(req)
                    val dt = System.currentTimeMillis() - t0
                    if (host.contains("sndcdn") || host.contains("soundcloud")) {
                        com.melo.music.util.FileLog.d("MeloPlay", "SC chunk ${resp.code} in ${dt}ms: ${req.url.pathSegments.lastOrNull()}")
                    }
                    val tag = if (resp.code !in 200..299) req.url.toString().take(180) else host
                    if (resp.code !in 200..299) {
                        android.util.Log.e("MeloPlay", "HTTP ${resp.code} <- $tag")
                    }
                    resp
                } catch (e: Exception) {
                    val dt = System.currentTimeMillis() - t0
                    android.util.Log.e("MeloPlay", "$host FAILED after ${dt}ms: ${e.javaClass.simpleName}: ${e.message}")
                    com.melo.music.util.FileLog.e("MeloPlay", "$host FAILED after ${dt}ms: ${e.javaClass.simpleName}: ${e.message}")
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
        // Транзиентные сетевые сбои (таймаут/RST/потеря сети) повторяем агрессивно,
        // чтобы при пропадании и появлении интернета плеер не вылетал в STATE_IDLE.
        val loadErrorPolicy = object : androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy() {
            override fun getMinimumLoadableRetryCount(dataType: Int): Int = 10

            override fun getRetryDelayMsFor(
                info: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo,
            ): Long {
                val ex = info.exception
                if (ex is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException &&
                    ex.responseCode in 400..499
                ) {
                    return androidx.media3.common.C.TIME_UNSET // 4xx: протухла ссылка — пусть пере-резолвит
                }
                return minOf(info.errorCount * 1_000L, 5_000L)
            }
        }
        // Важно: у каждого плеера СВОЙ LoadControl (общий требует общий поток).
        fun buildPlayer(): ExoPlayer {
            val vocalProcessor = com.melo.music.audio.VocalCutAudioProcessor()
            com.melo.music.audio.VocalCutManager.register(vocalProcessor)
            val dspProcessor = com.melo.music.audio.MeloDspAudioProcessor()
            com.melo.music.audio.EqualizerManager.registerDsp(dspProcessor)

            val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
                override fun buildAudioSink(
                    context: android.content.Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): androidx.media3.exoplayer.audio.AudioSink {
                    return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .setAudioProcessors(arrayOf(vocalProcessor, dspProcessor))
                        .build()
                }
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            return ExoPlayer.Builder(this, renderersFactory)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(dsFactory).setLoadErrorHandlingPolicy(loadErrorPolicy),
                )
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            20_000,
                            60_000,
                            800,
                            1_000,
                        )
                        .setPrioritizeTimeOverSizeThresholds(false)
                        .build(),
                )
                .build()
        }

        playerA = buildPlayer()
        playerB = buildPlayer()

        activePlayerProvider = { active }
        switchStreamUrlCmd = { streamUrl, pos ->
            handler.post {
                val curMeta = active.mediaMetadata
                val isHls = com.melo.music.extractor.isHlsUrl(streamUrl)
                val item = MediaItem.Builder()
                    .setUri(streamUrl)
                    .apply {
                        if (isHls) {
                            setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                        }
                    }
                    .setMediaMetadata(curMeta)
                    .build()
                active.setMediaItem(item, pos)
                active.prepare()
                active.play()
            }
        }

        active.addListener(endListener)

        audioSessionId = active.audioSessionId
        EqualizerManager.attach(audioSessionId)
        applyReverb(active)

        val likeBtn = androidx.media3.session.CommandButton.Builder()
            .setDisplayName("Избранное")
            .setSessionCommand(androidx.media3.session.SessionCommand("ACTION_TOGGLE_LIKE", android.os.Bundle()))
            .build()

        mediaSession = MediaSession.Builder(this, MeloForwardingPlayer(active))
            .setCallback(mediaButtonCallback)
            .setCustomLayout(listOf(likeBtn))
            .setBitmapLoader(CoilBitmapLoader(this))
            .build()

        // Изменили реверб в настройках → переустановить aux-send на активном плеере.
        EqualizerManager.onReverbChanged = { handler.post { applyReverb(active) } }

        cancelCrossfadeCmd = { handler.post { abortCrossfade() } }
        handler.post(crossfadeTick)

        // Слушаем восстановление сети для автоматического продолжения трека
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        if (networkCallback != null) return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handler.post {
                    retryPlaybackIfStalled()
                }
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    handler.post {
                        retryPlaybackIfStalled()
                    }
                }
            }
        }
        cm.registerDefaultNetworkCallback(cb)
        networkCallback = cb
    }

    /**
     * Если воспроизведение прервалось из-за отвала сети или плеер завис в STATE_IDLE/ошибке —
     * при возвращении интернета восстанавливаем проигрывание с той же секунды.
     */
    private fun retryPlaybackIfStalled() {
        val p = active
        val error = p.playerError
        val idle = p.playbackState == Player.STATE_IDLE
        val playWhenReady = p.playWhenReady
        if (error != null || (idle && playWhenReady)) {
            val curPos = p.currentPosition
            com.melo.music.util.FileLog.i("MeloPlay", "Network restored: retrying playback at pos=$curPos")
            p.prepare()
            if (curPos > 0) p.seekTo(curPos)
            p.play()
        }
    }

    /**
     * Управление с гарнитуры, Dynamic Island и системы:
     */
    private val mediaButtonCallback = object : MediaSession.Callback {
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_PREVIOUS -> {
                    // Разрешаем команду. Media3 автоматически выполнит её через
                    // MeloForwardingPlayer (seekToNext/seekToPrevious) без дублирования.
                    return androidx.media3.session.SessionResult.RESULT_SUCCESS
                }
            }
            return super.onPlayerCommandRequest(session, controllerInfo, playerCommand)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            customCommand: androidx.media3.session.SessionCommand,
            args: android.os.Bundle,
        ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
            if (customCommand.customAction == "ACTION_TOGGLE_LIKE") {
                handler.post { onToggleFavorite?.invoke() }
                return com.google.common.util.concurrent.Futures.immediateFuture(
                    androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
                )
            }
            return super.onCustomCommand(session, controllerInfo, customCommand, args)
        }

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
                KeyEvent.KEYCODE_MEDIA_NEXT -> handler.post { safeSkipNext() }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> handler.post { safeSkipPrev() }
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

    /** Накопление тапов в окне tapWindowMs и реакция по их числу. */
    private fun onHeadsetTap() {
        tapCount++
        tapRunnable?.let { handler.removeCallbacks(it) }
        val r = Runnable {
            when (tapCount) {
                1 -> if (active.isPlaying) active.pause() else active.play()
                2 -> safeSkipNext()
                else -> safeSkipPrev()
            }
            tapCount = 0
        }
        tapRunnable = r
        handler.postDelayed(r, 320L)
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
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.e("MeloPlay", "ExoPlayer error: ${error.errorCodeName} (${error.errorCode}): ${error.message}", error)
            com.melo.music.util.FileLog.e("MeloPlay", "ExoPlayer error: ${error.errorCodeName} (${error.errorCode}): ${error.message}")
            // При ошибке сетевого тайм-аута или разрыва соединения пробуем мягко восстановиться через 2 секунды
            if (active.playWhenReady) {
                handler.postDelayed({
                    if (active.playWhenReady && active.playbackState == Player.STATE_IDLE) {
                        retryPlaybackIfStalled()
                    }
                }, 2000L)
            }
        }
        override fun onAudioSessionIdChanged(newSessionId: Int) {
            if (newSessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET && newSessionId != 0) {
                // Сессия создалась уже после старта наложения — цепляем эффекты сейчас.
                attachEffects(active)
            }
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

    private var standbyPreparedUrl: String? = null
    private var fadeRunnable: Runnable? = null

    /**
     * Цепляет эквалайзер/реверб к аудио-сессии плеера.
     * Раньше здесь подставлялся sessionId=0 (сессия нового плеера ещё не создана) —
     * EqualizerManager.attach() падал в catch, и после кроссфейда EQ/реверб отваливались.
     */
    private fun attachEffects(player: ExoPlayer) {
        val sid = player.audioSessionId
        if (sid == androidx.media3.common.C.AUDIO_SESSION_ID_UNSET || sid == 0) return
        audioSessionId = sid
        EqualizerManager.attach(sid)
        applyReverb(player)
    }

    /**
     * Прогревает standby-плеер за 10-12 секунд до конца трека, чтобы
     * соединение было свежим, буфер полным, а старт кроссфейда мгновенным.
     */
    private fun prepareStandbyIfReady() {
        val url = nextUrl ?: return
        if (standbyPreparedUrl == url && standby.playbackState != Player.STATE_IDLE) return
        val title = nextTitle
        val artwork = nextArtwork
        val artist = nextArtist
        val speed = nextSpeed
        val toP = standby

        val cleanA = artist?.let { com.melo.music.extractor.cleanArtistName(it) }
            ?: (title?.let { com.melo.music.extractor.cleanArtistName(null, it) })
            ?: ""
        val cleanT = title?.let { com.melo.music.extractor.cleanTrackTitle(it, cleanA) } ?: ""
        val meta = MediaMetadata.Builder()
            .setTitle(cleanT)
            .setDisplayTitle(cleanT)
            .setArtist(cleanA)
            .setAlbumArtist(cleanA)
            .setSubtitle(cleanA)
        artwork?.let { meta.setArtworkUri(android.net.Uri.parse(it)) }
        val isHls = com.melo.music.extractor.isHlsUrl(url)
        toP.stop()
        toP.clearMediaItems()
        toP.setMediaItem(
            MediaItem.Builder()
                .setUri(url)
                .apply {
                    if (isHls) {
                        setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                    }
                }
                .setMediaMetadata(meta.build())
                .build(),
        )
        toP.playbackParameters = androidx.media3.common.PlaybackParameters(speed, speed)
        toP.volume = 0f
        toP.playWhenReady = false
        toP.prepare()
        standbyPreparedUrl = url
        com.melo.music.util.FileLog.d("MeloFade", "Standby pre-buffered: $cleanT")
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
                    dur > 0
                ) {
                    val remainMs = dur - pos
                    // Начинаем прогрев standby задолго до конца, как только есть nextUrl
                    if (remainMs <= crossfadeMs + 25_000L && remainMs > crossfadeMs) {
                        prepareStandbyIfReady()
                    } else if (remainMs <= crossfadeMs) {
                        prepareStandbyIfReady()
                        startCrossfade()
                    }
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
        val artist = nextArtist
        val advanceIndex = nextIndexPending
        val speed = nextSpeed
        crossfading = true
        nextUrl = null

        val fromP = active
        val toP = standby

        if (standbyPreparedUrl != url || toP.playbackState == Player.STATE_IDLE) {
            val cleanA = artist?.let { com.melo.music.extractor.cleanArtistName(it) }
                ?: (title?.let { com.melo.music.extractor.cleanArtistName(null, it) })
                ?: ""
            val cleanT = title?.let { com.melo.music.extractor.cleanTrackTitle(it, cleanA) } ?: ""
            val meta = MediaMetadata.Builder()
                .setTitle(cleanT)
                .setDisplayTitle(cleanT)
                .setArtist(cleanA)
                .setAlbumArtist(cleanA)
                .setSubtitle(cleanA)
            artwork?.let { meta.setArtworkUri(android.net.Uri.parse(it)) }
            val isHls = com.melo.music.extractor.isHlsUrl(url)
            toP.stop()
            toP.clearMediaItems()
            toP.setMediaItem(
                MediaItem.Builder()
                    .setUri(url)
                    .apply {
                        if (isHls) {
                            setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                        }
                    }
                    .setMediaMetadata(meta.build())
                    .build(),
            )
            toP.playbackParameters = androidx.media3.common.PlaybackParameters(speed, speed)
            toP.prepare()
        }

        currentAudioStreamUrl = url
        toP.volume = 0f
        toP.playWhenReady = true

        // Переключаем сессию/UI на новый трек СРАЗУ (меняется заранее, во время наложения).
        usingA = !usingA
        fromP.removeListener(endListener)
        toP.addListener(endListener)
        mediaSession?.player = MeloForwardingPlayer(toP)
        // Эффекты цепляем только к валидной audio-сессии нового плеера.
        attachEffects(toP)
        if (advanceIndex >= 0) {
            queueIndex = advanceIndex
            currentTrackItem = queue.getOrNull(advanceIndex)
            EqualizerManager.setBassBoostEnabled(currentTrackItem?.bassBoost ?: false)
            onCrossfadeAdvance?.invoke(advanceIndex)
        }

        fun runFade() {
            val duration = crossfadeMs.toLong().coerceAtLeast(600L)
            val start = SystemClock.elapsedRealtime()
            val fade = object : Runnable {
                override fun run() {
                    val elapsed = SystemClock.elapsedRealtime() - start
                    val frac = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                    // Равномощный синусный кроссфейд: сумма энергий 1.0 без провала громкости в центре
                    val rad = frac * (Math.PI.toFloat() / 2f)
                    fromP.volume = kotlin.math.cos(rad)
                    toP.volume = kotlin.math.sin(rad)
                    if (frac < 1f && crossfading) {
                        // 16мс — обновление громкости чаще, чем буфер аудио (≈20мс):
                        // ступени на слух не слышны даже при просадке UI-потока.
                        handler.postDelayed(this, 16)
                    } else {
                        fadeRunnable = null
                        fromP.stop()
                        fromP.clearMediaItems()
                        fromP.volume = 1f
                        toP.volume = 1f
                        crossfading = false
                        standbyPreparedUrl = null
                        com.melo.music.util.FileLog.d("MeloFade", "Crossfade done -> idx=$advanceIndex")
                    }
                }
            }
            fadeRunnable = fade
            handler.post(fade)
        }

        if (toP.playbackState == Player.STATE_READY) {
            runFade()
        } else {
            // Если standby ещё догружается — НЕ гасим fromP в тишину!
            // Ждем готовности toP. Когда toP станет готов — запускаем плавный переход.
            val readyListener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        toP.removeListener(this)
                        if (crossfading) runFade()
                    }
                }
            }
            toP.addListener(readyListener)
            handler.postDelayed({
                toP.removeListener(readyListener)
                if (!crossfading) return@postDelayed
                if (toP.playbackState == Player.STATE_READY) {
                    runFade()
                } else if (fromP.playbackState == Player.STATE_ENDED) {
                    // Старый трек уже физически кончился, а новый так и не готов за 8с — откатываемся на обычный fallback
                    com.melo.music.util.FileLog.w("MeloFade", "Standby not ready in 8s and old track ended, fallback")
                    rollbackCrossfade(fromP, toP)
                }
            }, 8000L)
        }
    }

    /** Откат неудачного кроссфейда: возвращаем управление старым треком без провала звука. */
    private fun rollbackCrossfade(fromP: ExoPlayer, toP: ExoPlayer) {
        crossfading = false
        fadeRunnable?.let { handler.removeCallbacks(it) }
        fadeRunnable = null
        toP.stop()
        toP.clearMediaItems()
        toP.volume = 1f
        toP.removeListener(endListener)
        fromP.addListener(endListener)
        fromP.volume = 1f
        fromP.playWhenReady = true
        standbyPreparedUrl = null
        usingA = !usingA
        mediaSession?.player = MeloForwardingPlayer(fromP)
        clearNext()
        // Старый трек к этому моменту уже доигран — отдаём UI, чтобы он переключил трек.
        if (fromP.playbackState == Player.STATE_ENDED) {
            onTrackEnded?.invoke()
        }
    }

    /** Прерывает кроссфейд (ручное переключение трека). */
    private fun abortCrossfade() {
        standbyPreparedUrl = null
        fadeRunnable?.let { handler.removeCallbacks(it) }
        fadeRunnable = null
        if (!crossfading) {
            standby.stop()
            standby.clearMediaItems()
            standby.volume = 1f
            clearNext()
            return
        }
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
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            runCatching { cm?.unregisterNetworkCallback(it) }
            networkCallback = null
        }
        clearNext()
        queue = emptyList()
        queueIndex = 0
        EqualizerManager.release()
        mediaSession?.release()
        runCatching { playerA.release() }
        runCatching { playerB.release() }
        activePlayerProvider = null
        switchStreamUrlCmd = null
        mediaSession = null
        super.onDestroy()
    }
}
