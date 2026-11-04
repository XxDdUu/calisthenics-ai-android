package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class GetWorkoutPlanDayExercisesUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(planId: Long, dayId: Long): Result<List<UserWorkoutPlanExercise>> {
        return repository.getDayExercises(planId, dayId)
    }
}
