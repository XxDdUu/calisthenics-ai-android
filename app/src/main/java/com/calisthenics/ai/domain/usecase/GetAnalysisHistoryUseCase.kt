package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.repository.AnalysisRepository
import javax.inject.Inject

class GetAnalysisHistoryUseCase @Inject constructor(
    private val repository: AnalysisRepository
) {
    suspend operator fun invoke(): Result<List<AnalysisResult>> {
        return repository.getAnalysisHistory()
    }
}
