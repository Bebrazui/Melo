package com.melo.music.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.melo.music.extractor.NewPipeResolver
import com.melo.music.extractor.TrackComment
import com.melo.music.extractor.TrackItem
import com.melo.music.extractor.TrackStats
import com.melo.music.favorites.FavoritesManager
import com.melo.music.history.HistoryManager
import com.melo.music.ui.theme.ShapeCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Шторка «О треке»: статистика YouTube (просмотры/лайки/комментарии),
 * персональная статистика (ваши прослушивания, лайк) и топ-комментарии.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackInfoSheet(
    item: TrackItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var stats by remember(item.url) { mutableStateOf<TrackStats?>(null) }
    var statsLoading by remember(item.url) { mutableStateOf(true) }
    var comments by remember(item.url) { mutableStateOf<List<TrackComment>>(emptyList()) }
    var commentsLoading by remember(item.url) { mutableStateOf(false) }
    var commentsShown by remember(item.url) { mutableStateOf(false) }
    val playCount = remember(item.url) { HistoryManager.playCount(item.url) }
    val lastPlayedAt = remember(item.url) { HistoryManager.lastPlayedAt(item.url) }
    val liked = remember(item.url) { FavoritesManager.isLiked(item.url) }

    LaunchedEffect(item.url) {
        statsLoading = true
        stats = withContext(Dispatchers.IO) { NewPipeResolver.trackStats(context, item) }
        statsLoading = false
    }

    LaunchedEffect(commentsShown) {
        if (commentsShown && comments.isEmpty() && !commentsLoading) {
            commentsLoading = true
            comments = withContext(Dispatchers.IO) { NewPipeResolver.trackComments(context, item) }
            commentsLoading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.size(88.dp).clip(ShapeCache.smooth16),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        item.uploader?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (liked) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Favorite, contentDescription = null,
                                    tint = Color(0xFFFF3B5C), modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "В избранном",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                if (statsLoading) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                    }
                } else if (stats == null) {
                    Text(
                        "Статистика недоступна для этого источника",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    StatsGrid(stats!!)
                }
            }

            if (playCount > 0 || lastPlayedAt > 0L) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Headphones, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            buildString {
                                append("Вы слушали")
                                if (playCount > 0) append(" $playCount раз")
                                if (lastPlayedAt > 0L) {
                                    append(" · ")
                                    append(SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(lastPlayedAt)))
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                when {
                    !commentsShown -> FilledTonalButton(onClick = { commentsShown = true }) {
                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Показать комментарии")
                    }
                    commentsLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    else -> {}
                }
            }

            if (commentsShown && !commentsLoading && comments.isEmpty()) {
                item {
                    Text(
                        "Комментариев нет или они скрыты",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(comments.size) { i ->
                CommentRow(comments[i])
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Хелперы ──────────────────────────────────────────────────────────────────

/** Короткий формат чисел: 1234567 → «1,2 млн». */
private fun formatViews(v: Long): String = when {
    v >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f млн", v / 1_000_000.0)
    v >= 1_000 -> String.format(Locale.getDefault(), "%.0f тыс.", v / 1_000.0)
    v > 0 -> v.toString()
    else -> "0"
}

@Composable
private fun StatCell(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatsGrid(stats: TrackStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeCache.smooth12)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatCell(Icons.Rounded.Visibility, formatViews(stats.viewCount), "просмотров")
        StatCell(
            Icons.Rounded.ThumbUp,
            if (stats.likeCount >= 0) formatViews(stats.likeCount) else "—",
            "лайков",
        )
        StatCell(
            Icons.Rounded.ChatBubbleOutline,
            if (stats.commentsCount >= 0) stats.commentsCount.toString() else "—",
            "комментариев",
        )
    }
}

@Composable
private fun CommentRow(comment: TrackComment) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        AsyncImage(
            model = comment.authorAvatar,
            contentDescription = null,
            modifier = Modifier.size(34.dp).clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.author,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!comment.dateText.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        comment.dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                comment.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
            if (comment.likeCount > 0) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ThumbUp, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatViews(comment.likeCount.toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
