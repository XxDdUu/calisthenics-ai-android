package com.calisthenics.ai.ui.workoutplan.details

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.repository.ExerciseRepository
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.usecase.AddExerciseToPlanDayUseCase
import com.calisthenics.ai.domain.usecase.GetExercisesUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlanDayExercisesUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlanDetailsUseCase
import com.calisthenics.ai.domain.usecase.RemoveExerciseFromPlanDayUseCase
import com.calisthenics.ai.domain.usecase.UpdatePlanDayExerciseUseCase
import com.calisthenics.ai.domain.usecase.UpdatePlanDayExercisesUseCase
import com.calisthenics.ai.domain.usecase.UpdateWorkoutPlanUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutPlanDetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val workoutPlanRepository: WorkoutPlanRepository = mockk(relaxed = true)
    private val exerciseRepository: ExerciseRepository = mockk(relaxed = true)

    private val sampleExercise1 = Exercise(
        id = 10L,
        name = "Parallel Bar Dips",
        code = "DIPS",
        category = ExerciseCategory.PUSH,
        difficulty = DifficultyLevel.BEGINNER,
        description = "Chest and triceps dip",
        targetMuscles = "Triceps, Chest",
        instructions = "Lock out at top"
    )

    private val sampleExercise2 = Exercise(
        id = 20L,
        name = "Pull-Ups",
        code = "PULL_UPS",
        category = ExerciseCategory.PULL,
        difficulty = DifficultyLevel.BEGINNER,
        description = "Standard bodyweight pull-up",
        targetMuscles = "Lats, Biceps",
        instructions = "Chin over bar"
    )

    private val mondayDay = UserWorkoutPlanDay(
        id = 101L,
        dayOfWeek = WorkoutDayOfWeek.MONDAY,
        sessionType = WorkoutSessionType.PUSH,
        exercises = listOf(
            UserWorkoutPlanExercise(
                id = 1001L,
                exercise = sampleExercise1,
                sets = 4,
                reps = 10,
                durationSeconds = null,
                restSeconds = 90,
                displayOrder = 1
            )
        )
    )

    private val tuesdayDay = UserWorkoutPlanDay(
        id = 102L,
        dayOfWeek = WorkoutDayOfWeek.TUESDAY,
        sessionType = WorkoutSessionType.PULL,
        exercises = listOf(
            UserWorkoutPlanExercise(
                id = 1002L,
                exercise = sampleExercise2,
                sets = 4,
                reps = 8,
                durationSeconds = null,
                restSeconds = 120,
                displayOrder = 1
            )
        )
    )

    private val samplePlan = UserWorkoutPlan(
        id = 42L,
        name = "PPL Foundation",
        splitType = WorkoutSplitType.PPL,
        days = listOf(mondayDay, tuesdayDay)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { exerciseRepository.getExercises() } returns Result.success(listOf(sampleExercise1, sampleExercise2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(planId: Long = 42L): WorkoutPlanDetailsViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("planId" to planId.toString()))
        return WorkoutPlanDetailsViewModel(
            savedStateHandle = savedStateHandle,
            getWorkoutPlanDetailsUseCase = GetWorkoutPlanDetailsUseCase(workoutPlanRepository),
            getWorkoutPlanDayExercisesUseCase = GetWorkoutPlanDayExercisesUseCase(workoutPlanRepository),
            addExerciseToPlanDayUseCase = AddExerciseToPlanDayUseCase(workoutPlanRepository),
            updatePlanDayExerciseUseCase = UpdatePlanDayExerciseUseCase(workoutPlanRepository),
            updatePlanDayExercisesUseCase = UpdatePlanDayExercisesUseCase(workoutPlanRepository),
            removeExerciseFromPlanDayUseCase = RemoveExerciseFromPlanDayUseCase(workoutPlanRepository),
            updateWorkoutPlanUseCase = UpdateWorkoutPlanUseCase(workoutPlanRepository),
            getExercisesUseCase = GetExercisesUseCase(exerciseRepository)
        )
    }

    @Test
    fun loadPlanDetails_success_populatesStateWithPlanAndDayExercises() = runTest {
        coEvery { workoutPlanRepository.getPlanById(42L) } returns Result.success(samplePlan)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.plan)
        assertEquals(42L, state.plan?.id)
        assertEquals("PPL Foundation", state.plan?.name)

        // Monday exercises
        assertEquals(1, state.currentDayExercises.size)
        assertEquals("Parallel Bar Dips", state.currentDayExercises[0].exercise.name)
    }

    @Test
    fun selectDay_switchesSelectedDayAndFiltersExercisesCorrectly() = runTest {
        coEvery { workoutPlanRepository.getPlanById(42L) } returns Result.success(samplePlan)
        coEvery { workoutPlanRepository.getDayExercises(42L, 102L) } returns Result.success(tuesdayDay.exercises)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Monday is initially selected
        assertEquals(WorkoutDayOfWeek.MONDAY, viewModel.uiState.value.selectedDayOfWeek)
        val mondayExercises = viewModel.uiState.value.currentDayExercises
        assertEquals(1, mondayExercises.size)
        assertEquals("Parallel Bar Dips", mondayExercises[0].exercise.name)

        // Select Tuesday
        viewModel.selectDay(WorkoutDayOfWeek.TUESDAY)
        advanceUntilIdle()

        assertEquals(WorkoutDayOfWeek.TUESDAY, viewModel.uiState.value.selectedDayOfWeek)
        val tuesdayExercises = viewModel.uiState.value.currentDayExercises
        assertEquals(1, tuesdayExercises.size)
        assertEquals("Pull-Ups", tuesdayExercises[0].exercise.name)

        // Verify that Monday's exercise does NOT appear on Tuesday
        assertTrue(tuesdayExercises.none { it.exercise.name == "Parallel Bar Dips" })
    }

    @Test
    fun addExercise_submitsWithCorrectParametersAndUpdatesState() = runTest {
        coEvery { workoutPlanRepository.getPlanById(42L) } returns Result.success(samplePlan)
        val newAssignment = UserWorkoutPlanExercise(
            id = 2001L,
            exercise = sampleExercise1,
            sets = 3,
            reps = 12,
            durationSeconds = null,
            restSeconds = 60,
            displayOrder = 2,
            notes = "Slow eccentric"
        )
        coEvery {
            workoutPlanRepository.addExercise(
                planId = 42L,
                dayId = 101L,
                exerciseId = 10L,
                sets = 3,
                reps = 12,
                durationSeconds = null,
                restSeconds = 60,
                displayOrder = 2,
                notes = "Slow eccentric"
            )
        } returns Result.success(newAssignment)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openAddExerciseSheet()
        viewModel.onExerciseSelectedForAdd(sampleExercise1)
        viewModel.updateSets("3")
        viewModel.updateReps("12")
        viewModel.updateHoldDuration("")
        viewModel.updateRestSeconds("60")
        viewModel.updateNotes("Slow eccentric")

        viewModel.submitExerciseAssignment()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAddExerciseSheetOpen)
        assertNull(viewModel.uiState.value.validationError)

        val mondayExercises = viewModel.uiState.value.currentDayExercises
        assertEquals(2, mondayExercises.size)
        assertEquals(newAssignment, mondayExercises.last())

        coVerify(exactly = 1) {
            workoutPlanRepository.addExercise(
                planId = 42L,
                dayId = 101L,
                exerciseId = 10L,
                sets = 3,
                reps = 12,
                durationSeconds = null,
                restSeconds = 60,
                displayOrder = 2,
                notes = "Slow eccentric"
            )
        }
    }

    @Test
    fun addExercise_validationRejectsZeroSetsOrNegativeRest() = runTest {
        coEvery { workoutPlanRepository.getPlanById(42L) } returns Result.success(samplePlan)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openAddExerciseSheet()
        viewModel.onExerciseSelectedForAdd(sampleExercise1)

        // Test sets = 0
        viewModel.updateSets("0")
        viewModel.submitExerciseAssignment()
        advanceUntilIdle()
        assertEquals("Sets must be greater than zero.", viewModel.uiState.value.validationError)

        // Test rest < 0
        viewModel.updateSets("3")
        viewModel.updateRestSeconds("-10")
        viewModel.submitExerciseAssignment()
        advanceUntilIdle()
        assertEquals("Rest seconds cannot be negative.", viewModel.uiState.value.validationError)

        coVerify(exactly = 0) { workoutPlanRepository.addExercise(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun removeExercise_callsRepositoryAndRemovesFromDayList() = runTest {
        coEvery { workoutPlanRepository.getPlanById(42L) } returns Result.success(samplePlan)
        coEvery { workoutPlanRepository.removeExercise(42L, 101L, 1001L) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val exerciseToRemove = viewModel.uiState.value.currentDayExercises.first()
        viewModel.confirmDeleteExercise(exerciseToRemove)
        assertEquals(exerciseToRemove, viewModel.uiState.value.exerciseToDelete)

        viewModel.executeDeleteExercise()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.exerciseToDelete)
        assertTrue(viewModel.uiState.value.currentDayExercises.isEmpty())
        coVerify(exactly = 1) { workoutPlanRepository.removeExercise(42L, 101L, 1001L) }
    }

    @Test
    fun reorderExercises_swapsPositionsAndUpdatesBatch() = runTest {
        val exerciseA = UserWorkoutPlanExercise(
            id = 1001L,
            exercise = sampleExercise1,
            sets = 4,
            reps = 10,
            durationSeconds = null,
            restSeconds = 90,
            displayOrder = 1
        )
        val exerciseB = UserWorkoutPlanExercise(
            id = 1002L,
            exercise = sampleExercise2,
            sets = 3,
            reps = 8,
            durationSeconds = null,
            restSeconds = 60,
            displayOrder = 2
        )
        val multiDay = mondayDay.copy(exercises = listOf(exerciseA, exerciseB))
        val plan = samplePlan.copy(days = listOf(multiDay))

        coEvery { workoutPlanRepository.getPlanById(42L) } returns Result.success(plan)
        coEvery { workoutPlanRepository.updateDayExercises(42L, 101L, any()) } answers {
            val list = thirdArg<List<UserWorkoutPlanExercise>>()
            Result.success(list)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.currentDayExercises.size)
        assertEquals(1001L, viewModel.uiState.value.currentDayExercises[0].id)
        assertEquals(1002L, viewModel.uiState.value.currentDayExercises[1].id)

        // Move B up to position 1
        viewModel.moveExerciseUp(exerciseB)
        advanceUntilIdle()

        val reordered = viewModel.uiState.value.currentDayExercises
        assertEquals(1002L, reordered[0].id)
        assertEquals(1, reordered[0].displayOrder)
        assertEquals(1001L, reordered[1].id)
        assertEquals(2, reordered[1].displayOrder)

        coVerify(exactly = 1) { workoutPlanRepository.updateDayExercises(42L, 101L, match { it[0].id == 1002L && it[1].id == 1001L }) }
    }

    @Test
    fun addExercise_durationBased_persistsHoldDuration() = runTest {
        coEvery { workoutPlanRepository.getPlanById(42L) } returns Result.success(samplePlan)
        val planche = sampleExercise1.copy(id = 30L, name = "Planche Hold", category = ExerciseCategory.ISOMETRIC)
        val addedAssignment = UserWorkoutPlanExercise(
            id = 1003L,
            exercise = planche,
            sets = 4,
            reps = null,
            durationSeconds = 15,
            restSeconds = 90,
            displayOrder = 2
        )
        coEvery {
            workoutPlanRepository.addExercise(
                planId = 42L,
                dayId = 101L,
                exerciseId = 30L,
                sets = 4,
                reps = null,
                durationSeconds = 15,
                restSeconds = 90,
                displayOrder = 2,
                notes = "Solid protraction"
            )
        } returns Result.success(addedAssignment)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openAddExerciseSheet()
        viewModel.onExerciseSelectedForAdd(planche)
        assertEquals(ExercisePrescriptionType.DURATION, viewModel.uiState.value.prescriptionType)

        viewModel.updateSets("4")
        viewModel.updateHoldDuration("15")
        viewModel.updateRestSeconds("90")
        viewModel.updateNotes("Solid protraction")
        viewModel.submitExerciseAssignment()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.currentDayExercises.size)
        val created = viewModel.uiState.value.currentDayExercises.last()
        assertEquals(15, created.durationSeconds)
        assertNull(created.reps)
    }
}
