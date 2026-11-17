package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AdminAiChatResponseDto
import com.calisthenics.ai.data.remote.dto.AdminConfirmationDecisionResponseDto
import com.calisthenics.ai.data.remote.dto.AdminPendingConfirmationDto
import com.calisthenics.ai.data.remote.dto.GettingStartedCampaignAnalyticsDto
import com.calisthenics.ai.data.remote.dto.GettingStartedCampaignSummaryDto
import com.calisthenics.ai.data.remote.dto.GettingStartedProgramPreviewDto
import com.calisthenics.ai.data.remote.dto.AdminAiConversationSummaryDto
import com.calisthenics.ai.data.remote.dto.AdminAiMessageDto
import com.calisthenics.ai.data.remote.dto.AdminAiRecommendationDto
import com.calisthenics.ai.data.remote.dto.AdminDashboardDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDeleteResponseDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseUsageStatDto
import com.calisthenics.ai.data.remote.dto.AdminGoalStatDto
import com.calisthenics.ai.data.remote.dto.AdminInactiveUsersSummaryDto
import com.calisthenics.ai.data.remote.dto.AdminPageDto
import com.calisthenics.ai.data.remote.dto.AdminPlanAdherenceSummaryDto
import com.calisthenics.ai.data.remote.dto.AdminPlatformOverviewDto
import com.calisthenics.ai.data.remote.dto.AdminProgramEngagementStatDto
import com.calisthenics.ai.data.remote.dto.AdminSkillFocusStatDto
import com.calisthenics.ai.data.remote.dto.AdminTrainingAnalyticsDto
import com.calisthenics.ai.data.remote.dto.AdminUserDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDto
import com.calisthenics.ai.data.remote.dto.ExerciseRecommendationPayloadDto
import com.calisthenics.ai.data.remote.dto.OperationalRecommendationPayloadDto
import com.calisthenics.ai.data.remote.dto.ProgramDayPayloadDto
import com.calisthenics.ai.data.remote.dto.ProgramExercisePayloadDto
import com.calisthenics.ai.data.remote.dto.WorkoutProgramRecommendationPayloadDto
import com.calisthenics.ai.domain.model.AdminAiChatResponse
import com.calisthenics.ai.domain.model.AdminConfirmationDecision
import com.calisthenics.ai.domain.model.AdminPendingConfirmation
import com.calisthenics.ai.domain.model.GettingStartedCampaignAnalytics
import com.calisthenics.ai.domain.model.GettingStartedCampaignSummary
import com.calisthenics.ai.domain.model.GettingStartedProgramPreview
import com.calisthenics.ai.domain.model.AdminAiConversationSummary
import com.calisthenics.ai.domain.model.AdminAiMessage
import com.calisthenics.ai.domain.model.AdminAiRecommendation
import com.calisthenics.ai.domain.model.AdminDashboard
import com.calisthenics.ai.domain.model.AdminExercise
import com.calisthenics.ai.domain.model.AdminExerciseDeleteResult
import com.calisthenics.ai.domain.model.AdminExerciseUsageStat
import com.calisthenics.ai.domain.model.AdminGoalStat
import com.calisthenics.ai.domain.model.AdminInactiveUsersSummary
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminPlanAdherenceSummary
import com.calisthenics.ai.domain.model.AdminPlatformOverview
import com.calisthenics.ai.domain.model.AdminProgramDay
import com.calisthenics.ai.domain.model.AdminProgramEngagementStat
import com.calisthenics.ai.domain.model.AdminProgramExercise
import com.calisthenics.ai.domain.model.AdminSkillFocusStat
import com.calisthenics.ai.domain.model.AdminTrainingAnalytics
import com.calisthenics.ai.domain.model.AdminUser
import com.calisthenics.ai.domain.model.AdminWorkoutPlan
import com.calisthenics.ai.domain.model.AdminWorkoutPlanDetail
import com.calisthenics.ai.domain.model.AdminWorkoutSession
import com.calisthenics.ai.domain.model.AdminWorkoutSessionDetail
import com.calisthenics.ai.domain.model.ExerciseRecommendationDetails
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.OperationalRecommendationDetails
import com.calisthenics.ai.domain.model.WorkoutProgramRecommendationDetails
import com.calisthenics.ai.data.remote.dto.AdminBucketCountDto
import com.calisthenics.ai.data.remote.dto.AdminCoachClientStatisticsDto
import com.calisthenics.ai.data.remote.dto.AdminCoachProfileSummaryDto
import com.calisthenics.ai.data.remote.dto.AdminCoachRelationshipItemDto
import com.calisthenics.ai.data.remote.dto.AdminExperienceComparisonItemDto
import com.calisthenics.ai.data.remote.dto.AdminGlobalUserStatisticsDto
import com.calisthenics.ai.data.remote.dto.AdminGlobalUserSummaryDto
import com.calisthenics.ai.data.remote.dto.AdminPagedResultDto
import com.calisthenics.ai.data.remote.dto.AdminPlatformEngagementDto
import com.calisthenics.ai.data.remote.dto.AdminSkillImproverItemDto
import com.calisthenics.ai.data.remote.dto.AdminTimeRangeMetadataDto
import com.calisthenics.ai.data.remote.dto.AdminToolExecutionRecordDto
import com.calisthenics.ai.data.remote.dto.AdminUserActivityDto
import com.calisthenics.ai.data.remote.dto.AdminUserDetailDossierDto
import com.calisthenics.ai.data.remote.dto.AdminUserPersonalRecordsDto
import com.calisthenics.ai.data.remote.dto.AdminUserProgressDto
import com.calisthenics.ai.data.remote.dto.AdminUserSegmentFilterRequestDto
import com.calisthenics.ai.data.remote.dto.AdminUserSegmentsDto
import com.calisthenics.ai.data.remote.dto.AdminUserWorkoutHistoryDto
import com.calisthenics.ai.domain.model.AdminBucketCount
import com.calisthenics.ai.domain.model.AdminCoachClientStatistics
import com.calisthenics.ai.domain.model.AdminCoachProfileSummary
import com.calisthenics.ai.domain.model.AdminCoachRelationshipItem
import com.calisthenics.ai.domain.model.AdminExperienceComparison
import com.calisthenics.ai.domain.model.AdminGlobalUserStatistics
import com.calisthenics.ai.domain.model.AdminGlobalUserSummary
import com.calisthenics.ai.domain.model.AdminPlatformEngagement
import com.calisthenics.ai.domain.model.AdminSkillImprover
import com.calisthenics.ai.domain.model.AdminTimeRange
import com.calisthenics.ai.domain.model.AdminToolExecution
import com.calisthenics.ai.domain.model.AdminUserActivity
import com.calisthenics.ai.domain.model.AdminUserDetailDossier
import com.calisthenics.ai.domain.model.AdminUserPersonalRecords
import com.calisthenics.ai.domain.model.AdminUserProgress
import com.calisthenics.ai.domain.model.AdminUserSegmentFilter
import com.calisthenics.ai.domain.model.AdminUserSegments
import com.calisthenics.ai.domain.model.AdminUserWorkoutHistory

