package com.melo.music

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.common.util.concurrent.ListenableFuture
import com.melo.music.auth.AuthManager
import com.melo.music.extractor.Extractor
import com.melo.music.extractor.NewPipeResolver
import com.melo.music.extractor.ResolvedTrack
import com.melo.music.extractor.SoundCloudFix
import com.melo.music.lyrics.LyricsRepository
import com.melo.music.playback.PlaybackService
import com.melo.music.ui.PlayerScreen
import com.melo.music.ui.theme.MeloTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller by mutableStateOf<MediaController?>(null)

    private lateinit var googleLauncher: ActivityResultLauncher<Intent>
    private var googleResult: CompletableDeferred<Result<Unit>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Классический Google Sign-In: возвращает Google idToken, даёт понятный код ошибки.
        googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    googleResult?.complete(Result.failure(Exception("Google не вернул idToken")))
                } else {
                    val photo = account.photoUrl?.toString()
                    val display = account.displayName
                    lifecycleScope.launch {
                        googleResult?.complete(AuthManager.loginWithGoogleIdToken(idToken, photo, display))
                    }
                }
            } catch (e: ApiException) {
                android.util.Log.e("MeloAuth", "GoogleSignIn failed code=${e.statusCode}", e)
                googleResult?.complete(Result.failure(Exception("Google: код ${e.statusCode}")))
            }
        }

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
                    onLoadAlbumTracks = { url -> NewPipeResolver.albumTracks(this, url) },
                    onLoadShelf = { seed -> NewPipeResolver.shelf(this, seed) },
                    onRelatedTracks = { track -> NewPipeResolver.relatedTracks(this, track) },
                    onGoogleLogin = { signInWithGoogle() },
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
        val item = MediaItem.Builder()
            .setUri(track.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(track.title).build(),
            )
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    private fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    private suspend fun signInWithGoogle(): Result<Unit> {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(AuthManager.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        runCatching { client.signOut() } // всегда показываем выбор аккаунта
        val deferred = CompletableDeferred<Result<Unit>>()
        googleResult = deferred
        googleLauncher.launch(client.signInIntent)
        return deferred.await()
    }
}
