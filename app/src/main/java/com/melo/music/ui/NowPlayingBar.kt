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
fun Artwork(url: String?, modifier: Modifier = Modifier) {
    val upscaled = remember(url) { upscaleThumb(url, 600) }
    if (upscaled != null) {
        val context = LocalContext.current
        val request = remember(upscaled) {
            coil.request.ImageRequest.Builder(context)
                .data(upscaled)
                .memoryCacheKey(upscaled)
                .diskCacheKey(upscaled)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .crossfade(true)
                .crossfade(300)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
            onError = { state ->
                com.melo.music.util.FileLog.e("MeloArtwork", "Artwork FAILED for $upscaled: ${state.result.throwable}")
            },
            onSuccess = {
                com.melo.music.util.FileLog.d("MeloArtwork", "Artwork OK for $upscaled")
            },
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
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
internal class ScallopedShape(
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
internal fun NowPlayingBarInner(
    item: TrackItem,
    isPlaying: Boolean,
    resolving: Boolean,
    animatedTrackColor: Color,
    scallopedArt: Shape,
    scallopedBtn: Shape,
    onTogglePlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit = {},
    showArtwork: Boolean = true,
    onArtPositioned: (Rect) -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

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
                if (showArtwork) {
                    Surface(
                        shape = scallopedArt,
                        color = if (isDark) Color(0xFF1E1A16) else cs.surfaceContainerHighest,
                        border = BorderStroke(1.8.dp, animatedTrackColor),
                        modifier = Modifier
                            .size(52.dp)
                            .onGloballyPositioned { coords ->
                                onArtPositioned(coords.boundsInRoot())
                            },
                    ) {
                        Artwork(
                            url = item.thumbnailUrl,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(scallopedArt),
                        )
                    }
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
            androidx.compose.animation.AnimatedContent(
                targetState = Pair(item.cleanTitle, item.cleanUploader ?: "Неизвестный исполнитель"),
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(240)) +
                        androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(280)) { h -> (h * 0.35f).toInt() })
                        .togetherWith(
                            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180)) +
                                androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(220)) { h -> (-h * 0.35f).toInt() }
                        )
                },
                label = "miniPlayerTextTransition",
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
            ) { (titleText, artistText) ->
                Column {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            letterSpacing = (-0.2).sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = artistText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                        ),
                        color = if (isDark) {
                            lerp(Color.White.copy(alpha = 0.7f), animatedTrackColor, 0.35f)
                        } else {
                            lerp(cs.onSurfaceVariant, animatedTrackColor, 0.35f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Кнопки управления в виде фигурных звездочек / лепестков
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val playBg = if (animatedTrackColor.luminance() < 0.2f) lerp(animatedTrackColor, Color.White, 0.65f) else lerp(animatedTrackColor, Color.White, 0.42f)
            val btnBg = if (animatedTrackColor.luminance() < 0.2f) lerp(animatedTrackColor, Color.White, 0.50f) else lerp(animatedTrackColor, Color.White, 0.26f)
            val playIconTint = if (playBg.luminance() > 0.22f) Color(0xFF0F1E28) else Color.White
            val btnIconTint = if (btnBg.luminance() > 0.22f) Color(0xFF0F1E28) else Color.White

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
                        tint = btnIconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Play / Pause (чуть крупнее 50dp) с пружинной отдачей и плавной морфинг-анимацией
            val playScale by animateFloatAsState(
                targetValue = if (isPlaying) 1f else 0.94f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
                label = "playScale",
            )
            Surface(
                shape = scallopedBtn,
                color = playBg,
                modifier = Modifier
                    .size(50.dp)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }
                    .clip(scallopedBtn)
                    .clickable(onClick = onTogglePlayPause),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (resolving) {
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp),
                            color = playIconTint,
                        )
                    } else {
                        androidx.compose.animation.AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (fadeIn(tween(220)) + androidx.compose.animation.scaleIn(tween(220), initialScale = 0.65f))
                                    .togetherWith(fadeOut(tween(160)) + androidx.compose.animation.scaleOut(tween(160), targetScale = 0.65f))
                            },
                            label = "playPauseMorph",
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Пауза" else "Играть",
                                tint = playIconTint,
                                modifier = Modifier.size(26.dp),
                            )
                        }
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
                        tint = btnIconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

