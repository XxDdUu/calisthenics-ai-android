package com.calisthenics.ai.domain.model

/**
 * Original video metadata extracted from the uploaded file.
 * Duration is always from the original video — never inferred from sampled frames.
 */
data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val originalFps: Double,
    val totalFrames: Int,
    val analyzedFrames: Int,
    val samplingFps: Double,
    val fileSizeBytes: Long
)

/**
 * Aggregated component scores from the AI biomechanical assessment.
 * Each score is 0–100; null means the metric could not be reliably computed.
 */
data class ComponentScores(
    val bodyAlignment: Double?,
    val elbowExtension: Double?,
    val shoulderPosition: Double?,
    val hipExtension: Double?,
    val stability: Double?,
    val overall: Double?
)

/**
 * Stability metrics computed across all analyzed frames with valid pose.
 */
data class StabilityMetrics(
    val averageBodyLineDeviation: Double,
    val maxBodyLineDeviation: Double,
    val shoulderPositionVariation: Double,
    val hipPositionVariation: Double,
    val averagePoseConfidence: Double,
    val stabilityScore: Double
)

/**
 * Per-frame biomechanical snapshot for timeline charts and frame inspection.
 * Fields are null when the pose was not detected or the landmark was unreliable.
 */
data class FrameMetric(
    val frameIndex: Int,
    val timestampSec: Double,
    val poseDetected: Boolean,
    val formScore: Double,
    val isValidHold: Boolean,
    val holdState: String?,
    val detectedProgression: String,
    val keyIssues: List<String>,
    // Per-frame biomechanical angles
    val elbowAngle: Double?,
    val shoulderAngle: Double?,
    val hipAlignment: Double?,
    val bodyLineDeviation: Double?,
    val shoulderLean: Double?,
    val kneeAngle: Double?,
    val poseConfidence: Double?
)

/**
 * Keypoint joint coordinate model for video timeline synchronization.
 */
data class KeypointItemModel(
    val name: String,
    val x: Float,
    val y: Float,
    val confidence: Float
)

data class KeypointFrameModel(
    val frameIndex: Int,
    val timestampMs: Long,
    val keypoints: List<KeypointItemModel>
)

data class VideoKeypointsDataModel(
    val analysisId: Long = 0L,
    val format: String = "COCO_17",
    val coordinateSystem: String = "NORMALIZED",
    val totalFrames: Int = 0,
    val frames: List<KeypointFrameModel> = emptyList()
)

data class QuotaEntitlement(
    val userId: Long = 0L,
    val membershipTier: String = "FREE",
    val dailyLimit: Int? = 1,
    val usedToday: Int = 0,
    val remainingToday: Int? = 1,
    val liveAnalysisUnlocked: Boolean = false,
    val timezone: String = "Asia/Ho_Chi_Minh",
    val quotaDate: String? = null,
    val resetsAt: String? = null,
    val isUnlimited: Boolean = false,
    val grantSource: String? = null,
    val adminGrantedPro: Boolean = false
) {
    val isAdmin: Boolean
        get() = membershipTier.equals("ADMIN", ignoreCase = true)

    val isPro: Boolean
        get() = isAdmin || membershipTier.equals("PRO", ignoreCase = true)

    val isExhausted: Boolean
        get() = if (isAdmin || isUnlimited) false else (remainingToday ?: 0) <= 0
}

data class RepBreakdown(
    val repNumber: Int,
    val valid: Boolean = true,
    val status: String = if (valid) "VALID" else "INCOMPLETE",
    val formScore: Double? = null,
    val startTimestampMs: Long = 0L,
    val endTimestampMs: Long? = null,
    val durationMs: Long? = null,
    val minAngleDeg: Double? = null,
    val romDeg: Double? = null,
    val depthClassification: String? = null,
    val issues: List<String> = emptyList(),
    val feedback: String? = null
)

data class MovementMetric(
    val key: String = "",
    val name: String = "",
    val value: Double? = null,
    val unit: String = "",
    val targetRange: String? = null,
    val score: Double? = null,
    val confidence: Double? = null,
    val status: String = "UNAVAILABLE",
    val unavailableReason: String? = null
) {
    val isUnavailable: Boolean
        get() = status.equals("UNAVAILABLE", ignoreCase = true) ||
            status.equals("INSUFFICIENT_DATA", ignoreCase = true) ||
            value == null
}

enum class IssueSeverity {
    LOW, MEDIUM, HIGH;

    companion object {
        fun fromString(raw: String?): IssueSeverity = when (raw?.trim()?.uppercase()) {
            "HIGH", "CRITICAL", "ERROR" -> HIGH
            "LOW", "INFO" -> LOW
            else -> MEDIUM
        }
    }
}

data class StructuredIssue(
    val code: String = "",
    val title: String = "",
    val message: String = "",
    val severity: IssueSeverity = IssueSeverity.MEDIUM,
    val affectedReps: List<Int> = emptyList(),
    val recommendation: String? = null,
    val measuredValue: Double? = null,
    val targetRange: String? = null,
    val timestampsMs: List<Long> = emptyList()
)

data class CameraQuality(
    val overallQuality: String = "UNKNOWN",
    val viewAngle: String = "UNKNOWN",
    val fullBodyVisibleRatio: Double? = null,
    val averageConfidence: Double? = null,
    val warnings: List<String> = emptyList(),
    val sideViewDetected: Boolean? = null
)

/**
 * Complete AI analysis result for a single video or image upload.
 */
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
    val createdAt: String,
    // Video metadata (populated only for VIDEO media type)
    val videoMetadata: VideoMetadata?,
    // Aggregated biomechanical data
    val componentScores: ComponentScores?,
    val stabilityMetrics: StabilityMetrics?,
    // Timeline for charts and frame inspection
    val timeline: List<FrameMetric>,
    // Video Comparison & Keypoints Streaming fields
    val originalVideoUrl: String? = null,
    val annotatedVideoUrl: String? = null,
    val status: String = "COMPLETED",
    val keypointsUrl: String? = null,
    val keypointsData: VideoKeypointsDataModel? = null,
    // Contract v1.0 fields
    val schemaVersion: String? = null,
    val exerciseCode: String? = null,
    val formAccuracy: Double? = null,
    val stabilityScore: Double? = null,
    val alignmentScore: Double? = null,
    val repCount: Int = 0,
    val validReps: Int = 0,
    val incompleteReps: Int = 0,
    val reps: List<RepBreakdown> = emptyList(),
    val movementMetrics: Map<String, MovementMetric> = emptyMap(),
    val issues: List<StructuredIssue> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val cameraQuality: CameraQuality? = null,
    val quota: QuotaEntitlement? = null,
    val errorMessage: String? = null,
    val updatedAt: String? = null
)
