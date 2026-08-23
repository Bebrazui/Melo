package com.melo.music.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Анимации и скругления в стиле PixelPlayer.
 *
 * Smooth corners (squircle, 60% smoothness) вместо обычных RoundedCornerShape
 * и «пружинные» спецификации M3 Expressive: press-scale на карточках,
 * emphasized-easing переходы экранов.
 */
object ShapeCache {
    /** 8dp — чипы, мелкие поверхности. */
    val smooth8 = AbsoluteSmoothCornerShape(cornerRadius = 8.dp, smoothnessAsPercent = 60)

    /** 12dp — элементы списков треков, маленькие карточки. */
    val smooth12 = AbsoluteSmoothCornerShape(cornerRadius = 12.dp, smoothnessAsPercent = 60)

    /** 16dp — обложки в карточках, плейлисты. */
    val smooth16 = AbsoluteSmoothCornerShape(cornerRadius = 16.dp, smoothnessAsPercent = 60)

    /** 20dp — крупные карточки (треки, альбомы). */
    val smooth20 = AbsoluteSmoothCornerShape(cornerRadius = 20.dp, smoothnessAsPercent = 60)

    /** 24dp — hero-карточки, диалоги. */
    val smooth24 = AbsoluteSmoothCornerShape(cornerRadius = 24.dp, smoothnessAsPercent = 60)

    /** 32dp — bottom sheets, плавающие панели. */
    val smooth32 = AbsoluteSmoothCornerShape(cornerRadius = 32.dp, smoothnessAsPercent = 60)

    /** Пилюля — кнопки/чипы. */
    val smoothPill = AbsoluteSmoothCornerShape(cornerRadius = 50.dp, smoothnessAsPercent = 60)
}

/** Спецификации движения (по мотивам PixelPlayer / M3 Expressive). */
object Motion {
    /** Базовый emphasized-easing (M3). */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Вход экрана: быстрый старт, плавное торможение. */
    val EmphasizedDecelerate = CubicBezierEasing(0.2f, 0.85f, 0.7f, 1f)

    /** Выход экрана: разгон и уход. */
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Основной переход push/pop, мс. */
    const val TRANSITION_MS = 450

    /** Fade контента — половина основного перехода, мс. */
    const val FADE_MS = 225

    /** Переключение табов нижней навигации, мс. */
    const val TAB_TRANSITION_MS = 380

    /** Fade при переключении табов — половина таб-перехода, мс. */
    const val TAB_FADE_MS = 190

    /** Пружина нажатия на карточках/кнопках. */
    fun <T> pressSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}

/**
 * Press-scale в стиле PixelPlayer: карточка сжимается до [pressedScale]
 * под medium-bouncy пружиной, пока палец на ней.
 *
 * Передай тот же [interactionSource] в clickable/combiniedClickable,
 * если он уже есть у компонента.
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = Motion.pressSpring(),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
