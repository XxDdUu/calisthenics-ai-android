package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.repository.AnalysisRepository
import java.io.File
import javax.inject.Inject

class AnalyzeImageUseCase @Inject constructor(private val repository: AnalysisRepository) {
    suspend operator fun invoke(imageFile: File, exerciseId: Long?): Result<AnalysisResult> =
        repository.analyzeImage(imageFile, exerciseId)
}
