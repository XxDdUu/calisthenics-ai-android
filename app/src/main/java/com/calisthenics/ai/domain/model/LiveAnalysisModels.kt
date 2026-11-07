package com.calisthenics.ai.domain.model

data class LiveAnalysisResult(
    val status: String,
    val exercise: String,
    val score: Double? = null,
    val confidence: Double? = null,
    val poseDetected: Boolean = false,
    val isValidHold: Boolean = false,
    val holdDurationSec: Double? = null,
    val repCount: Int? = null,
    val repPhase: String? = null,
    val measurements: Map<String, Double> = emptyMap(),
    val missingLandmarks: List<String> = emptyList(),
    val feedback: List<LiveFeedback> = emptyList(),
    val landmarks: List<LiveLandmark> = emptyList(),
    val timing: LiveTiming? = null,
    val sessionId: String? = null
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
