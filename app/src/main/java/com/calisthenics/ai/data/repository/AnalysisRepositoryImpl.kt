package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.data.remote.datasource.RemoteAnalysisDataSource
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.QuotaEntitlement
import com.calisthenics.ai.domain.model.UserUsage
import com.calisthenics.ai.domain.model.VideoKeypointsDataModel
import com.calisthenics.ai.domain.repository.AnalysisRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import javax.inject.Inject

class AnalysisRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteAnalysisDataSource
) : AnalysisRepository {

    private var cachedUserUsage: UserUsage? = null
    private val _usageUpdateEvents = MutableSharedFlow<UserUsage?>(replay = 0, extraBufferCapacity = 1)
    override val usageUpdateEvents: SharedFlow<UserUsage?> = _usageUpdateEvents.asSharedFlow()

    override fun getCachedUserUsage(): UserUsage? = cachedUserUsage

    override fun invalidateUserUsageCache() {
        cachedUserUsage = null
        _usageUpdateEvents.tryEmit(null)
    }

    override suspend fun getUserUsage(): Result<UserUsage> = runApiCall {
        val domain = remoteDataSource.getUserUsage().toDomain()
        cachedUserUsage = domain
        _usageUpdateEvents.tryEmit(domain)
        domain
    }

    private inline fun <T> runApiCall(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(NetworkErrorParser.toPreservedException(t))
        }
    }

    override suspend fun uploadVideoForAnalysis(
        videoFile: File,
        exerciseId: Long?,
        exerciseType: String?,
        idempotencyKey: String?,
        sideView: Boolean
    ): Result<AnalysisResult> = runApiCall {
        val result = remoteDataSource.uploadVideoForAnalysis(
            videoFile = videoFile,
            exerciseId = exerciseId,
            exerciseType = exerciseType,
            idempotencyKey = idempotencyKey,
            sideView = sideView
        ).toDomain()
        invalidateUserUsageCache()
        result
    }

    override suspend fun getVideoAnalysisById(id: Long): Result<AnalysisResult> = runApiCall {
        val analysis = remoteDataSource.getVideoAnalysisById(id).toDomain()
        if (analysis.status == "COMPLETED") {
            val keypoints = runCatching { remoteDataSource.getVideoAnalysisKeypoints(id).toDomain() }.getOrNull()
            analysis.copy(keypointsData = keypoints)
        } else {
            analysis
        }
    }

    override suspend fun getVideoAnalysisKeypoints(id: Long): Result<VideoKeypointsDataModel> = runApiCall {
        remoteDataSource.getVideoAnalysisKeypoints(id).toDomain()
    }

    override suspend fun getVideoAnalysisQuota(): Result<QuotaEntitlement> = runApiCall {
        remoteDataSource.getVideoAnalysisQuota().toDomain()
    }

    override suspend fun getUserEntitlements(): Result<QuotaEntitlement> = runApiCall {
        remoteDataSource.getUserEntitlements().toDomain()
    }

    override suspend fun updateMembershipTier(tier: String): Result<QuotaEntitlement> = runApiCall {
        val result = remoteDataSource.updateMembershipTier(tier).toDomain()
        invalidateUserUsageCache()
        result
    }

    override suspend fun getVideoAnalysisHistory(): Result<List<AnalysisResult>> = runApiCall {
        remoteDataSource.getVideoAnalysisHistory().map { it.toDomain() }
    }

    override suspend fun analyzeVideo(
        videoFile: File,
        exerciseId: Long?
    ): Result<AnalysisResult> = runApiCall {
        remoteDataSource.analyzeVideo(videoFile, exerciseId).toDomain()
    }

    override suspend fun analyzeImage(
        imageFile: File,
        exerciseId: Long?
    ): Result<AnalysisResult> = runApiCall {
        remoteDataSource.analyzeImage(imageFile, exerciseId).toDomain()
    }

    override suspend fun getAnalysisHistory(): Result<List<AnalysisResult>> = runApiCall {
        val videoAnalyses = runCatching {
            remoteDataSource.getVideoAnalysisHistory().map { it.toDomain() }
        }.getOrDefault(emptyList())

        val legacyAnalyses = runCatching {
            remoteDataSource.getAnalysisHistory().map { it.toDomain() }
        }.getOrDefault(emptyList())

        (videoAnalyses + legacyAnalyses).sortedByDescending { it.createdAt }
    }

    override suspend fun getAnalysisById(id: Long): Result<AnalysisResult> = runApiCall {
        // First try video analysis endpoint
        try {
            val videoAnalysis = remoteDataSource.getVideoAnalysisById(id).toDomain()
            val keypoints = if (videoAnalysis.status == "COMPLETED") {
                runCatching { remoteDataSource.getVideoAnalysisKeypoints(id).toDomain() }.getOrNull()
            } else {
                null
            }
            videoAnalysis.copy(keypointsData = keypoints)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val statusCode = NetworkErrorParser.httpStatusCode(e)
            if (statusCode == 401 || statusCode == 403 || statusCode == 429) {
                throw e
            }
            // Fallback to legacy endpoint
            remoteDataSource.getAnalysisById(id).toDomain()
        }
    }
}
