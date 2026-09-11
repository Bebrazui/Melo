package com.melo.desktop.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.rememberUpdatedState

/**
 * Фирменный волнистый ползунок Melo (WavySlider), в точности как на телефоне:
 * - Бегущая синусоидальная волна при воспроизведении
 * - Плавное выпрямление в тонкую линию на паузе
 * - Аккуратный круглый ползунок-точка
 * - Мгновенный отклик на клик и перетаскивание
 */
@Composable
fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.22f),
    thumbColor: Color = Color.White,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    val infiniteTransition = rememberInfiniteTransition(label = "waveAnim")
    val wavePhase by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wavePhase",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(value) }
    val currentFraction = (if (isDragging) dragProgress else value).coerceIn(0f, 1f)

    // Плавное выпрямление волны в прямую линию при паузе и обратный подъем при воспроизведении
    val targetWaveFactor = if (isPlaying && !isDragging) 1f else 0f
    val animatedWaveFactor by animateFloatAsState(
        targetValue = targetWaveFactor,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "waveAmplitude",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    var newF = (down.position.x / width).coerceIn(0f, 1f)
                    isDragging = true
                    dragProgress = newF
                    currentOnValueChange(newF)
                    down.consume()

                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        change.consume()
                        newF = (change.position.x / width).coerceIn(0f, 1f)
                        dragProgress = newF
                        currentOnValueChange(newF)
                    }
                    isDragging = false
                    currentOnValueChangeFinished?.invoke(newF)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val thumbX = (currentFraction * width).coerceIn(0f, width)

            val wavelength = 36.dp.toPx()
            val amplitude = 4.2.dp.toPx() * animatedWaveFactor
            val strokeWidth = 3.5.dp.toPx()
            val thumbRadius = 6.5.dp.toPx()

            // 1. Волнистая линия пройденной части трека
            if (thumbX > 1f) {
                val wavePath = Path()
                wavePath.moveTo(0f, centerY)
                var x = 0f
                val step = 2f
                while (x <= thumbX) {
                    val damp = if (thumbX - x < wavelength * 0.45f) (thumbX - x) / (wavelength * 0.45f) else 1f
                    val y = centerY + kotlin.math.sin((x / wavelength) * 2 * Math.PI.toFloat() - wavePhase) * amplitude * damp
                    wavePath.lineTo(x, y)
                    x += step
                }
                drawPath(
                    path = wavePath,
                    color = activeColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }

            // 2. Ползунок (аккуратный круглый dot)
            drawCircle(
                color = thumbColor,
                radius = if (isDragging) thumbRadius * 1.25f else thumbRadius,
                center = Offset(thumbX, centerY),
            )

            // 3. Прямая линия непроигранной части
            if (thumbX < width - 1f) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(thumbX + thumbRadius, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
