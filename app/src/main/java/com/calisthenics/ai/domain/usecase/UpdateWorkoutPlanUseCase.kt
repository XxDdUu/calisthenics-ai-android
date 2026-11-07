package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class UpdateWorkoutPlanUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(
        id: Long,
        name: String,
        splitType: WorkoutSplitType,
        days: List<Pair<WorkoutDayOfWeek, WorkoutSessionType>>
    ): Result<UserWorkoutPlan> {
        return repository.updatePlan(id, name, splitType, days)
    }
}
