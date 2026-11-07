package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AnalysisResult
import java.io.File

interface AnalysisRepository {
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