fun AdminDashboardDto.toDomain() = AdminDashboard(
    totalUsers,
    totalClients,
    totalCoaches,
    totalAdmins,
    totalWorkoutSessions,
    totalWorkoutPlans,
    totalExercises
)

fun AdminUserDto.toDomain(): AdminUser {
    val isAdminRole = role.equals("ROLE_ADMIN", ignoreCase = true)
    val resolvedTier = membershipTier?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        ?: if (isAdminRole) "ADMIN" else "FREE"
    val resolvedSource = membershipSource?.trim()?.takeIf { it.isNotBlank() }
        ?: when {
            isAdminRole -> "ADMIN Role"
            adminGrantedPro -> "Admin Grant"
            paidPro || resolvedTier == "PRO" -> "Subscription"
            else -> "Default"
        }
    return AdminUser(
        id = id,
        email = email,
        fullName = fullName,
        role = role,
        hasCoachProfile = hasCoachProfile,
        hasClientProfile = hasClientProfile,
        createdAt = createdAt,
        gender = Gender.fromApi(gender),
        membershipTier = resolvedTier,
        membershipSource = resolvedSource,
        unlimited = unlimited || isAdminRole || adminGrantedPro,
        paidPro = paidPro,
        adminGrantedPro = adminGrantedPro,
        adminProStatus = adminProStatus ?: if (adminGrantedPro) "ACTIVE" else "NONE",
        adminProGrantSource = adminProGrantSource,
        grantedByAdminId = grantedByAdminId,
        adminProGrantedAt = adminProGrantedAt,
        revokedByAdminId = revokedByAdminId,
        adminProRevokedAt = adminProRevokedAt,
        adminProReason = adminProReason
    )
}

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

fun AdminAiConversationSummaryDto.toDomain() = AdminAiConversationSummary(
    id = id,
    title = title,
    lastMessagePreview = lastMessagePreview,
    lastMessageRole = lastMessageRole,
    lastInteractionAt = lastInteractionAt,
    createdAt = createdAt,
    messageCount = messageCount
)

