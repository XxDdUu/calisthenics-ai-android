package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GenerateAiWorkoutPlanRequestDto(
    @SerializedName("prompt") val prompt: String,
    @SerializedName("clientId") val clientId: Long? = null,
    @SerializedName("fitnessLevel") val fitnessLevel: String? = null,
    @SerializedName("preferredSplit") val preferredSplit: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("durationWeeks") val durationWeeks: Int? = null
)

data class ModifyAiWorkoutPlanRequestDto(
    @SerializedName("prompt") val prompt: String,
    @SerializedName("currentPlan") val currentPlan: AiWorkoutPlanPreviewDto,
    @SerializedName("clientId") val clientId: Long? = null
)

data class CommitAiWorkoutPlanRequestDto(
    @SerializedName("plan") val plan: AiWorkoutPlanPreviewDto,
    @SerializedName("idempotencyKey") val idempotencyKey: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("replaceExisting") val replaceExisting: Boolean = true,
    @SerializedName("expectedActivePlanId") val expectedActivePlanId: Long? = null,
    @SerializedName("clientId") val clientId: Long? = null
)

data class AiWorkoutPlanPreviewDto(
    @SerializedName("planName") val planName: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("durationWeeks") val durationWeeks: Int? = 4,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("splitType") val splitType: String? = null,
    @SerializedName("sessions") val sessions: List<AiWorkoutSessionPreviewDto> = emptyList(),
    @SerializedName("metadata") val metadata: AiWorkoutMetadataDto? = null
)

data class AiWorkoutSessionPreviewDto(
    @SerializedName("dayOfWeek") val dayOfWeek: String,
    @SerializedName("title") val title: String,
    @SerializedName("restDay") val restDay: Boolean = false,
    @SerializedName("exercises") val exercises: List<AiWorkoutExercisePreviewDto> = emptyList()
)

data class AiWorkoutExercisePreviewDto(
    @SerializedName("exerciseId") val exerciseId: Long? = null,
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("exerciseCode") val exerciseCode: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("sets") val sets: Int? = null,
    @SerializedName("reps") val reps: Int? = null,
    @SerializedName("holdSeconds") val holdSeconds: Int? = null,
    @SerializedName("restSeconds") val restSeconds: Int? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("requiresReview") val requiresReview: Boolean = false
)

data class AiWorkoutMetadataDto(
    @SerializedName("modelUsed") val modelUsed: String? = null,
    @SerializedName("inputTokens") val inputTokens: Int? = null,
    @SerializedName("outputTokens") val outputTokens: Int? = null,
    @SerializedName("latencyMs") val latencyMs: Long? = null,
    @SerializedName("hasConflicts") val hasConflicts: Boolean = false,
    @SerializedName("existingActivePlanName") val existingActivePlanName: String? = null
)
