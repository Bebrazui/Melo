package com.melo.desktop.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.melo.desktop.ui.theme.MeloCardSurface
import com.melo.desktop.ui.theme.MeloDarkBackground
import com.melo.desktop.ui.theme.MeloDarkSurface
import com.melo.desktop.ui.theme.MeloOnPrimary
import com.melo.desktop.ui.theme.MeloPrimary
import com.melo.desktop.ui.theme.MeloTextSub
import com.melo.desktop.ui.theme.MeloTextWhite
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 12-лепестковая фигура M3 Expressive для бейджей и декоративных элементов */
class ScallopedFlowerShape(
    private val lobes: Int = 12,
    private val depth: Float = 0.12f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rBase = kotlin.math.min(cx, cy)
        val rOuter = rBase
        val rInner = rBase * (1f - depth)

        val angleStep = (2.0 * Math.PI / lobes).toFloat()

        for (i in 0 until lobes) {
            val theta = i * angleStep
            val nextTheta = (i + 1) * angleStep

            val v1X = cx + rOuter * kotlin.math.cos(theta)
            val v1Y = cy + rOuter * kotlin.math.sin(theta)

            val v2X = cx + rOuter * kotlin.math.cos(nextTheta)
            val v2Y = cy + rOuter * kotlin.math.sin(nextTheta)

            if (i == 0) {
                path.moveTo(v1X, v1Y)
            }

            val cp1X = cx + rOuter * 1.05f * kotlin.math.cos(theta + angleStep * 0.25f)
            val cp1Y = cy + rOuter * 1.05f * kotlin.math.sin(theta + angleStep * 0.25f)
            val cp2X = cx + rOuter * 1.05f * kotlin.math.cos(theta + angleStep * 0.75f)
            val cp2Y = cy + rOuter * 1.05f * kotlin.math.sin(theta + angleStep * 0.75f)

            path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, v2X, v2Y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Асимметричная каплевидная форма погодного/статусного виджета M3 */
private val TeardropWidgetShape = RoundedCornerShape(
    topStart = 38.dp,
    topEnd = 38.dp,
    bottomStart = 12.dp,
    bottomEnd = 38.dp
)

private val ExpressiveHeroShape = RoundedCornerShape(32.dp)
private val ExpressiveTileShape = RoundedCornerShape(24.dp)
private val ExpressiveCoverShape = RoundedCornerShape(24.dp)

// ── Гармоничная хвойно-мятная палитра Melo (Material 3 Expressive) ───────────
private val SurfaceCanvas = Color(0xFF0F1411)        // Глубокий тёмно-хвойный фон
private val CardSurface = Color(0xFF161F1A)          // Основной контейнер виджетов
private val ElevatedSurface = Color(0xFF1D2821)      // Приподнятый контейнер
private val HighestSurface = Color(0xFF24332A)       // Верхний контейнер для кнопок
private val BorderSubtle = Color.White.copy(alpha = 0.08f)
private val BorderActive = MeloPrimary.copy(alpha = 0.35f)

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

    val currentTimeStr = remember {
        try {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            "12:00"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceCanvas),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── 1. Верхняя панель: Навигация ─────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = CircleShape,
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderSubtle),
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
                            tint = MeloTextWhite,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Назад к музыке",
                            color = MeloTextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isAnyLoggedIn) MeloPrimary.copy(alpha = 0.12f) else CardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isAnyLoggedIn) MeloPrimary.copy(alpha = 0.35f) else BorderSubtle
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isAnyLoggedIn) MeloPrimary else Color(0xFFE57373))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isYtLoggedIn) "YouTube Connected"
                            else if (isMeloLoggedIn) "Melo Cloud Active"
                            else "Автономный режим",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAnyLoggedIn) MeloPrimary else MeloTextSub,
                        )
                    }
                }
            }
        }

        // ── 2. Карточка профиля ──────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ExpressiveHeroShape)
                    .background(CardSurface)
                    .border(BorderStroke(1.dp, BorderSubtle), ExpressiveHeroShape)
                    .padding(26.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Аватар пользователя (аккуратный контур с тонкой мятной каймой)
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .background(ElevatedSurface)
                                .border(BorderStroke(2.dp, MeloPrimary.copy(alpha = 0.4f)), RoundedCornerShape(30.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!userAvatar.isNullOrBlank()) {
                                AsyncCoverImage(
                                    url = userAvatar,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(30.dp),
                                )
                            } else {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MeloPrimary,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(22.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userName,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    color = MeloTextWhite,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                // Бейдж подтверждения
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(ScallopedFlowerShape(lobes = 12, depth = 0.12f))
                                        .background(MeloPrimary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Verified",
                                        tint = MeloOnPrimary,
                                        modifier = Modifier.size(13.dp),
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
                                        color = MeloPrimary.copy(alpha = 0.12f),
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
                                    color = MeloTextSub,
                                )

                                Icon(
                                    imageVector = Icons.Rounded.NorthEast,
                                    contentDescription = null,
                                    tint = MeloTextSub.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    // Кнопка синхронизации или входа
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
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp),
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MeloOnPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = MeloOnPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSyncing) "Синхронизация..." else "Синхронизировать",
                                color = MeloOnPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        Button(
                            onClick = onOpenAuth,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Login,
                                contentDescription = null,
                                tint = MeloOnPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Подключить аккаунт",
                                color = MeloOnPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Bento Matrix: Плеер + Виджеты библиотеки ──────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ── ЛЕВАЯ КОЛОНКА: Сцена плеера ──────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .clip(ExpressiveHeroShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1B2720), Color(0xFF141C16))
                            )
                        )
                        .border(BorderStroke(1.dp, BorderSubtle), ExpressiveHeroShape)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // 1. КРУПНАЯ, ЧЁТКАЯ ОБЛОЖКА ТРЕКА (220dp, выразительное скругление 24dp)
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(ExpressiveCoverShape)
                                .background(ElevatedSurface)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), ExpressiveCoverShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (displayTrack?.thumbnailUrl != null) {
                                AsyncCoverImage(
                                    url = displayTrack.thumbnailUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = ExpressiveCoverShape,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = MeloPrimary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(64.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 2. Название трека и артист
                        Text(
                            text = displayTrack?.title ?: "Выбери музыку",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MeloTextWhite,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = displayTrack?.uploader ?: "Melo Music Player",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MeloTextSub,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. Wavy Slider (синусоидальный волнистый скраббер)
                        val dur = DesktopAudioPlayer.durationMs.toFloat().coerceAtLeast(1f)
                        val pos = DesktopAudioPlayer.currentPositionMs.toFloat()
                        val prog = (pos / dur).coerceIn(0f, 1f)

                        WavySlider(
                            value = prog,
                            onValueChange = { f ->
                                DesktopAudioPlayer.seekTo((f * dur).toLong())
                            },
                            isPlaying = isPlaying,
                            activeColor = MeloPrimary,
                            inactiveColor = Color.White.copy(alpha = 0.15f),
                            thumbColor = MeloPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                        )

                        // Строка времени
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val curSec = (pos / 1000).toLong()
                            val durSec = (dur / 1000).toLong()
                            Text(
                                text = String.format("%02d:%02d", curSec / 60, curSec % 60),
                                fontSize = 11.sp,
                                color = MeloTextSub,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = String.format("%02d:%02d", durSec / 60, durSec % 60),
                                fontSize = 11.sp,
                                color = MeloTextSub,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Элементы управления: Skip buttons + Главная Pill-кнопка PLAY
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            // Кнопка Предыдущий
                            Surface(
                                shape = CircleShape,
                                color = HighestSurface,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable { MeloAppController.playPrev() },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Предыдущий",
                                        tint = MeloTextWhite,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Главная кнопка PLAY / PAUSE (фирменная мятная пилюля)
                            Surface(
                                shape = CircleShape,
                                color = MeloPrimary,
                                modifier = Modifier
                                    .height(52.dp)
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
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = MeloOnPrimary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPlaying) "PAUSE" else "PLAY",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp,
                                        color = MeloOnPrimary,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Кнопка Следующий
                            Surface(
                                shape = CircleShape,
                                color = HighestSurface,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable { MeloAppController.playNext() },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Следующий",
                                        tint = MeloTextWhite,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // ── ПРАВАЯ КОЛОНКА: Сетка виджетов M3 Expressive ─────────────
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Виджет 1: Connected Split-Pill статистики
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveTileShape)
                            .background(CardSurface)
                            .border(BorderStroke(1.dp, BorderSubtle), ExpressiveTileShape)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ExpressiveSplitPill(
                                count = "${favorites.size}",
                                label = "LIKED",
                                icon = Icons.Rounded.Favorite,
                                onClick = onOpenFavorites,
                                modifier = Modifier.weight(1f),
                            )

                            ExpressiveSplitPill(
                                count = "${playlists.size}",
                                label = "LISTS",
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                onClick = onOpenPlaylists,
                                modifier = Modifier.weight(1f),
                            )

                            ExpressiveSplitPill(
                                count = "${history.size}",
                                label = "HISTORY",
                                icon = Icons.Rounded.History,
                                onClick = onOpenHistory,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Виджет 2: Teardrop статус виджет + Быстрые действия
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Каплевидный виджет статуса
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(TeardropWidgetShape)
                                .background(CardSurface)
                                .border(BorderStroke(1.dp, BorderSubtle), TeardropWidgetShape)
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(ScallopedFlowerShape(lobes = 12, depth = 0.12f))
                                            .background(MeloPrimary.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = null,
                                            tint = MeloPrimary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }

                                    Text(
                                        text = currentTimeStr,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MeloTextWhite,
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "YouTube Sync",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MeloTextWhite,
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = syncStatusText ?: if (isYtLoggedIn) "Библиотека активна" else "Ожидание входа",
                                    fontSize = 12.sp,
                                    color = if (isYtLoggedIn) MeloPrimary else MeloTextSub,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        // Кластер быстрых действий 2x2
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(ExpressiveTileShape)
                                .background(CardSurface)
                                .border(BorderStroke(1.dp, BorderSubtle), ExpressiveTileShape)
                                .padding(14.dp)
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
                                        icon = if (isAnyLoggedIn) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login,
                                        active = false,
                                        onClick = {
                                            if (isAnyLoggedIn) {
                                                DesktopYouTubeAuthManager.logout()
                                                scope.launch { DesktopAuthManager.logout() }
                                            } else {
                                                onOpenAuth()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 4. Горизонтальная лента фильтров-капсул ─────────────────────────
        item {
            val filters = listOf("Все треки", "Любимые", "Плейлисты", "История")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                filters.forEachIndexed { idx, label ->
                    val isSelected = selectedFilterIndex == idx
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MeloPrimary else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MeloPrimary else BorderSubtle
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
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MeloOnPrimary else MeloTextWhite,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        )
                    }
                }
            }
        }

        // ── 5. Полка избранных треков ─────────────────────────────────────────
        if (favorites.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Избранные треки",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MeloTextWhite,
                    )

                    Surface(
                        shape = CircleShape,
                        color = ElevatedSurface,
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
                        ExpressiveTrackCard(
                            track = track,
                            onClick = { onPlayTrack(track) }
                        )
                    }
                }
            }
        }

        // ── 6. Полка плейлистов ──────────────────────────────────────────────
        if (playlists.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Плейлисты",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MeloTextWhite,
                    )

                    Surface(
                        shape = CircleShape,
                        color = ElevatedSurface,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenPlaylists),
                    ) {
                        Text(
                            text = "Все ${playlists.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MeloPrimary,
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

// ── Вспомогательные гармоничные M3 Expressive виджеты ────────────────────────

@Composable
private fun ExpressiveSplitPill(
    count: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isHovered) HighestSurface else ElevatedSurface,
        border = BorderStroke(1.dp, if (isHovered) BorderActive else BorderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MeloPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MeloTextWhite,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = MeloTextSub,
            )
        }
    }
}

@Composable
private fun ExpressiveMiniIconBtn(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
            active -> MeloPrimary.copy(alpha = 0.22f)
            isHovered -> HighestSurface
            else -> ElevatedSurface
        },
        border = BorderStroke(
            1.dp,
            if (active) MeloPrimary.copy(alpha = 0.45f) else BorderSubtle
        ),
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) MeloPrimary else MeloTextWhite,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ExpressiveTrackCard(
    track: TrackItem,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.04f else 1f, label = "cardScale")

    Column(
        modifier = Modifier
            .width(140.dp)
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
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ElevatedSurface)
                .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncCoverImage(
                url = track.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
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
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MeloOnPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MeloTextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        track.uploader?.let {
            Text(
                text = it,
                fontSize = 11.sp,
                color = MeloTextSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExpressivePlaylistCard(
    name: String,
    count: Int,
    coverUrl: String?,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.04f else 1f, label = "plScale")

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .width(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElevatedSurface),
                contentAlignment = Alignment.Center,
            ) {
                if (coverUrl != null) {
                    AsyncCoverImage(
                        url = coverUrl,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(14.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MeloPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MeloTextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$count треков",
                fontSize = 11.sp,
                color = MeloPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