fun AdminAiMessageDto.toDomain() = AdminAiMessage(
    id = id,
    conversationId = conversationId,
    role = role,
    content = content,
    intent = intent,
    createdAt = createdAt
)

fun AdminAiChatResponseDto.toDomain() = AdminAiChatResponse(
    conversationId = conversationId,
    conversationTitle = conversationTitle,
    userMessage = userMessage?.toDomain(),
    assistantMessage = assistantMessage.toDomain(),
    intent = intent,
    recommendations = recommendations.map { it.toDomain() },
    platformOverview = platformOverview?.toDomain(),
    trainingAnalytics = trainingAnalytics?.toDomain(),
    suggestedFollowUps = suggestedFollowUps,
    executedTools = executedTools.map { it.toDomain() },
    timeRangeUsed = timeRangeUsed?.toDomain(),
    userStatistics = userStatistics?.toDomain(),
    matchedUsers = matchedUsers.map { it.toDomain() },
    inspectedUserDetail = inspectedUserDetail?.toDomain(),
    segmentAnalysis = segmentAnalysis?.toDomain(),
    platformEngagement = platformEngagement?.toDomain(),
    coachClientStatistics = coachClientStatistics?.toDomain(),
    gettingStartedProgram = gettingStartedProgram?.toDomain(),
    gettingStartedPreview = gettingStartedPreview?.toDomain(),
    activeCampaigns = activeCampaigns.map { it.toDomain() },
    campaignAnalytics = campaignAnalytics?.toDomain(),
    pendingConfirmation = pendingConfirmation?.toDomain(),
    missingDataNotes = missingDataNotes,
    updatedProUser = updatedProUser?.toDomain(),
    proCandidates = proCandidates.map { it.toDomain() }
)

fun GettingStartedProgramPreviewDto.toDomain() = GettingStartedProgramPreview(
    program = program?.toDomain(),
    valid = valid,
    validationErrors = validationErrors,
    targetAudience = targetAudience,
    audienceLabel = audienceLabel,
    eligibleAthleteCount = eligibleAthleteCount,
    totalAthleteCount = totalAthleteCount,
    estimatedAudienceSize = estimatedAudienceSize
)

fun GettingStartedCampaignSummaryDto.toDomain() = GettingStartedCampaignSummary(
    programId = programId,
    title = title,
    description = description,
    published = published,
    version = version,
    targetAudience = targetAudience,
    audienceLabel = audienceLabel,
    eligibleAthleteCount = eligibleAthleteCount,
    enrollments = enrollments,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    days = days.map { it.toDomain() }
)

fun GettingStartedCampaignAnalyticsDto.toDomain() = GettingStartedCampaignAnalytics(
    programId = programId,
    programTitle = programTitle,
    published = published,
    version = version,
    targetAudience = targetAudience,
    audienceLabel = audienceLabel,
    eligibleAthleteCount = eligibleAthleteCount,
    modalImpressions = modalImpressions,
    modalDismissals = modalDismissals,
    programDetailViews = programDetailViews,
    enrollments = enrollments,
    day1Completions = day1Completions,
    day2Completions = day2Completions,
    day3Completions = day3Completions,
    day1CompletionRate = day1CompletionRate,
    day2CompletionRate = day2CompletionRate,
    day3CompletionRate = day3CompletionRate,
    completedEnrollments = completedEnrollments,
    overallCompletionRate = overallCompletionRate
)

fun AdminPendingConfirmationDto.toDomain() = AdminPendingConfirmation(
    confirmationToken = confirmationToken,
    actionType = actionType,
    status = status,
    programId = programId,
    programTitle = programTitle,
    targetAudience = targetAudience,
    audienceLabel = audienceLabel,
    eligibleAthleteCount = eligibleAthleteCount,
    summary = summary,
    createdAt = createdAt,
    expiresAt = expiresAt,
    targetUserId = targetUserId,
    targetUserName = targetUserName,
    targetUserEmail = targetUserEmail,
    targetUserCurrentTier = targetUserCurrentTier,
    targetUserMembershipSource = targetUserMembershipSource,
    targetUserPaidPro = targetUserPaidPro,
    targetUserAdminGrantedPro = targetUserAdminGrantedPro,
    reason = reason
)

fun AdminConfirmationDecisionResponseDto.toDomain() = AdminConfirmationDecision(
    confirmationToken = confirmationToken,
    actionType = actionType,
    status = status,
    conversationId = conversationId,
    reply = reply,
    execution = execution?.toDomain(),
    gettingStartedProgram = gettingStartedProgram?.toDomain(),
    activeCampaigns = activeCampaigns.map { it.toDomain() },
    updatedUser = updatedUser?.toDomain()
)

