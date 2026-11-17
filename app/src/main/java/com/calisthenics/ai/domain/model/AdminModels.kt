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
    val createdAt: String?,
    val gender: Gender? = null,
    val membershipTier: String = if (role == "ROLE_ADMIN") "ADMIN" else "FREE",
    val membershipSource: String = if (role == "ROLE_ADMIN") "ADMIN Role" else "Default",
    val unlimited: Boolean = role == "ROLE_ADMIN",
    val paidPro: Boolean = false,
    val adminGrantedPro: Boolean = false,
    val adminProStatus: String = "NONE",
    val adminProGrantSource: String? = null,
    val grantedByAdminId: Long? = null,
    val adminProGrantedAt: String? = null,
    val revokedByAdminId: Long? = null,
    val adminProRevokedAt: String? = null,
    val adminProReason: String? = null
) {
    val isAdminRole: Boolean
        get() = role.equals("ROLE_ADMIN", ignoreCase = true)

    val isProActive: Boolean
        get() = isAdminRole || adminGrantedPro || paidPro || membershipTier.equals("PRO", ignoreCase = true)

    val subscriptionStatusLabel: String
        get() = when {
            isAdminRole && adminGrantedPro -> "ADMIN (Unlimited) • Admin Grant"
            isAdminRole -> "ADMIN (Unlimited)"
            adminGrantedPro && paidPro -> "PRO • Unlimited (Admin Grant + Paid Sub)"
            adminGrantedPro -> "PRO • Unlimited (Admin Grant)"
            paidPro || membershipTier.equals("PRO", ignoreCase = true) -> "PRO • Paid Subscription"
            else -> "FREE"
        }
}

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

// Admin AI Agent Domain Models

data class AdminAiConversationSummary(
    val id: Long,
    val title: String,
    val lastMessagePreview: String?,
    val lastMessageRole: String?,
    val lastInteractionAt: String?,
    val createdAt: String?,
    val messageCount: Int
)

data class AdminAiMessage(
    val id: Long,
    val conversationId: Long,
    val role: String,
    val content: String,
    val intent: String?,
    val createdAt: String?
)

data class AdminAiChatResponse(
    val conversationId: Long,
    val conversationTitle: String?,
    val userMessage: AdminAiMessage?,
    val assistantMessage: AdminAiMessage,
    val intent: String?,
    val recommendations: List<AdminAiRecommendation>,
    val platformOverview: AdminPlatformOverview?,
    val trainingAnalytics: AdminTrainingAnalytics?,
    val suggestedFollowUps: List<String>,
    val executedTools: List<AdminToolExecution> = emptyList(),
    val timeRangeUsed: AdminTimeRange? = null,
    val userStatistics: AdminGlobalUserStatistics? = null,
    val matchedUsers: List<AdminGlobalUserSummary> = emptyList(),
    val inspectedUserDetail: AdminUserDetailDossier? = null,
    val segmentAnalysis: AdminUserSegments? = null,
    val platformEngagement: AdminPlatformEngagement? = null,
    val coachClientStatistics: AdminCoachClientStatistics? = null,
    val gettingStartedProgram: GettingStartedProgram? = null,
    val gettingStartedPreview: GettingStartedProgramPreview? = null,
    val activeCampaigns: List<GettingStartedCampaignSummary> = emptyList(),
    val campaignAnalytics: GettingStartedCampaignAnalytics? = null,
    val pendingConfirmation: AdminPendingConfirmation? = null,
    val missingDataNotes: List<String> = emptyList(),
    val updatedProUser: AdminUser? = null,
    val proCandidates: List<AdminUser> = emptyList()
)

data class GettingStartedProgramPreview(
    val program: GettingStartedProgram?,
    val valid: Boolean,
    val validationErrors: List<String>,
    val targetAudience: String?,
    val audienceLabel: String?,
    val eligibleAthleteCount: Long,
    val totalAthleteCount: Long,
    val estimatedAudienceSize: Long
)

data class GettingStartedCampaignSummary(
    val programId: Long,
    val title: String,
    val description: String?,
    val published: Boolean,
    val version: Int,
    val targetAudience: String?,
    val audienceLabel: String?,
    val eligibleAthleteCount: Long,
    val enrollments: Long,
    val publishedAt: String?,
    val updatedAt: String?,
    val days: List<GettingStartedDay>
)

