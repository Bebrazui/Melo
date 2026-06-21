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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
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
    onLoadAlbumTracks: suspend (String) -> List<TrackItem>,
    onLoadShelf: suspend (String) -> List<TrackItem>,
    onRelatedTracks: suspend (TrackItem) -> List<TrackItem> = { emptyList() },
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
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var artistOpen by rememberSaveable(stateSaver = TrackSaver.singleSaver()) { mutableStateOf<TrackItem?>(null) }
    var playlistOpen by remember { mutableStateOf<Playlist?>(null) }
    var searchMode by rememberSaveable { mutableStateOf(false) }
    var homeSettings by remember { mutableStateOf(false) }
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

    val controller = playerProvider()
    val playback = rememberPlaybackState(controller)
    val isPlaying = playback.isPlaying
    val playerError = playback.error

    // Скорость + тон воспроизведения (slowed / original / sped up). pitch = speed → меняется тон.
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    fun setSpeed(value: Float) {
        speed = value
        playerProvider()?.playbackParameters = PlaybackParameters(value, value)
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
        android.util.Log.e("MeloPerf", "PLAYER ERROR → re-resolve ${item.url}")
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
        runCatching { onLoadRecommendations() }
            .onSuccess { result ->
                // distinctBy: YouTube иногда отдаёт один трек дважды → дубль ключа в LazyColumn.
                items = result.distinctBy { it.url }
                listError = null
                // Фоновая предзагрузка видимых рекомендаций (очередь, по одному):
                // к моменту тапа трек уже в кэше → мгновенно.
                items.take(8).forEach { onPrefetch(it.url) }
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
        scope.launch {
            listTitle = "Результаты: $q"
            listLoading = true
            listError = null
            items = emptyList()
            // Потоковая выдача: YouTube прилетает первым, SoundCloud дописывается позже.
            onSearch(q)
                .catch { listError = it.message }
                .collect { partial ->
                    items = partial.distinctBy { it.url }
                    // Агрессивный prefetch: все треки из частичной выдачи (YT + SC + Bandcamp)
                    partial.filter { it.kind == ItemKind.TRACK }
                        .forEach { onPrefetch(it.url) }
                }
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
        val nextItem = list.getOrNull(nextIdx) ?: return
        scope.launch {
            val resolved = runCatching { onResolveAudioUrl(nextItem.url) }.getOrNull()
            // Проверяем, что очередь/индекс не сменились, пока резолвили.
            if (resolved != null && playingIndex == fromIndex) {
                com.melo.music.playback.PlaybackService.setNext(
                    resolved.audioUrl, nextItem.title, nextIdx,
                )
            }
        }
    }

    fun playAt(list: List<TrackItem>, index: Int, keepSea: Boolean = false) {
        if (index !in list.indices) return
        // Любой ручной выбор трека выходит из волны (волна сама зовёт с keepSea=true).
        if (!keepSea) seaActive = false
        // Тот же трек уже играет — это пауза/продолжить, а не перезапуск.
        if (list[index].url == nowPlaying?.url) {
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
                    android.util.Log.e(
                        "MeloPerf",
                        "TAP→resolved ${android.os.SystemClock.elapsedRealtime() - tStart}ms",
                    )
                    // Если пользователь уже переключился — не играть старый трек.
                    if (resolvingUrl == item.url) {
                        onPlayResolved(it)
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

    // Назад: закрыть плеер → закрыть артиста → выйти.
    BackHandler(enabled = playerExpanded || artistOpen != null) {
        when {
            playerExpanded -> playerExpanded = false
            artistOpen != null -> artistOpen = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                NowPlayingBar(
                    item = nowPlaying,
                    isPlaying = isPlaying,
                    resolving = resolvingUrl != null,
                    error = playerError,
                    onTogglePlayPause = onTogglePlayPause,
                    onClick = { playerExpanded = true },
                )
                MeloBottomNav(selected = selectedTab, onSelect = { selectedTab = it })
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
            },
            label = "tabs",
          ) { tab ->
           Column(modifier = Modifier.fillMaxSize()) {
            when (tab) {
                MeloTab.Home -> {
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
                    )
                    if (searchMode) {
                        when {
                            listLoading && items.isEmpty() -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }

                            listError != null && items.isEmpty() -> Text(
                                text = "Ошибка: $listError",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(20.dp),
                            )

                            else -> LazyColumn(
                                state = searchListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                itemsIndexed(items, key = { _, it -> it.url }) { _, item ->
                                    if (item.kind == ItemKind.ARTIST) {
                                        ArtistCard(item = item, onClick = { artistOpen = item })
                                    } else {
                                        TrackCard(
                                            item = item,
                                            resolving = resolvingUrl == item.url,
                                            playing = nowPlaying?.url == item.url && isPlaying,
                                            onClick = {
                                                val tracks = items.filter { it.kind == ItemKind.TRACK }
                                                playAt(tracks, tracks.indexOf(item))
                                            },
                                            onLongClick = { contextMenuTrack = item },
                                        )
                                    }
                                }
                                if (listLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    }
                                }
                            }
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
                            onSettings = { homeSettings = true },
                            onPrefetch = onPrefetch,
                            onStartSea = { startSea(null) },
                            seaLoading = seaLoading,
                            onRelatedTracks = onRelatedTracks,
                        )
                    }
                }

                MeloTab.Favorite -> {
                    val likedList = remember(likedVersion) { getLiked() }
                    // Прогреваем первые треки → первый тап мгновенный даже после перезапуска.
                    LaunchedEffect(likedVersion) {
                        likedList.take(6).forEach { onPrefetch(it.url) }
                    }
                    if (likedList.isEmpty()) {
                        Placeholder(
                            icon = Icons.Rounded.Favorite,
                            title = "Избранное",
                            subtitle = "Лайкнутые треки появятся здесь",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 12.dp),
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

                MeloTab.Account -> AccountTab(
                    onOpenPlaylist = { playlistOpen = it },
                    onOpenSettings = { homeSettings = true },
                )
            }
           }
          }
        }
    }

        val current = nowPlaying
        AnimatedVisibility(
            visible = playerExpanded && current != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
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
                    onCollapse = { playerExpanded = false },
                )
            }
        }

        AnimatedVisibility(
            visible = artistOpen != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            artistOpen?.let { artist ->
                ArtistScreen(
                    artist = artist,
                    onLoadTracks = { onLoadArtistTracks(artist) },
                    onLoadAlbums = { onLoadArtistAlbums(artist) },
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
                    onClose = { artistOpen = null },
                )
            }
        }

        AnimatedVisibility(
            visible = playlistOpen != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            playlistOpen?.let { pl ->
                PlaylistScreen(
                    playlist = pl,
                    nowPlayingUrl = nowPlaying?.url,
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

    if (homeSettings) {
        SettingsScreen(
            scGetId = scGetId,
            onScSetManual = onScSetManual,
            onScRefresh = onScRefresh,
            onBack = { homeSettings = false },
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
    Account("Аккаунт"),
}

@Composable
private fun MeloBottomNav(selected: MeloTab, onSelect: (MeloTab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == MeloTab.Home,
            onClick = { onSelect(MeloTab.Home) },
            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
            label = { Text(MeloTab.Home.label) },
        )
        NavigationBarItem(
            selected = selected == MeloTab.Favorite,
            onClick = { onSelect(MeloTab.Favorite) },
            icon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
            label = { Text(MeloTab.Favorite.label) },
        )
        NavigationBarItem(
            selected = selected == MeloTab.Account,
            onClick = { onSelect(MeloTab.Account) },
            icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null) },
            label = { Text(MeloTab.Account.label) },
        )
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
) {
    var playlists by remember { mutableStateOf(PlaylistManager.getAll().toList()) }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // Шапка: заголовок + шестерёнка настроек справа.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Аккаунт",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Настройки")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Мои плейлисты",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                onClick = { showCreate = true },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Создать")
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
                            onLongClick = { deleteTarget = pl },
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

/** Стилизованный диалог в духе приложения (скруглённый, с тенью). */
@Composable
private fun MeloDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp), content = content)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
            placeholder = { Text("Поиск трека или исполнителя") },
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

// ── Главная: лента с полками ──────────────────────────────────────────────────

/** Карта запуска бесконечной персональной волны «Sea». */
@Composable
private fun SeaCard(loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF7B4D6B), Color(0xFF422A3D))),
            )
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sea",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Бесконечная волна под твой вкус",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White,
                    )
                } else {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Запустить волну",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
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
    onSettings: () -> Unit,
    onPrefetch: (String) -> Unit,
    onStartSea: () -> Unit,
    seaLoading: Boolean,
    onRelatedTracks: suspend (TrackItem) -> List<TrackItem>,
) {
    val moods = remember {
        listOf(
            "Заряд энергии" to "энергичная музыка",
            "Спокойное" to "lofi chill спокойная музыка",
            "В дороге" to "музыка в дорогу",
            "Хип-хоп" to "русский рэп",
            "Рок" to "рок музыка",
            "Поп" to "поп музыка",
        )
    }
    val recTracks = recommendations.filter { it.kind == ItemKind.TRACK }.distinctBy { it.url }

    // Персонализированные полки из Taste Profile.
    var personalizedShelves by remember {
        mutableStateOf<List<Pair<String, List<TrackItem>>>>(emptyList())
    }
    LaunchedEffect(Unit) {
        personalizedShelves = Recommender.generatePersonalizedShelves(onLoadShelf, onRelatedTracks)
            .ifEmpty {
                listOf(
                    "Новинки" to emptyList(),
                    "Русский рэп" to emptyList(),
                )
            }
    }

    // Кэш полок живёт здесь (LazyColumn не уничтожается при скролле),
    // поэтому возврат к полке не перезапрашивает её.
    val shelfCache = remember { mutableStateMapOf<String, List<TrackItem>>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item { Greeting(onSettings) }
        item { SeaCard(loading = seaLoading, onClick = onStartSea) }
        item { MoodChips(moods, onMood) }

        if (history.isNotEmpty()) {
            item { SectionTitle("Недавно слушали") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(history.take(20), key = { "hist_" + it.url }) { t ->
                        ShelfCard(
                            item = t,
                            playing = nowPlayingUrl == t.url && isPlaying,
                            resolving = resolvingUrl == t.url,
                            onClick = { onPlay(history, history.indexOf(t)) },
                            onLongClick = { onTrackLongClick(t) },
                        )
                    }
                }
            }
        }

        item { SectionTitle("Быстрый выбор") }
        item {
            QuickPickGrid(
                recTracks, loading, nowPlayingUrl, isPlaying, resolvingUrl,
                onPlay, onTrackLongClick,
            )
        }

        // Персонализированные полки.
        if (personalizedShelves.isNotEmpty()) {
            personalizedShelves.forEachIndexed { idx, (title, tracks) ->
                item { SectionTitle(title) }
                if (tracks.isEmpty()) {
                    // Если данные ещё не загружены — показываем горизонтальную заглушку.
                    item {
                        HorizontalShelf(
                            title.lowercase(), shelfCache, onLoadShelf, nowPlayingUrl, isPlaying,
                            resolvingUrl, onPlay, onTrackLongClick, onPrefetch,
                        )
                    }
                } else {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(tracks, key = { _, t -> "pers_${idx}_${t.url}" }) { index, t ->
                                ShelfCard(
                                    item = t,
                                    playing = nowPlayingUrl == t.url && isPlaying,
                                    resolving = resolvingUrl == t.url,
                                    onClick = { onPlay(tracks, index) },
                                    onLongClick = { onTrackLongClick(t) },
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Fallback: стандартные полки.
            item { SectionTitle("Новинки") }
            item {
                HorizontalShelf(
                    "новинки музыки 2026", shelfCache, onLoadShelf, nowPlayingUrl, isPlaying,
                    resolvingUrl, onPlay, onTrackLongClick, onPrefetch,
                )
            }

            item { SectionTitle("Русский рэп") }
            item {
                HorizontalShelf(
                    "русский рэп хиты", shelfCache, onLoadShelf, nowPlayingUrl, isPlaying,
                    resolvingUrl, onPlay, onTrackLongClick, onPrefetch,
                )
            }

            item { SectionTitle("Под настроение") }
            item {
                HorizontalShelf(
                    "lofi chill", shelfCache, onLoadShelf, nowPlayingUrl, isPlaying,
                    resolvingUrl, onPlay, onTrackLongClick, onPrefetch,
                )
            }
        }

        item { SectionTitle("Рекомендуем") }
        if (loading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
        itemsIndexed(recTracks, key = { _, it -> "rec_" + it.url }) { index, t ->
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
private fun Greeting(onSettings: () -> Unit) {
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
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(greet, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Что послушаем?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Настройки")
        }
    }
}

@Composable
private fun MoodChips(moods: List<Pair<String, String>>, onMood: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(moods) { (label, seed) ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { onMood(seed) },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
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
        modifier = Modifier.fillMaxWidth().height(148.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(grid, key = { "qp_" + it.url }) { t ->
            Row(
                modifier = Modifier
                    .width(264.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { onPlay(grid, grid.indexOf(t)) },
                        onLongClick = { onLongClick(t) },
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(
                    url = t.thumbnailUrl,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        t.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
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

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tracks, key = { it.url }) { t ->
            ShelfCard(
                item = t,
                playing = nowPlayingUrl == t.url && isPlaying,
                resolving = resolvingUrl == t.url,
                onClick = { onPlay(tracks, tracks.indexOf(t)) },
                onLongClick = { onLongClick(t) },
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
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier = Modifier.size(150.dp).clip(RoundedCornerShape(16.dp)),
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
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
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
            .combinedClickable(
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
                    .clip(if (item.kind == ItemKind.ARTIST) CircleShape else RoundedCornerShape(16.dp)),
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
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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

@Composable
private fun ArtistScreen(
    artist: TrackItem,
    onLoadTracks: suspend () -> List<TrackItem>,
    onLoadAlbums: suspend () -> List<TrackItem>,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    audioSessionId: Int,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onShuffle: (List<TrackItem>) -> Unit,
    onLoadAlbumTracks: suspend (String) -> List<TrackItem>,
    onTrackLongClick: (TrackItem) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var tracks by remember(artist.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var albums by remember(artist.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
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

    val hiRes = remember(artist.thumbnailUrl) { upscaleThumb(artist.thumbnailUrl, 600) }
    val white = Color.White

    // Палитра из фото исполнителя → акцент и подложка карточек.
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
    // Подложка карточек/строк — затемнённый акцент (читается на тёмном фоне).
    val cardTint = lerp(accent, Color(0xFF111114), 0.72f)
    val darkAccent = lerp(accent, Color.Black, 0.82f)
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White

    val pageBg = Color(0xFF000000)
    Box(modifier = Modifier.fillMaxSize().background(pageBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                    // Большое фото исполнителя на всю ширину.
                    Artwork(
                        url = hiRes ?: artist.thumbnailUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.35f),
                                0.45f to Color.Transparent,
                                0.8f to pageBg.copy(alpha = 0.85f),
                                1f to pageBg,
                            ),
                        ),
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 10.dp, start = 10.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад", tint = white)
                    }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SourceBadge(artist.source, Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Исполнитель · ${sourceLabel(artist.source)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = white.copy(alpha = 0.85f),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = artist.title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = white,
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { if (tracks.isNotEmpty()) onPlay(tracks, 0) },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = onAccent,
                                ),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Слушать")
                            }
                            FilledTonalIconButton(
                                onClick = { if (tracks.isNotEmpty()) onShuffle(tracks) },
                                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = accent.copy(alpha = 0.35f),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = "Перемешать")
                            }
                        }
                    }
                }
            }

            if (albums.isNotEmpty()) {
                item {
                    Text(
                        "Альбомы",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = white,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
                    )
                }
                items(albums.distinctBy { it.url }, key = { it.url }) { al ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AlbumCard(
                            album = al,
                            accent = accent,
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
                if (tracks.isNotEmpty()) {
                    item {
                        Text(
                            "Треки",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = white,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                        )
                    }
                }
            }

            when {
                loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = white)
                    }
                }
                error != null && tracks.isEmpty() -> item {
                    Text(
                        text = "Не удалось загрузить треки: $error",
                        color = white.copy(alpha = 0.8f),
                        modifier = Modifier.padding(24.dp),
                    )
                }
                tracks.isEmpty() -> item {
                    Text(
                        text = "Треки не найдены",
                        color = white.copy(alpha = 0.8f),
                        modifier = Modifier.padding(24.dp),
                    )
                }
                else -> itemsIndexed(tracks, key = { i, it -> "$i:${it.url}" }) { index, t ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TrackCard(
                            item = t,
                            resolving = resolvingUrl == t.url,
                            playing = nowPlayingUrl == t.url && isPlaying,
                            onClick = { onPlay(tracks, index) },
                            onLongClick = { onTrackLongClick(t) },
                            containerColor = cardTint,
                        )
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
        shape = RoundedCornerShape(20.dp),
        color = cardTint,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded; if (expanded) load() }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(album.thumbnailUrl, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        album.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = white,
                        maxLines = 1,
                    )
                    Text(
                        if (tracks.isNotEmpty()) "Альбом · ${tracks.size} треков" else "Альбом",
                        style = MaterialTheme.typography.bodySmall,
                        color = white.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
                FilledIconButton(
                    onClick = { load { if (it.isNotEmpty()) onPlay(it, 0) } },
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = accent,
                        contentColor = onAccent,
                    ),
                ) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Играть альбом") }
                IconButton(onClick = { expanded = !expanded; if (expanded) load() }) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = white,
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
                            CircularProgressIndicator(
                                color = white, modifier = Modifier.size(24.dp), strokeWidth = 2.dp,
                            )
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
                                Artwork(t.thumbnailUrl, Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        t.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = white,
                                        maxLines = 1,
                                        fontWeight = if (nowPlayingUrl == t.url) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    t.uploader?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = white.copy(alpha = 0.7f),
                                            maxLines = 1,
                                        )
                                    }
                                }
                                if (resolvingUrl == t.url) {
                                    CircularProgressIndicator(
                                        color = white, modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                                    )
                                } else if (nowPlayingUrl == t.url && isPlaying) {
                                    Icon(
                                        Icons.Rounded.MusicNote, contentDescription = null,
                                        tint = white, modifier = Modifier.size(18.dp),
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
private fun PlaylistScreen(
    playlist: Playlist,
    nowPlayingUrl: String?,
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
            modifier = Modifier.fillMaxSize(),
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
                itemsIndexed(tracks, key = { i, it -> "$i:${it.url}" }) { index, t ->
                    TrackCard(
                        item = t,
                        resolving = resolvingUrl == t.url,
                        playing = nowPlayingUrl == t.url && isPlaying,
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
            }
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
    }
}

@Composable
private fun Artwork(url: String?, modifier: Modifier = Modifier) {
    if (url != null) {
        AsyncImage(
            model = url,
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

@Composable
private fun NowPlayingBar(
    item: TrackItem?,
    isPlaying: Boolean,
    resolving: Boolean,
    error: String?,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
) {
    if (item == null) return
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clickable(onClick = onClick),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(
                    url = item.thumbnailUrl,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                    )
                    item.uploader?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onTogglePlayPause) {
                    if (resolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Пауза" else "Играть",
                        )
                    }
                }
            }
            error?.let {
                Text(
                    text = "Плеер: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected) 1.6.dp else 1.dp,
                        color = if (selected) accent else white.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .background(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent)
                    .combinedClickable(
                        onClick = { onSetSpeed(value) },
                        onLongClick = { if (value != 1f) onAddVariant(value) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) accent else white,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
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
    onCollapse: () -> Unit,
) {
    BackHandler(onBack = onCollapse)

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
    val whiteDim = Color.White.copy(alpha = 0.75f)
    val accent = Color(0xFFFF4D6D)
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1f,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.roundToInt()) }
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
                            if (dragOffset > 320f) onCollapse() else dragOffset = 0f
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
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.88f),
                    ),
                ),
            ),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Опускаем шапку ниже статус-бара.
            Spacer(Modifier.height(40.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Свернуть", tint = white)
                }
                Text(
                    text = if (showLyrics) "Текст песни" else "Сейчас играет",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = white,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onShowQueue) {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Очередь",
                        tint = white,
                    )
                }
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = "Текст",
                        tint = if (showLyrics) accent else white,
                    )
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
                        Crossfade(
                            targetState = hiRes,
                            animationSpec = tween(500),
                            label = "art",
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .aspectRatio(1f),
                        ) { art ->
                            AsyncImage(
                                model = art,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color.White.copy(alpha = 0.06f)),
                            )
                        }
                        Spacer(Modifier.height(28.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = white,
                                    maxLines = 2,
                                )
                                item.uploader?.let {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = whiteDim,
                                        maxLines = 1,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SourceBadge(item.source, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = sourceLabel(item.source),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = whiteDim,
                                    )
                                }
                            }
                            IconButton(onClick = onToggleLike) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Нравится",
                                    tint = heartColor,
                                    modifier = Modifier.size(30.dp).scale(likeScale),
                                )
                            }
                        }

                        // Кнопки скорости/тона выезжают вверх под обложкой (по свайпу вверх).
                        // expandVertically анимирует высоту → соседние элементы съезжают плавно.
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

            Spacer(Modifier.height(28.dp))
            val fraction = dragFraction
                ?: if (duration > 0) position.toFloat() / duration else 0f
            Slider(
                value = fraction.coerceIn(0f, 1f),
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    val f = dragFraction
                    if (f != null && duration > 0) {
                        onSeek((f * duration).toLong())
                    }
                    dragFraction = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = white,
                    activeTrackColor = white,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatMillis(position), style = MaterialTheme.typography.bodySmall, color = whiteDim)
                Text(
                    text = if (duration > 0) formatMillis(duration) else "--:--",
                    style = MaterialTheme.typography.bodySmall,
                    color = whiteDim,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Перемешать",
                        tint = if (shuffle) accent else white,
                    )
                }
                IconButton(onClick = onPrev, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Назад",
                        tint = white,
                        modifier = Modifier.size(40.dp),
                    )
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = white,
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.size(78.dp),
                ) {
                    if (resolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 3.dp,
                            color = Color.Black,
                        )
                    } else {
                        AnimatedContent(targetState = isPlaying, label = "playPause") { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Пауза" else "Играть",
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Вперёд",
                        tint = white,
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = "Повтор",
                        tint = if (repeatOne) accent else white,
                    )
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
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
            Text(
                text = line.text.ifBlank { "♪" },
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
