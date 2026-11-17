package com.calisthenics.ai.data.local.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_workout_plans",
    primaryKeys = ["ownerUserId", "id"],
    indices = [
        Index(value = ["ownerUserId"]),
        Index(value = ["ownerUserId", "isActive"])
    ]
)
data class CachedWorkoutPlanEntity(
    val ownerUserId: Long,
    val id: Long,
    val name: String,
    val splitType: String,
    val isCoachAssigned: Boolean = false,
    val coachName: String? = null,
    val assignmentId: Long? = null,
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
