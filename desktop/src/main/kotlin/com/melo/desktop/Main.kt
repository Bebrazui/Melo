package com.melo.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.melo.desktop.audio.DesktopAudioPlayer
import com.melo.desktop.audio.LocalStreamServer
import com.melo.desktop.audio.cache.AudioPreloader
import com.melo.desktop.byedpi.ByeDpiManager
import com.melo.desktop.extractor.DesktopExtractor
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.recommend.DesktopRecommender
import com.melo.desktop.storage.DesktopStorage
import com.melo.desktop.storage.DpiEngine
import com.melo.desktop.ui.NavDestination
import com.melo.desktop.ui.components.BottomPlayer
import com.melo.desktop.ui.components.FlowingBackground
import com.melo.desktop.ui.components.LyricsView
import com.melo.desktop.ui.components.NowPlayingOverlay
import com.melo.desktop.ui.components.Sidebar
import com.melo.desktop.ui.screens.HomeScreen
import com.melo.desktop.ui.screens.LibraryScreen
import com.melo.desktop.ui.screens.SearchScreen
import com.melo.desktop.ui.screens.SettingsScreen
import com.melo.desktop.ui.theme.MeloDesktopTheme
import com.melo.desktop.zapret.ZapretManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun main() = application {
    // Инициализация сервисов
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            LocalStreamServer.start()
            when (DesktopStorage.dpiEngine.value) {
                DpiEngine.AUTO -> {
                    if (!ZapretManager.isRunning() && DesktopStorage.byedpiEnabled.value) {
                        ByeDpiManager.start(DesktopStorage.byedpiCmd.value)
                    }
                }
                DpiEngine.BYEDPI -> {
                    if (DesktopStorage.byedpiEnabled.value) {
                        ByeDpiManager.start(DesktopStorage.byedpiCmd.value)
                    }
                }
                DpiEngine.ZAPRET -> {
                    if (!ZapretManager.isRunning()) {
                        ZapretManager.start(
                            preset = DesktopStorage.zapretPreset.value,
                            customArgs = DesktopStorage.zapretCustomArgs.value,
                            customPath = DesktopStorage.zapretCustomPath.value,
                        )
                    }
                }
                DpiEngine.DISABLED -> {
                    ByeDpiManager.stop()
                }
            }
            DesktopExtractor.ensureInit()
        }
    }

    val windowState = rememberWindowState(width = 1180.dp, height = 760.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Melo — Музыка без границ",
        state = windowState,
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when {
                    keyEvent.key == Key.Spacebar -> {
                        DesktopAudioPlayer.togglePlayPause()
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.DirectionRight -> {
                        MeloAppController.playNext()
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.DirectionLeft -> {
                        MeloAppController.playPrev()
                        true
                    }
                    keyEvent.key == Key.DirectionRight -> {
                        DesktopAudioPlayer.seekTo(DesktopAudioPlayer.currentPositionMs + 5000)
                        true
                    }
                    keyEvent.key == Key.DirectionLeft -> {
                        DesktopAudioPlayer.seekTo((DesktopAudioPlayer.currentPositionMs - 5000).coerceAtLeast(0))
                        true
                    }
                    keyEvent.key == Key.M -> {
                        if (DesktopAudioPlayer.volume > 0f) DesktopAudioPlayer.setPlayerVolume(0f) else DesktopAudioPlayer.setPlayerVolume(0.8f)
                        true
                    }
                    keyEvent.key == Key.Escape -> {
                        if (MeloAppController.isNowPlayingOpen) {
                            MeloAppController.isNowPlayingOpen = false
                            true
                        } else if (MeloAppController.isLyricsOpen) {
                            MeloAppController.isLyricsOpen = false
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            } else {
                false
            }
        },
    ) {
        MeloDesktopTheme {
            MeloAppContent()
        }
    }
}

/** Глобальный контроллер очереди треков и бесконечной волны «Sea». */
object MeloAppController {
    val queue = mutableStateListOf<TrackItem>()
    var currentIndex by mutableStateOf(0)

    var isSeaActive by mutableStateOf(false)
    var isSeaLoading by mutableStateOf(false)

    var isNowPlayingOpen by mutableStateOf(false)
    var isLyricsOpen by mutableStateOf(false)

    suspend fun buildSeaBatch(seed: TrackItem, exclude: Set<String>): List<TrackItem> {
        val raw = runCatching { DesktopExtractor.relatedTracks(seed) }.getOrDefault(emptyList())
        val filtered = raw
            .filter { it.kind == com.melo.desktop.extractor.ItemKind.TRACK && it.url !in exclude }
            .distinctBy { it.url }
        if (filtered.isNotEmpty()) {
            return filtered.take(20)
        }
        val fallback = raw.filter { it.kind == com.melo.desktop.extractor.ItemKind.TRACK }.distinctBy { it.url }
        if (fallback.isNotEmpty()) {
            return fallback.take(20)
        }
        return DesktopExtractor.recommendations()
            .filter { it.kind == com.melo.desktop.extractor.ItemKind.TRACK && it.url !in exclude }
            .take(15)
    }

    fun startSea(seed: TrackItem? = null, querySeed: String? = null) {
        if (isSeaLoading) return
        isSeaLoading = true
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val seedTrack: TrackItem? = when {
                    seed != null -> seed
                    querySeed != null -> {
                        val list = DesktopExtractor.shelf(querySeed).filter { it.kind == com.melo.desktop.extractor.ItemKind.TRACK }
                        list.firstOrNull()
                    }
                    DesktopAudioPlayer.currentTrack != null -> {
                        val cur = DesktopAudioPlayer.currentTrack!!
                        TrackItem(
                            title = cur.title,
                            uploader = cur.artist,
                            url = cur.originalUrl,
                            durationSeconds = DesktopAudioPlayer.durationMs / 1000,
                            thumbnailUrl = cur.thumbnailUrl,
                            source = cur.source,
                        )
                    }
                    DesktopStorage.favorites.isNotEmpty() -> DesktopStorage.favorites.randomOrNull()
                    DesktopStorage.history.isNotEmpty() -> DesktopStorage.history.randomOrNull()
                    else -> DesktopExtractor.recommendations().firstOrNull { it.kind == com.melo.desktop.extractor.ItemKind.TRACK }
                }

                if (seedTrack == null) {
                    return@launch
                }

                val exclude = DesktopStorage.history.map { it.url }.toSet() + seedTrack.url
                var batch = if (querySeed != null) {
                    val list = DesktopExtractor.shelf(querySeed).filter { it.kind == com.melo.desktop.extractor.ItemKind.TRACK }
                    list.filter { it.url !in exclude }.distinctBy { it.url }.take(20).ifEmpty { list.take(20) }
                } else {
                    buildSeaBatch(seedTrack, exclude)
                }

                if (batch.isEmpty()) {
                    batch = listOf(seedTrack)
                }

                withContext(Dispatchers.Main) {
                    isSeaActive = true
                    playTrack(batch.first(), batch, keepSea = true)
                }
            } catch (e: Exception) {
                System.err.println("[MeloAppController] startSea error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isSeaLoading = false
                }
            }
        }
    }

    fun extendSea(autoPlayNext: Boolean = false) {
        if (!isSeaActive || isSeaLoading) return
        isSeaLoading = true
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val base = queue.getOrNull(currentIndex) ?: queue.lastOrNull()
                val exclude = (queue.map { it.url } + DesktopStorage.history.map { it.url }).toSet()
                val batch = if (base != null) {
                    buildSeaBatch(base, exclude)
                } else {
                    emptyList()
                }
                if (batch.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        queue.addAll(batch)
                        if (autoPlayNext && currentIndex < queue.lastIndex) {
                            currentIndex++
                            playTrack(queue[currentIndex], keepSea = true)
                        }
                    }
                }
            } catch (e: Exception) {
                System.err.println("[MeloAppController] extendSea error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isSeaLoading = false
                }
            }
        }
    }

    fun playTrack(track: TrackItem, newQueue: List<TrackItem>? = null, keepSea: Boolean = false) {
        if (!keepSea) {
            isSeaActive = false
        }
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
        scope.launch {
            withContext(Dispatchers.Main) {
                if (newQueue != null) {
                    queue.clear()
                    queue.addAll(newQueue)
                    currentIndex = newQueue.indexOfFirst { it.url == track.url }.coerceAtLeast(0)
                } else if (queue.none { it.url == track.url }) {
                    queue.add(track)
                    currentIndex = queue.lastIndex
                }
            }

            DesktopStorage.addToHistory(track)

            runCatching {
                var resolved = DesktopExtractor.resolveAudioUrl(track.url)
                if (resolved.durationSeconds <= 0 && track.durationSeconds > 0) {
                    resolved = resolved.copy(durationSeconds = track.durationSeconds)
                }
                withContext(Dispatchers.Main) {
                    DesktopAudioPlayer.play(resolved)
                }

                // Фоновая предзагрузка следующего трека в очереди или из волны
                val nextInQueue = queue.getOrNull(currentIndex + 1)
                AudioPreloader.preloadNext(currentTrack = track, nextTrack = nextInQueue)
            }.onFailure { err ->
                System.err.println("[MeloAppController] Resolve failed: ${err.message}")
            }
        }
    }

    fun playNext() {
        if (queue.isEmpty()) {
            if (isSeaActive) {
                startSea()
            }
            return
        }
        if (DesktopAudioPlayer.isShuffle && queue.size > 1) {
            var nextIdx = queue.indices.random()
            if (nextIdx == currentIndex) {
                nextIdx = (currentIndex + 1) % queue.size
            }
            currentIndex = nextIdx
            playTrack(queue[currentIndex], keepSea = isSeaActive)
            return
        }
        if (currentIndex < queue.lastIndex) {
            currentIndex++
            playTrack(queue[currentIndex], keepSea = isSeaActive)
            if (isSeaActive && currentIndex >= queue.size - 3) {
                extendSea()
            }
        } else if (isSeaActive) {
            extendSea(autoPlayNext = true)
        } else if (DesktopAudioPlayer.repeatMode == DesktopAudioPlayer.RepeatMode.ALL && queue.isNotEmpty()) {
            currentIndex = 0
            playTrack(queue[currentIndex], keepSea = isSeaActive)
        } else {
            DesktopAudioPlayer.seekTo(0)
            DesktopAudioPlayer.pause()
        }
    }

    fun playPrev() {
        if (DesktopAudioPlayer.currentPositionMs > 3000L) {
            DesktopAudioPlayer.seekTo(0)
            return
        }
        if (currentIndex > 0) {
            currentIndex--
            playTrack(queue[currentIndex], keepSea = isSeaActive)
        } else if (DesktopAudioPlayer.repeatMode == DesktopAudioPlayer.RepeatMode.ALL && queue.isNotEmpty()) {
            currentIndex = queue.lastIndex
            playTrack(queue[currentIndex], keepSea = isSeaActive)
        } else {
            DesktopAudioPlayer.seekTo(0)
        }
    }

    init {
        DesktopAudioPlayer.onTrackEnded = {
            playNext()
        }
    }
}

