package com.melo.music.ui

import android.media.audiofx.Visualizer
import android.media.MediaPlayer
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.sin

/**
 * Плавающий размытый фон.
 * - Медленно «плывёт» (пан + масштаб + поворот).
 * - При басе ускоряется плавание и увеличивается масштаб (пульсация).
 *
 * [audioSessionId] — ID аудиосессии ExoPlayer для Visualizer (0 = без детекции баса).
 */
@Composable
fun FlowingBackground(
    thumbnailUrl: String?,
    audioSessionId: Int = 0,
    modifier: Modifier = Modifier,
) {
    if (thumbnailUrl == null) return

    val context = LocalContext.current

    // --- Bass level (0f..1f) ---
    var bassLevel by remember { mutableFloatStateOf(0f) }

    DisposableEffect(audioSessionId) {
        if (audioSessionId <= 0) {
            onDispose { }
        } else {
            val visualizer = try {
                Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1] // max
                }
            } catch (_: Exception) { null }

            val listener = object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    vis: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int,
                ) { /* unused */ }

                override fun onFftDataCapture(
                    vis: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int,
                ) {
                    if (fft == null) return
                    // Берём нижние частоты (бас 20-200 Hz).
                    // FFT layout: [re0, im0, re1, im1, ...]
                    // bin index = freq * captureSize / samplingRate
                    val binSize = fft.size / 2
                    val lowEnd = (200f * fft.size / samplingRate).toInt().coerceIn(1, binSize - 1)
                    var sum = 0f
                    for (i in 1..lowEnd) {
                        val re = fft[2 * i].toFloat()
                        val im = fft[2 * i + 1].toFloat()
                        sum += re * re + im * im
                    }
                    val rms = kotlin.math.sqrt(sum / lowEnd) / 128f
                    bassLevel = (rms * 4f).coerceIn(0f, 1f)
                }
            }

            try {
                visualizer?.apply {
                    enabled = true
                    setDataCaptureListener(listener, Visualizer.getMaxCaptureRate(), false, true)
                }
            } catch (_: Exception) { }

            onDispose {
                try {
                    visualizer?.apply {
                        enabled = false
                        release()
                    }
                } catch (_: Exception) { }
            }
        }
    }

    // Анимация плавания.
    val infinite = rememberInfiniteTransition(label = "flow")

    // Базовая скорость: 15 сек на цикл (медленно).
    // При басе ускоряется до ~4 сек.
    val animDuration by remember {
        mutableStateOf(15_000)
    }
    val progressMs by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    // Фаза для масштаба (чуть быстрее).
    val scalePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (animDuration * 0.7f).toInt(), easing = EaseInOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scale",
    )

    // Преобразования.
    val rad = Math.toRadians(progressMs.toDouble())
    val baseOffset = 100f // px
    val speedMultiplier = 1f + bassLevel * 3f // x1..x4
    val tx = (sin(rad) * baseOffset * speedMultiplier).toFloat()
    val ty = (sin(rad * 0.7 + 1.0) * baseOffset * speedMultiplier).toFloat()
    val baseScale = 1.12f + bassLevel * 0.1f
    val scaleOsc = sin(Math.toRadians(scalePhase.toDouble())).toFloat() * 0.06f
    val scale = baseScale + scaleOsc
    val rotation = (sin(rad * 0.3) * 3f * speedMultiplier).toFloat()

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(48.dp)
                .graphicsLayer {
                    translationX = tx
                    translationY = ty
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                },
        )
    }
}
