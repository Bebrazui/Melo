package com.melo.music.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String, // Композитный ID: url + speed + bassBoost + crush + pitchSemi + noteShiftSeed
    val url: String,
    val title: String,
    val uploader: String?,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val source: String,
    val speed: Float,
    val bassBoost: Boolean,
    val crush: Float,
    val pitchSemi: Float,
    val noteShiftSeed: Long,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrackItem(): TrackItem = TrackItem(
        title = title,
        uploader = uploader,
        url = url,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl,
        source = runCatching { Source.valueOf(source) }.getOrDefault(Source.YOUTUBE_MUSIC),
        kind = ItemKind.TRACK,
        speed = speed,
        bassBoost = bassBoost,
        crush = crush,
        pitchSemi = pitchSemi,
        noteShiftSeed = noteShiftSeed,
    )

    companion object {
        fun fromTrackItem(item: TrackItem, addedAt: Long = System.currentTimeMillis()): FavoriteEntity {
            val compositeId = "${item.url}|${item.speed}|${item.bassBoost}|${item.crush}|${item.pitchSemi}|${item.noteShiftSeed}"
            return FavoriteEntity(
                id = compositeId,
                url = item.url,
                title = item.title,
                uploader = item.uploader,
                durationSeconds = item.durationSeconds,
                thumbnailUrl = item.thumbnailUrl,
                source = item.source.name,
                speed = item.speed,
                bassBoost = item.bassBoost,
                crush = item.crush,
                pitchSemi = item.pitchSemi,
                noteShiftSeed = item.noteShiftSeed,
                addedAt = addedAt
            )
        }
    }
}
