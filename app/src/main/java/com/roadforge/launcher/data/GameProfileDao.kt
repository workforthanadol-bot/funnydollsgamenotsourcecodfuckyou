package com.roadforge.launcher.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GameProfile>>

    @Query("SELECT * FROM game_profiles WHERE id = :id")
    suspend fun getById(id: String): GameProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: GameProfile)

    @Update
    suspend fun update(profile: GameProfile)

    @Delete
    suspend fun delete(profile: GameProfile)

    @Query("UPDATE game_profiles SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: GameStatus, error: String? = null)

    @Query("UPDATE game_profiles SET lastLaunchedAt = :ts WHERE id = :id")
    suspend fun updateLastLaunched(id: String, ts: Long)

    @Query("SELECT COUNT(*) FROM game_profiles")
    suspend fun count(): Int
}
