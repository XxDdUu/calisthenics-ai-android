package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.mapper.toDto
import com.calisthenics.ai.data.remote.datasource.RemoteAdminDataSource
import com.calisthenics.ai.data.remote.dto.BulkExerciseGenerationRequestDto
import com.calisthenics.ai.data.remote.dto.CreateAdminExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.ExerciseMediaItemRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateAdminExerciseRequestDto
import com.calisthenics.ai.domain.model.AdminAiChatResponse
import com.calisthenics.ai.domain.model.BulkExerciseGenerationItem
import com.calisthenics.ai.domain.model.BulkExerciseGenerationJob
import com.calisthenics.ai.domain.model.AdminConfirmationDecision
import com.calisthenics.ai.domain.model.AdminAiConversationSummary
import com.calisthenics.ai.domain.model.AdminAiMessage
import com.calisthenics.ai.domain.model.AdminAiRecommendation
import com.calisthenics.ai.domain.model.AdminCoachClientStatistics
import com.calisthenics.ai.domain.model.AdminDashboard
import com.calisthenics.ai.domain.model.AdminExercise
import com.calisthenics.ai.domain.model.AdminExerciseDeleteResult
import com.calisthenics.ai.domain.model.AdminExerciseStats
import com.calisthenics.ai.domain.model.AdminGlobalUserStatistics
import com.calisthenics.ai.domain.model.AdminGlobalUserSummary
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminPlatformEngagement
import com.calisthenics.ai.domain.model.AdminPlatformOverview
import com.calisthenics.ai.domain.model.AdminTrainingAnalytics
import com.calisthenics.ai.domain.model.AdminUser
import com.calisthenics.ai.domain.model.AdminUserDetailDossier
import com.calisthenics.ai.domain.model.AdminUserSegmentFilter
import com.calisthenics.ai.domain.model.AdminUserSegments
import com.calisthenics.ai.domain.model.AdminWorkoutPlan
import com.calisthenics.ai.domain.model.AdminWorkoutPlanDetail
import com.calisthenics.ai.domain.model.AdminWorkoutSession
import com.calisthenics.ai.domain.model.AdminWorkoutSessionDetail
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.repository.AdminRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class AdminRepositoryImpl private constructor(
    private val remoteDataSource: RemoteAdminDataSource,
    private val retryDelaysMs: List<Long>
) : AdminRepository {

    @Inject
    constructor(remoteDataSource: RemoteAdminDataSource) : this(
        remoteDataSource = remoteDataSource,
        retryDelaysMs = listOf(450L, 1100L)
    )

    companion object {
        fun forTest(remoteDataSource: RemoteAdminDataSource, retryDelaysMs: List<Long> = emptyList()) =
            AdminRepositoryImpl(remoteDataSource, retryDelaysMs)
    }

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

    override suspend fun grantAdminPro(userId: Long, reason: String?): Result<AdminUser> = runCatching {
        remoteDataSource.grantAdminPro(userId, reason).toDomain()
    }

    override suspend fun revokeAdminPro(userId: Long, reason: String?): Result<AdminUser> = runCatching {
        remoteDataSource.revokeAdminPro(userId, reason).toDomain()
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

    override suspend fun getExercisesFiltered(
        page: Int,
        size: Int,
        search: String?,
        category: String?,
        difficulty: String?,
        active: Boolean?,
        status: String?,
        origin: String?,
        sortBy: String,
        sortDirection: String
    ): Result<AdminPage<AdminExercise>> = runCatching {
        remoteDataSource.getExercises(page, size, search, category, difficulty, active, sortBy, sortDirection, status, origin)
            .toDomain { it.toDomain() }
    }

    override suspend fun getExercisesFilteredWithTrainingType(
        page: Int,
        size: Int,
        search: String?,
        category: String?,
        difficulty: String?,
        active: Boolean?,
        status: String?,
        origin: String?,
        trainingType: String?,
        sortBy: String,
        sortDirection: String
    ): Result<AdminPage<AdminExercise>> = runCatching {
        remoteDataSource.getExercises(
            page, size, search, category, difficulty, active, sortBy, sortDirection, status, origin, trainingType
        ).toDomain { it.toDomain() }
    }

    override suspend fun startBulkExerciseGeneration(
        prompt: String?,
        quantity: Int?,
        trainingType: String?,
        categories: List<String>?,
        focus: String?,
        conversationId: Long?,
        idempotencyKey: String?
    ): Result<BulkExerciseGenerationJob> = executeWithTransientRetry(maxRetries = 1) {
        val key = idempotencyKey?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        remoteDataSource.startBulkExerciseGeneration(
            BulkExerciseGenerationRequestDto(
                prompt = prompt,
                quantity = quantity,
                trainingType = trainingType,
                categories = categories,
                focus = focus,
                conversationId = conversationId,
                idempotencyKey = key
            ),
            idempotencyKey = key
        ).toDomain()
    }

    override suspend fun listBulkExerciseGenerationJobs(
        page: Int,
        size: Int
    ): Result<AdminPage<BulkExerciseGenerationJob>> = runCatching {
        remoteDataSource.listBulkExerciseGenerationJobs(page, size).toDomain { it.toDomain() }
    }

    override suspend fun getBulkExerciseGenerationJob(jobId: Long): Result<BulkExerciseGenerationJob> = runCatching {
        remoteDataSource.getBulkExerciseGenerationJob(jobId).toDomain()
    }

    override suspend fun getBulkExerciseGenerationItems(
        jobId: Long,
        outcome: String?,
        page: Int,
        size: Int
    ): Result<AdminPage<BulkExerciseGenerationItem>> = runCatching {
        remoteDataSource.getBulkExerciseGenerationItems(jobId, outcome, page, size)
            .toDomain { it.toDomain() }
    }

    override suspend fun resumeBulkExerciseGenerationJob(jobId: Long): Result<BulkExerciseGenerationJob> = runCatching {
        remoteDataSource.resumeBulkExerciseGenerationJob(jobId).toDomain()
    }

    override suspend fun getExerciseStats(): Result<AdminExerciseStats> = runCatching {
        remoteDataSource.getExerciseStats().toDomain()
    }

    override suspend fun bulkUpdateExerciseStatus(exerciseIds: List<Long>, active: Boolean): Result<List<AdminExercise>> = runCatching {
        remoteDataSource.bulkUpdateExerciseStatus(exerciseIds, active).map { it.toDomain() }
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

    override suspend fun sendAdminAiChat(
        conversationId: Long?,
        message: String,
        range: String,
        startDate: String?,
        endDate: String?
    ): Result<AdminAiChatResponse> = sendAdminAiChatIdempotent(
        conversationId = conversationId,
        message = message,
        range = range,
        startDate = startDate,
        endDate = endDate,
        idempotencyKey = UUID.randomUUID().toString()
    )

    override suspend fun sendAdminAiChatIdempotent(
        conversationId: Long?,
        message: String,
        range: String,
        startDate: String?,
        endDate: String?,
        idempotencyKey: String?
    ): Result<AdminAiChatResponse> {
        val stableKey = idempotencyKey?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        return executeWithTransientRetry(maxRetries = 2) {
            remoteDataSource.sendAdminAiChat(
                conversationId = conversationId,
                message = message,
                range = range,
                startDate = startDate,
                endDate = endDate,
                idempotencyKey = stableKey
            ).toDomain()
        }
    }

    private suspend fun <T> executeWithTransientRetry(
        maxRetries: Int,
        block: suspend () -> T
    ): Result<T> {
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt <= maxRetries) {
            try {
                return Result.success(block())
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                lastError = t
                if (attempt >= maxRetries || !isTransientRetryable(t)) {
                    return Result.failure(t)
                }
                val delayMs = retryDelaysMs.getOrElse(attempt) { 0L }
                attempt++
                if (delayMs > 0L) {
                    delay(delayMs)
                }
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Unknown request failure"))
    }

    private fun isTransientRetryable(t: Throwable): Boolean = when (t) {
        is IOException -> true
        is HttpException -> {
            val code = t.code()
            code == 408 || code == 429 || code in 500..599
        }
        else -> false
    }

    override suspend fun resolveAdminAiConfirmation(
        confirmationToken: String,
        approved: Boolean,
        conversationId: Long?
    ): Result<AdminConfirmationDecision> = runCatching {
        remoteDataSource.resolveAdminAiConfirmation(confirmationToken, approved, conversationId).toDomain()
    }

    override suspend fun getAdminAiConversations(): Result<List<AdminAiConversationSummary>> = runCatching {
        remoteDataSource.getAdminAiConversations().map { it.toDomain() }
    }

    override suspend fun createAdminAiConversation(title: String?): Result<AdminAiConversationSummary> = runCatching {
        remoteDataSource.createAdminAiConversation(title).toDomain()
    }

    override suspend fun getAdminAiConversationMessages(conversationId: Long): Result<List<AdminAiMessage>> = runCatching {
        remoteDataSource.getAdminAiConversationMessages(conversationId).map { it.toDomain() }
    }

    override suspend fun getAdminAiPlatformOverview(
        range: String,
        startDate: String?,
        endDate: String?
    ): Result<AdminPlatformOverview> = runCatching {
        remoteDataSource.getAdminAiPlatformOverview(range, startDate, endDate).toDomain()
    }

    override suspend fun getAdminAiTrainingAnalytics(
        range: String,
        startDate: String?,
        endDate: String?
    ): Result<AdminTrainingAnalytics> = runCatching {
        remoteDataSource.getAdminAiTrainingAnalytics(range, startDate, endDate).toDomain()
    }

    override suspend fun getAdminAiRecommendations(
        type: String?,
        status: String?
    ): Result<List<AdminAiRecommendation>> = runCatching {
        remoteDataSource.getAdminAiRecommendations(type, status).map { it.toDomain() }
    }

    override suspend fun createAdminAiRecommendations(
        type: String,
        targetSkill: String?,
        trainingLevel: String?,
        prompt: String?
    ): Result<List<AdminAiRecommendation>> = runCatching {
        remoteDataSource.createAdminAiRecommendations(type, targetSkill, trainingLevel, prompt).map { it.toDomain() }
    }

    override suspend fun approveAdminAiRecommendation(
        id: Long,
        reviewNotes: String?,
        publishExercise: Boolean?
    ): Result<AdminAiRecommendation> = runCatching {
        remoteDataSource.approveAdminAiRecommendation(id, reviewNotes, publishExercise).toDomain()
    }

    override suspend fun rejectAdminAiRecommendation(
        id: Long,
        reviewNotes: String?
    ): Result<AdminAiRecommendation> = runCatching {
        remoteDataSource.rejectAdminAiRecommendation(id, reviewNotes).toDomain()
    }

    override suspend fun implementAdminAiRecommendation(
        id: Long,
        reviewNotes: String?
    ): Result<AdminAiRecommendation> = runCatching {
        remoteDataSource.implementAdminAiRecommendation(id, reviewNotes).toDomain()
    }

    private fun List<ExerciseMedia>.toRequestDtos() = map {
        ExerciseMediaItemRequestDto(
            type = it.type.name,
            url = it.url,
            thumbnailUrl = it.thumbnailUrl,
            displayOrder = it.displayOrder
        )
    }

    override suspend fun getGlobalUserStatistics(
        range: String,
        startDate: String?,
        endDate: String?
    ): Result<AdminGlobalUserStatistics> = runCatching {
        remoteDataSource.getGlobalUserStatistics(range, startDate, endDate).toDomain()
    }

    override suspend fun getGlobalUsers(
        page: Int,
        size: Int,
        search: String?,
        experienceLevel: String?,
        fitnessGoal: String?,
        activityStatus: String?,
        inactiveDaysThreshold: Int?,
        maxCompletionRate: Double?,
        coachRelationshipStatus: String?,
        roleFilter: String?,
        skillKeyword: String?,
        includeEmail: Boolean,
        sortBy: String?,
        range: String,
        startDate: String?,
        endDate: String?
    ): Result<AdminPage<AdminGlobalUserSummary>> = runCatching {
        remoteDataSource.getGlobalUsers(
            page = page,
            size = size,
            search = search,
            experienceLevel = experienceLevel,
            fitnessGoal = fitnessGoal,
            activityStatus = activityStatus,
            inactiveDaysThreshold = inactiveDaysThreshold,
            maxCompletionRate = maxCompletionRate,
            coachRelationshipStatus = coachRelationshipStatus,
            roleFilter = roleFilter,
            skillKeyword = skillKeyword,
            includeEmail = includeEmail,
            sortBy = sortBy,
            range = range,
            startDate = startDate,
            endDate = endDate
        ).toDomain { it.toDomain() }
    }

    override suspend fun getGlobalUserDetail(
        userId: Long,
        includeEmail: Boolean,
        range: String,
        startDate: String?,
        endDate: String?
    ): Result<AdminUserDetailDossier> = runCatching {
        remoteDataSource.getGlobalUserDetail(userId, includeEmail, range, startDate, endDate).toDomain()
    }

    override suspend fun getCoachClientStatistics(
        range: String,
        startDate: String?,
        endDate: String?
    ): Result<AdminCoachClientStatistics> = runCatching {
        remoteDataSource.getCoachClientStatistics(range, startDate, endDate).toDomain()
    }

    override suspend fun getPlatformEngagement(
        range: String,
        startDate: String?,
        endDate: String?
    ): Result<AdminPlatformEngagement> = runCatching {
        remoteDataSource.getPlatformEngagement(range, startDate, endDate).toDomain()
    }

    override suspend fun analyzeUserSegments(
        filter: AdminUserSegmentFilter
    ): Result<AdminUserSegments> = runCatching {
        remoteDataSource.analyzeUserSegments(filter.toDto()).toDomain()
    }
}
