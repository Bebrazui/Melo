package com.melo.music.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM history WHERE url NOT IN (SELECT url FROM history ORDER BY playedAt DESC LIMIT :cap)")
    suspend fun trim(cap: Int = 50)

    // Play counts
    @Query("SELECT * FROM play_counts WHERE url = :url")
    suspend fun getPlayCount(url: String): PlayCountEntity?

    @Query("SELECT * FROM play_counts")
    suspend fun getAllPlayCounts(): List<PlayCountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayCount(entity: PlayCountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPlayCounts(entities: List<PlayCountEntity>)
}
