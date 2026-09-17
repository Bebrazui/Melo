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
                // 1. Уменьшение масштаба
                val scale = 0.83f + 0.17f * smooth
                scaleX = scale
                scaleY = scale

                // 2. Затемнение карточек по краям
                alpha = 0.65f + 0.35f * smooth

                // 3. 3D-наклон карточек к центру
                val offsetRatio = ((itemCenter - viewportCenter) / maxDistance).coerceIn(-1f, 1f)
                rotationY = offsetRatio * 18f
                cameraDistance = 16f * density

                // 4. Мягкое размытие боковых карточек (на Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val blurPx = ((1f - smooth) * 9f).coerceAtLeast(0f)
                    renderEffect = if (blurPx > 0.5f) {
                        android.graphics.RenderEffect.createBlurEffect(
                            blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP
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
                val scale = 0.88f + 0.12f * smooth
                scaleX = scale
                scaleY = scale
                alpha = 0.70f + 0.30f * smooth

                val offsetRatio = ((itemCenter - viewportCenter) / maxDistance).coerceIn(-1f, 1f)
                rotationY = offsetRatio * 14f
                cameraDistance = 16f * density

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val blurPx = ((1f - smooth) * 8f).coerceAtLeast(0f)
                    renderEffect = if (blurPx > 0.5f) {
                        android.graphics.RenderEffect.createBlurEffect(
                            blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    } else null
                }
            } else {
                val scale = 0.92f + 0.08f * smooth
                scaleX = scale
                scaleY = scale
                alpha = 0.88f + 0.12f * smooth
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
 * При скролле карточки (треки, настройки, профиль) при приближении к границам:
 * 1. Буквально немного уменьшаются (scale 0.955 .. 1.0)
 * 2. Слегка затемняются (alpha 0.68 .. 1.0)
 * 3. Плавно размываются сверху и снизу (blur 0 .. 8.5px)
 * При этом строка поиска и нижний навигационный бар не затрагиваются.
 */
fun Modifier.verticalScrollEdgeItemEffect(): Modifier = composed {
    if (!com.melo.music.settings.AppSettings.moreEffects) return@composed this

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    val topBoundary = with(density) { (if (isLandscape) 56.dp else 92.dp).toPx() }
    val bottomBoundary = screenHeightPx - with(density) { (if (isLandscape) 40.dp else 96.dp).toPx() }
    val edgeZone = with(density) { 110.dp.toPx() }

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

                val topFactor = if (itemTop < topBoundary + edgeZone) {
                    ((itemTop - topBoundary) / edgeZone).coerceIn(0f, 1f)
                } else 1f

                val bottomFactor = if (itemBottom > bottomBoundary - edgeZone) {
                    ((bottomBoundary - itemBottom) / edgeZone).coerceIn(0f, 1f)
                } else 1f

                val edgeFactor = min(topFactor, bottomFactor)
                if (edgeFactor < 1f) {
                    val smooth = edgeFactor * edgeFactor * (3f - 2f * edgeFactor)
                    val s = 0.955f + 0.045f * smooth
                    scaleX = s
                    scaleY = s
                    alpha = 0.68f + 0.32f * smooth

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blurPx = (1f - smooth) * 8.5f
                        renderEffect = if (blurPx > 0.5f) {
                            android.graphics.RenderEffect.createBlurEffect(
                                blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP
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
