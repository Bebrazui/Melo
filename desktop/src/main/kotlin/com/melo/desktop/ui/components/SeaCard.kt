package com.melo.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.ui.theme.MeloOnPrimary
import com.melo.desktop.ui.theme.MeloPrimary
import com.melo.desktop.ui.theme.MeloPrimaryContainer
import com.melo.desktop.ui.theme.MeloSecondaryContainer
import com.melo.desktop.ui.theme.pressScale
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Фирменная фигура «лепесток/цветок» с N лепестками (Material 3 Expressive). */
class CookieShape(
    private val petals: Int = 8,
    private val amp: Float = 0.12f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rMax = minOf(cx, cy)
        val base = rMax / (1f + amp)
        val steps = petals * 24
        val twoPi = (2.0 * Math.PI).toFloat()
        for (i in 0..steps) {
            val a = i.toFloat() / steps * twoPi
            val r = base * (1f + amp * cos(petals * a))
            val x = cx + r * cos(a)
            val y = cy + r * sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Фирменная карточка волны «Sea» — визуальное сердце Melo (1 в 1 с мобильной версией):
 * Двойной плавно вращающийся цветок с элементами управления CookieShape, анимациями загрузки
 * и физикой пружинного нажатия pressScale.
 */
@Composable
fun SeaCard(
    isPlaying: Boolean,
    isLiked: Boolean,
    loading: Boolean = false,
    playingTitle: String?,
    playingArtist: String?,
    selectedMood: String? = "Моя волна",
    onMoodSelect: ((String) -> Unit)? = null,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outerShape = remember { CookieShape(8, 0.11f) }
    val btnShape = remember { CookieShape(8, 0.16f) }

    val outerSize = 280.dp
    val innerSize = 216.dp

    // Лепестки медленно крутятся при воспроизведении
    val rotOuter = remember { Animatable(0f) }
    val rotInner = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        launch {
            while (isActive) {
                rotOuter.animateTo(rotOuter.value + 360f, tween(28000, easing = LinearEasing))
            }
        }
        launch {
            while (isActive) {
                rotInner.animateTo(rotInner.value - 360f, tween(19000, easing = LinearEasing))
            }
        }
    }

    val pulse = rememberInfiniteTransition(label = "seaPulse")
    val pOuter by pulse.animateFloat(1f, 1.04f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "po")
    val pInner by pulse.animateFloat(1.03f, 0.98f, infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pi")
    val sOuter = if (isPlaying) pOuter else 1f
    val sInner = if (isPlaying) pInner else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            // Внешний лепесток (вращается по часовой стрелке)
            Box(
                modifier = Modifier
                    .size(outerSize)
                    .graphicsLayer {
                        rotationZ = rotOuter.value
                        scaleX = sOuter
                        scaleY = sOuter
                    }
                    .clip(outerShape)
                    .background(MeloSecondaryContainer),
            )

            // Внутренний лепесток (вращается против часовой стрелки, темнее фон)
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .graphicsLayer {
                        rotationZ = rotInner.value
                        scaleX = sInner
                        scaleY = sInner
                    }
                    .clip(outerShape)
                    .background(Color(0xFF14241B)),
            )

            // Контент поверх лепестков (статичный, не вращается)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(innerSize),
            ) {
                Text(
                    text = "Sea",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp,
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3 фигурные кнопки-цветка CookieShape(8, 0.16f) как на телефоне с pressScale
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Лайк (46dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .pressScale(0.88f)
                            .clip(btnShape)
                            .background(MeloPrimaryContainer)
                            .clickable(onClick = onLike),
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Нравится",
                            tint = if (isLiked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Плей / Пауза / Загрузка (58dp, фигурный цветок с анимацией)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(58.dp)
                            .pressScale(0.88f)
                            .clip(btnShape)
                            .background(MeloPrimary)
                            .clickable(enabled = !loading, onClick = onPlayPause),
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = loading,
                            enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                            exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MeloOnPrimary,
                                strokeWidth = 2.5.dp,
                            )
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !loading,
                            enter = fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.65f),
                            exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.65f),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Играть",
                                tint = MeloOnPrimary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    // Дальше (46dp)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .pressScale(0.88f)
                            .clip(btnShape)
                            .background(MeloPrimaryContainer)
                            .clickable(onClick = onNext),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Дальше",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = playingTitle ?: "Бесконечная волна",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                Text(
                    text = (playingArtist ?: "под твой вкус").uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
        }

        // ── Чипы настроений волны (с физикой нажатия pressScale) ─────────────
        if (onMoodSelect != null) {
            Spacer(modifier = Modifier.height(14.dp))
            val moods = listOf("Моя волна", "Бодрое", "Спокойное", "Для работы", "Любимое")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                moods.forEach { mood ->
                    val isSelected = selectedMood == mood || (selectedMood == null && mood == "Моя волна")
                    val bg = if (isSelected) MeloPrimary else Color(0x2AFFFFFF)
                    val textCol = if (isSelected) MeloOnPrimary else Color.White

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .pressScale(0.92f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .clickable { onMoodSelect(mood) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = mood,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textCol,
                        )
                    }
                }
            }
        }
    }
}