fun AdminTimeRangeMetadataDto.toDomain() = AdminTimeRange(
    range = range ?: "LAST_30_DAYS",
    startDate = startDate,
    endDate = endDate,
    periodDays = periodDays
)

fun AdminToolExecutionRecordDto.toDomain() = AdminToolExecution(
    toolName = toolName,
    status = status,
    parametersSummary = parametersSummary,
    summary = summary,
    recordCount = recordCount,
    durationMs = durationMs,
    executedAt = executedAt
)

fun AdminBucketCountDto.toDomain() = AdminBucketCount(
    label = label,
    count = count,
    percentage = percentage
)

fun AdminUserSegmentFilter.toDto() = AdminUserSegmentFilterRequestDto(
    searchQuery = searchQuery,
    experienceLevel = experienceLevel,
    fitnessGoal = fitnessGoal,
    activityStatus = activityStatus,
    inactiveDaysThreshold = inactiveDaysThreshold,
    maxCompletionRate = maxCompletionRate,
    coachRelationshipStatus = coachRelationshipStatus,
    roleFilter = roleFilter,
    skillKeyword = skillKeyword,
    naturalLanguageQuery = naturalLanguageQuery,
    includeEmail = includeEmail,
    sortBy = sortBy,
    range = range,
    startDate = startDate,
    endDate = endDate,
    page = page,
    size = size
)

fun <T, R> AdminPagedResultDto<T>.toDomain(map: (T) -> R) = AdminPage(
    content = items.map(map),
    page = page,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    hasNext = hasNext
)

fun AdminGlobalUserSummaryDto.toDomain() = AdminGlobalUserSummary(
    userId = userId,
    fullName = fullName ?: displayName ?: "User #$userId",
    displayName = displayName,
    maskedEmail = maskedEmail,
    email = email,
    role = role,
    accountCategory = accountCategory,
    experienceLevel = experienceLevel,
    trainingExperience = trainingExperience,
    primaryGoals = primaryGoals,
    trainingInterests = trainingInterests,
    preferredFrequency = preferredFrequency,
    registeredAt = registeredAt,
    lastActivityAt = lastActivityAt,
    activityStatus = activityStatus,
    daysSinceLastWorkout = daysSinceLastWorkout,
    totalWorkouts = totalWorkouts,
    completedWorkouts = completedWorkouts,
    abandonedOrIncompleteWorkouts = abandonedOrIncompleteWorkouts,
    workoutCompletionRate = workoutCompletionRate,
    totalPersonalRecords = totalPersonalRecords,
    bestPrImprovementPercent = bestPrImprovementPercent,
    lastPrAchievedAt = lastPrAchievedAt,
    consistencyTrend = consistencyTrend,
    coachRelationshipStatus = coachRelationshipStatus,
    coachCount = coachCount,
    activeClientCount = activeClientCount,
    aiConversationsCount = aiConversationsCount,
    chatMessagesSentCount = chatMessagesSentCount
)

fun AdminGlobalUserStatisticsDto.toDomain() = AdminGlobalUserStatistics(
    timeRange = timeRange?.toDomain(),
    totalRegisteredUsers = totalRegisteredUsers,
    totalRegularUsers = totalRegularUsers,
    totalAdmins = totalAdmins,
    totalCoaches = totalCoaches,
    verifiedCoaches = verifiedCoaches,
    totalClients = totalClients,
    newRegistrationsInPeriod = newRegistrationsInPeriod,
    activeUsersToday = activeUsersToday,
    activeUsers7Days = activeUsers7Days,
    activeUsers30Days = activeUsers30Days,
    activeUsersInRange = activeUsersInRange,
    inactiveUsers14PlusDays = inactiveUsers14PlusDays,
    atRiskUsers7To14Days = atRiskUsers7To14Days,
    usersWithActiveCoach = usersWithActiveCoach,
    usersWithPendingCoachInvite = usersWithPendingCoachInvite,
    usersWithoutCoach = usersWithoutCoach,
    totalWorkoutsInRange = totalWorkoutsInRange,
    completedWorkoutsInRange = completedWorkoutsInRange,
    incompleteOrAbandonedWorkoutsInRange = incompleteOrAbandonedWorkoutsInRange,
    overallWorkoutCompletionRate = overallWorkoutCompletionRate,
    totalPersonalRecordsAllTime = totalPersonalRecordsAllTime,
    newPersonalRecordsInRange = newPersonalRecordsInRange,
    usersWithStalePrs30PlusDays = usersWithStalePrs30PlusDays,
    experienceLevelDistribution = experienceLevelDistribution.map { it.toDomain() },
    activityStatusDistribution = activityStatusDistribution.map { it.toDomain() },
    topFitnessGoals = topFitnessGoals.map { it.toDomain() },
    missingDataNotes = missingDataNotes
)