data class GettingStartedCampaignAnalytics(
    val programId: Long,
    val programTitle: String,
    val published: Boolean,
    val version: Int,
    val targetAudience: String?,
    val audienceLabel: String?,
    val eligibleAthleteCount: Long,
    val modalImpressions: Long,
    val modalDismissals: Long,
    val programDetailViews: Long,
    val enrollments: Long,
    val day1Completions: Long,
    val day2Completions: Long,
    val day3Completions: Long,
    val day1CompletionRate: Double,
    val day2CompletionRate: Double,
    val day3CompletionRate: Double,
    val completedEnrollments: Long,
    val overallCompletionRate: Double
)

data class AdminPendingConfirmation(
    val confirmationToken: String,
    val actionType: String,
    val status: String,
    val programId: Long? = null,
    val programTitle: String? = null,
    val targetAudience: String? = null,
    val audienceLabel: String? = null,
    val eligibleAthleteCount: Long = 0L,
    val summary: String? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val targetUserId: Long? = null,
    val targetUserName: String? = null,
    val targetUserEmail: String? = null,
    val targetUserCurrentTier: String? = null,
    val targetUserMembershipSource: String? = null,
    val targetUserPaidPro: Boolean? = null,
    val targetUserAdminGrantedPro: Boolean? = null,
    val reason: String? = null
) {
    val isProAction: Boolean
        get() = actionType.equals("GRANT_PRO", ignoreCase = true) ||
            actionType.equals("REVOKE_PRO", ignoreCase = true)
}

data class AdminConfirmationDecision(
    val confirmationToken: String,
    val actionType: String,
    val status: String,
    val conversationId: Long?,
    val reply: String?,
    val execution: AdminToolExecution?,
    val gettingStartedProgram: GettingStartedProgram?,
    val activeCampaigns: List<GettingStartedCampaignSummary>,
    val updatedUser: AdminUser? = null
)

data class AdminTimeRange(
    val range: String,
    val startDate: String?,
    val endDate: String?,
    val periodDays: Long
)

data class AdminToolExecution(
    val toolName: String,
    val status: String,
    val parametersSummary: String?,
    val summary: String?,
    val recordCount: Long?,
    val durationMs: Long?,
    val executedAt: String?
)

data class AdminBucketCount(
    val label: String,
    val count: Long,
    val percentage: Double
)

data class AdminGlobalUserSummary(
    val userId: Long,
    val fullName: String,
    val displayName: String?,
    val maskedEmail: String?,
    val email: String?,
    val role: String,
    val accountCategory: String,
    val experienceLevel: String?,
    val trainingExperience: String?,
    val primaryGoals: String?,
    val trainingInterests: String?,
    val preferredFrequency: String?,
    val registeredAt: String?,
    val lastActivityAt: String?,
    val activityStatus: String,
    val daysSinceLastWorkout: Long?,
    val totalWorkouts: Long,
    val completedWorkouts: Long,
    val abandonedOrIncompleteWorkouts: Long,
    val workoutCompletionRate: Double,
    val totalPersonalRecords: Long,
    val bestPrImprovementPercent: Double?,
    val lastPrAchievedAt: String?,
    val consistencyTrend: String?,
    val coachRelationshipStatus: String?,
    val coachCount: Long,
    val activeClientCount: Long,
    val aiConversationsCount: Long,
    val chatMessagesSentCount: Long
)

data class AdminGlobalUserStatistics(
    val timeRange: AdminTimeRange?,
    val totalRegisteredUsers: Long,
    val totalRegularUsers: Long,
    val totalAdmins: Long,
    val totalCoaches: Long,
    val verifiedCoaches: Long,
    val totalClients: Long,
    val newRegistrationsInPeriod: Long,
    val activeUsersToday: Long,
    val activeUsers7Days: Long,
    val activeUsers30Days: Long,
    val activeUsersInRange: Long,
    val inactiveUsers14PlusDays: Long,
    val atRiskUsers7To14Days: Long,
    val usersWithActiveCoach: Long,
    val usersWithPendingCoachInvite: Long,
    val usersWithoutCoach: Long,
    val totalWorkoutsInRange: Long,
    val completedWorkoutsInRange: Long,
    val incompleteOrAbandonedWorkoutsInRange: Long,
    val overallWorkoutCompletionRate: Double,
    val totalPersonalRecordsAllTime: Long,
    val newPersonalRecordsInRange: Long,
    val usersWithStalePrs30PlusDays: Long,
    val experienceLevelDistribution: List<AdminBucketCount>,
    val activityStatusDistribution: List<AdminBucketCount>,
    val topFitnessGoals: List<AdminBucketCount>,
    val missingDataNotes: List<String>
)

