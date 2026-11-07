package com.calisthenics.ai.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutScheduleDao {

    @Query("SELECT * FROM cached_workout_plans ORDER BY isCoachAssigned DESC, isActive DESC, id ASC")
    fun getAllPlansFlow(): Flow<List<CachedWorkoutPlanEntity>>

    @Query("SELECT * FROM cached_workout_plans ORDER BY isCoachAssigned DESC, isActive DESC, id ASC")
    suspend fun getAllPlans(): List<CachedWorkoutPlanEntity>

    @Query("SELECT * FROM cached_workout_plans WHERE id = :planId LIMIT 1")
    fun getPlanByIdFlow(planId: Long): Flow<CachedWorkoutPlanEntity?>

    @Query("SELECT * FROM cached_workout_plans WHERE id = :planId LIMIT 1")
    suspend fun getPlanById(planId: Long): CachedWorkoutPlanEntity?

    @Query("SELECT * FROM cached_workout_plans WHERE isCoachAssigned = 1 LIMIT 1")
    suspend fun getCoachAssignedPlan(): CachedWorkoutPlanEntity?

    @Query("SELECT * FROM cached_workout_plans WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlan(): CachedWorkoutPlanEntity?

    @Query("SELECT * FROM cached_workout_plan_days WHERE planId = :planId ORDER BY CASE dayOfWeek WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 WHEN 'SUNDAY' THEN 7 ELSE 8 END")
    fun getDaysForPlanFlow(planId: Long): Flow<List<CachedWorkoutPlanDayEntity>>

    @Query("SELECT * FROM cached_workout_plan_days WHERE planId = :planId ORDER BY CASE dayOfWeek WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 WHEN 'SUNDAY' THEN 7 ELSE 8 END")
    suspend fun getDaysForPlan(planId: Long): List<CachedWorkoutPlanDayEntity>

    @Query("SELECT * FROM cached_workout_plan_exercises WHERE dayId = :dayId ORDER BY displayOrder ASC")
    fun getExercisesForDayFlow(dayId: Long): Flow<List<CachedWorkoutPlanExerciseEntity>>

    @Query("SELECT * FROM cached_workout_plan_exercises WHERE dayId = :dayId ORDER BY displayOrder ASC")
    suspend fun getExercisesForDay(dayId: Long): List<CachedWorkoutPlanExerciseEntity>

    @Query("SELECT * FROM cached_workout_plan_exercises WHERE planId = :planId ORDER BY displayOrder ASC")
    fun getExercisesForPlanFlow(planId: Long): Flow<List<CachedWorkoutPlanExerciseEntity>>

    @Query("SELECT * FROM cached_workout_plan_exercises WHERE planId = :planId ORDER BY displayOrder ASC")
    suspend fun getExercisesForPlan(planId: Long): List<CachedWorkoutPlanExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<CachedWorkoutPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: CachedWorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<CachedWorkoutPlanDayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<CachedWorkoutPlanExerciseEntity>)

    @Query("DELETE FROM cached_workout_plan_days WHERE planId = :planId")
    suspend fun deleteDaysForPlan(planId: Long)

    @Query("DELETE FROM cached_workout_plan_exercises WHERE planId = :planId")
    suspend fun deleteExercisesForPlan(planId: Long)

    @Query("DELETE FROM cached_workout_plans WHERE id = :planId")
    suspend fun deletePlan(planId: Long)

    @Query("UPDATE cached_workout_plans SET isActive = (id = :activePlanId)")
    suspend fun setActivePlanId(activePlanId: Long)

    @Transaction
    suspend fun saveFullPlan(
        plan: CachedWorkoutPlanEntity,
        days: List<CachedWorkoutPlanDayEntity>,
        exercises: List<CachedWorkoutPlanExerciseEntity>
    ) {
        insertPlan(plan)
        deleteDaysForPlan(plan.id)
        if (days.isNotEmpty()) {
            insertDays(days)
        }
        deleteExercisesForPlan(plan.id)
        if (exercises.isNotEmpty()) {
            insertExercises(exercises)
        }
    }

    @Transaction
    suspend fun syncAllPlans(
        plans: List<CachedWorkoutPlanEntity>,
        days: List<CachedWorkoutPlanDayEntity>,
        exercises: List<CachedWorkoutPlanExerciseEntity>
    ) {
        if (plans.isNotEmpty()) {
            insertPlans(plans)
        }
        if (days.isNotEmpty()) {
            insertDays(days)
        }
        if (exercises.isNotEmpty()) {
            insertExercises(exercises)
        }
    }

    @Query("DELETE FROM cached_workout_plan_exercises")
    suspend fun clearExercises()

    @Query("DELETE FROM cached_workout_plan_days")
    suspend fun clearDays()

    @Query("DELETE FROM cached_workout_plans")
    suspend fun clearPlans()

    @Transaction
    suspend fun clearAll() {
        clearExercises()
        clearDays()
        clearPlans()
    }
}
