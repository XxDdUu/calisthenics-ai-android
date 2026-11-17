package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.mapper.toRequestDto
import com.calisthenics.ai.data.remote.datasource.RemoteReviewDataSource
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminReview
import com.calisthenics.ai.domain.model.CoachReview
import com.calisthenics.ai.domain.model.MyReviewStatus
import com.calisthenics.ai.domain.model.RatingSummary
import com.calisthenics.ai.domain.model.ReviewDraft
import com.calisthenics.ai.domain.model.ReviewModerationStatus
import com.calisthenics.ai.domain.model.ReviewPage
import com.calisthenics.ai.domain.repository.ReviewRepository
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val remote: RemoteReviewDataSource
) : ReviewRepository {

    override suspend fun getSummary(coachId: Long): Result<RatingSummary> = runCatching {
        remote.getSummary(coachId).toDomain()
    }

    override suspend fun getReviews(coachId: Long, page: Int, size: Int): Result<ReviewPage> = runCatching {
        remote.getReviews(coachId, page, size).toDomain()
    }

    override suspend fun getMyReview(coachId: Long): Result<MyReviewStatus> = runCatching {
        remote.getMyReview(coachId).toDomain()
    }

    override suspend fun createReview(coachId: Long, draft: ReviewDraft): Result<CoachReview> = runCatching {
        remote.createReview(coachId, draft.toRequestDto()).toDomain()
    }

    override suspend fun updateMyReview(coachId: Long, draft: ReviewDraft): Result<CoachReview> = runCatching {
        remote.updateMyReview(coachId, draft.toRequestDto()).toDomain()
    }

    override suspend fun getAdminReviews(
        status: ReviewModerationStatus?,
        coachId: Long?,
        page: Int,
        size: Int
    ): Result<AdminPage<AdminReview>> = runCatching {
        remote.getAdminReviews(status?.name, coachId, page, size).toDomain { it.toDomain() }
    }

    override suspend fun getAdminReview(id: Long): Result<AdminReview> = runCatching {
        remote.getAdminReview(id).toDomain()
    }

    override suspend fun moderateReview(
        id: Long,
        status: ReviewModerationStatus,
        reason: String?
    ): Result<AdminReview> = runCatching {
        remote.moderate(id, status.name, reason?.trim()?.takeIf { it.isNotEmpty() }).toDomain()
    }
}
