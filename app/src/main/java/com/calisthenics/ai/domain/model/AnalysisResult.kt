package com.calisthenics.ai.domain.model

data class AnalysisResult(
    val id: Long,
    val userId: Long,
    val exerciseId: Long?,
    val exerciseName: String,
    val mediaUrl: String,
    val mediaType: String,
    val overallScore: Double?,
    val holdDurationSeconds: Double?,
    val formFeedbackList: List<String>,
    val elbowAngle: Double?,
    val bodyLineAngle: Double?,
    val shoulderLeanAngle: Double?,
    val rawKeypointsJson: String?,
    val createdAt: String
)
