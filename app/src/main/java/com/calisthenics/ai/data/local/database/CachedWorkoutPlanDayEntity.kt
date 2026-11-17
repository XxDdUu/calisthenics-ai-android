package com.calisthenics.ai.data.local.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_workout_plan_days",
    primaryKeys = ["ownerUserId", "planId", "dayOfWeek"],
    indices = [
        Index(value = ["ownerUserId", "planId"]),
        Index(value = ["ownerUserId", "planId", "id"])
    ]
)
data class CachedWorkoutPlanDayEntity(
    val ownerUserId: Long,
    val id: Long,
    val planId: Long,
    val dayOfWeek: String, // e.g. MONDAY, TUESDAY
    val sessionType: String, // e.g. PUSH, PULL, REST
    val name: String? = null,
    val description: String? = null,
    val isRestDay: Boolean = false
)
