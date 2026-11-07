package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.data.remote.dto.*

interface BodyMetricsRepository {
    suspend fun getCurrentBodyMetrics(): Result<CurrentBodyMetricsDto>
    suspend fun recordBodyMeasurement(request: RecordBodyMeasurementRequestDto): Result<BodyMeasurementDto>
    suspend fun updateHeight(heightCm: Double): Result<CurrentBodyMetricsDto>
    suspend fun getBodyMetricsHistory(
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 0,
        size: Int = 50,
        sort: String = "desc"
    ): Result<BodyMetricsHistoryDto>
    suspend fun getCoachClientBodyMetrics(
        clientId: Long,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 0,
        size: Int = 50,
        sort: String = "asc"
    ): Result<CoachClientBodyMetricsDto>
}

