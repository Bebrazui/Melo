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
import androidx.compose.ui.layout.LayoutCoordinates
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.unit.Dp
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
internal fun FullPlayer(
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
    bassBoost: Boolean,
    onSetBassBoost: (Boolean) -> Unit,
    crushAmount: Float,
    onSetCrush: (Float) -> Unit,
    pitchSemi: Float,
    onSetPitchSemi: (Float) -> Unit,
    noteShiftSeed: Long,
    onRandomizeNotes: () -> Unit,
    onResetAudioFx: () -> Unit,
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
    miniPlayerBounds: Rect? = null,
    copyrightBlocked: Boolean = false,
    onFindSimilar: () -> Unit = {},
    onDismissCopyright: () -> Unit = {},
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

    // ── Плавная физическая трансформация полного плеера в мини-плеер ──
    val localDensity = LocalDensity.current
    val navBarBottomInset = 78.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val miniBarBottomOffset = navBarBottomInset + 10.dp
    val miniBarHeight = 72.dp

    val scallopedBtn = remember { ScallopedShape(petals = 8, depth = 0.15f) }
    val scallopedArt = remember { ScallopedShape(petals = 10, depth = 0.12f) }

    val gestureScope = rememberCoroutineScope()
    val collapseProgress = remember { Animatable(1f) }
    var isCollapsing by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showFxSheet by remember { mutableStateOf(false) }
    var fullArtBounds by remember { mutableStateOf<Rect?>(null) }
    var fullControlsCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(Unit) {
        collapseProgress.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 360f,
            ),
        )
    }

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

    val requestCollapse: () -> Unit = {
        if (!isCollapsing) {
            isCollapsing = true
            showLyrics = false
            gestureScope.launch {
                collapseProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 260,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing,
                    ),
                )
                onCollapse()
            }
        }
    }

    BackHandler(onBack = {
        if (isFullscreenVideo) {
            isFullscreenVideo = false
        } else if (showSpeed) {
            showSpeed = false
        } else {
            requestCollapse()
        }
    })

    // Читаем позицию/длительность только здесь → рекомпозится лишь плеер, не главная.
    val position by positionState
    val duration by durationState

    var dragFraction by remember { mutableStateOf<Float?>(null) }

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

    // Lottie анимация сердечка (Like.json)
    val likeComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.melo.music.R.raw.like_anim))
    val likeAnimProgress = remember { Animatable(if (isLiked) 1f else 0f) }
    LaunchedEffect(isLiked) {
        if (isLiked) {
            likeAnimProgress.snapTo(0f)
            likeAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = LinearEasing),
            )
        } else {
            likeAnimProgress.snapTo(0f)
        }
    }
    // ── Извлечение палитры обложки для кнопок Material 3 Expressive и сплошного фона ──
    val playerThemeCs = MaterialTheme.colorScheme
    val playerIsDark = playerThemeCs.background.luminance() < 0.5f

    val trackColors = TrackColorCache.get(item.thumbnailUrl, playerIsDark)
    LaunchedEffect(item.thumbnailUrl, playerIsDark) {
        val url = item.thumbnailUrl ?: return@LaunchedEffect
        TrackColorCache.extract(playerContext, url, playerIsDark)
    }

    val artColor by animateColorAsState(
        targetValue = trackColors.accent,
        animationSpec = tween(500),
        label = "artColor",
    )

    val animatedPlayerSolidBg by animateColorAsState(
        targetValue = trackColors.solidBg,
        animationSpec = tween(450),
        label = "animatedPlayerSolidBg",
    )

    val playBtnColor by animateColorAsState(
        targetValue = if (artColor.luminance() < 0.2f) lerp(artColor, Color.White, 0.65f) else lerp(artColor, Color.White, 0.42f),
        animationSpec = tween(500),
        label = "playBtnColor",
    )
    val sideBtnColor by animateColorAsState(
        targetValue = if (artColor.luminance() < 0.2f) lerp(artColor, Color.White, 0.50f) else lerp(artColor, Color.White, 0.26f),
        animationSpec = tween(500),
        label = "sideBtnColor",
    )
    val iconTint by animateColorAsState(
        targetValue = if (playBtnColor.luminance() > 0.22f) Color(0xFF0F1E28) else Color.White,
        animationSpec = tween(500),
        label = "iconTint",
    )

    // Анимация вытягивания и сжатия кнопок управления при нажатии
    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playBounceAnim = remember { Animatable(1f) }

    val prevInteractionSource = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteractionSource.collectIsPressedAsState()
    val prevBounceAnim = remember { Animatable(1f) }

    val nextInteractionSource = remember { MutableInteractionSource() }
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()
    val nextBounceAnim = remember { Animatable(1f) }

    // Динамический масштаб по X: зажатая кнопка сильнее вытягивается (до 1.46x), а соседние сжимаются (до 0.82x-0.90x)
    val prevTargetScale = when {
        isPrevPressed -> 1.46f
        isPlayPressed -> 0.82f
        isNextPressed -> 0.92f
        else -> prevBounceAnim.value
    }
    val prevScaleX by animateFloatAsState(
        targetValue = prevTargetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "prevScaleX",
    )

    val playTargetScale = when {
        isPlayPressed -> 1.40f
        isPrevPressed -> 0.80f
        isNextPressed -> 0.80f
        else -> playBounceAnim.value
    }
    val playScaleX by animateFloatAsState(
        targetValue = playTargetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "playScaleX",
    )

    val nextTargetScale = when {
        isNextPressed -> 1.46f
        isPlayPressed -> 0.82f
        isPrevPressed -> 0.92f
        else -> nextBounceAnim.value
    }
    val nextScaleX by animateFloatAsState(
        targetValue = nextTargetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nextScaleX",
    )

    // Динамическое скругление: пока кнопка зажата — скругление слегка уменьшается
    val prevCornerRadius by animateDpAsState(
        targetValue = if (isPrevPressed) 18.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "prevCornerRadius",
    )
    val playCornerRadius by animateDpAsState(
        targetValue = if (isPlayPressed) 16.dp else 26.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "playCornerRadius",
    )
    val nextCornerRadius by animateDpAsState(
        targetValue = if (isNextPressed) 18.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nextCornerRadius",
    )

    // ── 3D-эффект наклона устройства (гироскоп / акселерометр) ──
    val (tiltRoll, tiltPitch) = rememberDeviceTilt()

    var swipeAccum by remember { mutableStateOf(0f) }
    val swipeX = remember { Animatable(0f) }
    // 0 = ось ещё не выбрана, 1 = вертикаль, 2 = горизонталь.
    var axis by remember { mutableIntStateOf(0) }
    var trackSwitchDirection by remember { mutableIntStateOf(1) }

    var speedWasOpenAtDragStart by remember { mutableStateOf(false) }

    // Анимация волн и свечения мини-плеера при сворачивании
    val miniInfinite = rememberInfiniteTransition(label = "miniPlayerFluidDrift")
    val miniPhase1 by miniInfinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "miniPhase1",
    )
    val miniPhase2 by miniInfinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "miniPhase2",
    )
    val miniPlayIntensity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "miniPlayIntensity",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullScreenWidthDp = maxWidth
        val fullScreenHeightDp = maxHeight
        val screenWidthPx = with(localDensity) { maxWidth.toPx() }
        val screenHeightPx = with(localDensity) { maxHeight.toPx() }

        val isWide = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isCompactScreen = !isWide && fullScreenHeightDp < 750.dp
        val isVinylMode = com.melo.music.settings.AppSettings.vinylRecord

        val art3dModifier = Modifier
            .graphicsLayer {
                rotationY = tiltRoll * 5f
                rotationX = -tiltPitch * 5f
                translationX = tiltRoll * 4.dp.toPx()
                translationY = tiltPitch * 4.dp.toPx()
                cameraDistance = 20f * density
                shadowElevation = (10f + (kotlin.math.abs(tiltRoll) + kotlin.math.abs(tiltPitch)) * 2f).dp.toPx()
                shape = if (isVinylMode) CircleShape else RoundedCornerShape(32.dp)
                clip = isVinylMode
            }

        val p = collapseProgress.value.coerceIn(0f, 1f)

        // ── Компонент обложки или видеоклипа ──
        @Composable
        fun RenderArtwork(
            boxModifier: Modifier,
            shape: Shape = RoundedCornerShape(32.dp),
            border: BorderStroke? = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.12f)),
            elevation: Dp = 12.dp,
            forceStaticArt: Boolean = false,
        ) {
            if (showVideo && videoUrl != null) {
                Surface(
                    shape = shape,
                    color = Color.Black,
                    border = border,
                    shadowElevation = elevation,
                    modifier = boxModifier
                        .then(art3dModifier)
                        .clip(shape),
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
            } else if (forceStaticArt) {
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = border,
                    shadowElevation = elevation,
                    modifier = boxModifier.clip(shape),
                ) {
                    val context = LocalContext.current
                    val request = remember(hiRes) {
                        coil.request.ImageRequest.Builder(context)
                            .data(hiRes)
                            .memoryCacheKey(hiRes)
                            .diskCacheKey(hiRes)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                androidx.compose.animation.AnimatedContent(
                    targetState = Triple(item.url, hiRes, com.melo.music.settings.AppSettings.vinylRecord),
                    transitionSpec = {
                        val dir = trackSwitchDirection
                        (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { width -> (width * 0.35f * dir).toInt() } + fadeIn(tween(260)))
                            .togetherWith(slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { width -> (-width * 0.35f * dir).toInt() } + fadeOut(tween(200)))
                    },
                    label = "artTrackTransition",
                    modifier = boxModifier.then(art3dModifier).clipToBounds(),
                ) { (_, art, isVinyl) ->
                    if (isVinyl) {
                        VinylRecordView(
                            art = art,
                            isPlaying = isPlaying,
                            tiltRoll = tiltRoll,
                            tiltPitch = tiltPitch,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Surface(
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = border,
                            shadowElevation = elevation,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape),
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val context = LocalContext.current
                                val request = remember(art) {
                                    coil.request.ImageRequest.Builder(context)
                                        .data(art)
                                        .crossfade(true)
                                        .crossfade(250)
                                        .build()
                                }
                                AsyncImage(
                                    model = request,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )

                                // Едва заметный, мягкий круглый блик света
                                if (!com.melo.music.settings.AppSettings.solidBackground && p < 0.35f) {
                                    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 1f - (p / 0.35f) }) {
                                        val sheenAlpha = (0.10f + (kotlin.math.abs(tiltRoll) + kotlin.math.abs(tiltPitch)) * 0.04f).coerceIn(0.02f, 0.14f)
                                        val cx = size.width * (0.45f + tiltRoll * 0.25f)
                                        val cy = size.height * (0.40f + tiltPitch * 0.25f)
                                        val maxDim = kotlin.math.max(size.width, size.height)

                                        drawRect(
                                            brush = Brush.radialGradient(
                                                0.0f to Color.White.copy(alpha = sheenAlpha),
                                                0.35f to Color.White.copy(alpha = sheenAlpha * 0.45f),
                                                0.70f to Color.White.copy(alpha = sheenAlpha * 0.10f),
                                                1.0f to Color.Transparent,
                                                center = Offset(cx, cy),
                                                radius = maxDim * 0.95f,
                                            ),
                                            size = size,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val targetX = miniPlayerBounds?.left ?: with(localDensity) { 14.dp.toPx() }
        val targetY = miniPlayerBounds?.top ?: (screenHeightPx - with(localDensity) { 160.dp.toPx() })
        val targetW = miniPlayerBounds?.width ?: (screenWidthPx - with(localDensity) { 28.dp.toPx() })
        val targetH = miniPlayerBounds?.height ?: with(localDensity) { 72.dp.toPx() }

        val currentX = androidx.compose.ui.util.lerp(0f, targetX, p)
        val currentY = androidx.compose.ui.util.lerp(0f, targetY, p)
        val currentW = androidx.compose.ui.util.lerp(screenWidthPx, targetW, p)
        val currentH = androidx.compose.ui.util.lerp(screenHeightPx, targetH, p)
        val currentCorner = androidx.compose.ui.unit.lerp(0.dp, 42.dp, p)
        val currentElevation = androidx.compose.ui.unit.lerp(0.dp, 20.dp, p)

        val fullControlsAlpha = (1f - (p / 0.35f)).coerceIn(0f, 1f)
        val miniControlsAlpha = ((p - 0.65f) / 0.35f).coerceIn(0f, 1f)

        val topSpacerHeight = if (isCompactScreen) 14.dp else 36.dp
        val targetArtFraction = when {
            isCompactScreen && showSpeed -> 0.55f
            isCompactScreen -> 0.74f
            showSpeed -> 0.68f
            else -> 0.88f
        }
        val animatedArtFraction by animateFloatAsState(
            targetValue = targetArtFraction,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
            label = "artFraction",
        )
        val artFullWidthDp = fullScreenWidthDp * animatedArtFraction
        val fullArtSizePx = with(localDensity) { artFullWidthDp.toPx() }
        val fallbackX = (screenWidthPx - fullArtSizePx) / 2f
        val fallbackY = with(localDensity) { (topSpacerHeight + 48.dp + (if (isCompactScreen) 6.dp else 20.dp)).toPx() }

        val fullArtX = fullArtBounds?.left ?: fallbackX
        val fullArtY = fullArtBounds?.top ?: fallbackY
        val fullArtSize = fullArtBounds?.width ?: fullArtSizePx

        val miniArtSizePx = with(localDensity) { 52.dp.toPx() }
        val miniArtX = with(localDensity) { 13.dp.toPx() }
        val miniArtY = with(localDensity) { 11.dp.toPx() }

        val artSizePx = androidx.compose.ui.util.lerp(fullArtSize, miniArtSizePx, p)
        val artX = androidx.compose.ui.util.lerp(fullArtX, miniArtX, p)
        val artY = androidx.compose.ui.util.lerp(fullArtY, miniArtY, p)
        val artCorner = androidx.compose.ui.unit.lerp(32.dp, 26.dp, p)

        // Мягкое затемнение фона под выдвигающейся карточкой
        val scrimAlpha = (1f - p).coerceIn(0f, 1f) * 0.55f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        ) {
            val fullCs = MaterialTheme.colorScheme
            val fullIsDark = fullCs.background.luminance() < 0.5f
            val isSolid = com.melo.music.settings.AppSettings.solidBackground

            val fullPlayerBg = if (isSolid) {
                animatedPlayerSolidBg
            } else {
                if (fullIsDark) Color(0xEB13110E) else fullCs.surfaceContainerHigh.copy(alpha = 0.95f)
            }

            val targetMiniBarBg = if (isSolid) {
                trackColors.solidBg
            } else {
                if (fullIsDark) Color(0xEB13110E) else fullCs.surfaceContainerHigh.copy(alpha = 0.95f)
            }

            val collapseBg = lerp(fullPlayerBg, targetMiniBarBg, p)

            val miniBorderColor = if (fullIsDark) {
                lerp(Color(0x33FFFFFF), artColor, androidx.compose.ui.util.lerp(0.18f, 0.45f, miniPlayIntensity))
            } else {
                lerp(fullCs.outlineVariant.copy(alpha = 0.45f), artColor, androidx.compose.ui.util.lerp(0.20f, 0.50f, miniPlayIntensity))
            }
            val borderAlpha = (p / 0.30f).coerceIn(0f, 1f)
            val collapseBorder = if (borderAlpha > 0.01f) {
                BorderStroke(1.2.dp, miniBorderColor.copy(alpha = miniBorderColor.alpha * borderAlpha))
            } else null

            Surface(
                shape = RoundedCornerShape(currentCorner),
                color = collapseBg,
                shadowElevation = if (isSolid) 0.dp else currentElevation,
                border = collapseBorder,
                modifier = Modifier
                    .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                    .size(
                        width = with(localDensity) { currentW.toDp() },
                        height = with(localDensity) { currentH.toDp() },
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                swipeAccum = 0f
                                axis = 0
                                speedWasOpenAtDragStart = showSpeed
                            },
                            onDragCancel = {
                                if (speedWasOpenAtDragStart) {
                                    if (showSpeed && swipeAccum >= 35f) {
                                        showSpeed = false
                                        ClickFeedback.play()
                                    }
                                    gestureScope.launch {
                                        collapseProgress.snapTo(0f)
                                        swipeX.animateTo(0f, tween(180))
                                    }
                                } else if (axis == 1) {
                                    if (swipeAccum <= -45f) {
                                        showSpeed = true
                                        ClickFeedback.play()
                                    } else {
                                        val dragDistance = (targetY - 0f).coerceAtLeast(200f)
                                        val progress = (swipeAccum / dragDistance).coerceIn(0f, 1f)
                                        if (progress > 0.28f || swipeAccum > 180f) {
                                            requestCollapse()
                                        } else {
                                            gestureScope.launch {
                                                collapseProgress.animateTo(0f, spring(0.85f, 400f))
                                                swipeX.animateTo(0f, tween(180))
                                            }
                                        }
                                    }
                                }
                                swipeAccum = 0f
                                axis = 0
                                speedWasOpenAtDragStart = false
                            },
                            onDragEnd = {
                                if (axis == 2) {
                                    val v = swipeX.value
                                    val w = size.width.toFloat()
                                    gestureScope.launch {
                                        when {
                                            v > w * 0.20f -> {
                                                trackSwitchDirection = -1
                                                swipeX.animateTo(w * 0.45f, tween(120))
                                                swipeX.snapTo(0f)
                                                onPrev()
                                            }
                                            v < -w * 0.20f -> {
                                                trackSwitchDirection = 1
                                                swipeX.animateTo(-w * 0.45f, tween(120))
                                                swipeX.snapTo(0f)
                                                onNext()
                                            }
                                            else -> swipeX.animateTo(0f, tween(200))
                                        }
                                    }
                                } else if (speedWasOpenAtDragStart) {
                                    // Жест начался при открытом меню скоростей: закрывает ТОЛЬКО скорости, плеер остаётся на месте
                                    if (showSpeed && swipeAccum >= 30f) {
                                        showSpeed = false
                                        ClickFeedback.play()
                                    }
                                    gestureScope.launch {
                                        collapseProgress.animateTo(0f, spring(0.85f, 400f))
                                    }
                                } else {
                                    if (swipeAccum <= -45f) {
                                        showSpeed = true
                                        ClickFeedback.play()
                                        gestureScope.launch { collapseProgress.animateTo(0f, spring(0.85f, 400f)) }
                                    } else {
                                        val dragDistance = (targetY - 0f).coerceAtLeast(200f)
                                        val progress = (swipeAccum / dragDistance).coerceIn(0f, 1f)
                                        if (progress > 0.28f || swipeAccum > 180f) {
                                            requestCollapse()
                                        } else {
                                            gestureScope.launch {
                                                collapseProgress.animateTo(0f, spring(0.85f, 400f))
                                            }
                                        }
                                    }
                                }
                                swipeAccum = 0f
                                axis = 0
                                speedWasOpenAtDragStart = false
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
                                    if (speedWasOpenAtDragStart) {
                                        if (showSpeed && swipeAccum >= 40f) {
                                            showSpeed = false
                                            ClickFeedback.play()
                                        }
                                        if (collapseProgress.value != 0f) {
                                            gestureScope.launch { collapseProgress.snapTo(0f) }
                                        }
                                    } else {
                                        if (swipeAccum <= -45f && !showSpeed) {
                                            showSpeed = true
                                            swipeAccum = 0f
                                            ClickFeedback.play()
                                        } else if (swipeAccum > 0f) {
                                            val dragDistance = (targetY - 0f).coerceAtLeast(200f)
                                            val newProgress = (swipeAccum / dragDistance).coerceIn(0f, 1f)
                                            gestureScope.launch { collapseProgress.snapTo(newProgress) }
                                        }
                                    }
                                }
                            },
                        )
                    },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (fullControlsAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .size(width = fullScreenWidthDp, height = fullScreenHeightDp)
                                .graphicsLayer { alpha = fullControlsAlpha }
                                .onGloballyPositioned { fullControlsCoordinates = it },
                        ) {
        if (com.melo.music.settings.AppSettings.solidBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedPlayerSolidBg)
            )
        } else {
            FlowingBackground(
                thumbnailUrl = hiRes,
                audioSessionId = audioSessionId,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Контр-параллакс фона при наклоне устройства для создания ощущения глубины
                        translationX = -tiltRoll * 4.dp.toPx()
                        translationY = -tiltPitch * 4.dp.toPx()
                        scaleX = 1.03f
                        scaleY = 1.03f
                    },
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
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = isCompactScreen

            // ── Компонент текста песни / караоке ──
            @Composable
            fun RenderLyrics(boxModifier: Modifier) {
                val lyr = lyrics
                when {
                    lyricsLoading -> Box(
                        modifier = boxModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator(color = white, modifier = Modifier.size(36.dp))
                    }

                    lyr == null || lyr.isEmpty -> Box(
                        modifier = boxModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { Text("Текст не найден", color = whiteDim, textAlign = TextAlign.Center) }

                    lyr.isSynced -> SyncedLyrics(
                        lines = lyr.lines!!,
                        positionMs = position,
                        onSeek = onSeek,
                        white = white,
                        dim = whiteDim,
                        modifier = boxModifier.fillMaxSize(),
                    )

                    else -> Column(
                        modifier = boxModifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = lyr.plain ?: "",
                            color = white,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                        )
                    }
                }
            }

            // ── Компонент управления (название, слайдер, кнопки, капсула) с 3D-параллаксом ──
            @Composable
            fun RenderControls(isLandscape: Boolean) {
                // 3. Заголовок трека, исполнитель и кнопка Текста (3D-параллакс)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = tiltRoll * 2.5.dp.toPx()
                            translationY = tiltPitch * 2.5.dp.toPx()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = Pair(item.title, item.uploader),
                        transitionSpec = {
                            val dir = trackSwitchDirection
                            (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { width -> (width * 0.25f * dir).toInt() } + fadeIn(tween(260)))
                                .togetherWith(slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { width -> (-width * 0.25f * dir).toInt() } + fadeOut(tween(200)))
                        },
                        label = "titleTrackTransition",
                        modifier = Modifier.weight(1f).padding(end = 10.dp),
                    ) { (trackTitle, artistName) ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = trackTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = if (isLandscape) 20.sp else 24.sp,
                                    lineHeight = if (isLandscape) 24.sp else 28.sp,
                                    letterSpacing = (-0.4).sp,
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = white,
                                maxLines = if (isLandscape) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            artistName?.let { name ->
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = name.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = if (isLandscape) 13.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                    ),
                                    color = whiteDim,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable {
                                        val artistItem = TrackItem(
                                            title = name,
                                            uploader = name,
                                            url = "artist:$name",
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
                    }

                    // Кнопка Текста (Lyrics) в форме округлой диагональной гальки (pebble) без обводки (непрозрачная)
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 48.dp else 56.dp)
                            .graphicsLayer {
                                rotationY = tiltRoll * 3.5f
                                rotationX = -tiltPitch * 3.5f
                                translationX = tiltRoll * 1.5.dp.toPx()
                                translationY = tiltPitch * 1.5.dp.toPx()
                                cameraDistance = 20f * density
                            }
                            .clickable { showLyrics = !showLyrics },
                        contentAlignment = Alignment.Center,
                    ) {
                        // Наклонная округлая галька (без обводки, сплошная заливка)
                        Box(
                            modifier = Modifier
                                .width(if (isLandscape) 48.dp else 54.dp)
                                .height(if (isLandscape) 38.dp else 44.dp)
                                .graphicsLayer { rotationZ = -40f }
                                .clip(RoundedCornerShape(22.dp))
                                .background(playBtnColor),
                        )
                        // Прямая (не повернутая) иконка поверх наклонной гальки
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Текст песни",
                            tint = iconTint,
                            modifier = Modifier.size(if (isLandscape) 20.dp else 22.dp),
                        )
                    }
                }

                // Плашка: Трек удалён правообладателем (в полном плеере)
                androidx.compose.animation.AnimatedVisibility(
                    visible = copyrightBlocked,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    CopyrightBlockedBanner(
                        track = item,
                        onFindSimilar = onFindSimilar,
                        onDismiss = onDismissCopyright,
                    )
                }

                if (!isLandscape) {
                    Spacer(Modifier.height(if (isCompact) 8.dp else 18.dp))
                }

                // 4. Wavy/Squiggly Прогресс-бар с 3D-параллаксом
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = tiltRoll * 1.8.dp.toPx()
                            translationY = tiltPitch * 1.8.dp.toPx()
                        },
                ) {
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
                            modifier = Modifier.height(24.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp),
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
                }

                if (!isLandscape) {
                    Spacer(Modifier.height(if (isCompact) 10.dp else 20.dp))
                }

                // 5. Главный блок воспроизведения: асимметричный Expressive Shape Trio в цвет обложки с 3D
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationY = tiltRoll * 3.5f
                            rotationX = -tiltPitch * 3.5f
                            translationX = tiltRoll * 2.dp.toPx()
                            translationY = tiltPitch * 2.dp.toPx()
                            cameraDistance = 20f * density
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Предыдущий трек: сквиркл в цвет обложки с эффектом вытягивания при нажатии
                    Surface(
                        shape = RoundedCornerShape(prevCornerRadius),
                        color = sideBtnColor,
                        modifier = Modifier
                            .width((if (isLandscape || isCompact) 56.dp else 66.dp) * prevScaleX)
                            .height(if (isLandscape || isCompact) 56.dp else 66.dp)
                            .clip(RoundedCornerShape(prevCornerRadius))
                            .clickable(
                                interactionSource = prevInteractionSource,
                                indication = ripple(bounded = true),
                                onClick = {
                                    gestureScope.launch {
                                        prevBounceAnim.snapTo(1.25f)
                                        prevBounceAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                        )
                                    }
                                    ClickFeedback.play()
                                    trackSwitchDirection = -1
                                    onPrev()
                                },
                            ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.SkipPrevious,
                                contentDescription = "Назад",
                                tint = iconTint,
                                modifier = Modifier.size(if (isLandscape || isCompact) 28.dp else 32.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(if (isLandscape) 8.dp else 10.dp))

                    // Play / Pause: сквиркл в цвет обложки с вытягиванием в длину
                    Surface(
                        shape = RoundedCornerShape(playCornerRadius),
                        color = playBtnColor,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .width((if (isLandscape) 84.dp else if (isCompact) 88.dp else 96.dp) * playScaleX)
                            .height(if (isLandscape || isCompact) 62.dp else 74.dp)
                            .clip(RoundedCornerShape(playCornerRadius))
                            .clickable(
                                interactionSource = playInteractionSource,
                                indication = ripple(bounded = true),
                                onClick = {
                                    gestureScope.launch {
                                        playBounceAnim.snapTo(1.25f)
                                        playBounceAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                        )
                                    }
                                    ClickFeedback.play()
                                    onTogglePlayPause()
                                },
                            ),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = resolving,
                                enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                                exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                            ) {
                                LoadingIndicator(
                                    modifier = Modifier.size(if (isLandscape || isCompact) 26.dp else 30.dp),
                                    color = iconTint,
                                )
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !resolving,
                                enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                                exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                            ) {
                                AnimatedContent(targetState = isPlaying, label = "playPause") { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (playing) "Пауза" else "Играть",
                                        tint = iconTint,
                                        modifier = Modifier.size(if (isLandscape || isCompact) 34.dp else 40.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(if (isLandscape) 8.dp else 10.dp))

                    // Следующий трек: сквиркл в цвет обложки с эффектом вытягивания при нажатии
                    Surface(
                        shape = RoundedCornerShape(nextCornerRadius),
                        color = sideBtnColor,
                        modifier = Modifier
                            .width((if (isLandscape || isCompact) 56.dp else 66.dp) * nextScaleX)
                            .height(if (isLandscape || isCompact) 56.dp else 66.dp)
                            .clip(RoundedCornerShape(nextCornerRadius))
                            .clickable(
                                interactionSource = nextInteractionSource,
                                indication = ripple(bounded = true),
                                onClick = {
                                    gestureScope.launch {
                                        nextBounceAnim.snapTo(1.25f)
                                        nextBounceAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                        )
                                    }
                                    ClickFeedback.play()
                                    trackSwitchDirection = 1
                                    onNext()
                                },
                            ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.SkipNext,
                                contentDescription = "Вперёд",
                                tint = iconTint,
                                modifier = Modifier.size(if (isLandscape || isCompact) 28.dp else 32.dp),
                            )
                        }
                    }
                }

                if (!isLandscape) {
                    Spacer(Modifier.height(if (isCompact) 10.dp else 20.dp))
                }

                // 6. Нижний сегментированный остров действий с 3D-параллаксом
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.Black.copy(alpha = 0.38f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth(if (isLandscape) 0.95f else 0.88f)
                        .height(if (isLandscape) 52.dp else if (isCompact) 50.dp else 60.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .graphicsLayer {
                            rotationY = tiltRoll * 2.5f
                            rotationX = -tiltPitch * 2.5f
                            translationX = tiltRoll * 1.2.dp.toPx()
                            translationY = tiltPitch * 1.2.dp.toPx()
                            cameraDistance = 20f * density
                        },
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = "Перемешать",
                                tint = if (shuffle) accent else whiteDim,
                                modifier = Modifier.size(if (isLandscape || isCompact) 20.dp else 24.dp),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )

                        IconButton(onClick = onToggleRepeat) {
                            Icon(
                                imageVector = if (repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                contentDescription = "Повтор",
                                tint = if (repeatOne) accent else whiteDim,
                                modifier = Modifier.size(if (isLandscape || isCompact) 20.dp else 24.dp),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )

                        IconButton(
                            onClick = onToggleLike,
                            modifier = Modifier.size(if (isLandscape || isCompact) 44.dp else 56.dp),
                        ) {
                            if (isLiked) {
                                LottieAnimation(
                                    composition = likeComposition,
                                    progress = { likeAnimProgress.value },
                                    modifier = Modifier
                                        .size(if (isLandscape) 52.dp else 66.dp)
                                        .scale(1.4f),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Нравится",
                                    tint = white,
                                    modifier = Modifier
                                        .size(if (isLandscape) 26.dp else 30.dp)
                                        .scale(likeScale),
                                )
                            }
                        }
                    }
                }
            }

            // ── Кнопка верхнего бара с 3D-объёмом и световым бликом ──
            @Composable
            fun TopBarCircleButton(
                onClick: () -> Unit,
                modifier: Modifier = Modifier,
                isActive: Boolean = false,
                enabled: Boolean = true,
                isLandscape: Boolean = false,
                content: @Composable () -> Unit,
            ) {
                val buttonSize = if (isLandscape) 36.dp else 42.dp
                val baseModifier = if (isActive) {
                    // Активная кнопка: полностью непрозрачная, залита цветом темы, без обводки
                    Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(playBtnColor)
                } else {
                    // Неактивная кнопка: деликатная полупрозрачность со слабым мягким верхним бликом
                    Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))
                            )
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f))
                                )
                            ),
                            CircleShape,
                        )
                }

                Box(
                    modifier = modifier
                        .then(baseModifier)
                        .clickable(enabled = enabled, onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    content()
                }
            }

            // ── Верхний бар действий ──
            @Composable
            fun RenderTopBar(isLandscape: Boolean) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(if (isLandscape) 40.dp else 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TopBarCircleButton(
                        onClick = requestCollapse,
                        isLandscape = isLandscape,
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Свернуть",
                            tint = white,
                            modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                ClickFeedback.play()
                                showSpeed = !showSpeed
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                text = if (showLyrics) "ТЕКСТ" else "СЕЙЧАС",
                                fontFamily = FoglihtenFontFamily,
                                fontSize = if (isLandscape) 14.sp else 16.sp,
                                letterSpacing = 1.2.sp,
                                lineHeight = if (isLandscape) 13.5.sp else 15.5.sp,
                                color = white,
                                maxLines = 1,
                                modifier = if (!showLyrics) Modifier.offset(x = (-8).dp) else Modifier,
                            )
                            Text(
                                text = if (showLyrics) "ПЕСНИ" else "ИГРАЕТ",
                                fontFamily = FoglihtenFontFamily,
                                fontSize = if (isLandscape) 14.sp else 16.sp,
                                letterSpacing = 1.2.sp,
                                lineHeight = if (isLandscape) 13.5.sp else 15.5.sp,
                                color = white,
                                maxLines = 1,
                                modifier = if (!showLyrics) Modifier.offset(x = 16.dp, y = (-5).dp) else Modifier.offset(x = 10.dp),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val vocalCut = com.melo.music.audio.VocalCutManager.isEnabled
                        TopBarCircleButton(
                            onClick = { com.melo.music.audio.VocalCutManager.toggle() },
                            isActive = vocalCut,
                            isLandscape = isLandscape,
                        ) {
                            Icon(
                                imageVector = if (vocalCut) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = if (vocalCut) "Вокал выключен (Караоке)" else "Убрать вокал",
                                tint = if (vocalCut) iconTint else white,
                                modifier = Modifier.size(if (isLandscape) 18.dp else 20.dp),
                            )
                        }

                        TopBarCircleButton(
                            onClick = onShowQueue,
                            isLandscape = isLandscape,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Очередь",
                                tint = white,
                                modifier = Modifier.size(if (isLandscape) 18.dp else 20.dp),
                            )
                        }

                        if (videoUrl != null || videoLoading) {
                            TopBarCircleButton(
                                onClick = {
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
                                isActive = showVideo,
                                enabled = !videoLoading && videoUrl != null,
                                isLandscape = isLandscape,
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = videoLoading,
                                    enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                                    exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                                ) {
                                    LoadingIndicator(
                                        color = if (showVideo) iconTint else white,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !videoLoading,
                                    enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                                    exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                                ) {
                                    Icon(
                                        Icons.Rounded.Videocam,
                                        contentDescription = "Клип",
                                        tint = if (showVideo) iconTint else white,
                                        modifier = Modifier.size(if (isLandscape) 18.dp else 20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── РАСКЛАДКА: Горизонтальный режим (автомагнитолы, планшеты, ландшафт) ──
            if (isWide) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    RenderTopBar(isLandscape = true)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Левая колонка: обложка 3D (или караоке/видео)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (showLyrics) {
                                RenderLyrics(Modifier.fillMaxSize())
                            } else {
                                RenderArtwork(Modifier.fillMaxHeight(0.92f).aspectRatio(1f))
                            }
                        }

                        Spacer(Modifier.width(20.dp))

                        // Правая колонка: управление воспроизведением
                        Column(
                            modifier = Modifier
                                .weight(1.35f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showSpeed,
                                enter = expandVertically(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)) + fadeIn(tween(200)),
                                exit = shrinkVertically(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)) + fadeOut(tween(160)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 10.dp),
                                ) {
                                    ExpressiveSpeedButtonGroup(
                                        speed = speed,
                                        onSetSpeed = onSetSpeed,
                                        hasActiveFx = bassBoost || crushAmount > 0.01f || kotlin.math.abs(pitchSemi) > 0.01f || noteShiftSeed != 0L,
                                        onOpenFxPanel = { showFxSheet = true },
                                        accent = artColor,
                                    )
                                }
                            }

                            RenderControls(isLandscape = true)
                        }
                    }
                }
            } else {
                // ── РАСКЛАДКА: Вертикальный режим (Portrait) ──
                val topSpacerHeight = if (isCompact) 14.dp else 36.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (isCompact) 18.dp else 24.dp,
                            vertical = if (isCompact) 4.dp else 12.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(topSpacerHeight))
                    RenderTopBar(isLandscape = false)

                    if (showLyrics) {
                        // Режим текста: отдаём тексту максимум свободного пространства
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            RenderLyrics(Modifier.fillMaxSize())
                        }
                    } else {
                        // Режим обложки: место под морфирующуюся обложку
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = if (isCompactScreen) 2.dp else 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                val targetArtDp = minOf(fullScreenWidthDp * animatedArtFraction, maxHeight)
                                Spacer(
                                    modifier = Modifier
                                        .size(targetArtDp)
                                        .onGloballyPositioned { spacerCoords ->
                                            val parentCoords = fullControlsCoordinates
                                            if (parentCoords != null && parentCoords.isAttached) {
                                                val pos = parentCoords.localPositionOf(spacerCoords, Offset.Zero)
                                                fullArtBounds = Rect(
                                                    pos.x,
                                                    pos.y,
                                                    pos.x + spacerCoords.size.width,
                                                    pos.y + spacerCoords.size.height,
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }

                    // ── M3 Expressive ButtonGroup: ПОД ОБЛОЖКОЙ, НАД НАДПИСЬЮ ──
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSpeed,
                        enter = expandVertically(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)) + fadeIn(tween(200)),
                        exit = shrinkVertically(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)) + fadeOut(tween(160)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = if (isCompact) 6.dp else 12.dp),
                        ) {
                            ExpressiveSpeedButtonGroup(
                                speed = speed,
                                onSetSpeed = onSetSpeed,
                                hasActiveFx = bassBoost || crushAmount > 0.01f || kotlin.math.abs(pitchSemi) > 0.01f || noteShiftSeed != 0L,
                                onOpenFxPanel = { showFxSheet = true },
                                accent = artColor,
                            )
                        }
                    }

                    RenderControls(isLandscape = false)

                    Spacer(Modifier.height(if (isCompact) 4.dp else 8.dp))
                    SleepTimerControl(white = white, accent = accent)

                    error?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Плеер: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Комфортный отступ снизу без лишней пустоты
                    Spacer(Modifier.height(if (isCompact) 4.dp else 14.dp))
                }
            }

            // ── Атмосферный медленно движущийся туман (наплывает на кнопки и низ экрана) ──
            if (!com.melo.music.settings.AppSettings.solidBackground) {
                AtmosphericMistLayer(
                    mistColor = lerp(playBtnColor, Color.White, 0.35f),
                    tiltRoll = tiltRoll,
                    tiltPitch = tiltPitch,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

        // 1.5. Фоновый слой волн и точек мини-плеера (за обложкой)
        if (miniControlsAlpha > 0.01f && !isSolid) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = miniControlsAlpha }
                    .drawMiniPlayerBackdrop(
                        isDark = fullIsDark,
                        dotColorBase = if (fullIsDark) Color(0xFFF5E6CC) else fullCs.onSurfaceVariant.copy(alpha = 0.6f),
                        bgColor = Color.Transparent,
                        accentColor = artColor,
                        playIntensity = miniPlayIntensity,
                        phase1 = miniPhase1,
                        phase2 = miniPhase2,
                    ),
            )
        }

        // 2. Морфирующаяся независимая обложка трека (shared element)
        // Рисуется ПОВЕРХ фонового слоя мини-плеера, непрозрачна 100%!
        if (!isWide && !showLyrics) {
            val morphProgress = ((p - 0.25f) / 0.70f).coerceIn(0f, 1f)
            val currentShape = when {
                isVinylMode -> {
                    if (morphProgress > 0.05f) {
                        ScallopedShape(petals = 10, depth = 0.12f * ((morphProgress - 0.05f) / 0.95f))
                    } else {
                        CircleShape
                    }
                }
                morphProgress > 0.20f -> {
                    val scallopedDepth = 0.12f * ((morphProgress - 0.20f) / 0.80f)
                    ScallopedShape(petals = 10, depth = scallopedDepth)
                }
                else -> {
                    RoundedCornerShape(artCorner)
                }
            }

            val artElevation = androidx.compose.ui.unit.lerp(12.dp, 3.dp, p)
            val currentArtBorder = BorderStroke(
                width = androidx.compose.ui.unit.lerp(1.5.dp, 1.8.dp, p),
                color = lerp(
                    Color.White.copy(alpha = 0.12f * (1f - (p / 0.65f).coerceIn(0f, 1f))),
                    artColor,
                    morphProgress,
                ),
            )

            Box(
                modifier = Modifier
                    .offset {
                        val swipeOffset = if (p < 0.15f) swipeX.value else 0f
                        IntOffset((artX + swipeOffset).roundToInt(), artY.roundToInt())
                    }
                    .size(with(localDensity) { artSizePx.toDp() }),
            ) {
                RenderArtwork(
                    boxModifier = Modifier.fillMaxSize(),
                    shape = currentShape,
                    border = currentArtBorder,
                    elevation = artElevation,
                    forceStaticArt = isVinylMode && p > 0.35f,
                )
            }
        }

        // 3. Текст и кнопки мини-плеера (проявляются поверх обложки и фона)
        if (miniControlsAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = miniControlsAlpha },
                contentAlignment = Alignment.Center,
            ) {
                NowPlayingBarInner(
                    item = item,
                    isPlaying = isPlaying,
                    resolving = resolving,
                    animatedTrackColor = artColor,
                    scallopedArt = scallopedArt,
                    scallopedBtn = scallopedBtn,
                    onTogglePlayPause = onTogglePlayPause,
                    onPrev = onPrev,
                    onNext = onNext,
                    onClick = {},
                    showArtwork = isWide || showLyrics,
                )
            }
        }

        if (showFxSheet) {
            AudioFxBottomSheet(
                bassBoost = bassBoost,
                onSetBassBoost = onSetBassBoost,
                crushAmount = crushAmount,
                onSetCrush = onSetCrush,
                pitchSemi = pitchSemi,
                onSetPitchSemi = onSetPitchSemi,
                noteShiftSeed = noteShiftSeed,
                onRandomizeNotes = onRandomizeNotes,
                onResetAll = onResetAudioFx,
                accent = artColor,
                onDismiss = { showFxSheet = false },
            )
        }
    }
}
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

