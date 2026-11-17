package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.AiWorkoutPlanPreview
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class GenerateAiWorkoutPlanUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(
        prompt: String,
        clientId: Long? = null,
        fitnessLevel: String? = null,
        preferredSplit: String? = null,
        startDate: String? = null,
        durationWeeks: Int? = null
    ): Result<AiWorkoutPlanPreview> {
        return repository.generateAiWorkoutPlan(
            prompt = prompt,
            clientId = clientId,
            fitnessLevel = fitnessLevel,
            preferredSplit = preferredSplit,
            startDate = startDate,
            durationWeeks = durationWeeks
        )
    }
}
