package com.calisthenics.ai.ui.admin

import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminReview
import com.calisthenics.ai.domain.model.AdminUserReport
import com.calisthenics.ai.domain.model.ModerationAction
import com.calisthenics.ai.domain.model.ReportReason
import com.calisthenics.ai.domain.model.ReportUserRef
import com.calisthenics.ai.domain.model.ReviewModerationStatus
import com.calisthenics.ai.domain.model.UserReportStatus
import com.calisthenics.ai.domain.model.UserReportSummary
import com.calisthenics.ai.domain.repository.ReportRepository
import com.calisthenics.ai.domain.repository.ReviewRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AdminModerationViewModelsTest {

    private val dispatcher = StandardTestDispatcher()
    private val reviewRepository: ReviewRepository = mockk()
    private val reportRepository: ReportRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun adminReviewsViewModel_requiresReasonWhenHidingAndUpdatesDetail() = runTest {
        val review = sampleAdminReview(ReviewModerationStatus.HIDDEN)
        coEvery { reviewRepository.moderateReview(5L, ReviewModerationStatus.HIDDEN, "Abusive") } returns Result.success(review)

        val vm = AdminReviewsViewModel(reviewRepository)
        vm.moderate(5L, ReviewModerationStatus.HIDDEN, "  ")
        assertEquals(R.string.admin_review_reason_required, vm.uiState.value.moderationError?.res)
        coVerify(exactly = 0) { reviewRepository.moderateReview(any(), any(), any()) }

        vm.moderate(5L, ReviewModerationStatus.HIDDEN, "Abusive")
        advanceUntilIdle()

        assertEquals(ReviewModerationStatus.HIDDEN, vm.uiState.value.detail?.status)
        assertEquals(R.string.admin_review_hidden_done, vm.uiState.value.messageRes)
    }

    @Test
    fun adminUserReportsViewModel_validatesNotesSuspensionDaysAndModeration() = runTest {
        val now = Instant.parse("2026-11-13T00:00:00Z")
        assertNull(suspensionUntil("0", now))
        assertNull(suspensionUntil("abc", now))
        assertEquals("2026-11-20T00:00:00Z", suspensionUntil("7", now))

        val report = sampleReport(UserReportStatus.UNDER_REVIEW)
        val summary = UserReportSummary(5, 2, 2, 1, 0)
        coEvery { reportRepository.getSummary() } returns Result.success(summary)
        coEvery { reportRepository.getReports(null, null, 0, 20) } returns Result.success(
            AdminPage(listOf(report), 0, 20, 1L, 1, false)
        )
        coEvery { reportRepository.updateStatus(9L, UserReportStatus.UNDER_REVIEW, "Checking") } returns Result.success(report)
        coEvery { reportRepository.addAction(9L, ModerationAction.TEMPORARY_SUSPENSION, any(), "7d") } returns Result.success(report)

        val vm = AdminUserReportsViewModel(reportRepository)
        vm.refreshAll()
        advanceUntilIdle()
        assertEquals(5L, vm.uiState.value.summary?.total)

        vm.addNote(9L, "  ")
        assertEquals(R.string.admin_report_note_required, vm.uiState.value.actionError?.res)

        vm.addAction(9L, ModerationAction.TEMPORARY_SUSPENSION, "0", "Invalid")
        assertEquals(R.string.admin_report_suspension_days_invalid, vm.uiState.value.actionError?.res)

        vm.updateStatus(9L, UserReportStatus.UNDER_REVIEW, "Checking")
        advanceUntilIdle()
        assertEquals(R.string.admin_report_status_updated, vm.uiState.value.messageRes)

        vm.addAction(9L, ModerationAction.TEMPORARY_SUSPENSION, "7", "7d")
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.detail)
        assertEquals(R.string.admin_report_action_recorded, vm.uiState.value.messageRes)
    }

    private fun sampleAdminReview(status: ReviewModerationStatus) = AdminReview(
        id = 5L, coachId = 2L, coachName = "Coach", studentId = 3L, studentName = "Student",
        overallRating = 2, coachingQuality = null, communication = null, planEffectiveness = null,
        supportResponsiveness = null, comment = "Bad", status = status, moderationReason = "Abusive",
        moderatedByAdminId = 1L, moderatedAt = null, createdAt = null, updatedAt = null
    )

    private fun sampleReport(status: UserReportStatus) = AdminUserReport(
        id = 9L,
        reason = ReportReason.HARASSMENT,
        status = status,
        reporter = ReportUserRef(10L, "Reporter", null),
        reportedUser = ReportUserRef(11L, "Reported", null),
        createdAt = null,
        updatedAt = null
    )
}
