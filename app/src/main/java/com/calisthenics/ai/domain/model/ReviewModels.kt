package com.calisthenics.ai.domain.model

const val MAX_REVIEW_COMMENT_LENGTH = 1000

data class RatingSummary(
    val coachId: Long?,
    val averageRating: Double?,
    val totalReviews: Long,
    /** Count per star value, always containing keys 5 down to 1. */
    val distribution: Map<Int, Int>,
    val coachingQualityAvg: Double?,
    val communicationAvg: Double?,
    val planEffectivenessAvg: Double?,
    val supportResponsivenessAvg: Double?
) {
    /** Fraction (0..1) of reviews with the given star value. */
    fun fractionFor(stars: Int): Float {
        val count = distribution[stars] ?: 0
        val total = distribution.values.sum()
        return if (total <= 0) 0f else count.toFloat() / total
    }

    val hasCriteria: Boolean
        get() = listOf(coachingQualityAvg, communicationAvg, planEffectivenessAvg, supportResponsivenessAvg).any { it != null }
}

data class CoachReview(
    val id: Long,
    val overallRating: Int,
    val coachingQuality: Int? = null,
    val communication: Int? = null,
    val planEffectiveness: Int? = null,
    val supportResponsiveness: Int? = null,
    val comment: String? = null,
    val reviewerName: String = "",
    val reviewerAvatarUrl: String? = null,
    val mine: Boolean = false,
    val edited: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ReviewPage(
    val content: List<CoachReview>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)

data class MyReviewStatus(
    val eligible: Boolean,
    val reason: String? = null,
    val review: CoachReview? = null
)

data class ReviewDraft(
    val overallRating: Int = 0,
    val coachingQuality: Int? = null,
    val communication: Int? = null,
    val planEffectiveness: Int? = null,
    val supportResponsiveness: Int? = null,
    val comment: String = ""
) {
    val isValid: Boolean
        get() = overallRating in 1..5 &&
            listOf(coachingQuality, communication, planEffectiveness, supportResponsiveness).all { it == null || it in 1..5 } &&
            comment.length <= MAX_REVIEW_COMMENT_LENGTH

    companion object {
        fun from(review: CoachReview?): ReviewDraft = if (review == null) ReviewDraft() else ReviewDraft(
            overallRating = review.overallRating,
            coachingQuality = review.coachingQuality,
            communication = review.communication,
            planEffectiveness = review.planEffectiveness,
            supportResponsiveness = review.supportResponsiveness,
            comment = review.comment.orEmpty()
        )
    }
}

enum class ReviewModerationStatus {
    VISIBLE, HIDDEN;

    companion object {
        fun fromApi(value: String?): ReviewModerationStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: VISIBLE
    }
}

enum class ReviewHistoryType {
    CREATED, EDITED, HIDDEN, RESTORED, UNKNOWN;

    companion object {
        fun fromApi(value: String?): ReviewHistoryType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

data class AdminReviewHistoryEntry(
    val type: ReviewHistoryType,
    val actorId: Long?,
    val overallRating: Int?,
    val comment: String?,
    val note: String?,
    val createdAt: String?
)

data class AdminReview(
    val id: Long,
    val coachId: Long?,
    val coachName: String,
    val studentId: Long?,
    val studentName: String,
    val overallRating: Int,
    val coachingQuality: Int?,
    val communication: Int?,
    val planEffectiveness: Int?,
    val supportResponsiveness: Int?,
    val comment: String?,
    val status: ReviewModerationStatus,
    val moderationReason: String?,
    val moderatedByAdminId: Long?,
    val moderatedAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val history: List<AdminReviewHistoryEntry> = emptyList()
)
