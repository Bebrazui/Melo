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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Verified
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.auth.DesktopAuthManager
import com.melo.desktop.auth.DesktopYouTubeAuthManager
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.storage.DesktopStorage
import com.melo.desktop.sync.DesktopYouTubeSyncManager
import com.melo.desktop.ui.components.AsyncCoverImage
import com.melo.desktop.ui.theme.MeloPrimary
import kotlinx.coroutines.launch

// ── Асимметричные выразительные формы Material 3 Expressive ──────────────────
private val HeroExpressiveShape = RoundedCornerShape(topStart = 44.dp, topEnd = 20.dp, bottomEnd = 44.dp, bottomStart = 24.dp)
private val BentoLargeShape = RoundedCornerShape(topStart = 36.dp, topEnd = 16.dp, bottomEnd = 36.dp, bottomStart = 24.dp)
private val BentoCardShapeA = RoundedCornerShape(topStart = 28.dp, topEnd = 36.dp, bottomEnd = 18.dp, bottomStart = 32.dp)
private val BentoCardShapeB = RoundedCornerShape(topStart = 20.dp, topEnd = 32.dp, bottomEnd = 32.dp, bottomStart = 20.dp)
private val TrackExpressiveShape = RoundedCornerShape(topStart = 26.dp, topEnd = 12.dp, bottomEnd = 26.dp, bottomStart = 12.dp)

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

    val topFavorite = favorites.firstOrNull() ?: history.firstOrNull()

    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }

    // Бесконечная анимация свечения и пульсации для Expressive элементов
    val infiniteTransition = rememberInfiniteTransition(label = "profileAura")
    val auraGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "auraGlow"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C110E)),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        // ── 1. Навигационная строка с Pill-кнопкой ────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }

                // Индикатор живого музыкального статуса
                Surface(
                    shape = CircleShape,
                    color = MeloPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MeloPrimary.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExpressiveWaveformIndicator(color = MeloPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnyLoggedIn) "Melo Cloud Active" else "Offline Library",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MeloPrimary,
                        )
                    }
                }
            }
        }

        // ── 2. Главный Hero-модуль с необычной органической формой ────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HeroExpressiveShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1B2E22),
                                Color(0xFF142018),
                                Color(0xFF0F1713)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 600f)
                        )
                    )
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    MeloPrimary.copy(alpha = 0.45f * auraGlow),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        ),
                        HeroExpressiveShape
                    )
                    .padding(32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Левая часть: Выразительный подиум аватара
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            // Внешний декоративный ореол асимметричной формы
                            Box(
                                modifier = Modifier
                                    .size(126.dp)
                                    .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 24.dp, bottomEnd = 44.dp, bottomStart = 24.dp))
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                MeloPrimary.copy(alpha = 0.6f),
                                                Color(0xFF60A5FA).copy(alpha = 0.4f),
                                                MeloPrimary.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                                    .padding(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(topStart = 41.dp, topEnd = 21.dp, bottomEnd = 41.dp, bottomStart = 21.dp))
                                        .background(Color(0xFF101913)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!userAvatar.isNullOrBlank()) {
                                        AsyncCoverImage(
                                            url = userAvatar,
                                            modifier = Modifier.fillMaxSize(),
                                            shape = RoundedCornerShape(topStart = 41.dp, topEnd = 21.dp, bottomEnd = 41.dp, bottomStart = 21.dp),
                                        )
                                    } else {
                                        Text(
                                            text = userName.take(1).uppercase(),
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MeloPrimary,
                                        )
                                    }
                                }
                            }

                            // Парящий бейдж верификации
                            if (isAnyLoggedIn) {
                                Surface(
                                    shape = CircleShape,
                                    color = MeloPrimary,
                                    border = BorderStroke(2.dp, Color(0xFF0F1713)),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 4.dp, y = 4.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF0A2012),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(26.dp))

                        // Информация профиля
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userName,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.8).sp,
                                    color = Color.White,
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

                                if (userEmail != null && userEmail != userHandle) {
                                    Text(
                                        text = userEmail,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.65f),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Динамическая полоса характеристик профиля
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ProfilePillBadge(
                                    icon = Icons.Rounded.Favorite,
                                    text = "${favorites.size} любимых",
                                    color = MeloPrimary,
                                )
                                ProfilePillBadge(
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    text = "${playlists.size} плейлистов",
                                    color = Color(0xFF60A5FA),
                                )
                                ProfilePillBadge(
                                    icon = Icons.Rounded.History,
                                    text = "${history.size} в истории",
                                    color = Color(0xFFFBBF24),
                                )
                            }
                        }
                    }

                    // Правая часть: Expressive Action Trigger
                    Column(horizontalAlignment = Alignment.End) {
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
                                                "Синхронизировано: +${res.likedCount} треков"
                                            }
                                        }
                                    }
                                },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MeloPrimary,
                                    disabledContainerColor = MeloPrimary.copy(alpha = 0.6f)
                                ),
                                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
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
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                            }
                        } else {
                            Button(
                                onClick = onOpenAuth,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
                                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Login,
                                    contentDescription = null,
                                    tint = Color(0xFF0A2012),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Подключить сервисы",
                                    color = Color(0xFF0A2012),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        if (syncStatusText != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = syncStatusText!!,
                                fontSize = 12.sp,
                                color = MeloPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Bento Grid: 2-колоночная компоновка с винилом и сервисами ──────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ЛЕВАЯ КОЛОНКА BENTO: Большой интерактивный виниловый тайл
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .clip(BentoLargeShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF19251E), Color(0xFF101713))
                            )
                        )
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)), BentoLargeShape)
                        .padding(26.dp)
                ) {
                    if (topFavorite != null) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MeloPrimary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Radio,
                                                contentDescription = null,
                                                tint = MeloPrimary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "ГЛАВНЫЙ ВАЙБ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        color = MeloPrimary,
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(onClick = onOpenFavorites),
                                ) {
                                    Text(
                                        text = "Моя медиатека →",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Виниловый проигрыватель: конверт + выезжающий винил
                            ExpressiveVinylSleeve(
                                track = topFavorite,
                                onPlay = { onPlayTrack(topFavorite) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = topFavorite.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = topFavorite.uploader ?: "Неизвестный исполнитель",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        // Если нет треков — экспрессивный призыв
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MeloPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(64.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.LibraryMusic,
                                        contentDescription = null,
                                        tint = MeloPrimary,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Медиатека ждёт музыки",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Войдите в YouTube Music или добавьте треки в любимые, чтобы оживить профиль",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // ПРАВАЯ КОЛОНКА BENTO: Тайлы быстрой статистики и сервисный хаб
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // Тайл 1: Коллекция треков с мини-стеком обложек
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BentoCardShapeA)
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), BentoCardShapeA)
                            .clickable(onClick = onOpenFavorites)
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Любимые треки",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = MeloPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${favorites.size}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp,
                                    color = Color.White,
                                )

                                // Стек из 3-х обложек с наложением
                                if (favorites.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        favorites.take(3).forEachIndexed { index, track ->
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = (-10 * index).dp)
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, Color(0xFF16221A), CircleShape)
                                            ) {
                                                AsyncCoverImage(
                                                    url = track.thumbnailUrl,
                                                    modifier = Modifier.fillMaxSize(),
                                                    shape = CircleShape,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Тайл 2: Сплит плейлистов и истории
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Плейлисты
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(22.dp))
                                .clickable(onClick = onOpenPlaylists)
                                .padding(18.dp)
                        ) {
                            Column {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${playlists.size}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Плейлисты",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        // История
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(22.dp))
                                .clickable(onClick = onOpenHistory)
                                .padding(18.dp)
                        ) {
                            Column {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${history.size}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                )
                                Text(
                                    text = "История",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    // Тайл 3: Сервисный хаб (YouTube Music интеграция)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BentoCardShapeB)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF221616).copy(alpha = 0.5f),
                                        Color(0xFF141916).copy(alpha = 0.6f)
                                    )
                                )
                            )
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), BentoCardShapeB)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.16f),
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.LibraryMusic,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "YouTube Music",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = if (isYtLoggedIn) "Подключен и активен" else "Не авторизован",
                                        fontSize = 12.sp,
                                        color = if (isYtLoggedIn) MeloPrimary else Color.White.copy(alpha = 0.55f),
                                    )
                                }
                            }

                            if (isYtLoggedIn) {
                                IconButton(
                                    onClick = {
                                        DesktopYouTubeAuthManager.logout()
                                        syncStatusText = null
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.06f)),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = "Выйти",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MeloPrimary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(onClick = onOpenAuth),
                                ) {
                                    Text(
                                        text = "Войти",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0A2012),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 4. Полка любимых треков с виниловыми скруглениями ──────────────────
        if (favorites.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Коллекция",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = CircleShape,
                            color = MeloPrimary.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "${favorites.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MeloPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenFavorites),
                    ) {
                        Text(
                            text = "Смотреть все",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MeloPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(favorites.take(16)) { track ->
                        ExpressiveTrackCard(
                            track = track,
                            onClick = { onPlayTrack(track) }
                        )
                    }
                }
            }
        }

        // ── 5. Полка плейлистов с асимметричными карточками-кассетами ─────────
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
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF60A5FA),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(playlists.keys.toList()) { name ->
                        val pTracks = playlists[name] ?: emptyList()
                        val coverUrl = pTracks.firstOrNull()?.thumbnailUrl

                        ExpressivePlaylistTile(
                            name = name,
                            trackCount = pTracks.size,
                            coverUrl = coverUrl,
                            onClick = {
                                pTracks.firstOrNull()?.let { onPlayTrack(it) }
                            }
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

// ── Вспомогательные выразительные компоненты ─────────────────────────────────

/** Виниловый конверт с выезжающей пластинкой */
@Composable
private fun ExpressiveVinylSleeve(
    track: TrackItem,
    onPlay: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val discOffset by animateDpAsState(
        targetValue = if (isHovered) 56.dp else 24.dp,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "discOffset"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPlay
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Выезжающий виниловый диск
        Box(
            modifier = Modifier
                .offset(x = 120.dp + discOffset)
                .size(140.dp)
                .graphicsLayer {
                    rotationZ = if (isHovered) rotation else 0f
                }
                .clip(CircleShape)
                .background(Color(0xFF0F0F11))
                .border(1.5.dp, Color(0xFF26262B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Концентрические звуковые дорожки винила
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = Color.White.copy(alpha = 0.08f), radius = size.width * 0.42f, center = center, style = Stroke(width = 1f))
                drawCircle(color = Color.White.copy(alpha = 0.08f), radius = size.width * 0.35f, center = center, style = Stroke(width = 1f))
                drawCircle(color = Color.White.copy(alpha = 0.08f), radius = size.width * 0.28f, center = center, style = Stroke(width = 1f))
            }

            // Центральное отверстие яблока винила
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MeloPrimary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0C110E))
                )
            }
        }

        // Конверт пластинки (Sleeve)
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 14.dp, bottomEnd = 26.dp, bottomStart = 14.dp))
                .background(Color(0xFF1F2E25))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 26.dp, topEnd = 14.dp, bottomEnd = 26.dp, bottomStart = 14.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncCoverImage(
                url = track.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 14.dp, bottomEnd = 26.dp, bottomStart = 14.dp)
            )

            // Кнопка плей поверх конверта
            Surface(
                shape = CircleShape,
                color = MeloPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { scaleX = if (isHovered) 1.08f else 1f; scaleY = if (isHovered) 1.08f else 1f }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF0A2012),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

