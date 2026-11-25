package com.melo.music.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
        PlayCountEntity::class,
        PlaylistEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MeloDatabase : RoomDatabase() {

    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: MeloDatabase? = null

        fun getInstance(context: Context): MeloDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeloDatabase::class.java,
                    "melo.db"
                )
                .fallbackToDestructiveMigration() // Для плавного обновления схемы на этапе разработки
                .build().also { INSTANCE = it }
            }
        }
    }
}
