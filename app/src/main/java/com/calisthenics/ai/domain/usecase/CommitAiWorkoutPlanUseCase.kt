package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.AiWorkoutPlanPreview
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class CommitAiWorkoutPlanUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    suspend operator fun invoke(
        plan: AiWorkoutPlanPreview,
        idempotencyKey: String? = null,
        startDate: String? = null,
        replaceExisting: Boolean = true,
        clientId: Long? = null,
        expectedActivePlanId: Long? = null
    ): Result<UserWorkoutPlan> {
        return repository.commitAiWorkoutPlan(
            plan = plan,
            idempotencyKey = idempotencyKey,
            startDate = startDate,
            replaceExisting = replaceExisting,
            clientId = clientId,
            expectedActivePlanId = expectedActivePlanId
        )
    }
}
