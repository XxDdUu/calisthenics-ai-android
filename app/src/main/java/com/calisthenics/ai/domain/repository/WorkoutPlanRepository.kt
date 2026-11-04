package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.UserWorkoutPlanTemplate
import com.calisthenics.ai.domain.model.TodayWorkoutResolution
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutPlanSnapshot
import com.calisthenics.ai.domain.model.WorkoutSessionProgress
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType

interface WorkoutPlanRepository {
    // Local / Session backward-compatibility methods
    suspend fun getSnapshot(): Result<WorkoutPlanSnapshot>
    suspend fun savePlan(plan: WorkoutPlan): Result<Unit>
    suspend fun clearPlan(): Result<Unit>
    suspend fun saveSessionProgress(progress: WorkoutSessionProgress): Result<Unit>
    suspend fun getActivePlanId(): Result<Long?>
    suspend fun setActivePlanId(planId: Long?): Result<Unit>

    // Server-backed REST API methods
    suspend fun getPlans(): Result<List<UserWorkoutPlan>>
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
}
