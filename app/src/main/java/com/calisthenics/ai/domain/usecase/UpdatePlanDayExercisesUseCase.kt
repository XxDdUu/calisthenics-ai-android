package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class UpdatePlanDayExercisesUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(
        planId: Long,
        dayId: Long,
        exercises: List<UserWorkoutPlanExercise>
    ): Result<List<UserWorkoutPlanExercise>> {
        return repository.updateDayExercises(planId, dayId, exercises)
    }
}

