package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnalysisResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("exerciseId") val exerciseId: Long?,
    @SerializedName("exerciseName") val exerciseName: String?,
    @SerializedName("mediaUrl") val mediaUrl: String?,
    @SerializedName("mediaType") val mediaType: String?,
    @SerializedName("overallScore") val overallScore: Double?,
    @SerializedName("holdDurationSeconds") val holdDurationSeconds: Double?,
    @SerializedName("formFeedback") val formFeedback: String?,
    @SerializedName("elbowAngle") val elbowAngle: Double?,
    @SerializedName("bodyLineAngle") val bodyLineAngle: Double?,
    @SerializedName("shoulderLeanAngle") val shoulderLeanAngle: Double?,
    @SerializedName("rawKeypointsJson") val rawKeypointsJson: String?,
    @SerializedName("createdAt") val createdAt: String?
)