data class AdminUserDetailDossier(
    val timeRange: AdminTimeRange?,
    val userId: Long,
    val fullName: String,
    val displayName: String?,
    val maskedEmail: String?,
    val email: String?,
    val role: String,
    val accountCategory: String,
    val gender: String?,
    val bio: String?,
    val experienceLevel: String?,
    val trainingExperience: String?,
    val primaryGoals: String?,
    val trainingInterests: String?,
    val preferredFrequency: String?,
    val coachingLanguage: String?,
    val heightCm: Double?,
    val latestWeightKg: Double?,
    val latestBodyFatPercentage: Double?,
    val latestMeasurementAt: String?,
    val inferredAvailableEquipment: List<String>,
    val registeredAt: String?,
    val coachProfile: AdminCoachProfileSummary?,
    val workoutSummary: AdminUserWorkoutHistory?,
    val personalRecordsSummary: AdminUserPersonalRecords?,
    val progressSummary: AdminUserProgress?,
    val activitySummary: AdminUserActivity?,
    val coachRelationships: List<AdminCoachRelationshipItem>,
    val missingDataFields: List<String>
)

data class AdminCoachProfileSummary(
    val displayName: String?,
    val specialties: String?,
    val yearsExperience: Int?,
    val languages: String?,
    val isPublic: Boolean?,
    val isAcceptingClients: Boolean?,
    val isVerified: Boolean?,
    val activeClientsCount: Long,
    val pendingInvitationsCount: Long
)

data class AdminCoachRelationshipItem(
    val relationshipId: Long,
    val counterpartRole: String,
    val counterpartUserId: Long?,
    val counterpartName: String?,
    val status: String,
    val requestedByRole: String?,
    val createdAt: String?
)

data class AdminUserWorkoutHistory(
    val totalSessions: Long,
    val completedSessions: Long,
    val inProgressSessions: Long,
    val scheduledSessions: Long,
    val cancelledSessions: Long,
    val completionRate: Double,
    val totalTrainingMinutes: Long,
    val averageDurationMinutes: Double,
    val averageSessionsPerWeek: Double,
    val consistencyTrend: String?,
    val completedExercisesCount: Long,
    val pendingOrIncompleteExercisesCount: Long,
    val assignedPlanNames: List<String>,
    val personalPlanNames: List<String>,
    val recentSessionLabels: List<String>,
    val abandonedWorkoutLabels: List<String>,
    val missingDataNotes: List<String>
)

data class AdminUserPersonalRecords(
    val totalRecords: Long,
    val recordsInRange: Long,
    val averageImprovementPercent: Double?,
    val maxImprovementPercent: Double?,
    val lastPrAchievedAt: String?,
    val daysSinceLastPr: Long?,
    val sharedPrCount: Long,
    val coachFeedbackCount: Long,
    val recordSummaries: List<String>,
    val feedbackSummaries: List<String>,
    val missingDataNotes: List<String>
)

data class AdminUserProgress(
    val overallCompletionRate: Double,
    val currentPeriodWorkouts: Long,
    val previousPeriodWorkouts: Long,
    val workoutVolumeDeltaPercent: Double?,
    val consistencyTrend: String?,
    val topExercises: List<String>,
    val strengthProgressionSummaries: List<String>,
    val bodyMetricSummaries: List<String>,
    val missingDataNotes: List<String>
)

data class AdminUserActivity(
    val presenceStatus: String?,
    val lastSeenAt: String?,
    val lastWorkoutAt: String?,
    val lastChatMessageAt: String?,
    val lastAiInteractionAt: String?,
    val chatConversationsCount: Long,
    val chatMessagesSentInRange: Long,
    val aiConversationsCount: Long,
    val aiMessagesInRange: Long,
    val workoutPlannerRunsInRange: Long,
    val liveAnalysisSessionsInRange: Long,
    val videoAnalysesInRange: Long,
    val prRecordsLoggedInRange: Long,
    val topAiIntents: List<AdminBucketCount>,
    val privacyNotice: String?,
    val missingDataNotes: List<String>
)

