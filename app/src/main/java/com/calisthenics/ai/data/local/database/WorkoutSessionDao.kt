package com.calisthenics.ai.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveSession(entity: CachedActiveWorkoutSessionEntity)

    @Query("SELECT * FROM cached_active_workout_sessions WHERE ownerUserId = :userId AND sessionId = :sessionId")
    suspend fun getActiveSession(userId: Long, sessionId: Long): CachedActiveWorkoutSessionEntity?

    @Query("SELECT * FROM cached_active_workout_sessions WHERE ownerUserId = :userId ORDER BY updatedAt DESC")
    suspend fun getActiveSessionsForUser(userId: Long): List<CachedActiveWorkoutSessionEntity>

    @Query("SELECT * FROM cached_active_workout_sessions WHERE ownerUserId = :userId ORDER BY updatedAt DESC")
    fun observeActiveSessionsForUser(userId: Long): Flow<List<CachedActiveWorkoutSessionEntity>>

    @Query("DELETE FROM cached_active_workout_sessions WHERE ownerUserId = :userId AND sessionId = :sessionId")
    suspend fun deleteActiveSession(userId: Long, sessionId: Long)

    @Query("DELETE FROM cached_active_workout_sessions WHERE ownerUserId = :userId")
    suspend fun clearActiveSessions(userId: Long)
}
