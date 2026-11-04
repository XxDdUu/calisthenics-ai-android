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

data class PasswordResetUiState(
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val secondsRemaining: Int = 0,
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
        _state.update { it.copy(email = email, error = null) }
    }

    fun requestOtp() {
        requestOtp(navigateAfterSuccess = true)
    }

    private fun requestOtp(navigateAfterSuccess: Boolean) {
        if (_state.value.loading) return
        val email = _state.value.email.trim()
        if (!email.matches(EMAIL_PATTERN)) {
            _state.update { it.copy(error = "Nhập địa chỉ email hợp lệ.") }
            return
        }

        _state.update { it.copy(email = email, loading = true, error = null, message = null) }
        viewModelScope.launch {
            repository.requestPasswordResetOtp(email)
                .onSuccess { message ->
                    _state.update {
                        it.copy(
                            loading = false,
                            message = message,
                            secondsRemaining = OTP_COUNTDOWN_SECONDS,
                            resetToken = null,
                            verificationCompleted = false,
                            requestCompleted = navigateAfterSuccess
                        )
                    }
                    startCountdown()
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = AuthErrorMapper.passwordResetMessage(error)) }
                }
        }
    }

    fun resendOtp() {
        if (_state.value.secondsRemaining > 0 || _state.value.loading) return
        requestOtp(navigateAfterSuccess = false)
    }

    fun verifyOtp(otp: String) {
        if (_state.value.loading) return
        if (!OTP_PATTERN.matches(otp)) {
            _state.update { it.copy(error = "Nhập mã OTP gồm đúng 6 chữ số.") }
            return
        }

        val email = _state.value.email
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.verifyPasswordResetOtp(email, otp)
                .onSuccess { authorization ->
                    if (authorization.token.isBlank()) {
                        _state.update {
                            it.copy(loading = false, error = AuthErrorMapper.RESET_GENERIC_ERROR_MESSAGE)
                        }
                    } else {
                        _state.update {
                            it.copy(
                                loading = false,
                                resetToken = authorization.token,
                                verificationCompleted = true
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = AuthErrorMapper.passwordResetMessage(error)) }
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
            while (_state.value.secondsRemaining > 0) {
                delay(1_000)
                _state.update { current ->
                    current.copy(secondsRemaining = (current.secondsRemaining - 1).coerceAtLeast(0))
                }
            }
        }
    }

    private fun isStrongPassword(password: String): Boolean =
        password.length in 8..72
                && password.any(Char::isUpperCase)
                && password.any(Char::isLowerCase)
                && password.any(Char::isDigit)
                && password.any { !it.isLetterOrDigit() }

    private companion object {
        const val OTP_COUNTDOWN_SECONDS = 300
        val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        val OTP_PATTERN = Regex("[0-9]{6}")
    }
}
