package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AnalysisResponseDto
import com.calisthenics.ai.domain.model.AnalysisResult

fun AnalysisResponseDto.toDomain(): AnalysisResult {
    val feedbackList = formFeedback?.split(";")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

    return AnalysisResult(
        id = id,
        userId = userId,
        exerciseId = exerciseId,
        exerciseName = exerciseName ?: "Custom Exercise",
        mediaUrl = mediaUrl ?: "",
        mediaType = mediaType ?: "VIDEO",
        overallScore = overallScore,
        holdDurationSeconds = holdDurationSeconds,
        formFeedbackList = feedbackList,
        elbowAngle = elbowAngle,
        bodyLineAngle = bodyLineAngle,
        shoulderLeanAngle = shoulderLeanAngle,
        rawKeypointsJson = rawKeypointsJson,
        createdAt = createdAt ?: ""
    )
}
