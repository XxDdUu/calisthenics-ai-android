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
    @SerializedName("createdAt") val createdAt: String?,
    // --- Video metadata ---
    @SerializedName("videoDurationMs") val videoDurationMs: Long? = null,
    @SerializedName("videoWidth") val videoWidth: Int? = null,
    @SerializedName("videoHeight") val videoHeight: Int? = null,
    @SerializedName("originalFps") val originalFps: Double? = null,
    @SerializedName("totalFrames") val totalFrames: Int? = null,
    @SerializedName("analyzedFrames") val analyzedFrames: Int? = null,
    @SerializedName("samplingFps") val samplingFps: Double? = null,
    @SerializedName("fileSizeBytes") val fileSizeBytes: Long? = null,
    @SerializedName("processingTimeMs") val processingTimeMs: Double? = null,
    // --- Biomechanical aggregates as JSON strings ---
    @SerializedName("componentScoresJson") val componentScoresJson: String? = null,
    @SerializedName("stabilityJson") val stabilityJson: String? = null,
    @SerializedName("timelineJson") val timelineJson: String? = null
)
