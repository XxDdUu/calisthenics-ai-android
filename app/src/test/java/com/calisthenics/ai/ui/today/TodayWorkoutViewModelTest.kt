package com.calisthenics.ai.ui.today

import com.calisthenics.ai.data.local.TodayWorkoutPreferences
import com.calisthenics.ai.data.local.WorkoutExperiencePreferences
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutPlanSnapshot
import com.calisthenics.ai.domain.model.TodayWorkoutCandidate
import com.calisthenics.ai.domain.model.TodayWorkoutResolution
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionClassification
import com.calisthenics.ai.domain.model.WorkoutSource
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import com.calisthenics.ai.domain.usecase.GetWorkoutPlanDetailsUseCase
import com.calisthenics.ai.ui.workoutsession.bubble.UnfinishedWorkoutBubbleCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayWorkoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository: WorkoutPlanRepository = mockk()
    private val todayWorkoutPreferences: TodayWorkoutPreferences = mockk {
        every { priority } returns flowOf(WorkoutSource.COACH)
    }
    private val sessionRepository: WorkoutSessionRepository = mockk()
    private val bubbleEnabledFlow = MutableStateFlow(true)
    private val workoutExperiencePreferences: WorkoutExperiencePreferences = mockk {
        every { unfinishedWorkoutBubbleEnabled } returns bubbleEnabledFlow
    }
    private val bubbleCoordinator: UnfinishedWorkoutBubbleCoordinator = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.planCacheInvalidations } returns kotlinx.coroutines.flow.emptyFlow()
        coEvery { sessionRepository.getActiveSessions() } returns Result.success(emptyList())
        coEvery { sessionRepository.getUpcomingSessions() } returns Result.success(emptyList())
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

    @Test
    fun offlineMode_loadsCachedSchedule_withoutActiveSessionsNetworkCall() = runTest {
        val cached = plan(10L, "Offline Strength")
        coEvery { repository.resolveTodayWorkout(any(), null) } returns Result.success(
            resolution(listOf(cached), WorkoutSource.PERSONAL, cached.id)
        )
        coEvery { repository.getActivePlanId() } returns Result.success(null)
        coEvery { repository.getPlanById(cached.id) } returns Result.success(cached)

        val viewModel = createViewModel(isOnline = false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOnline)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(cached.id, viewModel.uiState.value.selection.plan?.id)
        coVerify(exactly = 0) { sessionRepository.getActiveSessions() }
    }

    @Test
    fun futureSessionsNeverLeakIntoUnfinishedAndAppearInUpcoming() = runTest {
        val today = java.time.LocalDate.now()
        val pastUnfinished = workoutSession(
            id = 501L,
            workoutDate = today.minusDays(1).toString(),
            status = "IN_PROGRESS",
            completedAt = null
        )
        val futureMisclassified = workoutSession(
            id = 502L,
            workoutDate = today.plusDays(6).toString(),
            status = "IN_PROGRESS",
            completedAt = null
        )
        val futureScheduled = workoutSession(
            id = 503L,
            workoutDate = today.plusDays(8).toString(),
            status = "SCHEDULED",
            completedAt = null
        )
        coEvery { repository.resolveTodayWorkout(any(), null) } returns Result.success(
            resolution(emptyList(), WorkoutSource.NONE, null)
        )
        coEvery { repository.getActivePlanId() } returns Result.success(null)
        coEvery { sessionRepository.getActiveSessions() } returns Result.success(listOf(pastUnfinished, futureMisclassified))
        coEvery { sessionRepository.getUpcomingSessions() } returns Result.success(listOf(pastUnfinished, futureScheduled))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf(501L), viewModel.uiState.value.activeSessions.map { it.id })
        assertEquals(listOf(503L), viewModel.uiState.value.upcomingSessions.map { it.id })
        assertEquals(WorkoutSessionClassification.UNFINISHED, viewModel.uiState.value.activeSessions.single().effectiveClassification(today))
        assertEquals(WorkoutSessionClassification.UPCOMING, viewModel.uiState.value.upcomingSessions.single().effectiveClassification(today))
    }

    @Test
    fun planCacheInvalidation_refreshesTodayAndClearsDeletedSessions() = runTest {
        val invalidations = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val today = java.time.LocalDate.now()
        val deletedPlan = plan(5L, "Deleted Plan")
        val unfinishedOfPlan5 = workoutSession(
            id = 601L,
            workoutDate = today.toString(),
            status = "IN_PROGRESS",
            completedAt = null
        )
        val upcomingOfPlan5 = workoutSession(
            id = 602L,
            workoutDate = today.plusDays(4).toString(),
            status = "SCHEDULED",
            completedAt = null
        )
        every { repository.planCacheInvalidations } returns invalidations
        coEvery { repository.resolveTodayWorkout(any(), null) } returnsMany listOf(
            Result.success(resolution(listOf(deletedPlan), WorkoutSource.PERSONAL, deletedPlan.id)),
            Result.success(resolution(emptyList(), WorkoutSource.NONE, null))
        )
        coEvery { repository.getActivePlanId() } returns Result.success(null)
        coEvery { repository.getPlanById(5L) } returns Result.success(deletedPlan)
        coEvery { sessionRepository.getActiveSessions() } returnsMany listOf(
            Result.success(listOf(unfinishedOfPlan5)),
            Result.success(emptyList())
        )
        coEvery { sessionRepository.getUpcomingSessions() } returnsMany listOf(
            Result.success(listOf(upcomingOfPlan5)),
            Result.success(emptyList())
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.activeSessions.size)
        assertEquals(1, viewModel.uiState.value.upcomingSessions.size)

        invalidations.emit(Unit)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.activeSessions.size)
        assertEquals(0, viewModel.uiState.value.upcomingSessions.size)
        assertEquals(TodayWorkoutKind.NO_PLANS, viewModel.uiState.value.selection.kind)
    }

    @Test
    fun coachRelationshipTerminationInvalidation_fallsBackFromCoachToPersonalPlan() = runTest {
        val invalidations = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val today = java.time.LocalDate.now()
        val coachPlan = plan(10L, "Coach Plan")
        val personalPlan = plan(20L, "Personal Plan")
        val coachSession = workoutSession(
            id = 701L,
            workoutDate = today.toString(),
            status = "IN_PROGRESS",
            completedAt = null
        ).copy(assignmentId = 99L, source = "COACH")
        val futureCoachSession = workoutSession(
            id = 702L,
            workoutDate = today.plusDays(2).toString(),
            status = "SCHEDULED",
            completedAt = null
        ).copy(assignmentId = 99L, source = "COACH")

        every { repository.planCacheInvalidations } returns invalidations
        coEvery { repository.resolveTodayWorkout(any(), null) } returnsMany listOf(
            Result.success(resolution(listOf(coachPlan), WorkoutSource.COACH, coachPlan.id, assignmentId = 99L)),
            Result.success(resolution(listOf(personalPlan), WorkoutSource.PERSONAL, personalPlan.id, assignmentId = null))
        )
        coEvery { repository.getActivePlanId() } returns Result.success(null)
        coEvery { repository.getPlanById(coachPlan.id) } returns Result.success(coachPlan)
        coEvery { repository.getPlanById(personalPlan.id) } returns Result.success(personalPlan)
        coEvery { sessionRepository.getActiveSessions() } returnsMany listOf(
            Result.success(listOf(coachSession)),
            Result.success(emptyList())
        )
        coEvery { sessionRepository.getUpcomingSessions() } returnsMany listOf(
            Result.success(listOf(futureCoachSession)),
            Result.success(emptyList())
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(WorkoutSource.COACH, viewModel.uiState.value.selection.source)
        assertEquals(coachPlan.id, viewModel.uiState.value.selection.plan?.id)
        assertEquals(1, viewModel.uiState.value.activeSessions.size)
        assertEquals(1, viewModel.uiState.value.upcomingSessions.size)

        invalidations.emit(Unit)
        advanceUntilIdle()

        assertEquals(WorkoutSource.PERSONAL, viewModel.uiState.value.selection.source)
        assertEquals(personalPlan.id, viewModel.uiState.value.selection.plan?.id)
        assertEquals(0, viewModel.uiState.value.activeSessions.size)
        assertEquals(0, viewModel.uiState.value.upcomingSessions.size)
    }

    @Test
    fun bubblePreferenceSynchronizesWithUiState() = runTest {
        coEvery { repository.resolveTodayWorkout(any(), any()) } returns Result.success(resolution(emptyList(), WorkoutSource.NONE, null))
        coEvery { repository.getActivePlanId() } returns Result.success(null)
        bubbleEnabledFlow.value = true
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBubbleEnabled)

        // Change setting ON -> OFF
        bubbleEnabledFlow.value = false
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isBubbleEnabled)

        // Change setting OFF -> ON
        bubbleEnabledFlow.value = true
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBubbleEnabled)
    }

    @Test
    fun openWorkoutBubbleOpensBubbleWhenEnabledAndActiveSessionExists() = runTest {
        val session = workoutSession(101L, "2026-11-13", "IN_PROGRESS", null)
        coEvery { sessionRepository.getActiveSessions() } returns Result.success(listOf(session))
        coEvery { repository.resolveTodayWorkout(any(), any()) } returns Result.success(resolution(emptyList(), WorkoutSource.NONE, null))
        coEvery { repository.getActivePlanId() } returns Result.success(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.activeSessions.isNotEmpty())
        assertTrue(viewModel.uiState.value.isBubbleEnabled)

        val result = viewModel.openWorkoutBubble()
        assertTrue(result)
        verify { bubbleCoordinator.openBubble() }
    }

    @Test
    fun openWorkoutBubbleFailsWhenDisabledOrNoActiveSession() = runTest {
        coEvery { repository.resolveTodayWorkout(any(), any()) } returns Result.success(resolution(emptyList(), WorkoutSource.NONE, null))
        coEvery { repository.getActivePlanId() } returns Result.success(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Empty active sessions
        assertTrue(viewModel.uiState.value.activeSessions.isEmpty())
        val resultNoSession = viewModel.openWorkoutBubble()
        assertFalse(resultNoSession)
        verify(exactly = 0) { bubbleCoordinator.openBubble() }

        // Disabled
        bubbleEnabledFlow.value = false
        advanceUntilIdle()
        val resultDisabled = viewModel.openWorkoutBubble()
        assertFalse(resultDisabled)
    }

    private fun createViewModel(
        isOnline: Boolean = true,
        experiencePreferences: WorkoutExperiencePreferences? = workoutExperiencePreferences,
        coordinator: UnfinishedWorkoutBubbleCoordinator? = bubbleCoordinator,
        gettingStartedRepository: com.calisthenics.ai.domain.repository.GettingStartedRepository =
            com.calisthenics.ai.domain.repository.NoOpGettingStartedRepository
    ): TodayWorkoutViewModel {
        val connectivityMonitor: com.calisthenics.ai.data.network.ConnectivityMonitor = mockk {
            io.mockk.every { isConnected() } returns isOnline
            io.mockk.every { this@mockk.isOnline } returns kotlinx.coroutines.flow.flowOf(isOnline)
        }
        return TodayWorkoutViewModel(
            getWorkoutPlanDetailsUseCase = GetWorkoutPlanDetailsUseCase(repository),
            workoutPlanRepository = repository,
            workoutSessionRepository = sessionRepository,
            connectivityMonitor = connectivityMonitor,
            todayWorkoutPreferences = todayWorkoutPreferences,
            workoutExperiencePreferences = experiencePreferences,
            bubbleCoordinator = coordinator,
            gettingStartedRepository = gettingStartedRepository
        )
    }

    private fun workoutSession(
        id: Long,
        workoutDate: String,
        status: String,
        completedAt: String?
    ) = WorkoutSession(
        id = id,
        workoutPlanId = 10L,
        workoutPlanDayId = 20L,
        assignmentId = null,
        source = "PERSONAL",
        title = "Session $id",
        workoutDate = workoutDate,
        status = status,
        startedAt = null,
        lastSavedAt = null,
        completedAt = completedAt,
        durationSeconds = 0L,
        completedExercises = 0,
        skippedExercises = 0,
        unfinishedExercises = 1,
        completedSets = 0,
        totalReps = 0,
        totalHoldDurationSeconds = 0,
        exercises = emptyList()
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

    @Test
    fun gettingStartedScheduledSessions_surfaceAlongsidePersonalPlan_andDisappearWhenCompleted() = runTest {
        val today = java.time.LocalDate.now()
        val personalPlan = plan(1L, "My Split")
        val todayGsSession = WorkoutSession(
            id = 501L,
            workoutPlanId = null,
            workoutPlanDayId = null,
            assignmentId = null,
            source = "GETTING_STARTED",
            title = "Day 1: Foundation",
            workoutDate = today.toString(),
            status = "SCHEDULED",
            startedAt = null,
            lastSavedAt = null,
            completedAt = null,
            durationSeconds = 0L,
            completedExercises = 0,
            skippedExercises = 0,
            unfinishedExercises = 1,
            completedSets = 0,
            totalReps = 0,
            totalHoldDurationSeconds = 0,
            exercises = emptyList(),
            gettingStartedDayNumber = 1
        )
        val futureGsSession = todayGsSession.copy(
            id = 502L,
            title = "Day 2: Pull & Core",
            workoutDate = today.plusDays(1).toString(),
            gettingStartedDayNumber = 2
        )
        val activeProgram = com.calisthenics.ai.domain.model.GettingStartedProgram(
            id = 10L,
            title = "Getting Started – 3 Days",
            published = true,
            enrollment = com.calisthenics.ai.domain.model.GettingStartedEnrollment(
                id = 88L,
                programId = 10L,
                status = "NOT_STARTED",
                addedToWorkoutPlan = true,
                isCompleted = false
            )
        )
        val completedProgram = activeProgram.copy(
            enrollment = activeProgram.enrollment?.copy(status = "COMPLETED", isCompleted = true, completedDaysCount = 3)
        )
        val enrollmentUpdates = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 4)
        val gsRepo = mockk<com.calisthenics.ai.domain.repository.GettingStartedRepository>()
        every { gsRepo.enrollmentUpdates } returns enrollmentUpdates
        coEvery { gsRepo.getPublishedProgram() } returnsMany listOf(
            Result.success(activeProgram),
            Result.success(completedProgram)
        )

        coEvery { repository.resolveTodayWorkout(any(), null) } returns Result.success(
            resolution(listOf(personalPlan), WorkoutSource.PERSONAL, personalPlan.id)
        )
        coEvery { repository.getActivePlanId() } returns Result.success(null)
        coEvery { repository.getPlanById(personalPlan.id) } returns Result.success(personalPlan)
        coEvery { sessionRepository.getActiveSessions() } returnsMany listOf(
            Result.success(listOf(todayGsSession)),
            Result.success(emptyList())
        )
        coEvery { sessionRepository.getUpcomingSessions() } returnsMany listOf(
            Result.success(listOf(futureGsSession)),
            Result.success(emptyList())
        )

        val viewModel = createViewModel(gettingStartedRepository = gsRepo)
        advanceUntilIdle()

        assertEquals(personalPlan.id, viewModel.uiState.value.selection.plan?.id)
        assertEquals(listOf(501L), viewModel.uiState.value.activeSessions.map { it.id })
        assertEquals(listOf(502L), viewModel.uiState.value.upcomingSessions.map { it.id })
        assertEquals(10L, viewModel.uiState.value.activeGettingStartedProgram?.id)

        enrollmentUpdates.emit(Unit)
        advanceUntilIdle()

        assertEquals(personalPlan.id, viewModel.uiState.value.selection.plan?.id)
        assertTrue(viewModel.uiState.value.activeSessions.isEmpty())
        assertTrue(viewModel.uiState.value.upcomingSessions.isEmpty())
        assertNull(viewModel.uiState.value.activeGettingStartedProgram)
    }
}
