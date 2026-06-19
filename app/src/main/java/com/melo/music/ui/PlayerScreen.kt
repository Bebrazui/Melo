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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import com.melo.music.extractor.ItemKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import com.melo.music.extractor.ResolvedTrack
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Главный экран MVP в стиле Material 3 Expressive: поиск + популярная музыка
 * по региону. Тап по треку → on-device резолв (NewPipe) → фоновое воспроизведение.
 */
@Composable
fun PlayerScreen(
    onSearch: (String) -> Flow<List<TrackItem>>,
    onLoadRecommendations: suspend () -> List<TrackItem>,
    onLoadArtistTracks: suspend (String) -> List<TrackItem>,
    scGetId: () -> String?,
    onScSetManual: suspend (String) -> Boolean,
    onScRefresh: suspend () -> String?,
    onResolveAudioUrl: suspend (String) -> ResolvedTrack,
    isCached: (String) -> Boolean,
    onPrefetch: (String) -> Unit,
    onFetchLyrics: suspend (String, String?) -> String?,
    onPlayResolved: (ResolvedTrack) -> Unit,
    onTogglePlayPause: () -> Unit,
    playerProvider: () -> MediaController?,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val recommendationsTitle = remember {
        val c = Locale.getDefault().country
        val name = if (c.isBlank()) null else Locale("", c).getDisplayCountry(Locale.getDefault())
        if (name.isNullOrBlank()) "Популярное" else "Популярное · $name"
    }

    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(MeloTab.Home) }
    var playerExpanded by remember { mutableStateOf(false) }
    var artistOpen by remember { mutableStateOf<TrackItem?>(null) }
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var listTitle by remember { mutableStateOf(recommendationsTitle) }
    var listLoading by remember { mutableStateOf(true) }
    var listError by remember { mutableStateOf<String?>(null) }

    var nowPlaying by remember { mutableStateOf<TrackItem?>(null) }
    var resolvingUrl by remember { mutableStateOf<String?>(null) }

    // Очередь воспроизведения (управляется в UI) + режимы.
    var playingList by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var playingIndex by remember { mutableStateOf(-1) }
    var shuffle by remember { mutableStateOf(false) }
    var repeatOne by remember { mutableStateOf(false) }
    val liked = remember { mutableStateListOf<TrackItem>() }
    fun isLiked(item: TrackItem) = liked.any { it.url == item.url }

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

    fun runSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
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
                    partial.filter { it.kind == ItemKind.TRACK }.take(8)
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
        playingList = list
        playingIndex = index
        val item = list[index]
        nowPlaying = item
        scope.launch {
            val tStart = android.os.SystemClock.elapsedRealtime()
            resolvingUrl = item.url
            runCatching { onResolveAudioUrl(item.url) }
                .onSuccess {
                    android.util.Log.e(
                        "MeloPerf",
                        "TAP→resolved ${android.os.SystemClock.elapsedRealtime() - tStart}ms",
                    )
                    onPlayResolved(it)
                }
                .onFailure { listError = "Не удалось воспроизвести: ${it.message}" }
            resolvingUrl = null
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
    fun toggleLike(item: TrackItem) {
        val i = liked.indexOfFirst { it.url == item.url }
        if (i >= 0) liked.removeAt(i) else liked.add(item)
    }

    // Автопереход в конце трека (или повтор).
    val playbackState = playback.playbackState
    var lastEndedFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(playbackState, nowPlaying?.url) {
        val cur = nowPlaying ?: return@LaunchedEffect
        if (playbackState == Player.STATE_ENDED && lastEndedFor != cur.url) {
            lastEndedFor = cur.url
            if (repeatOne) {
                controller?.let { it.seekTo(0); it.play() }
            } else {
                playNext()
            }
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
                    )
                    Text(
                        text = listTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                    )
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
                }

                MeloTab.Favorite -> if (liked.isEmpty()) {
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
                        itemsIndexed(liked, key = { _, it -> it.url }) { index, item ->
                            TrackCard(
                                item = item,
                                resolving = resolvingUrl == item.url,
                                playing = nowPlaying?.url == item.url && isPlaying,
                                onClick = { playAt(liked.toList(), index) },
                            )
                        }
                    }
                }

                MeloTab.Account -> AccountTab(
                    scGetId = scGetId,
                    onScSetManual = onScSetManual,
                    onScRefresh = onScRefresh,
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
                    position = playback.position,
                    duration = playback.duration,
                    onSeek = { ms -> controller?.seekTo(ms) },
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = { playNext() },
                    onPrev = { playPrev() },
                    onToggleShuffle = { shuffle = !shuffle },
                    onToggleRepeat = { repeatOne = !repeatOne },
                    onToggleLike = { toggleLike(item) },
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
                    onPlay = { tracks, index -> playAt(tracks, index) },
                    onClose = { artistOpen = null },
                )
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
) {
    val scope = rememberCoroutineScope()
    var currentId by remember { mutableStateOf(scGetId()) }
    var manual by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Аккаунт", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Вход и подписка — скоро",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        ElevatedCard(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(Source.SOUNDCLOUD, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SoundCloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = currentId?.let { "client_id: ✓ сохранён (${it.take(6)}…)" }
                        ?: "client_id: ✗ нет — SoundCloud не работает",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentId != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
                Text(
                    "Нужен один раз. Дальше SoundCloud работает без VPN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        if (busy) return@OutlinedButton
                        scope.launch {
                            busy = true; message = "Пробую добыть автоматически…"
                            val r = onScRefresh()
                            currentId = r
                            message = if (r != null) "Готово ✓" else "Не вышло — вставь вручную"
                            busy = false
                        }
                    },
                    enabled = !busy,
                ) { Text("Обновить автоматически") }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = manual,
                    onValueChange = { manual = it },
                    label = { Text("Вставить client_id вручную") },
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
            }
        }

        Text(
            "Как взять client_id: открой в браузере (с byebyedpi) m.soundcloud.com → DevTools/Network → " +
                "любой запрос к api-v2.soundcloud.com → скопируй параметр client_id.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Поиск трека или исполнителя") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Найти")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    )
}

