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


data class TrackColors(
    val accent: Color,
    val background: Color,
    val solidBg: Color,
)

internal object TrackColorCache {
    private val memoryCache = androidx.compose.runtime.mutableStateMapOf<String, TrackColors>()

    fun get(url: String?, isDark: Boolean): TrackColors {
        if (url == null) return fallback(isDark)
        return memoryCache[url] ?: fallback(isDark)
    }

    fun fallback(isDark: Boolean): TrackColors {
        val accent = Color(0xFFD4A853)
        val bg = if (isDark) Color(0xFF1E1A16) else Color(0xFFF5E6CC)
        val solid = if (isDark) Color(0xFF181512) else Color(0xFFEDE0D4)
        return TrackColors(accent, bg, solid)
    }

    suspend fun extract(context: Context, url: String, isDark: Boolean): TrackColors {
        memoryCache[url]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val req = coil.request.ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(128)
                    .build()
                val drawable = coil.Coil.imageLoader(context).execute(req).drawable
                val bmp = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bmp != null) {
                    val palette = androidx.palette.graphics.Palette.from(bmp).generate()
                    val accentSwatch = palette.vibrantSwatch
                        ?: palette.dominantSwatch
                        ?: palette.lightVibrantSwatch
                        ?: palette.mutedSwatch
                    val bgSwatch = palette.darkVibrantSwatch
                        ?: palette.dominantSwatch
                        ?: palette.vibrantSwatch
                        ?: palette.mutedSwatch

                    val fallbackColors = fallback(isDark)
                    val accent = accentSwatch?.let { Color(it.rgb) } ?: fallbackColors.accent
                    val bg = bgSwatch?.let { Color(it.rgb) } ?: accent

                    val solidBg = if (isDark) {
                        if (bg.luminance() > 0.28f) {
                            androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.42f)
                        } else if (bg.luminance() < 0.04f) {
                            androidx.compose.ui.graphics.lerp(bg, accent, 0.35f)
                        } else {
                            bg
                        }
                    } else {
                        if (bg.luminance() < 0.65f) {
                            androidx.compose.ui.graphics.lerp(bg, Color.White, 0.55f)
                        } else {
                            bg
                        }
                    }

                    val result = TrackColors(accent, bg, solidBg)
                    withContext(Dispatchers.Main) {
                        memoryCache[url] = result
                    }
                    result
                } else {
                    fallback(isDark)
                }
            } catch (_: Exception) {
                fallback(isDark)
            }
        }
    }
}

/** Состояние плеера для UI (через Player.Listener, без опроса). */
internal class PlaybackUi(
    val isPlaying: Boolean,
    val playbackState: Int,
    val position: State<Long>,
    val duration: State<Long>,
    val error: String?,
)

@Composable
internal fun rememberPlaybackState(controller: MediaController?): PlaybackUi {
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
internal fun SyncedLyrics(
    lines: List<LrcLine>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    white: Color,
    dim: Color,
    modifier: Modifier = Modifier.fillMaxSize(),
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
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 110.dp),
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
internal fun karaokeLine(
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

/** Повышает запрашиваемый размер обложки:
 * - для SoundCloud/sndcdn: -large/-badge/-small/-mini -> -t500x500 (высокое разрешение 500x500)
 * - для YouTube/googleusercontent: =wNNN-hNNN -> =w$size-h$size-l90-rj
 */
fun upscaleThumb(url: String?, size: Int = 600): String? {
    if (url == null) return null
    if (url.contains("sndcdn.com") || url.contains("soundcloud.com")) {
        if (url.contains("default_avatar")) return url
        return url.replace(Regex("-(?:large|badge|small|mini)\\.(jpg|jpeg|png)"), "-t500x500.$1")
    }
    val cut = url.indexOf('=')
    return if (cut > 0) url.substring(0, cut) + "=w$size-h$size-l90-rj" else url
}

internal fun formatMillis(ms: Long): String = formatDuration(ms / 1000).ifBlank { "0:00" }

internal fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}

/**
 * Плашка с предупреждением об удалении трека правообладателем и кнопкой быстрого поиска альтернативы.
 */
@Composable
internal fun CopyrightBlockedBanner(
    track: TrackItem,
    onFindSimilar: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF0201212),
        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF5252)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Удалено правообладателем",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onFindSimilar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Похожее",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Кинематографичный плавный туман, медленно дрейфующий в нижней части плеера
 * и мягко наплывающий на кнопки управления и нижний край обложки.
 */
@Composable
internal fun AtmosphericMistLayer(
    mistColor: Color,
    tiltRoll: Float,
    tiltPitch: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mistDrift")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase1",
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(36_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase2",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val p1 = phase1
        val p2 = phase2

        val tiltX = tiltRoll * 6.dp.toPx()
        val tiltY = tiltPitch * 6.dp.toPx()

        // 1. Основное облако в зоне кнопок воспроизведения
        val c1x = (w * 0.35f) + (kotlin.math.cos(p1) * 32.dp.toPx()) + tiltX
        val c1y = (h * 0.82f) + (kotlin.math.sin(p1) * 20.dp.toPx()) + tiltY
        val r1 = w * 0.72f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    mistColor.copy(alpha = 0.16f),
                    mistColor.copy(alpha = 0.07f),
                    Color.Transparent,
                ),
                center = Offset(c1x, c1y),
                radius = r1,
            ),
            center = Offset(c1x, c1y),
            radius = r1,
        )

        // 2. Второе облако в правой части (сикер и правая часть кнопок)
        val c2x = (w * 0.72f) + (kotlin.math.sin(p2) * 36.dp.toPx()) + (tiltX * 1.2f)
        val c2y = (h * 0.76f) + (kotlin.math.cos(p2) * 18.dp.toPx()) + (tiltY * 1.2f)
        val r2 = w * 0.65f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    mistColor.copy(alpha = 0.13f),
                    mistColor.copy(alpha = 0.05f),
                    Color.Transparent,
                ),
                center = Offset(c2x, c2y),
                radius = r2,
            ),
            center = Offset(c2x, c2y),
            radius = r2,
        )

        // 3. Более высокое мягкое облако, поднимающееся к низу обложки
        val c3x = (w * 0.50f) + (kotlin.math.cos(p2 * 0.8f) * 25.dp.toPx()) + (tiltX * 0.8f)
        val c3y = (h * 0.64f) + (kotlin.math.sin(p1 * 0.8f) * 16.dp.toPx()) + (tiltY * 0.8f)
        val r3 = w * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    mistColor.copy(alpha = 0.10f),
                    mistColor.copy(alpha = 0.04f),
                    Color.Transparent,
                ),
                center = Offset(c3x, c3y),
                radius = r3,
            ),
            center = Offset(c3x, c3y),
            radius = r3,
        )
    }
}


