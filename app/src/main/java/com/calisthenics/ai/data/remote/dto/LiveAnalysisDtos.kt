package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LiveAnalysisResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("exercise") val exercise: String? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName(value = "current_score", alternate = ["currentScore"]) val currentScore: Double? = null,
    @SerializedName(value = "score_confidence", alternate = ["scoreConfidence"]) val scoreConfidence: Double? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName(value = "poseDetected", alternate = ["pose_detected"]) val poseDetected: Boolean? = null,
    @SerializedName(value = "isValidHold", alternate = ["is_valid_hold"]) val isValidHold: Boolean? = null,
    @SerializedName(value = "holdDurationSec", alternate = ["hold_duration_sec"]) val holdDurationSec: Double? = null,
    @SerializedName(value = "hold_duration_ms", alternate = ["holdDurationMs"]) val holdDurationMs: Long? = null,
    @SerializedName(value = "repCount", alternate = ["rep_count"]) val repCount: Int? = null,
    @SerializedName(value = "repPhase", alternate = ["rep_phase"]) val repPhase: String? = null,
    @SerializedName("phase") val phase: String? = null,
    @SerializedName(value = "currentState", alternate = ["current_state"]) val currentState: String? = null,
    @SerializedName(value = "kneeAngle", alternate = ["knee_angle"]) val kneeAngle: Double? = null,
    @SerializedName(value = "minKneeAngle", alternate = ["min_knee_angle"]) val minKneeAngle: Double? = null,
    @SerializedName(value = "elbowAngle", alternate = ["elbow_angle", "average_elbow_angle", "averageElbowAngle"]) val elbowAngle: Double? = null,
    @SerializedName(value = "minElbowAngle", alternate = ["min_elbow_angle"]) val minElbowAngle: Double? = null,
    @SerializedName(value = "bodyAlignment", alternate = ["body_alignment"]) val bodyAlignment: Double? = null,
    @SerializedName("depth") val depth: String? = null,
    @SerializedName(value = "isTracking", alternate = ["is_tracking"]) val isTracking: Boolean? = null,
    @SerializedName("measurements") val measurements: Map<String, Double> = emptyMap(),
    @SerializedName(value = "metrics", alternate = ["scoring_metrics", "scoringMetrics"]) val metrics: List<LiveScoringMetricDto> = emptyList(),
    @SerializedName(value = "missingLandmarks", alternate = ["missing_landmarks"]) val missingLandmarks: List<String> = emptyList(),
    @SerializedName("feedback") val feedback: List<LiveFeedbackDto> = emptyList(),
    @SerializedName("landmarks") val landmarks: List<LiveLandmarkDto> = emptyList(),
    @SerializedName(value = "sessionId", alternate = ["session_id"]) val sessionId: String? = null,
    @SerializedName("timing") val timing: LiveTimingDto? = null,
    @SerializedName(value = "session_summary", alternate = ["sessionSummary"]) val sessionSummary: LiveSessionSummaryDto? = null
)

data class LiveScoringMetricDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("value") val value: Double? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("weight") val weight: Double? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("status") val status: String? = null
)

data class LiveSessionSummaryDto(
    @SerializedName(value = "sessionId", alternate = ["session_id"]) val sessionId: String? = null,
    @SerializedName("exercise") val exercise: String? = null,
    @SerializedName(value = "totalFrames", alternate = ["total_frames"]) val totalFrames: Int? = null,
    @SerializedName(value = "durationSec", alternate = ["duration_sec"]) val durationSec: Double? = null,
    @SerializedName(value = "averageScore", alternate = ["average_score"]) val averageScore: Double? = null,
    @SerializedName(value = "peakScore", alternate = ["peak_score"]) val peakScore: Double? = null,
    @SerializedName(value = "lowestScore", alternate = ["lowest_score"]) val lowestScore: Double? = null,
    @SerializedName(value = "peakHoldDurationSec", alternate = ["peak_hold_duration_sec"]) val peakHoldDurationSec: Double? = null,
    @SerializedName(value = "totalReps", alternate = ["total_reps"]) val totalReps: Int? = null
)

data class LiveFeedbackDto(
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("severity") val severity: String? = null
)

