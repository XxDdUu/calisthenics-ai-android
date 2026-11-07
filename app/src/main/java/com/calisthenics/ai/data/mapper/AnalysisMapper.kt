package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AnalysisResponseDto
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.ComponentScores
import com.calisthenics.ai.domain.model.FrameMetric
import com.calisthenics.ai.domain.model.StabilityMetrics
import com.calisthenics.ai.domain.model.VideoMetadata
import org.json.JSONArray
import org.json.JSONObject

fun AnalysisResponseDto.toDomain(): AnalysisResult {
    val feedbackList = formFeedback?.split(";")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

    val videoMetadata = buildVideoMetadata(this)
    val componentScores = parseComponentScores(componentScoresJson)
    val stabilityMetrics = parseStabilityMetrics(stabilityJson)
    val timeline = parseTimeline(timelineJson)

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
        createdAt = createdAt ?: "",
        videoMetadata = videoMetadata,
        componentScores = componentScores,
        stabilityMetrics = stabilityMetrics,
        timeline = timeline
    )
}

private fun buildVideoMetadata(dto: AnalysisResponseDto): VideoMetadata? {
    // Only build metadata if at least duration or frame count is available
    if (dto.videoDurationMs == null && dto.totalFrames == null) return null
    return VideoMetadata(
        durationMs = dto.videoDurationMs ?: 0L,
        width = dto.videoWidth ?: 0,
        height = dto.videoHeight ?: 0,
        originalFps = dto.originalFps ?: 0.0,
        totalFrames = dto.totalFrames ?: 0,
        analyzedFrames = dto.analyzedFrames ?: 0,
        samplingFps = dto.samplingFps ?: 0.0,
        fileSizeBytes = dto.fileSizeBytes ?: 0L
    )
}

/**
 * Parse JSON: {"elbow_extension": 85.0, "hip_extension": 70.0, "shoulder_position": 78.0, ...}
 * Keys match what the AI service's component_scores dict uses.
 */
private fun parseComponentScores(json: String?): ComponentScores? {
    if (json.isNullOrBlank()) return null
    return try {
        val obj = JSONObject(json)
        ComponentScores(
            bodyAlignment = obj.optDoubleOrNull("body_alignment"),
            elbowExtension = obj.optDoubleOrNull("elbow_extension"),
            shoulderPosition = obj.optDoubleOrNull("shoulder_position"),
            hipExtension = obj.optDoubleOrNull("hip_extension"),
            stability = obj.optDoubleOrNull("stability"),
            overall = obj.optDoubleOrNull("overall")
        )
    } catch (e: Exception) {
        null
    }
}

private fun parseStabilityMetrics(json: String?): StabilityMetrics? {
    if (json.isNullOrBlank()) return null
    return try {
        val obj = JSONObject(json)
        StabilityMetrics(
            averageBodyLineDeviation = obj.optDouble("average_body_line_deviation", 0.0),
            maxBodyLineDeviation = obj.optDouble("max_body_line_deviation", 0.0),
            shoulderPositionVariation = obj.optDouble("shoulder_position_variation", 0.0),
            hipPositionVariation = obj.optDouble("hip_position_variation", 0.0),
            averagePoseConfidence = obj.optDouble("average_pose_confidence", 0.0),
            stabilityScore = obj.optDouble("stability_score", 0.0)
        )
    } catch (e: Exception) {
        null
    }
}

private fun parseTimeline(json: String?): List<FrameMetric> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            try {
                val obj = arr.getJSONObject(i)
                val keyIssues = mutableListOf<String>()
                val issuesArr = obj.optJSONArray("key_issues")
                if (issuesArr != null) {
                    for (j in 0 until issuesArr.length()) keyIssues.add(issuesArr.getString(j))
                }
                FrameMetric(
                    frameIndex = obj.optInt("frame_index", i),
                    timestampSec = obj.optDouble("timestamp_sec", 0.0),
                    poseDetected = obj.optBoolean("pose_detected", false),
                    formScore = obj.optDouble("form_score", 0.0),
                    isValidHold = obj.optBoolean("is_valid_hold", false),
                    holdState = obj.optString("hold_state").takeIf { it.isNotEmpty() },
                    detectedProgression = obj.optString("detected_progression", ""),
                    keyIssues = keyIssues,
                    elbowAngle = obj.optDoubleOrNull("elbow_angle"),
                    shoulderAngle = obj.optDoubleOrNull("shoulder_angle"),
                    hipAlignment = obj.optDoubleOrNull("hip_alignment"),
                    bodyLineDeviation = obj.optDoubleOrNull("body_line_deviation"),
                    shoulderLean = obj.optDoubleOrNull("shoulder_lean"),
                    kneeAngle = obj.optDoubleOrNull("knee_angle"),
                    poseConfidence = obj.optDoubleOrNull("pose_confidence")
                )
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/** Returns null instead of Double.NaN or 0.0 when the key is absent. */
private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val v = optDouble(key, Double.NaN)
    return if (v.isNaN()) null else v
}