fun AdminUserDetailDossierDto.toDomain() = AdminUserDetailDossier(
    timeRange = timeRange?.toDomain(),
    userId = userId,
    fullName = fullName ?: displayName ?: "User #$userId",
    displayName = displayName,
    maskedEmail = maskedEmail,
    email = email,
    role = role,
    accountCategory = accountCategory,
    gender = gender,
    bio = bio,
    experienceLevel = experienceLevel,
    trainingExperience = trainingExperience,
    primaryGoals = primaryGoals,
    trainingInterests = trainingInterests,
    preferredFrequency = preferredFrequency,
    coachingLanguage = coachingLanguage,
    heightCm = heightCm,
    latestWeightKg = latestWeightKg,
    latestBodyFatPercentage = latestBodyFatPercentage,
    latestMeasurementAt = latestMeasurementAt,
    inferredAvailableEquipment = inferredAvailableEquipment,
    registeredAt = registeredAt,
    coachProfile = coachProfile?.toDomain(),
    workoutSummary = workoutSummary?.toDomain(),
    personalRecordsSummary = personalRecordsSummary?.toDomain(),
    progressSummary = progressSummary?.toDomain(),
    activitySummary = activitySummary?.toDomain(),
    coachRelationships = coachRelationships.map { it.toDomain() },
    missingDataFields = missingDataFields
)

fun AdminCoachProfileSummaryDto.toDomain() = AdminCoachProfileSummary(
    displayName = displayName,
    specialties = specialties,
    yearsExperience = yearsExperience,
    languages = languages,
    isPublic = isPublic,
    isAcceptingClients = isAcceptingClients,
    isVerified = isVerified,
    activeClientsCount = activeClientsCount,
    pendingInvitationsCount = pendingInvitationsCount
)

fun AdminCoachRelationshipItemDto.toDomain() = AdminCoachRelationshipItem(
    relationshipId = relationshipId,
    counterpartRole = counterpartRole,
    counterpartUserId = counterpartUserId,
    counterpartName = counterpartName,
    status = status,
    requestedByRole = requestedByRole,
    createdAt = createdAt
)

fun AdminUserWorkoutHistoryDto.toDomain() = AdminUserWorkoutHistory(
    totalSessions = totalSessions,
    completedSessions = completedSessions,
    inProgressSessions = inProgressSessions,
    scheduledSessions = scheduledSessions,
    cancelledSessions = cancelledSessions,
    completionRate = completionRate,
    totalTrainingMinutes = totalTrainingMinutes,
    averageDurationMinutes = averageDurationMinutes,
    averageSessionsPerWeek = averageSessionsPerWeek,
    consistencyTrend = consistencyTrend,
    completedExercisesCount = completedExercisesCount,
    pendingOrIncompleteExercisesCount = pendingOrIncompleteExercisesCount,
    assignedPlanNames = assignedPlans.mapNotNull { it.planName?.let { n -> "$n (${it.status})" } },
    personalPlanNames = personalPlans.map { "${it.name}${if (it.aiGenerated) " [AI]" else ""}" },
    recentSessionLabels = recentSessions.map {
        "${it.workoutDate ?: ""} • ${it.title ?: "Session #${it.sessionId}"} [${it.status}] (${it.completedExercises}/${it.totalExercises} ex)"
    },
    abandonedWorkoutLabels = abandonedOrIncompleteWorkouts.map {
        "${it.workoutDate ?: ""} • ${it.title ?: "Session #${it.sessionId}"} [${it.status}]"
    },
    missingDataNotes = missingDataNotes
)

