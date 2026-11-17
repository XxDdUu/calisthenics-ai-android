package com.calisthenics.ai.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutScheduleDao {

    @Query("SELECT * FROM cached_workout_plans WHERE ownerUserId = :ownerUserId ORDER BY isCoachAssigned DESC, isActive DESC, id ASC")
    fun getAllPlansFlow(ownerUserId: Long): Flow<List<CachedWorkoutPlanEntity>>

    @Query("SELECT * FROM cached_workout_plans WHERE ownerUserId = :ownerUserId ORDER BY isCoachAssigned DESC, isActive DESC, id ASC")
    suspend fun getAllPlans(ownerUserId: Long): List<CachedWorkoutPlanEntity>

    @Query("SELECT * FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND id = :planId LIMIT 1")
    fun getPlanByIdFlow(ownerUserId: Long, planId: Long): Flow<CachedWorkoutPlanEntity?>

    @Query("SELECT * FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND id = :planId LIMIT 1")
    suspend fun getPlanById(ownerUserId: Long, planId: Long): CachedWorkoutPlanEntity?

    @Query("SELECT * FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND isCoachAssigned = 1 LIMIT 1")
    suspend fun getCoachAssignedPlan(ownerUserId: Long): CachedWorkoutPlanEntity?

    @Query("SELECT * FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND isActive = 1 LIMIT 1")
    suspend fun getActivePlan(ownerUserId: Long): CachedWorkoutPlanEntity?

    @Query("SELECT * FROM cached_workout_plan_days WHERE ownerUserId = :ownerUserId AND planId = :planId ORDER BY CASE dayOfWeek WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 WHEN 'SUNDAY' THEN 7 ELSE 8 END")
    fun getDaysForPlanFlow(ownerUserId: Long, planId: Long): Flow<List<CachedWorkoutPlanDayEntity>>

    @Query("SELECT * FROM cached_workout_plan_days WHERE ownerUserId = :ownerUserId AND planId = :planId ORDER BY CASE dayOfWeek WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 WHEN 'SUNDAY' THEN 7 ELSE 8 END")
    suspend fun getDaysForPlan(ownerUserId: Long, planId: Long): List<CachedWorkoutPlanDayEntity>

    @Query("SELECT * FROM cached_workout_plan_exercises WHERE ownerUserId = :ownerUserId AND dayId = :dayId ORDER BY displayOrder ASC")
    fun getExercisesForDayFlow(ownerUserId: Long, dayId: Long): Flow<List<CachedWorkoutPlanExerciseEntity>>

    @Query("SELECT * FROM cached_workout_plan_exercises WHERE ownerUserId = :ownerUserId AND dayId = :dayId ORDER BY displayOrder ASC")
    suspend fun getExercisesForDay(ownerUserId: Long, dayId: Long): List<CachedWorkoutPlanExerciseEntity>

    @Query("SELECT * FROM cached_workout_plan_exercises WHERE ownerUserId = :ownerUserId AND planId = :planId ORDER BY displayOrder ASC")
    fun getExercisesForPlanFlow(ownerUserId: Long, planId: Long): Flow<List<CachedWorkoutPlanExerciseEntity>>

    @Query("SELECT * FROM cached_workout_plan_exercises WHERE ownerUserId = :ownerUserId AND planId = :planId ORDER BY displayOrder ASC")
    suspend fun getExercisesForPlan(ownerUserId: Long, planId: Long): List<CachedWorkoutPlanExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<CachedWorkoutPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: CachedWorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<CachedWorkoutPlanDayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<CachedWorkoutPlanExerciseEntity>)

    @Query("DELETE FROM cached_workout_plan_days WHERE ownerUserId = :ownerUserId AND planId = :planId")
    suspend fun deleteDaysForPlan(ownerUserId: Long, planId: Long)

    @Query("DELETE FROM cached_workout_plan_exercises WHERE ownerUserId = :ownerUserId AND planId = :planId")
    suspend fun deleteExercisesForPlan(ownerUserId: Long, planId: Long)

    @Query("DELETE FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND id = :planId")
    suspend fun deletePlan(ownerUserId: Long, planId: Long)

    @Query("DELETE FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND isCoachAssigned = 0")
    suspend fun deletePersonalPlansForUser(ownerUserId: Long)

    @Query("DELETE FROM cached_workout_plan_exercises WHERE ownerUserId = :ownerUserId AND planId IN (SELECT id FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND isCoachAssigned = 1)")
    suspend fun deleteCoachAssignedExercisesForUser(ownerUserId: Long)

    @Query("DELETE FROM cached_workout_plan_days WHERE ownerUserId = :ownerUserId AND planId IN (SELECT id FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND isCoachAssigned = 1)")
    suspend fun deleteCoachAssignedDaysForUser(ownerUserId: Long)

    @Query("DELETE FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND isCoachAssigned = 1")
    suspend fun deleteCoachAssignedPlansForUser(ownerUserId: Long)

    @Transaction
    suspend fun clearCoachAssignedPlansForUser(ownerUserId: Long) {
        deleteCoachAssignedExercisesForUser(ownerUserId)
        deleteCoachAssignedDaysForUser(ownerUserId)
        deleteCoachAssignedPlansForUser(ownerUserId)
    }

    @Query("DELETE FROM cached_workout_plans WHERE ownerUserId = :ownerUserId AND isCoachAssigned = 0 AND id NOT IN (:retainedPlanIds)")
    suspend fun deletePersonalPlansNotIn(ownerUserId: Long, retainedPlanIds: List<Long>)

    @Query("DELETE FROM cached_workout_plan_days WHERE ownerUserId = :ownerUserId AND planId NOT IN (SELECT id FROM cached_workout_plans WHERE ownerUserId = :ownerUserId)")
    suspend fun deleteOrphanedDaysForUser(ownerUserId: Long)

    @Query("DELETE FROM cached_workout_plan_exercises WHERE ownerUserId = :ownerUserId AND planId NOT IN (SELECT id FROM cached_workout_plans WHERE ownerUserId = :ownerUserId)")
    suspend fun deleteOrphanedExercisesForUser(ownerUserId: Long)

    @Query("UPDATE cached_workout_plans SET isActive = (id = :activePlanId) WHERE ownerUserId = :ownerUserId")
    suspend fun setActivePlanId(ownerUserId: Long, activePlanId: Long)

    @Transaction
    suspend fun saveFullPlan(
        plan: CachedWorkoutPlanEntity,
        days: List<CachedWorkoutPlanDayEntity>,
        exercises: List<CachedWorkoutPlanExerciseEntity>
    ) {
        insertPlan(plan)
        deleteDaysForPlan(plan.ownerUserId, plan.id)
        if (days.isNotEmpty()) {
            insertDays(days)
        }
        deleteExercisesForPlan(plan.ownerUserId, plan.id)
        if (exercises.isNotEmpty()) {
            insertExercises(exercises)
        }
    }

    @Transaction
    suspend fun syncPersonalPlans(
        ownerUserId: Long,
        plans: List<CachedWorkoutPlanEntity>
    ) {
        if (plans.isEmpty()) {
            deletePersonalPlansForUser(ownerUserId)
        } else {
            deletePersonalPlansNotIn(ownerUserId, plans.map { it.id })
            insertPlans(plans)
        }
        deleteOrphanedDaysForUser(ownerUserId)
        deleteOrphanedExercisesForUser(ownerUserId)
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

    @Transaction
    suspend fun deletePlanAndChildren(ownerUserId: Long, planId: Long) {
        deleteExercisesForPlan(ownerUserId, planId)
        deleteDaysForPlan(ownerUserId, planId)
        deletePlan(ownerUserId, planId)
    }

    @Query("DELETE FROM cached_workout_plan_exercises WHERE ownerUserId = :ownerUserId")
    suspend fun clearExercisesForUser(ownerUserId: Long)

    @Query("DELETE FROM cached_workout_plan_days WHERE ownerUserId = :ownerUserId")
    suspend fun clearDaysForUser(ownerUserId: Long)

    @Query("DELETE FROM cached_workout_plans WHERE ownerUserId = :ownerUserId")
    suspend fun clearPlansForUser(ownerUserId: Long)

    @Transaction
    suspend fun clearForUser(ownerUserId: Long) {
        clearExercisesForUser(ownerUserId)
        clearDaysForUser(ownerUserId)
        clearPlansForUser(ownerUserId)
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
