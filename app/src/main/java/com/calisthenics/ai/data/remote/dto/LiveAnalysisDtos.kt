package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LiveAnalysisResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("exercise") val exercise: String? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("poseDetected") val poseDetected: Boolean? = null,
    @SerializedName("isValidHold") val isValidHold: Boolean? = null,
    @SerializedName("holdDurationSec") val holdDurationSec: Double? = null,
    @SerializedName("repCount") val repCount: Int? = null,
    @SerializedName("repPhase") val repPhase: String? = null,
    @SerializedName("measurements") val measurements: Map<String, Double> = emptyMap(),
    @SerializedName("missingLandmarks") val missingLandmarks: List<String> = emptyList(),
    @SerializedName("feedback") val feedback: List<LiveFeedbackDto> = emptyList(),
    @SerializedName("landmarks") val landmarks: List<LiveLandmarkDto> = emptyList(),
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("timing") val timing: LiveTimingDto? = null
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
