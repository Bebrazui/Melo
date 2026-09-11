package com.melo.desktop.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.melo.desktop.ui.theme.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.audio.DesktopAudioPlayer
import com.melo.desktop.extractor.ResolvedTrack
import com.melo.desktop.extractor.Source
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.storage.DesktopStorage
import com.melo.desktop.ui.theme.MeloOnPrimary
import com.melo.desktop.ui.theme.MeloPrimary
import com.melo.desktop.ui.theme.MeloPrimaryContainer

/**
 * Полноэкранный атмосферный плеер NowPlaying, вдохновленный мобильным интерфейсом Melo:
 * - Атмосферный плавающий размытый фон от обложки (FlowingBackground)
 * - Крупная обложка с мягким светом и скруглением 32.dp
 * - Интерактивный просмотр текста песни (караоке / synced lyrics)
 * - Качественные контролы воспроизведения, индикаторы битрейта / формата, громкость
 */
@Composable
fun NowPlayingOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        NowPlayingContent(
            onDismiss = onDismiss,
            onPrevious = onPrevious,
            onNext = onNext,
        )
    }
}

@Composable
fun NowPlayingContent(
    onDismiss: () -> Unit,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    initialVinylMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val track = DesktopAudioPlayer.currentTrack
    if (track == null) {
        val emptyInteractionSource = remember { MutableInteractionSource() }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F1412))
                .clickable(
                    interactionSource = emptyInteractionSource,
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Кнопка сворачивания вверху слева
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(28.dp)
                    .size(44.dp)
                    .pressScale(0.88f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Свернуть",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(40.dp),
                    )
                }

                Text(
                    text = "Сейчас ничего не воспроизводится",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Выберите трек из каталога или запустите волну Sea",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .pressScale(0.90f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MeloPrimary)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Вернуться на главную",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MeloOnPrimary,
                    )
                }
            }
        }
        return
    }

    var showLyrics by remember { mutableStateOf(false) }
    var isVinylMode by remember { mutableStateOf(initialVinylMode) }
    val isLiked = DesktopStorage.isFavorite(track.originalUrl)
    val isPlaying = DesktopAudioPlayer.isPlaying
    val isBuffering = DesktopAudioPlayer.isBuffering
    val positionMs = DesktopAudioPlayer.currentPositionMs
    val durationMs = DesktopAudioPlayer.durationMs
    val effectiveDurationMs = if (durationMs > 0L) durationMs else (track.durationSeconds * 1000L)

    var tiltTargetX by remember { mutableStateOf(0f) }
    var tiltTargetY by remember { mutableStateOf(0f) }

    val tiltRoll by animateFloatAsState(
        targetValue = tiltTargetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "tiltRoll",
    )
    val tiltPitch by animateFloatAsState(
        targetValue = tiltTargetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "tiltPitch",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "haloGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isPlaying) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = if (isPlaying) 0.52f else 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val likeBounceAnim = remember { Animatable(1f) }

    val playInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
    val playBounceAnim = remember { Animatable(1f) }

    val prevInteractionSource = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteractionSource.collectIsPressedAsState()
    val prevBounceAnim = remember { Animatable(1f) }

    val nextInteractionSource = remember { MutableInteractionSource() }
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()
    val nextBounceAnim = remember { Animatable(1f) }

    val prevScaleX by animateFloatAsState(
        targetValue = when {
            isPrevPressed -> 1.46f
            isPlayPressed -> 0.82f
            isNextPressed -> 0.92f
            else -> prevBounceAnim.value
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "prevScaleX",
    )

    val playScaleX by animateFloatAsState(
        targetValue = when {
            isPlayPressed -> 1.40f
            isPrevPressed -> 0.80f
            isNextPressed -> 0.80f
            else -> playBounceAnim.value
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "playScaleX",
    )

    val nextScaleX by animateFloatAsState(
        targetValue = when {
            isNextPressed -> 1.46f
            isPlayPressed -> 0.82f
            isPrevPressed -> 0.92f
            else -> nextBounceAnim.value
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nextScaleX",
    )

    val prevCornerRadius by animateFloatAsState(if (isPrevPressed) 18f else 32f, label = "prevCornerRadius")
    val playCornerRadius by animateFloatAsState(if (isPlayPressed) 16f else 26f, label = "playCornerRadius")
    val nextCornerRadius by animateFloatAsState(if (isNextPressed) 18f else 32f, label = "nextCornerRadius")

    val animScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (w > 0 && h > 0) {
                                tiltTargetX = ((change.position.x - w / 2f) / (w / 2f)).coerceIn(-1f, 1f)
                                tiltTargetY = ((change.position.y - h / 2f) / (h / 2f)).coerceIn(-1f, 1f)
                            }
                        }
                    }
                }
            },
    ) {
        FlowingBackground(
            thumbnailUrl = track.thumbnailUrl,
            dimOverlayAlpha = 0.86f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .pressScale(0.88f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Свернуть",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Text(
                    text = "СЕЙЧАС ИГРАЕТ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.70f),
                    letterSpacing = 2.sp,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(
                        onClick = { isVinylMode = !isVinylMode },
                        modifier = Modifier
                            .size(44.dp)
                            .pressScale(0.88f)
                            .clip(CircleShape)
                            .background(if (isVinylMode) MeloPrimary else Color.White.copy(alpha = 0.10f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = "Виниловая пластинка",
                            tint = if (isVinylMode) MeloOnPrimary else Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = { showLyrics = !showLyrics },
                        modifier = Modifier
                            .size(44.dp)
                            .pressScale(0.88f)
                            .clip(CircleShape)
                            .background(if (showLyrics) MeloPrimary else Color.White.copy(alpha = 0.10f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Текст песни",
                            tint = if (showLyrics) MeloOnPrimary else Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    if (showLyrics) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(),
                        ) {
                            LyricsView(
                                onClose = { showLyrics = false },
                                modifier = Modifier.fillMaxSize().background(Color.Transparent),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(370.dp)
                                .graphicsLayer {
                                    scaleX = glowPulse
                                    scaleY = glowPulse
                                }
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            0.0f to MeloPrimary.copy(alpha = glowAlpha),
                                            0.45f to MeloPrimary.copy(alpha = glowAlpha * 0.45f),
                                            0.78f to Color.Transparent,
                                        ),
                                    )
                                },
                        )

                        AnimatedContent(
                            targetState = isVinylMode,
                            transitionSpec = {
                                (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.88f))
                                    .togetherWith(fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.88f))
                            },
                            label = "coverOrVinylMorph",
                            modifier = Modifier
                                .size(360.dp)
                                .graphicsLayer {
                                    rotationY = tiltRoll * 6.5f
                                    rotationX = -tiltPitch * 6.5f
                                    translationX = tiltRoll * 4.dp.toPx()
                                    translationY = tiltPitch * 4.dp.toPx()
                                    cameraDistance = 18f * 1f
                                },
                        ) { vinyl ->
                            if (vinyl) {
                                VinylRecordView(
                                    artUrl = track.thumbnailUrl,
                                    isPlaying = isPlaying,
                                    tiltRoll = tiltRoll,
                                    tiltPitch = tiltPitch,
                                    modifier = Modifier.size(360.dp),
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(32.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)),
                                    shadowElevation = 16.dp,
                                    modifier = Modifier
                                        .size(360.dp)
                                        .clip(RoundedCornerShape(32.dp)),
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncCoverImage(
                                            url = track.thumbnailUrl,
                                            contentScale = ContentScale.Crop,
                                            shape = RoundedCornerShape(32.dp),
                                            modifier = Modifier.fillMaxSize(),
                                        )

                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val cx = size.width * (0.45f + tiltRoll * 0.25f)
                                            val cy = size.height * (0.40f + tiltPitch * 0.25f)
                                            drawRect(
                                                brush = Brush.radialGradient(
                                                    0.0f to Color.White.copy(alpha = 0.14f),
                                                    0.35f to Color.White.copy(alpha = 0.05f),
                                                    0.70f to Color.White.copy(alpha = 0.01f),
                                                    1.0f to Color.Transparent,
                                                    center = Offset(cx, cy),
                                                    radius = size.width * 0.90f,
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

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1.1f)
                        .graphicsLayer {
                            translationX = tiltRoll * 2.dp.toPx()
                            translationY = tiltPitch * 2.dp.toPx()
                        },
                ) {
                    Text(
                        text = track.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 34.sp,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = track.artist ?: "Неизвестный исполнитель",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    val sliderPos = if (effectiveDurationMs > 0) (positionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f) else 0f
                    var pendingSeekFraction by remember { mutableStateOf<Float?>(null) }
                    val currentFraction = pendingSeekFraction ?: sliderPos

                    WavySlider(
                        value = currentFraction.coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            pendingSeekFraction = fraction
                        },
                        onValueChangeFinished = { fraction ->
                            if (effectiveDurationMs > 0) {
                                val targetMs = (fraction * effectiveDurationMs).toLong()
                                DesktopAudioPlayer.seekTo(targetMs)
                            }
                            animScope.launch {
                                delay(150)
                                pendingSeekFraction = null
                            }
                        },
                        isPlaying = isPlaying,
                        activeColor = Color.White,
                        inactiveColor = Color.White.copy(alpha = 0.22f),
                        thumbColor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val currentDisplayMs = pendingSeekFraction?.let { (it * effectiveDurationMs).toLong() } ?: positionMs

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                    ) {
                        Text(
                            text = formatTime(currentDisplayMs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.align(Alignment.CenterStart),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            val qualityLabel = remember(track.source, track.originalUrl) {
                                when (track.source) {
                                    Source.YOUTUBE_MUSIC -> "44.1 kHz • 256 kbps • Opus"
                                    Source.SOUNDCLOUD -> "44.1 kHz • 160 kbps • MP3"
                                    Source.BANDCAMP -> "44.1 kHz • 320 kbps • MP3"
                                    Source.LOCAL -> "44.1 kHz • Lossless • FLAC"
                                    else -> "44.1 kHz • 320 kbps • MP3"
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                modifier = Modifier.height(24.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                ) {
                                    Text(
                                        text = qualityLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.75f),
                                    )
                                }
                            }

                            // Компактный чип скорости (скрыт по умолчанию, открывается по клику)
                            DesktopSpeedDropdownChip(
                                speed = DesktopAudioPlayer.speed,
                                onSetSpeed = { DesktopAudioPlayer.setPlaybackSpeed(it) },
                            )
                        }

                        Text(
                            text = formatTime(effectiveDurationMs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val isShuffle = DesktopAudioPlayer.isShuffle
                        IconButton(
                            onClick = { DesktopAudioPlayer.toggleShuffle() },
                            modifier = Modifier.size(44.dp).pressScale(0.88f),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Перемешать",
                                tint = if (isShuffle) MeloPrimary else Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(prevCornerRadius.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .width(66.dp * prevScaleX)
                                    .height(66.dp)
                                    .clip(RoundedCornerShape(prevCornerRadius.dp))
                                    .clickable(
                                        interactionSource = prevInteractionSource,
                                        indication = null,
                                        onClick = {
                                            animScope.launch {
                                                prevBounceAnim.snapTo(1.25f)
                                                prevBounceAnim.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMediumLow,
                                                    ),
                                                )
                                            }
                                            onPrevious()
                                        },
                                    ),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Предыдущий",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(playCornerRadius.dp),
                                color = Color.White,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .width(96.dp * playScaleX)
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(playCornerRadius.dp))
                                    .clickable(
                                        interactionSource = playInteractionSource,
                                        indication = null,
                                        onClick = {
                                            animScope.launch {
                                                playBounceAnim.snapTo(1.25f)
                                                playBounceAnim.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMediumLow,
                                                    ),
                                                )
                                            }
                                            DesktopAudioPlayer.togglePlayPause()
                                        },
                                    ),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isBuffering,
                                        enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                                        exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(30.dp),
                                            color = Color(0xFF0F1412),
                                            strokeWidth = 3.dp,
                                        )
                                    }
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !isBuffering,
                                        enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                                        exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                                    ) {
                                        AnimatedContent(
                                            targetState = isPlaying,
                                            transitionSpec = {
                                                (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.7f))
                                                    .togetherWith(fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.7f))
                                            },
                                            label = "playPauseMorph",
                                        ) { playing ->
                                            Icon(
                                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (playing) "Пауза" else "Играть",
                                                tint = Color(0xFF0F1412),
                                                modifier = Modifier.size(36.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(nextCornerRadius.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .width(66.dp * nextScaleX)
                                    .height(66.dp)
                                    .clip(RoundedCornerShape(nextCornerRadius.dp))
                                    .clickable(
                                        interactionSource = nextInteractionSource,
                                        indication = null,
                                        onClick = {
                                            animScope.launch {
                                                nextBounceAnim.snapTo(1.25f)
                                                nextBounceAnim.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMediumLow,
                                                    ),
                                                )
                                            }
                                            onNext()
                                        },
                                    ),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Следующий",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }

                        val repeatMode = DesktopAudioPlayer.repeatMode
                        IconButton(
                            onClick = { DesktopAudioPlayer.toggleRepeatMode() },
                            modifier = Modifier.size(44.dp).pressScale(0.88f),
                        ) {
                            val (icon, tint) = when (repeatMode) {
                                DesktopAudioPlayer.RepeatMode.OFF -> Icons.Rounded.Repeat to Color.White.copy(alpha = 0.55f)
                                DesktopAudioPlayer.RepeatMode.ALL -> Icons.Rounded.Repeat to MeloPrimary
                                DesktopAudioPlayer.RepeatMode.ONE -> Icons.Rounded.RepeatOne to MeloPrimary
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "Повтор",
                                tint = tint,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(
                            onClick = {
                                animScope.launch {
                                    likeBounceAnim.snapTo(1.45f)
                                    likeBounceAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                                DesktopStorage.toggleFavorite(
                                    TrackItem(
                                        title = track.title,
                                        uploader = track.artist,
                                        url = track.originalUrl,
                                        durationSeconds = effectiveDurationMs / 1000,
                                        thumbnailUrl = track.thumbnailUrl,
                                        source = track.source,
                                    ),
                                )
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .graphicsLayer {
                                    scaleX = likeBounceAnim.value
                                    scaleY = likeBounceAnim.value
                                }
                                .pressScale(0.88f),
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Лайк",
                                tint = if (isLiked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.width(260.dp),
                        ) {
                            val vol = DesktopAudioPlayer.volume
                            val volIcon = when {
                                vol <= 0.01f -> Icons.AutoMirrored.Rounded.VolumeMute
                                vol < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                                else -> Icons.AutoMirrored.Rounded.VolumeUp
                            }

                            IconButton(
                                onClick = {
                                    if (vol > 0.01f) DesktopAudioPlayer.setPlayerVolume(0f) else DesktopAudioPlayer.setPlayerVolume(0.8f)
                                },
                                modifier = Modifier.size(32.dp).pressScale(0.88f),
                            ) {
                                Icon(
                                    imageVector = volIcon,
                                    contentDescription = "Громкость",
                                    tint = Color.White.copy(alpha = 0.70f),
                                    modifier = Modifier.size(22.dp),
                                )
                            }

                            SleekSlider(
                                value = vol,
                                onValueChange = { DesktopAudioPlayer.setPlayerVolume(it) },
                                activeColor = Color.White.copy(alpha = 0.85f),
                                inactiveColor = Color.White.copy(alpha = 0.20f),
                                thumbColor = Color.White,
                                modifier = Modifier.weight(1f),
                            )

                            Text(
                                text = "${(vol * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.60f),
                                modifier = Modifier.width(36.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VinylRecordView(
    artUrl: String?,
    isPlaying: Boolean,
    tiltRoll: Float,
    tiltPitch: Float,
    modifier: Modifier = Modifier,
) {
    val vinylRotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                vinylRotation.animateTo(
                    targetValue = vinylRotation.value + 360f,
                    animationSpec = tween(8000, easing = LinearEasing),
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF0D0D10),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.16f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = vinylRotation.value % 360f
                },
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val discRadius = kotlin.math.min(size.width, size.height) / 2f
                    val centerLabelRadius = discRadius * 0.46f

                    val numGrooves = 32
                    for (i in 0 until numGrooves) {
                        val r = centerLabelRadius + (discRadius - centerLabelRadius - 10.dp.toPx()) * (i.toFloat() / numGrooves)
                        val alpha = when {
                            i % 7 == 0 -> 0.10f
                            i % 3 == 0 -> 0.05f
                            else -> 0.025f
                        }
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = r,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                        )
                    }

                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = discRadius - 4.dp.toPx(),
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                    )

                    val sheenBrush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.10f to Color.White.copy(alpha = 0.07f),
                        0.16f to Color.White.copy(alpha = 0.18f),
                        0.22f to Color.White.copy(alpha = 0.07f),
                        0.32f to Color.Transparent,
                        0.58f to Color.Transparent,
                        0.66f to Color.White.copy(alpha = 0.07f),
                        0.72f to Color.White.copy(alpha = 0.18f),
                        0.78f to Color.White.copy(alpha = 0.07f),
                        0.88f to Color.Transparent,
                        1.0f to Color.Transparent,
                        center = center,
                    )
                    drawCircle(
                        brush = sheenBrush,
                        radius = discRadius - 4.dp.toPx(),
                        center = center,
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize(0.48f),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF18181C),
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.25f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncCoverImage(
                                url = artUrl,
                                contentScale = ContentScale.Crop,
                                shape = CircleShape,
                                modifier = Modifier.fillMaxSize(),
                            )

                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxSize(),
                            ) {}

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF08080A),
                                border = BorderStroke(1.5.dp, Color(0xFFB0B0B8)),
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(24.dp),
                            ) {}
                        }
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
            val sheenAlpha = (0.08f + (kotlin.math.abs(tiltRoll) + kotlin.math.abs(tiltPitch)) * 0.04f).coerceIn(0.02f, 0.14f)
            val cx = size.width * (0.45f + tiltRoll * 0.25f)
            val cy = size.height * (0.40f + tiltPitch * 0.25f)
            val discRadius = kotlin.math.min(size.width, size.height) / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color.White.copy(alpha = sheenAlpha),
                    0.35f to Color.White.copy(alpha = sheenAlpha * 0.4f),
                    0.70f to Color.White.copy(alpha = sheenAlpha * 0.1f),
                    1.0f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = discRadius * 0.95f,
                ),
                radius = discRadius,
                center = center,
            )
        }
    }
}

/**
 * Компактный выпадающий чип выбора скорости: slowed down (0.93) / original (1.0) / speed up (1.15).
 * По умолчанию занимает минимум места (~24dp) и открывает аккуратное меню только при нажатии.
 */
@Composable
private fun DesktopSpeedDropdownChip(
    speed: Float,
    onSetSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val isCustomSpeed = kotlin.math.abs(speed - DesktopAudioPlayer.SPEED_ORIGINAL) >= 0.02f
    val speedLabel = when {
        kotlin.math.abs(speed - DesktopAudioPlayer.SPEED_SLOWED) < 0.02f -> "0.93x slowed"
        kotlin.math.abs(speed - DesktopAudioPlayer.SPEED_SPEED_UP) < 0.02f -> "1.15x speed up"
        else -> "1.0x"
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCustomSpeed) MeloPrimary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
            border = BorderStroke(
                1.dp,
                if (isCustomSpeed) MeloPrimary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f),
            ),
            modifier = Modifier
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .pressScale(0.92f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = speedLabel,
                    fontSize = 11.sp,
                    fontWeight = if (isCustomSpeed) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCustomSpeed) MeloPrimary else Color.White.copy(alpha = 0.75f),
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Выбор скорости",
                    tint = if (isCustomSpeed) MeloPrimary else Color.White.copy(alpha = 0.60f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF1B2320), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        ) {
            val options = listOf(
                Triple("slowed down (0.93x)", DesktopAudioPlayer.SPEED_SLOWED, "Замедленный темп"),
                Triple("original (1.0x)", DesktopAudioPlayer.SPEED_ORIGINAL, "Оригинальная скорость"),
                Triple("speed up (1.15x)", DesktopAudioPlayer.SPEED_SPEED_UP, "Ускоренный темп"),
            )

            options.forEach { (label, value, hint) ->
                val isSelected = kotlin.math.abs(speed - value) < 0.02f
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MeloPrimary else Color.White,
                            )
                            Text(
                                text = hint,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.50f),
                            )
                        }
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MeloPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else null,
                    onClick = {
                        onSetSpeed(value)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