fun AdminUserPersonalRecordsDto.toDomain() = AdminUserPersonalRecords(
    totalRecords = totalRecords,
    recordsInRange = recordsInRange,
    averageImprovementPercent = averageImprovementPercent,
    maxImprovementPercent = maxImprovementPercent,
    lastPrAchievedAt = lastPrAchievedAt,
    daysSinceLastPr = daysSinceLastPr,
    sharedPrCount = sharedPrCount,
    coachFeedbackCount = coachFeedbackCount,
    recordSummaries = records.map {
        val imp = it.improvementPercentage?.let { p -> " (+${String.format(java.util.Locale.US, "%.1f%%", p)})" } ?: ""
        "${it.exerciseName ?: "Exercise"} [${it.recordType ?: "PR"}]: ${it.recordValue ?: "-"} ${it.unit ?: ""}$imp"
    },
    feedbackSummaries = sharingAndFeedbackHistory.mapNotNull {
        it.coachFeedback?.let { fb -> "${it.coachName ?: "Coach"} on ${it.exerciseName ?: "PR"}: \"$fb\"" }
    },
    missingDataNotes = missingDataNotes
)

fun AdminUserProgressDto.toDomain() = AdminUserProgress(
    overallCompletionRate = overallCompletionRate,
    currentPeriodWorkouts = currentPeriodWorkouts,
    previousPeriodWorkouts = previousPeriodWorkouts,
    workoutVolumeDeltaPercent = workoutVolumeDeltaPercent,
    consistencyTrend = consistencyTrend,
    topExercises = exercisePerformance.take(6).map {
        "${it.exerciseName}: ${it.completedCount}/${it.attempts} (${String.format(java.util.Locale.US, "%.0f%%", it.completionRate)})"
    },
    strengthProgressionSummaries = strengthProgression.take(6).map {
        val imp = it.bestImprovementPercent?.let { p -> " best +${String.format(java.util.Locale.US, "%.1f%%", p)}" } ?: ""
        "${it.exerciseName} [${it.recordType ?: ""}]: ${it.latestValue ?: "-"} ${it.unit ?: ""}$imp"
    },
    bodyMetricSummaries = bodyMetricTrends.take(5).map {
        "Weight ${it.weightKg ?: "-"}kg, BF ${it.bodyFatPercentage ?: "-"}% (${it.measuredAt?.take(10) ?: ""})"
    },
    missingDataNotes = missingDataNotes
)

fun AdminUserActivityDto.toDomain() = AdminUserActivity(
    presenceStatus = presenceStatus,
    lastSeenAt = lastSeenAt,
    lastWorkoutAt = lastWorkoutAt,
    lastChatMessageAt = lastChatMessageAt,
    lastAiInteractionAt = lastAiInteractionAt,
    chatConversationsCount = chatConversationsCount,
    chatMessagesSentInRange = chatMessagesSentInRange,
    aiConversationsCount = aiConversationsCount,
    aiMessagesInRange = aiMessagesInRange,
    workoutPlannerRunsInRange = workoutPlannerRunsInRange,
    liveAnalysisSessionsInRange = liveAnalysisSessionsInRange,
    videoAnalysesInRange = videoAnalysesInRange,
    prRecordsLoggedInRange = prRecordsLoggedInRange,
    topAiIntents = topAiIntents.map { it.toDomain() },
    privacyNotice = privacyNotice,
    missingDataNotes = missingDataNotes
)

fun AdminCoachClientStatisticsDto.toDomain() = AdminCoachClientStatistics(
    timeRange = timeRange?.toDomain(),
    totalCoaches = totalCoaches,
    verifiedCoaches = verifiedCoaches,
    totalRelationships = totalRelationships,
    activeRelationships = activeRelationships,
    pendingInvitations = pendingInvitations,
    clientsWithActiveCoach = clientsWithActiveCoach,
    totalAssignedPlans = totalAssignedPlans,
    activeAssignedPlans = activeAssignedPlans,
    totalSharedPrs = totalSharedPrs,
    feedbackResponseRate = feedbackResponseRate,
    topCoachNames = topCoaches.map {
        "${it.coachName ?: "Coach #${it.coachUserId}"} (${it.activeClients} active, ${it.activeAssignedPlans} plans)"
    },
    unassignedCoachCandidates = unassignedCoachCandidates.map { it.toDomain() },
    missingDataNotes = missingDataNotes
)

fun AdminPlatformEngagementDto.toDomain() = AdminPlatformEngagement(
    timeRange = timeRange?.toDomain(),
    dauCount = dauCount,
    wauCount = wauCount,
    mauCount = mauCount,
    stickinessDauToMauPercent = stickinessDauToMauPercent,
    experienceComparisons = experienceComparisons.map { it.toDomain() },
    topSkillImprovers = topSkillImprovers.map { it.toDomain() },
    decliningConsistencyUsers = decliningConsistencyUsers.map { it.toDomain() },
    stalePrUsers = stalePrUsers.map { it.toDomain() },
    totalChatMessagesInRange = chatAndAiUsageSummary?.totalChatMessagesInRange ?: 0L,
    totalAiMessagesInRange = chatAndAiUsageSummary?.totalAiMessagesInRange ?: 0L,
    workoutPlannerRunsInRange = chatAndAiUsageSummary?.workoutPlannerRunsInRange ?: 0L,
    missingDataNotes = missingDataNotes
)

