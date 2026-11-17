package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.AdminPageDto
import com.calisthenics.ai.data.remote.dto.AdminReviewDto
import com.calisthenics.ai.data.remote.dto.CoachReviewDto
import com.calisthenics.ai.data.remote.dto.CoachReviewPageDto
import com.calisthenics.ai.data.remote.dto.CoachReviewSummaryDto
import com.calisthenics.ai.data.remote.dto.ModerateReviewRequestDto
import com.calisthenics.ai.data.remote.dto.MyReviewStatusDto
import com.calisthenics.ai.data.remote.dto.ReviewRequestDto
import javax.inject.Inject

class RemoteReviewDataSource @Inject constructor(
    private val api: CalisthenicsApi
) {
    suspend fun getSummary(coachId: Long): CoachReviewSummaryDto = api.getCoachReviewSummary(coachId)

    suspend fun getReviews(coachId: Long, page: Int, size: Int): CoachReviewPageDto =
        api.getCoachReviews(coachId, page, size)

    suspend fun getMyReview(coachId: Long): MyReviewStatusDto = api.getMyCoachReview(coachId)

    suspend fun createReview(coachId: Long, request: ReviewRequestDto): CoachReviewDto =
        api.createCoachReview(coachId, request)

    suspend fun updateMyReview(coachId: Long, request: ReviewRequestDto): CoachReviewDto =
        api.updateMyCoachReview(coachId, request)

    suspend fun getAdminReviews(status: String?, coachId: Long?, page: Int, size: Int): AdminPageDto<AdminReviewDto> =
        api.getAdminReviews(status, coachId, page, size)

    suspend fun getAdminReview(id: Long): AdminReviewDto = api.getAdminReview(id)

    suspend fun moderate(id: Long, status: String, reason: String?): AdminReviewDto =
        api.moderateAdminReview(id, ModerateReviewRequestDto(status, reason))
}
