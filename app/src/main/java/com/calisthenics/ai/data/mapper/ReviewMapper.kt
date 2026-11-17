package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AdminReviewDto
import com.calisthenics.ai.data.remote.dto.AdminReviewHistoryDto
import com.calisthenics.ai.data.remote.dto.CoachReviewDto
import com.calisthenics.ai.data.remote.dto.CoachReviewPageDto
import com.calisthenics.ai.data.remote.dto.CoachReviewSummaryDto
import com.calisthenics.ai.data.remote.dto.MyReviewStatusDto
import com.calisthenics.ai.data.remote.dto.ReviewRequestDto
import com.calisthenics.ai.domain.model.AdminReview
import com.calisthenics.ai.domain.model.AdminReviewHistoryEntry
import com.calisthenics.ai.domain.model.CoachReview
import com.calisthenics.ai.domain.model.MyReviewStatus
import com.calisthenics.ai.domain.model.RatingSummary
import com.calisthenics.ai.domain.model.ReviewDraft
import com.calisthenics.ai.domain.model.ReviewHistoryType
import com.calisthenics.ai.domain.model.ReviewModerationStatus
import com.calisthenics.ai.domain.model.ReviewPage

fun CoachReviewSummaryDto.toDomain() = RatingSummary(
    coachId = coachId,
    averageRating = averageRating,
    totalReviews = totalReviews ?: 0L,
    distribution = (5 downTo 1).associateWith { star -> distribution?.get(star.toString()) ?: 0 },
    coachingQualityAvg = coachingQualityAvg,
    communicationAvg = communicationAvg,
    planEffectivenessAvg = planEffectivenessAvg,
    supportResponsivenessAvg = supportResponsivenessAvg
)

fun CoachReviewDto.toDomain() = CoachReview(
    id = id,
    overallRating = overallRating,
    coachingQuality = coachingQuality,
    communication = communication,
    planEffectiveness = planEffectiveness,
    supportResponsiveness = supportResponsiveness,
    comment = comment,
    reviewerName = reviewerName.orEmpty(),
    reviewerAvatarUrl = reviewerAvatarUrl,
    mine = mine ?: false,
    edited = edited ?: false,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CoachReviewPageDto.toDomain(): ReviewPage {
    val items = content.orEmpty().map { it.toDomain() }
    return ReviewPage(
        content = items,
        page = page ?: 0,
        size = size ?: items.size,
        totalElements = totalElements ?: items.size.toLong(),
        totalPages = totalPages ?: 1,
        hasNext = hasNext ?: false
    )
}

fun MyReviewStatusDto.toDomain() = MyReviewStatus(
    eligible = eligible ?: false,
    reason = reason,
    review = review?.toDomain()
)

fun ReviewDraft.toRequestDto() = ReviewRequestDto(
    overallRating = overallRating,
    coachingQuality = coachingQuality,
    communication = communication,
    planEffectiveness = planEffectiveness,
    supportResponsiveness = supportResponsiveness,
    comment = comment.trim().takeIf { it.isNotEmpty() }
)

fun AdminReviewHistoryDto.toDomain() = AdminReviewHistoryEntry(
    type = ReviewHistoryType.fromApi(type),
    actorId = actorId,
    overallRating = overallRating,
    comment = comment,
    note = note,
    createdAt = createdAt
)

fun AdminReviewDto.toDomain() = AdminReview(
    id = id,
    coachId = coachId,
    coachName = coachName.orEmpty(),
    studentId = studentId,
    studentName = studentName.orEmpty(),
    overallRating = overallRating,
    coachingQuality = coachingQuality,
    communication = communication,
    planEffectiveness = planEffectiveness,
    supportResponsiveness = supportResponsiveness,
    comment = comment,
    status = ReviewModerationStatus.fromApi(status),
    moderationReason = moderationReason,
    moderatedByAdminId = moderatedByAdminId,
    moderatedAt = moderatedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    history = history.orEmpty().map { it.toDomain() }
)