@Composable
private fun TrackCard(
    item: TrackItem,
    resolving: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
    onPlay: (List<TrackItem>, Int) -> Unit,
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
                    )
                }
            }
        }
    }
}

private fun sourceLabel(source: Source): String = when (source) {
    Source.YOUTUBE_MUSIC -> "YouTube Music"
    Source.SOUNDCLOUD -> "SoundCloud"
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
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onFetchLyrics: suspend () -> String?,
    onCollapse: () -> Unit,
) {
    BackHandler(onBack = onCollapse)

    var dragFraction by remember { mutableStateOf<Float?>(null) }

    var showLyrics by remember(item.url) { mutableStateOf(false) }
    var lyrics by remember(item.url) { mutableStateOf<String?>(null) }
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
        if (hiRes != null) {
            AsyncImage(
                model = hiRes,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(48.dp),
            )
        }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp, max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when {
                            lyricsLoading -> CircularProgressIndicator(color = white)
                            lyrics.isNullOrBlank() -> Text(
                                "Текст не найден",
                                color = whiteDim,
                                textAlign = TextAlign.Center,
                            )
                            else -> Text(
                                text = lyrics!!,
                                color = white,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
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
    val position: Long,
    val duration: Long,
    val error: String?,
)

@Composable
private fun rememberPlaybackState(controller: MediaController?): PlaybackUi {
    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose { }
        isPlaying = controller.isPlaying
        playbackState = controller.playbackState
        position = controller.currentPosition.coerceAtLeast(0L)
        duration = controller.duration.takeIf { it > 0 } ?: 0L
        error = controller.playerError?.message
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                duration = controller.duration.takeIf { it > 0 } ?: 0L
                position = controller.currentPosition.coerceAtLeast(0L)
                val name = when (state) {
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "IDLE"
                }
                android.util.Log.e("MeloPerf", "playbackState=$name")
            }
            override fun onPlayerErrorChanged(e: PlaybackException?) {
                error = e?.message
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    // Позицию двигаем только во время игры (для слайдера).
    LaunchedEffect(controller, isPlaying) {
        if (controller != null && isPlaying) {
            while (true) {
                position = controller.currentPosition.coerceAtLeast(0L)
                if (duration <= 0) duration = controller.duration.takeIf { it > 0 } ?: 0L
                delay(500)
            }
        }
    }

    return PlaybackUi(isPlaying, playbackState, position, duration, error)
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