@Composable
fun MeloAppContent() {
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    val isLyricsOpen = MeloAppController.isLyricsOpen
    val isNowPlayingOpen = MeloAppController.isNowPlayingOpen
    val currentTrack = DesktopAudioPlayer.currentTrack

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Основное окно (Sidebar + Контент + Нижний плеер) ───────────────
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Левая боковая панель
                Sidebar(
                    currentDestination = currentDestination,
                    onNavigate = { currentDestination = it },
                )

                // Центральный экран
                Box(modifier = Modifier.weight(1f)) {
                    when (currentDestination) {
                        NavDestination.HOME -> HomeScreen(
                            onPlayTrack = { track -> MeloAppController.playTrack(track) },
                            onOpenSearch = { currentDestination = NavDestination.SEARCH },
                            onOpenSettings = { currentDestination = NavDestination.SETTINGS },
                        )
                        NavDestination.SEARCH -> SearchScreen(
                            onPlayTrack = { track -> MeloAppController.playTrack(track) },
                        )
                        NavDestination.FAVORITES -> LibraryScreen(
                            initialTab = 0,
                            onPlayTrack = { track -> MeloAppController.playTrack(track) },
                        )
                        NavDestination.HISTORY -> LibraryScreen(
                            initialTab = 1,
                            onPlayTrack = { track -> MeloAppController.playTrack(track) },
                        )
                        NavDestination.PLAYLISTS -> LibraryScreen(
                            initialTab = 2,
                            onPlayTrack = { track -> MeloAppController.playTrack(track) },
                        )
                        NavDestination.SETTINGS -> SettingsScreen()
                    }
                }

                // Правая панель текста песни
                AnimatedVisibility(
                    visible = isLyricsOpen,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                ) {
                    LyricsView(
                        onClose = { MeloAppController.isLyricsOpen = false },
                    )
                }
            }

            // ── Нижний бар плеера (клик открывает NowPlaying) ──────────────────
            BottomPlayer(
                onToggleLyrics = { MeloAppController.isLyricsOpen = !MeloAppController.isLyricsOpen },
                isLyricsOpen = isLyricsOpen,
                onNext = { MeloAppController.playNext() },
                onPrev = { MeloAppController.playPrev() },
                onExpand = { MeloAppController.isNowPlayingOpen = true },
            )
        }

        // ── 3. Полноэкранный плеер NowPlaying (выдвигается поверх контента) ───
        NowPlayingOverlay(
            visible = isNowPlayingOpen,
            onDismiss = { MeloAppController.isNowPlayingOpen = false },
            onPrevious = { MeloAppController.playPrev() },
            onNext = { MeloAppController.playNext() },
        )
    }
}
