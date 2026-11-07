package com.calisthenics.ai.domain.model

data class AdminDashboard(
    val totalUsers: Long,
    val totalClients: Long,
    val totalCoaches: Long,
    val totalAdmins: Long,
    val totalWorkoutSessions: Long,
    val totalWorkoutPlans: Long,
    val totalExercises: Long = 0L
)

data class AdminUser(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String,
    val hasCoachProfile: Boolean,
    val hasClientProfile: Boolean,
    val createdAt: String?
)

data class AdminPage<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)

data class AdminWorkoutPlan(
    val id: Long,
    val userId: Long,
    val userEmail: String,
    val userFullName: String,
    val name: String,
    val splitType: String,
    val createdAt: String?,
    val updatedAt: String?
)

data class AdminWorkoutPlanDetail(
    val userId: Long,
    val userEmail: String,
    val userFullName: String,
    val plan: UserWorkoutPlan
)

data class AdminWorkoutSession(
    val id: Long,
    val userId: Long,
    val userEmail: String,
    val userFullName: String,
    val title: String,
    val workoutDate: String?,
    val status: String,
    val workoutPlanId: Long?,
    val workoutPlanName: String?,
    val createdAt: String?,
    val startedAt: String?,
    val completedAt: String?
)

data class AdminWorkoutSessionDetail(
    val userId: Long,
    val userEmail: String,
    val userFullName: String,
    val session: WorkoutSession
)

data class AdminExercise(
    val id: Long,
    val name: String,
    val code: String,
    val category: String,
    val difficulty: String,
    val description: String?,
    val targetMuscles: String?,
    val instructions: String?,
    val imageUrl: String?,
    val videoUrl: String?,
    val equipment: String?,
    val active: Boolean,
    val createdAt: String?,
    val planReferencesCount: Long = 0L,
    val sessionReferencesCount: Long = 0L,
    val canDelete: Boolean = true,
    val mediaItems: List<ExerciseMedia> = emptyList()
)

data class AdminExerciseDeleteResult(
    val success: Boolean,
    val softDeleted: Boolean,
    val message: String
)
