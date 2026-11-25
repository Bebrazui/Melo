package com.melo.music.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StreamCacheDao {

    @Query("SELECT * FROM stream_cache WHERE pageUrl = :pageUrl")
    suspend fun get(pageUrl: String): StreamCacheEntity?

    @Query("SELECT * FROM stream_cache WHERE expiresAt > :now")
    suspend fun getAllValid(now: Long): List<StreamCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StreamCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StreamCacheEntity>)

    @Query("DELETE FROM stream_cache WHERE pageUrl = :pageUrl")
    suspend fun delete(pageUrl: String)

    @Query("DELETE FROM stream_cache WHERE expiresAt <= :now")
    suspend fun pruneExpired(now: Long)

    @Query("DELETE FROM stream_cache WHERE pageUrl NOT IN (SELECT pageUrl FROM stream_cache ORDER BY addedAt DESC LIMIT :maxEntries)")
    suspend fun trim(maxEntries: Int = 400)

    @Query("DELETE FROM stream_cache")
    suspend fun clearAll()
}
