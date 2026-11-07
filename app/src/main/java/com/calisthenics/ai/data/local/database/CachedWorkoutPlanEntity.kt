package com.calisthenics.ai.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_workout_plans")
data class CachedWorkoutPlanEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val splitType: String,
    val isCoachAssigned: Boolean = false,
    val coachName: String? = null,
    val assignmentId: Long? = null,
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

