package com.calisthenics.ai.ui.workoutsession

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.domain.model.CoachConnectionOverview
import com.calisthenics.ai.domain.model.GettingStartedDayProgress
import com.calisthenics.ai.domain.model.GettingStartedDayStatus
import com.calisthenics.ai.domain.model.GettingStartedEnrollment
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionHistoryPage
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.GettingStartedRepository
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModelGettingStartedTest {

    private val dispatcher = StandardTestDispatcher()
    private val sessionRepository: WorkoutSessionRepository = mockk()
    private val liveAnalysisRepository: LiveAnalysisRepository = mockk()
    private val workoutPlanRepository: WorkoutPlanRepository = mockk()
    private val gettingStartedRepository: GettingStartedRepository = mockk()
    private val analyticsRepository: AnalyticsRepository = mockk()
    private val coachClientRepository: CoachClientRepository = mockk()
    private lateinit var accountSessionManager: AccountSessionManager

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        accountSessionManager = AccountSessionManager().apply { updateTestSession(101L, "ROLE_USER") }
        every { workoutPlanRepository.planCacheInvalidations } returns emptyFlow()
        every { gettingStartedRepository.enrollmentUpdates } returns emptyFlow()
        coEvery { liveAnalysisRepository.getSessions() } returns Result.success(emptyList())
        coEvery { sessionRepository.getActiveSessions() } returns Result.success(emptyList())
        coEvery { sessionRepository.getUpcomingSessions() } returns Result.success(emptyList())
        coEvery { sessionRepository.getHistory(any(), any()) } returns Result.success(
            WorkoutSessionHistoryPage(content = emptyList(), page = 0, size = 20, totalElements = 0L, totalPages = 0)
        )
        coEvery { analyticsRepository.getWorkoutSessionRecords(any()) } returns Result.success(emptyList())
        coEvery { analyticsRepository.getClientSharedPRs() } returns Result.success(emptyList())
        coEvery { coachClientRepository.loadOverview() } returns Result.success(
            CoachConnectionOverview(currentUserId = 101L, coaches = emptyList(), relationships = emptyList())
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun historyViewModel_loadsOnlyCompletedGettingStartedPrograms_andRefreshesOnUpdates() = runTest {
        val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
        every { gettingStartedRepository.enrollmentUpdates } returns updates

        val inProgress = GettingStartedEnrollment(
            id = 10L,
            programId = 1L,
            programTitle = "Getting Started – 3 Days",
            status = "IN_PROGRESS",
            addedToWorkoutPlan = true,
            completedDaysCount = 2,
            totalDays = 3,
            isCompleted = false
        )
        val completed = GettingStartedEnrollment(
            id = 10L,
            programId = 1L,
            programTitle = "Getting Started – 3 Days",
            status = "COMPLETED",
            addedToWorkoutPlan = true,
            completedAt = "2026-11-14T12:00:00Z",
            completedDays = listOf(1, 2, 3),
            dayProgress = listOf(
                GettingStartedDayProgress(1, "Day 1", GettingStartedDayStatus.COMPLETED),
                GettingStartedDayProgress(2, "Day 2", GettingStartedDayStatus.COMPLETED),
                GettingStartedDayProgress(3, "Day 3", GettingStartedDayStatus.COMPLETED)
            ),
            completedDaysCount = 3,
            totalDays = 3,
            isCompleted = true
        )
        coEvery { gettingStartedRepository.getUserEnrollments() } returnsMany listOf(
            Result.success(listOf(inProgress)),
            Result.success(listOf(completed))
        )

        val viewModel = WorkoutHistoryViewModel(
            repository = sessionRepository,
            liveAnalysisRepository = liveAnalysisRepository,
            workoutPlanRepository = workoutPlanRepository,
            accountSessionManager = accountSessionManager,
            gettingStartedRepository = gettingStartedRepository
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.completedGettingStartedPrograms.isEmpty())

        updates.emit(Unit)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.completedGettingStartedPrograms.size)
        assertEquals("COMPLETED", viewModel.uiState.value.completedGettingStartedPrograms.first().status)
    }

    @Test
    fun summaryViewModel_populatesCompletedProgramOnlyWhenGettingStartedProgramCompleted() = runTest {
        val day3Session = WorkoutSession(
            id = 303L,
            workoutPlanId = null,
            workoutPlanDayId = null,
            assignmentId = null,
            source = "GETTING_STARTED",
            title = "Day 3: Strength & Stability",
            workoutDate = "2026-11-14",
            status = "COMPLETED",
            startedAt = "2026-11-14T11:00:00Z",
            lastSavedAt = "2026-11-14T11:30:00Z",
            completedAt = "2026-11-14T11:30:00Z",
            durationSeconds = 1800L,
            completedExercises = 3,
            skippedExercises = 0,
            unfinishedExercises = 0,
            completedSets = 9,
            totalReps = 90,
            totalHoldDurationSeconds = 0,
            exercises = emptyList(),
            gettingStartedEnrollmentId = 10L,
            gettingStartedDayNumber = 3
        )
        val completed = GettingStartedEnrollment(
            id = 10L,
            programId = 1L,
            programTitle = "Getting Started – 3 Days",
            status = "COMPLETED",
            completedDaysCount = 3,
            totalDays = 3,
            isCompleted = true
        )
        val incomplete = completed.copy(status = "IN_PROGRESS", completedDaysCount = 2, isCompleted = false)

        coEvery { sessionRepository.getSession(303L) } returns Result.success(day3Session)
        coEvery { gettingStartedRepository.getUserEnrollments() } returnsMany listOf(
            Result.success(listOf(incomplete)),
            Result.success(listOf(completed))
        )

        val inProgressSummaryVm = WorkoutSummaryViewModel(
            repository = sessionRepository,
            analyticsRepository = analyticsRepository,
            coachClientRepository = coachClientRepository,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to 303L)),
            gettingStartedRepository = gettingStartedRepository
        )
        advanceUntilIdle()
        assertNull(inProgressSummaryVm.uiState.value.gettingStartedCompletedProgram)

        val completedSummaryVm = WorkoutSummaryViewModel(
            repository = sessionRepository,
            analyticsRepository = analyticsRepository,
            coachClientRepository = coachClientRepository,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to 303L)),
            gettingStartedRepository = gettingStartedRepository
        )
        advanceUntilIdle()
        assertEquals(10L, completedSummaryVm.uiState.value.gettingStartedCompletedProgram?.id)
        assertTrue(completedSummaryVm.uiState.value.gettingStartedCompletedProgram?.isCompleted == true)
    }
}
