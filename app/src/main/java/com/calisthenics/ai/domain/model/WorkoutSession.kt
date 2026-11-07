package com.calisthenics.ai.domain.model

data class WorkoutSession(
    val id: Long,
    val workoutPlanId: Long?,
    val workoutPlanDayId: Long?,
    val assignmentId: Long?,
    val source: String,
    val title: String,
    val workoutDate: String?,
    val status: String,
    val startedAt: String?,
    val lastSavedAt: String?,
    val completedAt: String?,
    val durationSeconds: Long?,
    val completedExercises: Int,
    val skippedExercises: Int,
    val unfinishedExercises: Int,
    val completedSets: Int,
    val totalReps: Int,
    val totalHoldDurationSeconds: Int,
    val exercises: List<WorkoutSessionExercise>
)

data class WorkoutSessionExercise(
    val id: Long,
    val version: Long,
    val exerciseId: Long?,
    val exerciseName: String,
    val instructions: String?,
    val notes: String?,
    val orderIndex: Int,
    val targetSets: Int,
    val targetReps: Int?,
    val targetHoldDurationSeconds: Int?,
    val restSeconds: Int,
    val status: String,
    val sets: List<WorkoutSessionSet>
)

data class WorkoutSessionSet(
    val id: Long,
    val version: Long,
    val setNumber: Int,
    val reps: Int?,
    val holdDurationSeconds: Int?,
    val weightKg: Double?,
    val assistanceWeightKg: Double?,
    val completed: Boolean,
    val completedAt: String?,
    val notes: String?
)

data class WorkoutSessionSetUpdate(
    val reps: Int?,
    val holdDurationSeconds: Int?,
    val weightKg: Double?,
    val assistanceWeightKg: Double?,
    val completed: Boolean,
    val notes: String?,
    val expectedVersion: Long = 0L
)

data class WorkoutSessionHistoryPage(
    val content: List<WorkoutSession>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
