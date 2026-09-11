package com.melo.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun TrackRow(
    track: TrackItem,
    index: Int? = null,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrent = DesktopAudioPlayer.currentTrack?.originalUrl == track.url
    val isPlaying = isCurrent && DesktopAudioPlayer.isPlaying
    val isLiked = DesktopStorage.isFavorite(track.url)

    val rowBg = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .clickable { onPlay() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Номер трека
        if (index != null) {
            Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Играет",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(
                        text = index.toString(),
                        fontSize = 13.sp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        // Обложка
        AsyncCoverImage(
            url = track.thumbnailUrl,
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(6.dp),
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Название и артист
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.uploader ?: "Неизвестный исполнитель",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Бейдж источника
        Text(
            text = if (track.source == Source.SOUNDCLOUD) "SoundCloud" else "YouTube Music",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Длительность
        Text(
            text = formatDuration(track.durationSeconds),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Кнопка лайка
        IconButton(
            onClick = { DesktopStorage.toggleFavorite(track) },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Лайк",
                tint = if (isLiked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "--:--"
    val min = seconds / 60
    val sec = seconds % 60
    return "%02d:%02d".format(min, sec)
}
