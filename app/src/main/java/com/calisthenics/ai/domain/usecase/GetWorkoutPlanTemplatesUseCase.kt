package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlanTemplate
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class GetWorkoutPlanTemplatesUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(): Result<List<UserWorkoutPlanTemplate>> {
        return repository.getPlanTemplates()
    }
}
