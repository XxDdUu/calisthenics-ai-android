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
        exerciseId: Long?
    ): Result<AnalysisResult> {
        return repository.analyzeVideo(videoFile, exerciseId)
    }
}
