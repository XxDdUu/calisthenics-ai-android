package com.calisthenics.ai.domain.model

data class LiveAnalysisResult(
    val status: String,
    val exercise: String,
    val score: Double? = null,
    val currentScore: Double? = score,
    val scoreConfidence: Double? = null,
    val confidence: Double? = null,
    val poseDetected: Boolean = false,
    val isValidHold: Boolean = false,
    val holdDurationSec: Double? = null,
    val holdDurationMs: Long? = null,
    val repCount: Int? = null,
    val repPhase: String? = null,
    val phase: String? = repPhase,
    val currentState: String? = null,
    val kneeAngle: Double? = null,
    val minKneeAngle: Double? = null,
    val elbowAngle: Double? = null,
    val minElbowAngle: Double? = null,
    val bodyAlignment: Double? = null,
    val depth: String? = null,
    val isTracking: Boolean? = null,
    val measurements: Map<String, Double> = emptyMap(),
    val scoringMetrics: List<LiveScoringMetric> = emptyList(),
    val missingLandmarks: List<String> = emptyList(),
    val feedback: List<LiveFeedback> = emptyList(),
    val landmarks: List<LiveLandmark> = emptyList(),
    val timing: LiveTiming? = null,
    val sessionId: String? = null,
    val sessionSummary: LiveSessionSummary? = null
)

data class LiveScoringMetric(
    val name: String,
    val value: Double,
    val score: Double,
    val weight: Double,
    val confidence: Double,
    val status: String
)

data class LiveSessionSummary(
    val sessionId: String,
    val exercise: String,
    val totalFrames: Int,
    val durationSec: Double,
    val averageScore: Double,
    val peakScore: Double,
    val lowestScore: Double,
    val peakHoldDurationSec: Double,
    val totalReps: Int
)

data class LiveFeedback(
    val code: String,
    val message: String,
    val severity: String
)

data class LiveLandmark(
    val name: String,
    val x: Double,
    val y: Double,
    val xNorm: Double,
    val yNorm: Double,
    val confidence: Double,
    val visible: Boolean
)

data class LiveTiming(
    val preprocessingMs: Double? = null,
    val inferenceMs: Double? = null,
    val analysisMs: Double? = null,
    val totalMs: Double? = null
)

data class SupportedExercise(
    val exercise: String,
    val name: String,
    val type: String,
    val cameraView: String,
    val variations: List<String>
)

data class ExerciseFormMetric(
    val name: String,
    val label: String,
    val value: Double? = null,
    val unit: String = "",
    val score: Double? = null,
    val confidence: Double = 0.0,
    val status: String = "UNAVAILABLE", // VALID, LOW_CONFIDENCE, UNAVAILABLE
    val unavailableReason: String? = null
)

enum class ErrorSeverity {
    LOW, MEDIUM, HIGH;

    companion object {
        fun fromString(raw: String?): ErrorSeverity {
            return when (raw?.uppercase()?.trim()) {
                "HIGH", "ERROR" -> HIGH
                "LOW", "INFO" -> LOW
                else -> MEDIUM
            }
        }
    }
}

data class DetectedFormError(
    val code: String,
    val errorName: String,
    val occurrences: Int,
    val severity: ErrorSeverity = ErrorSeverity.MEDIUM,
    val timestampsMs: List<Long> = emptyList(),
    val repNumbers: List<Int> = emptyList(),
    val suggestedCorrection: String? = null
)

data class AiPerformanceSummary(
    val overallAssessment: String? = null,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val nextSessionFocus: String? = null,
    val modelUsed: String? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val latencyMs: Long? = null,
    val status: String = "PENDING", // PENDING, COMPLETED, UNAVAILABLE
    val generatedAt: String? = null
)

data class LiveRepRecord(
    val id: Long? = null,
    val repNumber: Int,
    val score: Double? = null,
    val minKneeAngle: Double? = null,
    val minJointAngle: Double? = null,
    val minElbowAngle: Double? = null,
    val depth: String? = null,
    val completionStatus: String? = null,
    val durationMs: Long? = null,
    val timestampOffsetMs: Long? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val isValid: Boolean = true,
    val feedback: String? = null,
    val detectedErrors: List<String> = emptyList(),
    val metrics: Map<String, Double> = emptyMap()
)

data class LiveSessionRecord(
    val id: String,
    val userId: Long? = null,
    val exerciseType: String,
    val exerciseId: Long? = null,
    val status: String = "COMPLETED",
    val totalReps: Int = 0,
    val correctReps: Int = 0,
    val incorrectReps: Int = 0,
    val overallScore: Double? = null,
    val averageScore: Double? = null,
    val peakScore: Double? = null,
    val lowestScore: Double? = null,
    val analysisConfidence: Double? = null,
    val peakHoldDurationSeconds: Double? = null,
    val averageKneeAngle: Double? = null,
    val deepestKneeAngle: Double? = null,
    val averageJointAngle: Double? = null,
    val deepestJointAngle: Double? = null,
    val durationSeconds: Double = 0.0,
    val cameraView: String? = "SIDE",
    val createdAt: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val exerciseMetrics: List<ExerciseFormMetric> = emptyList(),
    val detectedErrors: List<DetectedFormError> = emptyList(),
    val aiSummary: String? = null,
    val aiRecommendations: AiPerformanceSummary? = null,
    val aiSummaryStatus: String = "PENDING",
    val originalVideoUrl: String? = null,
    val annotatedVideoUrl: String? = null,
    val reps: List<LiveRepRecord> = emptyList()
) {
    val exerciseDisplayName: String
        get() = exerciseType.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() }

    val resolvedOverallScore: Double?
        get() = overallScore ?: averageScore

    val formConsistencyPercent: Int?
        get() {
            val validScores = reps.mapNotNull { it.score }
            if (validScores.size < 2) return null
            val mean = validScores.average()
            val variance = validScores.map { Math.pow(it - mean, 2.0) }.average()
            val stdDev = Math.sqrt(variance)
            return (100 - (stdDev * 2)).toInt().coerceIn(0, 100)
        }

    val movementQuality: String?
        get() {
            val score = resolvedOverallScore ?: return null
            return when {
                score >= 85.0 -> "EXCELLENT"
                score >= 70.0 -> "GOOD"
                score >= 50.0 -> "MODERATE"
                else -> "NEEDS_WORK"
            }
        }
}

data class LiveAnalysisSummary(
    val totalSessions: Long = 0,
    val totalReps: Long = 0,
    val overallAverageScore: Double? = null,
    val totalDurationSeconds: Double = 0.0,
    val mostFrequentExercise: String? = null
)
