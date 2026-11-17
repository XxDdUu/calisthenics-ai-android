package com.calisthenics.ai.data.local.database

import androidx.room.Entity

@Entity(
    tableName = "cached_active_workout_sessions",
    primaryKeys = ["ownerUserId", "sessionId"]
)
data class CachedActiveWorkoutSessionEntity(
    val ownerUserId: Long,
    val sessionId: Long,
    val sessionJson: String,
    val updatedAt: Long
)
