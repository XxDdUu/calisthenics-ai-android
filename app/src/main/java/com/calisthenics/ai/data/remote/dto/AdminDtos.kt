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
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("membershipTier") val membershipTier: String? = null,
    @SerializedName("membershipSource") val membershipSource: String? = null,
    @SerializedName("unlimited") val unlimited: Boolean = false,
    @SerializedName("paidPro") val paidPro: Boolean = false,
    @SerializedName("adminGrantedPro") val adminGrantedPro: Boolean = false,
    @SerializedName("adminProStatus") val adminProStatus: String? = null,
    @SerializedName("adminProGrantSource") val adminProGrantSource: String? = null,
    @SerializedName("grantedByAdminId") val grantedByAdminId: Long? = null,
    @SerializedName("adminProGrantedAt") val adminProGrantedAt: String? = null,
    @SerializedName("revokedByAdminId") val revokedByAdminId: Long? = null,
    @SerializedName("adminProRevokedAt") val adminProRevokedAt: String? = null,
    @SerializedName("adminProReason") val adminProReason: String? = null
)

data class AdminProActionRequestDto(
    @SerializedName("reason") val reason: String? = null
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

// Admin AI Agent DTOs

data class AdminAiConversationSummaryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("lastMessagePreview") val lastMessagePreview: String? = null,
    @SerializedName("lastMessageRole") val lastMessageRole: String? = null,
    @SerializedName("lastInteractionAt") val lastInteractionAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("messageCount") val messageCount: Int = 0
)

data class AdminAiMessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
    @SerializedName("intent") val intent: String? = null,
    @SerializedName("metadata") val metadata: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class CreateAdminAiConversationRequestDto(
    @SerializedName("title") val title: String? = null
)

data class AdminAiChatRequestDto(
    @SerializedName("conversationId") val conversationId: Long? = null,
    @SerializedName("message") val message: String,
    @SerializedName("range") val range: String? = "LAST_30_DAYS",
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("targetUserId") val targetUserId: Long? = null,
    @SerializedName("includeEmail") val includeEmail: Boolean? = false
)

data class AdminAiChatResponseDto(
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("conversationTitle") val conversationTitle: String? = null,
    @SerializedName("userMessage") val userMessage: AdminAiMessageDto? = null,
    @SerializedName("assistantMessage") val assistantMessage: AdminAiMessageDto,
    @SerializedName("intent") val intent: String? = null,
    @SerializedName("recommendations") val recommendations: List<AdminAiRecommendationDto> = emptyList(),
    @SerializedName("platformOverview") val platformOverview: AdminPlatformOverviewDto? = null,
    @SerializedName("trainingAnalytics") val trainingAnalytics: AdminTrainingAnalyticsDto? = null,
    @SerializedName("executedTools") val executedTools: List<AdminToolExecutionRecordDto> = emptyList(),
    @SerializedName("timeRangeUsed") val timeRangeUsed: AdminTimeRangeMetadataDto? = null,
    @SerializedName("userStatistics") val userStatistics: AdminGlobalUserStatisticsDto? = null,
    @SerializedName("matchedUsers") val matchedUsers: List<AdminGlobalUserSummaryDto> = emptyList(),
    @SerializedName("inspectedUserDetail") val inspectedUserDetail: AdminUserDetailDossierDto? = null,
    @SerializedName("segmentAnalysis") val segmentAnalysis: AdminUserSegmentsDto? = null,
    @SerializedName("platformEngagement") val platformEngagement: AdminPlatformEngagementDto? = null,
    @SerializedName("coachClientStatistics") val coachClientStatistics: AdminCoachClientStatisticsDto? = null,
    @SerializedName("gettingStartedProgram") val gettingStartedProgram: GettingStartedProgramResponseDto? = null,
    @SerializedName("gettingStartedPreview") val gettingStartedPreview: GettingStartedProgramPreviewDto? = null,
    @SerializedName("activeCampaigns") val activeCampaigns: List<GettingStartedCampaignSummaryDto> = emptyList(),
    @SerializedName("campaignAnalytics") val campaignAnalytics: GettingStartedCampaignAnalyticsDto? = null,
    @SerializedName("pendingConfirmation") val pendingConfirmation: AdminPendingConfirmationDto? = null,
    @SerializedName("updatedProUser") val updatedProUser: AdminUserDto? = null,
    @SerializedName("proCandidates") val proCandidates: List<AdminUserDto> = emptyList(),
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList(),
    @SerializedName("suggestedFollowUps") val suggestedFollowUps: List<String> = emptyList()
)

data class GettingStartedProgramPreviewDto(
    @SerializedName("program") val program: GettingStartedProgramResponseDto? = null,
    @SerializedName("valid") val valid: Boolean = true,
    @SerializedName("validationErrors") val validationErrors: List<String> = emptyList(),
    @SerializedName("targetAudience") val targetAudience: String? = null,
    @SerializedName("audienceLabel") val audienceLabel: String? = null,
    @SerializedName("eligibleAthleteCount") val eligibleAthleteCount: Long = 0L,
    @SerializedName("totalAthleteCount") val totalAthleteCount: Long = 0L,
    @SerializedName("estimatedAudienceSize") val estimatedAudienceSize: Long = 0L
)

data class GettingStartedCampaignSummaryDto(
    @SerializedName("programId") val programId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("published") val published: Boolean = false,
    @SerializedName("version") val version: Int = 1,
    @SerializedName("targetAudience") val targetAudience: String? = null,
    @SerializedName("audienceLabel") val audienceLabel: String? = null,
    @SerializedName("eligibleAthleteCount") val eligibleAthleteCount: Long = 0L,
    @SerializedName("enrollments") val enrollments: Long = 0L,
    @SerializedName("publishedAt") val publishedAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("days") val days: List<GettingStartedDayResponseDto> = emptyList()
)

