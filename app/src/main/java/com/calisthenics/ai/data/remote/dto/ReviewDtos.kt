package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CoachReviewSummaryDto(
    @SerializedName("coachId") val coachId: Long? = null,
    @SerializedName("averageRating") val averageRating: Double? = null,
    @SerializedName("totalReviews") val totalReviews: Long? = null,
    @SerializedName("distribution") val distribution: Map<String, Int>? = null,
    @SerializedName("coachingQualityAvg") val coachingQualityAvg: Double? = null,
    @SerializedName("communicationAvg") val communicationAvg: Double? = null,
    @SerializedName("planEffectivenessAvg") val planEffectivenessAvg: Double? = null,
    @SerializedName("supportResponsivenessAvg") val supportResponsivenessAvg: Double? = null
)

data class CoachReviewDto(
    @SerializedName("id") val id: Long,
    @SerializedName("overallRating") val overallRating: Int = 0,
    @SerializedName("coachingQuality") val coachingQuality: Int? = null,
    @SerializedName("communication") val communication: Int? = null,
    @SerializedName("planEffectiveness") val planEffectiveness: Int? = null,
    @SerializedName("supportResponsiveness") val supportResponsiveness: Int? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("reviewerName") val reviewerName: String? = null,
    @SerializedName("reviewerAvatarUrl") val reviewerAvatarUrl: String? = null,
    @SerializedName("mine") val mine: Boolean? = null,
    @SerializedName("edited") val edited: Boolean? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CoachReviewPageDto(
    @SerializedName("content") val content: List<CoachReviewDto>? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("size") val size: Int? = null,
    @SerializedName("totalElements") val totalElements: Long? = null,
    @SerializedName("totalPages") val totalPages: Int? = null,
    @SerializedName("hasNext") val hasNext: Boolean? = null
)

data class MyReviewStatusDto(
    @SerializedName("eligible") val eligible: Boolean? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("review") val review: CoachReviewDto? = null
)

data class ReviewRequestDto(
    @SerializedName("overallRating") val overallRating: Int,
    @SerializedName("coachingQuality") val coachingQuality: Int? = null,
    @SerializedName("communication") val communication: Int? = null,
    @SerializedName("planEffectiveness") val planEffectiveness: Int? = null,
    @SerializedName("supportResponsiveness") val supportResponsiveness: Int? = null,
    @SerializedName("comment") val comment: String? = null
)

data class AdminReviewDto(
    @SerializedName("id") val id: Long,
    @SerializedName("coachId") val coachId: Long? = null,
    @SerializedName("coachName") val coachName: String? = null,
    @SerializedName("studentId") val studentId: Long? = null,
    @SerializedName("studentName") val studentName: String? = null,
    @SerializedName("overallRating") val overallRating: Int = 0,
    @SerializedName("coachingQuality") val coachingQuality: Int? = null,
    @SerializedName("communication") val communication: Int? = null,
    @SerializedName("planEffectiveness") val planEffectiveness: Int? = null,
    @SerializedName("supportResponsiveness") val supportResponsiveness: Int? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("moderationReason") val moderationReason: String? = null,
    @SerializedName("moderatedByAdminId") val moderatedByAdminId: Long? = null,
    @SerializedName("moderatedAt") val moderatedAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("history") val history: List<AdminReviewHistoryDto>? = null
)

data class AdminReviewHistoryDto(
    @SerializedName("type") val type: String? = null,
    @SerializedName("actorId") val actorId: Long? = null,
    @SerializedName("overallRating") val overallRating: Int? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class ModerateReviewRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("reason") val reason: String? = null
)
