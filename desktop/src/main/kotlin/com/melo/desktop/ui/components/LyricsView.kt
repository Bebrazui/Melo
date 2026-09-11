package com.melo.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.audio.DesktopAudioPlayer
import com.melo.desktop.lyrics.DesktopLyrics
import com.melo.desktop.lyrics.Lyrics

@Composable
fun LyricsView(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = DesktopAudioPlayer.currentTrack
    val posMs = DesktopAudioPlayer.currentPositionMs

    var lyrics by remember(track?.title) { mutableStateOf<Lyrics?>(null) }
    var isLoading by remember(track?.title) { mutableStateOf(true) }

    LaunchedEffect(track?.title, track?.artist) {
        if (track == null) {
            lyrics = null
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        lyrics = DesktopLyrics.fetch(track.title, track.artist)
        isLoading = false
    }

    val listState = rememberLazyListState()

    // Находим активную строку
    val activeIndex = remember(lyrics, posMs) {
        val lines = lyrics?.lines ?: return@remember -1
        lines.indexOfLast { it.timeMs <= posMs }
    }

    // Авто-прокрутка до активной строки
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val target = (activeIndex - 3).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Column(
        modifier = modifier
            .width(360.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Текст песни",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Закрыть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            lyrics == null || lyrics!!.isEmpty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Текст песни не найден",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            lyrics!!.isSynced -> {
                val lines = lyrics!!.lines!!
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(lines) { index, line ->
                        val isActive = index == activeIndex
                        val color = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else if (index < activeIndex) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        }

                        Text(
                            text = line.text,
                            fontSize = if (isActive) 18.sp else 15.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = color,
                            lineHeight = 22.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { DesktopAudioPlayer.seekTo(line.timeMs) }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
            else -> {
                // Обычный статичный текст
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = lyrics!!.plain.orEmpty(),
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
