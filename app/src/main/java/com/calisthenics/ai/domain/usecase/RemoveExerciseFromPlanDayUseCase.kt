package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class RemoveExerciseFromPlanDayUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(planId: Long, dayId: Long, assignmentId: Long): Result<Unit> {
        return repository.removeExercise(planId, dayId, assignmentId)
    }
}
