package com.calisthenics.ai.domain.model

data class CoachProfile(
    val id: Long,
    val fullName: String,
    val avatarUrl: String? = null,
    val experienceLevel: String? = null,
    val userId: Long = id,
    val displayName: String = fullName,
    val bio: String? = null,
    val specialties: String? = null,
    val yearsExperience: Int? = null,
    val trainingPhilosophy: String? = null,
    val coachingDescription: String? = null,
    val languages: String? = null,
    val certifications: String? = null,
    val socialLinks: String? = null,
    val isPublic: Boolean = true,
    val isAcceptingClients: Boolean = true,
    val isVerified: Boolean = false,
    val activeClientsCount: Long = 0L,
    val completedClientsCount: Long = 0L,
    val assignedPlansCount: Long = 0L,
    val connectionStatus: String? = null,
    val relationshipId: Long? = null,
    val conversationId: Long? = null,
    val gender: Gender? = null,
    val averageRating: Double? = null,
    val reviewCount: Long? = null
)

data class ClientProfile(
    val id: Long,
    val fullName: String,
    val avatarUrl: String? = null,
    val experienceLevel: String? = null,
    val userId: Long = id,
    val displayName: String = fullName,
    val bio: String? = null,
    val trainingExperience: String? = null,
    val primaryGoals: String? = null,
    val trainingInterests: String? = null,
    val preferredFrequency: String? = null,
    val coachingLanguage: String? = null,
    val activePlansCount: Long = 0L,
    val completedSessionsCount: Long = 0L,
    val currentPlanId: Long? = null,
    val currentPlanName: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val connectedCoach: ConnectedCoachSummary? = null,
    val gender: Gender? = null
)

data class ConnectedCoachSummary(
    val coachId: Long,
    val coachName: String,
    val coachAvatarUrl: String? = null,
    val coachExperienceLevel: String? = null,
    val connectionStatus: String = "ACTIVE",
    val relationshipId: Long? = null,
    val conversationId: Long? = null,
    val coachGender: Gender? = null
)

data class ClientTrainingOverview(
    val userId: Long,
    val activePlansCount: Long = 0L,
    val completedSessionsCount: Long = 0L,
    val currentPlanId: Long? = null,
    val currentPlanName: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val connectedCoach: ConnectedCoachSummary? = null,
    val recentWorkouts: List<RecentWorkoutItem> = emptyList()
)

data class RecentWorkoutItem(
    val id: Long,
    val title: String,
    val startedAt: String? = null,
    val completedAt: String? = null
)

data class CoachClientRelationship(
    val id: Long,
    val coachId: Long,
    val coachName: String,
    val coachAvatarUrl: String?,
    val coachExperienceLevel: String?,
    val clientId: Long,
    val clientName: String,
    val clientAvatarUrl: String?,
    val clientExperienceLevel: String?,
    val requestedBy: Long,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?,
    val coachGender: Gender? = null,
    val clientGender: Gender? = null
)

data class CoachConnectionOverview(
    val currentUserId: Long,
    val coaches: List<CoachProfile>,
    val relationships: List<CoachClientRelationship>
)
