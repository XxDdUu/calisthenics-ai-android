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
    val timeline: List<FrameMetric>
)
