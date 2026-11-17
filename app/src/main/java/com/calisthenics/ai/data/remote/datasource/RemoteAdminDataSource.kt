package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.AdminAiChatRequestDto
import com.calisthenics.ai.data.remote.dto.AdminAiChatResponseDto
import com.calisthenics.ai.data.remote.dto.AdminConfirmationDecisionRequestDto
import com.calisthenics.ai.data.remote.dto.AdminConfirmationDecisionResponseDto
import com.calisthenics.ai.data.remote.dto.AdminAiConversationSummaryDto
import com.calisthenics.ai.data.remote.dto.AdminAiMessageDto
import com.calisthenics.ai.data.remote.dto.AdminAiRecommendationDto
import com.calisthenics.ai.data.remote.dto.AdminCoachClientStatisticsDto
import com.calisthenics.ai.data.remote.dto.AdminDashboardDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDeleteResponseDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDto
import com.calisthenics.ai.data.remote.dto.AdminGlobalUserStatisticsDto
import com.calisthenics.ai.data.remote.dto.AdminGlobalUserSummaryDto
import com.calisthenics.ai.data.remote.dto.AdminPageDto
import com.calisthenics.ai.data.remote.dto.AdminPagedResultDto
import com.calisthenics.ai.data.remote.dto.AdminPlatformEngagementDto
import com.calisthenics.ai.data.remote.dto.AdminPlatformOverviewDto
import com.calisthenics.ai.data.remote.dto.AdminTrainingAnalyticsDto
import com.calisthenics.ai.data.remote.dto.AdminUserDetailDossierDto
import com.calisthenics.ai.data.remote.dto.AdminUserDto
import com.calisthenics.ai.data.remote.dto.AdminUserRoleDto
import com.calisthenics.ai.data.remote.dto.AdminUserSegmentFilterRequestDto
import com.calisthenics.ai.data.remote.dto.AdminUserSegmentsDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDto
import com.calisthenics.ai.data.remote.dto.CreateAdminAiConversationRequestDto
import com.calisthenics.ai.data.remote.dto.CreateAdminExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.CreateAdminRecommendationRequestDto
import com.calisthenics.ai.data.remote.dto.ReviewAdminRecommendationRequestDto
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

    suspend fun grantAdminPro(userId: Long, reason: String? = null): AdminUserDto =
        api.grantAdminPro(userId, com.calisthenics.ai.data.remote.dto.AdminProActionRequestDto(reason))

    suspend fun revokeAdminPro(userId: Long, reason: String? = null): AdminUserDto =
        api.revokeAdminPro(userId, com.calisthenics.ai.data.remote.dto.AdminProActionRequestDto(reason))

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

    suspend fun sendAdminAiChat(
        conversationId: Long?,
        message: String,
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): AdminAiChatResponseDto = api.sendAdminAiChat(
        AdminAiChatRequestDto(
            conversationId = conversationId,
            message = message,
            range = range,
            startDate = startDate,
            endDate = endDate
        )
    )

    suspend fun resolveAdminAiConfirmation(
        confirmationToken: String,
        approved: Boolean,
        conversationId: Long? = null
    ): AdminConfirmationDecisionResponseDto = api.resolveAdminAiConfirmation(
        confirmationToken = confirmationToken,
        request = AdminConfirmationDecisionRequestDto(
            approved = approved,
            conversationId = conversationId
        )
    )

    suspend fun getAdminAiConversations(): List<AdminAiConversationSummaryDto> =
        api.getAdminAiConversations()

    suspend fun createAdminAiConversation(title: String?): AdminAiConversationSummaryDto =
        api.createAdminAiConversation(CreateAdminAiConversationRequestDto(title = title))

    suspend fun getAdminAiConversationMessages(conversationId: Long): List<AdminAiMessageDto> =
        api.getAdminAiConversationMessages(conversationId)

    suspend fun getAdminAiPlatformOverview(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): AdminPlatformOverviewDto = api.getAdminAiPlatformOverview(range, startDate, endDate)

    suspend fun getAdminAiTrainingAnalytics(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): AdminTrainingAnalyticsDto = api.getAdminAiTrainingAnalytics(range, startDate, endDate)

    suspend fun getAdminAiRecommendations(
        type: String? = null,
        status: String? = null
    ): List<AdminAiRecommendationDto> = api.getAdminAiRecommendations(type, status)

    suspend fun createAdminAiRecommendations(
        type: String,
        targetSkill: String? = null,
        trainingLevel: String? = null,
        prompt: String? = null
    ): List<AdminAiRecommendationDto> = api.createAdminAiRecommendations(
        CreateAdminRecommendationRequestDto(
            type = type,
            targetSkill = targetSkill,
            trainingLevel = trainingLevel,
            prompt = prompt
        )
    )

    suspend fun approveAdminAiRecommendation(
        id: Long,
        reviewNotes: String? = null,
        publishExercise: Boolean? = null
    ): AdminAiRecommendationDto = api.approveAdminAiRecommendation(
        id,
        ReviewAdminRecommendationRequestDto(reviewNotes = reviewNotes, publishExercise = publishExercise)
    )

    suspend fun rejectAdminAiRecommendation(
        id: Long,
        reviewNotes: String? = null
    ): AdminAiRecommendationDto = api.rejectAdminAiRecommendation(
        id,
        ReviewAdminRecommendationRequestDto(reviewNotes = reviewNotes)
    )

    suspend fun implementAdminAiRecommendation(
        id: Long,
        reviewNotes: String? = null
    ): AdminAiRecommendationDto = api.implementAdminAiRecommendation(
        id,
        ReviewAdminRecommendationRequestDto(reviewNotes = reviewNotes, publishExercise = true)
    )

    suspend fun getGlobalUserStatistics(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): AdminGlobalUserStatisticsDto = api.getAdminAiGlobalUserStatistics(
        range = range,
        startDate = startDate,
        endDate = endDate
    )

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
    ): AdminPagedResultDto<AdminGlobalUserSummaryDto> = api.getAdminAiGlobalUsers(
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
    )

    suspend fun getGlobalUserDetail(
        userId: Long,
        includeEmail: Boolean = false,
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): AdminUserDetailDossierDto = api.getAdminAiGlobalUserDetail(
        userId = userId,
        includeEmail = includeEmail,
        range = range,
        startDate = startDate,
        endDate = endDate
    )

    suspend fun getCoachClientStatistics(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): AdminCoachClientStatisticsDto = api.getAdminAiCoachClientStatistics(
        range = range,
        startDate = startDate,
        endDate = endDate
    )

    suspend fun getPlatformEngagement(
        range: String = "LAST_30_DAYS",
        startDate: String? = null,
        endDate: String? = null
    ): AdminPlatformEngagementDto = api.getAdminAiPlatformEngagement(
        range = range,
        startDate = startDate,
        endDate = endDate
    )

    suspend fun analyzeUserSegments(
        filter: AdminUserSegmentFilterRequestDto
    ): AdminUserSegmentsDto = api.analyzeAdminAiUserSegments(filter)
}
