package com.calisthenics.ai.ui.pr

import com.calisthenics.ai.data.remote.dto.CreatePrRequestDto
import com.calisthenics.ai.data.remote.dto.PersonalRecordDto
import com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto
import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.ExerciseRepository
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalRecordsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val analyticsRepository: AnalyticsRepository = mockk()
    private val exerciseRepository: ExerciseRepository = mockk()

    private val pullUpExercise = Exercise(
        id = 1L,
        name = "Weighted Pull-up",
        code = "weighted-pull-up",
        category = ExerciseCategory.PULL,
        difficulty = DifficultyLevel.INTERMEDIATE,
        description = "Weighted pull-up",
        targetMuscles = "Lats, Biceps",
        instructions = "Pull until chin is over bar"
    )

    private val pr1 = PersonalRecordDto(
        id = 101L,
        exerciseId = 1L,
        exerciseName = "Weighted Pull-up",
        recordType = "MAX_WEIGHT",
        recordValue = 40.0,
        unit = "kg",
        achievedAt = "2026-11-02T10:00:00Z",
        notes = "Strict form"
    )

    private val pr2 = PersonalRecordDto(
        id = 102L,
        exerciseId = 2L,
        exerciseName = "Front Lever",
        recordType = "MAX_HOLD",
        recordValue = 10.0,
        unit = "seconds",
        achievedAt = "2026-11-01T10:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { analyticsRepository.getPrHistory(any(), any()) } returns Result.success(emptyList())
        coEvery { analyticsRepository.getClientSharedPRs() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsPersonalRecordsAndExercisesOnStart() = runTest {
        coEvery { exerciseRepository.getExercises() } returns Result.success(listOf(pullUpExercise))
        coEvery { analyticsRepository.getUserPersonalRecords() } returns Result.success(listOf(pr1, pr2))
        coEvery { analyticsRepository.getPrHistory(1L) } returns Result.success(listOf(pr1))

        val viewModel = PersonalRecordsViewModel(analyticsRepository, exerciseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.prs.size)
        assertEquals(1, state.exercises.size)
        assertEquals("Weighted Pull-up", state.selectedChartExerciseName)
        assertEquals(2, state.prsGroupedByExercise.size)
    }

    @Test
    fun filtersPrsByType() = runTest {
        coEvery { exerciseRepository.getExercises() } returns Result.success(emptyList())
        coEvery { analyticsRepository.getUserPersonalRecords() } returns Result.success(listOf(pr1, pr2))
        coEvery { analyticsRepository.getPrHistory(any()) } returns Result.success(emptyList())

        val viewModel = PersonalRecordsViewModel(analyticsRepository, exerciseRepository)
        advanceUntilIdle()

        viewModel.selectPrTypeFilter("MAX_WEIGHT")
        assertEquals(1, viewModel.uiState.value.filteredPrs.size)
        assertEquals("MAX_WEIGHT", viewModel.uiState.value.filteredPrs.first().recordType)

        viewModel.selectPrTypeFilter("ALL")
        assertEquals(2, viewModel.uiState.value.filteredPrs.size)
    }

    @Test
    fun createsNewPrSuccessfully() = runTest {
        coEvery { exerciseRepository.getExercises() } returns Result.success(listOf(pullUpExercise))
        coEvery { analyticsRepository.getUserPersonalRecords() } returns Result.success(emptyList())
        coEvery { analyticsRepository.createPersonalRecord(any()) } returns Result.success(pr1)

        val viewModel = PersonalRecordsViewModel(analyticsRepository, exerciseRepository)
        advanceUntilIdle()

        viewModel.savePr(
            exerciseId = 1L,
            exerciseName = "Weighted Pull-up",
            prType = "MAX_WEIGHT",
            value = 40.0,
            unit = "kg",
            achievedAt = "2026-11-02T10:00:00Z",
            notes = "Strict form"
        )
        advanceUntilIdle()

        coVerify {
            analyticsRepository.createPersonalRecord(match {
                it.exerciseId == 1L && it.value == 40.0 && it.prType == "MAX_WEIGHT"
            })
        }
        assertEquals("New PR created!", viewModel.uiState.value.successMessage)
    }

    @Test
    fun createPrWithInvalidValueSetsError() = runTest {
        coEvery { exerciseRepository.getExercises() } returns Result.success(emptyList())
        coEvery { analyticsRepository.getUserPersonalRecords() } returns Result.success(emptyList())

        val viewModel = PersonalRecordsViewModel(analyticsRepository, exerciseRepository)
        advanceUntilIdle()

        viewModel.savePr(
            exerciseId = 1L,
            exerciseName = "Pull-up",
            prType = "MAX_WEIGHT",
            value = 0.0, // invalid value <= 0
            unit = "kg",
            achievedAt = "2026-11-02T10:00:00Z",
            notes = null
        )

        assertEquals("Value must be greater than 0", viewModel.uiState.value.error)
        coVerify(exactly = 0) { analyticsRepository.createPersonalRecord(any()) }
    }

    @Test
    fun updatesExistingPrSuccessfully() = runTest {
        coEvery { exerciseRepository.getExercises() } returns Result.success(emptyList())
        coEvery { analyticsRepository.getUserPersonalRecords() } returns Result.success(listOf(pr1))
        coEvery { analyticsRepository.getPrHistory(any()) } returns Result.success(emptyList())
        coEvery { analyticsRepository.updatePersonalRecord(101L, any()) } returns Result.success(pr1.copy(recordValue = 42.5))

        val viewModel = PersonalRecordsViewModel(analyticsRepository, exerciseRepository)
        advanceUntilIdle()

        viewModel.openEditPrForm(pr1)
        assertTrue(viewModel.uiState.value.isFormOpen)
        assertNotNull(viewModel.uiState.value.editingPr)

        viewModel.savePr(
            exerciseId = 1L,
            exerciseName = "Weighted Pull-up",
            prType = "MAX_WEIGHT",
            value = 42.5,
            unit = "kg",
            achievedAt = "2026-11-02T10:00:00Z",
            notes = "Upgraded"
        )
        advanceUntilIdle()

        coVerify {
            analyticsRepository.updatePersonalRecord(101L, match {
                it.value == 42.5
            })
        }
        assertEquals("PR updated successfully!", viewModel.uiState.value.successMessage)
    }

    @Test
    fun deletesPrAfterConfirmation() = runTest {
        coEvery { exerciseRepository.getExercises() } returns Result.success(emptyList())
        coEvery { analyticsRepository.getUserPersonalRecords() } returns Result.success(listOf(pr1))
        coEvery { analyticsRepository.getPrHistory(any()) } returns Result.success(emptyList())
        coEvery { analyticsRepository.deletePersonalRecord(101L) } returns Result.success(Unit)

        val viewModel = PersonalRecordsViewModel(analyticsRepository, exerciseRepository)
        advanceUntilIdle()

        viewModel.promptDelete(pr1)
        assertEquals(pr1, viewModel.uiState.value.deletingPr)

        viewModel.confirmDelete()
        advanceUntilIdle()

        coVerify { analyticsRepository.deletePersonalRecord(101L) }
        assertEquals("PR deleted successfully", viewModel.uiState.value.successMessage)
    }

    @Test
    fun sharesPrWithSelectedRecipientAndClosesModal() = runTest {
        coEvery { exerciseRepository.getExercises() } returns Result.success(emptyList())
        coEvery { analyticsRepository.getUserPersonalRecords() } returns Result.success(listOf(pr1))
        coEvery { analyticsRepository.getPrHistory(any()) } returns Result.success(emptyList())
        coEvery {
            analyticsRepository.sharePersonalRecord(101L, 20L, any())
        } returns Result.success(SharedPersonalRecordDto(id = 55L, coachUserId = 20L, coachName = "Coach Sarah"))

        val viewModel = PersonalRecordsViewModel(analyticsRepository, exerciseRepository)
        advanceUntilIdle()

        viewModel.openShareSheet(pr1)
        assertEquals(pr1, viewModel.uiState.value.sharingPr)

        viewModel.shareWithCoach(101L, recipientId = 20L, clientMessageId = "test-msg-id")
        advanceUntilIdle()

        coVerify { analyticsRepository.sharePersonalRecord(101L, 20L, "test-msg-id") }
        assertNull(viewModel.uiState.value.sharingPr)
        assertTrue(viewModel.uiState.value.successMessage.orEmpty().contains("Coach Sarah"))
    }
}

