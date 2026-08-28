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
import com.melo.music.BuildConfig
import com.melo.music.auth.GoogleAuthHelper
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
    private var controller by mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        GoogleAuthHelper.init(this)

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture.addListener(
            { controller = controllerFuture.get() },
            ContextCompat.getMainExecutor(this),
        )

        setContent {
            MeloTheme {
                PlayerScreen(
                    onSearch = { query -> NewPipeResolver.search(this, query) },
                    onGetSuggestions = { query -> NewPipeResolver.getSuggestions(query) },
                    onLoadRecommendations = { NewPipeResolver.recommendations(this) },
                    onLoadArtistTracks = { artist -> NewPipeResolver.artistTracks(this, artist) },
                    onLoadArtistAlbums = { artist -> NewPipeResolver.artistAlbums(this, artist) },
                    onLoadSimilarArtists = { artist, seed -> NewPipeResolver.similarArtists(this, artist, seed) },
                    onLoadAlbumTracks = { url -> NewPipeResolver.albumTracks(this, url) },
                    onLoadShelf = { seed -> NewPipeResolver.shelf(this, seed) },
                    onRelatedTracks = { track -> NewPipeResolver.relatedTracks(this, track) },
                    onGoogleLogin = { GoogleAuthHelper.signIn(this) },
                    onSearchUsers = { q -> com.melo.music.profile.ProfilesRepository.search(q) },
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
                    onInvalidateCache = { url -> Extractor.invalidate(url) },
                    onFetchLyrics = { title, artist -> LyricsRepository.fetch(title, artist) },
                    onPlayResolved = ::playResolved,
                    onTogglePlayPause = ::togglePlayPause,
                    playerProvider = { controller },
                    audioSessionIdProvider = { PlaybackService.audioSessionId },
                    showGoogle = BuildConfig.FLAVOR == "google",
                )
            }
        }
    }

    override fun onDestroy() {
        MediaController.releaseFuture(controllerFuture)
        super.onDestroy()
    }

    private fun playResolved(track: ResolvedTrack) {
        val player = controller ?: return
        android.util.Log.e("MeloArt", "playResolved title=${track.title.take(40)} thumb=${track.thumbnailUrl}")
        val meta = MediaMetadata.Builder().setTitle(track.title)
        track.thumbnailUrl?.let { meta.setArtworkUri(android.net.Uri.parse(it)) }
        val item = MediaItem.Builder()
            .setUri(track.audioUrl)
            .setMediaMetadata(meta.build())
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    private fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }
}
