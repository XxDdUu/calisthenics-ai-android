package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.datasource.RemoteAdminDataSource
import com.calisthenics.ai.data.remote.dto.CreateAdminExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.ExerciseMediaItemRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateAdminExerciseRequestDto
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
import com.calisthenics.ai.domain.repository.AdminRepository
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteAdminDataSource
) : AdminRepository {

    override suspend fun getDashboard(): Result<AdminDashboard> = runCatching {
        remoteDataSource.getDashboard().toDomain()
    }

    override suspend fun getUsers(
        page: Int,
        size: Int,
        search: String?,
        role: String?
    ): Result<AdminPage<AdminUser>> = runCatching {
        remoteDataSource.getUsers(page, size, search, role).toDomain { it.toDomain() }
    }

    override suspend fun getUser(userId: Long): Result<AdminUser> = runCatching {
        remoteDataSource.getUser(userId).toDomain()
    }

    override suspend fun getUserRole(userId: Long): Result<String> = runCatching {
        remoteDataSource.getUserRole(userId).role
    }

    override suspend fun updateUserRole(userId: Long, role: String): Result<AdminUser> = runCatching {
        remoteDataSource.updateUserRole(userId, role).toDomain()
    }

    override suspend fun getWorkoutPlans(page: Int, size: Int): Result<AdminPage<AdminWorkoutPlan>> = runCatching {
        remoteDataSource.getWorkoutPlans(page, size).toDomain { it.toDomain() }
    }

    override suspend fun getWorkoutPlan(planId: Long): Result<AdminWorkoutPlanDetail> = runCatching {
        remoteDataSource.getWorkoutPlan(planId).toDomain()
    }

    override suspend fun getWorkoutSessions(page: Int, size: Int): Result<AdminPage<AdminWorkoutSession>> = runCatching {
        remoteDataSource.getWorkoutSessions(page, size).toDomain { it.toDomain() }
    }

    override suspend fun getWorkoutSession(sessionId: Long): Result<AdminWorkoutSessionDetail> = runCatching {
        remoteDataSource.getWorkoutSession(sessionId).toDomain()
    }

    override suspend fun getExercises(
        page: Int,
        size: Int,
        search: String?,
        category: String?,
        difficulty: String?,
        active: Boolean?,
        sortBy: String,
        sortDirection: String
    ): Result<AdminPage<AdminExercise>> = runCatching {
        remoteDataSource.getExercises(page, size, search, category, difficulty, active, sortBy, sortDirection)
            .toDomain { it.toDomain() }
    }

    override suspend fun getExercise(exerciseId: Long): Result<AdminExercise> = runCatching {
        remoteDataSource.getExercise(exerciseId).toDomain()
    }

    override suspend fun createExercise(
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
        active: Boolean,
        mediaItems: List<ExerciseMedia>
    ): Result<AdminExercise> = runCatching {
        remoteDataSource.createExercise(
            CreateAdminExerciseRequestDto(
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
                mediaItems = mediaItems.toRequestDtos()
            )
        ).toDomain()
    }

    override suspend fun updateExercise(
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
        active: Boolean?,
        mediaItems: List<ExerciseMedia>
    ): Result<AdminExercise> = runCatching {
        remoteDataSource.updateExercise(
            exerciseId,
            UpdateAdminExerciseRequestDto(
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
                mediaItems = mediaItems.toRequestDtos(),
                replaceMediaItems = true
            )
        ).toDomain()
    }

    override suspend fun updateExerciseStatus(exerciseId: Long, active: Boolean): Result<AdminExercise> = runCatching {
        remoteDataSource.updateExerciseStatus(exerciseId, active).toDomain()
    }

    override suspend fun deleteExercise(exerciseId: Long, softDelete: Boolean): Result<AdminExerciseDeleteResult> = runCatching {
        remoteDataSource.deleteExercise(exerciseId, softDelete).toDomain()
    }

    private fun List<ExerciseMedia>.toRequestDtos() = map {
        ExerciseMediaItemRequestDto(
            type = it.type.name,
            url = it.url,
            thumbnailUrl = it.thumbnailUrl,
            displayOrder = it.displayOrder
        )
    }
}