data class LiveLandmarkDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("x") val x: Double? = null,
    @SerializedName("y") val y: Double? = null,
    @SerializedName(value = "x_norm", alternate = ["xNorm"]) val xNorm: Double? = null,
    @SerializedName(value = "y_norm", alternate = ["yNorm"]) val yNorm: Double? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("visible") val visible: Boolean? = null
)

data class LiveTimingDto(
    @SerializedName(value = "preprocessing_ms", alternate = ["preprocessingMs"]) val preprocessingMs: Double? = null,
    @SerializedName(value = "inference_ms", alternate = ["inferenceMs"]) val inferenceMs: Double? = null,
    @SerializedName(value = "analysis_ms", alternate = ["analysisMs"]) val analysisMs: Double? = null,
    @SerializedName(value = "total_ms", alternate = ["totalMs"]) val totalMs: Double? = null
)

data class LiveExerciseCatalogDto(
    @SerializedName("supported_exercises") val supportedExercises: List<SupportedExerciseDto> = emptyList(),
    @SerializedName("all_catalog") val allCatalog: List<SupportedExerciseDto> = emptyList()
)

data class SupportedExerciseDto(
    @SerializedName("exercise") val exercise: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("camera_view") val cameraView: String? = null,
    @SerializedName("variations") val variations: List<String> = emptyList()
)

data class CreateAnalysisSessionRequestDto(
    @SerializedName(value = "exerciseType", alternate = ["exerciseCode"]) val exerciseType: String,
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("cameraView") val cameraView: String? = "SIDE"
)

data class ExerciseFormMetricDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("value") val value: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("unavailableReason") val unavailableReason: String? = null
)

data class DetectedFormErrorDto(
    @SerializedName("code") val code: String? = null,
    @SerializedName("errorName") val errorName: String? = null,
    @SerializedName("occurrences") val occurrences: Int? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("timestampsMs") val timestampsMs: List<Long>? = null,
    @SerializedName("repNumbers") val repNumbers: List<Int>? = null,
    @SerializedName("suggestedCorrection") val suggestedCorrection: String? = null
)

data class AiPerformanceSummaryDto(
    @SerializedName("overallAssessment") val overallAssessment: String? = null,
    @SerializedName("strengths") val strengths: List<String>? = null,
    @SerializedName("weaknesses") val weaknesses: List<String>? = null,
    @SerializedName("recommendations") val recommendations: List<String>? = null,
    @SerializedName("nextSessionFocus") val nextSessionFocus: String? = null,
    @SerializedName("modelUsed") val modelUsed: String? = null,
    @SerializedName("inputTokens") val inputTokens: Int? = null,
    @SerializedName("outputTokens") val outputTokens: Int? = null,
    @SerializedName("latencyMs") val latencyMs: Long? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("generatedAt") val generatedAt: String? = null
)

data class RepHistoryDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("repNumber") val repNumber: Int,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("durationMs") val durationMs: Long? = null,
    @SerializedName("timestampOffsetMs") val timestampOffsetMs: Long? = null,
    @SerializedName(value = "minimumKneeAngle", alternate = ["minKneeAngle"]) val minimumKneeAngle: Double? = null,
    @SerializedName(value = "minimumJointAngle", alternate = ["minJointAngle", "minimumElbowAngle", "minElbowAngle", "elbowAngle"]) val minimumJointAngle: Double? = null,
    @SerializedName(value = "formScore", alternate = ["score"]) val formScore: Double? = null,
    @SerializedName(value = "depthRating", alternate = ["depth"]) val depthRating: String? = null,
    @SerializedName("completionStatus") val completionStatus: String? = null,
    @SerializedName("feedback") val feedback: String? = null,
    @SerializedName("detectedErrors") val detectedErrors: List<String>? = null,
    @SerializedName("metrics") val metrics: Map<String, Double>? = null
)