data class GettingStartedCampaignAnalyticsDto(
    @SerializedName("programId") val programId: Long,
    @SerializedName("programTitle") val programTitle: String,
    @SerializedName("published") val published: Boolean = false,
    @SerializedName("version") val version: Int = 1,
    @SerializedName("targetAudience") val targetAudience: String? = null,
    @SerializedName("audienceLabel") val audienceLabel: String? = null,
    @SerializedName("eligibleAthleteCount") val eligibleAthleteCount: Long = 0L,
    @SerializedName("modalImpressions") val modalImpressions: Long = 0L,
    @SerializedName("modalDismissals") val modalDismissals: Long = 0L,
    @SerializedName("programDetailViews") val programDetailViews: Long = 0L,
    @SerializedName("enrollments") val enrollments: Long = 0L,
    @SerializedName("day1Completions") val day1Completions: Long = 0L,
    @SerializedName("day2Completions") val day2Completions: Long = 0L,
    @SerializedName("day3Completions") val day3Completions: Long = 0L,
    @SerializedName("day1CompletionRate") val day1CompletionRate: Double = 0.0,
    @SerializedName("day2CompletionRate") val day2CompletionRate: Double = 0.0,
    @SerializedName("day3CompletionRate") val day3CompletionRate: Double = 0.0,
    @SerializedName("completedEnrollments") val completedEnrollments: Long = 0L,
    @SerializedName("overallCompletionRate") val overallCompletionRate: Double = 0.0
)

data class AdminPendingConfirmationDto(
    @SerializedName("confirmationToken") val confirmationToken: String,
    @SerializedName("actionType") val actionType: String,
    @SerializedName("status") val status: String,
    @SerializedName("programId") val programId: Long? = null,
    @SerializedName("programTitle") val programTitle: String? = null,
    @SerializedName("targetAudience") val targetAudience: String? = null,
    @SerializedName("audienceLabel") val audienceLabel: String? = null,
    @SerializedName("eligibleAthleteCount") val eligibleAthleteCount: Long = 0L,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("targetUserId") val targetUserId: Long? = null,
    @SerializedName("targetUserName") val targetUserName: String? = null,
    @SerializedName("targetUserEmail") val targetUserEmail: String? = null,
    @SerializedName("targetUserCurrentTier") val targetUserCurrentTier: String? = null,
    @SerializedName("targetUserMembershipSource") val targetUserMembershipSource: String? = null,
    @SerializedName("targetUserPaidPro") val targetUserPaidPro: Boolean? = null,
    @SerializedName("targetUserAdminGrantedPro") val targetUserAdminGrantedPro: Boolean? = null,
    @SerializedName("reason") val reason: String? = null
)

data class AdminConfirmationDecisionRequestDto(
    @SerializedName("approved") val approved: Boolean,
    @SerializedName("conversationId") val conversationId: Long? = null
)

data class AdminConfirmationDecisionResponseDto(
    @SerializedName("confirmationToken") val confirmationToken: String,
    @SerializedName("actionType") val actionType: String,
    @SerializedName("status") val status: String,
    @SerializedName("conversationId") val conversationId: Long? = null,
    @SerializedName("reply") val reply: String? = null,
    @SerializedName("execution") val execution: AdminToolExecutionRecordDto? = null,
    @SerializedName("gettingStartedProgram") val gettingStartedProgram: GettingStartedProgramResponseDto? = null,
    @SerializedName("activeCampaigns") val activeCampaigns: List<GettingStartedCampaignSummaryDto> = emptyList(),
    @SerializedName("updatedUser") val updatedUser: AdminUserDto? = null
)

data class AdminTimeRangeMetadataDto(
    @SerializedName("range") val range: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("periodDays") val periodDays: Long = 0L
)

data class AdminToolExecutionRecordDto(
    @SerializedName("toolName") val toolName: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("parametersSummary") val parametersSummary: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("recordCount") val recordCount: Long? = null,
    @SerializedName("durationMs") val durationMs: Long? = null,
    @SerializedName("executedAt") val executedAt: String? = null
)

data class AdminBucketCountDto(
    @SerializedName("label") val label: String = "",
    @SerializedName("count") val count: Long = 0L,
    @SerializedName("percentage") val percentage: Double = 0.0
)

data class AdminPagedResultDto<T>(
    @SerializedName("items") val items: List<T> = emptyList(),
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 20,
    @SerializedName("totalElements") val totalElements: Long = 0L,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("hasNext") val hasNext: Boolean = false,
    @SerializedName("hasPrevious") val hasPrevious: Boolean = false,
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList()
)

data class AdminUserSegmentFilterRequestDto(
    @SerializedName("searchQuery") val searchQuery: String? = null,
    @SerializedName("experienceLevel") val experienceLevel: String? = null,
    @SerializedName("fitnessGoal") val fitnessGoal: String? = null,
    @SerializedName("preferredFrequency") val preferredFrequency: String? = null,
    @SerializedName("activityStatus") val activityStatus: String? = null,
    @SerializedName("inactiveDaysThreshold") val inactiveDaysThreshold: Int? = null,
    @SerializedName("minCompletionRate") val minCompletionRate: Double? = null,
    @SerializedName("maxCompletionRate") val maxCompletionRate: Double? = null,
    @SerializedName("prProgressionStatus") val prProgressionStatus: String? = null,
    @SerializedName("stalePrDaysThreshold") val stalePrDaysThreshold: Int? = null,
    @SerializedName("coachRelationshipStatus") val coachRelationshipStatus: String? = null,
    @SerializedName("roleFilter") val roleFilter: String? = null,
    @SerializedName("skillKeyword") val skillKeyword: String? = null,
    @SerializedName("naturalLanguageQuery") val naturalLanguageQuery: String? = null,
    @SerializedName("includeEmail") val includeEmail: Boolean = false,
    @SerializedName("sortBy") val sortBy: String? = null,
    @SerializedName("range") val range: String? = "LAST_30_DAYS",
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 20
)

data class AdminGlobalUserSummaryDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("maskedEmail") val maskedEmail: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String = "ROLE_USER",
    @SerializedName("accountCategory") val accountCategory: String = "CLIENT",
    @SerializedName("experienceLevel") val experienceLevel: String? = null,
    @SerializedName("trainingExperience") val trainingExperience: String? = null,
    @SerializedName("primaryGoals") val primaryGoals: String? = null,
    @SerializedName("trainingInterests") val trainingInterests: String? = null,
    @SerializedName("preferredFrequency") val preferredFrequency: String? = null,
    @SerializedName("registeredAt") val registeredAt: String? = null,
    @SerializedName("lastActivityAt") val lastActivityAt: String? = null,
    @SerializedName("activityStatus") val activityStatus: String = "INACTIVE",
    @SerializedName("daysSinceLastWorkout") val daysSinceLastWorkout: Long? = null,
    @SerializedName("totalWorkouts") val totalWorkouts: Long = 0L,
    @SerializedName("completedWorkouts") val completedWorkouts: Long = 0L,
    @SerializedName("abandonedOrIncompleteWorkouts") val abandonedOrIncompleteWorkouts: Long = 0L,
    @SerializedName("workoutCompletionRate") val workoutCompletionRate: Double = 0.0,
    @SerializedName("totalPersonalRecords") val totalPersonalRecords: Long = 0L,
    @SerializedName("bestPrImprovementPercent") val bestPrImprovementPercent: Double? = null,
    @SerializedName("lastPrAchievedAt") val lastPrAchievedAt: String? = null,
    @SerializedName("consistencyTrend") val consistencyTrend: String? = null,
    @SerializedName("coachRelationshipStatus") val coachRelationshipStatus: String? = null,
    @SerializedName("coachCount") val coachCount: Long = 0L,
    @SerializedName("activeClientCount") val activeClientCount: Long = 0L,
    @SerializedName("aiConversationsCount") val aiConversationsCount: Long = 0L,
    @SerializedName("chatMessagesSentCount") val chatMessagesSentCount: Long = 0L
)

data class AdminGlobalUserStatisticsDto(
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("totalRegisteredUsers") val totalRegisteredUsers: Long = 0L,
    @SerializedName("totalRegularUsers") val totalRegularUsers: Long = 0L,
    @SerializedName("totalAdmins") val totalAdmins: Long = 0L,
    @SerializedName("totalCoaches") val totalCoaches: Long = 0L,
    @SerializedName("verifiedCoaches") val verifiedCoaches: Long = 0L,
    @SerializedName("totalClients") val totalClients: Long = 0L,
    @SerializedName("newRegistrationsInPeriod") val newRegistrationsInPeriod: Long = 0L,
    @SerializedName("activeUsersToday") val activeUsersToday: Long = 0L,
    @SerializedName("activeUsers7Days") val activeUsers7Days: Long = 0L,
    @SerializedName("activeUsers30Days") val activeUsers30Days: Long = 0L,
    @SerializedName("activeUsersInRange") val activeUsersInRange: Long = 0L,
    @SerializedName("inactiveUsers14PlusDays") val inactiveUsers14PlusDays: Long = 0L,
    @SerializedName("atRiskUsers7To14Days") val atRiskUsers7To14Days: Long = 0L,
    @SerializedName("usersWithActiveCoach") val usersWithActiveCoach: Long = 0L,
    @SerializedName("usersWithPendingCoachInvite") val usersWithPendingCoachInvite: Long = 0L,
    @SerializedName("usersWithoutCoach") val usersWithoutCoach: Long = 0L,
    @SerializedName("totalWorkoutsInRange") val totalWorkoutsInRange: Long = 0L,
    @SerializedName("completedWorkoutsInRange") val completedWorkoutsInRange: Long = 0L,
    @SerializedName("incompleteOrAbandonedWorkoutsInRange") val incompleteOrAbandonedWorkoutsInRange: Long = 0L,
    @SerializedName("overallWorkoutCompletionRate") val overallWorkoutCompletionRate: Double = 0.0,
    @SerializedName("totalPersonalRecordsAllTime") val totalPersonalRecordsAllTime: Long = 0L,
    @SerializedName("newPersonalRecordsInRange") val newPersonalRecordsInRange: Long = 0L,
    @SerializedName("usersWithStalePrs30PlusDays") val usersWithStalePrs30PlusDays: Long = 0L,
    @SerializedName("experienceLevelDistribution") val experienceLevelDistribution: List<AdminBucketCountDto> = emptyList(),
    @SerializedName("activityStatusDistribution") val activityStatusDistribution: List<AdminBucketCountDto> = emptyList(),
    @SerializedName("topFitnessGoals") val topFitnessGoals: List<AdminBucketCountDto> = emptyList(),
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList()
)

data class AdminUserDetailDossierDto(
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("userId") val userId: Long,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("maskedEmail") val maskedEmail: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String = "ROLE_USER",
    @SerializedName("accountCategory") val accountCategory: String = "CLIENT",
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("experienceLevel") val experienceLevel: String? = null,
    @SerializedName("trainingExperience") val trainingExperience: String? = null,
    @SerializedName("primaryGoals") val primaryGoals: String? = null,
    @SerializedName("trainingInterests") val trainingInterests: String? = null,
    @SerializedName("preferredFrequency") val preferredFrequency: String? = null,
    @SerializedName("coachingLanguage") val coachingLanguage: String? = null,
    @SerializedName("heightCm") val heightCm: Double? = null,
    @SerializedName("latestWeightKg") val latestWeightKg: Double? = null,
    @SerializedName("latestBodyFatPercentage") val latestBodyFatPercentage: Double? = null,
    @SerializedName("latestMeasurementAt") val latestMeasurementAt: String? = null,
    @SerializedName("inferredAvailableEquipment") val inferredAvailableEquipment: List<String> = emptyList(),
    @SerializedName("registeredAt") val registeredAt: String? = null,
    @SerializedName("coachProfile") val coachProfile: AdminCoachProfileSummaryDto? = null,
    @SerializedName("workoutSummary") val workoutSummary: AdminUserWorkoutHistoryDto? = null,
    @SerializedName("personalRecordsSummary") val personalRecordsSummary: AdminUserPersonalRecordsDto? = null,
    @SerializedName("progressSummary") val progressSummary: AdminUserProgressDto? = null,
    @SerializedName("activitySummary") val activitySummary: AdminUserActivityDto? = null,
    @SerializedName("coachRelationships") val coachRelationships: List<AdminCoachRelationshipItemDto> = emptyList(),
    @SerializedName("missingDataFields") val missingDataFields: List<String> = emptyList()
)

