package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AdminAiChatResponse
import com.calisthenics.ai.domain.model.AdminAiConversationSummary
import com.calisthenics.ai.domain.model.AdminAiMessage
import com.calisthenics.ai.domain.model.AdminAiRecommendation
import com.calisthenics.ai.domain.model.BulkExerciseGenerationItem
import com.calisthenics.ai.domain.model.BulkExerciseGenerationJob
import com.calisthenics.ai.domain.model.AdminCoachClientStatistics
import com.calisthenics.ai.domain.model.AdminConfirmationDecision
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

interface AdminRepository {
    suspend fun getDashboard(): Result<AdminDashboard>
    suspend fun getGlobalUserStatistics(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): Result<AdminGlobalUserStatistics>
    suspend fun getGlobalUsers(
        page: Int = 0,
        size: Int = 20,
        search: String? = null,
        experienceLevel: String? = null,
        fitnessGoal: String? = null,
        activityStatus: String? = null,
        inactiveDaysThreshold: Int? = null,
        maxCompletionRate: Double? = null,
        coachRelationshipStatus: String? = null,
        roleFilter: String? = null,
        skillKeyword: String? = null,
        includeEmail: Boolean = false,
        sortBy: String? = null,
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): Result<AdminPage<AdminGlobalUserSummary>>
    suspend fun getGlobalUserDetail(
        userId: Long,
        includeEmail: Boolean = false,
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): Result<AdminUserDetailDossier>
    suspend fun getCoachClientStatistics(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): Result<AdminCoachClientStatistics>
    suspend fun getPlatformEngagement(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): Result<AdminPlatformEngagement>
    suspend fun analyzeUserSegments(
        filter: AdminUserSegmentFilter
    ): Result<AdminUserSegments>
    suspend fun getUsers(
        page: Int,
        size: Int,
        search: String?,
        role: String?
    ): Result<AdminPage<AdminUser>>
    suspend fun getUser(userId: Long): Result<AdminUser>
    suspend fun getUserRole(userId: Long): Result<String>
    suspend fun updateUserRole(userId: Long, role: String): Result<AdminUser>
    suspend fun grantAdminPro(userId: Long, reason: String? = null): Result<AdminUser>
    suspend fun revokeAdminPro(userId: Long, reason: String? = null): Result<AdminUser>
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
    suspend fun getExercisesFiltered(
        page: Int,
        size: Int,
        search: String?,
        category: String?,
        difficulty: String?,
        active: Boolean?,
        status: String?,
        origin: String?,
        sortBy: String = "name",
        sortDirection: String = "ASC"
    ): Result<AdminPage<AdminExercise>> = getExercises(page, size, search, category, difficulty, active, sortBy, sortDirection)
    suspend fun getExercisesFilteredWithTrainingType(
        page: Int,
        size: Int,
        search: String?,
        category: String?,
        difficulty: String?,
        active: Boolean?,
        status: String?,
        origin: String?,
        trainingType: String? = null,
        sortBy: String = "name",
        sortDirection: String = "ASC"
    ): Result<AdminPage<AdminExercise>> = getExercisesFiltered(page, size, search, category, difficulty, active, status, origin, sortBy, sortDirection)
    suspend fun startBulkExerciseGeneration(
        prompt: String?,
        quantity: Int?,
        trainingType: String?,
        categories: List<String>?,
        focus: String?,
        conversationId: Long?,
        idempotencyKey: String?
    ): Result<BulkExerciseGenerationJob> = Result.failure(UnsupportedOperationException())
    suspend fun listBulkExerciseGenerationJobs(page: Int = 0, size: Int = 20): Result<AdminPage<BulkExerciseGenerationJob>> =
        Result.failure(UnsupportedOperationException())
    suspend fun getBulkExerciseGenerationJob(jobId: Long): Result<BulkExerciseGenerationJob> =
        Result.failure(UnsupportedOperationException())
    suspend fun getBulkExerciseGenerationItems(
        jobId: Long,
        outcome: String? = null,
        page: Int = 0,
        size: Int = 25
    ): Result<AdminPage<BulkExerciseGenerationItem>> = Result.failure(UnsupportedOperationException())
    suspend fun resumeBulkExerciseGenerationJob(jobId: Long): Result<BulkExerciseGenerationJob> =
        Result.failure(UnsupportedOperationException())
    suspend fun getExerciseStats(): Result<AdminExerciseStats> = Result.failure(UnsupportedOperationException())
    suspend fun bulkUpdateExerciseStatus(exerciseIds: List<Long>, active: Boolean): Result<List<AdminExercise>> =
        Result.failure(UnsupportedOperationException())
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

    // Admin AI Agent
    suspend fun sendAdminAiChat(
        conversationId: Long?,
        message: String,
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): Result<AdminAiChatResponse>
    suspend fun sendAdminAiChatIdempotent(
        conversationId: Long?,
        message: String,
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null,
        idempotencyKey: String? = null
    ): Result<AdminAiChatResponse> = sendAdminAiChat(conversationId, message, range, startDate, endDate)
    suspend fun resolveAdminAiConfirmation(
        confirmationToken: String,
        approved: Boolean,
        conversationId: Long? = null
    ): Result<AdminConfirmationDecision>
    suspend fun getAdminAiConversations(): Result<List<AdminAiConversationSummary>>
    suspend fun createAdminAiConversation(title: String? = null): Result<AdminAiConversationSummary>
    suspend fun getAdminAiConversationMessages(conversationId: Long): Result<List<AdminAiMessage>>
    suspend fun getAdminAiPlatformOverview(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): Result<AdminPlatformOverview>
    suspend fun getAdminAiTrainingAnalytics(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): Result<AdminTrainingAnalytics>
    suspend fun getAdminAiRecommendations(
        type: String? = null,
        status: String? = null
    ): Result<List<AdminAiRecommendation>>
    suspend fun createAdminAiRecommendations(
        type: String,
        targetSkill: String? = null,
        trainingLevel: String? = null,
        prompt: String? = null
    ): Result<List<AdminAiRecommendation>>
    suspend fun approveAdminAiRecommendation(
        id: Long,
        reviewNotes: String? = null,
        publishExercise: Boolean? = null
    ): Result<AdminAiRecommendation>
    suspend fun rejectAdminAiRecommendation(
        id: Long,
        reviewNotes: String? = null
    ): Result<AdminAiRecommendation>
    suspend fun implementAdminAiRecommendation(
        id: Long,
        reviewNotes: String? = null
    ): Result<AdminAiRecommendation>
}
