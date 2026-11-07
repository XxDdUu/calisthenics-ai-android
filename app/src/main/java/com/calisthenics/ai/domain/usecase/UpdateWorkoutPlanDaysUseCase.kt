package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutPlanDayConfiguration
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class UpdateWorkoutPlanDaysUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(
        id: Long,
        days: List<WorkoutPlanDayConfiguration>
    ): Result<UserWorkoutPlan> = repository.updatePlanDays(id, days)
}
