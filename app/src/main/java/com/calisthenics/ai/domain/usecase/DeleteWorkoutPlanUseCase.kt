package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class DeleteWorkoutPlanUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return repository.deletePlan(id)
    }
}
