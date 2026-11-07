package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CoachProfileDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("experienceLevel") val experienceLevel: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("specialties") val specialties: String? = null,
    @SerializedName("yearsExperience") val yearsExperience: Int? = null,
    @SerializedName("trainingPhilosophy") val trainingPhilosophy: String? = null,
    @SerializedName("coachingDescription") val coachingDescription: String? = null,
    @SerializedName("languages") val languages: String? = null,
    @SerializedName("certifications") val certifications: String? = null,
    @SerializedName("socialLinks") val socialLinks: String? = null,
    @SerializedName("isPublic") val isPublic: Boolean? = null,
    @SerializedName("isAcceptingClients") val isAcceptingClients: Boolean? = null,
    @SerializedName("isVerified") val isVerified: Boolean? = null,
    @SerializedName("activeClientsCount") val activeClientsCount: Long? = null,
    @SerializedName("completedClientsCount") val completedClientsCount: Long? = null,
    @SerializedName("assignedPlansCount") val assignedPlansCount: Long? = null,
    @SerializedName("connectionStatus") val connectionStatus: String? = null,
    @SerializedName("relationshipId") val relationshipId: Long? = null,
    @SerializedName("conversationId") val conversationId: Long? = null
)

data class CoachProfileRequestDto(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("specialties") val specialties: String? = null,
    @SerializedName("yearsExperience") val yearsExperience: Int? = null,
    @SerializedName("trainingPhilosophy") val trainingPhilosophy: String? = null,
    @SerializedName("coachingDescription") val coachingDescription: String? = null,
    @SerializedName("languages") val languages: String? = null,
    @SerializedName("certifications") val certifications: String? = null,
    @SerializedName("socialLinks") val socialLinks: String? = null,
    @SerializedName("isPublic") val isPublic: Boolean? = null,
    @SerializedName("isAcceptingClients") val isAcceptingClients: Boolean? = null
)

data class CreateClientCoachRequestDto(
    @SerializedName("coachId") val coachId: Long
)