internal fun Modifier.drawMiniPlayerBackdrop(
    isDark: Boolean,
    dotColorBase: Color,
    bgColor: Color,
    accentColor: Color,
    playIntensity: Float,
    phase1: Float,
    phase2: Float,
): Modifier = this.drawBehind {
    val w = size.width
    val h = size.height

    // 1. Базовый фон карточки
    if (bgColor != Color.Transparent) {
        drawRect(color = bgColor)
    }

    if (com.melo.music.settings.AppSettings.solidBackground) {
        return@drawBehind
    }

    // 2. Живой плавающий волновой градиент с плавным затуханием при паузе
    val waveShift = kotlin.math.sin(phase1.toDouble() * 1.5).toFloat() * (h * 0.15f) * playIntensity
    val startY = androidx.compose.ui.util.lerp(h * 0.42f, (h * 0.10f + waveShift).coerceIn(0f, h * 0.40f), playIntensity)
    val dynamicGlow = (0.62f + kotlin.math.sin(phase2.toDouble() * 2.0).toFloat() * 0.18f).coerceIn(0.30f, 0.88f)
    val glowAlpha = androidx.compose.ui.util.lerp(0.08f, dynamicGlow, playIntensity)

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                accentColor.copy(alpha = androidx.compose.ui.util.lerp(0.02f, glowAlpha * (if (isDark) 0.35f else 0.18f), playIntensity)),
                accentColor.copy(alpha = glowAlpha * (if (isDark) 1f else 0.40f)),
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
        if (yFactor > 0.40f) {
            val progress = ((yFactor - 0.40f) / 0.60f).coerceIn(0f, 1f)
            val verticalAlpha = (progress * progress * progress * 0.88f) * androidx.compose.ui.util.lerp(0.35f, 1.0f, playIntensity)
            if (verticalAlpha > 0.005f) {
                for (c in 0..cols) {
                    drawCircle(
                        color = lerp(dotColorBase, accentColor, 0.75f).copy(alpha = verticalAlpha),
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

@Composable
internal fun NowPlayingBar(
    item: TrackItem?,
    isPlaying: Boolean,
    resolving: Boolean,
    error: String?,
    onTogglePlayPause: () -> Unit,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPositioned: (Rect) -> Unit = {},
    onArtPositioned: (Rect) -> Unit = {},
) {
    if (item == null) return
    val context = LocalContext.current
    val scallopedBtn = remember { ScallopedShape(petals = 8, depth = 0.15f) }
    val scallopedArt = remember { ScallopedShape(petals = 10, depth = 0.12f) }

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val isSolid = com.melo.music.settings.AppSettings.solidBackground

    // Извлечение палитры обложки трека через общий кэш TrackColorCache
    val trackColors = TrackColorCache.get(item.thumbnailUrl, isDark)
    LaunchedEffect(item.thumbnailUrl, isDark) {
        val url = item.thumbnailUrl ?: return@LaunchedEffect
        TrackColorCache.extract(context, url, isDark)
    }

    val animatedTrackColor by animateColorAsState(
        targetValue = trackColors.accent,
        animationSpec = tween(500),
        label = "miniPlayerAccentColor",
    )

    val miniBarBg = if (isSolid) {
        trackColors.solidBg
    } else {
        if (isDark) Color(0xEB13110E) else cs.surfaceContainerHigh.copy(alpha = 0.95f)
    }

    val animatedMiniBarBg by animateColorAsState(
        targetValue = miniBarBg,
        animationSpec = tween(450),
        label = "miniBarBg",
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

    val playIntensity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "miniPlayerPlayIntensity",
    )

    val miniBarBorder = if (isDark) {
        BorderStroke(1.2.dp, lerp(Color(0x33FFFFFF), animatedTrackColor, androidx.compose.ui.util.lerp(0.18f, 0.45f, playIntensity)))
    } else {
        BorderStroke(1.2.dp, lerp(cs.outlineVariant.copy(alpha = 0.45f), animatedTrackColor, androidx.compose.ui.util.lerp(0.20f, 0.50f, playIntensity)))
    }

    Surface(
        // Матовое полупрозрачное стекло
        color = animatedMiniBarBg,
        shape = RoundedCornerShape(42.dp),
        shadowElevation = androidx.compose.ui.unit.lerp(8.dp, 18.dp, playIntensity),
        border = miniBarBorder,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInRoot())
            },
    ) {
        Box(
            modifier = Modifier
                .drawMiniPlayerBackdrop(
                    isDark = isDark,
                    dotColorBase = if (isDark) Color(0xFFF5E6CC) else cs.onSurfaceVariant.copy(alpha = 0.6f),
                    bgColor = animatedMiniBarBg,
                    accentColor = animatedTrackColor,
                    playIntensity = playIntensity,
                    phase1 = phase1,
                    phase2 = phase2,
                )
        ) {
            NowPlayingBarInner(
                item = item,
                isPlaying = isPlaying,
                resolving = resolving,
                animatedTrackColor = animatedTrackColor,
                scallopedArt = scallopedArt,
                scallopedBtn = scallopedBtn,
                onTogglePlayPause = onTogglePlayPause,
                onPrev = onPrev,
                onNext = onNext,
                onClick = onClick,
                onArtPositioned = onArtPositioned,
            )
        }
    }
}