fun AdminExperienceComparisonItemDto.toDomain() = AdminExperienceComparison(
    experienceLevel = experienceLevel,
    userCount = userCount,
    activeUserCount = activeUserCount,
    activeRatePercent = activeRatePercent,
    avgWorkoutsPerUser = avgWorkoutsPerUser,
    avgCompletionRate = avgCompletionRate,
    prCount = prCount,
    avgAiInteractionsPerUser = avgAiInteractionsPerUser
)

fun AdminSkillImproverItemDto.toDomain() = AdminSkillImprover(
    userId = userId,
    fullName = fullName ?: "User #$userId",
    experienceLevel = experienceLevel,
    exerciseName = exerciseName ?: "Exercise",
    recordType = recordType,
    latestRecordValue = latestRecordValue,
    unit = unit,
    improvementPercent = improvementPercent
)

fun AdminUserSegmentsDto.toDomain() = AdminUserSegments(
    timeRange = timeRange?.toDomain(),
    segmentSummary = segmentSummary,
    matchedCount = matchedCount,
    returnedCount = returnedCount,
    page = page,
    size = size,
    totalPages = totalPages,
    users = users.map { it.toDomain() },
    experienceBreakdown = experienceBreakdown.map { it.toDomain() },
    activityBreakdown = activityBreakdown.map { it.toDomain() },
    goalBreakdown = goalBreakdown.map { it.toDomain() },
    coachStatusBreakdown = coachStatusBreakdown.map { it.toDomain() },
    missingDataNotes = missingDataNotes
)

fun AdminPlatformOverviewDto.toDomain() = AdminPlatformOverview(
    range = range ?: "LAST_30_DAYS",
    startDate = startDate,
    endDate = endDate,
    totalRegisteredUsers = totalRegisteredUsers,
    activeUsersInPeriod = activeUsersInPeriod,
    newRegistrationsInPeriod = newRegistrationsInPeriod,
    totalCoaches = totalCoaches,
    totalClients = totalClients,
    totalAdmins = totalAdmins,
    activeCoachClientRelationships = activeCoachClientRelationships,
    pendingCoachClientRelationships = pendingCoachClientRelationships,
    totalWorkoutPlans = totalWorkoutPlans,
    activeWorkoutPlans = activeWorkoutPlans,
    aiGeneratedWorkoutPlans = aiGeneratedWorkoutPlans,
    totalCompletedWorkoutsAllTime = totalCompletedWorkoutsAllTime,
    completedWorkoutsInPeriod = completedWorkoutsInPeriod,
    scheduledOrInProgressWorkoutsInPeriod = scheduledOrInProgressWorkoutsInPeriod,
    totalRecordedPrsAllTime = totalRecordedPrsAllTime,
    recordedPrsInPeriod = recordedPrsInPeriod,
    totalExercises = totalExercises,
    activeExercises = activeExercises,
    activeUserRatePercent = engagementMetrics?.activeUserRatePercent ?: 0.0,
    workoutCompletionRatePercent = engagementMetrics?.workoutCompletionRatePercent ?: 0.0,
    avgWorkoutsPerActiveUser = engagementMetrics?.avgWorkoutsPerActiveUser ?: 0.0,
    usersWithActivePlanCount = engagementMetrics?.usersWithActivePlanCount ?: 0L,
    workoutGrowthPercent = activityTrends?.workoutGrowthPercent ?: 0.0,
    userGrowthPercent = activityTrends?.userGrowthPercent ?: 0.0
)

fun AdminTrainingAnalyticsDto.toDomain() = AdminTrainingAnalytics(
    range = range ?: "LAST_30_DAYS",
    startDate = startDate,
    endDate = endDate,
    mostPerformedExercises = mostPerformedExercises.map { it.toDomain() },
    highestCompletionRateExercises = highestCompletionRateExercises.map { it.toDomain() },
    skillFocusBreakdown = skillFocusBreakdown.map { it.toDomain() },
    commonTrainingGoals = commonTrainingGoals.map { it.toDomain() },
    inactiveUsersSummary = inactiveUsersSummary?.toDomain(),
    planAdherenceSummary = planAdherenceSummary?.toDomain(),
    topEngagedPrograms = topEngagedPrograms.map { it.toDomain() },
    exerciseLibraryByCategory = exerciseLibraryByCategory,
    exerciseLibraryByDifficulty = exerciseLibraryByDifficulty
)

