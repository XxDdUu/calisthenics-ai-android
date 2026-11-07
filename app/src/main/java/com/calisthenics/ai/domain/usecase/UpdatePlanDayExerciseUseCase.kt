package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class UpdatePlanDayExerciseUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(
        planId: Long,
        dayId: Long,
        assignmentId: Long,
        exerciseId: Long,
        sets: Int,
        reps: Int?,
        durationSeconds: Int?,
        restSeconds: Int,
        displayOrder: Int,
        notes: String?
    ): Result<UserWorkoutPlanExercise> {
        return repository.updateExercise(
            planId = planId,
            dayId = dayId,
            assignmentId = assignmentId,
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
