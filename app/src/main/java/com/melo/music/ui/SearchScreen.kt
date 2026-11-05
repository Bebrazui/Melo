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
internal fun SearchBar(
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

/** Фигура «печенька/цветок» с N лепестками. */
internal class CookieShape(
    private val petals: Int = 8,
    private val amp: Float = 0.12f,
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rMax = minOf(cx, cy)
        val base = rMax / (1f + amp)
        val steps = petals * 24
        val twoPi = (2.0 * Math.PI).toFloat()
        for (i in 0..steps) {
            val a = i.toFloat() / steps * twoPi
            val r = base * (1f + amp * kotlin.math.cos(petals * a))
            val x = cx + r * kotlin.math.cos(a)
            val y = cy + r * kotlin.math.sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/** Карта «Sea» в виде цветка (системные цвета): запуск/управление бесконечной волной. */
/** Разнообразный экран результатов поиска: hero-трек, исполнители, альбомы, сетка и список. */
@Composable
internal fun SearchResultsScreen(
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
                    LoadingIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

/** Крупная карточка «Главный результат»: обложка на всю ширину с градиентом. */
@Composable
internal fun TopResultCard(
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
            .verticalScrollEdgeItemEffect()
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
                    resolving -> LoadingIndicator(
                        modifier = Modifier.size(24.dp),
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
internal fun SearchAlbumCard(
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
                            LoadingIndicator(modifier = Modifier.size(28.dp))
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
                                     LoadingIndicator(modifier = Modifier.size(20.dp))
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
internal fun SearchArtistTile(item: TrackItem, onClick: () -> Unit) {
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
internal fun SearchEmptyState(query: String, topInset: androidx.compose.ui.unit.Dp) {
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


