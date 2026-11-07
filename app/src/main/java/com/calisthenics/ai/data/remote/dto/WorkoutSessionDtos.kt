package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StartWorkoutSessionRequestDto(
    @SerializedName("planId") val planId: Long,
    @SerializedName("dayId") val dayId: Long,
    @SerializedName("workoutDate") val workoutDate: String,
    @SerializedName("assignmentId") val assignmentId: Long?
)

data class UpdateWorkoutSessionSetRequestDto(
    @SerializedName("reps") val reps: Int?,
    @SerializedName("holdDurationSeconds") val holdDurationSeconds: Int?,
    @SerializedName("weightKg") val weightKg: Double?,
    @SerializedName("assistanceWeightKg") val assistanceWeightKg: Double?,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("notes") val notes: String?,
    @SerializedName("expectedVersion") val expectedVersion: Long
)

data class UpdateWorkoutSessionExerciseRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("expectedVersion") val expectedVersion: Long
)

data class FinishWorkoutSessionRequestDto(
    @SerializedName("confirmPartial") val confirmPartial: Boolean
)

data class WorkoutSessionResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("workoutPlanId") val workoutPlanId: Long?,
    @SerializedName("workoutPlanDayId") val workoutPlanDayId: Long?,
    @SerializedName("assignmentId") val assignmentId: Long?,
    @SerializedName("source") val source: String = "LEGACY",
    @SerializedName("title") val title: String,
    @SerializedName("workoutDate") val workoutDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("lastSavedAt") val lastSavedAt: String?,
    @SerializedName("completedAt") val completedAt: String?,
    @SerializedName("durationSeconds") val durationSeconds: Long?,
    @SerializedName("completedExercises") val completedExercises: Int = 0,
    @SerializedName("skippedExercises") val skippedExercises: Int = 0,
    @SerializedName("unfinishedExercises") val unfinishedExercises: Int = 0,
    @SerializedName("completedSets") val completedSets: Int = 0,
    @SerializedName("totalReps") val totalReps: Int = 0,
    @SerializedName("totalHoldDurationSeconds") val totalHoldDurationSeconds: Int = 0,
    @SerializedName("exercises") val exercises: List<WorkoutSessionExerciseDto> = emptyList()
)

data class WorkoutSessionExerciseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("version") val version: Long = 0L,
    @SerializedName("exerciseId") val exerciseId: Long?,
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("instructions") val instructions: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("orderIndex") val orderIndex: Int,
    @SerializedName("targetSets") val targetSets: Int,
    @SerializedName("targetReps") val targetReps: Int?,
    @SerializedName("targetHoldDurationSeconds") val targetHoldDurationSeconds: Int?,
    @SerializedName("restSeconds") val restSeconds: Int,
    @SerializedName("status") val status: String,
    @SerializedName("sets") val sets: List<WorkoutSessionSetDto> = emptyList()
)

data class WorkoutSessionSetDto(
    @SerializedName("id") val id: Long,
    @SerializedName("version") val version: Long = 0L,
    @SerializedName("setNumber") val setNumber: Int,
    @SerializedName("reps") val reps: Int?,
    @SerializedName("holdDurationSeconds") val holdDurationSeconds: Int?,
    @SerializedName("weightKg") val weightKg: Double?,
    @SerializedName("assistanceWeightKg") val assistanceWeightKg: Double?,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("completedAt") val completedAt: String?,
    @SerializedName("notes") val notes: String?
)

data class WorkoutSessionPageResponseDto(
    @SerializedName("content") val content: List<WorkoutSessionResponseDto>,
    @SerializedName("page") val page: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int
)
