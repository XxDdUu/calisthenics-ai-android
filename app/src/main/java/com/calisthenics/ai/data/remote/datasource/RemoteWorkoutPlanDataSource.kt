package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.CreateWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutDayExercisesBatchRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanExerciseResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanTemplateResponseDto
import com.calisthenics.ai.data.remote.dto.TodayWorkoutResolutionResponseDto
import javax.inject.Inject

class RemoteWorkoutPlanDataSource @Inject constructor(
    private val api: CalisthenicsApi
) {
    suspend fun getWorkoutPlans(): List<WorkoutPlanResponseDto> {
        return api.getWorkoutPlans()
    }

    suspend fun resolveTodayWorkout(date: String, activePlanId: Long?): TodayWorkoutResolutionResponseDto {
        return api.resolveTodayWorkout(date, activePlanId)
    }

    suspend fun getWorkoutPlanTemplates(): List<WorkoutPlanTemplateResponseDto> {
        return api.getWorkoutPlanTemplates()
    }

    suspend fun createWorkoutPlan(request: CreateWorkoutPlanRequestDto): WorkoutPlanResponseDto {
        return api.createWorkoutPlan(request)
    }

    suspend fun getWorkoutPlanById(id: Long): WorkoutPlanResponseDto {
        return api.getWorkoutPlanById(id)
    }

    suspend fun updateWorkoutPlan(id: Long, request: UpdateWorkoutPlanRequestDto): WorkoutPlanResponseDto {
        return api.updateWorkoutPlan(id, request)
    }

    suspend fun deleteWorkoutPlan(id: Long) {
        val response = api.deleteWorkoutPlan(id)
        if (!response.isSuccessful && response.code() != 204) {
            throw RuntimeException("Failed to delete workout plan: HTTP ${response.code()}")
        }
    }

    suspend fun getWorkoutPlanDayExercises(planId: Long, dayId: Long): List<WorkoutPlanExerciseResponseDto> {
        return api.getWorkoutPlanDayExercises(planId, dayId)
    }

    suspend fun updateWorkoutPlanDayExercises(
        planId: Long,
        dayId: Long,
        request: WorkoutDayExercisesBatchRequestDto
    ): List<WorkoutPlanExerciseResponseDto> {
        return api.updateWorkoutPlanDayExercises(planId, dayId, request)
    }

    suspend fun addExerciseToWorkoutPlanDay(
        planId: Long,
        dayId: Long,
        request: WorkoutPlanExerciseRequestDto
    ): WorkoutPlanExerciseResponseDto {
        return api.addExerciseToWorkoutPlanDay(planId, dayId, request)
    }

    suspend fun updateWorkoutPlanExercise(
        planId: Long,
        dayId: Long,
        assignmentId: Long,
        request: WorkoutPlanExerciseRequestDto
    ): WorkoutPlanExerciseResponseDto {
        return api.updateWorkoutPlanExercise(planId, dayId, assignmentId, request)
    }

    suspend fun removeExerciseFromWorkoutPlanDay(
        planId: Long,
        dayId: Long,
        assignmentId: Long
    ) {
        val response = api.removeExerciseFromWorkoutPlanDay(planId, dayId, assignmentId)
        if (!response.isSuccessful && response.code() != 204) {
            throw RuntimeException("Failed to remove exercise from workout plan day: HTTP ${response.code()}")
        }
    }
}
