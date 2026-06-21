package com.melo.music.playback

import android.util.Log
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

        /** Вызывается при смене трека (в т.ч. автопереход). */
        var onQueueChanged: ((index: Int) -> Unit)? = null

        /**
         * Вызывается при окончании трека (STATE_ENDED).
         * UI подписывается сюда для автоперехода.
         */
        var onTrackEnded: (() -> Unit)? = null

        fun setQueue(list: List<TrackItem>, index: Int) {
            queue = list
            queueIndex = index
        }
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Аудио тоже идёт через ByeDPI (когда включён и запущен), иначе напрямую —
        // динамический ProxySelector решает на каждое соединение.
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
        val httpDataSourceFactory = OkHttpDataSource.Factory(okClient)
            .setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) " +
                    "Gecko/20100101 Firefox/91.0",
            )
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                50_000,
                500,
                1_000,
            )
            .build()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .setLoadControl(loadControl)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    Log.d("MeloService", "STATE_ENDED fired, queue.size=${queue.size}, index=$queueIndex")
                    onTrackEnded?.invoke()
                }
            }
        })

        audioSessionId = player.audioSessionId
        EqualizerManager.attach(audioSessionId)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        onTrackEnded = null
        onQueueChanged = null
        queue = emptyList()
        queueIndex = 0
        EqualizerManager.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
