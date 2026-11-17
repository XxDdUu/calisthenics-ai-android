package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.AiWorkoutPlanPreview
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class ModifyAiWorkoutPlanUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(
        prompt: String,
        currentPlan: AiWorkoutPlanPreview,
        clientId: Long? = null
    ): Result<AiWorkoutPlanPreview> {
        return repository.modifyAiWorkoutPlan(
            prompt = prompt,
            currentPlan = currentPlan,
            clientId = clientId
        )
    }
}
