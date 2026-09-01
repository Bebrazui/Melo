package com.melo.music.ui

import android.media.audiofx.Visualizer
import android.media.MediaPlayer
import androidx.compose.animation.core.LinearEasing
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
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.isActive

/**
 * Плавающий размытый фон.
 * - Медленно «плывёт» (пан + масштаб + поворот).
 * - При басе плавно пружинит с небольшим увеличением масштаба (+3%..5.5%).
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

    // --- Bass level from Visualizer fallback ---
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

            var lastFftUpdate = 0L
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
                    val now = android.os.SystemClock.uptimeMillis()
                    if (now - lastFftUpdate < 40) return // Ограничиваем частоту обновления до ~25 fps
                    lastFftUpdate = now

                    // Берём нижние частоты (бас 20-200 Hz).
                    // FFT layout: [re0, im0, re1, im1, ...]
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

    // Читаем уровень баса напрямую из DSP конвейера ExoPlayer (100% надёжность без permissions)
    var liveBass by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                val dsp = com.melo.music.audio.MeloDspAudioProcessor.currentBassLevel
                liveBass = maxOf(dsp, bassLevel)
            }
        }
    }

    // Мягкая упругая пульсация масштаба: небольшое аккуратное увеличение до +5.5% при басе
    val bassScalePulse by animateFloatAsState(
        targetValue = 1f + liveBass * 0.055f,
        animationSpec = spring(
            dampingRatio = 0.55f, // мягкий пружинящий отскок
            stiffness = 360f,
        ),
        label = "bassScalePulse",
    )

    // Один непрерывный линейный драйвер фазы → без «замедления и рывка».
    val infinite = rememberInfiniteTransition(label = "flow")
    val angle = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(36.dp)
                .graphicsLayer {
                    // Чтение state внутри graphicsLayer предотвращает лишнюю рекомпозицию
                    val curAngle = angle.value
                    val curBass = liveBass
                    val rad = Math.toRadians(curAngle.toDouble())
                    val baseOffset = 100f // px
                    val amp = 1f + curBass * 2f
                    val tx = (sin(rad) * baseOffset * amp).toFloat()
                    val ty = (sin(rad * 2.0 + 1.0) * baseOffset * amp).toFloat()
                    val baseScale = 1.12f
                    val scaleOsc = (sin(rad * 2.0) * 0.05).toFloat()
                    val sc = (baseScale + scaleOsc) * bassScalePulse
                    val rot = (cos(rad) * 2.5 * amp).toFloat()

                    translationX = tx
                    translationY = ty
                    scaleX = sc
                    scaleY = sc
                    rotationZ = rot
                },
        )
    }
}
