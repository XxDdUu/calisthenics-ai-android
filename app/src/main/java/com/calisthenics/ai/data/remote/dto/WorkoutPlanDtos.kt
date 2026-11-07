package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WorkoutPlanResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("splitType") val splitType: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("days") val days: List<WorkoutPlanDayResponseDto>? = null
)

data class WorkoutPlanDayResponseDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("dayOfWeek") val dayOfWeek: String,
    @SerializedName("sessionType") val sessionType: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("isRestDay") val isRestDay: Boolean = false,
    @SerializedName("displayOrder") val displayOrder: Int? = null,
    @SerializedName("exercises") val exercises: List<WorkoutPlanExerciseResponseDto>? = null
)

data class WorkoutPlanExerciseResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("exercise") val exercise: ExerciseResponseDto,
    @SerializedName("sets") val sets: Int,
    @SerializedName("reps") val reps: Int? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("restSeconds") val restSeconds: Int,
    @SerializedName("displayOrder") val displayOrder: Int,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class WorkoutPlanTemplateResponseDto(
    @SerializedName("splitType") val splitType: String,
    @SerializedName("days") val days: List<WorkoutPlanDayResponseDto>
)

data class TodayWorkoutResolutionResponseDto(
    @SerializedName("workoutDate") val workoutDate: String,
    @SerializedName("activeCoach") val activeCoach: Boolean,
    @SerializedName("source") val source: String,
    @SerializedName("selectionRequired") val selectionRequired: Boolean,
    @SerializedName("selectedPlanId") val selectedPlanId: Long?,
    @SerializedName("candidates") val candidates: List<TodayWorkoutCandidateDto>
)

data class TodayWorkoutCandidateDto(
    @SerializedName("workoutPlan") val workoutPlan: WorkoutPlanResponseDto,
    @SerializedName("today") val today: WorkoutPlanDayResponseDto,
    @SerializedName("assignmentId") val assignmentId: Long?,
    @SerializedName("coachName") val coachName: String?
)

data class CreateWorkoutPlanRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("splitType") val splitType: String,
    @SerializedName("days") val days: List<WorkoutPlanDayRequestDto>
)

data class UpdateWorkoutPlanRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("splitType") val splitType: String,
    @SerializedName("days") val days: List<WorkoutPlanDayRequestDto>
)

data class WorkoutPlanDayRequestDto(
    @SerializedName("dayOfWeek") val dayOfWeek: String,
    @SerializedName("sessionType") val sessionType: String?,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("isRestDay") val isRestDay: Boolean = false,
    @SerializedName("displayOrder") val displayOrder: Int? = null,
    @SerializedName("exercises") val exercises: List<WorkoutPlanExerciseRequestDto>? = null
)

data class WorkoutPlanExerciseRequestDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("exerciseId") val exerciseId: Long,
    @SerializedName("sets") val sets: Int,
    @SerializedName("reps") val reps: Int? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("restSeconds") val restSeconds: Int,
    @SerializedName("displayOrder") val displayOrder: Int,
    @SerializedName("notes") val notes: String? = null
)

data class WorkoutDayExercisesBatchRequestDto(
    @SerializedName("exercises") val exercises: List<WorkoutPlanExerciseRequestDto>
)
