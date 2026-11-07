package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RecordBodyMeasurementRequestDto(
    @SerializedName("weightKg") val weightKg: Double,
    @SerializedName("bodyFatPercentage") val bodyFatPercentage: Double? = null,
    @SerializedName("waistCm") val waistCm: Double? = null,
    @SerializedName("chestCm") val chestCm: Double? = null,
    @SerializedName("armCm") val armCm: Double? = null,
    @SerializedName("thighCm") val thighCm: Double? = null,
    @SerializedName("measuredAt") val measuredAt: String
)

data class UpdateHeightRequestDto(
    @SerializedName("heightCm") val heightCm: Double
)

data class BodyMeasurementDto(
    @SerializedName("id") val id: Long,
    @SerializedName("clientId") val clientId: Long,
    @SerializedName("weightKg") val weightKg: Double,
    @SerializedName("bodyFatPercentage") val bodyFatPercentage: Double? = null,
    @SerializedName("waistCm") val waistCm: Double? = null,
    @SerializedName("chestCm") val chestCm: Double? = null,
    @SerializedName("armCm") val armCm: Double? = null,
    @SerializedName("thighCm") val thighCm: Double? = null,
    @SerializedName("measuredAt") val measuredAt: String,
    @SerializedName("createdAt") val createdAt: String
)

data class CurrentBodyMetricsDto(
    @SerializedName("heightCm") val heightCm: Double? = null,
    @SerializedName("weightKg") val weightKg: Double? = null,
    @SerializedName("bodyFatPercentage") val bodyFatPercentage: Double? = null,
    @SerializedName("waistCm") val waistCm: Double? = null,
    @SerializedName("chestCm") val chestCm: Double? = null,
    @SerializedName("armCm") val armCm: Double? = null,
    @SerializedName("thighCm") val thighCm: Double? = null,
    @SerializedName("measuredAt") val measuredAt: String? = null,
    @SerializedName("totalMeasurementsCount") val totalMeasurementsCount: Long = 0L
)

data class BodyMetricsHistoryDto(
    @SerializedName("content") val content: List<BodyMeasurementDto> = emptyList(),
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 50,
    @SerializedName("totalElements") val totalElements: Long = 0L,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("currentHeightCm") val currentHeightCm: Double? = null
)

data class CoachClientBodyMetricsDto(
    @SerializedName("clientId") val clientId: Long,
    @SerializedName("clientName") val clientName: String,
    @SerializedName("currentHeightCm") val currentHeightCm: Double? = null,
    @SerializedName("currentMetrics") val currentMetrics: CurrentBodyMetricsDto? = null,
    @SerializedName("measurements") val measurements: List<BodyMeasurementDto> = emptyList(),
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 50,
    @SerializedName("totalElements") val totalElements: Long = 0L,
    @SerializedName("totalPages") val totalPages: Int = 0
)