data class CompleteAnalysisSessionRequestDto(
    @SerializedName("exerciseId") val exerciseId: Long? = null,
    @SerializedName("totalReps") val totalReps: Int = 0,
    @SerializedName("durationSeconds") val durationSeconds: Double = 0.0,
    @SerializedName("overallScore") val overallScore: Double? = null,
    @SerializedName(value = "averageFormScore", alternate = ["averageScore"]) val averageFormScore: Double? = null,
    @SerializedName(value = "bestFormScore", alternate = ["peakScore"]) val bestFormScore: Double? = null,
    @SerializedName(value = "lowestFormScore", alternate = ["lowestScore"]) val lowestFormScore: Double? = null,
    @SerializedName("analysisConfidence") val analysisConfidence: Double? = null,
    @SerializedName("peakHoldDurationSeconds") val peakHoldDurationSeconds: Double? = null,
    @SerializedName(value = "averageKneeAngle", alternate = ["kneeAngle"]) val averageKneeAngle: Double? = null,
    @SerializedName(value = "deepestKneeAngle", alternate = ["minKneeAngle"]) val deepestKneeAngle: Double? = null,
    @SerializedName(value = "averageJointAngle", alternate = ["averageElbowAngle"]) val averageJointAngle: Double? = null,
    @SerializedName(value = "deepestJointAngle", alternate = ["deepestElbowAngle"]) val deepestJointAngle: Double? = null,
    @SerializedName("correctReps") val correctReps: Int? = null,
    @SerializedName("incorrectReps") val incorrectReps: Int? = null,
    @SerializedName("analysisStatus") val analysisStatus: String = "COMPLETED",
    @SerializedName("exerciseMetrics") val exerciseMetrics: List<ExerciseFormMetricDto> = emptyList(),
    @SerializedName("detectedErrors") val detectedErrors: List<DetectedFormErrorDto> = emptyList(),
    @SerializedName("originalVideoUrl") val originalVideoUrl: String? = null,
    @SerializedName("annotatedVideoUrl") val annotatedVideoUrl: String? = null,
    @SerializedName("reps") val reps: List<RepHistoryDto> = emptyList()
)

data class AnalysisSessionResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName(value = "exerciseType", alternate = ["exerciseCode"]) val exerciseType: String,
    @SerializedName("exerciseId") val exerciseId: Long? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("durationSeconds") val durationSeconds: Double? = null,
    @SerializedName("totalReps") val totalReps: Int = 0,
    @SerializedName("overallScore") val overallScore: Double? = null,
    @SerializedName("averageFormScore") val averageFormScore: Double? = null,
    @SerializedName("bestFormScore") val bestFormScore: Double? = null,
    @SerializedName("lowestFormScore") val lowestFormScore: Double? = null,
    @SerializedName("analysisConfidence") val analysisConfidence: Double? = null,
    @SerializedName("peakHoldDurationSeconds") val peakHoldDurationSeconds: Double? = null,
    @SerializedName("averageKneeAngle") val averageKneeAngle: Double? = null,
    @SerializedName("deepestKneeAngle") val deepestKneeAngle: Double? = null,
    @SerializedName(value = "averageJointAngle", alternate = ["averageElbowAngle"]) val averageJointAngle: Double? = null,
    @SerializedName(value = "deepestJointAngle", alternate = ["deepestElbowAngle"]) val deepestJointAngle: Double? = null,
    @SerializedName("correctReps") val correctReps: Int? = null,
    @SerializedName("incorrectReps") val incorrectReps: Int? = null,
    @SerializedName("analysisStatus") val analysisStatus: String? = null,
    @SerializedName("cameraView") val cameraView: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("exerciseMetrics") val exerciseMetrics: List<ExerciseFormMetricDto>? = null,
    @SerializedName("detectedErrors") val detectedErrors: List<DetectedFormErrorDto>? = null,
    @SerializedName("aiSummary") val aiSummary: String? = null,
    @SerializedName("aiRecommendations") val aiRecommendations: AiPerformanceSummaryDto? = null,
    @SerializedName("aiSummaryStatus") val aiSummaryStatus: String? = null,
    @SerializedName("originalVideoUrl") val originalVideoUrl: String? = null,
    @SerializedName("annotatedVideoUrl") val annotatedVideoUrl: String? = null,
    @SerializedName("reps") val reps: List<RepHistoryDto>? = null
)

data class AnalysisHistorySummaryDto(
    @SerializedName("totalSessions") val totalSessions: Long = 0,
    @SerializedName("totalReps") val totalReps: Long = 0,
    @SerializedName("overallAverageScore") val overallAverageScore: Double? = null,
    @SerializedName("totalDurationSeconds") val totalDurationSeconds: Double = 0.0,
    @SerializedName("mostFrequentExercise") val mostFrequentExercise: String? = null
)
