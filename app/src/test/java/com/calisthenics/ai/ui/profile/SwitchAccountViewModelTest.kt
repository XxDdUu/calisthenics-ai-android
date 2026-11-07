package com.calisthenics.ai.ui.profile

import com.calisthenics.ai.data.remote.websocket.ChatWebSocketClient
import com.calisthenics.ai.domain.model.SavedAccount
import com.calisthenics.ai.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SwitchAccountViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val chatWebSocketClient: ChatWebSocketClient = mockk(relaxed = true)

    private val accountA = SavedAccount(
        userId = 1L,
        email = "athlete@test.com",
        fullName = "Athlete One",
        avatarUrl = null,
        role = "ROLE_USER",
        isActive = true,
        lastActiveTimestamp = 1000L
    )

    private val accountB = SavedAccount(
        userId = 2L,
        email = "coach@test.com",
        fullName = "Coach Dave",
        avatarUrl = "https://example.com/coach.jpg",
        role = "ROLE_USER",
        isActive = false,
        lastActiveTimestamp = 500L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { authRepository.getSavedAccounts() } returns listOf(accountA, accountB)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SwitchAccountViewModel {
        return SwitchAccountViewModel(
            authRepository = authRepository,
            chatWebSocketClient = chatWebSocketClient
        )
    }

    @Test
    fun `initial state loads saved accounts and active user id`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(2, state.accounts.size)
        assertEquals(1L, state.activeUserId)
        assertEquals("athlete@test.com", state.accounts[0].email)
        assertTrue(state.accounts[0].isActive)
        assertFalse(state.accounts[1].isActive)
    }

    @Test
    fun `onAccountSelected switches account, disconnects websocket and sets switchSuccess`() = runTest {
        val viewModel = createViewModel()
        val updatedAccountB = accountB.copy(isActive = true)
        coEvery { authRepository.switchToAccount(2L) } returns Result.success(updatedAccountB)

        viewModel.onAccountSelected(accountB)
        dispatcher.scheduler.advanceUntilIdle()

        verify { chatWebSocketClient.disconnect() }
        coVerify { authRepository.switchToAccount(2L) }
        assertTrue(viewModel.uiState.value.switchSuccess)
        assertNull(viewModel.uiState.value.switchingUserId)
    }

    @Test
    fun `onAccountSelected ignores selection when account is already active`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAccountSelected(accountA)
        dispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { chatWebSocketClient.disconnect() }
        coVerify(exactly = 0) { authRepository.switchToAccount(any()) }
        assertFalse(viewModel.uiState.value.switchSuccess)
    }

    @Test
    fun `onAccountSelected sets error when switch fails`() = runTest {
        val viewModel = createViewModel()
        coEvery { authRepository.switchToAccount(2L) } returns Result.failure(IllegalStateException("Session expired"))

        viewModel.onAccountSelected(accountB)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Session expired", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.switchSuccess)
        assertNull(viewModel.uiState.value.switchingUserId)
    }

    @Test
    fun `requestRemoveAccount sets pending removal account and cancel clears it`() = runTest {
        val viewModel = createViewModel()

        viewModel.requestRemoveAccount(accountB)
        assertEquals(accountB, viewModel.uiState.value.accountPendingRemoval)

        viewModel.cancelRemoveAccount()
        assertNull(viewModel.uiState.value.accountPendingRemoval)
    }

    @Test
    fun `confirmRemoveAccount removes account from repository and updates list`() = runTest {
        val viewModel = createViewModel()
        every { authRepository.getSavedAccounts() } returns listOf(accountA)
        every { authRepository.removeAccount(2L) } returns true

        viewModel.requestRemoveAccount(accountB)
        viewModel.confirmRemoveAccount()
        dispatcher.scheduler.advanceUntilIdle()

        verify { authRepository.removeAccount(2L) }
        assertNull(viewModel.uiState.value.accountPendingRemoval)
        assertEquals(1, viewModel.uiState.value.accounts.size)
        assertEquals(1L, viewModel.uiState.value.accounts[0].userId)
    }

    @Test
    fun `confirmRemoveAccount on active account automatically switches to next available account`() = runTest {
        val viewModel = createViewModel()
        val remainingAccountB = accountB.copy(isActive = true)
        every { authRepository.getSavedAccounts() } returns listOf(accountB)
        every { authRepository.removeAccount(1L) } returns true
        coEvery { authRepository.switchToAccount(2L) } returns Result.success(remainingAccountB)

        viewModel.requestRemoveAccount(accountA)
        viewModel.confirmRemoveAccount()
        dispatcher.scheduler.advanceUntilIdle()

        verify { authRepository.removeAccount(1L) }
        coVerify { authRepository.switchToAccount(2L) }
    }
}

