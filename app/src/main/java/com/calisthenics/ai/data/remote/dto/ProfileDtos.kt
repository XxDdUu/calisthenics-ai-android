package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClientProfileDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("experienceLevel") val experienceLevel: String? = null,
    @SerializedName("trainingExperience") val trainingExperience: String? = null,
    @SerializedName("primaryGoals") val primaryGoals: String? = null,
    @SerializedName("trainingInterests") val trainingInterests: String? = null,
    @SerializedName("preferredFrequency") val preferredFrequency: String? = null,
    @SerializedName("coachingLanguage") val coachingLanguage: String? = null,
    @SerializedName("heightCm") val heightCm: Double? = null,
    @SerializedName("activePlansCount") val activePlansCount: Long? = null,
    @SerializedName("completedSessionsCount") val completedSessionsCount: Long? = null,
    @SerializedName("currentPlanId") val currentPlanId: Long? = null,
    @SerializedName("currentPlanName") val currentPlanName: String? = null,
    @SerializedName("connectedCoach") val connectedCoach: ConnectedCoachSummaryDto? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class ConnectedCoachSummaryDto(
    @SerializedName("coachId") val coachId: Long,
    @SerializedName("coachName") val coachName: String,
    @SerializedName("coachAvatarUrl") val coachAvatarUrl: String? = null,
    @SerializedName("coachExperienceLevel") val coachExperienceLevel: String? = null,
    @SerializedName("connectionStatus") val connectionStatus: String = "ACTIVE",
    @SerializedName("relationshipId") val relationshipId: Long? = null,
    @SerializedName("conversationId") val conversationId: Long? = null
)

data class ClientProfileRequestDto(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("experienceLevel") val experienceLevel: String? = null,
    @SerializedName("trainingExperience") val trainingExperience: String? = null,
    @SerializedName("primaryGoals") val primaryGoals: String? = null,
    @SerializedName("trainingInterests") val trainingInterests: String? = null,
    @SerializedName("preferredFrequency") val preferredFrequency: String? = null,
    @SerializedName("coachingLanguage") val coachingLanguage: String? = null,
    @SerializedName("heightCm") val heightCm: Double? = null
)

data class ClientTrainingOverviewDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("activePlansCount") val activePlansCount: Long,
    @SerializedName("completedSessionsCount") val completedSessionsCount: Long,
    @SerializedName("currentPlanId") val currentPlanId: Long? = null,
    @SerializedName("currentPlanName") val currentPlanName: String? = null,
    @SerializedName("connectedCoach") val connectedCoach: ConnectedCoachSummaryDto? = null,
    @SerializedName("recentWorkouts") val recentWorkouts: List<RecentWorkoutSummaryDto>? = null
)

data class RecentWorkoutSummaryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null
)

data class FileUploadResponseDto(
    @SerializedName("fileName") val fileName: String,
    @SerializedName("fileDownloadUri") val fileDownloadUri: String,
    @SerializedName("fileType") val fileType: String? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("mediaItem") val mediaItem: ExerciseMediaResponseDto? = null
)

data class AvatarUploadResponseDto(
    @SerializedName("avatarUrl") val avatarUrl: String
)
