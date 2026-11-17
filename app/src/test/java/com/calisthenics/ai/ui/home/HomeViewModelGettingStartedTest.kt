package com.calisthenics.ai.ui.home

import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.OnboardingSessionStore
import com.calisthenics.ai.domain.model.*
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.GettingStartedRepository
import com.calisthenics.ai.domain.repository.RegistrationInitiation
import com.calisthenics.ai.domain.usecase.GetAnalysisHistoryUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
class HomeViewModelGettingStartedTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var gettingStartedRepository: FakeGettingStartedRepository
    private lateinit var onboardingSessionStore: OnboardingSessionStore
    private lateinit var accountSessionManager: AccountSessionManager
    private lateinit var getAnalysisHistoryUseCase: GetAnalysisHistoryUseCase

    private val samplePublishedProgram = GettingStartedProgram(
        id = 1L,
        title = "Getting Started – 3 Days",
        description = "Beginner program",
        published = true,
        version = 1,
        eligible = true,
        days = listOf(
            GettingStartedDay(id = 10L, dayNumber = 1, title = "Day 1: Full-Body Foundation", exercises = listOf()),
            GettingStartedDay(id = 20L, dayNumber = 2, title = "Day 2: Push & Pull", exercises = listOf()),
            GettingStartedDay(id = 30L, dayNumber = 3, title = "Day 3: Core & Flow", exercises = listOf())
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = FakeAuthRepository()
        gettingStartedRepository = FakeGettingStartedRepository()
        onboardingSessionStore = OnboardingSessionStore()
        accountSessionManager = AccountSessionManager()
        getAnalysisHistoryUseCase = mockk(relaxed = true)
        coEvery { getAnalysisHistoryUseCase() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            authRepository = authRepository,
            getAnalysisHistoryUseCase = getAnalysisHistoryUseCase,
            onboardingSessionStore = onboardingSessionStore,
            gettingStartedRepository = gettingStartedRepository,
            accountSessionManager = accountSessionManager
        )
    }

    @Test
    fun `first-time athlete with published program and NOT_SHOWN status displays expanded card`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.NOT_SHOWN
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "athlete@example.com",
                fullName = "New Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.NOT_SHOWN
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAthlete)
        assertEquals(OnboardingStatus.NOT_SHOWN, state.onboardingStatus)
        assertTrue(
            "publishedProgram: ${state.publishedProgram}, isAthlete: ${state.isAthlete}, onboardingStatus: ${state.onboardingStatus}",
            state.shouldShowOnboardingCard
        )
        assertFalse(state.shouldShowCompactBanner)
    }

    @Test
    fun `first-time athlete does NOT see onboarding card or banner if admin has not published program`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.NOT_SHOWN
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "athlete@example.com",
                fullName = "New Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.NOT_SHOWN
            )
        )
        gettingStartedRepository.publishedProgram = null // No program published by Admin
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAthlete)
        assertEquals(OnboardingStatus.NOT_SHOWN, state.onboardingStatus)
        assertNull(state.publishedProgram)
        // Must NOT show card or banner when unpublished
        assertFalse(state.shouldShowOnboardingCard)
        assertFalse(state.shouldShowCompactBanner)
    }

    @Test
    fun `admin account never shows onboarding card or compact banner`() = runTest {
        authRepository.activeUserId = 999L
        authRepository.activeUserRole = "ROLE_ADMIN"
        authRepository.cachedOnboardingMap[999L] = OnboardingStatus.NOT_SHOWN
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 999L,
                email = "admin@example.com",
                fullName = "Admin User",
                experienceLevel = "ADVANCED",
                role = "ROLE_ADMIN",
                onboardingStatus = OnboardingStatus.NOT_SHOWN
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram
        accountSessionManager.updateTestSession(999L, "ROLE_ADMIN")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAthlete)
        assertEquals(OnboardingStatus.DISMISSED, state.onboardingStatus)
        assertFalse(state.shouldShowOnboardingCard)
        assertFalse(state.shouldShowCompactBanner)
    }

    @Test
    fun `dismissToCompactBanner converts status to COMPACT_BANNER and updates backend`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.NOT_SHOWN
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "athlete@example.com",
                fullName = "New Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.NOT_SHOWN
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            eligible = true,
            userDismissalState = OnboardingStatus.NOT_SHOWN
        )
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shouldShowOnboardingCard)

        viewModel.dismissToCompactBanner()
        advanceUntilIdle()

        assertEquals(OnboardingStatus.COMPACT_BANNER, gettingStartedRepository.lastDismissalStatus)
        val state = viewModel.uiState.value
        assertFalse(state.shouldShowOnboardingCard)
        assertTrue(state.shouldShowCompactBanner)
    }

    @Test
    fun `permanentlyDismissOnboarding converts status to DISMISSED and permanently hides banner`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.COMPACT_BANNER
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "athlete@example.com",
                fullName = "New Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.COMPACT_BANNER
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            eligible = true,
            userDismissalState = OnboardingStatus.COMPACT_BANNER
        )
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shouldShowCompactBanner)

        viewModel.permanentlyDismissOnboarding()
        advanceUntilIdle()

        assertEquals(OnboardingStatus.DISMISSED, gettingStartedRepository.lastDismissalStatus)
        val state = viewModel.uiState.value
        assertFalse(state.shouldShowOnboardingCard)
        assertFalse(state.shouldShowCompactBanner)
        assertEquals(OnboardingStatus.DISMISSED, state.onboardingStatus)
    }

    @Test
    fun `voluntary enrollment updates state without modifying existing coach plans`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.NOT_SHOWN
        gettingStartedRepository.publishedProgram = samplePublishedProgram
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.publishedProgram?.enrollment)

        viewModel.enrollInProgram()
        advanceUntilIdle()

        val enrolledProg = viewModel.uiState.value.publishedProgram
        assertTrue(gettingStartedRepository.enrollCalled)
        assertEquals("NOT_STARTED", enrolledProg?.enrollment?.status)
    }

    @Test
    fun `completeProgramDay marks day completed in enrollment state`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            enrollment = GettingStartedEnrollment(
                id = 50L,
                programId = 1L,
                programVersion = 1,
                status = "ACTIVE",
                completedDays = listOf(1)
            )
        )
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.completeProgramDay(2)
        advanceUntilIdle()

        val enrolledProg = viewModel.uiState.value.publishedProgram
        assertTrue(enrolledProg?.days?.find { it.dayNumber == 2 }?.completed == true)
    }

    @Test
    fun `openProgramDetails and closeProgramDetails toggle bottom sheet state`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        gettingStartedRepository.publishedProgram = samplePublishedProgram
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showProgramDetails)

        viewModel.openProgramDetails()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showProgramDetails)

        viewModel.closeProgramDetails()
        assertFalse(viewModel.uiState.value.showProgramDetails)
    }

    @Test
    fun `existing user with DISMISSED status never shows onboarding card or compact banner`() = runTest {
        authRepository.activeUserId = 202L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[202L] = OnboardingStatus.DISMISSED
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 202L,
                email = "existing@example.com",
                fullName = "Existing Athlete",
                experienceLevel = "ADVANCED",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.DISMISSED
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            userDismissalState = OnboardingStatus.DISMISSED
        )
        accountSessionManager.updateTestSession(202L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAthlete)
        assertEquals(OnboardingStatus.DISMISSED, state.onboardingStatus)
        assertFalse(state.shouldShowOnboardingCard)
        assertFalse(state.shouldShowCompactBanner)
    }

    @Test
    fun `athlete outside target audience does NOT see onboarding card or banner`() = runTest {
        authRepository.activeUserId = 303L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 303L,
                email = "outside@example.com",
                fullName = "Outside Athlete",
                experienceLevel = "INTERMEDIATE",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.NOT_SHOWN
            )
        )
        // Program has target audience where this athlete is not eligible
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            targetAudience = TargetAudience.LAST_14_DAYS,
            eligible = false,
            userDismissalState = OnboardingStatus.NOT_SHOWN
        )
        accountSessionManager.updateTestSession(303L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAthlete)
        assertFalse(state.publishedProgram?.eligible == true)
        assertFalse(state.shouldShowOnboardingCard)
        assertFalse(state.shouldShowCompactBanner)
    }

    @Test
    fun `athlete inside target audience sees onboarding card`() = runTest {
        authRepository.activeUserId = 304L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 304L,
                email = "inside@example.com",
                fullName = "Inside Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.NOT_SHOWN
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            targetAudience = TargetAudience.LAST_30_DAYS,
            eligible = true,
            userDismissalState = OnboardingStatus.NOT_SHOWN
        )
        accountSessionManager.updateTestSession(304L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAthlete)
        assertTrue(state.publishedProgram?.eligible == true)
        assertEquals(OnboardingStatus.NOT_SHOWN, state.onboardingStatus)
        assertTrue(state.shouldShowOnboardingCard)
        assertFalse(state.shouldShowCompactBanner)
    }

    @Test
    fun `existing athlete matching audience sees onboarding modal automatically even when not first login`() = runTest {
        authRepository.activeUserId = 555L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[555L] = OnboardingStatus.DISMISSED
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 555L,
                email = "existing.athlete@example.com",
                fullName = "Existing Active Athlete",
                experienceLevel = "ADVANCED",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.DISMISSED // old legacy user status
            )
        )
        // Published campaign has NOT been dismissed by this athlete for this campaign
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            id = 10L,
            version = 2,
            targetAudience = TargetAudience.ALL_ATHLETES,
            eligible = true,
            userDismissalState = OnboardingStatus.NOT_SHOWN
        )
        accountSessionManager.updateTestSession(555L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAthlete)
        assertTrue(state.shouldShowOnboardingModal)
        assertFalse(state.shouldShowCompactBanner)

        // Dismiss modal -> converts to compact banner
        viewModel.dismissToCompactBanner()
        advanceUntilIdle()

        val bannerState = viewModel.uiState.value
        assertFalse(bannerState.shouldShowOnboardingModal)
        assertTrue(bannerState.shouldShowCompactBanner)

        // Dismiss banner -> permanently dismissed
        viewModel.permanentlyDismissOnboarding()
        advanceUntilIdle()

        val dismissedState = viewModel.uiState.value
        assertFalse(dismissedState.shouldShowOnboardingModal)
        assertFalse(dismissedState.shouldShowCompactBanner)
    }

    @Test
    fun `switching accounts updates onboarding status to match newly active account`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.DISMISSED
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "first@example.com",
                fullName = "First User",
                experienceLevel = "INTERMEDIATE",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.DISMISSED
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            userDismissalState = OnboardingStatus.DISMISSED
        )
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.shouldShowOnboardingCard)

        // Switch to account 102: NOT_SHOWN
        authRepository.activeUserId = 102L
        authRepository.cachedOnboardingMap[102L] = OnboardingStatus.NOT_SHOWN
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 102L,
                email = "second@example.com",
                fullName = "Second User",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.NOT_SHOWN
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            eligible = true,
            userDismissalState = OnboardingStatus.NOT_SHOWN
        )
        accountSessionManager.updateTestSession(102L, "ROLE_USER")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(102L, state.activeUserId)
        assertEquals(OnboardingStatus.NOT_SHOWN, state.onboardingStatus)
        assertTrue(state.shouldShowOnboardingCard)
    }

    @Test
    fun `trackModalImpression and openProgramDetails record telemetry events once per program version`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.NOT_SHOWN
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "athlete@example.com",
                fullName = "New Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.NOT_SHOWN
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.trackModalImpression()
        viewModel.trackModalImpression()
        viewModel.openProgramDetails()
        viewModel.openProgramDetails()
        advanceUntilIdle()

        assertEquals(
            listOf("MODAL_IMPRESSION" to 1L, "PROGRAM_DETAIL_VIEW" to 1L),
            gettingStartedRepository.recordedEvents
        )
    }

    @Test
    fun `requestEnroll then enrollJustStart enrolls with addedToWorkoutPlan false and NOT_STARTED status`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.COMPACT_BANNER
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "athlete@example.com",
                fullName = "New Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.COMPACT_BANNER
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.requestEnroll()
        assertTrue(viewModel.uiState.value.showEnrollChoiceDialog)

        viewModel.enrollJustStart()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showEnrollChoiceDialog)
        assertEquals(false to false, gettingStartedRepository.lastEnrollArgs)
        val enrollment = state.publishedProgram?.enrollment
        assertEquals(false, enrollment?.addedToWorkoutPlan)
        assertEquals("NOT_STARTED", enrollment?.status)
        assertEquals(0, enrollment?.completedDaysCount)
        assertEquals(GettingStartedDayStatus.NOT_STARTED, state.publishedProgram?.days?.first()?.status)
    }

    @Test
    fun `enrollAddToWorkoutPlan without conflicts enrolls cleanly and with conflicts prompts then confirms`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.COMPACT_BANNER
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "athlete@example.com",
                fullName = "New Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.COMPACT_BANNER
            )
        )
        val conflict = GettingStartedScheduleConflict(
            date = "2026-11-14",
            gettingStartedDayNumber = 1,
            gettingStartedDayTitle = "Day 1: Full-Body Foundation",
            existingWorkoutTitle = "Push Day",
            existingSource = "PERSONAL"
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram
        gettingStartedRepository.previewResult = Result.success(
            GettingStartedEnrollPreview(
                programId = 1L,
                programTitle = "Getting Started – 3 Days",
                alreadyEnrolled = false,
                alreadyAddedToWorkoutPlan = false,
                startDate = "2026-11-14",
                plannedDates = listOf("2026-11-14", "2026-11-15", "2026-11-16"),
                hasConflicts = true,
                conflicts = listOf(conflict)
            )
        )
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.requestEnroll()
        assertTrue(viewModel.uiState.value.showEnrollChoiceDialog)

        viewModel.enrollAddToWorkoutPlan()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showEnrollChoiceDialog)
        assertTrue(viewModel.uiState.value.showConflictDialog)
        assertEquals(listOf(conflict), viewModel.uiState.value.pendingScheduleConflicts)
        assertNull(gettingStartedRepository.lastEnrollArgs)

        viewModel.confirmConflictEnrollment()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showConflictDialog)
        assertEquals(true to true, gettingStartedRepository.lastEnrollArgs)
        assertEquals(true, state.publishedProgram?.enrollment?.addedToWorkoutPlan)
        assertEquals("NOT_STARTED", state.publishedProgram?.enrollment?.status)
    }

    @Test
    fun `enrollAddToWorkoutPlan catching 409 schedule conflict exception opens conflict dialog`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.COMPACT_BANNER
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "athlete@example.com",
                fullName = "New Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.COMPACT_BANNER
            )
        )
        val conflict = GettingStartedScheduleConflict(
            date = "2026-11-14",
            gettingStartedDayNumber = 1,
            gettingStartedDayTitle = "Day 1",
            existingWorkoutTitle = "Coach PPL",
            existingSource = "COACH"
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram
        gettingStartedRepository.previewResult = Result.success(
            GettingStartedEnrollPreview(
                programId = 1L,
                programTitle = "Getting Started – 3 Days",
                alreadyEnrolled = false,
                alreadyAddedToWorkoutPlan = false,
                startDate = "2026-11-14",
                hasConflicts = false
            )
        )
        gettingStartedRepository.conflictOnFirstPlanEnroll = conflict
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.enrollAddToWorkoutPlan()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showConflictDialog)
        assertEquals(listOf(conflict), viewModel.uiState.value.pendingScheduleConflicts)
    }

    @Test
    fun `startDayWorkout moves day to IN_PROGRESS never COMPLETED and enrollmentUpdates hides completed banner`() = runTest {
        authRepository.activeUserId = 101L
        authRepository.activeUserRole = "ROLE_USER"
        authRepository.cachedOnboardingMap[101L] = OnboardingStatus.NOT_SHOWN
        authRepository.currentUserResult = Result.success(
            UserProfile(
                id = 101L,
                email = "athlete@example.com",
                fullName = "New Athlete",
                experienceLevel = "BEGINNER",
                role = "ROLE_USER",
                onboardingStatus = OnboardingStatus.NOT_SHOWN
            )
        )
        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            enrollment = GettingStartedEnrollment(
                id = 100L,
                programId = 1L,
                status = "NOT_STARTED",
                addedToWorkoutPlan = false,
                dayProgress = listOf(
                    GettingStartedDayProgress(1, "Day 1", GettingStartedDayStatus.NOT_STARTED),
                    GettingStartedDayProgress(2, "Day 2", GettingStartedDayStatus.NOT_STARTED),
                    GettingStartedDayProgress(3, "Day 3", GettingStartedDayStatus.NOT_STARTED)
                )
            )
        )
        accountSessionManager.updateTestSession(101L, "ROLE_USER")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shouldShowOnboardingModal)

        var startedSessionId: Long? = null
        viewModel.startDayWorkout(1) { session -> startedSessionId = session.id }
        advanceUntilIdle()

        assertEquals(777L, startedSessionId)
        val day1AfterStart = viewModel.uiState.value.publishedProgram?.days?.first { it.dayNumber == 1 }
        assertEquals(GettingStartedDayStatus.IN_PROGRESS, day1AfterStart?.status)
        assertFalse(day1AfterStart?.completed == true)

        gettingStartedRepository.publishedProgram = samplePublishedProgram.copy(
            enrollment = GettingStartedEnrollment(
                id = 100L,
                programId = 1L,
                status = "COMPLETED",
                addedToWorkoutPlan = true,
                completedDaysCount = 3,
                totalDays = 3,
                completedDays = listOf(1, 2, 3),
                isCompleted = true
            )
        )
        gettingStartedRepository.emitUpdate()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isGettingStartedCompleted)
        assertFalse(viewModel.uiState.value.shouldShowOnboardingModal)
        assertFalse(viewModel.uiState.value.shouldShowOnboardingCard)
        assertFalse(viewModel.uiState.value.shouldShowCompactBanner)
    }

    private class FakeGettingStartedRepository : GettingStartedRepository {
        private val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
        override val enrollmentUpdates: Flow<Unit> = updates

        var publishedProgram: GettingStartedProgram? = null
        var enrollCalled = false
        var lastEnrollArgs: Pair<Boolean, Boolean>? = null
        var previewResult: Result<GettingStartedEnrollPreview>? = null
        var conflictOnFirstPlanEnroll: GettingStartedScheduleConflict? = null
        var completedDayNum: Int? = null
        var lastDismissalStatus: OnboardingStatus? = null
        val recordedEvents = mutableListOf<Pair<String, Long?>>()

        suspend fun emitUpdate() {
            updates.emit(Unit)
        }

        override suspend fun previewEnrollment(startDate: String?): Result<GettingStartedEnrollPreview> {
            return previewResult ?: Result.success(
                GettingStartedEnrollPreview(
                    programId = publishedProgram?.id ?: 1L,
                    programTitle = publishedProgram?.title,
                    alreadyEnrolled = publishedProgram?.enrollment != null,
                    alreadyAddedToWorkoutPlan = publishedProgram?.enrollment?.addedToWorkoutPlan == true,
                    startDate = startDate ?: "2026-11-14",
                    plannedDates = listOf("2026-11-14", "2026-11-15", "2026-11-16"),
                    hasConflicts = false,
                    conflicts = emptyList()
                )
            )
        }

        override suspend fun updateDismissalStatus(status: OnboardingStatus): Result<GettingStartedProgram> {
            lastDismissalStatus = status
            publishedProgram = publishedProgram?.copy(userDismissalState = status)
            return Result.success(publishedProgram ?: GettingStartedProgram(id = 1L, title = "Program", published = true, userDismissalState = status))
        }

        override suspend fun getAdminProgram(): Result<GettingStartedProgram?> =
            Result.success(publishedProgram)

        override suspend fun saveAdminProgram(draft: AdminProgramDraft): Result<GettingStartedProgram> =
            Result.success(GettingStartedProgram(id = 1L, title = draft.title, published = false, version = 1))

        override suspend fun publishProgram(): Result<GettingStartedProgram> {
            publishedProgram = publishedProgram?.copy(published = true)
            return Result.success(publishedProgram ?: GettingStartedProgram(id = 1L, title = "Program", published = true))
        }

        override suspend fun unpublishProgram(): Result<GettingStartedProgram> {
            publishedProgram = publishedProgram?.copy(published = false)
            return Result.success(publishedProgram ?: GettingStartedProgram(id = 1L, title = "Program", published = false))
        }

        override suspend fun deleteProgram(): Result<Unit> {
            publishedProgram = null
            return Result.success(Unit)
        }

        override suspend fun getPublishedProgram(): Result<GettingStartedProgram?> =
            Result.success(publishedProgram)

        override suspend fun enroll(): Result<GettingStartedEnrollment> =
            enroll(addToWorkoutPlan = false, confirmConflicts = false, startDate = null)

        override suspend fun enroll(
            addToWorkoutPlan: Boolean,
            confirmConflicts: Boolean,
            startDate: String?
        ): Result<GettingStartedEnrollment> {
            enrollCalled = true
            val conflict = conflictOnFirstPlanEnroll
            if (addToWorkoutPlan && !confirmConflicts && conflict != null) {
                conflictOnFirstPlanEnroll = null
                return Result.failure(GettingStartedScheduleConflictException("Conflict detected", listOf(conflict)))
            }
            lastEnrollArgs = addToWorkoutPlan to confirmConflicts
            val enrollment = GettingStartedEnrollment(
                id = 100L,
                programId = 1L,
                programTitle = publishedProgram?.title,
                programVersion = 1,
                status = "NOT_STARTED",
                addedToWorkoutPlan = addToWorkoutPlan,
                dayProgress = listOf(
                    GettingStartedDayProgress(1, "Day 1", GettingStartedDayStatus.NOT_STARTED),
                    GettingStartedDayProgress(2, "Day 2", GettingStartedDayStatus.NOT_STARTED),
                    GettingStartedDayProgress(3, "Day 3", GettingStartedDayStatus.NOT_STARTED)
                ),
                completedDaysCount = 0,
                totalDays = 3,
                isCompleted = false
            )
            publishedProgram = publishedProgram?.copy(enrollment = enrollment)
            return Result.success(enrollment)
        }

        override suspend fun startDaySession(dayNumber: Int, workoutDate: String?): Result<WorkoutSession> {
            return Result.success(
                WorkoutSession(
                    id = 777L,
                    workoutPlanId = null,
                    workoutPlanDayId = null,
                    assignmentId = null,
                    source = "GETTING_STARTED",
                    title = "Day $dayNumber",
                    workoutDate = workoutDate ?: "2026-11-14",
                    status = "IN_PROGRESS",
                    startedAt = "2026-11-14T10:00:00Z",
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
                    gettingStartedEnrollmentId = 100L,
                    gettingStartedDayNumber = dayNumber
                )
            )
        }

        override suspend fun completeDay(dayNumber: Int): Result<GettingStartedEnrollment> {
            completedDayNum = dayNumber
            return Result.success(
                GettingStartedEnrollment(
                    id = 100L,
                    programId = 1L,
                    programVersion = 1,
                    status = if (dayNumber == 3) "COMPLETED" else "ACTIVE",
                    completedDays = listOf(dayNumber)
                )
            )
        }

        override suspend fun trackCampaignEvent(eventType: String, programId: Long?): Result<Unit> {
            recordedEvents += (eventType to programId)
            return Result.success(Unit)
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var activeUserId: Long? = null
        var activeUserRole: String? = "ROLE_USER"
        val cachedOnboardingMap = mutableMapOf<Long, OnboardingStatus>()
        var currentUserResult: Result<UserProfile> = Result.failure(IllegalStateException("Not set"))
        var lastUpdatedStatus: OnboardingStatus? = null

        override fun isLoggedIn(): Boolean = activeUserId != null
        override fun getUserId(): Long? = activeUserId
        override fun getUserRole(): String? = activeUserRole
        override fun getUserEmail(): String? = "test@example.com"
        override fun getAccessToken(): String? = "fake_token"
        override fun saveToken(token: AuthToken) {}
        override fun clearAuth() {
            activeUserId = null
        }

        override fun getCachedOnboardingStatus(userId: Long?): OnboardingStatus {
            val id = userId ?: activeUserId ?: return OnboardingStatus.NOT_SHOWN
            return cachedOnboardingMap[id] ?: OnboardingStatus.NOT_SHOWN
        }

        override suspend fun getCurrentUser(): Result<UserProfile> = currentUserResult

        override suspend fun updateOnboardingStatus(status: OnboardingStatus): Result<UserProfile> {
            lastUpdatedStatus = status
            val currentId = activeUserId ?: 1L
            cachedOnboardingMap[currentId] = status
            val profile = UserProfile(
                id = currentId,
                email = "athlete@example.com",
                fullName = "Athlete",
                experienceLevel = "BEGINNER",
                role = activeUserRole ?: "ROLE_USER",
                onboardingStatus = status
            )
            return Result.success(profile)
        }

        override suspend fun login(email: String, password: String): Result<AuthToken> =
            Result.failure(UnsupportedOperationException())

        override suspend fun register(email: String, password: String, fullName: String): Result<RegistrationInitiation> =
            Result.failure(UnsupportedOperationException())
    }
}
