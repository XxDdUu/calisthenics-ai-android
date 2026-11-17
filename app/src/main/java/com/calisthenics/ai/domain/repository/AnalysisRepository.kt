package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.QuotaEntitlement
import com.calisthenics.ai.domain.model.UserUsage
import com.calisthenics.ai.domain.model.VideoKeypointsDataModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

interface AnalysisRepository {
    val usageUpdateEvents: SharedFlow<UserUsage?>
        get() = MutableSharedFlow()

    suspend fun getUserUsage(): Result<UserUsage>

    fun getCachedUserUsage(): UserUsage? = null

    fun invalidateUserUsageCache() {}

    suspend fun uploadVideoForAnalysis(
        videoFile: File,
        exerciseId: Long?,
        exerciseType: String? = null,
        idempotencyKey: String? = null,
        sideView: Boolean = false
    ): Result<AnalysisResult>

    suspend fun getVideoAnalysisById(id: Long): Result<AnalysisResult>

    suspend fun getVideoAnalysisKeypoints(id: Long): Result<VideoKeypointsDataModel>

    suspend fun getVideoAnalysisQuota(): Result<QuotaEntitlement>

    suspend fun getUserEntitlements(): Result<QuotaEntitlement>

    suspend fun updateMembershipTier(tier: String): Result<QuotaEntitlement>

    suspend fun getVideoAnalysisHistory(): Result<List<AnalysisResult>>

    suspend fun analyzeVideo(
        videoFile: File,
        exerciseId: Long?
    ): Result<AnalysisResult>

    suspend fun analyzeImage(
        imageFile: File,
        exerciseId: Long?
    ): Result<AnalysisResult>

    suspend fun getAnalysisHistory(): Result<List<AnalysisResult>>
    suspend fun getAnalysisById(id: Long): Result<AnalysisResult>
}
