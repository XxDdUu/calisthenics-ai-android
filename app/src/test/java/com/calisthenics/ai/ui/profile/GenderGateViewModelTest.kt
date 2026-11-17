package com.calisthenics.ai.ui.profile

import com.calisthenics.ai.data.local.GenderSessionStore
import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.UserProfile
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.RegistrationInitiation
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GenderGateViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeGenderAuthRepository
    private lateinit var genderSessionStore: GenderSessionStore
    private lateinit var viewModel: GenderGateViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        genderSessionStore = GenderSessionStore()
        authRepository = FakeGenderAuthRepository(genderSessionStore)
        viewModel = GenderGateViewModel(authRepository, genderSessionStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `new or existing user without gender triggers modal`() = runTest {
        authRepository.currentUserId = 10L
        authRepository.currentUserResult = Result.success(
            UserProfile(10L, "new@example.com", "New User", "INTERMEDIATE", "ROLE_USER", gender = null)
        )

        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.shouldShowModal)
        assertNull(viewModel.uiState.value.resolvedGender)
        assertEquals(1, authRepository.getCurrentUserCalls)
    }

    @Test
    fun `user selects Male and confirms saves to backend and dismisses modal`() = runTest {
        authRepository.currentUserId = 10L
        authRepository.currentUserResult = Result.success(
            UserProfile(10L, "user@example.com", "User", "INTERMEDIATE", "ROLE_USER", gender = null)
        )
        authRepository.updateGenderResult = Result.success(
            UserProfile(10L, "user@example.com", "User", "INTERMEDIATE", "ROLE_USER", gender = Gender.MALE)
        )

        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shouldShowModal)

        // Confirm without selecting does nothing
        viewModel.confirmGender()
        advanceUntilIdle()
        assertEquals(0, authRepository.updateGenderCalls)
        assertTrue(viewModel.uiState.value.shouldShowModal)

        viewModel.selectGender(Gender.MALE)
        viewModel.confirmGender()
        advanceUntilIdle()

        assertEquals(1, authRepository.updateGenderCalls)
        assertEquals(Gender.MALE, authRepository.lastUpdatedGender)
        assertFalse(viewModel.uiState.value.shouldShowModal)
        assertEquals(Gender.MALE, viewModel.uiState.value.resolvedGender)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `user selects Female and confirms saves successfully`() = runTest {
        authRepository.currentUserId = 11L
        authRepository.currentUserResult = Result.success(
            UserProfile(11L, "female@example.com", "Athlete", "INTERMEDIATE", "ROLE_USER", gender = null)
        )
        authRepository.updateGenderResult = Result.success(
            UserProfile(11L, "female@example.com", "Athlete", "INTERMEDIATE", "ROLE_USER", gender = Gender.FEMALE)
        )

        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()

        viewModel.selectGender(Gender.FEMALE)
        viewModel.confirmGender()
        advanceUntilIdle()

        assertEquals(Gender.FEMALE, authRepository.lastUpdatedGender)
        assertFalse(viewModel.uiState.value.shouldShowModal)
        assertEquals(Gender.FEMALE, viewModel.uiState.value.resolvedGender)
    }

    @Test
    fun `restarting app with configured gender never displays modal`() = runTest {
        authRepository.currentUserId = 12L
        authRepository.cachedGenders[12L] = Gender.MALE
        authRepository.currentUserResult = Result.success(
            UserProfile(12L, "restored@example.com", "Restored", "INTERMEDIATE", "ROLE_USER", gender = Gender.MALE)
        )

        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        assertFalse(viewModel.uiState.value.shouldShowModal)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.shouldShowModal)
        assertEquals(Gender.MALE, viewModel.uiState.value.resolvedGender)

        // Repeated syncs on recomposition/navigation do not re-fetch or show modal
        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        viewModel.retryVerificationIfNeeded(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()
        assertEquals(1, authRepository.getCurrentUserCalls)
        assertFalse(viewModel.uiState.value.shouldShowModal)
    }

    @Test
    fun `explicitly resetting gender on backend shows modal again on next launch`() = runTest {
        authRepository.currentUserId = 13L
        authRepository.cachedGenders[13L] = Gender.MALE
        authRepository.currentUserResult = Result.success(
            UserProfile(13L, "reset@example.com", "Reset User", "INTERMEDIATE", "ROLE_USER", gender = null)
        )

        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.shouldShowModal)
        assertNull(viewModel.uiState.value.resolvedGender)
    }

    @Test
    fun `offline or API failure on getCurrentUser does not mark gender as configured and retries when online`() = runTest {
        authRepository.currentUserId = 14L
        authRepository.currentUserResult = Result.failure(IOException("Network unavailable"))

        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()

        // Must NOT be marked checked/configured
        assertNull(viewModel.uiState.value.checkedOnlineForUserId)
        assertFalse(viewModel.uiState.value.shouldShowModal)

        // When network recovers and retry runs, backend returns null -> modal appears
        authRepository.currentUserResult = Result.success(
            UserProfile(14L, "retry@example.com", "Retry User", "INTERMEDIATE", "ROLE_USER", gender = null)
        )
        viewModel.retryVerificationIfNeeded(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()

        assertEquals(2, authRepository.getCurrentUserCalls)
        assertTrue(viewModel.uiState.value.shouldShowModal)
    }

    @Test
    fun `offline or API failure on confirm keeps modal open and preserves selection`() = runTest {
        authRepository.currentUserId = 15L
        authRepository.currentUserResult = Result.success(
            UserProfile(15L, "fail@example.com", "Fail Save", "INTERMEDIATE", "ROLE_USER", gender = null)
        )
        authRepository.updateGenderResult = Result.failure(IOException("Timeout"))

        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shouldShowModal)

        viewModel.selectGender(Gender.FEMALE)
        viewModel.confirmGender()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.shouldShowModal)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(Gender.FEMALE, viewModel.uiState.value.selectedGender)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(authRepository.cachedGenders[15L])
    }

    @Test
    fun `switching from configured user to user without gender shows modal for new user`() = runTest {
        authRepository.currentUserId = 1L
        authRepository.currentUserResult = Result.success(
            UserProfile(1L, "one@example.com", "One", "INTERMEDIATE", "ROLE_USER", gender = Gender.MALE)
        )

        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.shouldShowModal)

        // Switch to account 2 without gender
        authRepository.currentUserId = 2L
        authRepository.currentUserResult = Result.success(
            UserProfile(2L, "two@example.com", "Two", "INTERMEDIATE", "ROLE_USER", gender = null)
        )
        viewModel.syncSession(isAuthenticated = true, isOnline = true)
        advanceUntilIdle()

        assertEquals(2L, viewModel.uiState.value.activeUserId)
        assertTrue(viewModel.uiState.value.shouldShowModal)
    }
}

