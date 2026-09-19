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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.os.Build
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Спецификации скруглений и анимаций.
 *
 * Используем аппаратные RoundedCornerShape с полноценным сабпиксельным сглаживанием (anti-aliasing)
 * для идеально четких и гладких углов обложек без лесенок и размытия.
 */
object ShapeCache {
    /** 8dp — чипы, мелкие поверхности. */
    val smooth8 = RoundedCornerShape(8.dp)

    /** 12dp — элементы списков треков, маленькие карточки. */
    val smooth12 = RoundedCornerShape(12.dp)

    /** 16dp — обложки в карточках, плейлисты. */
    val smooth16 = RoundedCornerShape(16.dp)

    /** 20dp — крупные карточки (треки, альбомы). */
    val smooth20 = RoundedCornerShape(20.dp)

    /** 24dp — hero-карточки, диалоги. */
    val smooth24 = RoundedCornerShape(24.dp)

    /** 32dp — bottom sheets, плавающие панели. */
    val smooth32 = RoundedCornerShape(32.dp)

    /** Пилюля — кнопки/чипы. */
    val smoothPill = RoundedCornerShape(50.dp)
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
 * Элементы в центре экрана имеют полный масштаб 1.0x и прозрачность 1.0.
 *
 * При включенном тумблере «Больше эффектов» карточки по бокам:
 * 1. Уменьшаются (до 0.83f)
 * 2. Наклоняются в 3D (rotationY с глубиной перспективы cameraDistance)
 * 3. Размываются (RenderEffect blur)
 * 4. Слегка затемняются
 *
 * Вычисляется на фазе отрисовки (graphicsLayer) без лишних рекомпозиций.
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

            val moreEffects = com.melo.music.settings.AppSettings.moreEffects

            if (moreEffects) {
                val offsetRatio = ((itemCenter - viewportCenter) / maxDistance).coerceIn(-1f, 1f)
                val sign = kotlin.math.sign(offsetRatio)
                val absRatio = kotlin.math.abs(offsetRatio)

                // Начало 3D-поворота дальше от центра: в центральной зоне карточка стоит ровно
                val centerDeadzone = 0.26f
                val edgeProgress = if (absRatio > centerDeadzone) {
                    ((absRatio - centerDeadzone) / (1f - centerDeadzone)).coerceIn(0f, 1f)
                } else 0f
                val smoothEdge = edgeProgress * edgeProgress * (3f - 2f * edgeProgress)

                // 1. Плавное уменьшение масштаба ТОЛЬКО уходящих к краю карточек
                val scale = 1.0f - smoothEdge * 0.11f
                scaleX = scale
                scaleY = scale

                // 2. Легкое затемнение карточек по краям
                alpha = 1.0f - smoothEdge * 0.18f

                // 3. 3D-поворот, плавно нарастающий дальше от центра
                rotationY = sign * smoothEdge * 18f
                cameraDistance = 24f

                // 4. Мягкое боке на периферийных карточках без артефактов (DECAL)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val blurPx = smoothEdge * 2.5f
                    renderEffect = if (blurPx > 0.5f) {
                        android.graphics.RenderEffect.createBlurEffect(
                            blurPx, blurPx, android.graphics.Shader.TileMode.DECAL
                        ).asComposeRenderEffect()
                    } else null
                }
            } else {
                val scale = minScale + (maxScale - minScale) * smooth
                scaleX = scale
                scaleY = scale
                alpha = minAlpha + (maxAlpha - minAlpha) * smooth
                rotationY = 0f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = null
                }
            }
        }
    }
}

/**
 * Карусельный 3D-эффект для элементов сетки «Быстрый выбор» (LazyHorizontalGrid).
 * Начинается дальше от центра: центральные элементы стоят ровно.
 */
