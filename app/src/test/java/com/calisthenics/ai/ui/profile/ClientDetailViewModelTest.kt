package com.calisthenics.ai.ui.profile

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.ClientTrainingOverview
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.ConnectedCoachSummary
import com.calisthenics.ai.domain.repository.ChatRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.ProfileRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val profileRepository: ProfileRepository = mockk()
    private val coachClientRepository: CoachClientRepository = mockk()
    private val chatRepository: ChatRepository = mockk()

    private val sampleClient = ClientProfile(
        id = 2L,
        userId = 2L,
        fullName = "Client Athlete",
        displayName = "Client Athlete",
        connectedCoach = ConnectedCoachSummary(
            relationshipId = 88L,
            coachId = 1L,
            coachName = "Coach Dave",
            coachAvatarUrl = null,
            coachExperienceLevel = "ADVANCED"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { profileRepository.getClientProfile(2L) } returns Result.success(sampleClient)
        coEvery { profileRepository.getClientOverview(2L) } returns Result.success(ClientTrainingOverview(userId = 2L))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(clientId: Long = 2L): ClientDetailViewModel {
        return ClientDetailViewModel(
            profileRepository = profileRepository,
            coachClientRepository = coachClientRepository,
            chatRepository = chatRepository,
            savedStateHandle = SavedStateHandle(mapOf("clientId" to clientId))
        )
    }

    @Test
    fun `loads client profile and extracts active relationship ID`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(88L, state.activeRelationshipId)
        assertEquals(sampleClient, state.clientProfile)
    }

    @Test
    fun `terminateRelationship success triggers callback and resets loading`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { coachClientRepository.terminateRelationship(88L) } returns Result.success(
            CoachClientRelationship(
                id = 88L,
                coachId = 1L,
                coachName = "Coach Dave",
                coachAvatarUrl = null,
                coachExperienceLevel = "ADVANCED",
                clientId = 2L,
                clientName = "Client Athlete",
                clientAvatarUrl = null,
                clientExperienceLevel = "BEGINNER",
                requestedBy = 2L,
                status = "TERMINATED",
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-02T00:00:00Z"
            )
        )

        var onSuccessCalled = false
        viewModel.terminateRelationship {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertFalse(viewModel.uiState.value.isTerminating)
        coVerify(exactly = 1) { coachClientRepository.terminateRelationship(88L) }
    }

    @Test
    fun `terminateRelationship failure populates error state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { coachClientRepository.terminateRelationship(88L) } returns Result.failure(
            IllegalStateException("Network disconnected")
        )

        var onSuccessCalled = false
        viewModel.terminateRelationship {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        assertFalse(onSuccessCalled)
        assertFalse(viewModel.uiState.value.isTerminating)
        assertEquals("Network disconnected", viewModel.uiState.value.error)
    }
}