private class FakeGenderAuthRepository(
    private val genderSessionStore: GenderSessionStore
) : AuthRepository {
    var currentUserId: Long? = null
    val cachedGenders = mutableMapOf<Long, Gender?>()
    var currentUserResult: Result<UserProfile> = Result.failure(UnsupportedOperationException())
    var updateGenderResult: Result<UserProfile> = Result.failure(UnsupportedOperationException())
    var getCurrentUserCalls = 0
    var updateGenderCalls = 0
    var lastUpdatedGender: Gender? = null

    override suspend fun login(email: String, password: String): Result<AuthToken> =
        Result.failure(UnsupportedOperationException())

    override suspend fun register(email: String, password: String, fullName: String): Result<RegistrationInitiation> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getCurrentUser(): Result<UserProfile> {
        getCurrentUserCalls++
        return currentUserResult.onSuccess { profile ->
            cachedGenders[profile.id] = profile.gender
            genderSessionStore.publish(profile.id, profile.gender, isBackendConfirmed = true)
        }
    }

    override suspend fun updateGender(gender: Gender): Result<UserProfile> {
        updateGenderCalls++
        lastUpdatedGender = gender
        return updateGenderResult.onSuccess { profile ->
            cachedGenders[profile.id] = profile.gender
            genderSessionStore.publish(profile.id, profile.gender, isBackendConfirmed = true)
        }
    }

    override fun getCachedGender(userId: Long?): Gender? {
        val id = userId ?: currentUserId ?: return null
        return cachedGenders[id]
    }

    override fun isLoggedIn(): Boolean = currentUserId != null
    override fun getUserId(): Long? = currentUserId
    override fun saveToken(token: AuthToken) = Unit
    override fun getAccessToken(): String? = "token"
    override fun clearAuth() {
        currentUserId = null
        genderSessionStore.clear()
    }
}
