package com.calisthenics.ai.ui.workoutsession.bubble

import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.SetLoggingMode
import com.calisthenics.ai.data.local.WorkoutExperiencePreferences
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionExercise
import com.calisthenics.ai.domain.model.WorkoutSessionSet
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class UnfinishedWorkoutBubbleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val workoutSessionRepository: WorkoutSessionRepository = mockk(relaxed = true)
    private val workoutExperiencePreferences: WorkoutExperiencePreferences = mockk(relaxed = true)
    private val bubbleEnabledFlow = MutableStateFlow(true)
    private val loggingModeFlow = MutableStateFlow(SetLoggingMode.STEP_BY_STEP)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { workoutExperiencePreferences.unfinishedWorkoutBubbleEnabled } returns bubbleEnabledFlow
        every { workoutExperiencePreferences.setLoggingMode } returns loggingModeFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleUnfinishedSession(): WorkoutSession {
        val exercise1 = WorkoutSessionExercise(
            id = 101L,
            version = 1L,
            exerciseId = 1L,
            exerciseName = "Weighted Pull-Up",
            instructions = null,
            notes = null,
            orderIndex = 0,
            targetSets = 3,
            targetReps = 6,
            targetHoldDurationSeconds = null,
            restSeconds = 90,
            status = "IN_PROGRESS",
            sets = listOf(
                WorkoutSessionSet(1001L, 1L, 1, 6, null, 10.0, null, true, "2026-11-13T10:00:00Z", null),
                WorkoutSessionSet(1002L, 1L, 2, 6, null, 10.0, null, false, null, null),
                WorkoutSessionSet(1003L, 1L, 3, 6, null, 10.0, null, false, null, null)
            )
        )
        val exercise2 = WorkoutSessionExercise(
            id = 102L,
            version = 1L,
            exerciseId = 2L,
            exerciseName = "Tuck Planche Hold",
            instructions = null,
            notes = null,
            orderIndex = 1,
            targetSets = 2,
            targetReps = null,
            targetHoldDurationSeconds = 15,
            restSeconds = 60,
            status = "IN_PROGRESS",
            sets = listOf(
                WorkoutSessionSet(2001L, 1L, 1, null, 15, null, null, false, null, null),
                WorkoutSessionSet(2002L, 1L, 2, null, 15, null, null, false, null, null)
            )
        )
        return WorkoutSession(
            id = 555L,
            workoutPlanId = 10L,
            workoutPlanDayId = 20L,
            assignmentId = null,
            source = "PERSONAL",
            title = "Upper Body Strength",
            workoutDate = LocalDate.now().toString(),
            status = "IN_PROGRESS",
            startedAt = "2026-11-13T09:45:00Z",
            lastSavedAt = "2026-11-13T10:00:00Z",
            completedAt = null,
            durationSeconds = 900L,
            completedExercises = 0,
            skippedExercises = 0,
            unfinishedExercises = 2,
            completedSets = 1,
            totalReps = 6,
            totalHoldDurationSeconds = 0,
            exercises = listOf(exercise1, exercise2),
            classification = "UNFINISHED"
        )
    }

    @Test
    fun bubbleShowsInformationWhenActiveUnfinishedSessionExists() = runTest {
        val session = sampleUnfinishedSession()
        coEvery { workoutSessionRepository.getActiveSessions() } returns Result.success(listOf(session))

        val vm = UnfinishedWorkoutBubbleViewModel(
            workoutSessionRepository = workoutSessionRepository,
            workoutExperiencePreferences = workoutExperiencePreferences,
            accountSessionManager = AccountSessionManager()
        )
        testDispatcher.scheduler.runCurrent()
        vm.stopPolling()

        val state = vm.state.value
        assertTrue(state.isVisible)
        assertTrue(state.isEnabled)
        assertFalse(state.isDismissed)
        assertNotNull(state.activeSession)
        assertEquals(555L, state.activeSession?.id)
        assertEquals("Upper Body Strength", state.workoutTitle)
        assertEquals("Weighted Pull-Up", state.currentExerciseName)
        assertEquals(1, state.completedSets)
        assertEquals(5, state.totalSets)
        assertEquals(0.2f, state.progress, 0.01f)
    }

    @Test
    fun bubbleDismissalHidesBubbleUntilNextSession() = runTest {
        val session = sampleUnfinishedSession()
        coEvery { workoutSessionRepository.getActiveSessions() } returns Result.success(listOf(session))

        val vm = UnfinishedWorkoutBubbleViewModel(
            workoutSessionRepository = workoutSessionRepository,
            workoutExperiencePreferences = workoutExperiencePreferences,
            accountSessionManager = AccountSessionManager()
        )
        testDispatcher.scheduler.runCurrent()
        vm.stopPolling()
        assertTrue(vm.state.value.isVisible)

        // Dismiss bubble
        vm.dismiss()
        assertTrue(vm.state.value.isDismissed)
        assertFalse(vm.state.value.isVisible)

        // Minimize and expand toggle
        vm.toggleMinimized()
        assertTrue(vm.state.value.isMinimized)
        vm.toggleMinimized()
        assertFalse(vm.state.value.isMinimized)
    }

    @Test
    fun openBubbleRestoresDismissedAndMinimizedBubble() = runTest {
        val session = sampleUnfinishedSession()
        coEvery { workoutSessionRepository.getActiveSessions() } returns Result.success(listOf(session))

        val vm = UnfinishedWorkoutBubbleViewModel(
            workoutSessionRepository = workoutSessionRepository,
            workoutExperiencePreferences = workoutExperiencePreferences,
            accountSessionManager = AccountSessionManager()
        )
        testDispatcher.scheduler.runCurrent()
        vm.stopPolling()

        // User dismisses bubble
        vm.dismiss()
        assertTrue(vm.state.value.isDismissed)
        assertFalse(vm.state.value.isVisible)

        // User taps "Open Bubble" from Home card
        vm.openBubble()
        testDispatcher.scheduler.runCurrent()

        // Bubble is now visible, not dismissed, and not minimized
        assertTrue(vm.state.value.isVisible)
        assertFalse(vm.state.value.isDismissed)
        assertFalse(vm.state.value.isMinimized)
    }

    @Test
    fun bubbleHiddenWhenNoActiveSessionOrDisabled() = runTest {
        coEvery { workoutSessionRepository.getActiveSessions() } returns Result.success(emptyList())

        val vm = UnfinishedWorkoutBubbleViewModel(
            workoutSessionRepository = workoutSessionRepository,
            workoutExperiencePreferences = workoutExperiencePreferences,
            accountSessionManager = AccountSessionManager()
        )
        testDispatcher.scheduler.runCurrent()
        vm.stopPolling()

        assertFalse(vm.state.value.isVisible)
        assertNull(vm.state.value.activeSession)

        // When disabled via preferences
        bubbleEnabledFlow.value = false
        testDispatcher.scheduler.runCurrent()
        assertFalse(vm.state.value.isEnabled)
        assertFalse(vm.state.value.isVisible)
    }
}
