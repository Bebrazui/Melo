package com.melo.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
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
import com.melo.desktop.extractor.ItemKind
import com.melo.desktop.extractor.Source
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.ui.theme.MeloCardSurface
import com.melo.desktop.ui.theme.MeloSoundCloudOrange
import com.melo.desktop.ui.theme.MeloYoutubeRed

/**
 * Фирменная карточка элемента поиска / выдачи Melo (Material 3 Expressive):
 * Точно как в мобильном приложении — скругленный блок, бейдж источника с точкой/иконкой,
 * круглая аватарка для артиста и скругленная обложка для трека.
 */
@Composable
fun MeloTrackCard(
    item: TrackItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrent = DesktopAudioPlayer.currentTrack?.originalUrl == item.url
    val isPlaying = isCurrent && DesktopAudioPlayer.isPlaying
    val isArtist = item.kind == ItemKind.ARTIST

    val cardBg = if (isCurrent) Color(0xFF2B3D32) else MeloCardSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // Обложка / Аватар
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncCoverImage(
                url = item.thumbnailUrl,
                modifier = Modifier.size(54.dp),
                shape = if (isArtist) RoundedCornerShape(27.dp) else RoundedCornerShape(14.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Заголовок и подзаголовок с иконкой источника
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp),
            ) {
                // Красная / оранжевая иконка сервиса
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = if (item.source == Source.SOUNDCLOUD) MeloSoundCloudOrange else MeloYoutubeRed,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))

                val subText = if (isArtist) {
                    "Исполнитель"
                } else {
                    val dur = formatDuration(item.durationSeconds)
                    if (item.uploader != null) "${item.uploader} · $dur" else dur
                }

                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Иконка справа (шеврон для артиста, play для трека)
        if (isArtist) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Играть",
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "--:--"
    val min = seconds / 60
    val sec = seconds % 60
    return "%d:%02d".format(min, sec)
}
