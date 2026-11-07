package com.calisthenics.ai.data.remote.dto

import com.calisthenics.ai.domain.model.*
import com.google.gson.annotations.SerializedName

data class VideoAnalysisResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("exerciseId") val exerciseId: Long? = null,
    @SerializedName("exerciseName") val exerciseName: String? = null,
    @SerializedName("status") val status: String? = null, // PENDING, PROCESSING, COMPLETED, FAILED
    @SerializedName("overallScore") val overallScore: Double? = null,
    @SerializedName("formAccuracy") val formAccuracy: Double? = null,
    @SerializedName("stabilityScore") val stabilityScore: Double? = null,
    @SerializedName("alignmentScore") val alignmentScore: Double? = null,
    @SerializedName("durationMs") val durationMs: Long? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("fps") val fps: Double? = null,
    @SerializedName("originalVideoUrl") val originalVideoUrl: String? = null,
    @SerializedName("annotatedVideoUrl") val annotatedVideoUrl: String? = null,
    @SerializedName("keypointsUrl") val keypointsUrl: String? = null,
    @SerializedName("metrics") val metrics: Map<String, Any?>? = null,
    @SerializedName("feedback") val feedback: List<String>? = null,
    @SerializedName("errorMessage") val errorMessage: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
) {
    fun toDomain(): AnalysisResult {
        val meta = if ((durationMs ?: 0L) > 0 || (width ?: 0) > 0) {
            VideoMetadata(
                durationMs = durationMs ?: 0L,
                width = width ?: 0,
                height = height ?: 0,
                originalFps = fps ?: 30.0,
                totalFrames = if (fps != null && durationMs != null) ((durationMs / 1000.0) * fps).toInt() else 0,
                analyzedFrames = (metrics?.get("analyzedFrames") as? Number)?.toInt() ?: 0,
                samplingFps = (metrics?.get("samplingFps") as? Number)?.toDouble() ?: 10.0,
                fileSizeBytes = 0L
            )
        } else null

        val compScores = ComponentScores(
            bodyAlignment = alignmentScore,
            elbowExtension = (metrics?.get("elbow_angle") as? Number)?.toDouble(),
            shoulderPosition = (metrics?.get("shoulder_angle") as? Number)?.toDouble(),
            hipExtension = (metrics?.get("hip_angle") as? Number)?.toDouble(),
            stability = stabilityScore,
            overall = overallScore
        )

        return AnalysisResult(
            id = id,
            userId = userId,
            exerciseId = exerciseId,
            exerciseName = exerciseName ?: "Exercise",
            mediaUrl = annotatedVideoUrl ?: originalVideoUrl ?: "",
            mediaType = "VIDEO",
            overallScore = overallScore,
            holdDurationSeconds = durationMs?.let { it / 1000.0 },
            formFeedbackList = feedback ?: emptyList(),
            elbowAngle = (metrics?.get("elbow_angle") as? Number)?.toDouble(),
            bodyLineAngle = (metrics?.get("body_line_angle") as? Number)?.toDouble(),
            shoulderLeanAngle = (metrics?.get("shoulder_angle") as? Number)?.toDouble(),
            rawKeypointsJson = null,
            createdAt = createdAt ?: "",
            videoMetadata = meta,
            componentScores = compScores,
            stabilityMetrics = stabilityScore?.let {
                StabilityMetrics(
                    averageBodyLineDeviation = 0.0,
                    maxBodyLineDeviation = 0.0,
                    shoulderPositionVariation = 0.0,
                    hipPositionVariation = 0.0,
                    averagePoseConfidence = 0.9,
                    stabilityScore = it
                )
            },
            timeline = emptyList(),
            originalVideoUrl = originalVideoUrl,
            annotatedVideoUrl = annotatedVideoUrl,
            status = status ?: "COMPLETED",
            keypointsUrl = keypointsUrl
        )
    }
}

data class VideoAnalysisKeypointsDto(
    @SerializedName("analysisId") val analysisId: Long,
    @SerializedName("format") val format: String? = "COCO_17",
    @SerializedName("coordinateSystem") val coordinateSystem: String? = "NORMALIZED",
    @SerializedName("totalFrames") val totalFrames: Int = 0,
    @SerializedName("frames") val frames: List<VideoKeypointFrameDto>? = null
) {
    fun toDomain(): VideoKeypointsDataModel {
        return VideoKeypointsDataModel(
            analysisId = analysisId,
            format = format ?: "COCO_17",
            coordinateSystem = coordinateSystem ?: "NORMALIZED",
            totalFrames = totalFrames,
            frames = frames?.map { it.toDomain() } ?: emptyList()
        )
    }
}

data class VideoKeypointFrameDto(
    @SerializedName("frameIndex") val frameIndex: Int,
    @SerializedName("timestampMs") val timestampMs: Long,
    @SerializedName("keypoints") val keypoints: List<VideoKeypointItemDto>? = null
) {
    fun toDomain(): KeypointFrameModel {
        return KeypointFrameModel(
            frameIndex = frameIndex,
            timestampMs = timestampMs,
            keypoints = keypoints?.map { it.toDomain() } ?: emptyList()
        )
    }
}

data class VideoKeypointItemDto(
    @SerializedName("name") val name: String,
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float,
    @SerializedName("confidence") val confidence: Float
) {
    fun toDomain(): KeypointItemModel {
        return KeypointItemModel(
            name = name,
            x = x,
            y = y,
            confidence = confidence
        )
    }
}
