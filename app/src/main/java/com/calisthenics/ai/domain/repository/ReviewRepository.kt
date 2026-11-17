package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminReview
import com.calisthenics.ai.domain.model.CoachReview
import com.calisthenics.ai.domain.model.MyReviewStatus
import com.calisthenics.ai.domain.model.RatingSummary
import com.calisthenics.ai.domain.model.ReviewDraft
import com.calisthenics.ai.domain.model.ReviewModerationStatus
import com.calisthenics.ai.domain.model.ReviewPage

interface ReviewRepository {
    suspend fun getSummary(coachId: Long): Result<RatingSummary>
    suspend fun getReviews(coachId: Long, page: Int, size: Int): Result<ReviewPage>
    suspend fun getMyReview(coachId: Long): Result<MyReviewStatus>
    suspend fun createReview(coachId: Long, draft: ReviewDraft): Result<CoachReview>
    suspend fun updateMyReview(coachId: Long, draft: ReviewDraft): Result<CoachReview>

    suspend fun getAdminReviews(
        status: ReviewModerationStatus?,
        coachId: Long?,
        page: Int,
        size: Int
    ): Result<AdminPage<AdminReview>>
    suspend fun getAdminReview(id: Long): Result<AdminReview>
    suspend fun moderateReview(id: Long, status: ReviewModerationStatus, reason: String?): Result<AdminReview>
}
