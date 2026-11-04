package com.calisthenics.ai.ui.auth

import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.UserProfile
import com.calisthenics.ai.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun blankLoginIsRejectedWithoutCallingBackend() {
        val viewModel = AuthViewModel(repository)
        viewModel.login("", "")
        assertEquals("Enter your email and password", viewModel.state.value.error)
        assertEquals(0, repository.loginCalls)
    }

    @Test fun successfulLoginTransitionsToAuthenticated() = runTest {
        val viewModel = AuthViewModel(repository)
        viewModel.login("athlete@example.com", "password")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.authenticated)
        assertEquals(1, repository.loginCalls)
    }

    @Test fun failedLoginDisplaysSafeErrorAndDoesNotAuthenticate() = runTest {
        repository.loginResult = Result.failure(IllegalStateException("raw backend details"))
        val viewModel = AuthViewModel(repository)
        viewModel.login("athlete@example.com", "password")
        advanceUntilIdle()

        assertEquals("Đăng nhập thất bại. Vui lòng thử lại.", viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.authenticated)
        assertEquals(false, viewModel.state.value.loading)
    }

    @Test fun retryClearsPreviousLoginError() {
        val viewModel = AuthViewModel(repository)
        viewModel.login("", "")
        viewModel.login("athlete@example.com", "password")

        assertEquals(null, viewModel.state.value.error)
    }

    @Test fun successfulGoogleLoginTransitionsToAuthenticated() = runTest {
        val viewModel = AuthViewModel(repository)
        viewModel.loginWithGoogle("valid-google-id-token")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.authenticated)
        assertEquals(1, repository.googleLoginCalls)
    }

    @Test fun failedGoogleLoginDisplaysSafeErrorAndDoesNotAuthenticate() = runTest {
        repository.googleLoginResult = Result.failure(IllegalStateException("invalid token"))
        val viewModel = AuthViewModel(repository)
        viewModel.loginWithGoogle("bad-token")
        advanceUntilIdle()

        assertEquals("Đăng nhập thất bại. Vui lòng thử lại.", viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.authenticated)
        assertEquals(false, viewModel.state.value.loading)
        assertEquals(1, repository.googleLoginCalls)
    }

    @Test fun setErrorMessageUpdatesErrorAndClearsLoading() {
        val viewModel = AuthViewModel(repository)
        viewModel.setErrorMessage("Google Sign-In failed")
        assertEquals("Google Sign-In failed", viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.loading)
    }

    @Test fun startGoogleSignInSetsLoadingAndClearsPriorError() {
        val viewModel = AuthViewModel(repository)
        viewModel.setErrorMessage("Previous error")
        assertEquals("Previous error", viewModel.state.value.error)

        viewModel.startGoogleSignIn()
        assertEquals(true, viewModel.state.value.loading)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test fun cancelGoogleSignInClearsLoadingWithoutSettingError() {
        val viewModel = AuthViewModel(repository)
        viewModel.startGoogleSignIn()
        assertEquals(true, viewModel.state.value.loading)

        viewModel.cancelGoogleSignIn()
        assertEquals(false, viewModel.state.value.loading)
        assertEquals(null, viewModel.state.value.error)
    }

    private class FakeAuthRepository : AuthRepository {
        var loginCalls = 0
        var googleLoginCalls = 0
        var loginResult: Result<AuthToken>? = null
        var googleLoginResult: Result<AuthToken>? = null

        override suspend fun login(email: String, password: String): Result<AuthToken> {
            loginCalls++
            return loginResult ?: Result.success(AuthToken("access", "refresh", 1, email, "Athlete"))
        }

        override suspend fun loginWithGoogle(idToken: String): Result<AuthToken> {
            googleLoginCalls++
            return googleLoginResult ?: Result.success(AuthToken("access", "refresh", 1, "google@example.com", "Athlete"))
        }

        override suspend fun register(email: String, password: String, fullName: String) = Result.failure<AuthToken>(UnsupportedOperationException())
        override suspend fun getCurrentUser(): Result<UserProfile> = Result.failure(UnsupportedOperationException())
        override fun isLoggedIn() = false
        override fun saveToken(token: AuthToken) = Unit
        override fun getAccessToken(): String? = null
        override fun clearAuth() = Unit
    }
}
