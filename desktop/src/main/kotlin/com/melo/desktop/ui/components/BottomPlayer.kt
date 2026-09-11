package com.melo.desktop.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.melo.desktop.ui.theme.ScallopedShape
import com.melo.desktop.ui.theme.pressScale
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.audio.DesktopAudioPlayer
import com.melo.desktop.extractor.Source
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.storage.DesktopStorage
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.rounded.OpenInFull
import com.melo.desktop.ui.theme.MeloDarkSurface
import com.melo.desktop.ui.theme.MeloSoundCloudOrange
import com.melo.desktop.ui.theme.MeloYoutubeRed

/**
 * Нижний плеер Melo:
 * Frosted-glass полупрозрачный бар с кнопкой Play/Pause, бейджами источников,
 * таймлайном и возможностью клика для раскрытия полного экрана NowPlaying.
 */
@Composable
fun BottomPlayer(
    onToggleLyrics: () -> Unit,
    isLyricsOpen: Boolean,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onExpand: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val track = DesktopAudioPlayer.currentTrack
    val isPlaying = DesktopAudioPlayer.isPlaying
    val posMs = DesktopAudioPlayer.currentPositionMs
    val durMs = DesktopAudioPlayer.durationMs
    val volume = DesktopAudioPlayer.volume
    val speed = DesktopAudioPlayer.speed

    val scope = rememberCoroutineScope()
    var pendingSeekFraction by remember { mutableStateOf<Float?>(null) }

    val isLiked = track?.let { DesktopStorage.isFavorite(it.originalUrl) } ?: false

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(Color(0xF2121714))
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        // ── Левая зона: Обложка, название, артист, бейдж ──────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onExpand?.invoke() }
                .padding(4.dp),
        ) {
            AsyncCoverImage(
                url = track?.thumbnailUrl,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: "Ничего не играет",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track?.artist ?: "Выберите трек",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (track != null) {
                IconButton(
                    onClick = {
                        DesktopStorage.toggleFavorite(
                            TrackItem(
                                title = track.title,
                                uploader = track.artist,
                                url = track.originalUrl,
                                durationSeconds = durMs / 1000,
                                thumbnailUrl = track.thumbnailUrl,
                                source = track.source,
                            )
                        )
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "В избранное",
                        tint = if (isLiked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // ── Центральная зона: Контроллеры и таймлайн ──────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
        ) {
            // Кнопки плеера
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                IconButton(
                    onClick = { DesktopAudioPlayer.toggleShuffle() },
                    modifier = Modifier.size(32.dp).pressScale(0.88f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Перемешать",
                        tint = if (DesktopAudioPlayer.isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(onClick = onPrev, modifier = Modifier.size(34.dp).pressScale(0.88f)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Предыдущий",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Фирменная фигурная кнопка Play/Pause в стиле Melo Scalloped
                val scallopedPlayShape = remember { ScallopedShape(petals = 8, depth = 0.14f) }
                Surface(
                    shape = scallopedPlayShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .pressScale(0.88f)
                        .clip(scallopedPlayShape)
                        .clickable { DesktopAudioPlayer.togglePlayPause() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.7f))
                                    .togetherWith(fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.7f))
                            },
                            label = "bottomPlayPauseMorph",
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color(0xFF101512),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }

                IconButton(onClick = onNext, modifier = Modifier.size(34.dp).pressScale(0.88f)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Следующий",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = { DesktopAudioPlayer.toggleRepeatMode() },
                    modifier = Modifier.size(32.dp).pressScale(0.88f),
                ) {
                    val repeatMode = DesktopAudioPlayer.repeatMode
                    val (icon, tint) = when (repeatMode) {
                        DesktopAudioPlayer.RepeatMode.OFF -> Icons.Rounded.Repeat to Color.White.copy(alpha = 0.65f)
                        DesktopAudioPlayer.RepeatMode.ALL -> Icons.Rounded.Repeat to MaterialTheme.colorScheme.primary
                        DesktopAudioPlayer.RepeatMode.ONE -> Icons.Rounded.RepeatOne to MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Повтор",
                        tint = tint,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Таймлайн
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(22.dp),
            ) {
                val effectiveDurMs = if (durMs > 0L) durMs else ((track?.durationSeconds ?: 0L) * 1000L)
                val progressFraction = pendingSeekFraction ?: if (effectiveDurMs > 0) (posMs.toFloat() / effectiveDurMs.toFloat()).coerceIn(0f, 1f) else 0f
                val currentMs = pendingSeekFraction?.let { (it * effectiveDurMs).toLong() } ?: posMs
                Text(
                    text = formatTime(currentMs),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.width(42.dp),
                )

                SleekSlider(
                    value = progressFraction.coerceIn(0f, 1f),
                    onValueChange = {
                        pendingSeekFraction = it
                    },
                    onValueChangeFinished = { fraction ->
                        if (effectiveDurMs > 0) {
                            val targetMs = (fraction * effectiveDurMs).toLong()
                            DesktopAudioPlayer.seekTo(targetMs)
                        }
                        scope.launch {
                            delay(150)
                            pendingSeekFraction = null
                        }
                    },
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.2f),
                    thumbColor = Color.White,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )

                Text(
                    text = formatTime(effectiveDurMs),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.width(42.dp),
                )
            }
        }

        // ── Правая зона: Текст песни, Скорость и Громкость ────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.width(300.dp),
        ) {
            // Кнопка скорости (M3 Expressive Pill: slowed down / original / speed up)
            val speedLabel = when {
                kotlin.math.abs(speed - DesktopAudioPlayer.SPEED_SLOWED) < 0.02f -> "slowed 0.93x"
                kotlin.math.abs(speed - DesktopAudioPlayer.SPEED_SPEED_UP) < 0.02f -> "speed up 1.15x"
                else -> "1.0x"
            }
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = if (speed != 1.0f) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (speed != 1.0f) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.18f),
                ),
                modifier = Modifier
                    .height(30.dp)
                    .pressScale(0.92f)
                    .clip(RoundedCornerShape(15.dp))
                    .clickable { DesktopAudioPlayer.cyclePlaybackSpeed() },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = speedLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (speed != 1.0f) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Кнопка текста песен
            IconButton(
                onClick = onToggleLyrics,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = "Текст песни",
                    tint = if (isLyricsOpen) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Регулятор громкости
            IconButton(
                onClick = {
                    if (volume > 0f) DesktopAudioPlayer.setPlayerVolume(0f) else DesktopAudioPlayer.setPlayerVolume(0.8f)
                },
                modifier = Modifier.size(32.dp),
            ) {
                val volIcon = when {
                    volume == 0f -> Icons.AutoMirrored.Rounded.VolumeMute
                    volume < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                    else -> Icons.AutoMirrored.Rounded.VolumeUp
                }
                Icon(
                    imageVector = volIcon,
                    contentDescription = "Громкость",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp),
                )
            }

            SleekSlider(
                value = volume,
                onValueChange = { DesktopAudioPlayer.setPlayerVolume(it) },
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.2f),
                thumbColor = Color.White,
                modifier = Modifier.width(85.dp),
            )

            if (onExpand != null) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInFull,
                        contentDescription = "Развернуть",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
