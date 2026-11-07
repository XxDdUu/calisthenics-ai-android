package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AdminDashboardDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDeleteResponseDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDto
import com.calisthenics.ai.data.remote.dto.AdminPageDto
import com.calisthenics.ai.data.remote.dto.AdminUserDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDto
import com.calisthenics.ai.domain.model.AdminDashboard
import com.calisthenics.ai.domain.model.AdminExercise
import com.calisthenics.ai.domain.model.AdminExerciseDeleteResult
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminUser
import com.calisthenics.ai.domain.model.AdminWorkoutPlan
import com.calisthenics.ai.domain.model.AdminWorkoutPlanDetail
import com.calisthenics.ai.domain.model.AdminWorkoutSession
import com.calisthenics.ai.domain.model.AdminWorkoutSessionDetail

fun AdminDashboardDto.toDomain() = AdminDashboard(
    totalUsers,
    totalClients,
    totalCoaches,
    totalAdmins,
    totalWorkoutSessions,
    totalWorkoutPlans,
    totalExercises
)

fun AdminUserDto.toDomain() = AdminUser(
    id,
    email,
    fullName,
    role,
    hasCoachProfile,
    hasClientProfile,
    createdAt
)

fun <T, R> AdminPageDto<T>.toDomain(map: (T) -> R) = AdminPage(
    content = content.map(map),
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    hasNext = hasNext
)

fun AdminWorkoutPlanDto.toDomain() = AdminWorkoutPlan(
    id,
    userId,
    userEmail,
    userFullName,
    name,
    splitType,
    createdAt,
    updatedAt
)

fun AdminWorkoutPlanDetailDto.toDomain() = AdminWorkoutPlanDetail(
    userId,
    userEmail,
    userFullName,
    workoutPlan.toDomain()
)

fun AdminWorkoutSessionDto.toDomain() = AdminWorkoutSession(
    id,
    userId,
    userEmail,
    userFullName,
    title,
    workoutDate,
    status,
    workoutPlanId,
    workoutPlanName,
    createdAt,
    startedAt,
    completedAt
)

fun AdminWorkoutSessionDetailDto.toDomain() = AdminWorkoutSessionDetail(
    userId,
    userEmail,
    userFullName,
    workoutSession.toDomain()
)

fun AdminExerciseDto.toDomain() = AdminExercise(
    id = id,
    name = name,
    code = code,
    category = category,
    difficulty = difficulty,
    description = description,
    targetMuscles = targetMuscles,
    instructions = instructions,
    imageUrl = imageUrl,
    videoUrl = videoUrl,
    equipment = equipment,
    active = active,
    createdAt = createdAt,
    planReferencesCount = planReferencesCount,
    sessionReferencesCount = sessionReferencesCount,
    canDelete = canDelete,
    mediaItems = mediaItems.mapIndexedNotNull { index, item -> item.toDomainOrNull(index) }
)

fun AdminExerciseDeleteResponseDto.toDomain() = AdminExerciseDeleteResult(
    success = success,
    softDeleted = softDeleted,
    message = message
)
