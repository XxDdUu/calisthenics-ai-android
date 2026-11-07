package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AdminDashboard
import com.calisthenics.ai.domain.model.AdminExercise
import com.calisthenics.ai.domain.model.AdminExerciseDeleteResult
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminUser
import com.calisthenics.ai.domain.model.AdminWorkoutPlan
import com.calisthenics.ai.domain.model.AdminWorkoutPlanDetail
import com.calisthenics.ai.domain.model.AdminWorkoutSession
import com.calisthenics.ai.domain.model.AdminWorkoutSessionDetail
import com.calisthenics.ai.domain.model.ExerciseMedia

interface AdminRepository {
    suspend fun getDashboard(): Result<AdminDashboard>
    suspend fun getUsers(
        page: Int,
        size: Int,
        search: String?,
        role: String?
    ): Result<AdminPage<AdminUser>>
    suspend fun getUser(userId: Long): Result<AdminUser>
    suspend fun getUserRole(userId: Long): Result<String>
    suspend fun updateUserRole(userId: Long, role: String): Result<AdminUser>
    suspend fun getWorkoutPlans(page: Int, size: Int): Result<AdminPage<AdminWorkoutPlan>>
    suspend fun getWorkoutPlan(planId: Long): Result<AdminWorkoutPlanDetail>
    suspend fun getWorkoutSessions(page: Int, size: Int): Result<AdminPage<AdminWorkoutSession>>
    suspend fun getWorkoutSession(sessionId: Long): Result<AdminWorkoutSessionDetail>
    suspend fun getExercises(
        page: Int,
        size: Int,
        search: String?,
        category: String?,
        difficulty: String?,
        active: Boolean?,
        sortBy: String = "name",
        sortDirection: String = "ASC"
    ): Result<AdminPage<AdminExercise>>
    suspend fun getExercise(exerciseId: Long): Result<AdminExercise>
    suspend fun createExercise(
        name: String,
        code: String?,
        category: String,
        difficulty: String,
        description: String?,
        targetMuscles: String?,
        instructions: String?,
        imageUrl: String?,
        videoUrl: String?,
        equipment: String?,
        active: Boolean = true,
        mediaItems: List<ExerciseMedia> = emptyList()
    ): Result<AdminExercise>
    suspend fun updateExercise(
        exerciseId: Long,
        name: String,
        code: String?,
        category: String,
        difficulty: String,
        description: String?,
        targetMuscles: String?,
        instructions: String?,
        imageUrl: String?,
        videoUrl: String?,
        equipment: String?,
        active: Boolean? = null,
        mediaItems: List<ExerciseMedia> = emptyList()
    ): Result<AdminExercise>
    suspend fun updateExerciseStatus(exerciseId: Long, active: Boolean): Result<AdminExercise>
    suspend fun deleteExercise(exerciseId: Long, softDelete: Boolean): Result<AdminExerciseDeleteResult>
}
