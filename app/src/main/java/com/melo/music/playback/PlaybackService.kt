package com.melo.music.playback

import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Фоновый плеер на базе Media3. Держит [ExoPlayer] и [MediaSession],
 * чтобы воспроизведение продолжалось при свёрнутом приложении и управлялось
 * с экрана блокировки / наушников / системного уведомления.
 *
 * На следующих шагах сюда придёт DataSource, отдающий стрим-URL, который
 * yt-dlp резолвит на устройстве.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // YouTube/googlevideo отдают поток капризно — задаём «браузерный» User-Agent
        // и разрешаем кросс-протокольные редиректы.
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) " +
                    "Gecko/20100101 Firefox/91.0",
            )
            .setAllowCrossProtocolRedirects(true)
        // Меньше буфера до старта — звук начинается раньше, как только есть URL.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000,
            )
            .build()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .setLoadControl(loadControl)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
