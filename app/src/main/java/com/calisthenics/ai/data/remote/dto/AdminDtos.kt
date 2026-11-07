package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AdminDashboardDto(
    @SerializedName("totalUsers") val totalUsers: Long,
    @SerializedName("totalClients") val totalClients: Long,
    @SerializedName("totalCoaches") val totalCoaches: Long,
    @SerializedName("totalAdmins") val totalAdmins: Long,
    @SerializedName("totalWorkoutSessions") val totalWorkoutSessions: Long,
    @SerializedName("totalWorkoutPlans") val totalWorkoutPlans: Long,
    @SerializedName("totalExercises") val totalExercises: Long = 0L
)

data class AdminUserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("role") val role: String,
    @SerializedName("hasCoachProfile") val hasCoachProfile: Boolean,
    @SerializedName("hasClientProfile") val hasClientProfile: Boolean,
    @SerializedName("createdAt") val createdAt: String?
)

data class AdminPageDto<T>(
    @SerializedName("content") val content: List<T> = emptyList(),
    @SerializedName("page") val page: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("hasNext") val hasNext: Boolean
)

data class AdminUserRoleDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("role") val role: String
)

data class UpdateAdminUserRoleRequestDto(
    @SerializedName("role") val role: String
)

data class AdminWorkoutPlanDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("userFullName") val userFullName: String,
    @SerializedName("name") val name: String,
    @SerializedName("splitType") val splitType: String,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class AdminWorkoutPlanDetailDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("userFullName") val userFullName: String,
    @SerializedName("workoutPlan") val workoutPlan: WorkoutPlanResponseDto
)

data class AdminWorkoutSessionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("userFullName") val userFullName: String,
    @SerializedName("title") val title: String,
    @SerializedName("workoutDate") val workoutDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("workoutPlanId") val workoutPlanId: Long?,
    @SerializedName("workoutPlanName") val workoutPlanName: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("completedAt") val completedAt: String?
)

data class AdminWorkoutSessionDetailDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("userFullName") val userFullName: String,
    @SerializedName("workoutSession") val workoutSession: WorkoutSessionResponseDto
)

data class AdminExerciseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("category") val category: String,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("description") val description: String?,
    @SerializedName("targetMuscles") val targetMuscles: String?,
    @SerializedName("instructions") val instructions: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("videoUrl") val videoUrl: String?,
    @SerializedName("equipment") val equipment: String?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("planReferencesCount") val planReferencesCount: Long = 0L,
    @SerializedName("sessionReferencesCount") val sessionReferencesCount: Long = 0L,
    @SerializedName("canDelete") val canDelete: Boolean = true,
    @SerializedName("mediaItems") val mediaItems: List<ExerciseMediaResponseDto> = emptyList()
)

data class CreateAdminExerciseRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String? = null,
    @SerializedName("category") val category: String,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("targetMuscles") val targetMuscles: String? = null,
    @SerializedName("instructions") val instructions: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("videoUrl") val videoUrl: String? = null,
    @SerializedName("equipment") val equipment: String? = null,
    @SerializedName("active") val active: Boolean = true,
    @SerializedName("mediaItems") val mediaItems: List<ExerciseMediaItemRequestDto> = emptyList()
)

data class UpdateAdminExerciseRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String? = null,
    @SerializedName("category") val category: String,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("targetMuscles") val targetMuscles: String? = null,
    @SerializedName("instructions") val instructions: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("videoUrl") val videoUrl: String? = null,
    @SerializedName("equipment") val equipment: String? = null,
    @SerializedName("active") val active: Boolean? = null,
    @SerializedName("mediaItems") val mediaItems: List<ExerciseMediaItemRequestDto> = emptyList(),
    @SerializedName("replaceMediaItems") val replaceMediaItems: Boolean = false
)

data class ExerciseMediaItemRequestDto(
    @SerializedName("type") val type: String,
    @SerializedName("url") val url: String,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("displayOrder") val displayOrder: Int? = null
)

data class UpdateExerciseStatusRequestDto(
    @SerializedName("active") val active: Boolean
)

data class AdminExerciseDeleteResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("softDeleted") val softDeleted: Boolean,
    @SerializedName("message") val message: String
)
