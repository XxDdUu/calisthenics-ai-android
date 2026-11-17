package com.calisthenics.ai.ui.coach

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.BodyMetricsRepository
import com.calisthenics.ai.domain.repository.ExerciseRepository
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
class ClientProgressViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val analyticsRepository: AnalyticsRepository = mockk()
    private val exerciseRepository: ExerciseRepository = mockk()
    private val dummyMetrics = CoachClientBodyMetricsDto(clientId = 42L, clientName = "Client")
    private val bodyMetricsRepository: BodyMetricsRepository = mockk {
        coEvery { getCoachClientBodyMetrics(any()) } returns Result.success(dummyMetrics)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { analyticsRepository.getCoachClientPrRecords(any(), any(), any()) } returns Result.success(emptyList())
        coEvery { analyticsRepository.getCoachClientSharedPRs(any()) } returns Result.failure(RuntimeException("Fallback to coachSharedPRs"))
        coEvery { analyticsRepository.getCoachClientPrHistory(any(), any(), any()) } returns Result.success(emptyList())
        coEvery { analyticsRepository.getCoachClientExerciseProgress(any(), any(), any()) } returns Result.success(ExerciseProgressDto())
        coEvery { bodyMetricsRepository.getCoachClientBodyMetrics(any()) } returns Result.success(dummyMetrics)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsClientOverviewAndExerciseProgress() = runTest {
        val clientId = 42L
        val clientOverview = TrainingOverviewDto(
            totalWorkoutsCompleted = 8,
            currentStreakDays = 3,
            totalVolumeKg = 2400.0
        )
        val sampleExercise = Exercise(
            id = 5L,
            name = "Handstand Push-up",
            code = "hspu",
            category = ExerciseCategory.PUSH,
            difficulty = DifficultyLevel.ADVANCED,
            description = "Overhead pressing strength",
            targetMuscles = "Shoulders, Triceps",
            instructions = "Keep core tight"
        )
        val progressDto = ExerciseProgressDto(
            exerciseId = 5L,
            exerciseName = "Handstand Push-up",
            exerciseCategory = "PUSH",
            totalSessionsCompleted = 4,
            chartData = listOf(ProgressChartDataPointDto(date = "2026-11-01", value = 6.0, reps = 6))
        )

        coEvery { analyticsRepository.getCoachClientOverview(clientId) } returns Result.success(clientOverview)
        coEvery { analyticsRepository.getCoachClientPRs(clientId) } returns Result.success(emptyList())
        coEvery { analyticsRepository.getCoachSharedPRs() } returns Result.success(emptyList())
        coEvery { exerciseRepository.getExercises() } returns Result.success(listOf(sampleExercise))
        coEvery { analyticsRepository.getCoachClientExerciseProgress(clientId, 5L, "30d") } returns Result.success(progressDto)

        val savedStateHandle = SavedStateHandle(mapOf("clientId" to clientId))
        val viewModel = ClientProgressViewModel(analyticsRepository, exerciseRepository, bodyMetricsRepository, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.overview)
        assertEquals(8L, state.overview?.totalWorkoutsCompleted)
        assertEquals(5L, state.selectedExerciseId)
        assertEquals("Handstand Push-up", state.exerciseProgress?.exerciseName)
    }

    @Test
    fun submitsFeedbackOnSharedPRSuccessfully() = runTest {
        val clientId = 42L
        val pr = PersonalRecordDto(
            id = 10L,
            exerciseId = 5L,
            exerciseName = "Handstand Push-up",
            recordType = "MAX_REPS",
            recordValue = 6.0,
            formattedValue = "6 reps",
            achievedAt = "2026-11-01"
        )
        val initialShare = SharedPersonalRecordDto(
            id = 100L,
            personalRecord = pr,
            clientUserId = clientId,
            clientName = "Client John",
            coachUserId = 1L,
            coachName = "Coach Dave",
            sharedAt = "2026-11-01"
        )
        val updatedShare = initialShare.copy(
            coachFeedback = "Awesome lockout on set 3!",
            feedbackAt = "2026-11-01T20:00:00Z"
        )

        coEvery { analyticsRepository.getCoachClientOverview(clientId) } returns Result.success(TrainingOverviewDto())
        coEvery { analyticsRepository.getCoachClientPRs(clientId) } returns Result.success(listOf(pr))
        coEvery { analyticsRepository.getCoachSharedPRs() } returns Result.success(listOf(initialShare))
        coEvery { exerciseRepository.getExercises() } returns Result.success(emptyList())
        coEvery { analyticsRepository.addCoachFeedback(100L, "Awesome lockout on set 3!", any()) } returns Result.success(updatedShare)

        val savedStateHandle = SavedStateHandle(mapOf("clientId" to clientId))
        val viewModel = ClientProgressViewModel(analyticsRepository, exerciseRepository, bodyMetricsRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.submitFeedback(100L, "Awesome lockout on set 3!")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.feedbackSubmitting)
        assertEquals("Awesome lockout on set 3!", state.sharedPrs.first().coachFeedback)
        assertEquals("Feedback submitted to client!", state.feedbackSuccessMessage)
    }
}
