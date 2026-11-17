package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.UserWorkoutPlanTemplate
import com.calisthenics.ai.domain.model.TodayWorkoutResolution
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutPlanDayConfiguration
import com.calisthenics.ai.domain.model.WorkoutPlanSnapshot
import com.calisthenics.ai.domain.model.WorkoutSessionProgress
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.model.AiWorkoutPlanPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface WorkoutPlanRepository {
    // Emits when a plan is deleted or a coach relationship ends so active UI screens refresh Room/session caches immediately.
    val planCacheInvalidations: Flow<Unit> get() = emptyFlow()
    suspend fun invalidateCoachAssignedPlans() {}
    // Local Room Database / Offline observation methods
    fun observeCachedPlans(): Flow<List<UserWorkoutPlan>>
    fun observeCachedPlan(planId: Long): Flow<UserWorkoutPlan?>
    // Local / Session backward-compatibility methods
    suspend fun getSnapshot(): Result<WorkoutPlanSnapshot>
    suspend fun savePlan(plan: WorkoutPlan): Result<Unit>
    suspend fun clearPlan(): Result<Unit>
    suspend fun saveSessionProgress(progress: WorkoutSessionProgress): Result<Unit>
    suspend fun getActivePlanId(): Result<Long?>
    suspend fun setActivePlanId(planId: Long?): Result<Unit>

    // Server-backed REST API methods
    suspend fun getPlans(): Result<List<UserWorkoutPlan>>
    suspend fun getClientPersonalPlans(clientId: Long): Result<List<UserWorkoutPlan>> =
        Result.success(emptyList())
    suspend fun assignPlanToClient(relationshipId: Long, planId: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException("Plan assignment is unavailable."))

    suspend fun assignPlanToClient(
        relationshipId: Long,
        planId: Long,
        startDate: String?,
        endDate: String?,
        notes: String?
    ): Result<Unit> = assignPlanToClient(relationshipId, planId)
    suspend fun getMyAssignments(): Result<List<com.calisthenics.ai.domain.model.WorkoutPlanAssignment>> =
        Result.success(emptyList())
    suspend fun getMyCoachAssignments(): Result<List<com.calisthenics.ai.domain.model.WorkoutPlanAssignment>> =
        Result.success(emptyList())
    suspend fun updateAssignmentStatus(
        assignmentId: Long,
        status: com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus
    ): Result<com.calisthenics.ai.domain.model.WorkoutPlanAssignment> =
        Result.failure(UnsupportedOperationException("Assignment status updates are unavailable."))
    suspend fun getLatestActivePlan(clientId: Long? = null): Result<UserWorkoutPlan?> =
        getPlans().map { plans -> plans.firstOrNull { it.isActive } }
    suspend fun resolveTodayWorkout(date: String, activePlanId: Long?): Result<TodayWorkoutResolution>
    suspend fun getPlanById(id: Long): Result<UserWorkoutPlan>
    suspend fun getPlanTemplates(): Result<List<UserWorkoutPlanTemplate>>
    suspend fun createPlan(
        name: String,
        splitType: WorkoutSplitType,
        days: List<Pair<WorkoutDayOfWeek, WorkoutSessionType>>
    ): Result<UserWorkoutPlan>
    suspend fun updatePlan(
        id: Long,
        name: String,
        splitType: WorkoutSplitType,
        days: List<Pair<WorkoutDayOfWeek, WorkoutSessionType>>
    ): Result<UserWorkoutPlan>
    suspend fun updatePlanDays(
        id: Long,
        days: List<WorkoutPlanDayConfiguration>
    ): Result<UserWorkoutPlan>
    suspend fun deletePlan(id: Long): Result<Unit>
    suspend fun getDayExercises(planId: Long, dayId: Long): Result<List<UserWorkoutPlanExercise>>
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
    ): Result<UserWorkoutPlanExercise>
    suspend fun updateExercise(
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
    ): Result<UserWorkoutPlanExercise>
    suspend fun updateDayExercises(
        planId: Long,
        dayId: Long,
        exercises: List<UserWorkoutPlanExercise>
    ): Result<List<UserWorkoutPlanExercise>>
    suspend fun removeExercise(planId: Long, dayId: Long, assignmentId: Long): Result<Unit>

    // AI Workout Planner methods
    suspend fun generateAiWorkoutPlan(
        prompt: String,
        clientId: Long? = null,
        fitnessLevel: String? = null,
        preferredSplit: String? = null,
        startDate: String? = null,
        durationWeeks: Int? = null
    ): Result<AiWorkoutPlanPreview> = Result.failure(UnsupportedOperationException("AI workout planner is unavailable."))

    suspend fun modifyAiWorkoutPlan(
        prompt: String,
        currentPlan: AiWorkoutPlanPreview,
        clientId: Long? = null
    ): Result<AiWorkoutPlanPreview> = Result.failure(UnsupportedOperationException("AI workout planner is unavailable."))

    suspend fun commitAiWorkoutPlan(
        plan: AiWorkoutPlanPreview,
        idempotencyKey: String? = null,
        startDate: String? = null,
        replaceExisting: Boolean = true,
        clientId: Long? = null,
        expectedActivePlanId: Long? = null
    ): Result<UserWorkoutPlan> = Result.failure(UnsupportedOperationException("AI workout planner is unavailable."))
}
