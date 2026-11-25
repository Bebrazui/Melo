package com.melo.music.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stream_cache")
data class StreamCacheEntity(
    @PrimaryKey val pageUrl: String,
    val audioUrl: String,
    val title: String,
    val artist: String?,
    val thumbnailUrl: String?,
    val videoUrl: String?,
    val expiresAt: Long,
    val addedAt: Long = System.currentTimeMillis()
)
