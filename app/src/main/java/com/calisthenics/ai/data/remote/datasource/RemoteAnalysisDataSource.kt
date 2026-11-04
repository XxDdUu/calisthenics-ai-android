package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.AnalysisResponseDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class RemoteAnalysisDataSource @Inject constructor(
    private val api: CalisthenicsApi
) {
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
