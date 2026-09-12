package com.melo.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.audio.DesktopAudioPlayer
import com.melo.desktop.ui.NavDestination
import com.melo.desktop.ui.theme.MeloDarkSurface
import com.melo.desktop.ui.theme.MeloPrimary
import com.melo.desktop.ui.theme.MeloSurfaceVariant

@Composable
fun Sidebar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    onOpenNowPlaying: (() -> Unit)? = null,
    onOpenAuth: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val currentTrack = DesktopAudioPlayer.currentTrack
    val isPlaying = DesktopAudioPlayer.isPlaying

    Column(
        modifier = modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(MeloDarkSurface)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        // ── Шапка: Логотип Melo и аватарка профиля справа ───────────────────
        val isMeloLoggedIn = com.melo.desktop.auth.DesktopAuthManager.isLoggedIn
        val isYtLoggedIn = com.melo.desktop.auth.DesktopYouTubeAuthManager.isLoggedIn
        val isAnyLoggedIn = isMeloLoggedIn || isYtLoggedIn
        val userName = com.melo.desktop.auth.DesktopAuthManager.name 
            ?: com.melo.desktop.auth.DesktopYouTubeAuthManager.accountName 
            ?: "Профиль"
        val userAvatar = com.melo.desktop.auth.DesktopYouTubeAuthManager.accountAvatarUrl

        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val logoBitmap = remember {
                val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("melo_wordmark_white.png")
                stream?.use { androidx.compose.ui.res.loadImageBitmap(it) }
            }
            if (logoBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = logoBitmap,
                    contentDescription = "Melo",
                    modifier = Modifier.height(28.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                )
            } else {
                Text(
                    text = "Melo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            // Аватарка справа от текста Melo — открывает профиль
            val isProfileSelected = currentDestination == NavDestination.PROFILE
            val avatarInteractionSource = remember { MutableInteractionSource() }
            val isAvatarHovered by avatarInteractionSource.collectIsHoveredAsState()
            val avatarScale by animateFloatAsState(
                if (isAvatarHovered || isProfileSelected) 1.08f else 1f,
                label = "avatarScale"
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        scaleX = avatarScale
                        scaleY = avatarScale
                    }
                    .clip(CircleShape)
                    .then(
                        if (isProfileSelected) {
                            Modifier.border(androidx.compose.foundation.BorderStroke(2.dp, MeloPrimary), CircleShape)
                        } else Modifier
                    )
                    .background(if (isAnyLoggedIn) MeloPrimary else Color.White.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = avatarInteractionSource,
                        indication = null,
                        onClick = { onNavigate(NavDestination.PROFILE) }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!userAvatar.isNullOrBlank()) {
                    AsyncCoverImage(
                        url = userAvatar,
                        modifier = Modifier.size(if (isProfileSelected) 32.dp else 36.dp),
                        shape = CircleShape,
                    )
                } else if (isAnyLoggedIn) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A2012),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Профиль",
                        tint = if (isProfileSelected) MeloPrimary else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Пункты меню с плавной анимацией и hover эффектом (профиль доступен по аватарке в шапке)
        NavDestination.values().filter { it != NavDestination.PROFILE }.forEach { destination ->
            val isSelected = currentDestination == destination
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

            val targetBg = when {
                isSelected -> MeloSurfaceVariant
                isHovered -> Color.White.copy(alpha = 0.05f)
                else -> Color.Transparent
            }
            val bgColor by animateColorAsState(targetBg, label = "sidebarItemBg")

            val targetContentColor = when {
                isSelected -> MeloPrimary
                isHovered -> Color.White
                else -> Color.White.copy(alpha = 0.70f)
            }
            val contentColor by animateColorAsState(targetContentColor, label = "sidebarContentColor")

            val indicatorWidth by animateDpAsState(
                targetValue = if (isSelected) 4.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "indicatorWidth"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onNavigate(destination) }
                    )
                    .padding(horizontal = 12.dp),
            ) {
                // Активный левый вертикальный индикатор
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MeloPrimary)
                )

                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = destination.title,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Нижняя карточка играющего трека в сайдбаре ───────────────────────
        if (currentTrack != null) {
            val cardInteractionSource = remember { MutableInteractionSource() }
            val isCardHovered by cardInteractionSource.collectIsHoveredAsState()
            val cardScale by animateFloatAsState(if (isCardHovered) 1.02f else 1f, label = "cardScale")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF223026),
                                Color(0xFF19241D)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = cardInteractionSource,
                        indication = null,
                        onClick = { onOpenNowPlaying?.invoke() }
                    )
                    .padding(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AsyncCoverImage(
                        url = currentTrack.thumbnailUrl,
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(10.dp),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentTrack.artist ?: "Исполнитель",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    IconButton(
                        onClick = { DesktopAudioPlayer.togglePlayPause() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MeloPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

