package com.calisthenics.ai.ui.review

import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.CoachReview
import com.calisthenics.ai.domain.model.MAX_REVIEW_COMMENT_LENGTH
import com.calisthenics.ai.domain.model.MyReviewStatus
import com.calisthenics.ai.domain.model.RatingSummary
import com.calisthenics.ai.domain.model.ReviewPage
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CoachReviewsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: ReviewRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_populatesSummaryReviewsAndEligibility() = runTest {
        coEvery { repository.getSummary(10L) } returns Result.success(sampleSummary())
        coEvery { repository.getReviews(10L, 0, CoachReviewsViewModel.PAGE_SIZE) } returns Result.success(
            ReviewPage(listOf(sampleReview(1L)), 0, 10, 1L, 1, false)
        )
        coEvery { repository.getMyReview(10L) } returns Result.success(MyReviewStatus(eligible = true))

        val vm = CoachReviewsViewModel(repository)
        vm.load(10L)
        advanceUntilIdle()

        assertEquals(4.5, vm.uiState.value.summary?.averageRating)
        assertEquals(1, vm.uiState.value.reviews.size)
        assertTrue(vm.uiState.value.canRate)
        assertFalse(vm.uiState.value.hasMyReview)
    }

    @Test
    fun submit_createsOrUpdatesReviewAndMapsHttpErrors() = runTest {
        val existing = sampleReview(5L, comment = "Initial")
        coEvery { repository.getSummary(10L) } returns Result.success(sampleSummary())
        coEvery { repository.getReviews(10L, 0, CoachReviewsViewModel.PAGE_SIZE) } returns Result.success(
            ReviewPage(listOf(existing), 0, 10, 1L, 1, false)
        )
        coEvery { repository.getMyReview(10L) } returns Result.success(MyReviewStatus(eligible = true))
        coEvery { repository.createReview(10L, any()) } returns Result.success(existing)
        coEvery { repository.updateMyReview(10L, any()) } returns Result.failure(httpError(429))

        val vm = CoachReviewsViewModel(repository)
        vm.load(10L)
        advanceUntilIdle()

        vm.openSheet()
        assertTrue(vm.uiState.value.isSheetOpen)
        vm.setOverall(5)
        vm.setCriterion(ReviewCriterion.COMMUNICATION, 4)
        vm.setCriterion(ReviewCriterion.COMMUNICATION, 4) // toggles back off
        assertNull(vm.uiState.value.draft.communication)
        vm.setComment("x".repeat(MAX_REVIEW_COMMENT_LENGTH + 20))
        assertEquals(MAX_REVIEW_COMMENT_LENGTH, vm.uiState.value.draft.comment.length)

        vm.submit()
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.createReview(10L, any()) }
        assertEquals(R.string.review_submitted, vm.uiState.value.successMessageRes)

        coEvery { repository.getMyReview(10L) } returns Result.success(MyReviewStatus(eligible = true, review = existing))
        vm.load(10L, force = true)
        advanceUntilIdle()
        vm.openSheet()
        assertEquals("Initial", vm.uiState.value.draft.comment)
        vm.submit()
        advanceUntilIdle()

        assertEquals(R.string.review_error_edit_too_fast, vm.uiState.value.submitError?.res)
    }

    @Test
    fun loadMore_appendsDistinctReviews() = runTest {
        coEvery { repository.getSummary(10L) } returns Result.success(sampleSummary())
        coEvery { repository.getReviews(10L, 0, CoachReviewsViewModel.PAGE_SIZE) } returns Result.success(
            ReviewPage(listOf(sampleReview(1L)), 0, 10, 2L, 2, true)
        )
        coEvery { repository.getReviews(10L, 1, CoachReviewsViewModel.PAGE_SIZE) } returns Result.success(
            ReviewPage(listOf(sampleReview(1L), sampleReview(2L)), 1, 10, 2L, 2, false)
        )
        coEvery { repository.getMyReview(10L) } returns Result.success(MyReviewStatus(eligible = false))

        val vm = CoachReviewsViewModel(repository)
        vm.load(10L)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), vm.uiState.value.reviews.map { it.id })
        assertFalse(vm.uiState.value.hasNext)
    }

    private fun sampleSummary() = RatingSummary(10L, 4.5, 2L, mapOf(5 to 1, 4 to 1, 3 to 0, 2 to 0, 1 to 0), 4.5, null, null, null)
    private fun sampleReview(id: Long, comment: String? = "Nice") = CoachReview(id = id, overallRating = 5, comment = comment, reviewerName = "Student A.")
    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody("application/json".toMediaType())))
}
