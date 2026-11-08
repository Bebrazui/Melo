@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.melo.music.ui

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import com.melo.music.extractor.NewPipeResolver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ripple
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.melo.music.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.melo.music.audio.EqualizerManager
import com.melo.music.extractor.ItemKind
import com.melo.music.history.HistoryManager
import com.melo.music.lyrics.LrcLine
import com.melo.music.lyrics.Lyrics
import com.melo.music.playlists.Playlist
import com.melo.music.playlists.PlaylistManager
import com.melo.music.ui.theme.Motion
import com.melo.music.ui.theme.ShapeCache
import com.melo.music.ui.theme.pressScale
import com.melo.music.ui.theme.carouselCenterItemEffect
import com.melo.music.ui.theme.carouselCenterGridItemEffect
import com.melo.music.ui.theme.verticalScrollEdgeItemEffect
import com.melo.music.ui.theme.bouncyOverscroll
import com.melo.music.ui.theme.bouncyHorizontalOverscroll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import com.melo.music.extractor.ResolvedTrack
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager
import com.melo.music.recommend.Recommender
import com.melo.music.recommend.SkipTracker
import com.melo.music.recommend.TasteProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.melo.music.ui.sound.ClickFeedback
import java.util.Locale
import kotlin.math.roundToInt


internal val FoglihtenFontFamily = FontFamily(
    Font(R.font.foglihtenblackpcs, FontWeight.Normal),
)

