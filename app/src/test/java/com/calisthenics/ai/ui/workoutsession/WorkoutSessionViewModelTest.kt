package com.calisthenics.ai.ui.workoutsession

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.data.local.WorkoutTimerPreferences
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionExercise
import com.calisthenics.ai.domain.model.WorkoutSessionSet
import com.calisthenics.ai.domain.model.WorkoutSessionSetUpdate
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import com.calisthenics.ai.ui.workoutsession.timer.ExerciseTimerConfig
import com.calisthenics.ai.ui.workoutsession.timer.TimeSource
import com.calisthenics.ai.ui.workoutsession.timer.TimerStatus
import com.calisthenics.ai.ui.workoutsession.timer.WorkoutAudioHapticHelper
import com.calisthenics.ai.ui.workoutsession.timer.WorkoutNotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTimeSource(var currentTime: Long = 100_000L) : TimeSource {
    override fun elapsedRealtime(): Long = currentTime

    fun advanceBy(millis: Long) {
        currentTime += millis
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val sessionRepository: WorkoutSessionRepository = mockk(relaxed = true)
    private val timerPreferences: WorkoutTimerPreferences = mockk(relaxed = true)
    private val audioHapticHelper: WorkoutAudioHapticHelper = mockk(relaxed = true)
    private val notificationHelper: WorkoutNotificationHelper = mockk(relaxed = true)
    private val fakeTimeSource = FakeTimeSource()

    private val savedStateHandle = SavedStateHandle(
        mapOf(
            "date" to "2026-11-02",
            "planId" to 10L,
            "dayId" to 20L
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(session: WorkoutSession = sampleSession()): WorkoutSessionViewModel {
        coEvery { sessionRepository.startOrResume(any(), any(), any(), any()) } returns Result.success(session)
        coEvery { sessionRepository.getSession(any()) } returns Result.success(session)
        coEvery { sessionRepository.hasPendingChanges(any()) } returns false
        coEvery { sessionRepository.hasSyncConflict(any()) } returns false

        val vm = WorkoutSessionViewModel(
            sessionRepository = sessionRepository,
            savedStateHandle = savedStateHandle,
            timerPreferences = timerPreferences,
            audioHapticHelper = audioHapticHelper,
            notificationHelper = notificationHelper,
            timeSource = fakeTimeSource
        )
        testDispatcher.scheduler.runCurrent()
        vm.stopElapsedTimer()
        return vm
    }

    private fun sampleSession(
        sets: List<WorkoutSessionSet> = listOf(
            WorkoutSessionSet(
                id = 201L,
                version = 1L,
                setNumber = 1,
                reps = null,
                holdDurationSeconds = null,
                weightKg = null,
                assistanceWeightKg = null,
                completed = false,
                completedAt = null,
                notes = null
            ),
            WorkoutSessionSet(
                id = 202L,
                version = 1L,
                setNumber = 2,
                reps = null,
                holdDurationSeconds = null,
                weightKg = null,
                assistanceWeightKg = null,
                completed = false,
                completedAt = null,
                notes = null
            )
        )
    ): WorkoutSession {
        val exercise = WorkoutSessionExercise(
            id = 101L,
            version = 1L,
            exerciseId = 11L,
            exerciseName = "Planche Hold",
            instructions = null,
            notes = null,
            orderIndex = 0,
            targetSets = sets.size,
            targetReps = null,
            targetHoldDurationSeconds = 15,
            restSeconds = 90,
            status = "IN_PROGRESS",
            sets = sets
        )
        return WorkoutSession(
            id = 1L,
            workoutPlanId = 10L,
            workoutPlanDayId = 20L,
            assignmentId = null,
            source = "PERSONAL",
            title = "Calisthenics Hold Session",
            workoutDate = "2026-11-02",
            status = "IN_PROGRESS",
            startedAt = "2026-11-02T10:00:00Z",
            lastSavedAt = null,
            completedAt = null,
            durationSeconds = 0L,
            completedExercises = 0,
            skippedExercises = 0,
            unfinishedExercises = 1,
            completedSets = 0,
            totalReps = 0,
            totalHoldDurationSeconds = 0,
            exercises = listOf(exercise)
        )
    }

    @Test
    fun modeACountdownEnabledCountsDownAndStartsHolding() = runTest {
        every { timerPreferences.getTimerConfig("Planche Hold", any()) } returns ExerciseTimerConfig(
            countdownEnabled = true,
            countdownDurationSeconds = 3,
            restTimerEnabled = true,
            restDurationSeconds = 90
        )
        val vm = createViewModel()

        val exercise = vm.uiState.value.session!!.exercises[0]
        val set = exercise.sets[0]

        vm.startHoldTimer(exercise, set)
        testDispatcher.scheduler.runCurrent()

        assertEquals(TimerStatus.COUNTDOWN, vm.uiState.value.activeTimer.status)
        assertEquals(3, vm.uiState.value.activeTimer.countdownRemainingSeconds)
        verify { audioHapticHelper.playCountdownTick(true, true) }

        // Advance 1 second
        fakeTimeSource.advanceBy(1000L)
        testDispatcher.scheduler.advanceTimeBy(150L)
        assertEquals(2, vm.uiState.value.activeTimer.countdownRemainingSeconds)

        // Advance past countdown (2 more seconds)
        fakeTimeSource.advanceBy(2000L)
        testDispatcher.scheduler.advanceTimeBy(200L)

        // Should transition automatically to HOLDING
        assertEquals(TimerStatus.HOLDING, vm.uiState.value.activeTimer.status)
        verify { audioHapticHelper.playHoldStart(true, true) }

        vm.cancelTimer()
    }

    @Test
    fun modeBCountdownDisabledStartsImmediately() = runTest {
        every { timerPreferences.getTimerConfig("Planche Hold", any()) } returns ExerciseTimerConfig(
            countdownEnabled = false,
            countdownDurationSeconds = 3
        )
        val vm = createViewModel()

        val exercise = vm.uiState.value.session!!.exercises[0]
        val set = exercise.sets[0]

        vm.startHoldTimer(exercise, set)
        testDispatcher.scheduler.runCurrent()

        // Directly enters HOLDING
        assertEquals(TimerStatus.HOLDING, vm.uiState.value.activeTimer.status)
        assertEquals(0L, vm.uiState.value.activeTimer.holdElapsedMillis)
        verify { audioHapticHelper.playHoldStart(true, true) }

        vm.cancelTimer()
    }

    @Test
    fun customCountdownDurationIsRespected() = runTest {
        every { timerPreferences.getTimerConfig("Planche Hold", any()) } returns ExerciseTimerConfig(
            countdownEnabled = true,
            countdownDurationSeconds = 10
        )
        val vm = createViewModel()

        val exercise = vm.uiState.value.session!!.exercises[0]
        val set = exercise.sets[0]

        vm.startHoldTimer(exercise, set)
        testDispatcher.scheduler.runCurrent()

        assertEquals(TimerStatus.COUNTDOWN, vm.uiState.value.activeTimer.status)
        assertEquals(10, vm.uiState.value.activeTimer.countdownTotalSeconds)
        assertEquals(10, vm.uiState.value.activeTimer.countdownRemainingSeconds)

        vm.cancelTimer()
    }

    @Test
    fun holdTimerTracksMonotonicTimeAccurately() = runTest {
        every { timerPreferences.getTimerConfig("Planche Hold", any()) } returns ExerciseTimerConfig(
            countdownEnabled = false
        )
        val vm = createViewModel()

        val exercise = vm.uiState.value.session!!.exercises[0]
        val set = exercise.sets[0]

        vm.startHoldTimer(exercise, set)
        testDispatcher.scheduler.runCurrent()
        assertEquals(TimerStatus.HOLDING, vm.uiState.value.activeTimer.status)

        // Advance monotonic time by 12,450ms
        fakeTimeSource.advanceBy(12_450L)
        testDispatcher.scheduler.advanceTimeBy(60L)

        assertEquals(12_450L, vm.uiState.value.activeTimer.holdElapsedMillis)

        vm.cancelTimer()
    }

    @Test
    fun stopHoldTimerTransitionsToSetCompletedWithAccurateDuration() = runTest {
        every { timerPreferences.getTimerConfig("Planche Hold", any()) } returns ExerciseTimerConfig(
            countdownEnabled = false
        )
        val vm = createViewModel()

        val exercise = vm.uiState.value.session!!.exercises[0]
        val set = exercise.sets[0]

        vm.startHoldTimer(exercise, set)
        testDispatcher.scheduler.runCurrent()

        fakeTimeSource.advanceBy(14_600L)
        vm.stopHoldTimer()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TimerStatus.SET_COMPLETED, vm.uiState.value.activeTimer.status)
        assertEquals(14.6, vm.uiState.value.activeTimer.finalHoldDurationSeconds, 0.01)
        assertEquals("14.60s", vm.uiState.value.activeTimer.formattedHoldDuration)
        verify { audioHapticHelper.playHoldStop(true, true) }
    }

    @Test
    fun cancelTimerResetsToIdle() = runTest {
        every { timerPreferences.getTimerConfig("Planche Hold", any()) } returns ExerciseTimerConfig(
            countdownEnabled = true,
            countdownDurationSeconds = 5
        )
        val vm = createViewModel()

        val exercise = vm.uiState.value.session!!.exercises[0]
        val set = exercise.sets[0]

        vm.startHoldTimer(exercise, set)
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.activeTimer.isTimerActive)

        vm.cancelTimer()
        testDispatcher.scheduler.runCurrent()

        assertFalse(vm.uiState.value.activeTimer.isTimerActive)
        assertEquals(TimerStatus.IDLE, vm.uiState.value.activeTimer.status)
        verify { notificationHelper.cancelRestNotification() }
    }

    @Test
    fun retryHoldTimerRestartsFromCompletedState() = runTest {
        every { timerPreferences.getTimerConfig("Planche Hold", any()) } returns ExerciseTimerConfig(
            countdownEnabled = false
        )
        val vm = createViewModel()

        val exercise = vm.uiState.value.session!!.exercises[0]
        val set = exercise.sets[0]

        vm.startHoldTimer(exercise, set)
        testDispatcher.scheduler.runCurrent()

        fakeTimeSource.advanceBy(5000L)
        vm.stopHoldTimer()
        testDispatcher.scheduler.runCurrent()
        assertEquals(TimerStatus.SET_COMPLETED, vm.uiState.value.activeTimer.status)

        vm.retryHoldTimer()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TimerStatus.HOLDING, vm.uiState.value.activeTimer.status)
        assertEquals(0L, vm.uiState.value.activeTimer.holdElapsedMillis)

        vm.cancelTimer()
    }

    @Test
    fun saveSetFromTimerPersistsHoldDurationAndTriggersRestTimerWhenSetsRemain() = runTest {
        val initialSession = sampleSession()
        every { timerPreferences.getTimerConfig("Planche Hold", any()) } returns ExerciseTimerConfig(
            countdownEnabled = false,
            restTimerEnabled = true,
            restDurationSeconds = 60
        )
        val updatedSetSession = initialSession.copy(
            exercises = listOf(
                initialSession.exercises[0].copy(
                    sets = listOf(
                        initialSession.exercises[0].sets[0].copy(
                            holdDurationSeconds = 15,
                            completed = true,
                            version = 2L
                        ),
                        initialSession.exercises[0].sets[1]
                    )
                )
            )
        )
        coEvery {
            sessionRepository.updateSet(any(), eq(101L), eq(201L), any())
        } returns Result.success(updatedSetSession)
        coEvery {
            sessionRepository.saveSetDraft(any(), any(), any(), any())
        } returns Result.success(Unit)

        val vm = createViewModel(initialSession)

        val exercise = initialSession.exercises[0]
        val set = exercise.sets[0]

        vm.startHoldTimer(exercise, set)
        testDispatcher.scheduler.runCurrent()

        fakeTimeSource.advanceBy(15_200L)
        vm.stopHoldTimer()
        testDispatcher.scheduler.runCurrent()

        vm.saveSetFromTimer()
        testDispatcher.scheduler.advanceTimeBy(100L)
        testDispatcher.scheduler.runCurrent()

        // Verifies repo was called with holdDurationSeconds = 15
        coVerify {
            sessionRepository.updateSet(
                sessionId = 1L,
                exerciseId = 101L,
                setId = 201L,
                update = match { it.holdDurationSeconds == 15 && it.completed }
            )
        }

        // Rest timer should be automatically triggered because Set 2 remains
        assertEquals(TimerStatus.RESTING, vm.uiState.value.activeTimer.status)
        assertEquals(60, vm.uiState.value.activeTimer.restTotalSeconds)
        assertEquals(60, vm.uiState.value.activeTimer.restRemainingSeconds)
        assertEquals("Planche Hold", vm.uiState.value.activeTimer.nextExerciseName)
        assertEquals(2, vm.uiState.value.activeTimer.nextSetNumber)

        vm.cancelTimer()
    }

    @Test
    fun completingFinalSetDoesNotTriggerRestTimer() = runTest {
        val singleSetSession = sampleSession(
            sets = listOf(
                WorkoutSessionSet(
                    id = 201L,
                    version = 1L,
                    setNumber = 1,
                    reps = null,
                    holdDurationSeconds = null,
                    weightKg = null,
                    assistanceWeightKg = null,
                    completed = false,
                    completedAt = null,
                    notes = null
                )
            )
        )
        val finishedSession = singleSetSession.copy(
            exercises = listOf(
                singleSetSession.exercises[0].copy(
                    status = "COMPLETED",
                    sets = listOf(
                        singleSetSession.exercises[0].sets[0].copy(
                            holdDurationSeconds = 20,
                            completed = true,
                            version = 2L
                        )
                    )
                )
            )
        )
        every { timerPreferences.getTimerConfig("Planche Hold", any()) } returns ExerciseTimerConfig(
            countdownEnabled = false,
            restTimerEnabled = true,
            restDurationSeconds = 60
        )
        coEvery {
            sessionRepository.updateSet(any(), eq(101L), eq(201L), any())
        } returns Result.success(finishedSession)
        coEvery {
            sessionRepository.saveSetDraft(any(), any(), any(), any())
        } returns Result.success(Unit)

        val vm = createViewModel(singleSetSession)

        val exercise = singleSetSession.exercises[0]
        val set = exercise.sets[0]

        vm.startHoldTimer(exercise, set)
        testDispatcher.scheduler.runCurrent()

        fakeTimeSource.advanceBy(20_000L)
        vm.stopHoldTimer()
        testDispatcher.scheduler.runCurrent()

        vm.saveSetFromTimer()
        testDispatcher.scheduler.advanceTimeBy(100L)
        testDispatcher.scheduler.runCurrent()

        // Rest timer should NOT be triggered because there are no more sets
        assertEquals(TimerStatus.IDLE, vm.uiState.value.activeTimer.status)
    }

    @Test
    fun restTimerPauseAndResumeMaintainsAccurateRemainingTime() = runTest {
        val vm = createViewModel()

        vm.triggerRestTimer(
            restDurationSeconds = 90,
            config = ExerciseTimerConfig(soundEnabled = true, vibrationEnabled = true),
            nextExerciseName = "Planche Hold",
            nextSetNumber = 2
        )
        testDispatcher.scheduler.runCurrent()
        assertEquals(TimerStatus.RESTING, vm.uiState.value.activeTimer.status)

        // Rest for 30s
        fakeTimeSource.advanceBy(30_000L)
        testDispatcher.scheduler.advanceTimeBy(250L)
        assertEquals(60, vm.uiState.value.activeTimer.restRemainingSeconds)

        // Pause rest
        vm.pauseRestTimer()
        testDispatcher.scheduler.runCurrent()
        assertEquals(TimerStatus.REST_PAUSED, vm.uiState.value.activeTimer.status)

        // Monotonic time passes while paused (e.g. 45s)
        fakeTimeSource.advanceBy(45_000L)
        testDispatcher.scheduler.advanceTimeBy(300L)
        // Still paused with 60s remaining
        assertEquals(TimerStatus.REST_PAUSED, vm.uiState.value.activeTimer.status)
        assertEquals(60, vm.uiState.value.activeTimer.restRemainingSeconds)

        // Resume rest
        vm.resumeRestTimer()
        testDispatcher.scheduler.runCurrent()
        assertEquals(TimerStatus.RESTING, vm.uiState.value.activeTimer.status)

        // Advance 10s
        fakeTimeSource.advanceBy(10_000L)
        testDispatcher.scheduler.advanceTimeBy(250L)
        assertEquals(50, vm.uiState.value.activeTimer.restRemainingSeconds)

        vm.cancelTimer()
    }

    @Test
    fun restTimerAddSecondsExtendsTime() = runTest {
        val vm = createViewModel()

        vm.triggerRestTimer(
            restDurationSeconds = 60,
            config = ExerciseTimerConfig(),
            nextExerciseName = "Planche Hold",
            nextSetNumber = 2
        )
        testDispatcher.scheduler.runCurrent()

        // Add 15 seconds
        vm.addRestSeconds(15)
        assertEquals(75, vm.uiState.value.activeTimer.restTotalSeconds)
        assertEquals(75, vm.uiState.value.activeTimer.restRemainingSeconds)

        // Add 30 seconds
        vm.addRestSeconds(30)
        assertEquals(105, vm.uiState.value.activeTimer.restTotalSeconds)
        assertEquals(105, vm.uiState.value.activeTimer.restRemainingSeconds)

        vm.cancelTimer()
    }

    @Test
    fun restTimerSkipImmediatelyDismisses() = runTest {
        val vm = createViewModel()

        vm.triggerRestTimer(
            restDurationSeconds = 90,
            config = ExerciseTimerConfig(),
            nextExerciseName = "Planche Hold",
            nextSetNumber = 2
        )
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.activeTimer.isResting)

        vm.skipRestTimer()
        testDispatcher.scheduler.runCurrent()

        assertFalse(vm.uiState.value.activeTimer.isResting)
        assertEquals(TimerStatus.IDLE, vm.uiState.value.activeTimer.status)
        verify { notificationHelper.cancelRestNotification() }
    }

    @Test
    fun restTimerCompletionPlaysSignalsAndPostsNotification() = runTest {
        val vm = createViewModel()

        vm.triggerRestTimer(
            restDurationSeconds = 10,
            config = ExerciseTimerConfig(soundEnabled = true, vibrationEnabled = true),
            nextExerciseName = "Planche Hold",
            nextSetNumber = 2
        )
        testDispatcher.scheduler.runCurrent()

        fakeTimeSource.advanceBy(10_000L)
        testDispatcher.scheduler.advanceTimeBy(250L)

        assertEquals(TimerStatus.REST_COMPLETED, vm.uiState.value.activeTimer.status)
        assertEquals(0, vm.uiState.value.activeTimer.restRemainingSeconds)
        verify { audioHapticHelper.playRestCompleted(true, true) }
        verify { notificationHelper.showRestCompleteNotification("Planche Hold", 2) }

        // Manual advancement to next set
        vm.startNextSet()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TimerStatus.IDLE, vm.uiState.value.activeTimer.status)
        verify { notificationHelper.cancelRestNotification() }
    }

    @Test
    fun onAppForegroundedCatchesUpMonotonicTimeAccurately() = runTest {
        val vm = createViewModel()

        vm.triggerRestTimer(
            restDurationSeconds = 60,
            config = ExerciseTimerConfig(),
            nextExerciseName = "Planche Hold",
            nextSetNumber = 2
        )
        testDispatcher.scheduler.runCurrent()

        // App backgrounded, 65s passed
        fakeTimeSource.advanceBy(65_000L)
        vm.onAppForegrounded()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TimerStatus.REST_COMPLETED, vm.uiState.value.activeTimer.status)
        assertEquals(0, vm.uiState.value.activeTimer.restRemainingSeconds)
        verify { audioHapticHelper.playRestCompleted(any(), any()) }
        verify { notificationHelper.showRestCompleteNotification("Planche Hold", 2) }

        vm.cancelTimer()
    }

    @Test
    fun timerSettingsPreferencesAreLoadedAndPersisted() = runTest {
        val config = ExerciseTimerConfig(
            countdownEnabled = true,
            countdownDurationSeconds = 5,
            restTimerEnabled = true,
            restDurationSeconds = 120,
            soundEnabled = false,
            vibrationEnabled = true
        )
        every { timerPreferences.getTimerConfig("Front Lever", any()) } returns config

        val vm = createViewModel()

        vm.openTimerSettings("Front Lever", 90)
        assertTrue(vm.uiState.value.isTimerSettingsDialogVisible)
        assertEquals("Front Lever", vm.uiState.value.activeConfigExerciseName)
        assertEquals(config, vm.uiState.value.activeTimerConfig)

        val updated = config.copy(countdownDurationSeconds = 10, restDurationSeconds = 180)
        vm.saveTimerConfig(updated)

        assertFalse(vm.uiState.value.isTimerSettingsDialogVisible)
        verify { timerPreferences.saveTimerConfig("Front Lever", updated) }
    }
}
