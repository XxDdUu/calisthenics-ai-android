package com.calisthenics.ai.ui.workoutplan.list

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.usecase.CreateWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.DeleteWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlansUseCase
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
class WorkoutPlanListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: WorkoutPlanRepository = mockk(relaxed = true)

    private lateinit var getWorkoutPlansUseCase: GetWorkoutPlansUseCase
    private lateinit var createWorkoutPlanUseCase: CreateWorkoutPlanUseCase
    private lateinit var deleteWorkoutPlanUseCase: DeleteWorkoutPlanUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        getWorkoutPlansUseCase = GetWorkoutPlansUseCase(repository)
        createWorkoutPlanUseCase = CreateWorkoutPlanUseCase(repository)
        deleteWorkoutPlanUseCase = DeleteWorkoutPlanUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadPlans_success_populatesPlansInState() = runTest {
        val samplePlans = listOf(
            UserWorkoutPlan(id = 1L, name = "Hypertrophy PPL", splitType = WorkoutSplitType.PPL),
            UserWorkoutPlan(id = 2L, name = "Upper Lower Strength", splitType = WorkoutSplitType.UPPER_LOWER)
        )
        coEvery { repository.getPlans() } returns Result.success(samplePlans)

        val viewModel = WorkoutPlanListViewModel(
            getWorkoutPlansUseCase = getWorkoutPlansUseCase,
            createWorkoutPlanUseCase = createWorkoutPlanUseCase,
            deleteWorkoutPlanUseCase = deleteWorkoutPlanUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.plans.size)
        assertEquals("Hypertrophy PPL", state.plans[0].name)
    }

    @Test
    fun loadPlans_empty_showsEmptyState() = runTest {
        coEvery { repository.getPlans() } returns Result.success(emptyList())

        val viewModel = WorkoutPlanListViewModel(
            getWorkoutPlansUseCase = getWorkoutPlansUseCase,
            createWorkoutPlanUseCase = createWorkoutPlanUseCase,
            deleteWorkoutPlanUseCase = deleteWorkoutPlanUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.plans.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun loadPlans_error_setsErrorMessage() = runTest {
        coEvery { repository.getPlans() } returns Result.failure(RuntimeException("Network error"))

        val viewModel = WorkoutPlanListViewModel(
            getWorkoutPlansUseCase = getWorkoutPlansUseCase,
            createWorkoutPlanUseCase = createWorkoutPlanUseCase,
            deleteWorkoutPlanUseCase = deleteWorkoutPlanUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.plans.isEmpty())
        assertEquals("Network error", state.error)
    }

    @Test
    fun submitCreatePlan_withValidName_callsRepositoryAndReloads() = runTest {
        val newPlan = UserWorkoutPlan(id = 10L, name = "Full Body AI", splitType = WorkoutSplitType.FULL_BODY)
        coEvery { repository.getPlans() } returns Result.success(emptyList())
        coEvery { repository.createPlan(any(), any(), any()) } returns Result.success(newPlan)

        val viewModel = WorkoutPlanListViewModel(
            getWorkoutPlansUseCase = getWorkoutPlansUseCase,
            createWorkoutPlanUseCase = createWorkoutPlanUseCase,
            deleteWorkoutPlanUseCase = deleteWorkoutPlanUseCase
        )
        advanceUntilIdle()

        viewModel.openCreateDialog()
        viewModel.updateCreatePlanName("Full Body AI")
        viewModel.updateCreateSplitType(WorkoutSplitType.FULL_BODY)
        viewModel.submitCreatePlan()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCreateDialogOpen)
        coVerify(exactly = 1) { repository.createPlan("Full Body AI", WorkoutSplitType.FULL_BODY, any()) }
    }

    @Test
    fun submitCreatePlan_withBlankName_setsValidationErrorWithoutCallingRepo() = runTest {
        coEvery { repository.getPlans() } returns Result.success(emptyList())

        val viewModel = WorkoutPlanListViewModel(
            getWorkoutPlansUseCase = getWorkoutPlansUseCase,
            createWorkoutPlanUseCase = createWorkoutPlanUseCase,
            deleteWorkoutPlanUseCase = deleteWorkoutPlanUseCase
        )
        advanceUntilIdle()

        viewModel.openCreateDialog()
        viewModel.updateCreatePlanName("   ")
        viewModel.submitCreatePlan()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCreateDialogOpen)
        assertEquals("Plan name cannot be empty", viewModel.uiState.value.createError)
        coVerify(exactly = 0) { repository.createPlan(any(), any(), any()) }
    }

    @Test
    fun executeDeletePlan_deletesAndReloads() = runTest {
        val plan = UserWorkoutPlan(id = 5L, name = "Old Plan", splitType = WorkoutSplitType.PPL)
        coEvery { repository.getPlans() } returns Result.success(listOf(plan))
        coEvery { repository.deletePlan(5L) } returns Result.success(Unit)

        val viewModel = WorkoutPlanListViewModel(
            getWorkoutPlansUseCase = getWorkoutPlansUseCase,
            createWorkoutPlanUseCase = createWorkoutPlanUseCase,
            deleteWorkoutPlanUseCase = deleteWorkoutPlanUseCase
        )
        advanceUntilIdle()

        viewModel.confirmDeletePlan(plan)
        assertEquals(plan, viewModel.uiState.value.planToDelete)

        viewModel.executeDeletePlan()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.planToDelete)
        coVerify(exactly = 1) { repository.deletePlan(5L) }
    }
}
