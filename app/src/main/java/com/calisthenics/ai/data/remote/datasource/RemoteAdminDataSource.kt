package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.AdminDashboardDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDeleteResponseDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDto
import com.calisthenics.ai.data.remote.dto.AdminPageDto
import com.calisthenics.ai.data.remote.dto.AdminUserDto
import com.calisthenics.ai.data.remote.dto.AdminUserRoleDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDto
import com.calisthenics.ai.data.remote.dto.CreateAdminExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateAdminExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateAdminUserRoleRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateExerciseStatusRequestDto
import javax.inject.Inject

class RemoteAdminDataSource @Inject constructor(
    private val api: CalisthenicsApi
) {
    suspend fun getDashboard(): AdminDashboardDto = api.getAdminDashboard()

    suspend fun getUsers(
        page: Int,
        size: Int,
        search: String?,
        role: String?
    ): AdminPageDto<AdminUserDto> = api.getAdminUsers(
        page = page,
        size = size,
        search = search,
        role = role,
        sortBy = "createdAt",
        sortDirection = "DESC"
    )

    suspend fun getUser(userId: Long): AdminUserDto = api.getAdminUser(userId)

    suspend fun getUserRole(userId: Long): AdminUserRoleDto = api.getAdminUserRole(userId)

    suspend fun updateUserRole(userId: Long, role: String): AdminUserDto =
        api.updateAdminUserRole(userId, UpdateAdminUserRoleRequestDto(role))

    suspend fun getWorkoutPlans(page: Int, size: Int): AdminPageDto<AdminWorkoutPlanDto> =
        api.getAdminWorkoutPlans(page, size)

    suspend fun getWorkoutPlan(planId: Long): AdminWorkoutPlanDetailDto =
        api.getAdminWorkoutPlan(planId)

    suspend fun getWorkoutSessions(page: Int, size: Int): AdminPageDto<AdminWorkoutSessionDto> =
        api.getAdminWorkoutSessions(page, size)

    suspend fun getWorkoutSession(sessionId: Long): AdminWorkoutSessionDetailDto =
        api.getAdminWorkoutSession(sessionId)

    suspend fun getExercises(
        page: Int,
        size: Int,
        search: String?,
        category: String?,
        difficulty: String?,
        active: Boolean?,
        sortBy: String = "name",
        sortDirection: String = "ASC"
    ): AdminPageDto<AdminExerciseDto> = api.getAdminExercises(
        page = page,
        size = size,
        search = search,
        category = category,
        difficulty = difficulty,
        active = active,
        sortBy = sortBy,
        sortDirection = sortDirection
    )

    suspend fun getExercise(exerciseId: Long): AdminExerciseDto =
        api.getAdminExercise(exerciseId)

    suspend fun createExercise(request: CreateAdminExerciseRequestDto): AdminExerciseDto =
        api.createAdminExercise(request)

    suspend fun updateExercise(exerciseId: Long, request: UpdateAdminExerciseRequestDto): AdminExerciseDto =
        api.updateAdminExercise(exerciseId, request)

    suspend fun updateExerciseStatus(exerciseId: Long, active: Boolean): AdminExerciseDto =
        api.updateAdminExerciseStatus(exerciseId, UpdateExerciseStatusRequestDto(active))

    suspend fun deleteExercise(exerciseId: Long, softDelete: Boolean): AdminExerciseDeleteResponseDto =
        api.deleteAdminExercise(exerciseId, softDelete)
}
