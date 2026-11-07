package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.repository.AnalysisRepository
import javax.inject.Inject

class GetAnalysisByIdUseCase @Inject constructor(
    private val repository: AnalysisRepository
) {
    suspend operator fun invoke(id: Long): Result<AnalysisResult> {
        return repository.getAnalysisById(id)
    }
}