data class AdminCoachClientStatistics(
    val timeRange: AdminTimeRange?,
    val totalCoaches: Long,
    val verifiedCoaches: Long,
    val totalRelationships: Long,
    val activeRelationships: Long,
    val pendingInvitations: Long,
    val clientsWithActiveCoach: Long,
    val totalAssignedPlans: Long,
    val activeAssignedPlans: Long,
    val totalSharedPrs: Long,
    val feedbackResponseRate: Double,
    val topCoachNames: List<String>,
    val unassignedCoachCandidates: List<AdminGlobalUserSummary>,
    val missingDataNotes: List<String>
)

data class AdminPlatformEngagement(
    val timeRange: AdminTimeRange?,
    val dauCount: Long,
    val wauCount: Long,
    val mauCount: Long,
    val stickinessDauToMauPercent: Double,
    val experienceComparisons: List<AdminExperienceComparison>,
    val topSkillImprovers: List<AdminSkillImprover>,
    val decliningConsistencyUsers: List<AdminGlobalUserSummary>,
    val stalePrUsers: List<AdminGlobalUserSummary>,
    val totalChatMessagesInRange: Long,
    val totalAiMessagesInRange: Long,
    val workoutPlannerRunsInRange: Long,
    val missingDataNotes: List<String>
)

data class AdminExperienceComparison(
    val experienceLevel: String,
    val userCount: Long,
    val activeUserCount: Long,
    val activeRatePercent: Double,
    val avgWorkoutsPerUser: Double,
    val avgCompletionRate: Double,
    val prCount: Long,
    val avgAiInteractionsPerUser: Double
)

data class AdminSkillImprover(
    val userId: Long,
    val fullName: String,
    val experienceLevel: String?,
    val exerciseName: String,
    val recordType: String?,
    val latestRecordValue: Double?,
    val unit: String?,
    val improvementPercent: Double?
)

data class AdminUserSegments(
    val timeRange: AdminTimeRange?,
    val segmentSummary: String?,
    val matchedCount: Long,
    val returnedCount: Int,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val users: List<AdminGlobalUserSummary>,
    val experienceBreakdown: List<AdminBucketCount>,
    val activityBreakdown: List<AdminBucketCount>,
    val goalBreakdown: List<AdminBucketCount>,
    val coachStatusBreakdown: List<AdminBucketCount>,
    val missingDataNotes: List<String>
)

