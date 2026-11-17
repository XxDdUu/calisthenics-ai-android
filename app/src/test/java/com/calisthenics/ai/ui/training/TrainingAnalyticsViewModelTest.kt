package com.calisthenics.ai.ui.training

import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import io.mockk.coEvery
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingAnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val analyticsRepository: AnalyticsRepository = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsOverviewAndInsightsSuccessfully() = runTest {
        val overview = TrainingOverviewDto(
            totalWorkoutsCompleted = 12,
            currentStreakDays = 4,
            longestStreakDays = 7,
            activeDaysThisMonth = 10,
            totalVolumeKg = 3200.0,
            totalExercisesTracked = 8,
            totalPersonalRecords = 3,
            recentPRs = listOf(
                PersonalRecordDto(
                    id = 1L,
                    exerciseId = 5L,
                    exerciseName = "Weighted Dip",
                    recordType = "MAX_WEIGHT",
                    recordValue = 40.0,
                    formattedValue = "+40.0 kg",
                    achievedAt = "2026-11-01"
                )
            )
        )

        val weekly = WeeklyAnalyticsDto(
            startDate = "2026-10-26",
            endDate = "2026-11-01",
            totalWorkouts = 4,
            totalSets = 16,
            totalReps = 128,
            totalVolumeKg = 1200.0,
            totalDurationSeconds = 3600,
            activeDays = 4
        )

        val insights = TrainingInsightsDto(
            currentStreakDays = 4,
            longestStreakDays = 7,
            weeklyFrequency = 4.0,
            activeDaysCount = 10,
            insights = listOf("Your training volume increased by 15% this week.")
        )

        coEvery { analyticsRepository.getTrainingOverview() } returns Result.success(overview)
        coEvery { analyticsRepository.getWeeklyAnalytics() } returns Result.success(weekly)
        coEvery { analyticsRepository.getMonthlyAnalytics() } returns Result.success(MonthlyAnalyticsDto(yearMonth = "2026-11"))
        coEvery { analyticsRepository.getTrainingInsights() } returns Result.success(insights)
        coEvery { analyticsRepository.getClientSharedPRs() } returns Result.success(emptyList())

        val viewModel = TrainingAnalyticsViewModel(analyticsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.overview)
        assertEquals(4, state.overview?.currentStreakDays)
        assertEquals(1, state.insights?.insights?.size)
    }

    @Test
    fun sharesPersonalRecordWithCoachSuccessfully() = runTest {
        val pr = PersonalRecordDto(
            id = 50L,
            exerciseId = 2L,
            exerciseName = "Muscle-up",
            recordType = "MAX_REPS",
            recordValue = 10.0,
            formattedValue = "10 reps",
            achievedAt = "2026-11-01"
        )

        val shared = SharedPersonalRecordDto(
            id = 100L,
            personalRecord = pr,
            clientUserId = 1L,
            clientName = "Client",
            coachUserId = 2L,
            coachName = "Coach Dave",
            sharedAt = "2026-11-01"
        )

        coEvery { analyticsRepository.getTrainingOverview() } returns Result.success(TrainingOverviewDto())
        coEvery { analyticsRepository.getWeeklyAnalytics() } returns Result.success(WeeklyAnalyticsDto(startDate = "", endDate = ""))
        coEvery { analyticsRepository.getMonthlyAnalytics() } returns Result.success(MonthlyAnalyticsDto(yearMonth = ""))
        coEvery { analyticsRepository.getTrainingInsights() } returns Result.success(TrainingInsightsDto())
        coEvery { analyticsRepository.getClientSharedPRs() } returns Result.success(emptyList())
        coEvery { analyticsRepository.sharePersonalRecord(50L, any(), any()) } returns Result.success(shared)

        val viewModel = TrainingAnalyticsViewModel(analyticsRepository)
        advanceUntilIdle()

        viewModel.shareRecordWithCoach(50L, recipientId = 2L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.sharedPrs.size)
        assertEquals(100L, state.sharedPrs.first().id)
        assertNotNull(state.shareSuccessMessage)
    }
}