/**
 * Главный экран MVP в стиле Material 3 Expressive: поиск + популярная музыка
 * по региону. Тап по треку → on-device резолв (NewPipe) → фоновое воспроизведение.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerScreen(
    onSearch: (String) -> Flow<List<TrackItem>>,
    onGetSuggestions: (String) -> List<String>,
    onLoadRecommendations: suspend () -> List<TrackItem>,
    onLoadArtistTracks: suspend (TrackItem) -> List<TrackItem>,
    onLoadArtistAlbums: suspend (TrackItem) -> List<TrackItem>,
    onLoadSimilarArtists: suspend (TrackItem, List<TrackItem>) -> List<TrackItem> = { _, _ -> emptyList() },
    onLoadAlbumTracks: suspend (String) -> List<TrackItem>,
    onLoadShelf: suspend (String) -> List<TrackItem>,
    onRelatedTracks: suspend (TrackItem) -> List<TrackItem> = { emptyList() },
    onGoogleLogin: suspend () -> Result<Unit> = { Result.failure(Exception("n/a")) },
    onSearchUsers: suspend (String) -> List<com.melo.music.profile.MeloProfile> = { emptyList() },
    scGetId: () -> String?,
    onScSetManual: suspend (String) -> Boolean,
    onScRefresh: suspend () -> String?,
    onResolveAudioUrl: suspend (String, String?) -> ResolvedTrack,
    isCached: (String) -> Boolean,
    onPrefetch: (String) -> Unit,
    onInvalidateCache: (String) -> Unit = {},
    onFetchLyrics: suspend (String, String?) -> Lyrics?,
    onPlayResolved: (ResolvedTrack) -> Unit,
    onTogglePlayPause: () -> Unit,
    playerProvider: () -> MediaController?,
    audioSessionIdProvider: () -> Int = { 0 },
    showGoogle: Boolean = true,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Диалог краша.
    val context = LocalContext.current
    var showCrashDialog by remember {
        mutableStateOf(com.melo.music.crash.CrashHandler.hadCrash(context))
    }
    if (showCrashDialog) {
        val path = com.melo.music.crash.CrashHandler.getCrashPath(context)
        val time = com.melo.music.crash.CrashHandler.getCrashTime(context)
        AlertDialog(
            onDismissRequest = { showCrashDialog = false },
            title = { Text("Melo has crashed", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (time.isNotBlank()) {
                        Text("Время: $time", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("Логи сохранены:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        path ?: "не удалось сохранить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    com.melo.music.crash.CrashHandler.clearCrash(context)
                    showCrashDialog = false
                }) {
                    Text("OK")
                }
            },
        )
    }

    val recommendationsTitle = remember {
        val c = Locale.getDefault().country
        val name = if (c.isBlank()) null else Locale("", c).getDisplayCountry(Locale.getDefault())
        if (name.isNullOrBlank()) "Популярное" else "Популярное · $name"
    }

    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(MeloTab.Home) }
    var previousTab by rememberSaveable { mutableStateOf(MeloTab.Home) }
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var miniPlayerBounds by remember { mutableStateOf<Rect?>(null) }
    var miniArtBounds by remember { mutableStateOf<Rect?>(null) }
    var artistOpen by rememberSaveable(stateSaver = TrackSaver.singleSaver()) { mutableStateOf<TrackItem?>(null) }
    var playlistOpen by remember { mutableStateOf<Playlist?>(null) }
    var searchMode by rememberSaveable { mutableStateOf(false) }
    var homeSettings by remember { mutableStateOf(false) }
    // Экран входа: при первом запуске обязателен (не закрыть), из «Аккаунта» — закрываемый.
    var authVisible by remember { mutableStateOf(!com.melo.music.settings.AppSettings.seenWelcome) }
    var authDismissible by remember { mutableStateOf(false) }
    var showYouTubeLoginSheet by remember { mutableStateOf(false) }
    var importOpen by remember { mutableStateOf(false) }
    var libraryVersion by remember { mutableIntStateOf(0) }
    var showQueue by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(HistoryManager.getAll()) }
    var query by rememberSaveable { mutableStateOf("") }
    var ghostSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var ghostIndex by remember { mutableIntStateOf(0) }
    var recommendations by rememberSaveable(stateSaver = TrackSaver.listOfSaver()) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var searchResults by rememberSaveable(stateSaver = TrackSaver.listOfSaver()) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var searchLoading by rememberSaveable { mutableStateOf(false) }
    var searchError by rememberSaveable { mutableStateOf<String?>(null) }
    var listTitle by rememberSaveable { mutableStateOf(recommendationsTitle) }
    var listLoading by rememberSaveable { mutableStateOf(true) }
    var listError by rememberSaveable { mutableStateOf<String?>(null) }

    // Состояние скролла результатов поиска.
    val searchListState = rememberLazyListState()
    var hasScrolled by remember { mutableStateOf(false) }
    var lastItemCount by remember { mutableIntStateOf(0) }

    var nowPlaying by rememberSaveable(stateSaver = TrackSaver.singleSaver()) { mutableStateOf<TrackItem?>(null) }
    var copyrightBlockedTrack by remember { mutableStateOf<TrackItem?>(null) }
    var resolvingUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var trackStartTime by remember { mutableLongStateOf(0L) }

    // Очередь воспроизведения (управляется в UI) + режимы.
    var playingList by rememberSaveable(stateSaver = TrackSaver.listOfSaver()) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var playingIndex by rememberSaveable { mutableIntStateOf(-1) }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    val shuffleRecentHistory = remember { ArrayDeque<Int>() }
    var repeatOne by rememberSaveable { mutableStateOf(false) }
    // «Sea» — бесконечная персональная волна (радио YouTube Music + наш реранк).
    var seaActive by rememberSaveable { mutableStateOf(false) }
    var seaLoading by remember { mutableStateOf(false) }
    // Избранное — сохраняется в SharedPreferences.
    var likedVersion by remember { mutableIntStateOf(0) }
    fun getLiked(): MutableList<TrackItem> = FavoritesManager.getAll()
    fun isLiked(item: TrackItem): Boolean = FavoritesManager.isLiked(item)
    fun toggleLike(item: TrackItem) {
        val nowLiked = FavoritesManager.toggle(item)
        likedVersion++
        if (nowLiked && com.melo.music.settings.AppSettings.autoDownloadFavorites) {
            com.melo.music.offline.TrackDownloader.download(context, item)
        }
    }

    var contextMenuTrack by remember { mutableStateOf<TrackItem?>(null) }
    // Цель сохранения кастомной/slowed/sped up/bass/fx версии.
    var speedVariantTarget by remember { mutableStateOf<TrackItem?>(null) }
    // Поиск людей + открытый чужой профиль.
    var userResults by remember { mutableStateOf<List<com.melo.music.profile.MeloProfile>>(emptyList()) }
    var profileOpen by remember { mutableStateOf<com.melo.music.profile.MeloProfile?>(null) }

    val controller = playerProvider()
    val playback = rememberPlaybackState(controller)
    val isPlaying = playback.isPlaying
    val playerError = playback.error

    // Виджет на главном экране: отражает текущий трек и состояние.
    val widgetCtx = androidx.compose.ui.platform.LocalContext.current
    val widgetLiked = run { likedVersion; nowPlaying?.let { isLiked(it) } ?: false }
    LaunchedEffect(nowPlaying?.url, isPlaying, widgetLiked) {
        com.melo.music.widget.WidgetUpdater.setNowPlaying(
            widgetCtx,
            title = nowPlaying?.title,
            artist = nowPlaying?.uploader,
            coverUrl = nowPlaying?.thumbnailUrl,
            isPlaying = isPlaying,
            isLiked = widgetLiked,
        )
    }

    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var isBassBoost by rememberSaveable { mutableStateOf(false) }
    var crushAmount by rememberSaveable { mutableFloatStateOf(0f) }
    var pitchSemi by rememberSaveable { mutableFloatStateOf(0f) }
    var noteShiftSeed by rememberSaveable { mutableLongStateOf(0L) }

    fun applyAudioParameters(
        sp: Float = speed,
        bb: Boolean = isBassBoost,
        cr: Float = crushAmount,
        ps: Float = pitchSemi,
        ns: Long = noteShiftSeed,
    ) {
        speed = sp
        isBassBoost = bb
        crushAmount = cr
        pitchSemi = ps
        noteShiftSeed = ns

        val pitchFactor = Math.pow(2.0, (ps / 12.0).toDouble()).toFloat()
        val totalPitch = sp * pitchFactor
        playerProvider()?.playbackParameters = PlaybackParameters(sp, totalPitch)
        com.melo.music.playback.PlaybackService.playbackSpeed = sp

        EqualizerManager.setBassBoostEnabled(bb)
        EqualizerManager.setCrushAmount(cr)
        EqualizerManager.setNoteShiftSeed(ns)

        nowPlaying = nowPlaying?.copy(
            speed = sp,
            bassBoost = bb,
            crush = cr,
            pitchSemi = ps,
            noteShiftSeed = ns,
        )
    }

    fun setSpeed(value: Float) {
        applyAudioParameters(sp = value)
    }

    fun setBassBoost(value: Boolean) {
        applyAudioParameters(bb = value)
    }

    fun setCrush(value: Float) {
        applyAudioParameters(cr = value)
    }

    fun setPitchSemi(value: Float) {
        applyAudioParameters(ps = value)
    }

    fun randomizeNotes() {
        val newSeed = System.currentTimeMillis()
        applyAudioParameters(ns = newSeed)
    }

    fun resetAudioFx() {
        applyAudioParameters(bb = false, cr = 0f, ps = 0f, ns = 0L)
    }

    // Применяем скорость/тон, басс-буст и эффекты САМОГО трека при смене трека или переподключении плеера.
    LaunchedEffect(
        nowPlaying?.url,
        nowPlaying?.speed,
        nowPlaying?.bassBoost,
        nowPlaying?.crush,
        nowPlaying?.pitchSemi,
        nowPlaying?.noteShiftSeed,
        controller,
    ) {
        if (controller != null) {
            applyAudioParameters(
                sp = nowPlaying?.speed ?: 1f,
                bb = nowPlaying?.bassBoost ?: false,
                cr = nowPlaying?.crush ?: 0f,
                ps = nowPlaying?.pitchSemi ?: 0f,
                ns = nowPlaying?.noteShiftSeed ?: 0L,
            )
        }
    }

    // Восстановление состояния при перезапуске/возвращении в приложение из фона:
    // если музыка играет в PlaybackService, сразу подхватываем трек и очередь.
    LaunchedEffect(controller) {
        if (nowPlaying == null) {
            val sTrack = com.melo.music.playback.PlaybackService.currentTrackItem
            if (sTrack != null) {
                nowPlaying = sTrack
                if (playingList.isEmpty()) playingList = com.melo.music.playback.PlaybackService.queue
                if (playingIndex == -1) playingIndex = com.melo.music.playback.PlaybackService.queueIndex
            } else if (controller?.currentMediaItem != null) {
                val meta = controller.currentMediaItem?.mediaMetadata
                val title = meta?.title?.toString()
                if (!title.isNullOrBlank()) {
                    nowPlaying = TrackItem(
                        title = title,
                        uploader = meta.artist?.toString() ?: "",
                        url = com.melo.music.playback.PlaybackService.currentAudioStreamUrl ?: "",
                        durationSeconds = 0L,
                        thumbnailUrl = meta.artworkUri?.toString(),
                        source = Source.YOUTUBE_MUSIC,
                    )
                }
            }
        }
    }

    // Авто-перерезолв: плеер словил ошибку (протухшая/IP-битая ссылка) → чистим
    // кэш, резолвим заново и продолжаем с того же места. Защита от зацикливания.
    var recoverUrl by remember { mutableStateOf<String?>(null) }
    var recoverAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerError) {
        if (playerError == null) return@LaunchedEffect
        val item = nowPlaying ?: return@LaunchedEffect
        val ctrl = playerProvider() ?: return@LaunchedEffect
        val now = android.os.SystemClock.elapsedRealtime()
        // Тот же трек только что лечили (<15с назад) — не зацикливаемся.
        if (recoverUrl == item.url && now - recoverAt < 15_000) return@LaunchedEffect
        recoverUrl = item.url
        recoverAt = now
        val resumePos = ctrl.currentPosition.coerceAtLeast(0L)
        // android.util.Log.e("MeloPerf", "PLAYER ERROR → re-resolve ${item.url}")
        onInvalidateCache(item.url)
        val query = listOfNotNull(item.title, item.uploader).joinToString(" ").takeIf { it.isNotBlank() }
        val resolved = runCatching { onResolveAudioUrl(item.url, query) }.getOrNull() ?: return@LaunchedEffect
        if (nowPlaying?.url == item.url) {
            onPlayResolved(resolved)
            playerProvider()?.let { p ->
                p.playbackParameters = PlaybackParameters(speed, speed)
                if (resumePos > 2_000L) p.seekTo(resumePos)
            }
        }
    }

    LaunchedEffect(Unit) {
        listLoading = true
        runCatching {
            com.melo.music.recommend.Recommender.generatePersonalizedRecommendations(
                fallbackProvider = { onLoadRecommendations() },
                related = onRelatedTracks,
            )
        }
            .onSuccess { result ->
                // distinctBy: YouTube иногда отдаёт один трек дважды → дубль ключа в LazyColumn.
                recommendations = result.distinctBy { it.url }
                listError = null
                // Фоновая предзагрузка первых рекомендаций
                recommendations.take(2).forEach { onPrefetch(it.url) }
            }
            .onFailure { listError = it.message }
        listLoading = false
    }

    // Отслеживаем, начал ли пользователь листать результаты поиска.
    LaunchedEffect(searchListState) {
        snapshotFlow { searchListState.firstVisibleItemIndex }
            .collect { index ->
                if (index > 0) hasScrolled = true
            }
    }

    // Авто-скролл наверх при новых результатах, если пользователь не листал.
    LaunchedEffect(searchResults.size, searchMode) {
        if (searchMode && searchResults.size > lastItemCount && !hasScrolled && searchResults.isNotEmpty()) {
            searchListState.animateScrollToItem(0)
        }
        lastItemCount = searchResults.size
    }

    // Сброс флага скролла при новом поисковом запросе.
    LaunchedEffect(query) {
        if (searchMode) {
            hasScrolled = false
            lastItemCount = 0
        }
    }

    // Дебаунс: подгружаем подсказки через 2с, затем циклически переключаем каждые 2с.
    LaunchedEffect(query) {
        ghostSuggestions = emptyList()
        ghostIndex = 0
        if (query.isBlank() || searchMode) return@LaunchedEffect
        delay(2000)
        val results = withContext(Dispatchers.IO) { onGetSuggestions(query) }
        val q = query.trim().lowercase()
        // Берём до 5 вариантов, которые дополняют ввод.
        ghostSuggestions = results.filter { s ->
            val rest = s.trim().lowercase()
            rest != q && rest.startsWith(q)
        }.take(5)
        // Циклическое переключение каждые 2с.
        while (ghostSuggestions.isNotEmpty()) {
            delay(2000)
            ghostIndex = (ghostIndex + 1) % ghostSuggestions.size
        }
    }

    var searchJob: kotlinx.coroutines.Job? by remember { mutableStateOf(null) }
    var userSearchJob: kotlinx.coroutines.Job? by remember { mutableStateOf(null) }

    fun runSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        searchJob?.cancel()
        userSearchJob?.cancel()
        searchMode = true
        searchResults = emptyList()
        ghostSuggestions = emptyList()
        ghostIndex = 0
        // Параллельно ищем людей.
        userResults = emptyList()
        userSearchJob = scope.launch { userResults = runCatching { onSearchUsers(q) }.getOrDefault(emptyList()) }
        searchJob = scope.launch {
            listTitle = "Результаты: $q"
            searchLoading = true
            searchError = null
            // Потоковая выдача: YouTube прилетает первым, SoundCloud дописывается позже.
            onSearch(q)
                .catch { searchError = it.message }
                .collect { partial ->
                    searchResults = partial.distinctBy { it.url }
                }
            // Префетчим первые треки только один раз после завершения сбора выдачи,
            // чтобы не забивать сокеты и сеть во время поиска и отрисовки списка.
            searchResults.filter { it.kind == ItemKind.TRACK }.take(2).forEach { onPrefetch(it.url) }
            searchLoading = false
        }
    }

    // Фоновый prefetch следующих треков (очередь с одним воркером в Extractor).
    fun prefetchAround(list: List<TrackItem>, index: Int) {
        listOf(index + 1, index + 2, index + 3).forEach { i ->
            list.getOrNull(i)?.let { onPrefetch(it.url) }
        }
    }

    // Заранее резолвит следующий трек и отдаёт сервису для кроссфейда.
    fun pickNextShuffleIndex(listSize: Int, currentIdx: Int): Int {
        if (listSize <= 1) return 0
        if (listSize == 2) return if (currentIdx == 0) 1 else 0

        // Не повторять последние N сыгранных треков (половина очереди, но не менее 2 и не более 10)
        val maxHistorySize = (listSize / 2).coerceIn(2, 10).coerceAtMost(listSize - 1)
        val pool = (0 until listSize).filter { it != currentIdx && it !in shuffleRecentHistory }
        val candidate = if (pool.isNotEmpty()) {
            pool.random()
        } else {
            // Если все в истории, берем любой кроме текущего
            (0 until listSize).filter { it != currentIdx }.randomOrNull() ?: 0
        }
        shuffleRecentHistory.addLast(currentIdx)
        while (shuffleRecentHistory.size > maxHistorySize) {
            shuffleRecentHistory.removeFirst()
        }
        return candidate
    }

    fun pushNextResolved(fromIndex: Int) {
        val list = playingList
        if (repeatOne || list.isEmpty()) {
            com.melo.music.playback.PlaybackService.clearNext()
            return
        }
        val nextIdx = if (shuffle) {
            pickNextShuffleIndex(list.size, fromIndex)
        } else {
            (fromIndex + 1) % list.size
        }
        scope.launch {
            val nextItem = list.getOrNull(nextIdx) ?: return@launch
            val nextQuery = listOfNotNull(nextItem.title, nextItem.uploader).joinToString(" ").takeIf { it.isNotBlank() }
            val resolved = runCatching { onResolveAudioUrl(nextItem.url, nextQuery) }.getOrNull()
            // Проверяем, что очередь/индекс не сменились, пока резолвили.
            if (resolved != null && playingIndex == fromIndex) {
                com.melo.music.playback.PlaybackService.setNext(
                    resolved.audioUrl, nextItem.title, nextIdx, nextItem.speed, nextItem.thumbnailUrl, nextItem.uploader,
                )
            }
        }
    }

    fun playAt(list: List<TrackItem>, index: Int, keepSea: Boolean = false) {
        if (index !in list.indices) return
        // Любой ручной выбор трека выходит из волны (волна сама зовёт с keepSea=true).
        if (!keepSea) seaActive = false
        // Тот же трек, та же скорость И тот же басс уже играют — это пауза/продолжить, а не перезапуск.
        // (нормальная, slowed/sped up и bass версии имеют одинаковый url, но разные speed/bass).
        val cur = nowPlaying
        if (cur != null && list[index].url == cur.url &&
            kotlin.math.abs(list[index].speed - cur.speed) < 0.01f &&
            list[index].bassBoost == cur.bassBoost
        ) {
            playerProvider()?.let { if (it.isPlaying) it.pause() else it.play() }
            return
        }
        // Ручное переключение — отменяем активный кроссфейд и заготовку.
        com.melo.music.playback.PlaybackService.cancelCrossfade()
        com.melo.music.playback.PlaybackService.clearNext()
        // Записать skip-data для предыдущего трека.
        val prevUrl = nowPlaying?.url
        if (prevUrl != null && trackStartTime > 0) {
            val listenedMs = android.os.SystemClock.elapsedRealtime() - trackStartTime
            val prevDuration = nowPlaying?.durationSeconds?.let { it * 1000L } ?: 0L
            SkipTracker.recordListen(prevUrl, listenedMs, prevDuration)
        }
        playingList = list
        playingIndex = index
        val item = list[index]
        nowPlaying = item
        com.melo.music.playback.PlaybackService.setQueue(list, index)
        trackStartTime = android.os.SystemClock.elapsedRealtime()
        HistoryManager.add(item)
        history = HistoryManager.getAll()
        // Останавливаем прежний трек сразу, чтобы не звучал старый под новым UI
        // пока резолвится новый (особенно при смене источника, напр. Bandcamp).
        playerProvider()?.pause()
        scope.launch {
            val tStart = android.os.SystemClock.elapsedRealtime()
            resolvingUrl = item.url
            val query = listOfNotNull(item.title, item.uploader).joinToString(" ").takeIf { it.isNotBlank() }
            runCatching { onResolveAudioUrl(item.url, query) }
                .onSuccess {
                    copyrightBlockedTrack = null
                    android.util.Log.d("MeloTrack", "RESOLVE OK [index=$index] url=${item.url} streamUrl=${it.audioUrl.take(60)}")
                    // Если пользователь уже переключился — не играть старый трек.
                    if (resolvingUrl == item.url) {
                        val resolvedWithArtist = if (it.artist.isNullOrBlank()) it.copy(artist = item.uploader) else it
                        onPlayResolved(resolvedWithArtist)
                        // Сохранённая slowed/sped up/bass версия играет со своими параметрами.
                        setSpeed(item.speed)
                        setBassBoost(item.bassBoost)
                        pushNextResolved(index)
                    }
                }
                .onFailure {
                    android.util.Log.e("MeloTrack", "RESOLVE FAILED [index=$index] url=${item.url}: ${it.message}", it)
                    val isCopyright = it is com.melo.music.extractor.TrackCopyrightException ||
                        it.message?.contains("правообладател", ignoreCase = true) == true ||
                        it.message?.contains("copyright", ignoreCase = true) == true ||
                        it.message?.contains("interscope", ignoreCase = true) == true
                    val isAgeRestricted = it is org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException ||
                        it.message?.contains("age-restricted", ignoreCase = true) == true ||
                        it.message?.contains("confirm your age", ignoreCase = true) == true ||
                        it.message?.contains("inappropriate for some users", ignoreCase = true) == true
                    if (isCopyright) {
                        copyrightBlockedTrack = item
                    }
                    if (resolvingUrl == item.url) {
                        listError = when {
                            isCopyright -> "Трек удалён правообладателем"
                            isAgeRestricted -> "Трек 18+. Войдите в аккаунт Google в настройках Melo"
                            else -> "Не удалось воспроизвести: ${it.message}"
                        }
                    }
                }
            if (resolvingUrl == item.url) {
                resolvingUrl = null
            }
        }
        prefetchAround(list, index)
    }
    fun playNext() {
        if (playingList.isEmpty()) return
        val next = if (shuffle) pickNextShuffleIndex(playingList.size, playingIndex) else (playingIndex + 1) % playingList.size
        playAt(playingList, next, keepSea = seaActive)
    }
    fun playPrev() {
        if (playingList.isEmpty()) return
        val prev = if (playingIndex <= 0) playingList.lastIndex else playingIndex - 1
        playAt(playingList, prev, keepSea = seaActive)
    }

    // ── Редактирование очереди ──────────────────────────────────────────────
    fun removeFromQueue(index: Int) {
        if (index !in playingList.indices) return
        val newList = playingList.toMutableList().also { it.removeAt(index) }
        com.melo.music.playback.PlaybackService.clearNext()
        when {
            index == playingIndex -> {
                if (newList.isEmpty()) {
                    playingList = newList
                    playingIndex = -1
                    return
                }
                val newIdx = index.coerceAtMost(newList.lastIndex)
                playingList = newList
                playAt(newList, newIdx, keepSea = seaActive)
                return
            }
            index < playingIndex -> playingIndex -= 1
        }
        playingList = newList
        pushNextResolved(playingIndex)
    }

    fun moveInQueue(from: Int, to: Int) {
        if (from !in playingList.indices || to !in playingList.indices || from == to) return
        val newList = playingList.toMutableList()
        newList.add(to, newList.removeAt(from))
        playingList = newList
        // Держим индекс на реально играющем треке.
        playingIndex = newList.indexOfFirst { it.url == nowPlaying?.url }
            .let { if (it >= 0) it else playingIndex.coerceIn(0, newList.lastIndex) }
        com.melo.music.playback.PlaybackService.clearNext()
        pushNextResolved(playingIndex)
    }

    // ── Sea: бесконечная волна ──────────────────────────────────────────────
    // Берём радио YouTube Music от seed, чистим от слышанного/скипнутого,
    // реранжируем по вкусу. Очередь дотягивается на лету у конца списка.
    suspend fun buildSeaBatch(seed: TrackItem, exclude: Set<String>): List<TrackItem> {
        val raw = runCatching { onRelatedTracks(seed) }.getOrDefault(emptyList())
        val banned = SkipTracker.getBannedUrls()
        val filtered = raw
            .filter {
                it.kind == ItemKind.TRACK &&
                    it.url !in exclude &&
                    it.url !in banned &&
                    (it.durationSeconds == 0L || it.durationSeconds in 30..900) &&
                    !com.melo.music.extractor.YouTubeMusicSearcher.isLikelyNonMusic(it.title)
            }
            .distinctBy { it.url }
        return TasteProfile.rankByTaste(filtered).take(20)
    }

    fun startSea(seed: TrackItem?) {
        val seedTrack = seed
            ?: nowPlaying
            ?: FavoritesManager.getAll().randomOrNull()
            ?: (if (searchMode) searchResults else recommendations).firstOrNull { it.kind == ItemKind.TRACK }
            ?: return
        if (seaLoading) return
        seaLoading = true
        scope.launch {
            val exclude = HistoryManager.getAll().map { it.url }.toSet() + seedTrack.url
            val batch = buildSeaBatch(seedTrack, exclude)
            seaLoading = false
            if (batch.isEmpty()) {
                listError = "Не удалось запустить волну — попробуй другой трек"
                return@launch
            }
            seaActive = true
            playAt(batch, 0, keepSea = true)
            playerExpanded = true
        }
    }

    fun extendSea() {
        if (!seaActive || seaLoading) return
        val base = nowPlaying ?: playingList.lastOrNull() ?: return
        seaLoading = true
        scope.launch {
            val exclude = (playingList.map { it.url } + HistoryManager.getAll().map { it.url }).toSet()
            val more = buildSeaBatch(base, exclude)
            if (more.isNotEmpty()) playingList = playingList + more
            seaLoading = false
        }
    }
    // Подтягиваем волну заранее, у конца очереди.
    LaunchedEffect(seaActive, playingIndex, playingList.size) {
        if (seaActive && playingList.isNotEmpty() && playingIndex >= playingList.size - 3) {
            extendSea()
        }
    }
    // Автопереход в конце трека (или повтор).
    // Через PlaybackService.onTrackEnded — работает даже в фоне.
    val autoAdvanceRef = remember { mutableStateOf<(() -> Unit)?>(null) }
    // Обновляем ссылку каждый раз, когда меняются dependecies.
    LaunchedEffect(nowPlaying?.url, repeatOne, playingList.size, playingIndex) {
        autoAdvanceRef.value = {
            if (repeatOne) {
                controller?.let { it.seekTo(0); it.play() }
            } else {
                playNext()
            }
        }
    }
    // Кроссфейд переключил трек: сервис уже играет следующий — синхронизируем UI.
    val crossfadeAdvanceRef = remember { mutableStateOf<((Int) -> Unit)?>(null) }
    LaunchedEffect(playingList, repeatOne, shuffle) {
        crossfadeAdvanceRef.value = ref@{ idx ->
            val item = playingList.getOrNull(idx) ?: return@ref
            playingIndex = idx
            nowPlaying = item
            com.melo.music.playback.PlaybackService.setQueue(playingList, idx)
            trackStartTime = android.os.SystemClock.elapsedRealtime()
            HistoryManager.add(item)
            history = HistoryManager.getAll()
            pushNextResolved(idx)
        }
    }

    DisposableEffect(Unit) {
        com.melo.music.playback.PlaybackService.onTrackEnded = { autoAdvanceRef.value?.invoke() }
        // Управление с наушников / виджетов:
        com.melo.music.playback.PlaybackService.onSkipNext = { playNext() }
        com.melo.music.playback.PlaybackService.onSkipPrev = { playPrev() }
        com.melo.music.playback.PlaybackService.onToggleFavorite = { nowPlaying?.let { toggleLike(it) } }
        com.melo.music.playback.PlaybackService.onCrossfadeAdvance = { idx ->
            crossfadeAdvanceRef.value?.invoke(idx)
        }
        onDispose {
            com.melo.music.playback.PlaybackService.onTrackEnded = null
            com.melo.music.playback.PlaybackService.onSkipNext = null
            com.melo.music.playback.PlaybackService.onSkipPrev = null
            com.melo.music.playback.PlaybackService.onToggleFavorite = null
            com.melo.music.playback.PlaybackService.onCrossfadeAdvance = null
        }
    }

    // Назад: закрыть плеер → закрыть артиста → выйти из карты/поиска.
    BackHandler(enabled = playerExpanded || artistOpen != null || searchMode || selectedTab == MeloTab.Map) {
        when {
            playerExpanded -> playerExpanded = false
            artistOpen != null -> artistOpen = null
            selectedTab == MeloTab.Map -> selectedTab = previousTab
            searchMode -> {
                searchJob?.cancel()
                userSearchJob?.cancel()
                searchMode = false
                query = ""
                searchResults = emptyList()
                searchLoading = false
                searchError = null
                ghostSuggestions = emptyList()
                ghostIndex = 0
            }
        }
    }

    // Пятна мягко тонируем в системный цвет темы (Monet), адаптивно для тёмной и светлой темы.
    val ambientBg = MaterialTheme.colorScheme.background
    val isDarkTheme = ambientBg.luminance() < 0.5f
    val ambientLight = if (isDarkTheme) lerp(ambientBg, MaterialTheme.colorScheme.primary, 0.18f) else Color.Transparent
    val ambientDark = if (isDarkTheme) lerp(ambientBg, Color.Black, 0.32f) else Color.Transparent
    val onBg = MaterialTheme.colorScheme.onBackground
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Плавающие над контентом плашки: лента скроллится под ними (без фоновой подложки).
    val playerInset = if (nowPlaying != null) {
        if (isLandscape) 80.dp else 96.dp
    } else 0.dp
    val searchInset = if (isLandscape) 70.dp else 84.dp
    // Очень медленный дрейф пятен (один цикл ~70с, бесшовно через sin/cos).
    val ambientPhase by rememberInfiniteTransition(label = "ambient").animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(70_000, easing = LinearEasing), RepeatMode.Restart),
        label = "ambientPhase",
    )

    val isSolidBg = com.melo.music.settings.AppSettings.solidBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientBg)
            // Анимированные пятна на самом заднем слое (выключаются в режиме сплошного фона)
            .then(
                if (!isSolidBg) {
                    Modifier.drawBehind {
                        val p = ambientPhase
                        fun spot(color: Color, cx: Float, cy: Float, r: Float) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(color, Color.Transparent),
                                    center = Offset(size.width * cx, size.height * cy),
                                    radius = size.minDimension * r,
                                ),
                            )
                        }
                        spot(ambientLight, 0.18f + 0.07f * kotlin.math.sin(p), 0.16f + 0.05f * kotlin.math.cos(p * 0.8f), 0.42f)
                        spot(ambientDark, 0.92f + 0.06f * kotlin.math.cos(p * 0.7f), 0.32f + 0.07f * kotlin.math.sin(p * 1.1f), 0.46f)
                        spot(ambientLight, 0.85f + 0.07f * kotlin.math.sin(p * 1.3f + 1f), 0.78f + 0.06f * kotlin.math.cos(p), 0.38f)
                        spot(ambientDark, 0.08f + 0.06f * kotlin.math.cos(p * 0.9f + 2f), 0.9f + 0.05f * kotlin.math.sin(p * 0.6f), 0.34f)
                        // Пятно у низа — чтобы за стеклянной плашкой плеера было что просвечивать.
                        spot(ambientLight, 0.5f + 0.05f * kotlin.math.sin(p * 0.5f), 0.99f, 0.45f)
                    }
                } else Modifier
            ),
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = onBg,
        bottomBar = {
            if (!isLandscape) {
                MeloBottomNav(
                    selected = selectedTab,
                    onSelect = {
                        if (it != selectedTab) {
                            ClickFeedback.play()
                            previousTab = selectedTab
                            selectedTab = it
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
          if (isLandscape) {
              MeloNavRail(
                  selected = selectedTab,
                  onSelect = {
                      if (it != selectedTab) {
                          ClickFeedback.play()
                          previousTab = selectedTab
                          selectedTab = it
                      }
                  },
              )
          }
          Box(
              modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
          ) {
          AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                // Направленное переключение табов в стиле PixelPlayer:
                // slide + fade, emphasized-easing, fade — половина длительности.
                val forward = targetState.ordinal >= initialState.ordinal
                val slideSpec = tween<IntOffset>(Motion.TAB_TRANSITION_MS, easing = Motion.EmphasizedEasing)
                val fadeSpec = tween<Float>(Motion.TAB_FADE_MS)
                if (forward) {
                    (slideInHorizontally(slideSpec) { it / 4 } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(slideSpec) { -it / 6 } + fadeOut(fadeSpec))
                } else {
                    (slideInHorizontally(slideSpec) { -it / 4 } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(slideSpec) { it / 6 } + fadeOut(fadeSpec))
                }
            },
            label = "tabs",
          ) { tab ->
           Column(modifier = Modifier.fillMaxSize()) {
            when (tab) {
                MeloTab.Home -> Box(modifier = Modifier.fillMaxSize()) {
                    if (searchMode) {
                        when {
                            searchLoading && searchResults.isEmpty() -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.7f),
                                    exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.7f),
                                ) {
                                    LoadingIndicator(modifier = Modifier.size(48.dp))
                                }
                            }

                            searchError != null && searchResults.isEmpty() -> Text(
                                text = "Ошибка: $searchError",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = searchInset + 12.dp, start = 20.dp, end = 20.dp),
                            )

                            searchResults.isEmpty() && !searchLoading -> SearchEmptyState(
                                query = query,
                                topInset = searchInset,
                            )

                            else -> androidx.compose.animation.AnimatedVisibility(
                                visible = searchResults.isNotEmpty(),
                                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 12 },
                                exit = fadeOut(tween(200)),
                            ) {
                                SearchResultsScreen(
                                    rawItems = searchResults,
                                    users = userResults,
                                    query = query,
                                    loadingMore = searchLoading,
                                    nowPlayingUrl = nowPlaying?.url,
                                    isPlaying = isPlaying,
                                    resolvingUrl = resolvingUrl,
                                    state = searchListState,
                                    topInset = searchInset,
                                    bottomInset = playerInset,
                                    onPlayFrom = { list, i -> playAt(list, i) },
                                    onShuffleAll = {
                                        val t = searchResults.filter { it.kind == ItemKind.TRACK }
                                        if (t.isNotEmpty()) {
                                            shuffle = true
                                            playAt(t, t.indices.random())
                                        }
                                    },
                                    onOpenArtist = { artistOpen = it },
                                    onOpenUser = { profileOpen = it },
                                    onTrackLongClick = { contextMenuTrack = it },
                                    onLoadAlbumTracks = { onLoadAlbumTracks(it) },
                                )
                            }
                        }
                    } else {
                        HomeFeed(
                            recommendations = recommendations,
                            history = history,
                            loading = listLoading && recommendations.isEmpty(),
                            onLoadShelf = onLoadShelf,
                            nowPlayingUrl = nowPlaying?.url,
                            isPlaying = isPlaying,
                            resolvingUrl = resolvingUrl,
                            onPlay = { list, index -> playAt(list, index) },
                            onTrackLongClick = { contextMenuTrack = it },
                            onMood = { seed -> query = seed; runSearch() },
                            onOpenAccount = { selectedTab = MeloTab.Account },
                            onPrefetch = onPrefetch,
                            seaLoading = seaLoading,
                            // Sea отражает своё состояние ТОЛЬКО когда играет именно волна.
                            seaIsPlaying = seaActive && isPlaying,
                            seaTitle = if (seaActive) nowPlaying?.title else null,
                            seaArtist = if (seaActive) nowPlaying?.uploader else null,
                            seaIsLiked = run { likedVersion; if (seaActive) nowPlaying?.let { isLiked(it) } ?: false else false },
                            onSeaPlayPause = {
                                if (seaActive) onTogglePlayPause() else startSea(null)
                            },
                            onSeaNext = {
                                if (seaActive) playNext() else startSea(null)
                            },
                            onSeaLike = { if (seaActive) nowPlaying?.let { toggleLike(it) } },
                            onRelatedTracks = onRelatedTracks,
                            topInset = searchInset,
                            bottomInset = playerInset,
                        )
                    }

                    // Поиск плавает над лентой (лента скроллится под ним).
                    SearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = ::runSearch,
                        onClear = {
                            searchJob?.cancel()
                            userSearchJob?.cancel()
                            query = ""
                            searchMode = false
                            searchResults = emptyList()
                            searchLoading = false
                            searchError = null
                            ghostSuggestions = emptyList()
                            ghostIndex = 0
                        },
                        ghostSuggestion = ghostSuggestions.getOrNull(ghostIndex) ?: "",
                        onGhostAccept = { accepted ->
                            query = accepted
                            ghostSuggestions = emptyList()
                            ghostIndex = 0
                            runSearch()
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .then(if (isLandscape) Modifier.widthIn(max = 560.dp).padding(top = 4.dp) else Modifier.fillMaxWidth()),
                    )
                }

                MeloTab.Favorite -> {
                    val likedList = remember(likedVersion) { getLiked() }
                    // Прогреваем первые треки → первый тап мгновенный даже после перезапуска.
                    LaunchedEffect(likedVersion) {
                        likedList.take(3).forEach { onPrefetch(it.url) }
                    }
                    if (likedList.isEmpty()) {
                        Placeholder(
                            icon = Icons.Rounded.Favorite,
                            title = "Избранное",
                            subtitle = "Лайкнутые треки появятся здесь",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().bouncyOverscroll(),
                            contentPadding = PaddingValues(bottom = 12.dp + playerInset),
                        ) {
                            item {
                                FavoritesHeader(
                                    tracks = likedList,
                                    onPlayAll = { playAt(likedList, 0) },
                                    onShuffle = {
                                        shuffle = true
                                        playAt(likedList, likedList.indices.random())
                                    },
                                    onDownloadAll = {
                                        ClickFeedback.play()
                                        com.melo.music.offline.TrackDownloader.downloadAll(context, likedList)
                                    },
                                )
                            }
                            itemsIndexed(likedList, key = { _, it -> "${it.url}@${it.speed}@${it.bassBoost}@${it.crush}@${it.pitchSemi}@${it.noteShiftSeed}" }) { index, item ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                                    TrackCard(
                                        item = item,
                                        resolving = resolvingUrl == item.url,
                                        playing = nowPlaying?.url == item.url &&
                                            kotlin.math.abs((nowPlaying?.speed ?: 1f) - item.speed) < 0.01f &&
                                            (nowPlaying?.bassBoost ?: false) == item.bassBoost &&
                                            kotlin.math.abs((nowPlaying?.crush ?: 0f) - item.crush) < 0.01f &&
                                            kotlin.math.abs((nowPlaying?.pitchSemi ?: 0f) - item.pitchSemi) < 0.01f &&
                                            (nowPlaying?.noteShiftSeed ?: 0L) == item.noteShiftSeed && isPlaying,
                                        onClick = { playAt(likedList, index) },
                                        onLongClick = { contextMenuTrack = item },
                                    )
                                }
                            }
                        }
                    }
                }

                // Карта рисуется отдельным слоем во весь экран (под статус-баром).
                MeloTab.Map -> Box(modifier = Modifier.fillMaxSize())

                MeloTab.Account -> AccountTab(
                    onOpenPlaylist = { playlistOpen = it },
                    onOpenSettings = { homeSettings = true },
                    onOpenImport = { importOpen = true },
                    onOpenLogin = { authDismissible = true; authVisible = true },
                    refreshKey = libraryVersion,
                    bottomInset = playerInset,
                )
            }
           }
          }

          // Плашка плеера плавает над контентом (лента скроллится под стеклом).
          NowPlayingBar(
              item = nowPlaying,
              isPlaying = isPlaying,
              resolving = resolvingUrl != null,
              error = playerError,
              onTogglePlayPause = onTogglePlayPause,
              onPrev = { playPrev() },
              onNext = { playNext() },
              onClick = { playerExpanded = true },
              modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .then(if (isLandscape) Modifier.widthIn(max = 560.dp).padding(bottom = 6.dp) else Modifier.fillMaxWidth())
                  .graphicsLayer { alpha = if (playerExpanded) 0f else 1f },
              onPositioned = { miniPlayerBounds = it },
              onArtPositioned = { miniArtBounds = it },
          )

          // ── Плашка: Трек удалён правообладателем (на главном экране) ──
          androidx.compose.animation.AnimatedVisibility(
              visible = copyrightBlockedTrack != null && !playerExpanded,
              enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
              exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
              modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .then(if (isLandscape) Modifier.widthIn(max = 560.dp) else Modifier.fillMaxWidth())
                  .padding(bottom = if (nowPlaying != null) 86.dp else 18.dp)
                  .padding(horizontal = 14.dp)
                  .zIndex(15f),
          ) {
              copyrightBlockedTrack?.let { track ->
                  CopyrightBlockedBanner(
                      track = track,
                      onFindSimilar = {
                          copyrightBlockedTrack = null
                          val q = listOfNotNull(track.title, track.uploader).joinToString(" ").trim()
                          if (q.isNotBlank()) {
                              query = q
                              runSearch()
                          }
                      },
                      onDismiss = {
                          copyrightBlockedTrack = null
                      },
                  )
              }
          }

          // ── Плашка доступного обновления ──
          val updateAvailable = com.melo.music.update.UpdateManager.availableUpdate
          var updateBannerDismissed by remember { mutableStateOf(false) }

          androidx.compose.animation.AnimatedVisibility(
              visible = updateAvailable != null && !updateBannerDismissed && !playerExpanded,
              enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
              exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
              modifier = Modifier
                  .align(Alignment.TopCenter)
                  .padding(top = 4.dp),
          ) {
              updateAvailable?.let { info ->
                  com.melo.music.ui.components.UpdatePillBanner(
                      updateInfo = info,
                      onDismiss = {
                          updateBannerDismissed = true
                      },
                  )
              }
          }
        }
       }

        // Карта — отдельный слой во весь экран с направленным боковым выездом (смотря откуда перешли).
        val mapFromLeft = previousTab.ordinal < MeloTab.Map.ordinal
        AnimatedVisibility(
            visible = selectedTab == MeloTab.Map,
            enter = slideInHorizontally(
                animationSpec = tween(Motion.TRANSITION_MS, easing = Motion.EmphasizedDecelerate),
                initialOffsetX = { if (mapFromLeft) it else -it },
            ) + fadeIn(tween(Motion.FADE_MS)),
            exit = slideOutHorizontally(
                animationSpec = tween(280, easing = Motion.EmphasizedAccelerate),
                targetOffsetX = { if (mapFromLeft) it else -it },
            ) + fadeOut(tween(160)),
        ) {
            com.melo.music.map.MusicMapScreen(
                onSearch = onSearch,
                onPlay = { track -> playAt(listOf(track), 0) },
                onClose = { selectedTab = previousTab },
                topInset = innerPadding.calculateTopPadding(),
                bottomInset = innerPadding.calculateBottomPadding(),
                nowPlayingBar = {
                    NowPlayingBar(
                        item = nowPlaying,
                        isPlaying = isPlaying,
                        resolving = resolvingUrl != null,
                        error = playerError,
                        onTogglePlayPause = onTogglePlayPause,
                        onPrev = { playPrev() },
                        onNext = { playNext() },
                        onClick = { playerExpanded = true },
                        modifier = Modifier.graphicsLayer { alpha = if (playerExpanded) 0f else 1f },
                        onPositioned = { miniPlayerBounds = it },
                        onArtPositioned = { miniArtBounds = it },
                    )
                },
            )
        }
      }
    }

        val current = nowPlaying
        AnimatedVisibility(
            visible = playerExpanded && current != null,
            enter = fadeIn(tween(200)),
            exit = androidx.compose.animation.ExitTransition.None,
        ) {
            val item = current ?: nowPlaying
            if (item != null) {
                // Привязка к likedVersion → сердечко обновляется после toggle.
                val itemLiked = remember(likedVersion, item.url) { isLiked(item) }
                FullPlayer(
                    item = item,
                    isPlaying = isPlaying,
                    resolving = resolvingUrl == item.url,
                    error = playerError,
                    isLiked = itemLiked,
                    shuffle = shuffle,
                    repeatOne = repeatOne,
                    positionState = playback.position,
                    durationState = playback.duration,
                    audioSessionId = audioSessionIdProvider(),
                    speed = speed,
                    onSetSpeed = { setSpeed(it) },
                    bassBoost = isBassBoost,
                    onSetBassBoost = { setBassBoost(it) },
                    crushAmount = crushAmount,
                    onSetCrush = { setCrush(it) },
                    pitchSemi = pitchSemi,
                    onSetPitchSemi = { setPitchSemi(it) },
                    noteShiftSeed = noteShiftSeed,
                    onRandomizeNotes = { randomizeNotes() },
                    onResetAudioFx = { resetAudioFx() },
                    onAddSpeedVariant = { sp ->
                        speedVariantTarget = speedVariant(
                            base = item,
                            speed = sp,
                            bassBoost = isBassBoost,
                            crush = crushAmount,
                            pitchSemi = pitchSemi,
                            noteShiftSeed = noteShiftSeed,
                        )
                    },
                    onSeek = { ms -> controller?.seekTo(ms) },
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = { playNext() },
                    onPrev = { playPrev() },
                    onToggleShuffle = { shuffle = !shuffle },
                    onToggleRepeat = { repeatOne = !repeatOne },
                    onToggleLike = { toggleLike(nowPlaying ?: item) },
                    onShowQueue = { showQueue = true },
                    onFetchLyrics = { onFetchLyrics(item.title, item.uploader) },
                    onOpenArtist = { artistItem ->
                        playerExpanded = false
                        artistOpen = artistItem
                    },
                    onCollapse = { playerExpanded = false },
                    miniPlayerBounds = miniPlayerBounds,
                    miniArtBounds = miniArtBounds,
                    copyrightBlocked = copyrightBlockedTrack?.url == item.url,
                    onFindSimilar = {
                        copyrightBlockedTrack = null
                        playerExpanded = false
                        val q = listOfNotNull(item.title, item.uploader).joinToString(" ").trim()
                        if (q.isNotBlank()) {
                            query = q
                            runSearch()
                        }
                    },
                    onDismissCopyright = {
                        copyrightBlockedTrack = null
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = artistOpen != null,
            enter = slideInVertically(
                animationSpec = tween(Motion.TRANSITION_MS, easing = Motion.EmphasizedDecelerate),
                initialOffsetY = { it },
            ) + fadeIn(tween(Motion.FADE_MS)),
            exit = slideOutVertically(
                animationSpec = tween(300, easing = Motion.EmphasizedAccelerate),
                targetOffsetY = { it },
            ) + fadeOut(tween(180)),
        ) {
            artistOpen?.let { artist ->
                ArtistScreen(
                    artist = artist,
                    onLoadTracks = { onLoadArtistTracks(artist) },
                    onLoadAlbums = { onLoadArtistAlbums(artist) },
                    onLoadSimilar = { seed -> onLoadSimilarArtists(artist, seed) },
                    nowPlayingUrl = nowPlaying?.url,
                    isPlaying = isPlaying,
                    resolvingUrl = resolvingUrl,
                    audioSessionId = audioSessionIdProvider(),
                    onPlay = { tracks, index -> playAt(tracks, index) },
                    onShuffle = { tracks ->
                        shuffle = true
                        if (tracks.isNotEmpty()) playAt(tracks, tracks.indices.random())
                    },
                    onLoadAlbumTracks = { url -> onLoadAlbumTracks(url) },
                    onTrackLongClick = { contextMenuTrack = it },
                    onOpenSimilar = { artistOpen = it },
                    onClose = { artistOpen = null },
                )
            }
        }

        AnimatedVisibility(
            visible = playlistOpen != null,
            enter = slideInVertically(
                animationSpec = tween(Motion.TRANSITION_MS, easing = Motion.EmphasizedDecelerate),
                initialOffsetY = { it },
            ) + fadeIn(tween(Motion.FADE_MS)),
            exit = slideOutVertically(
                animationSpec = tween(300, easing = Motion.EmphasizedAccelerate),
                targetOffsetY = { it },
            ) + fadeOut(tween(180)),
        ) {
            playlistOpen?.let { pl ->
                PlaylistScreen(
                    playlist = pl,
                    nowPlayingUrl = nowPlaying?.url,
                    nowPlayingSpeed = nowPlaying?.speed ?: 1f,
                    nowPlayingBassBoost = nowPlaying?.bassBoost ?: false,
                    isPlaying = isPlaying,
                    resolvingUrl = resolvingUrl,
                    onPlay = { tracks, index -> playAt(tracks, index) },
                    onTrackLongClick = { contextMenuTrack = it },
                    onClose = { playlistOpen = null },
                )
            }
        }
    }

    TrackContextMenu(
        item = contextMenuTrack?.let {
            if (nowPlaying?.url == it.url) nowPlaying else it
        },
        onDismiss = { contextMenuTrack = null },
    )

    speedVariantTarget?.let { variant ->
        SpeedVariantSheet(
            variant = variant,
            onFavorite = {
                FavoritesManager.toggle(variant)
                likedVersion++
                speedVariantTarget = null
            },
            onAddToPlaylist = { playlist ->
                PlaylistManager.addTrack(playlist.id, variant)
                speedVariantTarget = null
            },
            onDismiss = { speedVariantTarget = null },
        )
    }

    AnimatedVisibility(
        visible = homeSettings,
        enter = slideInHorizontally(
            animationSpec = tween(Motion.TRANSITION_MS, easing = Motion.EmphasizedDecelerate),
            initialOffsetX = { it },
        ) + fadeIn(tween(Motion.FADE_MS)),
        exit = slideOutHorizontally(
            animationSpec = tween(280, easing = Motion.EmphasizedAccelerate),
            targetOffsetX = { it },
        ) + fadeOut(tween(160)),
    ) {
        SettingsScreen(
            scGetId = scGetId,
            onScSetManual = onScSetManual,
            onScRefresh = onScRefresh,
            onBack = { homeSettings = false },
        )
    }

    AnimatedVisibility(
        visible = profileOpen != null,
        enter = slideInHorizontally(
            animationSpec = tween(Motion.TRANSITION_MS, easing = Motion.EmphasizedDecelerate),
            initialOffsetX = { it },
        ) + fadeIn(tween(Motion.FADE_MS)),
        exit = slideOutHorizontally(
            animationSpec = tween(280, easing = Motion.EmphasizedAccelerate),
            targetOffsetX = { it },
        ) + fadeOut(tween(160)),
    ) {
        profileOpen?.let { p ->
            ProfileScreen(
                profile = p,
                onPlay = { list, index -> playAt(list, index) },
                onClose = { profileOpen = null },
            )
        }
    }

    AnimatedVisibility(
        visible = authVisible,
        enter = fadeIn(tween(240)) + scaleIn(tween(240, easing = Motion.EmphasizedDecelerate), initialScale = 0.92f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.92f),
    ) {
        WelcomeScreen(
            onLogin = { e, p -> com.melo.music.auth.AuthManager.login(e, p) },
            onStartRegister = { e, p, n -> com.melo.music.auth.AuthManager.startEmailRegister(e, p, n) },
            onConfirmCode = { uid, c -> com.melo.music.auth.AuthManager.confirmEmailCode(uid, c) },
            onGoogle = { onGoogleLogin() },
            onYouTubeLogin = { showYouTubeLoginSheet = true },
            onLocal = {
                com.melo.music.settings.AppSettings.setSeenWelcome()
                authVisible = false
            },
            onSuccess = {
                com.melo.music.settings.AppSettings.setSeenWelcome()
                authVisible = false
            },
            onClose = if (authDismissible) {
                { authVisible = false }
            } else {
                null
            },
            showGoogle = showGoogle,
        )
    }

    if (showYouTubeLoginSheet) {
        com.melo.music.ui.auth.GoogleLoginBottomSheet(
            onDismiss = { showYouTubeLoginSheet = false },
            onSuccess = {
                showYouTubeLoginSheet = false
                com.melo.music.settings.AppSettings.setSeenWelcome()
                authVisible = false
                // Автоматически запускаем синк в фоне
                scope.launch {
                    com.melo.music.sync.YouTubeSyncManager.syncLibrary(context)
                    libraryVersion++
                }
            }
        )
    }

    AnimatedVisibility(
        visible = importOpen,
        enter = slideInHorizontally(tween(280)) { it } + fadeIn(),
        exit = slideOutHorizontally(tween(240)) { it } + fadeOut(),
    ) {
        ImportScreen(
            onBack = { importOpen = false },
            onImported = { libraryVersion++ },
        )
    }

    if (showQueue) {
        QueueSheet(
            tracks = playingList,
            currentIndex = playingIndex,
            nowPlayingUrl = nowPlaying?.url,
            isPlaying = isPlaying,
            onJump = { i -> playAt(playingList, i, keepSea = seaActive) },
            onRemove = { i -> removeFromQueue(i) },
            onMove = { from, to -> moveInQueue(from, to) },
            onDismiss = { showQueue = false },
        )
    }
}

