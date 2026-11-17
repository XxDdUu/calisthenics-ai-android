package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AdminUserReportDto
import com.calisthenics.ai.data.remote.dto.CoachReviewSummaryDto
import com.calisthenics.ai.data.remote.dto.PreviousUserReportDto
import com.calisthenics.ai.data.remote.dto.ReportUserRefDto
import com.calisthenics.ai.data.remote.dto.UserReportActionDto
import com.calisthenics.ai.domain.model.CoachReview
import com.calisthenics.ai.domain.model.MAX_REPORT_DESCRIPTION_LENGTH
import com.calisthenics.ai.domain.model.MAX_REPORT_EVIDENCE
import com.calisthenics.ai.domain.model.ModerationAction
import com.calisthenics.ai.domain.model.ReportReason
import com.calisthenics.ai.domain.model.ReviewDraft
import com.calisthenics.ai.domain.model.ReviewModerationStatus
import com.calisthenics.ai.domain.model.UserReportActionType
import com.calisthenics.ai.domain.model.UserReportDraft
import com.calisthenics.ai.domain.model.UserReportStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewAndReportMapperTest {

    @Test
    fun ratingSummary_mapsAllFiveStarsAndFractions() {
        val summary = CoachReviewSummaryDto(
            coachId = 42L,
            averageRating = 4.5,
            totalReviews = 4L,
            distribution = mapOf("5" to 2, "4" to 2),
            coachingQualityAvg = 4.5
        ).toDomain()

        assertEquals(listOf(5, 4, 3, 2, 1), summary.distribution.keys.toList())
        assertEquals(0.5f, summary.fractionFor(5), 0.0001f)
        assertEquals(0.0f, summary.fractionFor(1), 0.0001f)
        assertTrue(summary.hasCriteria)
    }

    @Test
    fun reviewDraft_validatesBoundsAndPrefillsExistingReview() {
        assertFalse(ReviewDraft().isValid)
        assertFalse(ReviewDraft(overallRating = 6).isValid)
        assertTrue(ReviewDraft(overallRating = 5, coachingQuality = 4).isValid)

        val prefilled = ReviewDraft.from(
            CoachReview(id = 7L, overallRating = 4, communication = 5, comment = "Solid")
        )
        assertEquals(4, prefilled.overallRating)
        assertEquals(5, prefilled.communication)
        assertEquals("Solid", prefilled.comment)
        assertEquals(ReviewModerationStatus.VISIBLE, ReviewModerationStatus.fromApi("unknown"))
    }

    @Test
    fun userReportDraft_validatesAndTrimsPayload() {
        assertFalse(UserReportDraft().isValid)
        assertFalse(
            UserReportDraft(
                reason = ReportReason.OTHER,
                description = "x".repeat(MAX_REPORT_DESCRIPTION_LENGTH + 1)
            ).isValid
        )
        assertFalse(
            UserReportDraft(
                reason = ReportReason.SPAM_OR_SCAM,
                evidenceUrls = List(MAX_REPORT_EVIDENCE + 1) { "url/$it" }
            ).isValid
        )

        val request = UserReportDraft(reason = ReportReason.HARASSMENT, description = "   ").toRequestDto()
        assertEquals("HARASSMENT", request.reason)
        assertNull(request.description)
        assertNull(request.evidenceUrls)
    }

    @Test
    fun adminUserReportDto_mapsNestedEntitiesAndUnknownEnumsSafely() {
        val report = AdminUserReportDto(
            id = 99L,
            reason = "FRAUD",
            status = "UNDER_REVIEW",
            reporter = ReportUserRefDto(1L, "Alice", "a@test.com"),
            reportedUser = ReportUserRefDto(2L, "Bob", "b@test.com"),
            evidenceUrls = listOf("/api/v1/storage/files/e1.jpg"),
            previousReports = listOf(PreviousUserReportDto(10L, "FUTURE_REASON", "PENDING", null)),
            actions = listOf(
                UserReportActionDto(
                    id = 5L,
                    type = "MODERATION_ACTION",
                    moderationAction = "WARNING",
                    note = "First warning"
                )
            )
        ).toDomain()

        assertEquals(ReportReason.FRAUD, report.reason)
        assertEquals(UserReportStatus.UNDER_REVIEW, report.status)
        assertEquals("Alice", report.reporter.fullName)
        assertNull(report.previousReports.single().reason)
        assertEquals(UserReportActionType.MODERATION_ACTION, report.actions.single().type)
        assertEquals(ModerationAction.WARNING, report.actions.single().moderationAction)
    }
}
