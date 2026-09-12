package com.melo.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.auth.DesktopAuthManager
import com.melo.desktop.auth.DesktopYouTubeAuthManager
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.storage.DesktopStorage
import com.melo.desktop.sync.DesktopYouTubeSyncManager
import com.melo.desktop.ui.components.AsyncCoverImage
import com.melo.desktop.ui.theme.MeloCardSurface
import com.melo.desktop.ui.theme.MeloPrimary
import com.melo.desktop.ui.theme.MeloSurfaceVariant
import kotlinx.coroutines.launch

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

    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // ── 1. Верхняя панель навигации ─────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Профиль",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        // ── 2. Hero карточка пользователя (крупный аватар с двойным кольцом) ─────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(32.dp))
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Аватар с двойным кольцом и мягким свечением
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(BorderStroke(2.5.dp, MeloPrimary.copy(alpha = 0.55f)), CircleShape)
                        .padding(7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!userAvatar.isNullOrBlank()) {
                        AsyncCoverImage(
                            url = userAvatar,
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape),
                            shape = CircleShape,
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MeloPrimary,
                            modifier = Modifier.size(104.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isAnyLoggedIn) {
                                    Text(
                                        text = userName.take(1).uppercase(),
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0A2012),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF0A2012),
                                        modifier = Modifier.size(54.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Имя в выразительной типографике
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = userName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    if (isAnyLoggedIn) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MeloPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Верифицирован",
                                    tint = MeloPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                // Handle или подпись
                val subtitle = userHandle ?: userEmail ?: if (isAnyLoggedIn) "Подключен" else "Гостевой профиль"
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = if (userHandle != null) MeloPrimary else Color.White.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Medium,
                )

                // Чип статуса
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAnyLoggedIn) MeloPrimary.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(
                        1.dp,
                        if (isAnyLoggedIn) MeloPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
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
                                .background(if (isAnyLoggedIn) MeloPrimary else Color.White.copy(alpha = 0.4f)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnyLoggedIn) "Синхронизация активна" else "Локальный режим",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAnyLoggedIn) MeloPrimary else Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        // ── 3. Интерактивные карточки статистики ────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileStatCard(
                    title = "Любимые треки",
                    count = favorites.size.toString(),
                    icon = Icons.Rounded.Favorite,
                    iconTint = MeloPrimary,
                    onClick = onOpenFavorites,
                    modifier = Modifier.weight(1f),
                )

                ProfileStatCard(
                    title = "Плейлисты",
                    count = playlists.size.toString(),
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    iconTint = Color(0xFF60A5FA),
                    onClick = onOpenPlaylists,
                    modifier = Modifier.weight(1f),
                )

                ProfileStatCard(
                    title = "История",
                    count = history.size.toString(),
                    icon = Icons.Rounded.History,
                    iconTint = Color(0xFFFBBF24),
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── 4. Подключенные аккаунты и Синхронизация ────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MeloSurfaceVariant.copy(alpha = 0.35f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(26.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "ПОДКЛЮЧЕННЫЕ СЕРВИСЫ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeloPrimary,
                    letterSpacing = 1.sp,
                )

                // YouTube Music карточка
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            modifier = Modifier.size(46.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "YouTube Music",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isYtLoggedIn) {
                                    syncStatusText ?: (DesktopYouTubeAuthManager.accountHandle ?: "Подключено")
                                } else {
                                    "Войдите для синхронизации плейлистов и лайков"
                                },
                                fontSize = 13.sp,
                                color = if (isYtLoggedIn) MeloPrimary else Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (isYtLoggedIn) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
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
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSyncing) "Синхронизация..." else "Синхронизировать",
                                    color = Color(0xFF0A2012),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }

                            IconButton(
                                onClick = {
                                    DesktopYouTubeAuthManager.logout()
                                    syncStatusText = null
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.08f)),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = "Выйти из YouTube Music",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onOpenAuth,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Login,
                                contentDescription = null,
                                tint = Color(0xFF0A2012),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Войти",
                                color = Color(0xFF0A2012),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        // ── 5. Недавние любимые треки (Горизонтальная полка) ─────────────────────
        if (favorites.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Любимые треки",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MeloPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(favorites.take(15)) { track ->
                        ProfileTrackCard(
                            track = track,
                            onClick = { onPlayTrack(track) },
                        )
                    }
                }
            }
        }

        // ── 6. Плейлисты (Горизонтальная полка) ──────────────────────────────────
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
                        letterSpacing = (-0.3).sp,
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MeloPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(playlists.keys.toList()) { name ->
                        val pTracks = playlists[name] ?: emptyList()
                        val coverUrl = pTracks.firstOrNull()?.thumbnailUrl

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .width(150.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    pTracks.firstOrNull()?.let { onPlayTrack(it) }
                                },
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(126.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MeloSurfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!coverUrl.isNullOrBlank()) {
                                        AsyncCoverImage(
                                            url = coverUrl,
                                            modifier = Modifier.fillMaxSize(),
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                            contentDescription = null,
                                            tint = MeloPrimary,
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
                                    text = "${pTracks.size} треков",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 7. Пустое состояние, если вообще нет музыки ─────────────────────────
        if (favorites.isEmpty() && playlists.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)), RoundedCornerShape(28.dp))
                        .padding(40.dp),
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
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Тут пока пусто",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Синхронизируйте аккаунт YouTube Music или добавляйте треки в любимые через Поиск",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Карточка быстрой статистики (Любимые, Плейлисты, История) */
@Composable
private fun ProfileStatCard(
    title: String,
    count: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.02f else 1f, label = "statCardScale")
    val bgColor by animateColorAsState(
        if (isHovered) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
        label = "statCardBg"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        border = BorderStroke(1.dp, if (isHovered) iconTint.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.16f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = count,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Карточка трека в горизонтальной ленте профиля */
@Composable
private fun ProfileTrackCard(
    track: TrackItem,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.03f else 1f, label = "profileTrackScale")

    Column(
        modifier = Modifier
            .width(144.dp)
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
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MeloSurfaceVariant)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncCoverImage(
                url = track.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
            )

            // Кнопка воспроизведения на hover
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
                        modifier = Modifier.size(46.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = Color(0xFF0A2012),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        track.uploader?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
