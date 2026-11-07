package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.repository.BodyMetricsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyMetricsRepositoryImpl @Inject constructor(
    private val api: CalisthenicsApi
) : BodyMetricsRepository {

    override suspend fun getCurrentBodyMetrics(): Result<CurrentBodyMetricsDto> = runCatching {
        api.getCurrentBodyMetrics()
    }

    override suspend fun recordBodyMeasurement(request: RecordBodyMeasurementRequestDto): Result<BodyMeasurementDto> = runCatching {
        api.recordBodyMeasurement(request)
    }

    override suspend fun updateHeight(heightCm: Double): Result<CurrentBodyMetricsDto> = runCatching {
        api.updateHeight(UpdateHeightRequestDto(heightCm))
    }

    override suspend fun getBodyMetricsHistory(
        startDate: String?,
        endDate: String?,
        page: Int,
        size: Int,
        sort: String
    ): Result<BodyMetricsHistoryDto> = runCatching {
        api.getBodyMetricsHistory(startDate, endDate, page, size, sort)
    }

    override suspend fun getCoachClientBodyMetrics(
        clientId: Long,
        startDate: String?,
        endDate: String?,
        page: Int,
        size: Int,
        sort: String
    ): Result<CoachClientBodyMetricsDto> = runCatching {
        api.getCoachClientBodyMetrics(clientId, startDate, endDate, page, size, sort)
    }
}

