package com.melo.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.MeloAppController
import com.melo.desktop.audio.DesktopAudioPlayer
import com.melo.desktop.extractor.DesktopExtractor
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.recommend.DesktopRecommender
import com.melo.desktop.storage.DesktopStorage
import com.melo.desktop.ui.components.AsyncCoverImage
import com.melo.desktop.ui.components.SeaCard
import com.melo.desktop.ui.theme.MeloCardSurface
import com.melo.desktop.ui.theme.MeloSurfaceVariant
import java.util.Calendar

@Composable
fun HomeScreen(
    onPlayTrack: (TrackItem) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var recommendations by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var moodTracks by remember { mutableStateOf<List<TrackItem>>(emptyList()) }

    val history = DesktopStorage.history
    val isPlaying = DesktopAudioPlayer.isPlaying
    val currentTrack = DesktopAudioPlayer.currentTrack

    val isSeaActive = MeloAppController.isSeaActive
    val isSeaLoading = MeloAppController.isSeaLoading
    val seaIsPlaying = isSeaActive && isPlaying

    val currentSeaTrack = if (isSeaActive) {
        currentTrack?.let {
            TrackItem(
                title = it.title,
                uploader = it.artist,
                url = it.originalUrl,
                durationSeconds = DesktopAudioPlayer.durationMs / 1000,
                thumbnailUrl = it.thumbnailUrl,
                source = it.source,
            )
        } ?: MeloAppController.queue.getOrNull(MeloAppController.currentIndex)
    } else null

    val seaTitle = currentSeaTrack?.title
    val seaArtist = currentSeaTrack?.uploader
    val seaIsLiked = currentSeaTrack?.let { DesktopStorage.isFavorite(it.url) } ?: false

    // Автоподтягивание бесконечной волны Sea у конца очереди
    LaunchedEffect(isSeaActive, MeloAppController.currentIndex, MeloAppController.queue.size) {
        if (isSeaActive && MeloAppController.queue.isNotEmpty() && MeloAppController.currentIndex >= MeloAppController.queue.size - 3) {
            MeloAppController.extendSea()
        }
    }

    // Расчет приветствия по времени суток
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val recs = DesktopExtractor.recommendations().take(20)
        recommendations = recs
        isLoading = false
        com.melo.desktop.extractor.DesktopPrefetcher.prefetchAll(recs.take(3).map { it.url })
    }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            com.melo.desktop.extractor.DesktopPrefetcher.prefetchAll(history.take(2).map { it.url })
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        // ── Верхняя плашка поиска (M3 Rounded Pill) ───────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(MeloSurfaceVariant)
                    .clickable { onOpenSearch() }
                    .padding(horizontal = 20.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Поиск трека или исполнителя",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }
        // ── Приветствие и кнопка настроек ────────────────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        text = greeting,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Text(
                        text = "Главная",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MeloSurfaceVariant),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Поиск",
                            tint = Color.White.copy(alpha = 0.8f),
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MeloSurfaceVariant),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Настройки",
                            tint = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // ── Фирменный цветок «Sea» (Бесконечная волна с чипами настроений) ────
        item {
            SeaCard(
                isPlaying = seaIsPlaying,
                isLiked = seaIsLiked,
                loading = isSeaLoading,
                playingTitle = seaTitle,
                playingArtist = seaArtist,
                selectedMood = selectedMood,
                onMoodSelect = { mood ->
                    selectedMood = mood
                    when (mood) {
                        "Моя волна" -> MeloAppController.startSea()
                        "Бодрое" -> MeloAppController.startSea(querySeed = "energetic phonk workout hits")
                        "Спокойное" -> MeloAppController.startSea(querySeed = "calm lofi chill study")
                        "Для работы" -> MeloAppController.startSea(querySeed = "deep focus electronic ambient")
                        "Любимое" -> {
                            val favSeed = DesktopStorage.favorites.randomOrNull() ?: DesktopStorage.history.randomOrNull()
                            if (favSeed != null) {
                                MeloAppController.startSea(seed = favSeed)
                            } else {
                                MeloAppController.startSea()
                            }
                        }
                    }
                },
                onPlayPause = {
                    if (isSeaActive) {
                        DesktopAudioPlayer.togglePlayPause()
                    } else {
                        MeloAppController.startSea()
                    }
                },
                onNext = {
                    if (isSeaActive) {
                        MeloAppController.playNext()
                    } else {
                        MeloAppController.startSea()
                    }
                },
                onLike = {
                    if (isSeaActive) {
                        currentSeaTrack?.let { DesktopStorage.toggleFavorite(it) }
                    }
                },
            )
        }

        // ── Недавно слушали (Горизонтальная полка) ────────────────────────────
        if (history.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Недавно слушали",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(history.take(15)) { track ->
                            ShelfTrackCard(
                                track = track,
                                onClick = { onPlayTrack(track) },
                            )
                        }
                    }
                }
            }
        }

        // ── Полка рекомендаций ────────────────────────────────────────────────
        item {
            Column {
                Text(
                    text = "Рекомендуем",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(recommendations) { track ->
                            ShelfTrackCard(
                                track = track,
                                onClick = { onPlayTrack(track) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Фирменная карточка трека на полке (скругленный квадрат с обложкой и бейджем). */
@Composable
private fun ShelfTrackCard(
    track: TrackItem,
    onClick: () -> Unit,
) {
    val isCurrent = DesktopAudioPlayer.currentTrack?.originalUrl == track.url
    val isPlaying = isCurrent && DesktopAudioPlayer.isPlaying

    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MeloCardSurface)
            .clickable { onClick() }
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncCoverImage(
                url = track.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
            )

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = track.uploader ?: "Артист",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
