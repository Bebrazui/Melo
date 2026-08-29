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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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

/**
 * Карусельный фокус-эффект для элементов горизонтального списка (LazyRow):
 * Элементы в центре экрана имеют полный масштаб 1.0x и прозрачность 1.0,
 * а по мере смещения к краям плавно и мягко уменьшаются до [minScale] (по умолчанию 0.88f)
 * с лёгким затуханием до [minAlpha] (0.82f).
 * Вычисляется на фазе отрисовки (graphicsLayer) без вызова лишних рекомпозиций.
 */
fun Modifier.carouselCenterItemEffect(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    index: Int,
    minScale: Float = 0.88f,
    maxScale: Float = 1.0f,
    minAlpha: Float = 0.82f,
    maxAlpha: Float = 1.0f,
): Modifier = graphicsLayer {
    val layoutInfo = lazyListState.layoutInfo
    val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (visibleItem != null) {
        val viewportWidth = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
        if (viewportWidth > 0f) {
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            val itemCenter = visibleItem.offset + visibleItem.size / 2f
            val distanceFromCenter = kotlin.math.abs(viewportCenter - itemCenter)
            val maxDistance = viewportWidth / 2f
            val factor = (1f - (distanceFromCenter / maxDistance)).coerceIn(0f, 1f)
            // Косинусоидная плавная кривая интерполяции
            val smooth = (1f - kotlin.math.cos(factor * Math.PI).toFloat()) / 2f
            val scale = minScale + (maxScale - minScale) * smooth
            scaleX = scale
            scaleY = scale
            alpha = minAlpha + (maxAlpha - minAlpha) * smooth
        }
    }
}

/**
 * Пружинистый оверскролл (Bouncy Elastic Bounce):
 * При упоре в самый верх или в самый низ контент растягивается с нелинейным
 * резиновым сопротивлением, а при отпускании пальца весело и мягко
 * отпружинивает назад на физике Spring.
 */
fun Modifier.bouncyOverscroll(
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this
    val overscrollOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val current = overscrollOffset.value
                if (current != 0f) {
                    val delta = available.y
                    if ((current > 0f && delta < 0f) || (current < 0f && delta > 0f)) {
                        val consumed = if (kotlin.math.abs(delta) >= kotlin.math.abs(current)) -current else delta
                        scope.launch { overscrollOffset.snapTo(current + consumed) }
                        return Offset(0f, consumed)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val current = overscrollOffset.value
                    // Прогрессивное сопротивление пружины
                    val resistance = 0.38f / (1f + kotlin.math.abs(current) / 180f)
                    val newOffset = (current + available.y * resistance).coerceIn(-320f, 320f)
                    scope.launch { overscrollOffset.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollOffset.value != 0f) {
                    scope.launch { springBack(overscrollOffset) }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                if (overscrollOffset.value != 0f) {
                    springBack(overscrollOffset)
                }
                return Velocity.Zero
            }

            private suspend fun springBack(anim: androidx.compose.animation.core.Animatable<Float, *>) {
                anim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
        }
    }

    this
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer {
            translationY = overscrollOffset.value
        }
}

/**
 * Горизонтальный пружинистый оверскролл для каруселей (LazyRow):
 * При упоре в край карусель упруго оттягивается по горизонтали с сопротивлением резины,
 * а при отпускании пальца весело отпружинивает назад на физике Spring.
 */
fun Modifier.bouncyHorizontalOverscroll(
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this
    val overscrollOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val current = overscrollOffset.value
                if (current != 0f) {
                    val delta = available.x
                    if ((current > 0f && delta < 0f) || (current < 0f && delta > 0f)) {
                        val consumed = if (kotlin.math.abs(delta) >= kotlin.math.abs(current)) -current else delta
                        scope.launch { overscrollOffset.snapTo(current + consumed) }
                        return Offset(consumed, 0f)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.x != 0f) {
                    val current = overscrollOffset.value
                    val resistance = 0.38f / (1f + kotlin.math.abs(current) / 160f)
                    val newOffset = (current + available.x * resistance).coerceIn(-260f, 260f)
                    scope.launch { overscrollOffset.snapTo(newOffset) }
                    return Offset(available.x, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollOffset.value != 0f) {
                    scope.launch { springBack(overscrollOffset) }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                if (overscrollOffset.value != 0f) {
                    springBack(overscrollOffset)
                }
                return Velocity.Zero
            }

            private suspend fun springBack(anim: androidx.compose.animation.core.Animatable<Float, *>) {
                anim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
        }
    }

    this
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer {
            translationX = overscrollOffset.value
        }
}
