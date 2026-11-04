package com.calisthenics.ai.ui.today

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutPlanSnapshot
import com.calisthenics.ai.domain.model.TodayWorkoutCandidate
import com.calisthenics.ai.domain.model.TodayWorkoutResolution
import com.calisthenics.ai.domain.model.WorkoutSource
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import com.calisthenics.ai.domain.usecase.GetWorkoutPlanDetailsUseCase
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayWorkoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository: WorkoutPlanRepository = mockk()
    private val sessionRepository: WorkoutSessionRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { sessionRepository.getActiveSessions() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun planLoadFailureIsReportedAndCanBeRetried() = runTest {
        coEvery { repository.resolveTodayWorkout(any(), null) } returnsMany listOf(
            Result.failure(IllegalStateException("Network unavailable")),
            Result.success(resolution(emptyList(), WorkoutSource.NONE, null))
        )
        coEvery { repository.getActivePlanId() } returns Result.success(null)
        val viewModel = createViewModel()

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network unavailable", viewModel.uiState.value.error)

        viewModel.refresh()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(TodayWorkoutKind.NO_PLANS, viewModel.uiState.value.selection.kind)
        coVerify(exactly = 2) { repository.resolveTodayWorkout(any(), null) }
    }

    @Test
    fun multiplePlansRequireChoiceAndSelectedPlanIsLoaded() = runTest {
        val plans = listOf(plan(1L, "First"), plan(2L, "Second"))
        coEvery { repository.resolveTodayWorkout(any(), null) } returns Result.success(
            resolution(plans, WorkoutSource.PERSONAL, null)
        )
        coEvery { repository.getActivePlanId() } returns Result.success(null)
        coEvery { repository.setActivePlanId(2L) } returns Result.success(Unit)
        coEvery { repository.getPlanById(2L) } returns Result.success(plans[1])
        coEvery { repository.getSnapshot() } returns Result.success(WorkoutPlanSnapshot(null, emptyList()))

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(TodayWorkoutKind.PLAN_SELECTION_REQUIRED, viewModel.uiState.value.selection.kind)

        viewModel.selectPlan(2L)
        advanceUntilIdle()

        assertEquals(2L, viewModel.uiState.value.selection.plan?.id)
        assertEquals(TodayWorkoutKind.SCHEDULED, viewModel.uiState.value.selection.kind)
        coVerify(exactly = 1) { repository.setActivePlanId(2L) }
    }

    @Test
    fun coachAssignedResolutionIsDisplayedAsCoachSource() = runTest {
        val assigned = plan(9L, "Coach workout")
        coEvery { repository.resolveTodayWorkout(any(), null) } returns Result.success(
            resolution(listOf(assigned), WorkoutSource.COACH, assigned.id, assignmentId = 32L)
        )
        coEvery { repository.getActivePlanId() } returns Result.success(null)
        coEvery { repository.getPlanById(assigned.id) } returns Result.success(assigned)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(WorkoutSource.COACH, viewModel.uiState.value.selection.source)
        assertEquals(32L, viewModel.uiState.value.selection.assignmentId)
        assertEquals(assigned.id, viewModel.uiState.value.selection.plan?.id)
    }

    private fun createViewModel() = TodayWorkoutViewModel(
        GetWorkoutPlanDetailsUseCase(repository),
        repository,
        sessionRepository
    )

    private fun plan(id: Long, name: String) = UserWorkoutPlan(
        id = id,
        name = name,
        splitType = WorkoutSplitType.PPL,
        days = WorkoutDayOfWeek.entries.map { day ->
            UserWorkoutPlanDay(
                id = id * 10 + day.order,
                dayOfWeek = day,
                sessionType = WorkoutSessionType.PUSH
            )
        }
    )

    private fun resolution(
        plans: List<UserWorkoutPlan>,
        source: WorkoutSource,
        selectedId: Long?,
        assignmentId: Long? = null
    ) = TodayWorkoutResolution(
        workoutDate = "2026-10-26",
        activeCoach = source == WorkoutSource.COACH,
        source = source,
        selectionRequired = plans.size > 1 && selectedId == null,
        selectedPlanId = selectedId,
        candidates = plans.map {
            TodayWorkoutCandidate(
                plan = it,
                assignmentId = assignmentId,
                coachName = if (assignmentId != null) "Alex" else null
            )
        }
    )
}
