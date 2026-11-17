package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.repository.AnalysisRepository
import java.io.File
import javax.inject.Inject

class AnalyzeVideoUseCase @Inject constructor(
    private val repository: AnalysisRepository
) {
    suspend operator fun invoke(
        videoFile: File,
        exerciseId: Long?,
        exerciseType: String? = null,
        idempotencyKey: String? = null,
        sideView: Boolean = false
    ): Result<AnalysisResult> {
        return repository.uploadVideoForAnalysis(
            videoFile = videoFile,
            exerciseId = exerciseId,
            exerciseType = exerciseType,
            idempotencyKey = idempotencyKey,
            sideView = sideView
        )
    }
}