data class AdminCoachProfileSummaryDto(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("specialties") val specialties: String? = null,
    @SerializedName("yearsExperience") val yearsExperience: Int? = null,
    @SerializedName("languages") val languages: String? = null,
    @SerializedName("isPublic") val isPublic: Boolean? = null,
    @SerializedName("isAcceptingClients") val isAcceptingClients: Boolean? = null,
    @SerializedName("isVerified") val isVerified: Boolean? = null,
    @SerializedName("activeClientsCount") val activeClientsCount: Long = 0L,
    @SerializedName("pendingInvitationsCount") val pendingInvitationsCount: Long = 0L
)

data class AdminCoachRelationshipItemDto(
    @SerializedName("relationshipId") val relationshipId: Long,
    @SerializedName("counterpartRole") val counterpartRole: String = "",
    @SerializedName("counterpartUserId") val counterpartUserId: Long? = null,
    @SerializedName("counterpartName") val counterpartName: String? = null,
    @SerializedName("status") val status: String = "",
    @SerializedName("requestedByRole") val requestedByRole: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class AdminUserWorkoutHistoryDto(
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("userId") val userId: Long,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("totalSessions") val totalSessions: Long = 0L,
    @SerializedName("completedSessions") val completedSessions: Long = 0L,
    @SerializedName("inProgressSessions") val inProgressSessions: Long = 0L,
    @SerializedName("scheduledSessions") val scheduledSessions: Long = 0L,
    @SerializedName("cancelledSessions") val cancelledSessions: Long = 0L,
    @SerializedName("completionRate") val completionRate: Double = 0.0,
    @SerializedName("totalTrainingMinutes") val totalTrainingMinutes: Long = 0L,
    @SerializedName("averageDurationMinutes") val averageDurationMinutes: Double = 0.0,
    @SerializedName("averageSessionsPerWeek") val averageSessionsPerWeek: Double = 0.0,
    @SerializedName("consistencyTrend") val consistencyTrend: String? = null,
    @SerializedName("completedExercisesCount") val completedExercisesCount: Long = 0L,
    @SerializedName("pendingOrIncompleteExercisesCount") val pendingOrIncompleteExercisesCount: Long = 0L,
    @SerializedName("assignedPlans") val assignedPlans: List<AdminAssignedPlanItemDto> = emptyList(),
    @SerializedName("personalPlans") val personalPlans: List<AdminPersonalPlanItemDto> = emptyList(),
    @SerializedName("recentSessions") val recentSessions: List<AdminWorkoutSessionItemDto> = emptyList(),
    @SerializedName("abandonedOrIncompleteWorkouts") val abandonedOrIncompleteWorkouts: List<AdminWorkoutSessionItemDto> = emptyList(),
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList()
)

data class AdminAssignedPlanItemDto(
    @SerializedName("assignmentId") val assignmentId: Long,
    @SerializedName("workoutPlanId") val workoutPlanId: Long? = null,
    @SerializedName("planName") val planName: String? = null,
    @SerializedName("splitType") val splitType: String? = null,
    @SerializedName("coachName") val coachName: String? = null,
    @SerializedName("status") val status: String = "",
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class AdminPersonalPlanItemDto(
    @SerializedName("workoutPlanId") val workoutPlanId: Long,
    @SerializedName("name") val name: String = "",
    @SerializedName("splitType") val splitType: String? = null,
    @SerializedName("active") val active: Boolean = false,
    @SerializedName("aiGenerated") val aiGenerated: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class AdminWorkoutSessionItemDto(
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("title") val title: String? = null,
    @SerializedName("status") val status: String = "",
    @SerializedName("source") val source: String? = null,
    @SerializedName("workoutDate") val workoutDate: String? = null,
    @SerializedName("durationSeconds") val durationSeconds: Long? = null,
    @SerializedName("completedExercises") val completedExercises: Long = 0L,
    @SerializedName("totalExercises") val totalExercises: Long = 0L,
    @SerializedName("workoutPlanName") val workoutPlanName: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null
)

data class AdminUserPersonalRecordsDto(
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("userId") val userId: Long,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("totalRecords") val totalRecords: Long = 0L,
    @SerializedName("recordsInRange") val recordsInRange: Long = 0L,
    @SerializedName("averageImprovementPercent") val averageImprovementPercent: Double? = null,
    @SerializedName("maxImprovementPercent") val maxImprovementPercent: Double? = null,
    @SerializedName("lastPrAchievedAt") val lastPrAchievedAt: String? = null,
    @SerializedName("daysSinceLastPr") val daysSinceLastPr: Long? = null,
    @SerializedName("sharedPrCount") val sharedPrCount: Long = 0L,
    @SerializedName("coachFeedbackCount") val coachFeedbackCount: Long = 0L,
    @SerializedName("records") val records: List<AdminPersonalRecordItemDto> = emptyList(),
    @SerializedName("sharingAndFeedbackHistory") val sharingAndFeedbackHistory: List<AdminSharedPrItemDto> = emptyList(),
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList()
)

data class AdminPersonalRecordItemDto(
    @SerializedName("recordId") val recordId: Long,
    @SerializedName("exerciseName") val exerciseName: String? = null,
    @SerializedName("recordType") val recordType: String? = null,
    @SerializedName("recordValue") val recordValue: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("previousRecordValue") val previousRecordValue: Double? = null,
    @SerializedName("improvementPercentage") val improvementPercentage: Double? = null,
    @SerializedName("achievedAt") val achievedAt: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class AdminSharedPrItemDto(
    @SerializedName("sharedId") val sharedId: Long,
    @SerializedName("personalRecordId") val personalRecordId: Long? = null,
    @SerializedName("exerciseName") val exerciseName: String? = null,
    @SerializedName("coachUserId") val coachUserId: Long? = null,
    @SerializedName("coachName") val coachName: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("rating") val rating: Int? = null,
    @SerializedName("coachFeedback") val coachFeedback: String? = null,
    @SerializedName("sharedAt") val sharedAt: String? = null,
    @SerializedName("feedbackAt") val feedbackAt: String? = null
)

data class AdminUserProgressDto(
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("userId") val userId: Long,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("overallCompletionRate") val overallCompletionRate: Double = 0.0,
    @SerializedName("currentPeriodWorkouts") val currentPeriodWorkouts: Long = 0L,
    @SerializedName("previousPeriodWorkouts") val previousPeriodWorkouts: Long = 0L,
    @SerializedName("workoutVolumeDeltaPercent") val workoutVolumeDeltaPercent: Double? = null,
    @SerializedName("consistencyTrend") val consistencyTrend: String? = null,
    @SerializedName("weeklySessionCounts") val weeklySessionCounts: List<AdminPeriodPointDto> = emptyList(),
    @SerializedName("exercisePerformance") val exercisePerformance: List<AdminExercisePerformanceItemDto> = emptyList(),
    @SerializedName("strengthProgression") val strengthProgression: List<AdminStrengthProgressionItemDto> = emptyList(),
    @SerializedName("bodyMetricTrends") val bodyMetricTrends: List<AdminBodyMeasurementPointDto> = emptyList(),
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList()
)

data class AdminPeriodPointDto(
    @SerializedName("periodStart") val periodStart: String? = null,
    @SerializedName("periodEnd") val periodEnd: String? = null,
    @SerializedName("label") val label: String = "",
    @SerializedName("completedCount") val completedCount: Long = 0L,
    @SerializedName("totalCount") val totalCount: Long = 0L
)

data class AdminExercisePerformanceItemDto(
    @SerializedName("exerciseName") val exerciseName: String = "",
    @SerializedName("attempts") val attempts: Long = 0L,
    @SerializedName("completedCount") val completedCount: Long = 0L,
    @SerializedName("completionRate") val completionRate: Double = 0.0
)

data class AdminStrengthProgressionItemDto(
    @SerializedName("exerciseName") val exerciseName: String = "",
    @SerializedName("recordType") val recordType: String? = null,
    @SerializedName("latestValue") val latestValue: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("bestImprovementPercent") val bestImprovementPercent: Double? = null,
    @SerializedName("prCount") val prCount: Long = 0L,
    @SerializedName("lastAchievedAt") val lastAchievedAt: String? = null
)

data class AdminBodyMeasurementPointDto(
    @SerializedName("weightKg") val weightKg: Double? = null,
    @SerializedName("bodyFatPercentage") val bodyFatPercentage: Double? = null,
    @SerializedName("measuredAt") val measuredAt: String? = null
)

data class AdminUserActivityDto(
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("userId") val userId: Long,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("presenceStatus") val presenceStatus: String? = null,
    @SerializedName("lastSeenAt") val lastSeenAt: String? = null,
    @SerializedName("lastWorkoutAt") val lastWorkoutAt: String? = null,
    @SerializedName("lastChatMessageAt") val lastChatMessageAt: String? = null,
    @SerializedName("lastAiInteractionAt") val lastAiInteractionAt: String? = null,
    @SerializedName("lastVideoAnalysisAt") val lastVideoAnalysisAt: String? = null,
    @SerializedName("lastLiveAnalysisAt") val lastLiveAnalysisAt: String? = null,
    @SerializedName("chatConversationsCount") val chatConversationsCount: Long = 0L,
    @SerializedName("chatMessagesSentTotal") val chatMessagesSentTotal: Long = 0L,
    @SerializedName("chatMessagesSentInRange") val chatMessagesSentInRange: Long = 0L,
    @SerializedName("aiConversationsCount") val aiConversationsCount: Long = 0L,
    @SerializedName("aiMessagesTotal") val aiMessagesTotal: Long = 0L,
    @SerializedName("aiMessagesInRange") val aiMessagesInRange: Long = 0L,
    @SerializedName("workoutPlannerRunsTotal") val workoutPlannerRunsTotal: Long = 0L,
    @SerializedName("workoutPlannerRunsInRange") val workoutPlannerRunsInRange: Long = 0L,
    @SerializedName("workoutPlannerSuccessCount") val workoutPlannerSuccessCount: Long = 0L,
    @SerializedName("liveAnalysisSessionsInRange") val liveAnalysisSessionsInRange: Long = 0L,
    @SerializedName("videoAnalysesInRange") val videoAnalysesInRange: Long = 0L,
    @SerializedName("prRecordsLoggedInRange") val prRecordsLoggedInRange: Long = 0L,
    @SerializedName("topAiIntents") val topAiIntents: List<AdminBucketCountDto> = emptyList(),
    @SerializedName("privacyNotice") val privacyNotice: String? = null,
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList()
)

data class AdminCoachClientStatisticsDto(
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("totalCoaches") val totalCoaches: Long = 0L,
    @SerializedName("publicCoaches") val publicCoaches: Long = 0L,
    @SerializedName("acceptingClientsCoaches") val acceptingClientsCoaches: Long = 0L,
    @SerializedName("verifiedCoaches") val verifiedCoaches: Long = 0L,
    @SerializedName("totalRelationships") val totalRelationships: Long = 0L,
    @SerializedName("activeRelationships") val activeRelationships: Long = 0L,
    @SerializedName("pendingInvitations") val pendingInvitations: Long = 0L,
    @SerializedName("rejectedRelationships") val rejectedRelationships: Long = 0L,
    @SerializedName("cancelledRelationships") val cancelledRelationships: Long = 0L,
    @SerializedName("terminatedRelationships") val terminatedRelationships: Long = 0L,
    @SerializedName("clientsWithActiveCoach") val clientsWithActiveCoach: Long = 0L,
    @SerializedName("totalAssignedPlans") val totalAssignedPlans: Long = 0L,
    @SerializedName("activeAssignedPlans") val activeAssignedPlans: Long = 0L,
    @SerializedName("completedAssignedPlans") val completedAssignedPlans: Long = 0L,
    @SerializedName("totalSharedPrs") val totalSharedPrs: Long = 0L,
    @SerializedName("sharedPrsWithFeedback") val sharedPrsWithFeedback: Long = 0L,
    @SerializedName("feedbackResponseRate") val feedbackResponseRate: Double = 0.0,
    @SerializedName("topCoaches") val topCoaches: List<AdminCoachPerformanceItemDto> = emptyList(),
    @SerializedName("unassignedCoachCandidates") val unassignedCoachCandidates: List<AdminGlobalUserSummaryDto> = emptyList(),
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList()
)

data class AdminCoachPerformanceItemDto(
    @SerializedName("coachUserId") val coachUserId: Long,
    @SerializedName("coachName") val coachName: String? = null,
    @SerializedName("verified") val verified: Boolean = false,
    @SerializedName("acceptingClients") val acceptingClients: Boolean = false,
    @SerializedName("activeClients") val activeClients: Long = 0L,
    @SerializedName("pendingInvitations") val pendingInvitations: Long = 0L,
    @SerializedName("activeAssignedPlans") val activeAssignedPlans: Long = 0L,
    @SerializedName("sharedPrFeedbackCount") val sharedPrFeedbackCount: Long = 0L
)

data class AdminPlatformEngagementDto(
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("dauCount") val dauCount: Long = 0L,
    @SerializedName("wauCount") val wauCount: Long = 0L,
    @SerializedName("mauCount") val mauCount: Long = 0L,
    @SerializedName("stickinessDauToMauPercent") val stickinessDauToMauPercent: Double = 0.0,
    @SerializedName("experienceComparisons") val experienceComparisons: List<AdminExperienceComparisonItemDto> = emptyList(),
    @SerializedName("topPerformedExercises") val topPerformedExercises: List<AdminExerciseUsageItemDto> = emptyList(),
    @SerializedName("topSkillImprovers") val topSkillImprovers: List<AdminSkillImproverItemDto> = emptyList(),
    @SerializedName("decliningConsistencyUsers") val decliningConsistencyUsers: List<AdminGlobalUserSummaryDto> = emptyList(),
    @SerializedName("stalePrUsers") val stalePrUsers: List<AdminGlobalUserSummaryDto> = emptyList(),
    @SerializedName("chatAndAiUsageSummary") val chatAndAiUsageSummary: AdminChatAiUsageSummaryDto? = null,
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList()
)

data class AdminExperienceComparisonItemDto(
    @SerializedName("experienceLevel") val experienceLevel: String = "",
    @SerializedName("userCount") val userCount: Long = 0L,
    @SerializedName("activeUserCount") val activeUserCount: Long = 0L,
    @SerializedName("activeRatePercent") val activeRatePercent: Double = 0.0,
    @SerializedName("totalWorkouts") val totalWorkouts: Long = 0L,
    @SerializedName("completedWorkouts") val completedWorkouts: Long = 0L,
    @SerializedName("avgWorkoutsPerUser") val avgWorkoutsPerUser: Double = 0.0,
    @SerializedName("avgCompletionRate") val avgCompletionRate: Double = 0.0,
    @SerializedName("prCount") val prCount: Long = 0L,
    @SerializedName("avgAiInteractionsPerUser") val avgAiInteractionsPerUser: Double = 0.0
)

data class AdminExerciseUsageItemDto(
    @SerializedName("exerciseId") val exerciseId: Long,
    @SerializedName("exerciseName") val exerciseName: String = "",
    @SerializedName("category") val category: String? = null,
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName("totalPerformances") val totalPerformances: Long = 0L,
    @SerializedName("completedPerformances") val completedPerformances: Long = 0L,
    @SerializedName("completionRate") val completionRate: Double = 0.0,
    @SerializedName("uniqueUsersCount") val uniqueUsersCount: Long = 0L
)

data class AdminSkillImproverItemDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("experienceLevel") val experienceLevel: String? = null,
    @SerializedName("exerciseName") val exerciseName: String? = null,
    @SerializedName("recordType") val recordType: String? = null,
    @SerializedName("latestRecordValue") val latestRecordValue: Double? = null,
    @SerializedName("previousRecordValue") val previousRecordValue: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("improvementPercent") val improvementPercent: Double? = null,
    @SerializedName("achievedAt") val achievedAt: String? = null
)

data class AdminChatAiUsageSummaryDto(
    @SerializedName("totalChatMessagesInRange") val totalChatMessagesInRange: Long = 0L,
    @SerializedName("activeChatUsersInRange") val activeChatUsersInRange: Long = 0L,
    @SerializedName("totalAiMessagesInRange") val totalAiMessagesInRange: Long = 0L,
    @SerializedName("activeAiUsersInRange") val activeAiUsersInRange: Long = 0L,
    @SerializedName("workoutPlannerRunsInRange") val workoutPlannerRunsInRange: Long = 0L,
    @SerializedName("workoutPlannerSuccessRatePercent") val workoutPlannerSuccessRatePercent: Double = 0.0,
    @SerializedName("prRecordsInRange") val prRecordsInRange: Long = 0L,
    @SerializedName("liveAnalysisSessionsInRange") val liveAnalysisSessionsInRange: Long = 0L,
    @SerializedName("videoAnalysesInRange") val videoAnalysesInRange: Long = 0L,
    @SerializedName("topAiIntents") val topAiIntents: List<AdminBucketCountDto> = emptyList()
)

data class AdminUserSegmentsDto(
    @SerializedName("timeRange") val timeRange: AdminTimeRangeMetadataDto? = null,
    @SerializedName("segmentSummary") val segmentSummary: String? = null,
    @SerializedName("matchedCount") val matchedCount: Long = 0L,
    @SerializedName("returnedCount") val returnedCount: Int = 0,
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 20,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("users") val users: List<AdminGlobalUserSummaryDto> = emptyList(),
    @SerializedName("experienceBreakdown") val experienceBreakdown: List<AdminBucketCountDto> = emptyList(),
    @SerializedName("activityBreakdown") val activityBreakdown: List<AdminBucketCountDto> = emptyList(),
    @SerializedName("goalBreakdown") val goalBreakdown: List<AdminBucketCountDto> = emptyList(),
    @SerializedName("coachStatusBreakdown") val coachStatusBreakdown: List<AdminBucketCountDto> = emptyList(),
    @SerializedName("missingDataNotes") val missingDataNotes: List<String> = emptyList()
)

data class AdminPlatformOverviewDto(
    @SerializedName("range") val range: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("totalRegisteredUsers") val totalRegisteredUsers: Long = 0L,
    @SerializedName("activeUsersInPeriod") val activeUsersInPeriod: Long = 0L,
    @SerializedName("newRegistrationsInPeriod") val newRegistrationsInPeriod: Long = 0L,
    @SerializedName("totalCoaches") val totalCoaches: Long = 0L,
    @SerializedName("totalClients") val totalClients: Long = 0L,
    @SerializedName("totalAdmins") val totalAdmins: Long = 0L,
    @SerializedName("activeCoachClientRelationships") val activeCoachClientRelationships: Long = 0L,
    @SerializedName("pendingCoachClientRelationships") val pendingCoachClientRelationships: Long = 0L,
    @SerializedName("totalWorkoutPlans") val totalWorkoutPlans: Long = 0L,
    @SerializedName("activeWorkoutPlans") val activeWorkoutPlans: Long = 0L,
    @SerializedName("aiGeneratedWorkoutPlans") val aiGeneratedWorkoutPlans: Long = 0L,
    @SerializedName("totalCompletedWorkoutsAllTime") val totalCompletedWorkoutsAllTime: Long = 0L,
    @SerializedName("completedWorkoutsInPeriod") val completedWorkoutsInPeriod: Long = 0L,
    @SerializedName("scheduledOrInProgressWorkoutsInPeriod") val scheduledOrInProgressWorkoutsInPeriod: Long = 0L,
    @SerializedName("totalRecordedPrsAllTime") val totalRecordedPrsAllTime: Long = 0L,
    @SerializedName("recordedPrsInPeriod") val recordedPrsInPeriod: Long = 0L,
    @SerializedName("totalExercises") val totalExercises: Long = 0L,
    @SerializedName("activeExercises") val activeExercises: Long = 0L,
    @SerializedName("engagementMetrics") val engagementMetrics: AdminEngagementMetricsDto? = null,
    @SerializedName("activityTrends") val activityTrends: AdminActivityTrendSummaryDto? = null
)

data class AdminEngagementMetricsDto(
    @SerializedName("activeUserRatePercent") val activeUserRatePercent: Double = 0.0,
    @SerializedName("workoutCompletionRatePercent") val workoutCompletionRatePercent: Double = 0.0,
    @SerializedName("avgWorkoutsPerActiveUser") val avgWorkoutsPerActiveUser: Double = 0.0,
    @SerializedName("usersWithActivePlanCount") val usersWithActivePlanCount: Long = 0L
)

data class AdminActivityTrendSummaryDto(
    @SerializedName("previousPeriodCompletedWorkouts") val previousPeriodCompletedWorkouts: Long = 0L,
    @SerializedName("previousPeriodNewUsers") val previousPeriodNewUsers: Long = 0L,
    @SerializedName("previousPeriodPrs") val previousPeriodPrs: Long = 0L,
    @SerializedName("workoutGrowthPercent") val workoutGrowthPercent: Double = 0.0,
    @SerializedName("userGrowthPercent") val userGrowthPercent: Double = 0.0
)

data class AdminTrainingAnalyticsDto(
    @SerializedName("range") val range: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("mostPerformedExercises") val mostPerformedExercises: List<AdminExerciseUsageStatDto> = emptyList(),
    @SerializedName("highestCompletionRateExercises") val highestCompletionRateExercises: List<AdminExerciseUsageStatDto> = emptyList(),
    @SerializedName("skillFocusBreakdown") val skillFocusBreakdown: List<AdminSkillFocusStatDto> = emptyList(),
    @SerializedName("commonTrainingGoals") val commonTrainingGoals: List<AdminGoalStatDto> = emptyList(),
    @SerializedName("inactiveUsersSummary") val inactiveUsersSummary: AdminInactiveUsersSummaryDto? = null,
    @SerializedName("planAdherenceSummary") val planAdherenceSummary: AdminPlanAdherenceSummaryDto? = null,
    @SerializedName("topEngagedPrograms") val topEngagedPrograms: List<AdminProgramEngagementStatDto> = emptyList(),
    @SerializedName("exerciseLibraryByCategory") val exerciseLibraryByCategory: Map<String, Long> = emptyMap(),
    @SerializedName("exerciseLibraryByDifficulty") val exerciseLibraryByDifficulty: Map<String, Long> = emptyMap()
)

data class AdminExerciseUsageStatDto(
    @SerializedName("exerciseId") val exerciseId: Long = 0L,
    @SerializedName("exerciseName") val exerciseName: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("difficulty") val difficulty: String = "",
    @SerializedName("sessionPerformances") val sessionPerformances: Long = 0L,
    @SerializedName("completedPerformances") val completedPerformances: Long = 0L,
    @SerializedName("planAssignmentsCount") val planAssignmentsCount: Long = 0L,
    @SerializedName("prCount") val prCount: Long = 0L,
    @SerializedName("completionRatePercent") val completionRatePercent: Double = 0.0
)

data class AdminSkillFocusStatDto(
    @SerializedName("skillName") val skillName: String = "",
    @SerializedName("existingExerciseCount") val existingExerciseCount: Long = 0L,
    @SerializedName("planReferencesCount") val planReferencesCount: Long = 0L,
    @SerializedName("sessionPerformancesCount") val sessionPerformancesCount: Long = 0L,
    @SerializedName("prCount") val prCount: Long = 0L,
    @SerializedName("interestedUsersCount") val interestedUsersCount: Long = 0L,
    @SerializedName("missingProgressions") val missingProgressions: List<String> = emptyList()
)

data class AdminGoalStatDto(
    @SerializedName("goal") val goal: String = "",
    @SerializedName("userCount") val userCount: Long = 0L,
    @SerializedName("percentage") val percentage: Double = 0.0
)

data class AdminInactiveUsersSummaryDto(
    @SerializedName("totalUsersEvaluated") val totalUsersEvaluated: Long = 0L,
    @SerializedName("inactiveOver14DaysCount") val inactiveOver14DaysCount: Long = 0L,
    @SerializedName("inactiveOver30DaysCount") val inactiveOver30DaysCount: Long = 0L,
    @SerializedName("neverCompletedWorkoutCount") val neverCompletedWorkoutCount: Long = 0L,
    @SerializedName("inactiveRatePercent") val inactiveRatePercent: Double = 0.0
)

data class AdminPlanAdherenceSummaryDto(
    @SerializedName("usersWithActivePersonalPlan") val usersWithActivePersonalPlan: Long = 0L,
    @SerializedName("activeCoachAssignmentsCount") val activeCoachAssignmentsCount: Long = 0L,
    @SerializedName("completedAssignedSessions") val completedAssignedSessions: Long = 0L,
    @SerializedName("totalAssignedSessions") val totalAssignedSessions: Long = 0L,
    @SerializedName("adherenceRatePercent") val adherenceRatePercent: Double = 0.0
)

data class AdminProgramEngagementStatDto(
    @SerializedName("planId") val planId: Long = 0L,
    @SerializedName("planName") val planName: String = "",
    @SerializedName("splitType") val splitType: String = "",
    @SerializedName("ownerName") val ownerName: String = "",
    @SerializedName("aiGenerated") val aiGenerated: Boolean = false,
    @SerializedName("completedSessionsCount") val completedSessionsCount: Long = 0L,
    @SerializedName("totalSessionsCount") val totalSessionsCount: Long = 0L,
    @SerializedName("completionRatePercent") val completionRatePercent: Double = 0.0
)

data class AdminAiRecommendationDto(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: String,
    @SerializedName("priority") val priority: String = "MEDIUM",
    @SerializedName("title") val title: String,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("rationale") val rationale: String? = null,
    @SerializedName("exerciseDetails") val exerciseDetails: ExerciseRecommendationPayloadDto? = null,
    @SerializedName("programDetails") val programDetails: WorkoutProgramRecommendationPayloadDto? = null,
    @SerializedName("operationalDetails") val operationalDetails: OperationalRecommendationPayloadDto? = null,
    @SerializedName("conversationId") val conversationId: Long? = null,
    @SerializedName("createdResourceId") val createdResourceId: Long? = null,
    @SerializedName("createdResourceType") val createdResourceType: String? = null,
    @SerializedName("reviewNotes") val reviewNotes: String? = null,
    @SerializedName("reviewedAt") val reviewedAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class ExerciseRecommendationPayloadDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("code") val code: String? = null,
    @SerializedName("category") val category: String = "SKILL",
    @SerializedName("difficulty") val difficulty: String = "INTERMEDIATE",
    @SerializedName("targetSkill") val targetSkill: String? = null,
    @SerializedName("progressionLevel") val progressionLevel: String? = null,
    @SerializedName("prerequisites") val prerequisites: String? = null,
    @SerializedName("targetMuscles") val targetMuscles: String? = null,
    @SerializedName("equipment") val equipment: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("instructions") val instructions: String? = null,
    @SerializedName("whyAdd") val whyAdd: String? = null
)

data class WorkoutProgramRecommendationPayloadDto(
    @SerializedName("programName") val programName: String = "",
    @SerializedName("targetAudience") val targetAudience: String? = null,
    @SerializedName("trainingLevel") val trainingLevel: String? = null,
    @SerializedName("weeklyFrequency") val weeklyFrequency: Int = 3,
    @SerializedName("durationWeeks") val durationWeeks: Int = 8,
    @SerializedName("splitType") val splitType: String? = null,
    @SerializedName("progressionCriteria") val progressionCriteria: String? = null,
    @SerializedName("requiredEquipment") val requiredEquipment: String? = null,
    @SerializedName("safetyConsiderations") val safetyConsiderations: String? = null,
    @SerializedName("rationale") val rationale: String? = null,
    @SerializedName("days") val days: List<ProgramDayPayloadDto> = emptyList()
)

data class ProgramDayPayloadDto(
    @SerializedName("dayOfWeek") val dayOfWeek: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("restDay") val restDay: Boolean = false,
    @SerializedName("exercises") val exercises: List<ProgramExercisePayloadDto> = emptyList()
)

data class ProgramExercisePayloadDto(
    @SerializedName("exerciseId") val exerciseId: Long? = null,
    @SerializedName("exerciseName") val exerciseName: String = "",
    @SerializedName("sets") val sets: Int = 3,
    @SerializedName("reps") val reps: Int? = null,
    @SerializedName("holdSeconds") val holdSeconds: Int? = null,
    @SerializedName("restSeconds") val restSeconds: Int = 90,
    @SerializedName("notes") val notes: String? = null
)

data class OperationalRecommendationPayloadDto(
    @SerializedName("identifiedIssue") val identifiedIssue: String = "",
    @SerializedName("supportingData") val supportingData: String? = null,
    @SerializedName("suggestedAction") val suggestedAction: String? = null,
    @SerializedName("expectedBenefit") val expectedBenefit: String? = null,
    @SerializedName("affectedArea") val affectedArea: String? = null
)

data class CreateAdminRecommendationRequestDto(
    @SerializedName("type") val type: String,
    @SerializedName("targetSkill") val targetSkill: String? = null,
    @SerializedName("trainingLevel") val trainingLevel: String? = null,
    @SerializedName("prompt") val prompt: String? = null
)

data class ReviewAdminRecommendationRequestDto(
    @SerializedName("reviewNotes") val reviewNotes: String? = null,
    @SerializedName("publishExercise") val publishExercise: Boolean? = null
)
