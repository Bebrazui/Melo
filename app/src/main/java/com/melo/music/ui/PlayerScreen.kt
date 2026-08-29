package com.melo.music.ui

import android.Manifest
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.ui.viewinterop.AndroidView
import com.melo.music.extractor.NewPipeResolver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Главный экран MVP в стиле Material 3 Expressive: поиск + популярная музыка
 * по региону. Тап по треку → on-device резолв (NewPipe) → фоновое воспроизведение.
 */
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
    onResolveAudioUrl: suspend (String) -> ResolvedTrack,
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
    var artistOpen by rememberSaveable(stateSaver = TrackSaver.singleSaver()) { mutableStateOf<TrackItem?>(null) }
    var playlistOpen by remember { mutableStateOf<Playlist?>(null) }
    var searchMode by rememberSaveable { mutableStateOf(false) }
    var homeSettings by remember { mutableStateOf(false) }
    // Экран входа: при первом запуске обязателен (не закрыть), из «Аккаунта» — закрываемый.
    var authVisible by remember { mutableStateOf(!com.melo.music.settings.AppSettings.seenWelcome) }
    var authDismissible by remember { mutableStateOf(false) }
    var importOpen by remember { mutableStateOf(false) }
    var libraryVersion by remember { mutableIntStateOf(0) }
    var showQueue by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(HistoryManager.getAll()) }
    var query by rememberSaveable { mutableStateOf("") }
    var ghostSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var ghostIndex by remember { mutableIntStateOf(0) }
    var items by rememberSaveable(stateSaver = TrackSaver.listOfSaver()) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var listTitle by rememberSaveable { mutableStateOf(recommendationsTitle) }
    var listLoading by rememberSaveable { mutableStateOf(true) }
    var listError by rememberSaveable { mutableStateOf<String?>(null) }

    // Состояние скролла результатов поиска.
    val searchListState = rememberLazyListState()
    var hasScrolled by remember { mutableStateOf(false) }
    var lastItemCount by remember { mutableIntStateOf(0) }

    var nowPlaying by rememberSaveable(stateSaver = TrackSaver.singleSaver()) { mutableStateOf<TrackItem?>(null) }
    var resolvingUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var trackStartTime by remember { mutableLongStateOf(0L) }

    // Очередь воспроизведения (управляется в UI) + режимы.
    var playingList by rememberSaveable(stateSaver = TrackSaver.listOfSaver()) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var playingIndex by rememberSaveable { mutableIntStateOf(-1) }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    var repeatOne by rememberSaveable { mutableStateOf(false) }
    // «Sea» — бесконечная персональная волна (радио YouTube Music + наш реранк).
    var seaActive by rememberSaveable { mutableStateOf(false) }
    var seaLoading by remember { mutableStateOf(false) }
    // Избранное — сохраняется в SharedPreferences.
    var likedVersion by remember { mutableIntStateOf(0) }
    fun getLiked(): MutableList<TrackItem> = FavoritesManager.getAll()
    fun isLiked(item: TrackItem): Boolean = FavoritesManager.isLiked(item)
    fun toggleLike(item: TrackItem) {
        FavoritesManager.toggle(item)
        likedVersion++
    }

    var contextMenuTrack by remember { mutableStateOf<TrackItem?>(null) }
    // Цель сохранения slowed/sped up версии: (исходный трек, скорость).
    var speedVariantTarget by remember { mutableStateOf<Pair<TrackItem, Float>?>(null) }
    // Поиск людей + открытый чужой профиль.
    var userResults by remember { mutableStateOf<List<com.melo.music.profile.MeloProfile>>(emptyList()) }
    var profileOpen by remember { mutableStateOf<com.melo.music.profile.MeloProfile?>(null) }

    val controller = playerProvider()
    val playback = rememberPlaybackState(controller)
    val isPlaying = playback.isPlaying
    val playerError = playback.error

    // Виджет на главном экране: отражает текущий трек и состояние.
    val widgetCtx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(nowPlaying?.url, isPlaying) {
        com.melo.music.widget.WidgetUpdater.setNowPlaying(
            widgetCtx,
            title = nowPlaying?.title,
            artist = nowPlaying?.uploader,
            coverUrl = nowPlaying?.thumbnailUrl,
            isPlaying = isPlaying,
        )
    }

    // Скорость + тон воспроизведения (slowed / original / sped up). pitch = speed → меняется тон.
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    fun setSpeed(value: Float) {
        speed = value
        playerProvider()?.playbackParameters = PlaybackParameters(value, value)
        com.melo.music.playback.PlaybackService.playbackSpeed = value
    }

    // Применяем скорость/тон САМОГО трека при смене трека или переподключении плеера.
    // После перезапуска создаётся новый плеер с дефолтными параметрами (1.0) —
    // этот эффект восстанавливает сохранённую скорость slowed/sped up версий.
    LaunchedEffect(nowPlaying?.url, nowPlaying?.speed, controller) {
        if (controller != null) setSpeed(nowPlaying?.speed ?: 1f)
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
        val resolved = runCatching { onResolveAudioUrl(item.url) }.getOrNull() ?: return@LaunchedEffect
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
                items = result.distinctBy { it.url }
                listError = null
                // Фоновая предзагрузка первых рекомендаций
                items.take(2).forEach { onPrefetch(it.url) }
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
    LaunchedEffect(items.size, searchMode) {
        if (searchMode && items.size > lastItemCount && !hasScrolled && items.isNotEmpty()) {
            searchListState.animateScrollToItem(0)
        }
        lastItemCount = items.size
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

    fun runSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        searchMode = true
        ghostSuggestions = emptyList()
        ghostIndex = 0
        // Параллельно ищем людей.
        userResults = emptyList()
        scope.launch { userResults = runCatching { onSearchUsers(q) }.getOrDefault(emptyList()) }
        scope.launch {
            listTitle = "Результаты: $q"
            listLoading = true
            listError = null
            // Потоковая выдача: YouTube прилетает первым, SoundCloud дописывается позже.
            onSearch(q)
                .catch { listError = it.message }
                .collect { partial ->
                    items = partial.distinctBy { it.url }
                }
            // Префетчим первые треки только один раз после завершения сбора выдачи,
            // чтобы не забивать сокеты и сеть во время поиска и отрисовки списка.
            items.filter { it.kind == ItemKind.TRACK }.take(2).forEach { onPrefetch(it.url) }
            listLoading = false
        }
    }

    // Фоновый prefetch следующих треков (очередь с одним воркером в Extractor).
    fun prefetchAround(list: List<TrackItem>, index: Int) {
        listOf(index + 1, index + 2, index + 3).forEach { i ->
            list.getOrNull(i)?.let { onPrefetch(it.url) }
        }
    }

    // Заранее резолвит следующий трек и отдаёт сервису для кроссфейда.
    fun pushNextResolved(fromIndex: Int) {
        val list = playingList
        if (repeatOne || list.isEmpty()) {
            com.melo.music.playback.PlaybackService.clearNext()
            return
        }
        val nextIdx = if (shuffle) {
            if (list.size > 1) ((fromIndex + 1 + (0 until list.size - 1).random()) % list.size)
            else fromIndex
        } else {
            (fromIndex + 1) % list.size
        }
        scope.launch {
            val nextItem = list.getOrNull(nextIdx) ?: return@launch
            val resolved = runCatching { onResolveAudioUrl(nextItem.url) }.getOrNull()
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
        // Тот же трек И та же скорость уже играют — это пауза/продолжить, а не перезапуск.
        // (нормальная и slowed/sped up версии имеют одинаковый url, но разную speed).
        val cur = nowPlaying
        if (cur != null && list[index].url == cur.url &&
            kotlin.math.abs(list[index].speed - cur.speed) < 0.01f
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
        trackStartTime = android.os.SystemClock.elapsedRealtime()
        HistoryManager.add(item)
        history = HistoryManager.getAll()
        // Останавливаем прежний трек сразу, чтобы не звучал старый под новым UI
        // пока резолвится новый (особенно при смене источника, напр. Bandcamp).
        playerProvider()?.pause()
        scope.launch {
            val tStart = android.os.SystemClock.elapsedRealtime()
            resolvingUrl = item.url
            runCatching { onResolveAudioUrl(item.url) }
                .onSuccess {
                    // Если пользователь уже переключился — не играть старый трек.
                    if (resolvingUrl == item.url) {
                        val resolvedWithArtist = if (it.artist.isNullOrBlank()) it.copy(artist = item.uploader) else it
                        onPlayResolved(resolvedWithArtist)
                        // Сохранённая slowed/sped up версия играет со своим тоном.
                        setSpeed(item.speed)
                        pushNextResolved(index)
                    }
                }
                .onFailure {
                    if (resolvingUrl == item.url) {
                        listError = "Не удалось воспроизвести: ${it.message}"
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
        val next = if (shuffle) playingList.indices.random() else (playingIndex + 1) % playingList.size
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
            .filter { it.kind == ItemKind.TRACK && it.url !in exclude && it.url !in banned }
            .distinctBy { it.url }
        return TasteProfile.rankByTaste(filtered).take(20)
    }

    fun startSea(seed: TrackItem?) {
        val seedTrack = seed
            ?: nowPlaying
            ?: FavoritesManager.getAll().randomOrNull()
            ?: items.firstOrNull { it.kind == ItemKind.TRACK }
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
    LaunchedEffect(Unit) {
        com.melo.music.playback.PlaybackService.onTrackEnded = { autoAdvanceRef.value?.invoke() }
        // Управление с наушников: next/prev резолвятся и играются здесь.
        com.melo.music.playback.PlaybackService.onSkipNext = { playNext() }
        com.melo.music.playback.PlaybackService.onSkipPrev = { playPrev() }
    }

    // Кроссфейд переключил трек: сервис уже играет следующий — синхронизируем UI.
    val crossfadeAdvanceRef = remember { mutableStateOf<((Int) -> Unit)?>(null) }
    LaunchedEffect(playingList, repeatOne, shuffle) {
        crossfadeAdvanceRef.value = ref@{ idx ->
            val item = playingList.getOrNull(idx) ?: return@ref
            playingIndex = idx
            nowPlaying = item
            trackStartTime = android.os.SystemClock.elapsedRealtime()
            HistoryManager.add(item)
            history = HistoryManager.getAll()
            pushNextResolved(idx)
        }
    }
    LaunchedEffect(Unit) {
        com.melo.music.playback.PlaybackService.onCrossfadeAdvance = { idx ->
            crossfadeAdvanceRef.value?.invoke(idx)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            com.melo.music.playback.PlaybackService.onTrackEnded = null
            com.melo.music.playback.PlaybackService.onCrossfadeAdvance = null
            com.melo.music.playback.PlaybackService.onSkipNext = null
            com.melo.music.playback.PlaybackService.onSkipPrev = null
        }
    }

    // Назад: закрыть плеер → закрыть артиста → выйти из карты/поиска.
    BackHandler(enabled = playerExpanded || artistOpen != null || searchMode || selectedTab == MeloTab.Map) {
        when {
            playerExpanded -> playerExpanded = false
            artistOpen != null -> artistOpen = null
            selectedTab == MeloTab.Map -> selectedTab = previousTab
            searchMode -> {
                searchMode = false
                query = ""
                ghostSuggestions = emptyList()
                ghostIndex = 0
            }
        }
    }

    // Пятна тонируем в зелёный (≈#19261E), а не нейтрально-белый.
    val ambientBg = MaterialTheme.colorScheme.background
    val ambientLight = lerp(ambientBg, Color(0xFF3E6B4E), 0.22f)
    val ambientDark = lerp(ambientBg, Color.Black, 0.32f)
    val onBg = MaterialTheme.colorScheme.onBackground
    // Плавающие над контентом плашки: лента скроллится под ними (без фоновой подложки).
    val playerInset = if (nowPlaying != null) 96.dp else 0.dp
    val searchInset = 84.dp
    // Очень медленный дрейф пятен (один цикл ~70с, бесшовно через sin/cos).
    val ambientPhase by rememberInfiniteTransition(label = "ambient").animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(70_000, easing = LinearEasing), RepeatMode.Restart),
        label = "ambientPhase",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientBg)
            // Анимированные пятна на самом заднем слое — под всем, включая плашку плеера.
            .drawBehind {
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
            },
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = onBg,
        bottomBar = {
            MeloBottomNav(
                selected = selectedTab,
                onSelect = {
                    if (it != selectedTab) {
                        previousTab = selectedTab
                        selectedTab = it
                    }
                },
            )
        },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                            listLoading && items.isEmpty() -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }

                            listError != null && items.isEmpty() -> Text(
                                text = "Ошибка: $listError",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = searchInset + 12.dp, start = 20.dp, end = 20.dp),
                            )

                            items.isEmpty() -> SearchEmptyState(
                                query = query,
                                topInset = searchInset,
                            )

                            else -> SearchResultsScreen(
                                rawItems = items,
                                users = userResults,
                                query = query,
                                loadingMore = listLoading,
                                nowPlayingUrl = nowPlaying?.url,
                                isPlaying = isPlaying,
                                resolvingUrl = resolvingUrl,
                                state = searchListState,
                                topInset = searchInset,
                                bottomInset = playerInset,
                                onPlayFrom = { list, i -> playAt(list, i) },
                                onShuffleAll = {
                                    val t = items.filter { it.kind == ItemKind.TRACK }
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
                    } else {
                        HomeFeed(
                            recommendations = items,
                            history = history,
                            loading = listLoading && items.isEmpty(),
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
                        onClear = { query = ""; searchMode = false; ghostSuggestions = emptyList(); ghostIndex = 0 },
                        ghostSuggestion = ghostSuggestions.getOrNull(ghostIndex) ?: "",
                        onGhostAccept = { accepted ->
                            query = accepted
                            ghostSuggestions = emptyList()
                            ghostIndex = 0
                            runSearch()
                        },
                        modifier = Modifier.align(Alignment.TopCenter),
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
                                )
                            }
                            itemsIndexed(likedList, key = { _, it -> it.url + "@" + it.speed }) { index, item ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                                    TrackCard(
                                        item = item,
                                        resolving = resolvingUrl == item.url,
                                        playing = nowPlaying?.url == item.url && isPlaying,
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
              modifier = Modifier.align(Alignment.BottomCenter),
          )
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
                    )
                },
            )
        }
      }
    }

        val current = nowPlaying
        AnimatedVisibility(
            visible = playerExpanded && current != null,
            enter = slideInVertically(
                animationSpec = tween(380, easing = androidx.compose.animation.core.CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
                initialOffsetY = { (it * 0.80f).toInt() },
            ) + scaleIn(
                animationSpec = tween(380, easing = androidx.compose.animation.core.CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
                initialScale = 0.82f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.96f),
            ) + fadeIn(tween(250)),
            exit = slideOutVertically(
                animationSpec = tween(320, easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
                targetOffsetY = { (it * 0.80f).toInt() },
            ) + scaleOut(
                animationSpec = tween(320, easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
                targetScale = 0.82f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.96f),
            ) + fadeOut(tween(260)),
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
                    onAddSpeedVariant = { sp -> speedVariantTarget = item to sp },
                    onSeek = { ms -> controller?.seekTo(ms) },
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = { playNext() },
                    onPrev = { playPrev() },
                    onToggleShuffle = { shuffle = !shuffle },
                    onToggleRepeat = { repeatOne = !repeatOne },
                    onToggleLike = { toggleLike(item) },
                    onShowQueue = { showQueue = true },
                    onFetchLyrics = { onFetchLyrics(item.title, item.uploader) },
                    onOpenArtist = { artistItem ->
                        playerExpanded = false
                        artistOpen = artistItem
                    },
                    onCollapse = { playerExpanded = false },
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
        item = contextMenuTrack,
        onDismiss = { contextMenuTrack = null },
    )

    speedVariantTarget?.let { (base, sp) ->
        SpeedVariantSheet(
            base = base,
            speed = sp,
            onFavorite = {
                FavoritesManager.toggle(speedVariant(base, sp))
                likedVersion++
                speedVariantTarget = null
            },
            onAddToPlaylist = { playlist ->
                PlaylistManager.addTrack(playlist.id, speedVariant(base, sp))
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

/** Делает «slowed»/«sped up» копию трека: тот же URL, своя скорость и пометка в названии. */
private fun speedVariant(base: TrackItem, speed: Float): TrackItem =
    base.copy(
        title = base.title + if (speed < 1f) " (slowed)" else " (sped up)",
        speed = speed,
    )

/** Лист сохранения slowed/sped up версии: в избранное или в плейлист. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedVariantSheet(
    base: TrackItem,
    speed: Float,
    onFavorite: () -> Unit,
    onAddToPlaylist: (Playlist) -> Unit,
    onDismiss: () -> Unit,
) {
    val playlists = remember { PlaylistManager.getAll() }
    val versionName = if (speed < 1f) "замедленную" else "ускоренную"
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                "Сохранить $versionName версию",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                base.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onFavorite() }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(14.dp))
                Text("В избранное", style = MaterialTheme.typography.bodyLarge)
            }

            if (playlists.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "В плейлист",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                )
                playlists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onAddToPlaylist(pl) }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(pl.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    tracks: List<TrackItem>,
    currentIndex: Int,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    onJump: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Очередь", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${tracks.size} треков",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (tracks.isEmpty()) {
                Text(
                    "Очередь пуста",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(tracks, key = { i, it -> "q_${i}_${it.url}" }) { i, t ->
                        val current = i == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onJump(i); onDismiss() }
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Artwork(
                                url = t.thumbnailUrl,
                                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                                    color = if (current) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                t.uploader?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (nowPlayingUrl == t.url) {
                                Icon(
                                    Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(2.dp))
                            }
                            // Управление очередью: вверх / вниз / удалить.
                            IconButton(
                                onClick = { onMove(i, i - 1) },
                                enabled = i > 0,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Вверх",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (i > 0) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                )
                            }
                            IconButton(
                                onClick = { onMove(i, i + 1) },
                                enabled = i < tracks.lastIndex,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Вниз",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (i < tracks.lastIndex) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                )
                            }
                            IconButton(
                                onClick = { onRemove(i) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Убрать",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class MeloTab(val label: String) {
    Home("Главная"),
    Favorite("Избранное"),
    Map("Карта"),
    Account("Аккаунт"),
}

@Composable
private fun MeloBottomNav(selected: MeloTab, onSelect: (MeloTab) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val tabs = remember {
        listOf(
            Triple(MeloTab.Home, Icons.Rounded.Home, MeloTab.Home.label),
            Triple(MeloTab.Favorite, Icons.Rounded.Favorite, MeloTab.Favorite.label),
            Triple(MeloTab.Map, Icons.Rounded.Map, MeloTab.Map.label),
            Triple(MeloTab.Account, Icons.Rounded.AccountCircle, MeloTab.Account.label),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp, top = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xF2141F19),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().height(62.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                tabs.forEach { (tab, icon, label) ->
                    val isSelected = selected == tab

                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) cs.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelect(tab) }
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) cs.onPrimaryContainer else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(22.dp),
                            )
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally(),
                            ) {
                                Row {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = cs.onPrimaryContainer,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Placeholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountTab(
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenLogin: () -> Unit,
    refreshKey: Int,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val accountScope = rememberCoroutineScope()
    var playlists by remember(refreshKey) { mutableStateOf(PlaylistManager.getAll().toList()) }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var menuTarget by remember { mutableStateOf<Playlist?>(null) }
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .bouncyOverscroll()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp)
            .padding(bottom = bottomInset),
    ) {
        // Шапка: заголовок в стиле M3 Expressive + шестерёнка настроек справа.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Аккаунт",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
                com.melo.music.auth.AuthManager.email?.let { mail ->
                    Text(
                        mail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (com.melo.music.auth.AuthManager.loggedIn) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { accountScope.launch { com.melo.music.auth.AuthManager.logout() } },
                    ) {
                        Text(
                            "Выйти",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = cs.primaryContainer,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenLogin),
                    ) {
                        Text(
                            "Войти",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenSettings),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Настройки",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // Профиль (Hero Card) — для вошедших и для гостей.
        if (com.melo.music.auth.AuthManager.loggedIn) {
            ProfileHeaderCard()
            val uid = com.melo.music.auth.AuthManager.userId
            val clipboard = LocalClipboardManager.current
            if (uid != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "ID: $uid (тап — копировать)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { clipboard.setText(AnnotatedString(uid)) },
                    )
                }
            }
        } else {
            // Bento Hero карточка для Гостя (в едином стиле темы приложения)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = cs.primaryContainer,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = cs.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Гостевой профиль",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Синхронизируй плейлисты и делись треками на карте",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onOpenLogin,
                        shape = RoundedCornerShape(24.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Войти в аккаунт", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bento-сетка быстрой статистики (Любимое + Плейлисты)
        val favCount = remember(refreshKey) { com.melo.music.favorites.FavoritesManager.getAll().size }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Карточка «Избранное»
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE53935).copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF6E6E),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "$favCount треков",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "В Избранном",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }

            // Карточка «Плейлисты»
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = cs.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.LibraryMusic,
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${playlists.size} шт.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "Мои плейлисты",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }

        // Секция «Мои плейлисты»
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Мои плейлисты",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp,
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = CircleShape,
                color = cs.primaryContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showCreate = true },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = cs.onPrimaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Создать",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onPrimaryContainer,
                    )
                }
            }
        }

        // Импорт плейлиста из других сервисов (Material 3 Expressive Frosted Card)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenImport),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Импорт плейлиста",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "Из YouTube Music или SoundCloud",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                )
            }
        }

        if (playlists.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Здесь будут твои плейлисты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Добавляй треки долгим тапом → «В плейлист»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = { showCreate = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Создать плейлист")
                }
            }
        } else {
            // Квадратные карточки по 2 в ряд.
            playlists.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    for (pl in row) {
                        PlaylistCard(
                            playlist = pl,
                            onClick = { onOpenPlaylist(pl) },
                            onLongClick = { menuTarget = pl },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        MeloDialog(onDismiss = { showCreate = false }) {
            Text(
                "Новый плейлист",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showCreate = false }) { Text("Отмена") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            PlaylistManager.create(name)
                            playlists = PlaylistManager.getAll().toList()
                        }
                        showCreate = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Создать") }
            }
        }
    }

    menuTarget?.let { target ->
        MeloDialog(onDismiss = { menuTarget = null }) {
            Text(target.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        PlaylistManager.setPublic(target.id, !target.isPublic)
                        playlists = PlaylistManager.getAll().toList()
                        menuTarget = null
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (target.isPublic) Icons.Rounded.Lock else Icons.Rounded.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(if (target.isPublic) "Сделать закрытым" else "Сделать открытым", fontWeight = FontWeight.Medium)
                    Text(
                        if (target.isPublic) "Сейчас виден другим" else "Сейчас скрыт",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { deleteTarget = target; menuTarget = null }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(14.dp))
                Text("Удалить плейлист", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
        }
    }

    deleteTarget?.let { target ->
        MeloDialog(onDismiss = { deleteTarget = null }) {
            Text(
                "Удалить плейлист?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "«${target.name}» будет удалён безвозвратно.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        PlaylistManager.delete(target.id)
                        playlists = PlaylistManager.getAll().toList()
                        deleteTarget = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Удалить") }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val placeholderBg = Color(0xFF26262C)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        if (playlist.coverUrl != null) {
            Artwork(url = playlist.coverUrl, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(placeholderBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.28f),
                    modifier = Modifier.size(46.dp),
                )
            }
        }

        // Скрим снизу для читаемости текста.
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.72f),
                ),
            ),
        )

        // Бейдж «закрытый» в углу.
        if (!playlist.isPublic) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(6.dp),
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = "Закрытый", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
            )
            Text(
                "${playlist.tracks.size} треков",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

/** Стилизованный диалог в духе приложения (скруглённый, с тенью и пружинистой анимацией). */
@Composable
private fun MeloDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val animState = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = animState,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.84f,
            ) + fadeIn(tween(180)),
            exit = scaleOut(tween(140), targetScale = 0.84f) + fadeOut(tween(140)),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp), content = content)
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit = {},
    ghostSuggestion: String = "",
    onGhostAccept: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ghostTail = remember(ghostSuggestion, query) {
        val q = query.trim()
        if (ghostSuggestion.isBlank() || q.isBlank()) ""
        else ghostSuggestion.removePrefix(q).removePrefix(" ").trimStart()
    }
    var prevGhost by remember { mutableStateOf("") }
    var animPhase by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(ghostTail) {
        if (ghostTail == prevGhost) return@LaunchedEffect
        if (prevGhost.isNotEmpty() && ghostTail.isNotEmpty()) {
            animPhase = 0f
            delay(200)
        }
        prevGhost = ghostTail
        animPhase = if (ghostTail.isNotEmpty()) 1f else 0f
    }
    val displayGhost = prevGhost

    val ghostAlpha by animateFloatAsState(
        targetValue = animPhase,
        animationSpec = tween(durationMillis = 300),
        label = "ghostAlpha",
    )
    val ghostBlur by animateFloatAsState(
        targetValue = if (animPhase > 0.5f) 0f else 8f,
        animationSpec = tween(durationMillis = 300),
        label = "ghostBlur",
    )

    val ghostColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            // Плашка поля поиска (под ghost-текстом и прозрачным TextField).
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    ) {
        // Ghost-текст: positioned behind TextField, aligned with text content area.
        if (displayGhost.isNotEmpty() && query.isNotBlank()) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.Transparent)) { append(query) }
                    withStyle(SpanStyle(color = ghostColor)) { append(" $displayGhost") }
                },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = ghostAlpha
                        if (ghostBlur > 1f) {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(ghostBlur, ghostBlur, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        } else {
                            renderEffect = null
                        }
                    }
                    .clickable { onGhostAccept(ghostSuggestion) }
                    // Отступ совпадает с contentPadding TextField + ширина иконки.
                    .padding(start = 50.dp, end = 16.dp, top = 15.dp, bottom = 13.dp),
            )
        }

        // TextField поверх ghost — прозрачный фон, чтобы ghost был виден.
        TextField(
            value = query,
            onValueChange = { onQueryChange(it) },
            placeholder = {
                Text("Поиск трека или исполнителя", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (displayGhost.isNotEmpty()) onGhostAccept(ghostSuggestion)
                onSearch()
            }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Главная: лента с полками (Material 3 Expressive) ──────────────────────────

/**
 * Выразительная карточка «Моя Волна» в стиле Material 3 Expressive.
 *
 * Особенности:
 * 1. Крупная живая типографика: бейдж времени суток (Утренний вайб / Дневной поток / Ночной чилл)
 *    + огромный акцентный заголовок «Твоя Волна» (38sp ExtraBold).
 * 2. Органический кластер обложек (Living Wave Cluster):
 *    Центральный суперэллипс (Squircle 26dp) с плавающими круглыми бабблами любимых артистов.
 * 3. Крупная тактильная кнопка Play (74dp) с пружинным масштабированием и встроенным Like / Next.
 */
@Composable
private fun SeaCard(
    loading: Boolean,
    playingTitle: String?,
    playingArtist: String?,
    isPlaying: Boolean,
    isLiked: Boolean,
    recentThumbs: List<String> = emptyList(),
    topArtistsHint: String = "",
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onLike: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val subtitle = remember(playingArtist, topArtistsHint) {
        when {
            !playingArtist.isNullOrBlank() -> playingArtist
            topArtistsHint.isNotBlank() -> "В стиле $topArtistsHint"
            else -> "Бесконечный персональный поток"
        }
    }

    // Мягкая плавающая анимация бабблов при воспроизведении
    val infiniteTransition = rememberInfiniteTransition(label = "bubbleFloat")
    val floatY1 by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fy1",
    )
    val floatY2 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(3100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fy2",
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    val currentFloat1 = if (isPlaying) floatY1 else 0f
    val currentFloat2 = if (isPlaying) floatY2 else 0f
    val currentPulse = if (isPlaying) pulseScale else 1.0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        // 1. Верхний блок: Заголовок + Большая кнопка Play
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                // Огромный выразительный заголовок M3 Expressive
                Text(
                    text = "Твоя\nВолна",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 38.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Крупная чистая кнопка Play M3 Expressive (72dp) без темного ореола
            val scaleBtn by animateFloatAsState(
                targetValue = currentPulse,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "btnScale",
            )

            Surface(
                shape = CircleShape,
                color = cs.primary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = scaleBtn
                        scaleY = scaleBtn
                    }
                    .clip(CircleShape)
                    .clickable(
                        enabled = !loading,
                        onClick = { onPlayPause() },
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = cs.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Волна",
                            tint = cs.onPrimary,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. Органический кластер обложек и управление
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().height(164.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Кластер органических обложек (центральный сквиркл + плавающие круглые бабблы)
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Главная центральная капсула / сквиркл
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = cs.surfaceVariant,
                        border = BorderStroke(1.5.dp, cs.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(26.dp)),
                    ) {
                        Artwork(
                            url = recentThumbs.firstOrNull(),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Баббл 1: Слева сверху
                    if (recentThumbs.size > 1) {
                        Surface(
                            shape = CircleShape,
                            color = cs.surfaceVariant,
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .size(42.dp)
                                .align(Alignment.TopStart)
                                .offset(x = (-4).dp, y = currentFloat1.dp)
                                .clip(CircleShape),
                        ) {
                            Artwork(
                                url = recentThumbs.getOrNull(1),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    // Баббл 2: Справа снизу
                    if (recentThumbs.size > 2) {
                        Surface(
                            shape = CircleShape,
                            color = cs.surfaceVariant,
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .size(46.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = currentFloat2.dp)
                                .clip(CircleShape),
                        ) {
                            Artwork(
                                url = recentThumbs.getOrNull(2),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Информация о треке и быстрые действия
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = playingTitle ?: "Бесконечный поток",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = playingArtist ?: "Персональные рекомендации",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Лайк капсула
                        Surface(
                            shape = CircleShape,
                            color = if (isLiked) cs.primaryContainer else Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onLike),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Лайк",
                                    tint = if (isLiked) cs.onPrimaryContainer else Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        // Следующий капсула
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onNext),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Дальше",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFeed(
    recommendations: List<TrackItem>,
    history: List<TrackItem>,
    loading: Boolean,
    onLoadShelf: suspend (String) -> List<TrackItem>,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onTrackLongClick: (TrackItem) -> Unit,
    onMood: (String) -> Unit,
    onOpenAccount: () -> Unit,
    onPrefetch: (String) -> Unit,
    seaLoading: Boolean,
    seaIsPlaying: Boolean,
    seaTitle: String?,
    seaArtist: String?,
    seaIsLiked: Boolean,
    onSeaPlayPause: () -> Unit,
    onSeaNext: () -> Unit,
    onSeaLike: () -> Unit,
    onRelatedTracks: suspend (TrackItem) -> List<TrackItem>,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val recTracks = recommendations.filter { it.kind == ItemKind.TRACK }.distinctBy { it.url }

    // Персонализированные полки из Taste Profile (с мгновенным кэшем при смене табов).
    var personalizedShelves by remember {
        mutableStateOf(Recommender.cachedShelves)
    }
    var quickPicks by remember {
        mutableStateOf(Recommender.cachedQuickPicks)
    }

    LaunchedEffect(history.size, recommendations.size) {
        if (personalizedShelves.isEmpty()) {
            personalizedShelves = Recommender.generatePersonalizedShelves(onLoadShelf, onRelatedTracks)
        }
        if (quickPicks.isEmpty()) {
            quickPicks = Recommender.generateQuickPicks(fallbackTracks = recTracks, related = onRelatedTracks)
        }
    }

    // Кэш полок живёт здесь (LazyColumn не уничтожается при скролле),
    // поэтому возврат к полке не перезапрашивает её.
    val shelfCache = remember { mutableStateMapOf<String, List<TrackItem>>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().bouncyOverscroll(),
        contentPadding = PaddingValues(top = topInset, bottom = 16.dp + bottomInset),
    ) {
        item(key = "home_greeting") { Greeting(onOpenAccount) }

        item(key = "home_sea") {
            val thumbs = remember(history, recommendations) {
                (history + recommendations).mapNotNull { it.thumbnailUrl }.distinct().take(3)
            }
            val topArtists = remember(history) {
                history.mapNotNull { it.uploader }.distinct().take(2).joinToString(", ")
            }
            SeaCard(
                loading = seaLoading,
                playingTitle = seaTitle,
                playingArtist = seaArtist,
                isPlaying = seaIsPlaying,
                isLiked = seaIsLiked,
                recentThumbs = thumbs,
                topArtistsHint = topArtists,
                onPlayPause = onSeaPlayPause,
                onNext = onSeaNext,
                onLike = onSeaLike,
            )
        }

        if (history.isNotEmpty()) {
            item(key = "home_history_title") { SectionTitle("Недавно слушали") }
            item(key = "home_history_row") {
                val histRowState = rememberLazyListState()
                LazyRow(
                    state = histRowState,
                    modifier = Modifier.bouncyHorizontalOverscroll(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(history.take(20), key = { idx, t -> "hist_${idx}_${t.url}" }) { index, t ->
                        ShelfCard(
                            item = t,
                            playing = nowPlayingUrl == t.url && isPlaying,
                            resolving = resolvingUrl == t.url,
                            onClick = { onPlay(history, history.indexOf(t)) },
                            onLongClick = { onTrackLongClick(t) },
                            modifier = Modifier.carouselCenterItemEffect(histRowState, index),
                        )
                    }
                }
            }
        }

        val displayedQuickPicks = if (quickPicks.isNotEmpty()) quickPicks else recTracks.take(12)
        if (displayedQuickPicks.isNotEmpty()) {
            item(key = "home_quick_title") { SectionTitle("Быстрый выбор") }
            item(key = "home_quick_grid") {
                QuickPickGrid(
                    displayedQuickPicks, loading && displayedQuickPicks.isEmpty(), nowPlayingUrl, isPlaying, resolvingUrl,
                    onPlay, onTrackLongClick,
                )
            }
        }

        // Персонализированные полки.
        if (personalizedShelves.isNotEmpty()) {
            personalizedShelves.forEachIndexed { idx, (title, tracks) ->
                item(key = "pers_title_$idx") { SectionTitle(title) }
                if (tracks.isEmpty()) {
                    item(key = "pers_shelf_$idx") {
                        HorizontalShelf(
                            title.lowercase(), shelfCache, onLoadShelf, nowPlayingUrl, isPlaying,
                            resolvingUrl, onPlay, onTrackLongClick, onPrefetch,
                        )
                    }
                } else {
                    item(key = "pers_shelf_$idx") {
                        val persRowState = rememberLazyListState()
                        LazyRow(
                            state = persRowState,
                            modifier = Modifier.bouncyHorizontalOverscroll(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(tracks, key = { itemIdx, t -> "pers_${idx}_${itemIdx}_${t.url}" }) { index, t ->
                                ShelfCard(
                                    item = t,
                                    playing = nowPlayingUrl == t.url && isPlaying,
                                    resolving = resolvingUrl == t.url,
                                    onClick = { onPlay(tracks, index) },
                                    onLongClick = { onTrackLongClick(t) },
                                    modifier = Modifier.carouselCenterItemEffect(persRowState, index),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Fallback
            item(key = "fb_title_1") { SectionTitle("Новинки") }
            item(key = "fb_shelf_1") {
                HorizontalShelf(
                    "новинки музыки 2026", shelfCache, onLoadShelf, nowPlayingUrl, isPlaying,
                    resolvingUrl, onPlay, onTrackLongClick, onPrefetch,
                )
            }

            item(key = "fb_title_2") { SectionTitle("Электроника") }
            item(key = "fb_shelf_2") {
                HorizontalShelf(
                    "electronic synthwave chill", shelfCache, onLoadShelf, nowPlayingUrl, isPlaying,
                    resolvingUrl, onPlay, onTrackLongClick, onPrefetch,
                )
            }

            item(key = "fb_title_3") { SectionTitle("Рок и Альтернатива") }
            item(key = "fb_shelf_3") {
                HorizontalShelf(
                    "rock indie alternative", shelfCache, onLoadShelf, nowPlayingUrl, isPlaying,
                    resolvingUrl, onPlay, onTrackLongClick, onPrefetch,
                )
            }
        }

        item(key = "rec_title") { SectionTitle("Рекомендуем") }
        if (loading) {
            item(key = "rec_loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
        itemsIndexed(recTracks, key = { index, it -> "rec_${index}_${it.url}" }) { index, t ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                TrackCard(
                    item = t,
                    resolving = resolvingUrl == t.url,
                    playing = nowPlayingUrl == t.url && isPlaying,
                    onClick = { onPlay(recTracks, index) },
                    onLongClick = { onTrackLongClick(t) },
                )
            }
        }
    }
}

@Composable
private fun Greeting(onOpenAccount: () -> Unit) {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greet = when (hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            com.melo.music.auth.AuthManager.avatarUrl,
            46.dp,
            Modifier.clip(CircleShape).clickable(onClick = onOpenAccount),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greet,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "Твоя музыка всегда с тобой",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 22.sp,
            letterSpacing = (-0.3).sp,
        ),
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
    )
}

@Composable
private fun QuickPickGrid(
    tracks: List<TrackItem>,
    loading: Boolean,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onLongClick: (TrackItem) -> Unit,
) {
    if (loading) {
        Box(
            modifier = Modifier.fillMaxWidth().height(148.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        return
    }
    if (tracks.isEmpty()) return
    val grid = tracks.distinctBy { it.url }.take(12)
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(156.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(grid, key = { "qp_" + it.url }) { t ->
            Row(
                modifier = Modifier
                    .width(268.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .combinedClickable(
                        onClick = { onPlay(grid, grid.indexOf(t)) },
                        onLongClick = { onLongClick(t) },
                    )
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(20.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(
                    url = t.thumbnailUrl,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        t.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                    )
                    t.uploader?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalShelf(
    seed: String,
    shelfCache: androidx.compose.runtime.snapshots.SnapshotStateMap<String, List<TrackItem>>,
    onLoadShelf: suspend (String) -> List<TrackItem>,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onLongClick: (TrackItem) -> Unit,
    onPrefetch: (String) -> Unit,
) {
    // Грузим один раз на сессию; данные живут в shelfCache (переживают скролл).
    LaunchedEffect(seed) {
        if (shelfCache[seed] == null) {
            val result = runCatching { onLoadShelf(seed) }.getOrDefault(emptyList())
            shelfCache[seed] = result.distinctBy { it.url }
        }
    }
    val tracks = shelfCache[seed]

    if (tracks == null) {
        Box(
            modifier = Modifier.fillMaxWidth().height(208.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        return
    }
    if (tracks.isEmpty()) return

    val shelfRowState = rememberLazyListState()
    LazyRow(
        state = shelfRowState,
        modifier = Modifier.bouncyHorizontalOverscroll(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(tracks, key = { _, t -> t.url }) { index, t ->
            ShelfCard(
                item = t,
                playing = nowPlayingUrl == t.url && isPlaying,
                resolving = resolvingUrl == t.url,
                onClick = { onPlay(tracks, tracks.indexOf(t)) },
                onLongClick = { onLongClick(t) },
                modifier = Modifier.carouselCenterItemEffect(shelfRowState, index),
            )
        }
    }
}

@Composable
private fun ShelfCard(
    item: TrackItem,
    playing: Boolean,
    resolving: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pressSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .width(150.dp)
            .pressScale(pressedScale = 0.95f, interactionSource = pressSource)
            .combinedClickable(
                interactionSource = pressSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier.size(150.dp).clip(ShapeCache.smooth16),
            contentAlignment = Alignment.Center,
        ) {
            Artwork(url = item.thumbnailUrl, modifier = Modifier.fillMaxSize())
            if (resolving) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp), strokeWidth = 2.dp) }
            } else if (playing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        item.uploader?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TrackCard(
    item: TrackItem,
    resolving: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color? = null,
) {
    // Контент-цвет под подложку: на тёмной карточке — белый.
    val onColor = if (containerColor != null && containerColor.luminance() < 0.5f) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subColor = if (containerColor != null && containerColor.luminance() < 0.5f) {
        Color.White.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val pressSource = remember { MutableInteractionSource() }
    ElevatedCard(
        shape = ShapeCache.smooth20,
        colors = if (containerColor != null) {
            androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = containerColor,
                contentColor = onColor,
            )
        } else {
            androidx.compose.material3.CardDefaults.elevatedCardColors()
        },
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource = pressSource)
            .combinedClickable(
                interactionSource = pressSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                url = item.thumbnailUrl,
                modifier = Modifier
                    .size(56.dp)
                    .clip(if (item.kind == ItemKind.ARTIST) CircleShape else ShapeCache.smooth12),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onColor,
                    maxLines = 2,
                )
                val subtitle = listOfNotNull(
                    item.uploader,
                    formatDuration(item.durationSeconds),
                ).joinToString(" · ")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Флаг офлайн: трек скачан и играет с диска.
                    val offline = com.melo.music.offline.OfflineManager.version.let {
                        com.melo.music.offline.OfflineManager.isOffline(item.url)
                    }
                    if (offline) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadForOffline,
                            contentDescription = "Офлайн",
                            tint = Color(0xFF35C759),
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    SourceBadge(item.source, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = subtitle.ifBlank { sourceLabel(item.source) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = subColor,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            val iconTint = if (containerColor != null && containerColor.luminance() < 0.5f) {
                Color.White
            } else {
                MaterialTheme.colorScheme.primary
            }
            when {
                item.kind == ItemKind.ARTIST -> Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Песни исполнителя",
                    tint = iconTint,
                    modifier = Modifier.padding(end = 6.dp),
                )
                resolving -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = iconTint,
                )
                else -> Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ArtistCard(item: TrackItem, onClick: () -> Unit) {
    val pressSource = remember { MutableInteractionSource() }
    Surface(
        shape = ShapeCache.smooth20,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource = pressSource)
            .clickable(
                interactionSource = pressSource,
                indication = ripple(),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                url = item.thumbnailUrl,
                modifier = Modifier.size(64.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(item.source, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Исполнитель",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/** Разнообразный экран результатов поиска: hero-трек, исполнители, альбомы, сетка и список. */
@Composable
private fun SearchResultsScreen(
    rawItems: List<TrackItem>,
    users: List<com.melo.music.profile.MeloProfile>,
    query: String,
    loadingMore: Boolean,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    state: androidx.compose.foundation.lazy.LazyListState,
    topInset: androidx.compose.ui.unit.Dp,
    bottomInset: androidx.compose.ui.unit.Dp,
    onPlayFrom: (List<TrackItem>, Int) -> Unit,
    onShuffleAll: () -> Unit,
    onOpenArtist: (TrackItem) -> Unit,
    onOpenUser: (com.melo.music.profile.MeloProfile) -> Unit,
    onTrackLongClick: (TrackItem) -> Unit,
    onLoadAlbumTracks: suspend (String) -> List<TrackItem>,
) {
    val tracks = remember(rawItems) { rawItems.filter { it.kind == ItemKind.TRACK }.distinctBy { it.url } }
    val artists = remember(rawItems) { rawItems.filter { it.kind == ItemKind.ARTIST }.distinctBy { it.url } }
    val albums = remember(rawItems) { rawItems.filter { it.kind == ItemKind.ALBUM }.distinctBy { it.url } }
    val hero = tracks.firstOrNull()
    val rest = tracks.drop(1)

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().bouncyOverscroll(),
        contentPadding = PaddingValues(top = topInset + 4.dp, bottom = 16.dp + bottomInset),
    ) {
        if (users.isNotEmpty()) {
            item(key = "people_header") { SectionTitle("Люди") }
            items(users, key = { "u_" + it.userId }) { p ->
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    com.melo.music.ui.UserRow(profile = p, onClick = { onOpenUser(p) })
                }
            }
        }

        if (hero != null) {
            item(key = "hero_" + hero.url) {
                Column {
                    SectionTitle("Главный результат")
                    TopResultCard(
                        item = hero,
                        playing = nowPlayingUrl == hero.url && isPlaying,
                        resolving = resolvingUrl == hero.url,
                        onClick = { onPlayFrom(tracks, 0) },
                        onLongClick = { onTrackLongClick(hero) },
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilledTonalButton(onClick = { onPlayFrom(tracks, 0) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Слушать")
                        }
                        OutlinedButton(onClick = onShuffleAll, enabled = tracks.size > 1) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Перемешать")
                        }
                    }
                }
            }
        }

        if (artists.isNotEmpty()) {
            item(key = "artists_header") { SectionTitle("Исполнители") }
            item(key = "artists_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(artists, key = { _, a -> "ar_" + a.url }) { _, a ->
                        SearchArtistTile(item = a, onClick = { onOpenArtist(a) })
                    }
                }
            }
        }

        if (albums.isNotEmpty()) {
            item(key = "albums_header") { SectionTitle("Альбомы и синглы") }
            itemsIndexed(albums.take(8), key = { _, a -> "al_" + a.url }) { _, a ->
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                ) {
                    SearchAlbumCard(
                        album = a,
                        nowPlayingUrl = nowPlayingUrl,
                        isPlaying = isPlaying,
                        resolvingUrl = resolvingUrl,
                        onLoadTracks = { onLoadAlbumTracks(a.url) },
                        onPlay = onPlayFrom,
                        onTrackLongClick = onTrackLongClick,
                    )
                }
            }
        }

        if (rest.isNotEmpty()) {
            item(key = "quick_header") { SectionTitle("Треки") }
            item(key = "quick_grid") {
                QuickPickGrid(
                    tracks = rest.take(8),
                    loading = false,
                    nowPlayingUrl = nowPlayingUrl,
                    isPlaying = isPlaying,
                    resolvingUrl = resolvingUrl,
                    onPlay = onPlayFrom,
                    onLongClick = onTrackLongClick,
                )
            }
            val tail = rest.drop(8)
            if (tail.isNotEmpty()) {
                item(key = "tail_header") { SectionTitle("Ещё") }
                itemsIndexed(tail, key = { _, t -> "t_" + t.url }) { _, t ->
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    ) {
                        TrackCard(
                            item = t,
                            resolving = resolvingUrl == t.url,
                            playing = nowPlayingUrl == t.url && isPlaying,
                            onClick = { onPlayFrom(tracks, tracks.indexOf(t)) },
                            onLongClick = { onTrackLongClick(t) },
                        )
                    }
                }
            }
        }

        if (loadingMore) {
            item(key = "footer_loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

/** Крупная карточка «Главный результат»: обложка на всю ширину с градиентом. */
@Composable
private fun TopResultCard(
    item: TrackItem,
    playing: Boolean,
    resolving: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val pressSource = remember { MutableInteractionSource() }
    ElevatedCard(
        shape = ShapeCache.smooth24,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pressScale(pressedScale = 0.97f, interactionSource = pressSource)
            .combinedClickable(
                interactionSource = pressSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box {
            Artwork(
                url = item.thumbnailUrl,
                modifier = Modifier.fillMaxWidth().height(190.dp),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, end = 72.dp, bottom = 12.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(item.source, Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = listOfNotNull(
                            item.uploader ?: sourceLabel(item.source),
                            formatDuration(item.durationSeconds).takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FilledIconButton(
                onClick = onClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(46.dp),
            ) {
                when {
                    resolving -> CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black,
                    )
                    else -> Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

/**
 * Карточка альбома в поиске: тап — раскрыть треки, play — слушать весь альбом.
 * Тематические цвета (в отличие от тёмного варианта на экране исполнителя).
 */
@Composable
private fun SearchAlbumCard(
    album: TrackItem,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    onLoadTracks: suspend () -> List<TrackItem>,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onTrackLongClick: (TrackItem) -> Unit,
) {
    var expanded by remember(album.url) { mutableStateOf(false) }
    var tracks by remember(album.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var loading by remember(album.url) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White

    fun load(then: (List<TrackItem>) -> Unit = {}) {
        if (tracks.isNotEmpty()) { then(tracks); return }
        if (loading) return
        loading = true
        scope.launch {
            val r = runCatching { onLoadTracks() }.getOrDefault(emptyList())
            tracks = r
            loading = false
            then(r)
        }
    }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "searchAlbumChev")

    ElevatedCard(
        shape = ShapeCache.smooth20,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { expanded = !expanded; if (expanded) load() },
                        onLongClick = { onTrackLongClick(album) },
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(album.thumbnailUrl, Modifier.size(56.dp).clip(ShapeCache.smooth12))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        album.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (tracks.isNotEmpty()) "Альбом · ${tracks.size} треков" else "Альбом",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                FilledIconButton(
                    onClick = { load { if (it.isNotEmpty()) onPlay(it, 0) } },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accent,
                        contentColor = onAccent,
                    ),
                ) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Играть альбом") }
                IconButton(onClick = { expanded = !expanded; if (expanded) load() }) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    if (loading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        tracks.forEachIndexed { i, t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onPlay(tracks, i) },
                                        onLongClick = { onTrackLongClick(t) },
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                 verticalAlignment = Alignment.CenterVertically,
                             ) {
                                 Artwork(t.thumbnailUrl, Modifier.size(40.dp).clip(ShapeCache.smooth8))
                                 Spacer(Modifier.width(12.dp))
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text(
                                         t.title,
                                         style = MaterialTheme.typography.bodyLarge,
                                         maxLines = 1,
                                         overflow = TextOverflow.Ellipsis,
                                         fontWeight = if (nowPlayingUrl == t.url) FontWeight.Bold else FontWeight.Normal,
                                     )
                                     t.uploader?.let {
                                         Text(
                                             it,
                                             style = MaterialTheme.typography.bodySmall,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                                             maxLines = 1,
                                         )
                                     }
                                 }
                                 if (resolvingUrl == t.url) {
                                     CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                 } else if (nowPlayingUrl == t.url && isPlaying) {
                                     Icon(
                                         Icons.Rounded.MusicNote, contentDescription = null,
                                         tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp),
                                     )
                                 }
                             }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun SearchArtistTile(item: TrackItem, onClick: () -> Unit) {
    val pressSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(104.dp)
            .pressScale(pressedScale = 0.94f, interactionSource = pressSource)
            .clickable(
                interactionSource = pressSource,
                indication = ripple(),
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(
            url = item.thumbnailUrl,
            modifier = Modifier.size(104.dp).clip(CircleShape),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Исполнитель",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Пустой результат поиска. */
@Composable
private fun SearchEmptyState(query: String, topInset: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = topInset),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 110.dp, start = 32.dp, end = 32.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Ничего не нашлось",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "По запросу «$query» нет результатов",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}


@Composable
private fun ArtistScreen(
    artist: TrackItem,
    onLoadTracks: suspend () -> List<TrackItem>,
    onLoadAlbums: suspend () -> List<TrackItem>,
    onLoadSimilar: suspend (List<TrackItem>) -> List<TrackItem>,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    audioSessionId: Int,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onShuffle: (List<TrackItem>) -> Unit,
    onLoadAlbumTracks: suspend (String) -> List<TrackItem>,
    onTrackLongClick: (TrackItem) -> Unit,
    onOpenSimilar: (TrackItem) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var tracks by remember(artist.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var albums by remember(artist.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var similar by remember(artist.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var loading by remember(artist.url) { mutableStateOf(true) }
    var error by remember(artist.url) { mutableStateOf<String?>(null) }

    LaunchedEffect(artist.url) {
        loading = true
        error = null
        runCatching { onLoadTracks() }
            .onSuccess { tracks = it }
            .onFailure { error = it.message }
        loading = false
    }
    LaunchedEffect(artist.url) {
        albums = runCatching { onLoadAlbums() }.getOrDefault(emptyList())
    }
    LaunchedEffect(tracks) {
        if (tracks.isEmpty() || similar.isNotEmpty()) return@LaunchedEffect
        similar = runCatching { onLoadSimilar(tracks) }.getOrDefault(emptyList())
    }

    val popular = remember(tracks) { tracks.sortedByDescending { it.viewCount }.take(5) }
    val popularUrls = remember(popular) { popular.map { it.url }.toSet() }
    val otherTracks = tracks.filter { it.url !in popularUrls }

    val hiRes = remember(artist.thumbnailUrl) { upscaleThumb(artist.thumbnailUrl, 700) }
    val white = Color.White

    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.primary
    var accent by remember(artist.url) { mutableStateOf(fallback) }
    LaunchedEffect(hiRes) {
        val url = hiRes ?: return@LaunchedEffect
        runCatching {
            val req = ImageRequest.Builder(context).data(url).allowHardware(false).size(256).build()
            val drawable = context.imageLoader.execute(req).drawable
            val bmp = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bmp != null) {
                val palette = androidx.palette.graphics.Palette.from(bmp).generate()
                val rgb = palette.vibrantSwatch?.rgb
                    ?: palette.lightVibrantSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                if (rgb != null) accent = Color(rgb)
            }
        }
    }

    val animatedAccent by animateColorAsState(targetValue = accent, animationSpec = tween(500), label = "artistAccent")
    val cardTint = lerp(animatedAccent, Color(0xFF141218), 0.78f)
    val onAccent = if (animatedAccent.luminance() > 0.45f) Color.Black else Color.White
    val pageBg = Color(0xFF0E0C11)

    Box(modifier = Modifier.fillMaxSize().background(pageBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().bouncyOverscroll(),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp),
                ) {
                    Artwork(
                        url = hiRes ?: artist.thumbnailUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.25f),
                                    0.40f to animatedAccent.copy(alpha = 0.20f),
                                    0.75f to pageBg.copy(alpha = 0.88f),
                                    1f to pageBg,
                                ),
                            ),
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 14.dp, start = 16.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClose),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Назад",
                                tint = white,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            ) {
                                SourceBadge(artist.source, Modifier.size(13.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "АРТИСТ · ${sourceLabel(artist.source).uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = white.copy(alpha = 0.90f),
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = artist.title,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 34.sp,
                                lineHeight = 36.sp,
                                letterSpacing = (-0.6).sp,
                            ),
                            fontWeight = FontWeight.Black,
                            color = white,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = if (tracks.isNotEmpty()) {
                                "${tracks.size} треков${if (albums.isNotEmpty()) " · ${albums.size} альбомов" else ""}"
                            } else {
                                "Загрузка дискографии..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = white.copy(alpha = 0.70f),
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = animatedAccent,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .clickable(enabled = tracks.isNotEmpty()) { onPlay(tracks, 0) },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 22.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = onAccent,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Слушать",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = onAccent,
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = tracks.isNotEmpty()) { onShuffle(tracks) },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Перемешать",
                                        tint = white,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (popular.isNotEmpty()) {
                item(key = "popular_header") {
                    Text(
                        text = "Популярное",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = white,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(popular, key = { _, t -> "pop_" + t.url }) { i, t ->
                    val isCurrent = nowPlayingUrl == t.url
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isCurrent) animatedAccent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (isCurrent) animatedAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .combinedClickable(
                                onClick = { onPlay(tracks, tracks.indexOf(t)) },
                                onLongClick = { onTrackLongClick(t) },
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${i + 1}",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) animatedAccent else white.copy(alpha = 0.40f),
                                modifier = Modifier.width(24.dp),
                            )
                            Artwork(t.thumbnailUrl, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = white,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val sub = listOfNotNull(
                                    formatViews(t.viewCount).takeIf { it.isNotBlank() },
                                    formatDuration(t.durationSeconds).takeIf { it.isNotBlank() },
                                    HistoryManager.playCount(t.url).takeIf { it > 0 }?.let { "слушали $it раз" },
                                ).joinToString(" · ")
                                if (sub.isNotBlank()) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = white.copy(alpha = 0.60f),
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (resolvingUrl == t.url) {
                                CircularProgressIndicator(color = animatedAccent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else if (isCurrent && isPlaying) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = animatedAccent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (albums.isNotEmpty()) {
                item(key = "albums_header") {
                    Text(
                        text = "Альбомы и релизы",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = white,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
                    )
                }
                items(albums.distinctBy { it.url }, key = { it.url }) { al ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                        AlbumCard(
                            album = al,
                            accent = animatedAccent,
                            onAccent = onAccent,
                            cardTint = cardTint,
                            nowPlayingUrl = nowPlayingUrl,
                            isPlaying = isPlaying,
                            resolvingUrl = resolvingUrl,
                            onLoadTracks = { onLoadAlbumTracks(al.url) },
                            onPlay = onPlay,
                            onTrackLongClick = onTrackLongClick,
                        )
                    }
                }
            }

            when {
                loading -> item(key = "tracks_loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = animatedAccent)
                    }
                }
                error != null && tracks.isEmpty() -> item(key = "tracks_error") {
                    Text(
                        text = "Не удалось загрузить дискографию: $error",
                        color = white.copy(alpha = 0.8f),
                        modifier = Modifier.padding(24.dp),
                    )
                }
                tracks.isEmpty() -> item(key = "tracks_empty") {
                    Text(
                        text = "Треки не найдены",
                        color = white.copy(alpha = 0.8f),
                        modifier = Modifier.padding(24.dp),
                    )
                }
                otherTracks.isNotEmpty() -> {
                    item(key = "all_tracks_header") {
                        Text(
                            text = "Все треки",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 21.sp,
                                letterSpacing = (-0.3).sp,
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = white,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
                        )
                    }
                    val rows = otherTracks.chunked(2)
                    itemsIndexed(rows, key = { i, row -> i.toString() + row.joinToString("|") { it.url } }) { _, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { t ->
                                ArtistTrackTile(
                                    item = t,
                                    playing = nowPlayingUrl == t.url,
                                    resolving = resolvingUrl == t.url,
                                    accent = animatedAccent,
                                    onClick = { onPlay(tracks, tracks.indexOf(t)) },
                                    onLongClick = { onTrackLongClick(t) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (similar.isNotEmpty()) {
                item(key = "similar_header") {
                    Text(
                        text = "Похожие артисты",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = white,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                    )
                }
                item(key = "similar_row") {
                    val simRowState = rememberLazyListState()
                    LazyRow(
                        state = simRowState,
                        modifier = Modifier.bouncyHorizontalOverscroll(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(similar, key = { _, a -> "sim_" + a.url }) { index, a ->
                            SimilarArtistTile(
                                item = a,
                                accent = animatedAccent,
                                onClick = { onOpenSimilar(a) },
                                modifier = Modifier.carouselCenterItemEffect(simRowState, index),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: TrackItem,
    accent: Color,
    onAccent: Color,
    cardTint: Color,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    onLoadTracks: suspend () -> List<TrackItem>,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onTrackLongClick: (TrackItem) -> Unit,
) {
    val white = Color.White
    var expanded by remember(album.url) { mutableStateOf(false) }
    var tracks by remember(album.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var loading by remember(album.url) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load(then: (List<TrackItem>) -> Unit = {}) {
        if (tracks.isNotEmpty()) { then(tracks); return }
        if (loading) return
        loading = true
        scope.launch {
            val r = runCatching { onLoadTracks() }.getOrDefault(emptyList())
            tracks = r
            loading = false
            then(r)
        }
    }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chev")

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardTint,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded; if (expanded) load() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(album.thumbnailUrl, Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = white,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (tracks.isNotEmpty()) "Альбом · ${tracks.size} треков" else "Альбом",
                        style = MaterialTheme.typography.bodySmall,
                        color = white.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { load { if (it.isNotEmpty()) onPlay(it, 0) } },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Играть альбом",
                            tint = onAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded; if (expanded) load() }) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = white,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp)) {
                    if (loading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        tracks.forEachIndexed { i, t ->
                            val isTrackCurrent = nowPlayingUrl == t.url
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isTrackCurrent) accent.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .combinedClickable(
                                        onClick = { onPlay(tracks, i) },
                                        onLongClick = { onTrackLongClick(t) },
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Artwork(t.thumbnailUrl, Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = t.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = white,
                                            maxLines = 1,
                                            fontWeight = if (isTrackCurrent) FontWeight.Bold else FontWeight.Normal,
                                        )
                                        t.uploader?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = white.copy(alpha = 0.65f),
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                    if (resolvingUrl == t.url) {
                                        CircularProgressIndicator(
                                            color = accent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                                        )
                                    } else if (isTrackCurrent && isPlaying) {
                                        Icon(
                                            Icons.Rounded.MusicNote, contentDescription = null,
                                            tint = accent, modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Плитка трека в 2-колоночной сетке «Все треки» (Material 3 Expressive). */
@Composable
private fun ArtistTrackTile(
    item: TrackItem,
    playing: Boolean,
    resolving: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val white = Color.White
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (playing) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (playing) accent.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.06f)),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Artwork(item.thumbnailUrl, Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)))
                if (resolving) {
                    Box(
                        modifier = Modifier.size(46.dp).background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium,
                    color = white,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.uploader?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = white.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (playing) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Круглая выразительная плитка похожего артиста с неоновым обрамлением. */
@Composable
private fun SimilarArtistTile(
    item: TrackItem,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(2.dp, accent.copy(alpha = 0.50f)),
            modifier = Modifier.size(76.dp),
        ) {
            Artwork(
                url = item.thumbnailUrl,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Короткий формат просмотров: 1234567 → «1,2 млн». */
private fun formatViews(v: Long): String = when {
    v >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f млн", v / 1_000_000.0)
    v >= 1_000 -> String.format(Locale.getDefault(), "%.0f тыс.", v / 1_000.0)
    v > 0 -> v.toString()
    else -> ""
}

@Composable
private fun PlaylistScreen(
    playlist: Playlist,    nowPlayingUrl: String?,
    nowPlayingSpeed: Float,
    isPlaying: Boolean,
    resolvingUrl: String?,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onTrackLongClick: (TrackItem) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val tracks = playlist.tracks
    val hiRes = remember(playlist.coverUrl) { upscaleThumb(playlist.coverUrl, 600) }
    val white = Color.White

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E12))) {
        if (hiRes != null) {
            AsyncImage(
                model = hiRes,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(60.dp),
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.88f)),
                ),
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().bouncyOverscroll(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Закрыть", tint = white)
                        }
                    }
                    Box(
                        modifier = Modifier.size(160.dp).clip(RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (playlist.coverUrl != null) {
                            Artwork(url = playlist.coverUrl, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary,
                                        ),
                                    ),
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = white,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${tracks.size} треков",
                        style = MaterialTheme.typography.bodyMedium,
                        color = white.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (tracks.isEmpty()) {
                item {
                    Text(
                        text = "Плейлист пуст. Добавляй треки через «···» на карточке трека.",
                        color = white.copy(alpha = 0.8f),
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                itemsIndexed(tracks, key = { i, it -> "$i:${it.url}@${it.speed}" }) { index, t ->
                    TrackCard(
                        item = t,
                        resolving = resolvingUrl == t.url,
                        playing = nowPlayingUrl == t.url &&
                            kotlin.math.abs(nowPlayingSpeed - t.speed) < 0.01f && isPlaying,
                        onClick = { onPlay(tracks, index) },
                        onLongClick = { onTrackLongClick(t) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesHeader(
    tracks: List<TrackItem>,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    val covers = remember(tracks) { tracks.mapNotNull { it.thumbnailUrl }.distinct() }
    val topArtist = remember(tracks) {
        tracks.mapNotNull { it.uploader?.takeIf { u -> u.isNotBlank() } }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }
    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        CollageBackground(covers)
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.2f to Color.Black.copy(alpha = 0.25f),
                    1f to Color.Black.copy(alpha = 0.9f),
                ),
            ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
        ) {
            Text(
                "Избранное",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${tracks.size} треков" + (topArtist?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
            )
            Spacer(Modifier.height(14.dp))
            var pub by remember { mutableStateOf(FavoritesManager.isPublic()) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onPlayAll) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Слушать")
                }
                FilledTonalIconButton(onClick = onShuffle) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Перемешать")
                }
                FilledTonalIconButton(onClick = { pub = !pub; FavoritesManager.setPublic(pub) }) {
                    Icon(
                        if (pub) Icons.Rounded.Public else Icons.Rounded.Lock,
                        contentDescription = if (pub) "Открыто" else "Закрыто",
                    )
                }
            }
            Text(
                if (pub) "Лайки видны другим" else "Лайки скрыты",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun CollageBackground(covers: List<String>) {
    if (covers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
            ),
        )
        return
    }
    val four = remember(covers) { List(4) { covers[it % covers.size] } }
    Column(modifier = Modifier.fillMaxSize().blur(22.dp)) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Artwork(four[0], modifier = Modifier.weight(1f).fillMaxHeight())
            Artwork(four[1], modifier = Modifier.weight(1f).fillMaxHeight())
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Artwork(four[2], modifier = Modifier.weight(1f).fillMaxHeight())
            Artwork(four[3], modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

private fun sourceLabel(source: Source): String = when (source) {
    Source.YOUTUBE_MUSIC -> "YouTube Music"
    Source.SOUNDCLOUD -> "SoundCloud"
    Source.BANDCAMP -> "Bandcamp"
    Source.DEEZER -> "Deezer"
    Source.TIDAL -> "Tidal"
    Source.LOCAL -> "На устройстве"
}

@Composable
private fun SourceBadge(source: Source, modifier: Modifier = Modifier) {
    when (source) {
        Source.SOUNDCLOUD -> Icon(
            imageVector = Icons.Rounded.Cloud,
            contentDescription = "SoundCloud",
            tint = Color(0xFFFF5500),
            modifier = modifier,
        )
        Source.YOUTUBE_MUSIC -> Icon(
            imageVector = Icons.Rounded.PlayCircle,
            contentDescription = "YouTube Music",
            tint = Color(0xFFFF0000),
            modifier = modifier,
        )
        Source.BANDCAMP -> Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = "Bandcamp",
            tint = Color(0xFF629AA9),
            modifier = modifier,
        )
        Source.DEEZER -> Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = "Deezer",
            tint = Color(0xFFA238FF),
            modifier = modifier,
        )
        Source.TIDAL -> Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = "Tidal",
            tint = Color(0xFF000000),
            modifier = modifier,
        )
        Source.LOCAL -> Icon(
            imageVector = Icons.Rounded.DownloadForOffline,
            contentDescription = "На устройстве",
            tint = Color(0xFF35C759),
            modifier = modifier,
        )
    }
}

/** Таймер сна: кнопка с выпадающим меню (15/30/45/60 мин, до конца трека, выкл). */
@Composable
private fun SleepTimerControl(white: Color, accent: Color) {
    var menuOpen by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(com.melo.music.playback.PlaybackService.sleepActive()) }
    var remaining by remember { mutableLongStateOf(com.melo.music.playback.PlaybackService.sleepRemainingMs()) }
    var endOfTrack by remember { mutableStateOf(com.melo.music.playback.PlaybackService.sleepEndOfTrack) }
    LaunchedEffect(Unit) {
        while (true) {
            active = com.melo.music.playback.PlaybackService.sleepActive()
            remaining = com.melo.music.playback.PlaybackService.sleepRemainingMs()
            endOfTrack = com.melo.music.playback.PlaybackService.sleepEndOfTrack
            delay(1000)
        }
    }
    val label = when {
        endOfTrack -> "До конца трека"
        remaining > 0 -> "Сон через ${(remaining / 60000) + 1} мин"
        else -> "Таймер сна"
    }
    Box {
        TextButton(onClick = { menuOpen = true }) {
            Icon(
                Icons.Rounded.Bedtime,
                contentDescription = "Таймер сна",
                tint = if (active) accent else white.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(label, color = if (active) accent else white.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            listOf(15, 30, 45, 60).forEach { m ->
                DropdownMenuItem(
                    text = { Text("$m минут") },
                    onClick = {
                        com.melo.music.playback.PlaybackService.setSleepTimerMinutes(m)
                        active = true; menuOpen = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("До конца трека") },
                onClick = {
                    com.melo.music.playback.PlaybackService.setSleepEndOfTrack()
                    active = true; menuOpen = false
                },
            )
            if (active) {
                DropdownMenuItem(
                    text = { Text("Выключить таймер") },
                    onClick = {
                        com.melo.music.playback.PlaybackService.cancelSleepTimer()
                        active = false; menuOpen = false
                    },
                )
            }
        }
    }
}

@Composable
fun Artwork(url: String?, modifier: Modifier = Modifier) {
    if (url != null) {
        val context = LocalContext.current
        val request = remember(url) {
            coil.request.ImageRequest.Builder(context)
                .data(url)
                .memoryCacheKey(url)
                .diskCacheKey(url)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .crossfade(false)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

/** Material 3 Expressive Scalloped / Starburst Shape (как на фото) */
private class ScallopedShape(
    val petals: Int = 8,
    val depth: Float = 0.14f,
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = kotlin.math.min(cx, cy)
        val minR = maxR * (1f - depth)
        val totalPoints = petals * 2
        val step = (2.0 * Math.PI / totalPoints).toFloat()

        val points = (0 until totalPoints).map { i ->
            val angle = i * step - (Math.PI / 2).toFloat()
            val r = if (i % 2 == 0) maxR else minR
            androidx.compose.ui.geometry.Offset(
                cx + r * kotlin.math.cos(angle.toDouble()).toFloat(),
                cy + r * kotlin.math.sin(angle.toDouble()).toFloat(),
            )
        }

        path.moveTo(
            (points[0].x + points.last().x) / 2f,
            (points[0].y + points.last().y) / 2f,
        )

        for (i in points.indices) {
            val p = points[i]
            val next = points[(i + 1) % points.size]
            val midX = (p.x + next.x) / 2f
            val midY = (p.y + next.y) / 2f
            path.quadraticTo(p.x, p.y, midX, midY)
        }

        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

@Composable
private fun NowPlayingBar(
    item: TrackItem?,
    isPlaying: Boolean,
    resolving: Boolean,
    error: String?,
    onTogglePlayPause: () -> Unit,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (item == null) return
    val context = LocalContext.current
    val scallopedBtn = remember { ScallopedShape(petals = 8, depth = 0.15f) }
    val scallopedArt = remember { ScallopedShape(petals = 10, depth = 0.12f) }

    // Извлечение доминантного цвета из обложки трека в реальном времени
    var trackColor by remember(item.thumbnailUrl) { mutableStateOf(Color(0xFFD4A853)) }
    LaunchedEffect(item.thumbnailUrl) {
        val url = item.thumbnailUrl ?: return@LaunchedEffect
        runCatching {
            val req = coil.request.ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(128)
                .build()
            val drawable = coil.Coil.imageLoader(context).execute(req).drawable
            val bmp = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bmp != null) {
                val palette = androidx.palette.graphics.Palette.from(bmp).generate()
                val rgb = palette.vibrantSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                    ?: palette.mutedSwatch?.rgb
                    ?: palette.lightVibrantSwatch?.rgb
                if (rgb != null) {
                    trackColor = Color(rgb)
                }
            }
        }
    }

    val animatedTrackColor by animateColorAsState(
        targetValue = trackColor,
        animationSpec = tween(650),
        label = "miniPlayerAccentColor",
    )

    // Плавный непредсказуемый органичный дрейф волн (без повторений и не в бит)
    val infiniteTransition = rememberInfiniteTransition(label = "organicFluidDrift")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase1",
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase2",
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase3",
    )

    Surface(
        // Матовое полупрозрачное стекло
        color = Color(0xEB13110E),
        shape = RoundedCornerShape(42.dp),
        shadowElevation = 18.dp,
        border = BorderStroke(1.2.dp, lerp(Color(0x55FFFFFF), animatedTrackColor, 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                // Мягкий плавный фоновый градиент и матрица точек
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    // 1. Матовое темное стекло (база)
                    drawRect(color = Color(0xEB13110E))

                    // 2. Живой плавающий волновой градиент (дышит и плавно колышется по высоте и яркости)
                    val waveShift = kotlin.math.sin(phase1.toDouble() * 1.5).toFloat() * (h * 0.15f)
                    val startY = if (isPlaying) (h * 0.10f + waveShift).coerceIn(0f, h * 0.40f) else h * 0.35f
                    val glowAlpha = if (isPlaying) (0.62f + kotlin.math.sin(phase2.toDouble() * 2.0).toFloat() * 0.18f).coerceIn(0.30f, 0.88f) else 0.16f

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                animatedTrackColor.copy(alpha = if (isPlaying) glowAlpha * 0.35f else 0.04f),
                                animatedTrackColor.copy(alpha = if (isPlaying) glowAlpha else 0.16f),
                            ),
                            startY = startY,
                            endY = h,
                        ),
                    )

                    // 3. Точки матрицы: сверху ПОЛНОСТЬЮ исчезают (alpha = 0), плавно нарастают книзу
                    val dotSpacing = 16.dp.toPx()
                    val dotRadius = 1.6.dp.toPx()
                    val cols = (w / dotSpacing).toInt() + 1
                    val rows = (h / dotSpacing).toInt() + 1
                    for (r in 0..rows) {
                        val yFactor = (r.toFloat() / rows.coerceAtLeast(1)).coerceIn(0f, 1f)
                        // Сверху (первые 40% высоты) точки полностью отсутствуют
                        if (yFactor > 0.40f) {
                            val progress = ((yFactor - 0.40f) / 0.60f).coerceIn(0f, 1f)
                            val verticalAlpha = (progress * progress * progress * 0.88f) * if (isPlaying) 1.0f else 0.45f
                            if (verticalAlpha > 0.005f) {
                                for (c in 0..cols) {
                                    drawCircle(
                                        color = lerp(Color(0xFFF5E6CC), animatedTrackColor, 0.75f).copy(alpha = verticalAlpha),
                                        radius = dotRadius,
                                        center = Offset(
                                            x = c * dotSpacing + (dotSpacing / 2),
                                            y = r * dotSpacing + (dotSpacing / 2),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Левая зона (обложка + название + автор) — при тапе раскрывает полный плеер
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(onClick = onClick),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Обложка в фигурной волнистой рамке со значком ноты
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = scallopedArt,
                            color = Color(0xFF1E1A16),
                            border = BorderStroke(1.8.dp, animatedTrackColor),
                            modifier = Modifier.size(52.dp),
                        ) {
                            Artwork(
                                url = item.thumbnailUrl,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(scallopedArt),
                            )
                        }
                        // Круглый бейдж ноты снизу справа в цвет обложки
                        Surface(
                            shape = CircleShape,
                            color = animatedTrackColor,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = if (animatedTrackColor.luminance() > 0.4f) Color.Black else Color.White,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Название трека и автор
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp,
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = "by — ${item.uploader ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                            ),
                            color = lerp(Color.White.copy(alpha = 0.7f), animatedTrackColor, 0.35f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Кнопки управления в виде фигурных звездочек / лепестков
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val btnBg = lerp(Color(0xFFE2D6BE), animatedTrackColor, 0.12f)
                    val playBg = lerp(Color(0xFFF5EACF), animatedTrackColor, 0.20f)

                    // Prev
                    Surface(
                        shape = scallopedBtn,
                        color = btnBg,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(scallopedBtn)
                            .clickable(onClick = onPrev),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.SkipPrevious,
                                contentDescription = "Назад",
                                tint = Color(0xFF1A1612),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    // Play / Pause (чуть крупнее 50dp)
                    Surface(
                        shape = scallopedBtn,
                        color = playBg,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(scallopedBtn)
                            .clickable(onClick = onTogglePlayPause),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (resolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color(0xFF1A1612),
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Пауза" else "Играть",
                                    tint = Color(0xFF1A1612),
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }

                    // Next
                    Surface(
                        shape = scallopedBtn,
                        color = btnBg,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(scallopedBtn)
                            .clickable(onClick = onNext),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.SkipNext,
                                contentDescription = "Вперед",
                                tint = Color(0xFF1A1612),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Сегментированный выбор скорости/тона: slowed down (0.93) / original (1.0) / speed up (1.15).
 * Долгое нажатие на slowed/speed up → предложить сохранить эту версию.
 */
@Composable
private fun SpeedSelector(
    speed: Float,
    onSetSpeed: (Float) -> Unit,
    onAddVariant: (Float) -> Unit,
    accent: Color,
    white: Color,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        "slowed down" to 0.93f,
        "original" to 1.0f,
        "speed up" to 1.15f,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { (label, value) ->
            val selected = kotlin.math.abs(speed - value) < 0.01f
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) accent else Color.White.copy(alpha = 0.15f),
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .combinedClickable(
                        onClick = { onSetSpeed(value) },
                        onLongClick = { if (value != 1f) onAddVariant(value) },
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) accent else white,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Wavy/Squiggly Progress Bar в стиле Android 13/14/15 Media Player (Material 3 Expressive):
 * Проигранная часть — живая синусоидальная волна с кругляшом-ползунком на конце,
 * оставшаяся часть — прямая полупрозрачная линия. При воспроизведении волна мягко колышется!
 */
@Composable
private fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.25f),
    thumbColor: Color = Color.White,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnim")
    val wavePhase by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wavePhase",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(value) }
    val currentFraction = if (isDragging) dragProgress else value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newF = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(newF)
                    onValueChangeFinished()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val newF = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragProgress = newF
                        onValueChange(newF)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newF = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragProgress = newF
                        onValueChange(newF)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val thumbX = (currentFraction * width).coerceIn(0f, width)

            val wavelength = 38.dp.toPx()
            val amplitude = 4.5.dp.toPx()
            val strokeWidth = 3.5.dp.toPx()
            val thumbRadius = 7.dp.toPx()

            // 1. Волнистая линия пройденной части трека
            if (thumbX > 1f) {
                val wavePath = Path()
                wavePath.moveTo(0f, centerY)
                var x = 0f
                val step = 2f
                while (x <= thumbX) {
                    val damp = if (thumbX - x < wavelength * 0.45f) (thumbX - x) / (wavelength * 0.45f) else 1f
                    val y = centerY + kotlin.math.sin((x / wavelength) * 2 * Math.PI.toFloat() - wavePhase) * amplitude * damp
                    wavePath.lineTo(x, y)
                    x += step
                }
                drawPath(
                    path = wavePath,
                    color = activeColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }

            // 2. Ползунок (Thumb)
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(thumbX, centerY),
            )

            // 3. Прямая линия непроигранной части
            if (thumbX < width - 1f) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(thumbX + thumbRadius, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun FullPlayer(
    item: TrackItem,
    isPlaying: Boolean,
    resolving: Boolean,
    error: String?,
    isLiked: Boolean,
    shuffle: Boolean,
    repeatOne: Boolean,
    positionState: State<Long>,
    durationState: State<Long>,
    audioSessionId: Int,
    speed: Float,
    onSetSpeed: (Float) -> Unit,
    onAddSpeedVariant: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onShowQueue: () -> Unit,
    onFetchLyrics: suspend () -> Lyrics?,
    onOpenArtist: (TrackItem) -> Unit = {},
    onCollapse: () -> Unit,
) {
    val playerContext = androidx.compose.ui.platform.LocalContext.current
    var showVideo by remember(item.url) { mutableStateOf(false) }
    var videoUrl by remember(item.url) { mutableStateOf<String?>(null) }
    var videoLoading by remember(item.url) { mutableStateOf(false) }
    var isFullscreenVideo by remember { mutableStateOf(false) }

    LaunchedEffect(item.url) {
        showVideo = false
        videoUrl = null
        if (item.source == Source.YOUTUBE_MUSIC || NewPipeResolver.isYouTube(item.url)) {
            videoLoading = true
            videoUrl = runCatching { NewPipeResolver.resolveVideo(playerContext, item.url) }.getOrNull()
            videoLoading = false
        }
    }

    BackHandler(onBack = {
        if (isFullscreenVideo) {
            isFullscreenVideo = false
        } else {
            onCollapse()
        }
    })

    // Читаем позицию/длительность только здесь → рекомпозится лишь плеер, не главная.
    val position by positionState
    val duration by durationState

    var dragFraction by remember { mutableStateOf<Float?>(null) }

    var showLyrics by remember(item.url) { mutableStateOf(false) }
    var lyrics by remember(item.url) { mutableStateOf<Lyrics?>(null) }
    var lyricsLoading by remember(item.url) { mutableStateOf(false) }
    LaunchedEffect(showLyrics, item.url) {
        if (showLyrics && lyrics == null && !lyricsLoading) {
            lyricsLoading = true
            lyrics = onFetchLyrics()
            lyricsLoading = false
        }
    }

    val hiRes = remember(item.thumbnailUrl) { upscaleThumb(item.thumbnailUrl, 600) }
    val white = Color.White
    val whiteDim = Color.White.copy(alpha = 0.7f)
    val accent = Color(0xFFFF4D6D)
    val cs = MaterialTheme.colorScheme
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale",
    )
    val heartColor by animateColorAsState(if (isLiked) accent else white, label = "heart")

    // Жесты: вниз → свернуть, вверх → панель скорости, вправо/влево → пред/след трек.
    var dragOffset by remember { mutableStateOf(0f) }
    var showSpeed by remember { mutableStateOf(false) }
    // Накапливаем смещение, чтобы жесты не срабатывали от случайного касания.
    var swipeAccum by remember { mutableStateOf(0f) }
    val swipeThreshold = 90f
    val gestureScope = rememberCoroutineScope()
    val swipeX = remember { Animatable(0f) }
    // 0 = ось ещё не выбрана, 1 = вертикаль, 2 = горизонталь.
    var axis by remember { mutableIntStateOf(0) }

    val dragProgress = (dragOffset / 550f).coerceIn(0f, 1f)
    val playerCornerRadius = (dragProgress * 42f).dp
    val playerScale = 1f - dragProgress * 0.18f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .graphicsLayer {
                scaleX = playerScale
                scaleY = playerScale
                clip = true
                shape = RoundedCornerShape(playerCornerRadius)
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.96f)
            }
            .background(Color(0xFF0E0E12))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { swipeAccum = 0f; axis = 0 },
                    onDragCancel = {
                        swipeAccum = 0f; axis = 0; dragOffset = 0f
                        gestureScope.launch { swipeX.animateTo(0f, tween(180)) }
                    },
                    onDragEnd = {
                        swipeAccum = 0f
                        if (axis == 2) {
                            val v = swipeX.value
                            val w = size.width.toFloat()
                            gestureScope.launch {
                                when {
                                    // Вправо → предыдущий, влево → следующий. Новый трек
                                    // въезжает на место со стороны свайпа.
                                    v > w * 0.22f -> { onPrev(); swipeX.animateTo(0f, tween(260)) }
                                    v < -w * 0.22f -> { onNext(); swipeX.animateTo(0f, tween(260)) }
                                    else -> swipeX.animateTo(0f, tween(200))
                                }
                            }
                        } else {
                            if (dragOffset > 240f) onCollapse() else dragOffset = 0f
                        }
                        axis = 0
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        if (axis == 0) {
                            axis = if (kotlin.math.abs(drag.x) > kotlin.math.abs(drag.y) * 1.3f) 2 else 1
                        }
                        if (axis == 2) {
                            gestureScope.launch { swipeX.snapTo(swipeX.value + drag.x) }
                        } else {
                            val delta = drag.y
                            swipeAccum += delta
                            when {
                                // Уверенный свайп вверх → панель скорости.
                                swipeAccum <= -swipeThreshold && !showSpeed && dragOffset <= 0f -> {
                                    showSpeed = true; swipeAccum = 0f
                                }
                                // Уверенный свайп вниз при открытой панели → прячем её.
                                swipeAccum >= swipeThreshold && showSpeed && dragOffset <= 0f -> {
                                    showSpeed = false; swipeAccum = 0f
                                }
                                // Сворачивание: плеер едет за пальцем только после явного намерения.
                                !showSpeed && (dragOffset > 0f || swipeAccum >= swipeThreshold) ->
                                    dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                            }
                        }
                    },
                )
            },
    ) {
        FlowingBackground(
            thumbnailUrl = hiRes,
            audioSessionId = audioSessionId,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.58f),
                        Color.Black.copy(alpha = 0.90f),
                    ),
                ),
            ),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Опускаем шапку ниже статус-бара.
            Spacer(Modifier.height(40.dp))

            // 1. Верхний бар действий в стиле Material 3 Expressive
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCollapse),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Свернуть",
                            tint = white,
                        )
                    }
                }

                Text(
                    text = if (showLyrics) "Текст песни" else "Сейчас играет",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = white,
                    textAlign = TextAlign.Center,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val vocalCut = com.melo.music.audio.VocalCutManager.isEnabled
                    Surface(
                        shape = CircleShape,
                        color = if (vocalCut) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                        border = if (vocalCut) BorderStroke(1.dp, accent) else null,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { com.melo.music.audio.VocalCutManager.toggle() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (vocalCut) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = if (vocalCut) "Вокал выключен (Караоке)" else "Убрать вокал",
                                tint = if (vocalCut) accent else white,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onShowQueue),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Очередь",
                                tint = white,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (showLyrics) cs.primaryContainer else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { showLyrics = !showLyrics },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Lyrics,
                                contentDescription = "Текст",
                                tint = if (showLyrics) cs.onPrimaryContainer else white,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    if (videoUrl != null || videoLoading) {
                        Surface(
                            shape = CircleShape,
                            color = if (showVideo) cs.primaryContainer else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !videoLoading && videoUrl != null) {
                                    val next = !showVideo
                                    showVideo = next
                                    if (next) {
                                        showLyrics = false
                                        videoUrl?.let { vUrl ->
                                            com.melo.music.playback.PlaybackService.switchToVideo(vUrl)
                                        }
                                    } else {
                                        com.melo.music.playback.PlaybackService.switchToAudio()
                                    }
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (videoLoading) {
                                    CircularProgressIndicator(
                                        color = white,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Videocam,
                                        contentDescription = "Клип",
                                        tint = if (showVideo) cs.onPrimaryContainer else white,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            AnimatedContent(targetState = showLyrics, label = "lyricsToggle") { lyricsMode ->
                if (lyricsMode) {
                    val lyr = lyrics
                    when {
                        lyricsLoading -> Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = white) }

                        lyr == null || lyr.isEmpty -> Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Текст не найден", color = whiteDim, textAlign = TextAlign.Center) }

                        lyr.isSynced -> SyncedLyrics(
                            lines = lyr.lines!!,
                            positionMs = position,
                            onSeek = onSeek,
                            white = white,
                            dim = whiteDim,
                        )

                        else -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp, max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = lyr.plain ?: "",
                                color = white,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset { IntOffset(swipeX.value.roundToInt(), 0) },
                    ) {
                        // 2. Обложка или видеоклип с выразительными скруглениями (32dp) и рамкой
                        if (showVideo && videoUrl != null) {
                            Surface(
                                shape = RoundedCornerShape(32.dp),
                                color = Color.Black,
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.12f)),
                                shadowElevation = 12.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(32.dp)),
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AndroidView(
                                        factory = { ctx ->
                                            androidx.media3.ui.PlayerView(ctx).apply {
                                                useController = false
                                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                player = com.melo.music.playback.PlaybackService.activePlayer
                                            }
                                        },
                                        update = { view ->
                                            if (view.player == null) {
                                                view.player = com.melo.music.playback.PlaybackService.activePlayer
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(12.dp)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { isFullscreenVideo = true },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Rounded.Fullscreen,
                                                contentDescription = "Полный экран",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Crossfade(
                                targetState = hiRes,
                                animationSpec = tween(500),
                                label = "art",
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .aspectRatio(1f),
                            ) { art ->
                                Surface(
                                    shape = RoundedCornerShape(32.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.12f)),
                                    shadowElevation = 12.dp,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(32.dp)),
                                ) {
                                    AsyncImage(
                                        model = art,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // 3. Заголовок трека, исполнитель и кнопка Текста (M3 Expressive)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = 24.sp,
                                        lineHeight = 28.sp,
                                        letterSpacing = (-0.4).sp,
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = white,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                item.uploader?.let { artistName ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = artistName.uppercase(Locale.getDefault()),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp,
                                        ),
                                        color = whiteDim,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable {
                                            val artistItem = TrackItem(
                                                title = artistName,
                                                uploader = artistName,
                                                url = "artist:$artistName",
                                                durationSeconds = 0L,
                                                thumbnailUrl = item.thumbnailUrl,
                                                source = item.source,
                                                kind = ItemKind.ARTIST,
                                            )
                                            onOpenArtist(artistItem)
                                        },
                                    )
                                }
                            }

                            // Круглая кнопка Текста (Lyrics) как на референсе
                            Surface(
                                shape = CircleShape,
                                color = if (showLyrics) cs.primaryContainer else Color.White.copy(alpha = 0.10f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { showLyrics = !showLyrics },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Lyrics,
                                        contentDescription = "Текст песни",
                                        tint = if (showLyrics) cs.onPrimaryContainer else white,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }

                        // Панель скорости / тона
                        AnimatedVisibility(
                            visible = showSpeed,
                            enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(220)),
                            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(140)),
                        ) {
                            SpeedSelector(
                                speed = speed,
                                onSetSpeed = onSetSpeed,
                                onAddVariant = onAddSpeedVariant,
                                accent = accent,
                                white = white,
                                modifier = Modifier.padding(top = 18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 4. Wavy/Squiggly Прогресс-бар в стиле Android 14/15 Media Player
            val fraction = dragFraction
                ?: if (duration > 0) position.toFloat() / duration else 0f

            WavySlider(
                value = fraction.coerceIn(0f, 1f),
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    val f = dragFraction
                    if (f != null && duration > 0) {
                        onSeek((f * duration).toLong())
                    }
                    dragFraction = null
                },
                isPlaying = isPlaying,
                activeColor = white,
                inactiveColor = Color.White.copy(alpha = 0.24f),
                thumbColor = white,
                modifier = Modifier.fillMaxWidth(),
            )

            // Временные метки и плашка качества звука
            val qualityLabel = remember(item.source, item.url) {
                when (item.source) {
                    Source.YOUTUBE_MUSIC -> "44.1 kHz • 256 kbps • Opus"
                    Source.SOUNDCLOUD -> "44.1 kHz • 160 kbps • MP3"
                    Source.BANDCAMP -> "44.1 kHz • 320 kbps • MP3"
                    Source.LOCAL -> "44.1 kHz • Lossless • FLAC"
                    else -> "44.1 kHz • 320 kbps • MP3"
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatMillis(position),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = whiteDim,
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.height(26.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Text(
                            text = qualityLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = whiteDim,
                        )
                    }
                }

                Text(
                    text = if (duration > 0) formatMillis(duration) else "--:--",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = whiteDim,
                )
            }

            Spacer(Modifier.height(22.dp))

            // 5. Главный блок воспроизведения: асимметричный Expressive Shape Trio (Круг - Сквиркл - Круг)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Предыдущий трек: небесно-голубой круг
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF90CEFF),
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPrev),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Назад",
                            tint = Color(0xFF003355),
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }

                // Play / Pause: лавандово-голубой сквиркл M3 Expressive
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFFBAC7FF),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .clickable(onClick = onTogglePlayPause),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (resolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = Color(0xFF003355),
                            )
                        } else {
                            AnimatedContent(targetState = isPlaying, label = "playPause") { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playing) "Пауза" else "Играть",
                                    tint = Color(0xFF003355),
                                    modifier = Modifier.size(42.dp),
                                )
                            }
                        }
                    }
                }

                // Следующий трек: небесно-голубой круг
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF90CEFF),
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onNext),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Вперёд",
                            tint = Color(0xFF003355),
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // 6. Нижний сегментированный остров действий (Pill Island: Shuffle • Repeat • Like)
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.Black.copy(alpha = 0.38f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(60.dp)
                    .clip(RoundedCornerShape(32.dp)),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Перемешать
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Перемешать",
                            tint = if (shuffle) accent else whiteDim,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color.White.copy(alpha = 0.10f)),
                    )

                    // Повтор
                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = if (repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Повтор",
                            tint = if (repeatOne) accent else whiteDim,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color.White.copy(alpha = 0.10f)),
                    )

                    // Лайк / Избранное
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Нравится",
                            tint = heartColor,
                            modifier = Modifier
                                .size(26.dp)
                                .scale(likeScale),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SleepTimerControl(white = white, accent = accent)

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Плеер: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }

    if (isFullscreenVideo && videoUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isFullscreenVideo = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            useController = true
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            player = com.melo.music.playback.PlaybackService.activePlayer
                        }
                    },
                    update = { view ->
                        if (view.player == null) {
                            view.player = com.melo.music.playback.PlaybackService.activePlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 44.dp, start = 20.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { isFullscreenVideo = false },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Состояние плеера для UI (через Player.Listener, без опроса). */
private class PlaybackUi(
    val isPlaying: Boolean,
    val playbackState: Int,
    val position: State<Long>,
    val duration: State<Long>,
    val error: String?,
)

@Composable
private fun rememberPlaybackState(controller: MediaController?): PlaybackUi {
    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    // position/duration — отдельные State: их частые тики НЕ должны рекомпозить
    // главную, только тех, кто реально читает значение (полноэкранный плеер).
    val position = remember { mutableLongStateOf(0L) }
    val duration = remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose { }
        isPlaying = controller.isPlaying
        playbackState = controller.playbackState
        position.longValue = controller.currentPosition.coerceAtLeast(0L)
        duration.longValue = controller.duration.takeIf { it > 0 } ?: 0L
        error = controller.playerError?.message
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                duration.longValue = controller.duration.takeIf { it > 0 } ?: 0L
                position.longValue = controller.currentPosition.coerceAtLeast(0L)
            }
            override fun onPlayerErrorChanged(e: PlaybackException?) {
                error = e?.message
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    // Позицию двигаем только во время игры (для слайдера/текста).
    LaunchedEffect(controller, isPlaying) {
        if (controller != null && isPlaying) {
            while (true) {
                position.longValue = controller.currentPosition.coerceAtLeast(0L)
                if (duration.longValue <= 0) {
                    duration.longValue = controller.duration.takeIf { it > 0 } ?: 0L
                }
                delay(500)
            }
        }
    }

    return PlaybackUi(isPlaying, playbackState, position, duration, error)
}

/** Синхронный текст (LRC): подсветка текущей строки + автопрокрутка. */
@Composable
private fun SyncedLyrics(
    lines: List<LrcLine>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    white: Color,
    dim: Color,
) {
    val activeIndex = remember(positionMs, lines) {
        lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()
    val currentPos by rememberUpdatedState(positionMs)
    // Пока пользователь листает руками — снимаем размытие и не догоняем активную строку.
    var userScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is DragInteraction.Start -> userScrolling = true
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    // 3 секунды без касаний → возвращаем размытие и прокрутку к текущему месту.
                    delay(3000)
                    val idx = lines.indexOfLast { it.timeMs <= currentPos }.coerceAtLeast(0)
                    userScrolling = false
                    runCatching { listState.animateScrollToItem(idx) }
                }
            }
        }
    }
    // Авто-слежение за активной строкой — только когда пользователь не листает.
    LaunchedEffect(activeIndex) {
        if (!userScrolling) runCatching { listState.animateScrollToItem(activeIndex) }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 130.dp),
    ) {
        val karaoke = com.melo.music.settings.AppSettings.karaoke
        itemsIndexed(lines) { i, line ->
            val active = i == activeIndex
            // Чем дальше строка от активной — тем сильнее размытие (активная чёткая).
            // При ручном листании размытие убираем полностью.
            val distance = kotlin.math.abs(i - activeIndex)
            val blur = if (userScrolling) 0.dp else when (distance) {
                0 -> 0.dp
                1 -> 2.dp
                2 -> 4.dp
                3 -> 6.dp
                else -> 8.dp
            }
            // Караоке: активная строка тускло-серая, слова загораются по времени.
            val content = if (active && karaoke && line.text.isNotBlank()) {
                val nextMs = lines.getOrNull(i + 1)?.timeMs ?: (line.timeMs + 4000L)
                karaokeLine(line.text, line.timeMs, nextMs, positionMs, white, white.copy(alpha = 0.32f))
            } else {
                androidx.compose.ui.text.AnnotatedString(line.text.ifBlank { "♪" })
            }
            Text(
                text = content,
                color = if (active) white else dim.copy(alpha = 0.45f),
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                style = if (active) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .blur(blur)
                    .clickable { onSeek(line.timeMs) }
                    .padding(vertical = 7.dp, horizontal = 8.dp),
            )
        }
    }
}

/**
 * Караоке-строка: время строки (до следующей) делим на число слов, слова до текущего
 * момента — светлые (lit), остальные — приглушённые (unlit).
 */
private fun karaokeLine(
    text: String,
    startMs: Long,
    nextMs: Long,
    posMs: Long,
    lit: Color,
    unlit: Color,
): androidx.compose.ui.text.AnnotatedString {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return androidx.compose.ui.text.AnnotatedString(text)

    val totalChars = words.sumOf { it.length }.coerceAtLeast(1)
    val lineDur = (nextMs - startMs).coerceIn(800L, 9000L)
    // Вокальная фразировка: пение занимает основную часть строки (~80%), оставляя паузу на вдох
    val singingDur = (lineDur * 0.82f).toLong().coerceAtLeast(600L)
    val elapsed = (posMs - startMs).coerceAtLeast(0L)
    val rawProgress = (elapsed.toFloat() / singingDur).coerceIn(0f, 1f)

    // Плавная вокальная S-кривая без резких механических скачков
    val easedProgress = rawProgress * rawProgress * (3f - 2f * rawProgress)
    val litChars = (easedProgress * totalChars).toInt()

    var charIndex = 0
    return androidx.compose.ui.text.buildAnnotatedString {
        words.forEachIndexed { idx, w ->
            val wordStart = charIndex
            val wordEnd = charIndex + w.length
            charIndex = wordEnd

            val wordColor = when {
                litChars >= wordEnd -> lit
                litChars <= wordStart -> unlit
                else -> {
                    val wordFrac = ((litChars - wordStart).toFloat() / w.length).coerceIn(0f, 1f)
                    androidx.compose.ui.graphics.lerp(unlit, lit, wordFrac)
                }
            }
            withStyle(androidx.compose.ui.text.SpanStyle(color = wordColor)) {
                append(w)
            }
            if (idx < words.lastIndex) append(" ")
        }
    }
}

/** Повышает запрашиваемый размер обложки googleusercontent (=wNNN-hNNN). */
private fun upscaleThumb(url: String?, size: Int): String? {
    if (url == null) return null
    val cut = url.indexOf('=')
    return if (cut > 0) url.substring(0, cut) + "=w$size-h$size-l90-rj" else url
}

private fun formatMillis(ms: Long): String = formatDuration(ms / 1000).ifBlank { "0:00" }

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}
