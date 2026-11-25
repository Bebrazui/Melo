package com.melo.music.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val url: String,
    val title: String,
    val uploader: String?,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val source: String,
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toTrackItem(): TrackItem = TrackItem(
        title = title,
        uploader = uploader,
        url = url,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl,
        source = runCatching { Source.valueOf(source) }.getOrDefault(Source.YOUTUBE_MUSIC),
        kind = ItemKind.TRACK,
    )

    companion object {
        fun fromTrackItem(item: TrackItem, playedAt: Long = System.currentTimeMillis()): HistoryEntity =
            HistoryEntity(
                url = item.url,
                title = item.title,
                uploader = item.uploader,
                durationSeconds = item.durationSeconds,
                thumbnailUrl = item.thumbnailUrl,
                source = item.source.name,
                playedAt = playedAt
            )
    }
}

@Entity(tableName = "play_counts")
data class PlayCountEntity(
    @PrimaryKey val url: String,
    val count: Int,
    val lastPlayedAt: Long
)
