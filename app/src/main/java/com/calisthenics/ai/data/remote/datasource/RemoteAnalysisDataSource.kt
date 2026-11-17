package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.AnalysisResponseDto
import com.calisthenics.ai.data.remote.dto.QuotaEntitlementDto
import com.calisthenics.ai.data.remote.dto.UpdateMembershipRequestDto
import com.calisthenics.ai.data.remote.dto.UserUsageResponseDto
import com.calisthenics.ai.data.remote.dto.VideoAnalysisKeypointsDto
import com.calisthenics.ai.data.remote.dto.VideoAnalysisResponseDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class RemoteAnalysisDataSource @Inject constructor(
    private val api: CalisthenicsApi
) {
    suspend fun uploadVideoForAnalysis(
        videoFile: File,
        exerciseId: Long?,
        exerciseType: String? = null,
        idempotencyKey: String? = null,
        sideView: Boolean = false
    ): VideoAnalysisResponseDto {
        val requestFile = videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
        val multipartPart = MultipartBody.Part.createFormData("video", videoFile.name, requestFile)
        val exerciseIdBody = exerciseId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val exerciseTypeBody = exerciseType?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
        val sideViewBody = sideView.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        return api.uploadVideoForAnalysis(
            idempotencyKey = idempotencyKey?.takeIf { it.isNotBlank() },
            video = multipartPart,
            exerciseId = exerciseIdBody,
            exerciseType = exerciseTypeBody,
            sideView = sideViewBody
        )
    }

    suspend fun getVideoAnalysisQuota(): QuotaEntitlementDto {
        return api.getVideoAnalysisQuota()
    }

    suspend fun getUserEntitlements(): QuotaEntitlementDto {
        return api.getUserEntitlements()
    }

    suspend fun getUserUsage(): UserUsageResponseDto {
        return api.getUserUsage()
    }

    suspend fun updateMembershipTier(tier: String): QuotaEntitlementDto {
        return api.updateMembershipTier(UpdateMembershipRequestDto(membershipTier = tier))
    }

    suspend fun getVideoAnalysisById(id: Long): VideoAnalysisResponseDto {
        return api.getVideoAnalysisById(id)
    }

    suspend fun getVideoAnalysisKeypoints(id: Long): VideoAnalysisKeypointsDto {
        return api.getVideoAnalysisKeypoints(id)
    }

    suspend fun getVideoAnalysisHistory(): List<VideoAnalysisResponseDto> {
        return api.getVideoAnalysisHistory()
    }

    suspend fun analyzeVideo(videoFile: File, exerciseId: Long?): AnalysisResponseDto {
        val requestFile = videoFile.asRequestBody("video/*".toMediaTypeOrNull())
        val multipartPart = MultipartBody.Part.createFormData("file", videoFile.name, requestFile)

        return api.analyzeVideo(
            exerciseId = exerciseId,
            file = multipartPart
        )
    }

    suspend fun analyzeImage(imageFile: File, exerciseId: Long?): AnalysisResponseDto {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val multipartPart = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
        return api.analyzeImage(exerciseId = exerciseId, file = multipartPart)
    }

    suspend fun getAnalysisHistory(): List<AnalysisResponseDto> {
        return api.getAnalysisHistory()
    }

    suspend fun getAnalysisById(id: Long): AnalysisResponseDto {
        return api.getAnalysisById(id)
    }
}
