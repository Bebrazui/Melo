package com.melo.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.rememberUpdatedState

/**
 * Аккуратный минималистичный слайдер с круглым ползунком-точкой и тонким треком:
 * Заменяет стандартный громоздкий слайдер Material 3 Desktop (с капсулами и вертикальными чертами).
 */
@Composable
fun SleekSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.22f),
    thumbColor: Color = Color.White,
    trackHeightDp: Float = 4f,
    thumbRadiusDp: Float = 5.5f,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    var isDragging by remember { mutableStateOf(false) }
    var dragVal by remember { mutableFloatStateOf(value) }
    val currentVal = (if (isDragging) dragVal else value).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(28.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    var newF = (down.position.x / width).coerceIn(0f, 1f)
                    isDragging = true
                    dragVal = newF
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
                        dragVal = newF
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
            val trackH = trackHeightDp.dp.toPx()
            val thumbR = if (isDragging) (thumbRadiusDp * 1.35f).dp.toPx() else thumbRadiusDp.dp.toPx()
            val corner = CornerRadius(trackH / 2f, trackH / 2f)

            val thumbX = (currentVal * width).coerceIn(0f, width)

            // 1. Неактивная часть трека (подложка)
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - trackH / 2f),
                size = Size(width, trackH),
                cornerRadius = corner,
            )

            // 2. Активная часть трека
            if (thumbX > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, centerY - trackH / 2f),
                    size = Size(thumbX, trackH),
                    cornerRadius = corner,
                )
            }

            // 3. Круглый ползунок-точка
            drawCircle(
                color = thumbColor,
                radius = thumbR,
                center = Offset(thumbX, centerY),
            )
        }
    }
}
