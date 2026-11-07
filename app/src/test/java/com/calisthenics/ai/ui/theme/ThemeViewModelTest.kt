package com.calisthenics.ai.ui.theme

import com.calisthenics.ai.data.local.ThemePreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val themeFlow = MutableStateFlow(ThemeMode.DARK)
    private val preferences = mockk<ThemePreferences>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { preferences.themeMode } returns themeFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial theme mode is DARK by default`() = runTest {
        val viewModel = ThemeViewModel(preferences)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `setThemeMode LIGHT triggers preference update`() = runTest {
        val viewModel = ThemeViewModel(preferences)
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        coVerify { preferences.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `setThemeMode NEUTRAL triggers preference update`() = runTest {
        val viewModel = ThemeViewModel(preferences)
        viewModel.setThemeMode(ThemeMode.NEUTRAL)
        advanceUntilIdle()

        coVerify { preferences.setThemeMode(ThemeMode.NEUTRAL) }
    }

    @Test
    fun `themeMode enum fromName parses correctly with case insensitivity and fallback`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromName("light"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromName("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("dark"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
        assertEquals(ThemeMode.NEUTRAL, ThemeMode.fromName("neutral"))
        assertEquals(ThemeMode.NEUTRAL, ThemeMode.fromName("NEUTRAL"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("system"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("invalid"))
    }
}

