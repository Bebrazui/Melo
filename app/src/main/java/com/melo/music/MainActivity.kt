package com.melo.music

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.NewPipeResolver
import com.melo.music.extractor.ResolvedTrack
import com.melo.music.extractor.SoundCloudFix
import com.melo.music.lyrics.LyricsRepository
import com.melo.music.playback.PlaybackService
import com.melo.music.ui.PlayerScreen
import com.melo.music.ui.theme.MeloTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    // Наблюдаемый — чтобы Compose подцепил Player.Listener, как только контроллер готов.
    private var controller by mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeloTheme {
                PlayerScreen(
                    onSearch = { query -> NewPipeResolver.search(this, query) },
                    onLoadRecommendations = { NewPipeResolver.recommendations(this) },
                    onLoadArtistTracks = { url -> NewPipeResolver.artistTracks(this, url) },
                    scGetId = { SoundCloudFix.currentId(this) },
                    onScSetManual = { id ->
                        withContext(Dispatchers.IO) { SoundCloudFix.setManual(this@MainActivity, id) }
                    },
                    onScRefresh = {
                        withContext(Dispatchers.IO) { SoundCloudFix.refresh(this@MainActivity) }
                    },
                    onResolveAudioUrl = { url -> Extractor.resolveAudioUrl(this, url) },
                    isCached = { url -> Extractor.isCached(url) },
                    onPrefetch = { url -> Extractor.prefetch(this, url) },
                    onFetchLyrics = { title, artist -> LyricsRepository.fetch(title, artist) },
                    onPlayResolved = ::playResolved,
                    onTogglePlayPause = ::togglePlayPause,
                    playerProvider = { controller },
                    audioSessionIdProvider = { PlaybackService.audioSessionId },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener(
            { controller = controllerFuture.get() },
            ContextCompat.getMainExecutor(this),
        )
    }

    override fun onStop() {
        controller = null
        MediaController.releaseFuture(controllerFuture)
        super.onStop()
    }

    private fun playResolved(track: ResolvedTrack) {
        val player = controller ?: return
        val item = MediaItem.Builder()
            .setUri(track.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(track.title).build(),
            )
            .build()
        android.util.Log.e("MeloPerf", "setMediaItem+prepare+play")
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    private fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }
}
