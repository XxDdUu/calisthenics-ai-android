package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AiWorkoutExercisePreviewDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutMetadataDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutPlanPreviewDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutSessionPreviewDto
import com.calisthenics.ai.domain.model.AiWorkoutExercisePreview
import com.calisthenics.ai.domain.model.AiWorkoutMetadata
import com.calisthenics.ai.domain.model.AiWorkoutPlanPreview
import com.calisthenics.ai.domain.model.AiWorkoutSessionPreview
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSplitType

fun AiWorkoutPlanPreviewDto.toDomain(): AiWorkoutPlanPreview {
    val split = try {
        WorkoutSplitType.valueOf((splitType ?: "CUSTOM").uppercase())
    } catch (_: Exception) {
        WorkoutSplitType.CUSTOM
    }

    return AiWorkoutPlanPreview(
        planName = planName,
        description = description,
        durationWeeks = durationWeeks ?: 4,
        startDate = startDate,
        splitType = split,
        sessions = sessions.map { it.toDomain() },
        metadata = metadata?.toDomain() ?: AiWorkoutMetadata(
            modelUsed = "ai-planner",
            inputTokens = 0,
            outputTokens = 0,
            latencyMs = 0L,
            hasConflicts = false,
            existingActivePlanName = null
        )
    )
}

fun AiWorkoutSessionPreviewDto.toDomain(): AiWorkoutSessionPreview {
    val day = WorkoutDayOfWeek.fromName(dayOfWeek)
    return AiWorkoutSessionPreview(
        dayOfWeek = day,
        title = title,
        isRestDay = restDay,
        exercises = exercises.map { it.toDomain() }
    )
}

fun AiWorkoutExercisePreviewDto.toDomain(): AiWorkoutExercisePreview {
    return AiWorkoutExercisePreview(
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        exerciseCode = exerciseCode,
        category = category,
        sets = sets?.takeIf { it > 0 } ?: 3,
        reps = reps?.takeIf { it > 0 },
        holdSeconds = holdSeconds?.takeIf { it > 0 },
        restSeconds = restSeconds?.takeIf { it >= 0 } ?: 60,
        notes = notes,
        requiresReview = requiresReview
    )
}

fun AiWorkoutMetadataDto.toDomain(): AiWorkoutMetadata {
    return AiWorkoutMetadata(
        modelUsed = modelUsed ?: "ai-planner",
        inputTokens = inputTokens ?: 0,
        outputTokens = outputTokens ?: 0,
        latencyMs = latencyMs ?: 0L,
        hasConflicts = hasConflicts,
        existingActivePlanName = existingActivePlanName
    )
}

fun AiWorkoutPlanPreview.toDto(): AiWorkoutPlanPreviewDto {
    return AiWorkoutPlanPreviewDto(
        planName = planName,
        description = description,
        durationWeeks = durationWeeks,
        startDate = startDate,
        splitType = splitType.name,
        sessions = sessions.map { it.toDto() },
        metadata = metadata.toDto()
    )
}

fun AiWorkoutSessionPreview.toDto(): AiWorkoutSessionPreviewDto {
    return AiWorkoutSessionPreviewDto(
        dayOfWeek = dayOfWeek.name,
        title = title,
        restDay = isRestDay,
        exercises = exercises.map { it.toDto() }
    )
}

fun AiWorkoutExercisePreview.toDto(): AiWorkoutExercisePreviewDto {
    return AiWorkoutExercisePreviewDto(
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        exerciseCode = exerciseCode,
        category = category,
        sets = sets.takeIf { it > 0 } ?: 3,
        reps = reps?.takeIf { it > 0 },
        holdSeconds = holdSeconds?.takeIf { it > 0 },
        restSeconds = restSeconds.takeIf { it >= 0 } ?: 60,
        notes = notes,
        requiresReview = requiresReview
    )
}

fun AiWorkoutMetadata.toDto(): AiWorkoutMetadataDto {
    return AiWorkoutMetadataDto(
        modelUsed = modelUsed,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        latencyMs = latencyMs,
        hasConflicts = hasConflicts,
        existingActivePlanName = existingActivePlanName
    )
}