/** Карточка трека в коллекции с асимметричными углами */
@Composable
private fun ExpressiveTrackCard(
    track: TrackItem,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.04f else 1f, label = "trackScale")

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
            )
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(TrackExpressiveShape)
                .background(Color(0xFF1B2820))
                .border(
                    BorderStroke(1.dp, if (isHovered) MeloPrimary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)),
                    TrackExpressiveShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncCoverImage(
                url = track.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                shape = TrackExpressiveShape,
            )

            if (isHovered) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.38f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MeloPrimary,
                        modifier = Modifier.size(44.dp)
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

        Spacer(modifier = Modifier.height(8.dp))

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

/** Плейлист-кассета с ярким акцентным заголовком */
@Composable
private fun ExpressivePlaylistTile(
    name: String,
    trackCount: Int,
    coverUrl: String?,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.03f else 1f, label = "plScale")

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 14.dp, bottomEnd = 28.dp, bottomStart = 14.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, if (isHovered) Color(0xFF60A5FA).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .width(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 14.dp, bottomEnd = 28.dp, bottomStart = 14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 10.dp, bottomEnd = 22.dp, bottomStart = 10.dp))
                    .background(Color(0xFF1E2822)),
                contentAlignment = Alignment.Center
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncCoverImage(
                        url = coverUrl,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 10.dp, bottomEnd = 22.dp, bottomStart = 10.dp)
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$trackCount треков",
                fontSize = 12.sp,
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Pill-бейдж в шапке профиля */
@Composable
private fun ProfilePillBadge(
    icon: ImageVector,
    text: String,
    color: Color,
) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

/** Анимированный индикатор звуковой волны для Expressive шапки */
@Composable
private fun ExpressiveWaveformIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "wave")
    val h1 by transition.animateFloat(initialValue = 4f, targetValue = 14f, animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "h1")
    val h2 by transition.animateFloat(initialValue = 12f, targetValue = 5f, animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "h2")
    val h3 by transition.animateFloat(initialValue = 6f, targetValue = 16f, animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "h3")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(16.dp)
    ) {
        Box(modifier = Modifier.width(2.5.dp).height(h1.dp).clip(CircleShape).background(color))
        Box(modifier = Modifier.width(2.5.dp).height(h2.dp).clip(CircleShape).background(color))
        Box(modifier = Modifier.width(2.5.dp).height(h3.dp).clip(CircleShape).background(color))
    }
}
