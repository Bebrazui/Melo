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


@Composable
internal fun SeaCard(
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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isDark = cs.background.luminance() < 0.5f
    val outerShape = remember { CookieShape(8, 0.11f) }
    val btnShape = remember { CookieShape(8, 0.16f) }
    val innerColor = if (isDark) {
        lerp(cs.secondaryContainer, Color.Black, 0.25f)
    } else {
        cs.primaryContainer
    }
    val onColor = if (isDark) Color.White else cs.onPrimaryContainer
    val sideBtnBg = if (isDark) cs.primaryContainer else cs.surfaceContainerHighest.copy(alpha = 0.85f)
    val sideBtnTint = if (isDark) cs.onPrimaryContainer else cs.onSurface

    val outerSize = if (isLandscape) 180.dp else 300.dp
    val innerSize = if (isLandscape) 140.dp else 232.dp

    // Лепестки медленно крутятся при воспроизведении (на паузе замирают, без рывка).
    val rotOuter = remember { Animatable(0f) }
    val rotInner = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        launch { while (isActive) { rotOuter.animateTo(rotOuter.value + 360f, tween(28000, easing = LinearEasing)) } }
        launch { while (isActive) { rotInner.animateTo(rotInner.value - 360f, tween(19000, easing = LinearEasing)) } }
    }
    // Лёгкая пульсация лепестков.
    val pulse = rememberInfiniteTransition(label = "seaPulse")
    val pOuter by pulse.animateFloat(1f, 1.04f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "po")
    val pInner by pulse.animateFloat(1.03f, 0.98f, infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pi")
    val sOuter = if (isPlaying) pOuter else 1f
    val sInner = if (isPlaying) pInner else 1f

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = if (isLandscape) 2.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Внешний лепесток (вращается в одну сторону).
        Box(
            modifier = Modifier
                .size(outerSize)
                .graphicsLayer { rotationZ = rotOuter.value; scaleX = sOuter; scaleY = sOuter }
                .clip(outerShape)
                .background(cs.secondaryContainer),
        )
        // Внутренний лепесток (вращается в другую сторону, чуть быстрее).
        Box(
            modifier = Modifier
                .size(innerSize)
                .graphicsLayer { rotationZ = rotInner.value; scaleX = sInner; scaleY = sInner }
                .clip(outerShape)
                .background(innerColor),
        )
        // Контент поверх — не вращается.
        Box(modifier = Modifier.width(innerSize), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Sea",
                    style = if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = onColor,
                )
                Spacer(Modifier.height(if (isLandscape) 4.dp else 10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 10.dp),
                ) {
                    // Лайк
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 36.dp else 46.dp)
                            .clip(btnShape)
                            .background(sideBtnBg)
                            .clickable(onClick = {
                                ClickFeedback.play()
                                onLike()
                            }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Нравится",
                            tint = if (isLiked) (if (isDark) cs.primary else Color(0xFFE53935)) else sideBtnTint,
                            modifier = Modifier.size(if (isLandscape) 16.dp else 20.dp),
                        )
                    }
                    // Плей/пауза
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 44.dp else 58.dp)
                            .clip(btnShape)
                            .background(cs.primary)
                            .clickable(enabled = !loading, onClick = {
                                ClickFeedback.play()
                                onPlayPause()
                            }),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = loading,
                            enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                            exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp),
                                color = cs.onPrimary,
                            )
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !loading,
                            enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                            exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Играть",
                                tint = cs.onPrimary,
                                modifier = Modifier.size(if (isLandscape) 22.dp else 28.dp),
                            )
                        }
                    }
                    // Следующий
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 36.dp else 46.dp)
                            .clip(btnShape)
                            .background(sideBtnBg)
                            .clickable(onClick = {
                                ClickFeedback.play()
                                onNext()
                            }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Дальше",
                            tint = sideBtnTint,
                            modifier = Modifier.size(if (isLandscape) 18.dp else 22.dp),
                        )
                    }
                }
                Spacer(Modifier.height(if (isLandscape) 4.dp else 12.dp))
                Text(
                    text = playingTitle ?: "Бесконечная волна",
                    style = if (isLandscape) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = if (isLandscape) 8.dp else 18.dp),
                )
                Text(
                    text = playingArtist ?: "под твой вкус",
                    style = MaterialTheme.typography.labelSmall,
                    color = onColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = if (isLandscape) 8.dp else 18.dp),
                )
            }
        }
    }
}

@Composable
internal fun HomeFeed(
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
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.7f),
                        exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.7f),
                    ) {
                        LoadingIndicator(modifier = Modifier.size(44.dp))
                    }
                }
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
internal fun Greeting(onOpenAccount: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
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
            .padding(start = 20.dp, end = 20.dp, top = if (isLandscape) 4.dp else 16.dp, bottom = if (isLandscape) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            com.melo.music.auth.AuthManager.avatarUrl,
            if (isLandscape) 36.dp else 46.dp,
            Modifier.clip(CircleShape).clickable(onClick = onOpenAccount),
        )
        Spacer(Modifier.width(if (isLandscape) 10.dp else 14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greet,
                style = (if (isLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge).copy(
                    fontSize = if (isLandscape) 18.sp else 22.sp,
                    letterSpacing = (-0.3).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = cs.onBackground,
            )
            Text(
                text = "Твоя музыка всегда с тобой",
                style = if (isLandscape) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = cs.onBackground.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 22.sp,
            letterSpacing = (-0.3).sp,
        ),
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
    )
}

@Composable
internal fun QuickPickGrid(
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
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.7f),
                exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.7f),
            ) {
                LoadingIndicator(modifier = Modifier.size(36.dp))
            }
        }
        return
    }
    if (tracks.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val qpBg = if (isDark) Color.White.copy(alpha = 0.06f) else cs.surfaceContainerHigh
    val qpBorder = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.06f) else cs.outlineVariant.copy(alpha = 0.35f))
    val grid = tracks.distinctBy { it.url }.take(12)
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .bouncyHorizontalOverscroll(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(grid.size, key = { "qp_" + grid[it].url }) { index ->
            val t = grid[index]
            Row(
                modifier = Modifier
                    .carouselCenterGridItemEffect(gridState, index)
                    .width(268.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .combinedClickable(
                        onClick = { onPlay(grid, index) },
                        onLongClick = { onLongClick(t) },
                    )
                    .background(qpBg)
                    .border(qpBorder, RoundedCornerShape(20.dp))
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
                        color = cs.onSurface,
                        maxLines = 1,
                    )
                    t.uploader?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HorizontalShelf(
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
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.7f),
                exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.7f),
            ) {
                LoadingIndicator(modifier = Modifier.size(36.dp))
            }
        }
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
internal fun ShelfCard(
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
                ) { LoadingIndicator(color = Color.White, modifier = Modifier.size(28.dp)) }
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
internal fun TrackCard(
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
            .verticalScrollEdgeItemEffect()
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
                resolving -> androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.6f),
                    exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.6f),
                ) {
                    LoadingIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 4.dp),
                        color = iconTint,
                    )
                }
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
internal fun ArtistCard(item: TrackItem, onClick: () -> Unit) {
    val pressSource = remember { MutableInteractionSource() }
    Surface(
        shape = ShapeCache.smooth20,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScrollEdgeItemEffect()
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

