package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.model.*

fun CoachProfileDto.toDomain() = CoachProfile(
    id = id,
    userId = userId ?: id,
    fullName = fullName,
    displayName = displayName ?: fullName,
    avatarUrl = avatarUrl,
    experienceLevel = experienceLevel,
    bio = bio,
    specialties = specialties,
    yearsExperience = yearsExperience,
    trainingPhilosophy = trainingPhilosophy,
    coachingDescription = coachingDescription,
    languages = languages,
    certifications = certifications,
    socialLinks = socialLinks,
    isPublic = isPublic ?: true,
    isAcceptingClients = isAcceptingClients ?: true,
    isVerified = isVerified ?: false,
    activeClientsCount = activeClientsCount ?: 0L,
    completedClientsCount = completedClientsCount ?: 0L,
    assignedPlansCount = assignedPlansCount ?: 0L,
    connectionStatus = connectionStatus,
    relationshipId = relationshipId,
    conversationId = conversationId,
    gender = Gender.fromApi(gender),
    averageRating = averageRating,
    reviewCount = reviewCount
)

fun CoachClientRelationshipDto.toDomain() = CoachClientRelationship(
    id = id,
    coachId = coachId,
    coachName = coachName,
    coachAvatarUrl = coachAvatarUrl,
    coachExperienceLevel = coachExperienceLevel,
    clientId = clientId,
    clientName = clientName,
    clientAvatarUrl = clientAvatarUrl,
    clientExperienceLevel = clientExperienceLevel,
    requestedBy = requestedBy,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    coachGender = Gender.fromApi(coachGender),
    clientGender = Gender.fromApi(clientGender)
)

fun ConnectedCoachSummaryDto.toDomain() = ConnectedCoachSummary(
    coachId = coachId,
    coachName = coachName,
    coachAvatarUrl = coachAvatarUrl,
    coachExperienceLevel = coachExperienceLevel,
    connectionStatus = connectionStatus,
    relationshipId = relationshipId,
    conversationId = conversationId,
    coachGender = Gender.fromApi(coachGender)
)

fun ClientProfileDto.toDomain() = ClientProfile(
    id = id,
    userId = userId,
    fullName = fullName,
    displayName = displayName ?: fullName,
    avatarUrl = avatarUrl,
    bio = bio,
    experienceLevel = experienceLevel,
    trainingExperience = trainingExperience,
    primaryGoals = primaryGoals,
    trainingInterests = trainingInterests,
    preferredFrequency = preferredFrequency,
    coachingLanguage = coachingLanguage,
    activePlansCount = activePlansCount ?: 0L,
    completedSessionsCount = completedSessionsCount ?: 0L,
    currentPlanId = currentPlanId,
    currentPlanName = currentPlanName,
    heightCm = heightCm,
    weightKg = weightKg,
    connectedCoach = connectedCoach?.toDomain(),
    gender = Gender.fromApi(gender)
)

fun ClientTrainingOverviewDto.toDomain() = ClientTrainingOverview(
    userId = userId,
    activePlansCount = activePlansCount,
    completedSessionsCount = completedSessionsCount,
    currentPlanId = currentPlanId,
    currentPlanName = currentPlanName,
    heightCm = null,
    connectedCoach = connectedCoach?.toDomain(),
    recentWorkouts = recentWorkouts?.map {
        RecentWorkoutItem(
            id = it.id,
            title = it.title,
            startedAt = it.startedAt,
            completedAt = it.completedAt
        )
    } ?: emptyList()
)
