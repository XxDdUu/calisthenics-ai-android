package com.calisthenics.ai.ui.admin

import com.calisthenics.ai.domain.model.*
import com.calisthenics.ai.domain.repository.ExerciseRepository
import com.calisthenics.ai.domain.repository.GettingStartedRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminGettingStartedViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var gettingStartedRepository: FakeAdminGettingStartedRepository
    private lateinit var exerciseRepository: FakeExerciseRepository
    private lateinit var viewModel: AdminGettingStartedViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        gettingStartedRepository = FakeAdminGettingStartedRepository()
        exerciseRepository = FakeExerciseRepository()
        viewModel = AdminGettingStartedViewModel(gettingStartedRepository, exerciseRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadData populates available exercises and draft`() = runTest {
        exerciseRepository.exercises = listOf(
            Exercise(
                id = 1L,
                name = "Push-ups",
                code = "push_ups",
                category = ExerciseCategory.PUSH,
                difficulty = DifficultyLevel.BEGINNER,
                description = "Push up",
                targetMuscles = "Chest, triceps",
                instructions = "Push up"
            )
        )
        viewModel.loadData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.availableExercises.size)
        assertEquals("Push-ups", state.availableExercises[0].name)
        assertEquals(3, state.draft.days.size)
    }

    @Test
    fun `addOrUpdateExercise adds exercise to day`() = runTest {
        viewModel.selectDay(0) // Day 1
        val exercise = AdminExerciseDraft(
            exerciseName = "Push-ups",
            sets = 3,
            reps = 10,
            restSeconds = 60,
            instructions = "Tight core"
        )

        viewModel.addOrUpdateExercise(dayIndex = 0, exercise = exercise, existingIndex = null)

        val day1Exercises = viewModel.uiState.value.draft.days[0].exercises
        assertEquals(1, day1Exercises.size)
        assertEquals("Push-ups", day1Exercises[0].exerciseName)
        assertEquals(3, day1Exercises[0].sets)
    }

    @Test
    fun `removeExercise removes exercise and reindexes displayOrder`() = runTest {
        val ex1 = AdminExerciseDraft(exerciseName = "Ex1", displayOrder = 0)
        val ex2 = AdminExerciseDraft(exerciseName = "Ex2", displayOrder = 1)
        viewModel.addOrUpdateExercise(0, ex1, null)
        viewModel.addOrUpdateExercise(0, ex2, null)

        assertEquals(2, viewModel.uiState.value.draft.days[0].exercises.size)

        viewModel.removeExercise(0, 0)

        val remaining = viewModel.uiState.value.draft.days[0].exercises
        assertEquals(1, remaining.size)
        assertEquals("Ex2", remaining[0].exerciseName)
        assertEquals(0, remaining[0].displayOrder)
    }

    @Test
    fun `saveProgram rejects if any day has empty exercises`() = runTest {
        viewModel.updateProgramTitle("Test Program")
        // Day 1, 2, 3 have no exercises
        viewModel.saveProgram()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("must have at least one exercise"))
    }

    @Test
    fun `updateTargetAudience updates draft target audience`() = runTest {
        viewModel.updateTargetAudience(TargetAudience.LAST_14_DAYS)
        assertEquals(TargetAudience.LAST_14_DAYS, viewModel.uiState.value.draft.targetAudience)

        viewModel.updateTargetAudience(TargetAudience.LAST_30_DAYS)
        assertEquals(TargetAudience.LAST_30_DAYS, viewModel.uiState.value.draft.targetAudience)
    }

    @Test
    fun `saveProgram succeeds when all 3 days have exercises`() = runTest {
        viewModel.updateProgramTitle("Valid Program")
        viewModel.updateTargetAudience(TargetAudience.LAST_30_DAYS)
        for (i in 0..2) {
            viewModel.addOrUpdateExercise(i, AdminExerciseDraft(exerciseName = "Exercise $i"), null)
        }

        viewModel.saveProgram()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals("Valid Program", gettingStartedRepository.savedDraft?.title)
        assertEquals(TargetAudience.LAST_30_DAYS, gettingStartedRepository.savedDraft?.targetAudience)
        assertNotNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun `publishProgram calls repository and updates state`() = runTest {
        gettingStartedRepository.currentProgram = GettingStartedProgram(
            id = 1L,
            title = "Program",
            published = false,
            version = 1
        )

        viewModel.publishProgram()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.program?.published == true)
        assertTrue(gettingStartedRepository.publishCalled)
    }

    @Test
    fun `unpublishProgram calls repository and updates state`() = runTest {
        gettingStartedRepository.currentProgram = GettingStartedProgram(
            id = 1L,
            title = "Program",
            published = true,
            version = 1
        )

        viewModel.unpublishProgram()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.program?.published == true)
        assertTrue(gettingStartedRepository.unpublishCalled)
    }

    @Test
    fun `deleteProgram clears state`() = runTest {
        gettingStartedRepository.currentProgram = GettingStartedProgram(
            id = 1L,
            title = "Program",
            published = true,
            version = 1
        )

        viewModel.deleteProgram()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.program)
        assertTrue(gettingStartedRepository.deleteCalled)
    }

    private class FakeExerciseRepository : ExerciseRepository {
        var exercises = emptyList<Exercise>()
        override suspend fun getExercises(): Result<List<Exercise>> = Result.success(exercises)
        override suspend fun getExerciseById(id: Long): Result<Exercise> =
            Result.success(exercises.first { it.id == id })
        override suspend fun getExerciseByCode(code: String): Result<Exercise> =
            Result.success(exercises.first { it.code == code })
    }

    private class FakeAdminGettingStartedRepository : GettingStartedRepository {
        var currentProgram: GettingStartedProgram? = null
        var savedDraft: AdminProgramDraft? = null
        var publishCalled = false
        var unpublishCalled = false
        var deleteCalled = false

        override suspend fun getAdminProgram(): Result<GettingStartedProgram?> =
            Result.success(currentProgram)

        override suspend fun saveAdminProgram(draft: AdminProgramDraft): Result<GettingStartedProgram> {
            savedDraft = draft
            currentProgram = GettingStartedProgram(id = 1L, title = draft.title, published = false, version = 1)
            return Result.success(currentProgram!!)
        }

        override suspend fun publishProgram(): Result<GettingStartedProgram> {
            publishCalled = true
            currentProgram = currentProgram?.copy(published = true) ?: GettingStartedProgram(id = 1L, title = "Program", published = true)
            return Result.success(currentProgram!!)
        }

        override suspend fun unpublishProgram(): Result<GettingStartedProgram> {
            unpublishCalled = true
            currentProgram = currentProgram?.copy(published = false) ?: GettingStartedProgram(id = 1L, title = "Program", published = false)
            return Result.success(currentProgram!!)
        }

        override suspend fun deleteProgram(): Result<Unit> {
            deleteCalled = true
            currentProgram = null
            return Result.success(Unit)
        }

        override suspend fun getPublishedProgram(): Result<GettingStartedProgram?> = Result.success(currentProgram)
        override suspend fun updateDismissalStatus(status: OnboardingStatus): Result<GettingStartedProgram> =
            Result.success(currentProgram ?: GettingStartedProgram(id = 1L, title = "Program", published = true, userDismissalState = status))
        override suspend fun enroll(): Result<GettingStartedEnrollment> = Result.failure(UnsupportedOperationException())
        override suspend fun completeDay(dayNumber: Int): Result<GettingStartedEnrollment> = Result.failure(UnsupportedOperationException())
        override suspend fun trackCampaignEvent(eventType: String, programId: Long?): Result<Unit> = Result.success(Unit)
    }
}
