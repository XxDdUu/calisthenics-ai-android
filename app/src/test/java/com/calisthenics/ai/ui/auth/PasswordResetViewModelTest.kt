package com.calisthenics.ai.ui.auth

import com.calisthenics.ai.data.remote.mapper.AuthErrorMapper
import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.UserProfile
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.PasswordResetAuthorization
import com.calisthenics.ai.domain.repository.RegistrationInitiation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invalidEmailIsRejectedWithoutRequestingOtp() {
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateEmail("not-an-email")

        viewModel.requestOtp()

        assertEquals(0, repository.requestOtpCalls)
        assertTrue(viewModel.state.value.error.orEmpty().contains("email", ignoreCase = true))
    }

    @Test
    fun successfulForgotRequestStartsFiveMinuteCountdownAndResendCooldown() = runTest {
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateEmail("athlete@example.com")

        viewModel.requestOtp()
        runCurrent()

        assertEquals(1, repository.requestOtpCalls)
        assertEquals(300, viewModel.state.value.secondsRemaining)
        assertEquals(30, viewModel.state.value.resendSecondsRemaining)
        assertEquals(AuthErrorMapper.OTP_SENT_SUCCESS_MESSAGE, viewModel.state.value.message)
        assertTrue(viewModel.state.value.requestCompleted)
        assertEquals(OtpStatus.INITIAL, viewModel.state.value.status)
    }

    @Test
    fun duplicateForgotRequestsAreIgnoredWhileLoading() = runTest {
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateEmail("athlete@example.com")

        viewModel.requestOtp()
        viewModel.requestOtp()
        runCurrent()

        assertEquals(1, repository.requestOtpCalls)
    }

    @Test
    fun otpMustBeSixDigitsAndSuccessfulVerificationKeepsTokenInViewModelState() = runTest {
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateEmail("athlete@example.com")
        viewModel.verifyOtp("12345")
        assertEquals(0, repository.verifyOtpCalls)
        assertEquals(OtpStatus.INVALID_OTP, viewModel.state.value.status)

        viewModel.verifyOtp("123456")
        runCurrent()

        assertEquals(1, repository.verifyOtpCalls)
        assertEquals("temporary-reset-token", viewModel.state.value.resetToken)
        assertTrue(viewModel.state.value.verificationCompleted)
        assertEquals(OtpStatus.SUCCESS, viewModel.state.value.status)
        assertEquals(AuthErrorMapper.OTP_VERIFY_SUCCESS_MESSAGE, viewModel.state.value.message)
    }

    @Test
    fun verificationFailureDisplaysSafeInvalidOtpMessage() = runTest {
        repository.verifyOtpResult = Result.failure(IllegalStateException("raw server details"))
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateEmail("athlete@example.com")
        viewModel.verifyOtp("123456")
        runCurrent()

        assertEquals(AuthErrorMapper.OTP_INVALID_MESSAGE, viewModel.state.value.error)
        assertEquals(OtpStatus.INVALID_OTP, viewModel.state.value.status)
        assertNull(viewModel.state.value.resetToken)
        assertFalse(viewModel.state.value.verificationCompleted)
    }

    @Test
    fun verificationNetworkFailureSetsNetworkErrorStatus() = runTest {
        repository.verifyOtpResult = Result.failure(IOException("Failed to connect"))
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateEmail("athlete@example.com")
        viewModel.verifyOtp("123456")
        runCurrent()

        assertEquals(AuthErrorMapper.OTP_NETWORK_ERROR_MESSAGE, viewModel.state.value.error)
        assertEquals(OtpStatus.NETWORK_ERROR, viewModel.state.value.status)
    }

    @Test
    fun passwordMismatchAndWeakPasswordAreRejectedBeforeApiCall() {
        val viewModel = PasswordResetViewModel(repository)
        viewModel.resetPassword("StrongPassword123!", "DifferentPassword123!")
        assertEquals("Mật khẩu xác nhận không khớp.", viewModel.state.value.error)

        viewModel.resetPassword("weakpass", "weakpass")
        assertTrue(viewModel.state.value.error.orEmpty().contains("8 ký tự"))
        assertEquals(0, repository.resetPasswordCalls)
    }

    @Test
    fun successfulResetClearsTemporaryTokenAndSignalsCompletion() = runTest {
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateEmail("athlete@example.com")
        viewModel.verifyOtp("123456")
        runCurrent()

        viewModel.resetPassword("NewPassword123!", "NewPassword123!")
        runCurrent()

        assertEquals(1, repository.resetPasswordCalls)
        assertNull(viewModel.state.value.resetToken)
        assertTrue(viewModel.state.value.resetCompleted)
    }

    @Test
    fun countdownFinishesBeforeOtpCanBeResent() = runTest {
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateEmail("athlete@example.com")
        viewModel.requestOtp()
        runCurrent()
        viewModel.resendOtp()
        runCurrent()
        assertEquals(1, repository.requestOtpCalls)

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(0, viewModel.state.value.resendSecondsRemaining)
        viewModel.resendOtp()
        runCurrent()
        assertEquals(2, repository.requestOtpCalls)
    }

    @Test
    fun maskEmailMasksCorrectly() {
        assertEquals("a***e@example.com", maskEmail("athlete@example.com"))
        assertEquals("d***y@domain.vn", maskEmail("duy@domain.vn"))
        assertEquals("m***@domain.com", maskEmail("me@domain.com"))
        assertEquals("a***@b.com", maskEmail("a@b.com"))
        assertEquals("", maskEmail(""))
        assertEquals("no-at-sign", maskEmail("no-at-sign"))
    }

    private class FakeAuthRepository : AuthRepository {
        var requestOtpCalls = 0
        var verifyOtpCalls = 0
        var resetPasswordCalls = 0
        var verifyOtpResult: Result<PasswordResetAuthorization> =
            Result.success(PasswordResetAuthorization("temporary-reset-token", 300))

        override suspend fun login(email: String, password: String): Result<AuthToken> =
            Result.failure(UnsupportedOperationException())

        override suspend fun register(email: String, password: String, fullName: String): Result<RegistrationInitiation> =
            Result.failure(UnsupportedOperationException())

        override suspend fun requestPasswordResetOtp(email: String): Result<String> {
            requestOtpCalls++
            return Result.success("If the account exists, we sent a code.")
        }

        override suspend fun verifyPasswordResetOtp(
            email: String,
            otp: String
        ): Result<PasswordResetAuthorization> {
            verifyOtpCalls++
            return verifyOtpResult
        }

        override suspend fun resetPassword(
            resetToken: String,
            newPassword: String,
            confirmPassword: String
        ): Result<Unit> {
            resetPasswordCalls++
            return Result.success(Unit)
        }

        override suspend fun getCurrentUser(): Result<UserProfile> =
            Result.failure(UnsupportedOperationException())

        override fun isLoggedIn() = false
        override fun saveToken(token: AuthToken) = Unit
        override fun getAccessToken(): String? = null
        override fun clearAuth() = Unit
    }
}
