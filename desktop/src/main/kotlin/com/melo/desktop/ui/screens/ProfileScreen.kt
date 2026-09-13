package com.melo.desktop.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.MeloAppController
import com.melo.desktop.audio.DesktopAudioPlayer
import com.melo.desktop.auth.DesktopAuthManager
import com.melo.desktop.auth.DesktopYouTubeAuthManager
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.storage.DesktopStorage
import com.melo.desktop.sync.DesktopYouTubeSyncManager
import com.melo.desktop.ui.components.AsyncCoverImage
import com.melo.desktop.ui.components.WavySlider
import com.melo.desktop.ui.theme.MeloPrimary
import kotlinx.coroutines.launch
import java.util.Calendar

// ── Фирменные формы Material 3 Expressive (Google Design System) ──────────────

/**
 * Каноничный 12-лепестковый цветок / облако (M3 Scalloped Flower Shape)
 * В точности как на официальных промо-материалах Google:
 * «Serafina» cloud mask, M3 Expressive Clock widget, Lime Reward starburst.
 */
class ScallopedFlowerShape(val lobes: Int = 12, val depth: Float = 0.16f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rOuter = minOf(cx, cy)
        val rInner = rOuter * (1f - depth)
        val angleStep = (2.0 * Math.PI / lobes).toFloat()

        for (i in 0 until lobes) {
            val theta = i * angleStep
            val nextTheta = (i + 1) * angleStep

            val v1X = cx + rInner * kotlin.math.cos(theta)
            val v1Y = cy + rInner * kotlin.math.sin(theta)
            val v2X = cx + rInner * kotlin.math.cos(nextTheta)
            val v2Y = cy + rInner * kotlin.math.sin(nextTheta)

            if (i == 0) {
                path.moveTo(v1X, v1Y)
            }

            val cp1X = cx + rOuter * 1.08f * kotlin.math.cos(theta + angleStep * 0.25f)
            val cp1Y = cy + rOuter * 1.08f * kotlin.math.sin(theta + angleStep * 0.25f)
            val cp2X = cx + rOuter * 1.08f * kotlin.math.cos(theta + angleStep * 0.75f)
            val cp2Y = cy + rOuter * 1.08f * kotlin.math.sin(theta + angleStep * 0.75f)

            path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, v2X, v2Y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Асимметричная каплевидная форма погодного виджета M3 (Image 2) */
private val TeardropWidgetShape = RoundedCornerShape(
    topStart = 44.dp,
    topEnd = 44.dp,
    bottomStart = 10.dp,
    bottomEnd = 44.dp
)

/** Выразительные контейнеры с повышенными радиусами скругления */
private val ExpressiveHeroShape = RoundedCornerShape(36.dp)
private val ExpressiveTileShape = RoundedCornerShape(28.dp)

// ── Цветовая палитра M3 Expressive ──────────────────────────────────────────
private val M3ElectricLime = Color(0xFFDFFF4F)      // Фирменный неоновый лайм кнопки PLAY
private val M3DeepViolet = Color(0xFF43346B)        // Глубокий фиолетовый из Serafina
private val M3CardSurface = Color(0xFF141D17)       // Тёмно-хвойный M3 Surface
private val M3SurfaceHigh = Color(0xFF1D2921)       // Tonal Container High
private val M3SurfaceHighest = Color(0xFF26372C)    // Tonal Container Highest

@Composable
fun ProfileScreen(
    onPlayTrack: (TrackItem) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAuth: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isYtLoggedIn = DesktopYouTubeAuthManager.isLoggedIn
    val isMeloLoggedIn = DesktopAuthManager.isLoggedIn
    val isAnyLoggedIn = isYtLoggedIn || isMeloLoggedIn

    val userName = DesktopYouTubeAuthManager.accountName
        ?: DesktopAuthManager.name
        ?: "Пользователь Melo"
    val userAvatar = DesktopYouTubeAuthManager.accountAvatarUrl
        ?: DesktopAuthManager.avatarUrl
    val userHandle = DesktopYouTubeAuthManager.accountHandle
    val userEmail = DesktopAuthManager.email ?: DesktopYouTubeAuthManager.accountEmail

    val favorites = DesktopStorage.favorites
    val history = DesktopStorage.history
    val playlists = DesktopStorage.playlists.value

    val currentTrack = DesktopAudioPlayer.currentTrack
    val isPlaying = DesktopAudioPlayer.isPlaying
    val displayTrack = if (currentTrack != null) {
        TrackItem(
            title = currentTrack.title,
            uploader = currentTrack.artist,
            url = currentTrack.originalUrl,
            durationSeconds = DesktopAudioPlayer.durationMs / 1000,
            thumbnailUrl = currentTrack.thumbnailUrl,
            source = currentTrack.source,
        )
    } else {
        favorites.firstOrNull() ?: history.firstOrNull()
    }

    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }
    var selectedFilterIndex by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D0A)),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // ── 1. Верхняя панель: Expressive Pill навигации и статус ──────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Pill-кнопка Назад
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Главная",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                // Цветочный Scallop-бейдж верификации и статус
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MeloPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MeloPrimary.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isAnyLoggedIn) MeloPrimary else Color.White.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAnyLoggedIn) "Melo Cloud Active" else "Offline Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAnyLoggedIn) MeloPrimary else Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        // ── 2. Hero-шапка профиля: Цветочный Scallop-аватар и крупная типографика ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ExpressiveHeroShape)
                    .background(M3CardSurface)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), ExpressiveHeroShape)
                    .padding(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Аватар в каноничном 12-лепестковом цветке M3
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(ScallopedFlowerShape(lobes = 12))
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        MeloPrimary,
                                        Color(0xFF60A5FA),
                                        M3ElectricLime,
                                        MeloPrimary
                                    )
                                )
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(ScallopedFlowerShape(lobes = 12))
                                .background(Color(0xFF0F1511)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!userAvatar.isNullOrBlank()) {
                                AsyncCoverImage(
                                    url = userAvatar,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = ScallopedFlowerShape(lobes = 12),
                                )
                            } else {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MeloPrimary,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(26.dp))

                    // Выразительная типографика имени (в стиле «ELENA» и «Serafina»)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userName,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            // Фирменный 12-лепестковый бейдж верификации
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(ScallopedFlowerShape(lobes = 12))
                                    .background(MeloPrimary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF0A2012),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (userHandle != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MeloPrimary.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        text = userHandle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MeloPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }

                            Text(
                                text = userEmail ?: "Melo Account",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f),
                            )

                            // Стрелочка ссылки как в Image 3 (Echo Bridge ↗)
                            Icon(
                                imageVector = Icons.Rounded.NorthEast,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                // Кнопка синхронизации / подключения в форме Expressive Pill
                if (isYtLoggedIn) {
                    Button(
                        onClick = {
                            if (!isSyncing) {
                                isSyncing = true
                                syncStatusText = "Синхронизация..."
                                scope.launch {
                                    val res = DesktopYouTubeSyncManager.syncLibrary { msg ->
                                        syncStatusText = msg
                                    }
                                    isSyncing = false
                                    syncStatusText = if (res.error != null) {
                                        "Ошибка: ${res.error}"
                                    } else {
                                        "Добавлено: +${res.likedCount} треков"
                                    }
                                }
                            }
                        },
                        enabled = !isSyncing,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF0A2012),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null,
                                tint = Color(0xFF0A2012),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSyncing) "Синхронизация..." else "Синхронизировать",
                            color = Color(0xFF0A2012),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    Button(
                        onClick = onOpenAuth,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Login,
                            contentDescription = null,
                            tint = Color(0xFF0A2012),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Подключить аккаунт",
                            color = Color(0xFF0A2012),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        // ── 3. Bento Matrix: «Serafina» Player Stage + «M3 Expressive Widgets» ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                // ── ЛЕВАЯ КОЛОНКА: Сцена «Serafina» (в точности как в Image 1) ─────
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .clip(ExpressiveHeroShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(M3DeepViolet, Color(0xFF1E1731))
                            )
                        )
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), ExpressiveHeroShape)
                        .padding(26.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // 1. Цветочное облако с обложкой трека (Scallop Cloud Mask из Image 1)
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(ScallopedFlowerShape(lobes = 12, depth = 0.18f))
                                .background(Color(0xFF32244F)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (displayTrack?.thumbnailUrl != null) {
                                AsyncCoverImage(
                                    url = displayTrack.thumbnailUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = ScallopedFlowerShape(lobes = 12, depth = 0.18f),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = M3ElectricLime,
                                    modifier = Modifier.size(54.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 2. Выразительный заголовок трека в стиле Serafina
                        Text(
                            text = displayTrack?.title ?: "Выбери музыку",
                            fontSize = 28.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.ExtraBold,
                            color = M3ElectricLime,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = displayTrack?.uploader ?: "Melo Music Player",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Гигантская Pill-кнопка PLAY и круги переключения (Image 1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            // Кнопка Назад
                            Surface(
                                shape = CircleShape,
                                color = M3ElectricLime,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { MeloAppController.playPrev() },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Предыдущий",
                                        tint = Color(0xFF0A2012),
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Главная гигантская Pill-кнопка «PLAY / PAUSE»
                            Surface(
                                shape = CircleShape,
                                color = M3ElectricLime,
                                modifier = Modifier
                                    .height(56.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (displayTrack != null) {
                                            if (currentTrack?.originalUrl == displayTrack.url) {
                                                DesktopAudioPlayer.togglePlayPause()
                                            } else {
                                                onPlayTrack(displayTrack)
                                            }
                                        }
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 36.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF0A2012),
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isPlaying) "PAUSE" else "PLAY",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        color = Color(0xFF0A2012),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Кнопка Вперёд
                            Surface(
                                shape = CircleShape,
                                color = M3ElectricLime,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { MeloAppController.playNext() },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Следующий",
                                        tint = Color(0xFF0A2012),
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Фирменный синусоидальный Wavy Slider (скругленный волнистый скраббер из Image 1)
                        val dur = DesktopAudioPlayer.durationMs.toFloat().coerceAtLeast(1f)
                        val pos = DesktopAudioPlayer.currentPositionMs.toFloat()
                        val prog = (pos / dur).coerceIn(0f, 1f)

                        WavySlider(
                            value = prog,
                            onValueChange = { f ->
                                DesktopAudioPlayer.seekTo((f * dur).toLong())
                            },
                            isPlaying = isPlaying,
                            activeColor = M3ElectricLime,
                            inactiveColor = Color.White.copy(alpha = 0.25f),
                            thumbColor = M3ElectricLime,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        )
                    }
                }

                // ── ПРАВАЯ КОЛОНКА: Сетка виджетов «M3 Expressive Widgets» (Image 2) ──
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Виджет 1: Сплит-капсула статистики (в точности как «13 Wed - 14 AM - 15 Sat» в Image 2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveTileShape)
                            .background(M3CardSurface)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), ExpressiveTileShape)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Капсула 1: Любимые
                            ExpressiveSplitPill(
                                count = "${favorites.size}",
                                label = "LIKED",
                                icon = Icons.Rounded.Favorite,
                                isAccent = true,
                                onClick = onOpenFavorites,
                                modifier = Modifier.weight(1f),
                            )

                            // Капсула 2: Плейлисты
                            ExpressiveSplitPill(
                                count = "${playlists.size}",
                                label = "LISTS",
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                isAccent = false,
                                onClick = onOpenPlaylists,
                                modifier = Modifier.weight(1f),
                            )

                            // Капсула 3: История
                            ExpressiveSplitPill(
                                count = "${history.size}",
                                label = "HISTORY",
                                icon = Icons.Rounded.History,
                                isAccent = false,
                                onClick = onOpenHistory,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Виджет 2: Асимметричный каплевидный виджет статуса (по образцу 23° weather в Image 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Каплевидный виджет (Teardrop shape)
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(TeardropWidgetShape)
                                .background(M3SurfaceHigh)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), TeardropWidgetShape)
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    // 12-лепестковые M3 часы
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(ScallopedFlowerShape(lobes = 12))
                                            .background(MeloPrimary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = null,
                                            tint = Color(0xFF0A2012),
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }

                                    val cal = Calendar.getInstance()
                                    val timeStr = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                                    Text(
                                        text = timeStr,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (isYtLoggedIn) "YouTube Sync" else "Local Library",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )

                                Text(
                                    text = if (isYtLoggedIn) "Стриминг активен" else "Авторизуйтесь в YouTube",
                                    fontSize = 12.sp,
                                    color = if (isYtLoggedIn) MeloPrimary else Color.White.copy(alpha = 0.6f),
                                )
                            }
                        }

                        // Виджет 3: Quick Apps / Controls (3x3 grid как в Image 2)
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(ExpressiveTileShape)
                                .background(M3CardSurface)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), ExpressiveTileShape)
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    ExpressiveMiniIconBtn(
                                        icon = Icons.Rounded.Shuffle,
                                        active = DesktopAudioPlayer.isShuffle,
                                        onClick = { DesktopAudioPlayer.toggleShuffle() }
                                    )
                                    ExpressiveMiniIconBtn(
                                        icon = Icons.Rounded.Repeat,
                                        active = DesktopAudioPlayer.repeatMode != DesktopAudioPlayer.RepeatMode.OFF,
                                        onClick = { DesktopAudioPlayer.toggleRepeatMode() }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    ExpressiveMiniIconBtn(
                                        icon = Icons.Rounded.Sync,
                                        active = isSyncing,
                                        onClick = {
                                            if (!isSyncing && isYtLoggedIn) {
                                                isSyncing = true
                                                scope.launch {
                                                    DesktopYouTubeSyncManager.syncLibrary()
                                                    isSyncing = false
                                                }
                                            } else if (!isYtLoggedIn) {
                                                onOpenAuth()
                                            }
                                        }
                                    )
                                    ExpressiveMiniIconBtn(
                                        icon = if (isYtLoggedIn) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login,
                                        active = isYtLoggedIn,
                                        onClick = {
                                            if (isYtLoggedIn) DesktopYouTubeAuthManager.logout() else onOpenAuth()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 4. Полоса фильтров (Image 3 «THURS - FRI - SAT - SUN» Pill Bar) ────
        item {
            val filters = listOf("ВСЕ ТРЕКИ", "ИЗБРАННОЕ", "ПЛЕЙЛИСТЫ", "ИСТОРИЯ")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                filters.forEachIndexed { idx, label ->
                    val isSelected = selectedFilterIndex == idx
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MeloPrimary else Color.Transparent,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) MeloPrimary else Color.White.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                selectedFilterIndex = idx
                                when (idx) {
                                    1 -> onOpenFavorites()
                                    2 -> onOpenPlaylists()
                                    3 -> onOpenHistory()
                                }
                            }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color(0xFF0A2012) else Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }

        // ── 5. Expressive Полка треков в цветочных Scallop-формах ─────────────
        if (favorites.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Избранные треки",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Color.White,
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenFavorites),
                    ) {
                        Text(
                            text = "Все ${favorites.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MeloPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(favorites.take(16)) { track ->
                        ExpressiveFlowerTrackCard(
                            track = track,
                            onClick = { onPlayTrack(track) }
                        )
                    }
                }
            }
        }

        // ── 6. Expressive Полка плейлистов ────────────────────────────────────
        if (playlists.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Плейлисты",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Color.White,
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenPlaylists),
                    ) {
                        Text(
                            text = "Все ${playlists.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(playlists.keys.toList()) { name ->
                        val pTracks = playlists[name] ?: emptyList()
                        ExpressivePlaylistCard(
                            name = name,
                            count = pTracks.size,
                            coverUrl = pTracks.firstOrNull()?.thumbnailUrl,
                            onClick = { pTracks.firstOrNull()?.let(onPlayTrack) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Вспомогательные M3 Expressive виджеты ────────────────────────────────────

/**
 * Сплит-капсула (Connected Pill Widget из Image 2):
 * Высокий радиус скругления, крупное число, иконка и лейбл.
 */
@Composable
private fun ExpressiveSplitPill(
    count: String,
    label: String,
    icon: ImageVector,
    isAccent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isAccent) MeloPrimary else M3SurfaceHigh
    val fg = if (isAccent) Color(0xFF0A2012) else Color.White
    val subFg = if (isAccent) Color(0xFF1B3824) else Color.White.copy(alpha = 0.65f)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = bg,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = fg,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = subFg,
            )
        }
    }
}

/** Мини-кнопка из сетки 3x3 (Image 2) */
@Composable
private fun ExpressiveMiniIconBtn(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (active) MeloPrimary else M3SurfaceHigh,
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Color(0xFF0A2012) else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Карточка трека в Scallop Flower маске */
@Composable
private fun ExpressiveFlowerTrackCard(
    track: TrackItem,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.05f else 1f, label = "cardScale")

    Column(
        modifier = Modifier
            .width(148.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        // Scalloped 12-лепестковая маска обложки
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(ScallopedFlowerShape(lobes = 12, depth = 0.14f))
                .background(M3SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            AsyncCoverImage(
                url = track.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                shape = ScallopedFlowerShape(lobes = 12, depth = 0.14f),
            )

            if (isHovered) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MeloPrimary,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF0A2012),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = track.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        track.uploader?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Карточка плейлиста */
@Composable
private fun ExpressivePlaylistCard(
    name: String,
    count: Int,
    coverUrl: String?,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = M3CardSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(M3SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncCoverImage(
                        url = coverUrl,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$count треков",
                fontSize = 12.sp,
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
