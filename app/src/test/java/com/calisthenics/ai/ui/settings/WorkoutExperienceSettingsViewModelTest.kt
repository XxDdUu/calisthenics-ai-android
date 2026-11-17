package com.calisthenics.ai.ui.settings

import com.calisthenics.ai.data.local.SetLoggingMode
import com.calisthenics.ai.data.local.WorkoutExperiencePreferences
import io.mockk.coVerify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutExperienceSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val preferences: WorkoutExperiencePreferences = mockk(relaxed = true)
    private val bubbleEnabledFlow = MutableStateFlow(true)
    private val loggingModeFlow = MutableStateFlow(SetLoggingMode.STEP_BY_STEP)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { preferences.unfinishedWorkoutBubbleEnabled } returns bubbleEnabledFlow
        every { preferences.setLoggingMode } returns loggingModeFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun bubbleEnabledToggle() = runTest {
        val vm = WorkoutExperienceSettingsViewModel(preferences)
        testDispatcher.scheduler.runCurrent()

        assertTrue(vm.bubbleEnabled.value)

        vm.setBubbleEnabled(false)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { preferences.setUnfinishedWorkoutBubbleEnabled(false) }
    }

    @Test
    fun loggingModeChange() = runTest {
        val vm = WorkoutExperienceSettingsViewModel(preferences)
        testDispatcher.scheduler.runCurrent()

        assertEquals(SetLoggingMode.STEP_BY_STEP, vm.loggingMode.value)

        vm.setLoggingMode(SetLoggingMode.SHOW_ALL)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { preferences.setSetLoggingMode(SetLoggingMode.SHOW_ALL) }
    }
}
