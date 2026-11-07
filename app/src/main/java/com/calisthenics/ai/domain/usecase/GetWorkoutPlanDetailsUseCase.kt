package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class GetWorkoutPlanDetailsUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(id: Long): Result<UserWorkoutPlan> {
        return repository.getPlanById(id)
    }
}
