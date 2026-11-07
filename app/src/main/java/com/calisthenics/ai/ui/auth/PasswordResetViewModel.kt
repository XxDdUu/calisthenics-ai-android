package com.calisthenics.ai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.mapper.AuthErrorMapper
import com.calisthenics.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OtpStatus {
    INITIAL,
    LOADING,
    VERIFYING,
    RESENDING,
    INVALID_OTP,
    EXPIRED_OTP,
    TOO_MANY_ATTEMPTS,
    NETWORK_ERROR,
    SUCCESS
}

data class PasswordResetUiState(
    val email: String = "",
    val loading: Boolean = false,
    val verifying: Boolean = false,
    val resending: Boolean = false,
    val status: OtpStatus = OtpStatus.INITIAL,
    val error: String? = null,
    val message: String? = null,
    val secondsRemaining: Int = 0,
    val resendSecondsRemaining: Int = 0,
    val resetToken: String? = null,
    val requestCompleted: Boolean = false,
    val verificationCompleted: Boolean = false,
    val resetCompleted: Boolean = false
)

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PasswordResetUiState())
    val state = _state.asStateFlow()
    private var countdownJob: Job? = null

    fun updateEmail(email: String) {
        _state.update {
            it.copy(
                email = email.trim(),
                error = null,
                message = null,
                status = OtpStatus.INITIAL
            )
        }
    }

    fun requestOtp() {
        requestOtp(navigateAfterSuccess = true)
    }

    private fun requestOtp(navigateAfterSuccess: Boolean) {
        if (_state.value.loading || _state.value.resending) return
        val email = _state.value.email.trim()
        if (!email.matches(EMAIL_PATTERN)) {
            _state.update { it.copy(error = "Nhập địa chỉ email hợp lệ.") }
            return
        }

        _state.update {
            it.copy(
                email = email,
                loading = true,
                status = OtpStatus.LOADING,
                error = null,
                message = null
            )
        }
        viewModelScope.launch {
            repository.requestPasswordResetOtp(email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            loading = false,
                            resending = false,
                            message = AuthErrorMapper.OTP_SENT_SUCCESS_MESSAGE,
                            secondsRemaining = OTP_COUNTDOWN_SECONDS,
                            resendSecondsRemaining = RESEND_COOLDOWN_SECONDS,
                            status = OtpStatus.INITIAL,
                            resetToken = null,
                            verificationCompleted = false,
                            requestCompleted = navigateAfterSuccess
                        )
                    }
                    startCountdown()
                }
                .onFailure { error ->
                    val errorMsg = AuthErrorMapper.otpErrorMessage(error)
                    val status = mapErrorToStatus(errorMsg)
                    _state.update {
                        it.copy(
                            loading = false,
                            resending = false,
                            status = status,
                            error = errorMsg
                        )
                    }
                }
        }
    }

    fun resendOtp() {
        if (_state.value.resendSecondsRemaining > 0 || _state.value.resending || _state.value.loading || _state.value.verifying) return
        val email = _state.value.email.trim()
        if (email.isBlank()) return

        _state.update {
            it.copy(
                resending = true,
                status = OtpStatus.RESENDING,
                error = null,
                message = null
            )
        }
        viewModelScope.launch {
            repository.requestPasswordResetOtp(email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            resending = false,
                            message = AuthErrorMapper.OTP_SENT_SUCCESS_MESSAGE,
                            error = null,
                            status = OtpStatus.INITIAL,
                            resendSecondsRemaining = RESEND_COOLDOWN_SECONDS,
                            secondsRemaining = OTP_COUNTDOWN_SECONDS
                        )
                    }
                    startCountdown()
                }
                .onFailure { error ->
                    val errorMsg = AuthErrorMapper.otpErrorMessage(error)
                    val status = mapErrorToStatus(errorMsg)
                    _state.update {
                        it.copy(
                            resending = false,
                            status = status,
                            error = errorMsg
                        )
                    }
                }
        }
    }

    fun verifyOtp(otp: String) {
        if (_state.value.verifying || _state.value.loading) return
        val cleanOtp = otp.filter { it.isDigit() }
        if (!OTP_PATTERN.matches(cleanOtp)) {
            _state.update {
                it.copy(
                    error = AuthErrorMapper.OTP_INVALID_MESSAGE,
                    status = OtpStatus.INVALID_OTP
                )
            }
            return
        }

        val email = _state.value.email.trim()
        _state.update {
            it.copy(
                verifying = true,
                loading = true,
                status = OtpStatus.VERIFYING,
                error = null,
                message = null
            )
        }
        viewModelScope.launch {
            repository.verifyPasswordResetOtp(email, cleanOtp)
                .onSuccess { authorization ->
                    if (authorization.token.isBlank()) {
                        _state.update {
                            it.copy(
                                verifying = false,
                                loading = false,
                                status = OtpStatus.INVALID_OTP,
                                error = AuthErrorMapper.OTP_INVALID_MESSAGE
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                verifying = false,
                                loading = false,
                                status = OtpStatus.SUCCESS,
                                message = AuthErrorMapper.OTP_VERIFY_SUCCESS_MESSAGE,
                                resetToken = authorization.token,
                                verificationCompleted = true
                            )
                        }
                    }
                }
                .onFailure { error ->
                    val errorMsg = AuthErrorMapper.otpErrorMessage(error)
                    val status = mapErrorToStatus(errorMsg)
                    _state.update {
                        it.copy(
                            verifying = false,
                            loading = false,
                            status = status,
                            error = errorMsg
                        )
                    }
                }
        }
    }

    fun resetPassword(newPassword: String, confirmPassword: String) {
        if (_state.value.loading) return
        if (newPassword != confirmPassword) {
            _state.update { it.copy(error = "Mật khẩu xác nhận không khớp.") }
            return
        }
        if (!isStrongPassword(newPassword)) {
            _state.update {
                it.copy(error = "Mật khẩu cần ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.")
            }
            return
        }
        val resetToken = _state.value.resetToken
        if (resetToken.isNullOrBlank()) {
            _state.update { it.copy(error = "Phiên đặt lại mật khẩu đã hết hạn. Hãy xác minh lại OTP.") }
            return
        }

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.resetPassword(resetToken, newPassword, confirmPassword)
                .onSuccess {
                    countdownJob?.cancel()
                    _state.value = PasswordResetUiState(
                        message = "Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại.",
                        resetCompleted = true
                    )
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = AuthErrorMapper.passwordResetMessage(error)) }
                }
        }
    }

    fun consumeRequestCompleted() {
        _state.update { it.copy(requestCompleted = false) }
    }

    fun consumeVerificationCompleted() {
        _state.update { it.copy(verificationCompleted = false) }
    }

    fun consumeResetCompleted() {
        _state.value = PasswordResetUiState()
    }

    fun clearResetAuthorization() {
        countdownJob?.cancel()
        _state.update {
            it.copy(resetToken = null, verificationCompleted = false, error = null, loading = false)
        }
    }

    fun cancelFlow() {
        countdownJob?.cancel()
        _state.value = PasswordResetUiState()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_state.value.secondsRemaining > 0 || _state.value.resendSecondsRemaining > 0) {
                delay(1_000)
                _state.update { current ->
                    current.copy(
                        secondsRemaining = (current.secondsRemaining - 1).coerceAtLeast(0),
                        resendSecondsRemaining = (current.resendSecondsRemaining - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }

    private fun mapErrorToStatus(message: String): OtpStatus = when {
        message == AuthErrorMapper.OTP_EXPIRED_MESSAGE -> OtpStatus.EXPIRED_OTP
        message == AuthErrorMapper.OTP_TOO_MANY_ATTEMPTS_MESSAGE -> OtpStatus.TOO_MANY_ATTEMPTS
        message == AuthErrorMapper.OTP_NETWORK_ERROR_MESSAGE -> OtpStatus.NETWORK_ERROR
        message == AuthErrorMapper.OTP_INVALID_MESSAGE -> OtpStatus.INVALID_OTP
        message == AuthErrorMapper.OTP_ALREADY_USED_MESSAGE -> OtpStatus.INVALID_OTP
        else -> OtpStatus.INITIAL
    }

    private fun isStrongPassword(password: String): Boolean =
        password.length in 8..72
                && password.any(Char::isUpperCase)
                && password.any(Char::isLowerCase)
                && password.any(Char::isDigit)
                && password.any { !it.isLetterOrDigit() }

    companion object {
        const val OTP_COUNTDOWN_SECONDS = 300
        const val RESEND_COOLDOWN_SECONDS = 30
        val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        val OTP_PATTERN = Regex("[0-9]{6}")
    }
}
