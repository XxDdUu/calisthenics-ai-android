package com.calisthenics.ai.ui.exercise

import androidx.lifecycle.SavedStateHandle
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
class ExerciseProgressViewModelTest {

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
    fun loadsExerciseProgressSuccessfully() = runTest {
        val sampleDto = ExerciseProgressDto(
            exerciseId = 10L,
            exerciseName = "Weighted Pull-up",
            exerciseCategory = "PULL",
            totalSessionsCompleted = 5,
            totalSetsCompleted = 15,
            totalRepsCompleted = 120,
            maxWeightKg = 25.0,
            maxReps = 12,
            bestSetSummary = "12 reps @ +25.0 kg",
            lastPerformedDate = "2026-11-01",
            overloadComparison = ProgressiveOverloadComparisonDto(
                metricName = "kg",
                previousValue = 20.0,
                currentValue = 25.0,
                difference = 5.0,
                percentageChange = 25.0,
                improved = true
            ),
            chartData = listOf(
                ProgressChartDataPointDto(date = "2026-10-15", value = 20.0, weightKg = 20.0, reps = 8),
                ProgressChartDataPointDto(date = "2026-11-01", value = 25.0, weightKg = 25.0, reps = 12, isPersonalRecord = true)
            ),
            history = listOf(
                ExerciseHistorySessionDto(
                    sessionId = 101L,
                    sessionTitle = "Pull Day",
                    workoutDate = "2026-11-01",
                    totalVolume = 300.0,
                    sets = listOf(ExerciseHistorySetDto(setNumber = 1, weightKg = 25.0, reps = 12, completed = true, isPr = true))
                )
            )
        )

        coEvery { analyticsRepository.getExerciseProgress(10L, "30d") } returns Result.success(sampleDto)

        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to 10L))
        val viewModel = ExerciseProgressViewModel(analyticsRepository, savedStateHandle)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.progress)
        assertEquals("Weighted Pull-up", state.progress?.exerciseName)
        assertEquals(ProgressMetricType.WEIGHT, state.selectedMetric)
        assertEquals(2, viewModel.getMetricChartPoints().size)
    }

    @Test
    fun switchesMetricAndFilter() = runTest {
        val sampleDto = ExerciseProgressDto(
            exerciseId = 10L,
            exerciseName = "Push-up",
            exerciseCategory = "PUSH",
            maxReps = 30,
            chartData = listOf(
                ProgressChartDataPointDto(date = "2026-11-01", value = 30.0, reps = 30, totalVolume = 1500.0)
            )
        )

        coEvery { analyticsRepository.getExerciseProgress(10L, "30d") } returns Result.success(sampleDto)
        coEvery { analyticsRepository.getExerciseProgress(10L, "7d") } returns Result.success(sampleDto)

        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to 10L))
        val viewModel = ExerciseProgressViewModel(analyticsRepository, savedStateHandle)

        advanceUntilIdle()

        viewModel.onMetricSelected(ProgressMetricType.VOLUME)
        assertEquals(ProgressMetricType.VOLUME, viewModel.uiState.value.selectedMetric)
        assertEquals(1500.0, viewModel.getMetricChartPoints().first().value, 0.01)

        viewModel.onFilterSelected("7d")
        advanceUntilIdle()
        assertEquals("7d", viewModel.uiState.value.selectedFilter)
    }
}

