package com.melo.music.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import com.melo.music.audio.EqualizerManager
import com.melo.music.extractor.ItemKind
import com.melo.music.history.HistoryManager
import com.melo.music.lyrics.LrcLine
import com.melo.music.lyrics.Lyrics
import com.melo.music.playlists.Playlist
import com.melo.music.playlists.PlaylistManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import com.melo.music.extractor.ResolvedTrack
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager
import com.melo.music.recommend.Recommender
import com.melo.music.recommend.SkipTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    onLoadArtistTracks: suspend (String) -> List<TrackItem>,
    onLoadShelf: suspend (String) -> List<TrackItem>,
    scGetId: () -> String?,
    onScSetManual: suspend (String) -> Boolean,
    onScRefresh: suspend () -> String?,
    onResolveAudioUrl: suspend (String) -> ResolvedTrack,
    isCached: (String) -> Boolean,
    onPrefetch: (String) -> Unit,
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
    // Избранное — сохраняется в SharedPreferences.
    var likedVersion by remember { mutableIntStateOf(0) }
    fun getLiked(): MutableList<TrackItem> = FavoritesManager.getAll()
    fun isLiked(item: TrackItem): Boolean = FavoritesManager.isLiked(item.url)
    fun toggleLike(item: TrackItem) {
        FavoritesManager.toggle(item)
        likedVersion++
    }

    var contextMenuTrack by remember { mutableStateOf<TrackItem?>(null) }

    val controller = playerProvider()
    val playback = rememberPlaybackState(controller)
    val isPlaying = playback.isPlaying
    val playerError = playback.error

    LaunchedEffect(Unit) {
        listLoading = true
        runCatching { onLoadRecommendations() }
            .onSuccess { result ->
                items = result
                listError = null
                // Фоновая предзагрузка видимых рекомендаций (очередь, по одному):
                // к моменту тапа трек уже в кэше → мгновенно.
                result.take(8).forEach { onPrefetch(it.url) }
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
                    items = partial
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

    fun playAt(list: List<TrackItem>, index: Int) {
        if (index !in list.indices) return
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
        playAt(playingList, next)
    }
    fun playPrev() {
        if (playingList.isEmpty()) return
        val prev = if (playingIndex <= 0) playingList.lastIndex else playingIndex - 1
        playAt(playingList, prev)
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
    }
    DisposableEffect(Unit) {
        onDispose { com.melo.music.playback.PlaybackService.onTrackEnded = null }
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
            when (selectedTab) {
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
                        )
                    }
                }

                MeloTab.Favorite -> {
                    val likedList = getLiked()
                    if (likedList.isEmpty()) {
                        Placeholder(
                            icon = Icons.Filled.Favorite,
                            title = "Избранное",
                            subtitle = "Лайкнутые треки появятся здесь",
                        )
                    } else {
                        Text(
                            text = "Избранное",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(likedList, key = { _, it -> it.url }) { index, item ->
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

                MeloTab.Account -> AccountTab(
                    scGetId = scGetId,
                    onScSetManual = onScSetManual,
                    onScRefresh = onScRefresh,
                    onOpenPlaylist = { playlistOpen = it },
                )
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
                FullPlayer(
                    item = item,
                    isPlaying = isPlaying,
                    resolving = resolvingUrl == item.url,
                    error = playerError,
                    isLiked = isLiked(item),
                    shuffle = shuffle,
                    repeatOne = repeatOne,
                    positionState = playback.position,
                    durationState = playback.duration,
                    audioSessionId = audioSessionIdProvider(),
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
                    onLoadTracks = { onLoadArtistTracks(artist.url) },
                    nowPlayingUrl = nowPlaying?.url,
                    isPlaying = isPlaying,
                    resolvingUrl = resolvingUrl,
                    audioSessionId = audioSessionIdProvider(),
                    onPlay = { tracks, index -> playAt(tracks, index) },
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

    if (homeSettings) {
        SettingsSheet(
            scGetId = scGetId,
            onScSetManual = onScSetManual,
            onScRefresh = onScRefresh,
            onDismiss = { homeSettings = false },
        )
    }

    if (showQueue) {
        QueueSheet(
            tracks = playingList,
            currentIndex = playingIndex,
            nowPlayingUrl = nowPlaying?.url,
            isPlaying = isPlaying,
            onJump = { i -> playAt(playingList, i) },
            onDismiss = { showQueue = false },
        )
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
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
            Text("Очередь", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    itemsIndexed(tracks, key = { _, it -> "q_" + it.url }) { i, t ->
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
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
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
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(MeloTab.Home.label) },
        )
        NavigationBarItem(
            selected = selected == MeloTab.Favorite,
            onClick = { onSelect(MeloTab.Favorite) },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label = { Text(MeloTab.Favorite.label) },
        )
        NavigationBarItem(
            selected = selected == MeloTab.Account,
            onClick = { onSelect(MeloTab.Account) },
            icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
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
    scGetId: () -> String?,
    onScSetManual: suspend (String) -> Boolean,
    onScRefresh: suspend () -> String?,
    onOpenPlaylist: (Playlist) -> Unit,
) {
    var playlists by remember { mutableStateOf(PlaylistManager.getAll().toList()) }
    var showSettings by remember { mutableStateOf(false) }
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
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "Настройки")
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
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
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
                    Icons.Filled.LibraryMusic,
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
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
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

    if (showSettings) {
        SettingsSheet(
            scGetId = scGetId,
            onScSetManual = onScSetManual,
            onScRefresh = onScRefresh,
            onDismiss = { showSettings = false },
        )
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
                    Icons.Filled.LibraryMusic,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    scGetId: () -> String?,
    onScSetManual: suspend (String) -> Boolean,
    onScRefresh: suspend () -> String?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var currentId by remember { mutableStateOf(scGetId()) }
    var manual by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(Source.SOUNDCLOUD, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("SoundCloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = currentId?.let { "client_id: ✓ сохранён (${it.take(6)}…)" }
                    ?: "client_id: ✗ нет",
                style = MaterialTheme.typography.bodyMedium,
                color = if (currentId != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    if (busy) return@OutlinedButton
                    scope.launch {
                        busy = true; message = "Пробую добыть…"
                        val r = onScRefresh(); currentId = r
                        message = if (r != null) "Готово ✓" else "Не вышло — вставь вручную"
                        busy = false
                    }
                },
                enabled = !busy,
            ) { Text("Обновить автоматически") }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = manual,
                onValueChange = { manual = it },
                label = { Text("client_id вручную") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val id = manual.trim()
                    if (busy || id.isEmpty()) return@Button
                    scope.launch {
                        busy = true; message = "Проверяю…"
                        val ok = onScSetManual(id)
                        if (ok) { currentId = id; manual = ""; message = "Сохранён ✓" }
                        else message = "Неверный client_id"
                        busy = false
                    }
                },
                enabled = !busy,
            ) { Text("Сохранить") }

            if (busy) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            EqualizerSection()
        }
    }
}

@Composable
private fun EqualizerSection() {
    var enabled by remember { mutableStateOf(EqualizerManager.isEnabled()) }
    var selectedPreset by remember { mutableIntStateOf(EqualizerManager.getPreset()) }
    val bandCount = EqualizerManager.bandCount
    val bandRange = EqualizerManager.bandLevelRange
    val frequencies = EqualizerManager.bandFrequencies
    val presets = EqualizerManager.presetNames

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Эквалайзер",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    EqualizerManager.setEnabled(it)
                },
            )
        }

        if (enabled && bandCount > 0) {
            Spacer(Modifier.height(12.dp))

            // Пресеты
            Text(
                "Пресет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            var presetExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { presetExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (selectedPreset in 0 until presets.size) presets[selectedPreset]
                        else "Ручная настройка",
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = presetExpanded,
                    onDismissRequest = { presetExpanded = false },
                ) {
                    presets.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedPreset = index
                                EqualizerManager.setPreset(index)
                                presetExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Полосы эквалайзера
            Text(
                "Полосы",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            val bandLabels = remember(bandCount) {
                frequencies.map { freq ->
                    when {
                        freq < 1000 -> "${freq}Гц"
                        else -> "${freq / 1000}кГц"
                    }
                }
            }

            // Полосы эквалайзера — горизонтальные слайдеры.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                for (i in 0 until bandCount) {
                    var level by remember {
                        mutableStateOf(EqualizerManager.getBandLevel(i))
                    }
                    val dbValue = (level / 100f)
                    val barColor = when {
                        dbValue > 0 -> MaterialTheme.colorScheme.primary
                        dbValue < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                bandLabels[i],
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (dbValue > 0) "+${dbValue.toInt()} дБ"
                                else if (dbValue < 0) "${dbValue.toInt()} дБ" else "0 дБ",
                                style = MaterialTheme.typography.labelMedium,
                                color = barColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { level = it.toInt().toShort() },
                            onValueChangeFinished = {
                                selectedPreset = -1
                                EqualizerManager.setBandLevel(i, level)
                            },
                            valueRange = bandRange[0].toFloat()..bandRange[1].toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = barColor,
                                activeTrackColor = barColor,
                                inactiveTrackColor = barColor.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
            }
        } else if (enabled) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Эквалайзер недоступен на этом устройстве",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
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

    // Анимация переключения: blur + затемнение.
    var prevGhost by remember { mutableStateOf("") }
    var animPhase by remember { mutableFloatStateOf(0f) }
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
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = "Очистить")
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
    val recTracks = recommendations.filter { it.kind == ItemKind.TRACK }

    // Персонализированные полки из Taste Profile.
    var personalizedShelves by remember {
        mutableStateOf<List<Pair<String, List<TrackItem>>>>(emptyList())
    }
    LaunchedEffect(Unit) {
        personalizedShelves = Recommender.generatePersonalizedShelves(onLoadShelf)
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
            Icon(Icons.Filled.Settings, contentDescription = "Настройки")
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
    val grid = tracks.take(12)
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
            shelfCache[seed] = result
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
                        Icons.Filled.MusicNote,
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
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
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
                    maxLines = 2,
                )
                val subtitle = listOfNotNull(
                    item.uploader,
                    formatDuration(item.durationSeconds),
                ).joinToString(" · ")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(item.source, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = subtitle.ifBlank { sourceLabel(item.source) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                item.kind == ItemKind.ARTIST -> Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Песни исполнителя",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp),
                )
                resolving -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
                else -> Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    audioSessionId: Int,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onTrackLongClick: (TrackItem) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var tracks by remember(artist.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
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

    val hiRes = remember(artist.thumbnailUrl) { upscaleThumb(artist.thumbnailUrl, 600) }
    val white = Color.White

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E12))) {
        FlowingBackground(
            thumbnailUrl = hiRes,
            audioSessionId = audioSessionId,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.45f),
                        Color.Black.copy(alpha = 0.85f),
                    ),
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
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Закрыть", tint = white)
                        }
                    }
                    Artwork(
                        url = hiRes ?: artist.thumbnailUrl,
                        modifier = Modifier.size(140.dp).clip(CircleShape),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = artist.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = white,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SourceBadge(artist.source, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Исполнитель · ${sourceLabel(artist.source)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = white.copy(alpha = 0.8f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
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
                else -> itemsIndexed(tracks, key = { _, it -> it.url }) { index, t ->
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
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Закрыть", tint = white)
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
                                    Icons.Filled.MusicNote,
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
                itemsIndexed(tracks, key = { _, it -> it.url }) { index, t ->
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
            imageVector = Icons.Filled.Cloud,
            contentDescription = "SoundCloud",
            tint = Color(0xFFFF5500),
            modifier = modifier,
        )
        Source.YOUTUBE_MUSIC -> Icon(
            imageVector = Icons.Filled.PlayCircle,
            contentDescription = "YouTube Music",
            tint = Color(0xFFFF0000),
            modifier = modifier,
        )
        Source.BANDCAMP -> Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = "Bandcamp",
            tint = Color(0xFF629AA9),
            modifier = modifier,
        )
        Source.DEEZER -> Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = "Deezer",
            tint = Color(0xFFA238FF),
            modifier = modifier,
        )
        Source.TIDAL -> Icon(
            imageVector = Icons.Filled.MusicNote,
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
                Icons.Filled.MusicNote,
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
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
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

    // Свайп вниз → свернуть в мини-плеер.
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .background(Color(0xFF0E0E12))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 320f) onCollapse() else dragOffset = 0f
                    },
                    onVerticalDrag = { _, delta ->
                        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
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
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Свернуть", tint = white)
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
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Очередь",
                        tint = white,
                    )
                }
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        Icons.Filled.Lyrics,
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = hiRes,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.White.copy(alpha = 0.06f)),
                        )
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
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Нравится",
                                    tint = heartColor,
                                    modifier = Modifier.size(30.dp).scale(likeScale),
                                )
                            }
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
                        Icons.Filled.Shuffle,
                        contentDescription = "Перемешать",
                        tint = if (shuffle) accent else white,
                    )
                }
                IconButton(onClick = onPrev, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Filled.SkipPrevious,
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
                                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "Пауза" else "Играть",
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Вперёд",
                        tint = white,
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        Icons.Filled.Repeat,
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
    LaunchedEffect(activeIndex) {
        runCatching { listState.animateScrollToItem(activeIndex) }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 130.dp),
    ) {
        itemsIndexed(lines) { i, line ->
            val active = i == activeIndex
            Text(
                text = line.text.ifBlank { "♪" },
                color = if (active) white else dim.copy(alpha = 0.45f),
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                style = if (active) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
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