fun AdminExerciseUsageStatDto.toDomain() = AdminExerciseUsageStat(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    category = category,
    difficulty = difficulty,
    sessionPerformances = sessionPerformances,
    completedPerformances = completedPerformances,
    planAssignmentsCount = planAssignmentsCount,
    prCount = prCount,
    completionRatePercent = completionRatePercent
)

fun AdminSkillFocusStatDto.toDomain() = AdminSkillFocusStat(
    skillName = skillName,
    existingExerciseCount = existingExerciseCount,
    planReferencesCount = planReferencesCount,
    sessionPerformancesCount = sessionPerformancesCount,
    prCount = prCount,
    interestedUsersCount = interestedUsersCount,
    missingProgressions = missingProgressions
)

fun AdminGoalStatDto.toDomain() = AdminGoalStat(
    goal = goal,
    userCount = userCount,
    percentage = percentage
)

fun AdminInactiveUsersSummaryDto.toDomain() = AdminInactiveUsersSummary(
    totalUsersEvaluated = totalUsersEvaluated,
    inactiveOver14DaysCount = inactiveOver14DaysCount,
    inactiveOver30DaysCount = inactiveOver30DaysCount,
    neverCompletedWorkoutCount = neverCompletedWorkoutCount,
    inactiveRatePercent = inactiveRatePercent
)

fun AdminPlanAdherenceSummaryDto.toDomain() = AdminPlanAdherenceSummary(
    usersWithActivePersonalPlan = usersWithActivePersonalPlan,
    activeCoachAssignmentsCount = activeCoachAssignmentsCount,
    completedAssignedSessions = completedAssignedSessions,
    totalAssignedSessions = totalAssignedSessions,
    adherenceRatePercent = adherenceRatePercent
)

fun AdminProgramEngagementStatDto.toDomain() = AdminProgramEngagementStat(
    planId = planId,
    planName = planName,
    splitType = splitType,
    ownerName = ownerName,
    aiGenerated = aiGenerated,
    completedSessionsCount = completedSessionsCount,
    totalSessionsCount = totalSessionsCount,
    completionRatePercent = completionRatePercent
)

fun AdminAiRecommendationDto.toDomain() = AdminAiRecommendation(
    id = id,
    type = type,
    status = status,
    priority = priority,
    title = title,
    summary = summary,
    rationale = rationale,
    exerciseDetails = exerciseDetails?.toDomain(),
    programDetails = programDetails?.toDomain(),
    operationalDetails = operationalDetails?.toDomain(),
    conversationId = conversationId,
    createdResourceId = createdResourceId,
    createdResourceType = createdResourceType,
    reviewNotes = reviewNotes,
    reviewedAt = reviewedAt,
    createdAt = createdAt
)

fun ExerciseRecommendationPayloadDto.toDomain() = ExerciseRecommendationDetails(
    name = name,
    code = code,
    category = category,
    difficulty = difficulty,
    targetSkill = targetSkill,
    progressionLevel = progressionLevel,
    prerequisites = prerequisites,
    targetMuscles = targetMuscles,
    equipment = equipment,
    description = description,
    instructions = instructions,
    whyAdd = whyAdd
)

fun WorkoutProgramRecommendationPayloadDto.toDomain() = WorkoutProgramRecommendationDetails(
    programName = programName,
    targetAudience = targetAudience,
    trainingLevel = trainingLevel,
    weeklyFrequency = weeklyFrequency,
    durationWeeks = durationWeeks,
    splitType = splitType,
    progressionCriteria = progressionCriteria,
    requiredEquipment = requiredEquipment,
    safetyConsiderations = safetyConsiderations,
    rationale = rationale,
    days = days.map { it.toDomain() }
)

fun ProgramDayPayloadDto.toDomain() = AdminProgramDay(
    dayOfWeek = dayOfWeek,
    title = title,
    restDay = restDay,
    exercises = exercises.map { it.toDomain() }
)

fun ProgramExercisePayloadDto.toDomain() = AdminProgramExercise(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    sets = sets,
    reps = reps,
    holdSeconds = holdSeconds,
    restSeconds = restSeconds,
    notes = notes
)

fun OperationalRecommendationPayloadDto.toDomain() = OperationalRecommendationDetails(
    identifiedIssue = identifiedIssue,
    supportingData = supportingData,
    suggestedAction = suggestedAction,
    expectedBenefit = expectedBenefit,
    affectedArea = affectedArea
)
