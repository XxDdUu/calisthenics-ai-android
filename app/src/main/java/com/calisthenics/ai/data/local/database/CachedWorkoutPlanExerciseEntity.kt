package com.calisthenics.ai.data.local.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_workout_plan_exercises",
    primaryKeys = ["planId", "dayId", "id"],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["dayId"]),
        Index(value = ["planId", "dayId"])
    ]
)
data class CachedWorkoutPlanExerciseEntity(
    val id: Long,
    val planId: Long,
    val dayId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val category: String,
    val difficulty: String,
    val muscleGroup: String,
    val description: String,
    val sets: Int,
    val reps: Int?,
    val durationSeconds: Int?,
    val restSeconds: Int,
    val displayOrder: Int,
    val notes: String?
)

