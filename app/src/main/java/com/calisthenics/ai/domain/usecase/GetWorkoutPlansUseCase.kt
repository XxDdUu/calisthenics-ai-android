package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWorkoutPlansUseCase @Inject constructor(
    private val repository: WorkoutPlanRepository
) {
    val planCacheInvalidations: Flow<Unit> get() = repository.planCacheInvalidations
    suspend operator fun invoke(): Result<List<UserWorkoutPlan>> {
        return repository.getPlans()
    }

    suspend fun getClientPersonalPlans(clientId: Long): Result<List<UserWorkoutPlan>> =
        repository.getClientPersonalPlans(clientId)

    suspend fun assignPlanToClient(relationshipId: Long, planId: Long): Result<Unit> =
        repository.assignPlanToClient(relationshipId, planId)

    suspend fun assignPlanToClient(
        relationshipId: Long,
        planId: Long,
        startDate: String?,
        endDate: String?,
        notes: String?
    ): Result<Unit> = repository.assignPlanToClient(relationshipId, planId, startDate, endDate, notes)

    suspend fun updatePlanDays(
        id: Long,
        days: List<com.calisthenics.ai.domain.model.WorkoutPlanDayConfiguration>
    ): Result<com.calisthenics.ai.domain.model.UserWorkoutPlan> = repository.updatePlanDays(id, days)

    suspend fun addExercise(
        planId: Long,
        dayId: Long,
        exerciseId: Long,
        sets: Int,
        reps: Int?,
        durationSeconds: Int?,
        restSeconds: Int,
        displayOrder: Int,
        notes: String?
    ): Result<com.calisthenics.ai.domain.model.UserWorkoutPlanExercise> = repository.addExercise(
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

    suspend fun getMyAssignments() = repository.getMyAssignments()

    suspend fun getMyCoachAssignments() = repository.getMyCoachAssignments()

    suspend fun updateAssignmentStatus(
        assignmentId: Long,
        status: com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus
    ) = repository.updateAssignmentStatus(assignmentId, status)

    suspend fun getLatestActivePlan(clientId: Long? = null): Result<UserWorkoutPlan?> =
        repository.getLatestActivePlan(clientId)
}