fun Modifier.carouselCenterGridItemEffect(
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    index: Int,
): Modifier = graphicsLayer {
    val layoutInfo = lazyGridState.layoutInfo
    val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (visibleItem != null) {
        val viewportWidth = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
        if (viewportWidth > 0f) {
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            val itemCenter = visibleItem.offset.x + visibleItem.size.width / 2f
            val distanceFromCenter = kotlin.math.abs(viewportCenter - itemCenter)
            val maxDistance = viewportWidth / 2f
            val factor = (1f - (distanceFromCenter / maxDistance)).coerceIn(0f, 1f)
            val smooth = (1f - kotlin.math.cos(factor * Math.PI).toFloat()) / 2f

            val moreEffects = com.melo.music.settings.AppSettings.moreEffects

            if (moreEffects) {
                val offsetRatio = ((itemCenter - viewportCenter) / maxDistance).coerceIn(-1f, 1f)
                val sign = kotlin.math.sign(offsetRatio)
                val absRatio = kotlin.math.abs(offsetRatio)

                val centerDeadzone = 0.28f
                val edgeProgress = if (absRatio > centerDeadzone) {
                    ((absRatio - centerDeadzone) / (1f - centerDeadzone)).coerceIn(0f, 1f)
                } else 0f
                val smoothEdge = edgeProgress * edgeProgress * (3f - 2f * edgeProgress)

                val scale = 1.0f - smoothEdge * 0.08f
                scaleX = scale
                scaleY = scale
                alpha = 1.0f - smoothEdge * 0.15f
                rotationY = sign * smoothEdge * 11f
                cameraDistance = 24f

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val blurPx = smoothEdge * 2f
                    renderEffect = if (blurPx > 0.5f) {
                        android.graphics.RenderEffect.createBlurEffect(
                            blurPx, blurPx, android.graphics.Shader.TileMode.DECAL
                        ).asComposeRenderEffect()
                    } else null
                }
            } else {
                val scale = 0.95f + 0.05f * smooth
                scaleX = scale
                scaleY = scale
                alpha = 0.92f + 0.08f * smooth
                rotationY = 0f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = null
                }
            }
        }
    }
}

/**
 * Кинематографичный вертикальный эффект затухания, сжатия и размытия у верхней и нижней границ:
 * Срабатывает СТРОГО когда элемент физически покидает рабочую область экрана
 * (уходит под верхнюю панель поиска или под нижнюю панель навигации).
 * Весь контент внутри рабочей зоны остаётся на 100% резким и без размытия.
 */
fun Modifier.verticalScrollEdgeItemEffect(): Modifier = composed {
    if (!com.melo.music.settings.AppSettings.moreEffects) return@composed this

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    val topBoundary = with(density) { (if (isLandscape) 48.dp else 68.dp).toPx() }
    val bottomBoundary = screenHeightPx - with(density) { (if (isLandscape) 44.dp else 80.dp).toPx() }
    val exitZone = with(density) { 36.dp.toPx() }

    var yInRoot by remember { mutableFloatStateOf(-1f) }
    var itemHeight by remember { mutableFloatStateOf(0f) }

    this
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                val pos = coords.positionInRoot()
                yInRoot = pos.y
                itemHeight = coords.size.height.toFloat()
            }
        }
        .graphicsLayer {
            if (yInRoot >= 0f && itemHeight > 0f) {
                val itemTop = yInRoot
                val itemBottom = yInRoot + itemHeight

                // Сверху: срабатывает ТОЛЬКО когда верх элемента уходит под строку поиска
                val topFactor = if (itemTop < topBoundary) {
                    ((itemTop - (topBoundary - exitZone)) / exitZone).coerceIn(0f, 1f)
                } else 1f

                // Снизу: срабатывает ТОЛЬКО когда низ элемента уходит за нижний бар
                val bottomFactor = if (itemBottom > bottomBoundary) {
                    (((bottomBoundary + exitZone) - itemBottom) / exitZone).coerceIn(0f, 1f)
                } else 1f

                val edgeFactor = min(topFactor, bottomFactor)
                if (edgeFactor < 1f) {
                    val smooth = edgeFactor * edgeFactor * (3f - 2f * edgeFactor)
                    val s = 0.965f + 0.035f * smooth
                    scaleX = s
                    scaleY = s
                    alpha = 0.75f + 0.25f * smooth

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blurPx = (1f - smooth) * 4f
                        renderEffect = if (blurPx > 0.5f) {
                            android.graphics.RenderEffect.createBlurEffect(
                                blurPx, blurPx, android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                        } else null
                    }
                } else {
                    scaleX = 1f
                    scaleY = 1f
                    alpha = 1f
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = null
                    }
                }
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
