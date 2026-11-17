package com.calisthenics.ai.ui.auth

import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.UserProfile
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.RegistrationInitiation
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
        assertEquals(1, repository.firebaseLoginCalls)
    }

    @Test fun successfulFirebaseLoginTransitionsToAuthenticated() = runTest {
        val viewModel = AuthViewModel(repository)
        viewModel.loginWithFirebase("valid-firebase-id-token", "req-fb")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.authenticated)
        assertEquals(1, repository.firebaseLoginCalls)
        assertEquals("valid-firebase-id-token", repository.lastFirebaseToken)
    }

    @Test fun failedFirebaseLoginDisplaysSafeErrorAndDoesNotAuthenticate() = runTest {
        repository.firebaseLoginResult = Result.failure(IllegalStateException("expired firebase token"))
        val viewModel = AuthViewModel(repository)
        viewModel.loginWithFirebase("expired-token")
        advanceUntilIdle()

        assertEquals("Đăng nhập thất bại. Vui lòng thử lại.", viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.authenticated)
        assertEquals(false, viewModel.state.value.loading)
        assertEquals(1, repository.firebaseLoginCalls)
    }

    @Test fun logoutClearsRepositoryAuthAndGoogleFirebaseCredentialState() = runTest {
        val googleAuthManager = io.mockk.mockk<GoogleAuthManager>(relaxed = true)
        val viewModel = AuthViewModel(repository = repository, googleAuthManager = googleAuthManager)
        viewModel.loginWithFirebase("valid-firebase-id-token")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.authenticated)

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.authenticated)
        assertEquals(1, repository.clearAuthCalls)
        io.mockk.coVerify(exactly = 1) { googleAuthManager.clearCredentialState() }
    }

    @Test fun failedGoogleLoginDisplaysSafeErrorAndDoesNotAuthenticate() = runTest {
        repository.firebaseLoginResult = Result.failure(IllegalStateException("invalid token"))
        val viewModel = AuthViewModel(repository)
        viewModel.loginWithGoogle("bad-token")
        advanceUntilIdle()

        assertEquals("Đăng nhập thất bại. Vui lòng thử lại.", viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.authenticated)
        assertEquals(false, viewModel.state.value.loading)
        assertEquals(1, repository.firebaseLoginCalls)
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

    @Test fun initiateSwitchAccountDeactivatesSessionAndSetsSwitchingState() {
        val viewModel = AuthViewModel(repository)
        viewModel.initiateSwitchAccount()

        assertEquals(1, repository.deactivateSessionCalls)
        assertEquals(false, viewModel.state.value.authenticated)
        assertTrue(viewModel.state.value.authState is AuthState.SwitchingAccount)
    }

    @Test fun prepareForAddAccountDeactivatesSessionAndKeepsUnauthenticated() {
        val viewModel = AuthViewModel(repository)
        viewModel.prepareForAddAccount()

        assertEquals(1, repository.deactivateSessionCalls)
        assertEquals(false, viewModel.state.value.authenticated)
        assertTrue(viewModel.state.value.authState is AuthState.SwitchingAccount)
    }

    @Test fun switchToAccountTransitionsToAuthenticatedOnSuccess() = runTest {
        val viewModel = AuthViewModel(repository)
        viewModel.switchToAccount(2L)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.authenticated)
        assertTrue(viewModel.state.value.authState is AuthState.Authenticated)
        assertEquals(2L, (viewModel.state.value.authState as AuthState.Authenticated).userId)
    }

    @Test fun switchToAccountSetsErrorAndStaysUnauthenticatedOnFailure() = runTest {
        repository.switchResult = Result.failure(IllegalStateException("Account not found"))
        val viewModel = AuthViewModel(repository)
        viewModel.switchToAccount(99L)
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.authenticated)
        assertTrue(viewModel.state.value.authState is AuthState.Error)
        assertEquals("Account not found", viewModel.state.value.error)
    }

    @Test fun logoutClearsAuthAndSetsUnauthenticated() {
        val viewModel = AuthViewModel(repository)
        viewModel.logout()

        assertEquals(1, repository.clearAuthCalls)
        assertEquals(false, viewModel.state.value.authenticated)
        assertTrue(viewModel.state.value.authState is AuthState.Unauthenticated)
    }

    @Test fun blankRegisterIsRejectedWithoutCallingBackend() {
        val viewModel = AuthViewModel(repository)
        viewModel.register("", "", "")
        assertEquals("Complete all fields to create your account", viewModel.state.value.error)
        assertEquals(0, repository.registerCalls)
    }

    @Test fun invalidEmailOrShortPasswordRejected() {
        val viewModel = AuthViewModel(repository)
        viewModel.register("Test User", "invalid-email", "pass1234")
        assertEquals("Please enter a valid email address", viewModel.state.value.error)
        assertEquals(0, repository.registerCalls)

        viewModel.register("Test User", "user@example.com", "short")
        assertEquals("Use a password with at least 8 characters", viewModel.state.value.error)
        assertEquals(0, repository.registerCalls)
    }

    @Test fun successfulRegisterTransitionsToOtpPending() = runTest {
        val viewModel = AuthViewModel(repository)
        var navigatedEmail: String? = null
        viewModel.register("Test Athlete", "athlete@example.com", "Password123!") { email ->
            navigatedEmail = email
        }
        advanceUntilIdle()

        assertEquals(1, repository.registerCalls)
        assertEquals(true, viewModel.state.value.registrationOtpSent)
        assertEquals("athlete@example.com", viewModel.state.value.registrationPendingEmail)
        assertEquals("athlete@example.com", navigatedEmail)
        assertEquals(false, viewModel.state.value.authenticated)
    }

    @Test fun failedRegisterDisplaysError() = runTest {
        repository.registerResult = Result.failure(IllegalStateException("Email already taken"))
        val viewModel = AuthViewModel(repository)
        viewModel.register("Test Athlete", "athlete@example.com", "Password123!")
        advanceUntilIdle()

        assertEquals(1, repository.registerCalls)
        assertEquals(false, viewModel.state.value.registrationOtpSent)
        assertEquals("Email already taken", viewModel.state.value.error)
    }

    @Test fun successfulOtpVerificationTransitionsToAuthenticated() = runTest {
        val viewModel = AuthViewModel(repository)
        viewModel.register("Test Athlete", "athlete@example.com", "Password123!")
        advanceUntilIdle()

        viewModel.verifyRegisterOtp("123456")
        advanceUntilIdle()

        assertEquals(1, repository.verifyRegistrationOtpCalls)
        assertEquals(true, viewModel.state.value.authenticated)
        assertEquals(null, viewModel.state.value.registrationPendingEmail)
        assertEquals(false, viewModel.state.value.registrationOtpSent)
    }

    @Test fun failedOtpVerificationDisplaysOtpError() = runTest {
        repository.verifyRegistrationOtpResult = Result.failure(IllegalStateException("Invalid OTP"))
        val viewModel = AuthViewModel(repository)
        viewModel.register("Test Athlete", "athlete@example.com", "Password123!")
        advanceUntilIdle()

        viewModel.verifyRegisterOtp("999999")
        advanceUntilIdle()

        assertEquals(1, repository.verifyRegistrationOtpCalls)
        assertEquals(false, viewModel.state.value.authenticated)
        assertTrue(viewModel.state.value.otpError != null)
    }

    private class FakeAuthRepository : AuthRepository {
        var loginCalls = 0
        var googleLoginCalls = 0
        var firebaseLoginCalls = 0
        var lastFirebaseToken: String? = null
        var deactivateSessionCalls = 0
        var clearAuthCalls = 0
        var registerCalls = 0
        var verifyRegistrationOtpCalls = 0
        var resendRegistrationOtpCalls = 0
        var loginResult: Result<AuthToken>? = null
        var googleLoginResult: Result<AuthToken>? = null
        var firebaseLoginResult: Result<AuthToken>? = null
        var registerResult: Result<RegistrationInitiation>? = null
        var verifyRegistrationOtpResult: Result<AuthToken>? = null
        var resendRegistrationOtpResult: Result<RegistrationInitiation>? = null
        var switchResult: Result<com.calisthenics.ai.domain.model.SavedAccount>? = null

        override suspend fun login(email: String, password: String): Result<AuthToken> {
            loginCalls++
            return loginResult ?: Result.success(AuthToken("access", "refresh", 1, email, "Athlete"))
        }

        override suspend fun loginWithFirebase(idToken: String): Result<AuthToken> {
            firebaseLoginCalls++
            lastFirebaseToken = idToken
            return firebaseLoginResult ?: Result.success(AuthToken("access", "refresh", 1, "firebase@example.com", "Athlete"))
        }

        override suspend fun loginWithGoogle(idToken: String): Result<AuthToken> {
            googleLoginCalls++
            return googleLoginResult ?: Result.success(AuthToken("access", "refresh", 1, "google@example.com", "Athlete"))
        }

        override suspend fun register(email: String, password: String, fullName: String): Result<RegistrationInitiation> {
            registerCalls++
            return registerResult ?: Result.success(
                RegistrationInitiation(
                    email = email,
                    message = "OTP verification code sent",
                    expiresIn = 300,
                    resendCooldown = 30
                )
            )
        }

        override suspend fun verifyRegistrationOtp(email: String, otp: String): Result<AuthToken> {
            verifyRegistrationOtpCalls++
            return verifyRegistrationOtpResult ?: Result.success(
                AuthToken(
                    accessToken = "access_token_123",
                    refreshToken = "refresh_token_123",
                    userId = 100L,
                    email = email,
                    fullName = "Test Athlete",
                    role = "ROLE_USER"
                )
            )
        }

        override suspend fun resendRegistrationOtp(email: String): Result<RegistrationInitiation> {
            resendRegistrationOtpCalls++
            return resendRegistrationOtpResult ?: Result.success(
                RegistrationInitiation(
                    email = email,
                    message = "New OTP verification code sent",
                    expiresIn = 300,
                    resendCooldown = 30
                )
            )
        }

        override suspend fun getCurrentUser(): Result<UserProfile> = Result.failure(UnsupportedOperationException())
        override fun isLoggedIn() = false
        override fun saveToken(token: AuthToken) = Unit
        override fun getAccessToken(): String? = null
        override fun clearAuth() {
            clearAuthCalls++
        }
        override fun deactivateActiveSession() {
            deactivateSessionCalls++
        }
        override suspend fun switchToAccount(userId: Long): Result<com.calisthenics.ai.domain.model.SavedAccount> {
            return switchResult ?: Result.success(
                com.calisthenics.ai.domain.model.SavedAccount(
                    userId = userId,
                    email = "user$userId@example.com",
                    fullName = "User $userId",
                    avatarUrl = null,
                    role = "ROLE_USER",
                    isActive = true,
                    lastActiveTimestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
