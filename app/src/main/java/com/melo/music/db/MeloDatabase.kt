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
        PlaylistEntity::class,
        StreamCacheEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MeloDatabase : RoomDatabase() {

    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun streamCacheDao(): StreamCacheDao

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
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}
