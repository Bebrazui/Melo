package com.melo.desktop.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Атмосферный плавающий размытый фон рабочего стола, адаптированный из мобильного Melo:
 * - Плавный бесконечный дрейф (32-секундный цикл)
 * - Skia-размытие обложки текущего трека
 * - Тёмный градиентный оверлей для кристальной читаемости текста и Material 3 элементов.
 */
@Composable
fun FlowingBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    dimOverlayAlpha: Float = 0.82f,
) {
    if (thumbnailUrl.isNullOrBlank()) return

    val bitmap = rememberCoverBitmap(thumbnailUrl)

    val infinite = rememberInfiniteTransition(label = "flowBackground")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bgAngle",
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (bitmap != null) {
            val rad = Math.toRadians(angle.toDouble())
            val tx = (sin(rad) * 40f).toFloat()
            val ty = (sin(rad * 1.5 + 0.8) * 32f).toFloat()
            val scaleOsc = (sin(rad * 2.0) * 0.04).toFloat()
            val sc = 1.20f + scaleOsc
            val rot = (cos(rad) * 1.8).toFloat()

            Image(
                painter = BitmapPainter(bitmap),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(54.dp)
                    .graphicsLayer {
                        translationX = tx
                        translationY = ty
                        scaleX = sc
                        scaleY = sc
                        rotationZ = rot
                    },
            )
        }

        // Тёмный градиентный виньеточный оверлей
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = dimOverlayAlpha * 0.70f),
                            Color.Black.copy(alpha = dimOverlayAlpha * 0.85f),
                            Color.Black.copy(alpha = dimOverlayAlpha),
                        ),
                    ),
                ),
        )
    }
}