data class AdminUserSegmentFilter(
    val searchQuery: String? = null,
    val experienceLevel: String? = null,
    val fitnessGoal: String? = null,
    val activityStatus: String? = null,
    val inactiveDaysThreshold: Int? = null,
    val maxCompletionRate: Double? = null,
    val coachRelationshipStatus: String? = null,
    val roleFilter: String? = null,
    val skillKeyword: String? = null,
    val naturalLanguageQuery: String? = null,
    val includeEmail: Boolean = false,
    val sortBy: String? = null,
    val range: String = "LAST_30_DAYS",
    val startDate: String? = null,
    val endDate: String? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class AdminPlatformOverview(
    val range: String,
    val startDate: String?,
    val endDate: String?,
    val totalRegisteredUsers: Long,
    val activeUsersInPeriod: Long,
    val newRegistrationsInPeriod: Long,
    val totalCoaches: Long,
    val totalClients: Long,
    val totalAdmins: Long,
    val activeCoachClientRelationships: Long,
    val pendingCoachClientRelationships: Long,
    val totalWorkoutPlans: Long,
    val activeWorkoutPlans: Long,
    val aiGeneratedWorkoutPlans: Long,
    val totalCompletedWorkoutsAllTime: Long,
    val completedWorkoutsInPeriod: Long,
    val scheduledOrInProgressWorkoutsInPeriod: Long,
    val totalRecordedPrsAllTime: Long,
    val recordedPrsInPeriod: Long,
    val totalExercises: Long,
    val activeExercises: Long,
    val activeUserRatePercent: Double,
    val workoutCompletionRatePercent: Double,
    val avgWorkoutsPerActiveUser: Double,
    val usersWithActivePlanCount: Long,
    val workoutGrowthPercent: Double,
    val userGrowthPercent: Double
)

data class AdminTrainingAnalytics(
    val range: String,
    val startDate: String?,
    val endDate: String?,
    val mostPerformedExercises: List<AdminExerciseUsageStat>,
    val highestCompletionRateExercises: List<AdminExerciseUsageStat>,
    val skillFocusBreakdown: List<AdminSkillFocusStat>,
    val commonTrainingGoals: List<AdminGoalStat>,
    val inactiveUsersSummary: AdminInactiveUsersSummary?,
    val planAdherenceSummary: AdminPlanAdherenceSummary?,
    val topEngagedPrograms: List<AdminProgramEngagementStat>,
    val exerciseLibraryByCategory: Map<String, Long>,
    val exerciseLibraryByDifficulty: Map<String, Long>
)

data class AdminExerciseUsageStat(
    val exerciseId: Long,
    val exerciseName: String,
    val category: String,
    val difficulty: String,
    val sessionPerformances: Long,
    val completedPerformances: Long,
    val planAssignmentsCount: Long,
    val prCount: Long,
    val completionRatePercent: Double
)

data class AdminSkillFocusStat(
    val skillName: String,
    val existingExerciseCount: Long,
    val planReferencesCount: Long,
    val sessionPerformancesCount: Long,
    val prCount: Long,
    val interestedUsersCount: Long,
    val missingProgressions: List<String>
)

data class AdminGoalStat(
    val goal: String,
    val userCount: Long,
    val percentage: Double
)

data class AdminInactiveUsersSummary(
    val totalUsersEvaluated: Long,
    val inactiveOver14DaysCount: Long,
    val inactiveOver30DaysCount: Long,
    val neverCompletedWorkoutCount: Long,
    val inactiveRatePercent: Double
)

data class AdminPlanAdherenceSummary(
    val usersWithActivePersonalPlan: Long,
    val activeCoachAssignmentsCount: Long,
    val completedAssignedSessions: Long,
    val totalAssignedSessions: Long,
    val adherenceRatePercent: Double
)

data class AdminProgramEngagementStat(
    val planId: Long,
    val planName: String,
    val splitType: String,
    val ownerName: String,
    val aiGenerated: Boolean,
    val completedSessionsCount: Long,
    val totalSessionsCount: Long,
    val completionRatePercent: Double
)

data class AdminAiRecommendation(
    val id: Long,
    val type: String,
    val status: String,
    val priority: String,
    val title: String,
    val summary: String?,
    val rationale: String?,
    val exerciseDetails: ExerciseRecommendationDetails?,
    val programDetails: WorkoutProgramRecommendationDetails?,
    val operationalDetails: OperationalRecommendationDetails?,
    val conversationId: Long?,
    val createdResourceId: Long?,
    val createdResourceType: String?,
    val reviewNotes: String?,
    val reviewedAt: String?,
    val createdAt: String?
)

data class ExerciseRecommendationDetails(
    val name: String,
    val code: String?,
    val category: String,
    val difficulty: String,
    val targetSkill: String?,
    val progressionLevel: String?,
    val prerequisites: String?,
    val targetMuscles: String?,
    val equipment: String?,
    val description: String?,
    val instructions: String?,
    val whyAdd: String?
)

data class WorkoutProgramRecommendationDetails(
    val programName: String,
    val targetAudience: String?,
    val trainingLevel: String?,
    val weeklyFrequency: Int,
    val durationWeeks: Int,
    val splitType: String?,
    val progressionCriteria: String?,
    val requiredEquipment: String?,
    val safetyConsiderations: String?,
    val rationale: String?,
    val days: List<AdminProgramDay>
)

data class AdminProgramDay(
    val dayOfWeek: String,
    val title: String,
    val restDay: Boolean,
    val exercises: List<AdminProgramExercise>
)

data class AdminProgramExercise(
    val exerciseId: Long?,
    val exerciseName: String,
    val sets: Int,
    val reps: Int?,
    val holdSeconds: Int?,
    val restSeconds: Int,
    val notes: String?
)

data class OperationalRecommendationDetails(
    val identifiedIssue: String,
    val supportingData: String?,
    val suggestedAction: String?,
    val expectedBenefit: String?,
    val affectedArea: String?
)
