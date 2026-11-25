package com.melo.music.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isPublic: Boolean = true,
    val tracksJson: String // Храним список треков плейлиста в формате JSON в строке таблицы
)
