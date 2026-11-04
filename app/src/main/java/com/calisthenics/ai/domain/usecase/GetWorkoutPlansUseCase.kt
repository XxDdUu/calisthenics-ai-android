package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class GetWorkoutPlansUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(): Result<List<UserWorkoutPlan>> {
        return repository.getPlans()
    }
}
