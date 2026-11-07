package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class AddExerciseToPlanDayUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(
        planId: Long,
        dayId: Long,
        exerciseId: Long,
        sets: Int,
        reps: Int?,
        durationSeconds: Int?,
        restSeconds: Int,
        displayOrder: Int,
        notes: String?
    ): Result<UserWorkoutPlanExercise> {
        return repository.addExercise(
            planId = planId,
            dayId = dayId,
            exerciseId = exerciseId,
            sets = sets,
            reps = reps,
            durationSeconds = durationSeconds,
            restSeconds = restSeconds,
            displayOrder = displayOrder,
            notes = notes
        )
    }
}
