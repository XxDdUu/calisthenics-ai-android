package com.calisthenics.ai.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.SessionManager
import com.calisthenics.ai.data.remote.mapper.AuthErrorMapper
import com.calisthenics.ai.data.remote.websocket.ChatWebSocketClient
import com.calisthenics.ai.domain.model.SavedAccount
import com.calisthenics.ai.domain.repository.AuthRepository
import retrofit2.HttpException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val userId: Long, val role: String?, val email: String?) : AuthState
    data object SwitchingAccount : AuthState
    data class Error(val message: String) : AuthState
}

data class AuthUiState(
    val authState: AuthState = AuthState.Unauthenticated,
    val loading: Boolean = false,
    val authenticated: Boolean = false,
    val role: String? = null,
    val error: String? = null,
    val savedAccounts: List<SavedAccount> = emptyList(),
    val switchingUserId: Long? = null,
    val registrationPendingEmail: String? = null,
    val registrationOtpSent: Boolean = false,
    val otpVerifying: Boolean = false,
    val otpResending: Boolean = false,
    val otpError: String? = null,
    val otpMessage: String? = null,
    val otpResendCooldownSeconds: Int = 0
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    sessionManager: SessionManager? = null,
    private val chatWebSocketClient: ChatWebSocketClient? = null,
    private val googleAuthManager: GoogleAuthManager? = null,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state = _state.asStateFlow()
    private var otpCountdownJob: Job? = null

    private fun createInitialState(): AuthUiState {
        val isLoggedIn = repository.isLoggedIn()
        val role = repository.getUserRole()
        val userId = repository.getUserId()
        val savedAccounts = repository.getSavedAccounts()
        return if (isLoggedIn && userId != null) {
            AuthUiState(
                authState = AuthState.Authenticated(userId, role, repository.getUserEmail()),
                authenticated = true,
                role = role,
                savedAccounts = savedAccounts
            )
        } else {
            AuthUiState(
                authState = AuthState.Unauthenticated,
                authenticated = false,
                role = null,
                savedAccounts = savedAccounts
            )
        }
    }

    init {
        sessionManager?.let { sm ->
            viewModelScope.launch {
                sm.sessionExpiredEvent.collect {
                    if (repository.isLoggedIn()) return@collect
                    _state.update {
                        it.copy(
                            authState = AuthState.Unauthenticated,
                            authenticated = false,
                            role = null,
                            error = "Session expired. Please sign in again."
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            var lastSession = accountSessionManager.currentSession()
            accountSessionManager.sessionState.collect { session ->
                if (session.generation == lastSession.generation && session.activeUserId == lastSession.activeUserId) {
                    return@collect
                }
                lastSession = session
                val userId = session.activeUserId
                if (session.isAuthenticated && userId != null) {
                    val role = session.role ?: repository.getUserRole()
                    _state.value = AuthUiState(
                        authState = AuthState.Authenticated(userId, role, repository.getUserEmail()),
                        authenticated = true,
                        role = role,
                        savedAccounts = repository.getSavedAccounts()
                    )
                } else if (_state.value.authState !is AuthState.SwitchingAccount) {
                    _state.value = AuthUiState(
                        authState = AuthState.Unauthenticated,
                        authenticated = false,
                        role = null,
                        savedAccounts = repository.getSavedAccounts()
                    )
                }
            }
        }
    }

    fun login(email: String, password: String) {
        _state.update { it.copy(error = null) }
        if (email.isBlank() || password.isBlank()) {
            val msg = "Enter your email and password"
            _state.update { it.copy(error = msg, authState = AuthState.Error(msg)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, authState = AuthState.Loading) }
            repository.login(email.trim(), password)
                .onSuccess { token ->
                    _state.value = AuthUiState(
                        authState = AuthState.Authenticated(token.userId, token.role, token.email),
                        authenticated = true,
                        role = token.role,
                        savedAccounts = repository.getSavedAccounts()
                    )
                }
                .onFailure { error ->
                    val msg = AuthErrorMapper.message(error)
                    _state.value = AuthUiState(
                        authState = AuthState.Error(msg),
                        authenticated = false,
                        error = msg,
                        savedAccounts = repository.getSavedAccounts()
                    )
                }
        }
    }

    companion object {
        private const val TAG = "GoogleSignIn"
    }

    fun startGoogleSignIn(requestId: String = "") {
        Log.d(TAG, "[$requestId] Google sign-in UI loading started")
        _state.update { it.copy(loading = true, error = null) }
    }

    fun cancelGoogleSignIn(requestId: String = "") {
        Log.d(TAG, "[$requestId] Google sign-in cancelled or reset")
        _state.update { it.copy(loading = false) }
    }

    fun loginWithGoogle(idToken: String, requestId: String = "") {
        loginWithFirebase(idToken, requestId)
    }

    fun loginWithFirebase(idToken: String, requestId: String = "") {
        viewModelScope.launch {
            Log.d(TAG, "[$requestId] BACKEND_FIREBASE_REQUEST_STARTED")
            _state.update { it.copy(loading = true, error = null, authState = AuthState.Loading) }
            repository.loginWithFirebase(idToken)
                .onSuccess { token ->
                    Log.d(TAG, "[$requestId] BACKEND_RESPONSE_SUCCESS (userId=${token.userId}, role=${token.role})")
                    Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Success)")
                    _state.value = AuthUiState(
                        authState = AuthState.Authenticated(token.userId, token.role, token.email),
                        authenticated = true,
                        role = token.role,
                        savedAccounts = repository.getSavedAccounts()
                    )
                }
                .onFailure { error ->
                    val httpStatus = (error as? HttpException)?.code()
                    Log.e(TAG, "[$requestId] BACKEND_RESPONSE_ERROR (httpStatus=$httpStatus, message=${error.message})", error)
                    Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                    val msg = AuthErrorMapper.message(error)
                    _state.value = AuthUiState(
                        authState = AuthState.Error(msg),
                        authenticated = false,
                        error = msg,
                        savedAccounts = repository.getSavedAccounts()
                    )
                }
        }
    }

    fun switchToAccount(userId: Long) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    switchingUserId = userId,
                    error = null,
                    authState = AuthState.SwitchingAccount
                )
            }
            chatWebSocketClient?.disconnect()
            repository.switchToAccount(userId)
                .onSuccess { account ->
                    _state.value = AuthUiState(
                        authState = AuthState.Authenticated(account.userId, account.role, account.email),
                        authenticated = true,
                        role = account.role,
                        savedAccounts = repository.getSavedAccounts()
                    )
                }
                .onFailure { error ->
                    val msg = error.localizedMessage ?: "Không thể chuyển tài khoản"
                    _state.update {
                        it.copy(
                            loading = false,
                            switchingUserId = null,
                            error = msg,
                            authState = AuthState.Error(msg),
                            savedAccounts = repository.getSavedAccounts()
                        )
                    }
                }
        }
    }

    fun removeSavedAccount(userId: Long) {
        repository.removeAccount(userId)
        val remaining = repository.getSavedAccounts()
        _state.update {
            it.copy(savedAccounts = remaining)
        }
    }

    fun refreshSavedAccounts() {
        val saved = repository.getSavedAccounts()
        _state.update { it.copy(savedAccounts = saved) }
    }

    fun initiateSwitchAccount() {
        chatWebSocketClient?.disconnect()
        repository.deactivateActiveSession()
        viewModelScope.launch { googleAuthManager?.clearCredentialState() }
        val saved = repository.getSavedAccounts()
        _state.value = AuthUiState(
            authState = AuthState.SwitchingAccount,
            authenticated = false,
            role = null,
            loading = false,
            error = null,
            savedAccounts = saved
        )
    }

    fun setErrorMessage(message: String) {
        _state.update { it.copy(error = message, loading = false, authState = AuthState.Error(message)) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun prepareForAddAccount() {
        repository.deactivateActiveSession()
        viewModelScope.launch { googleAuthManager?.clearCredentialState() }
        _state.update {
            it.copy(
                authState = AuthState.SwitchingAccount,
                authenticated = false,
                role = null,
                loading = false,
                error = null,
                savedAccounts = repository.getSavedAccounts()
            )
        }
    }

    fun register(name: String, email: String, password: String, onNavigateToOtp: ((String) -> Unit)? = null) {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
            val msg = "Complete all fields to create your account"
            _state.update { it.copy(error = msg, authState = AuthState.Error(msg)) }
            return
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!trimmedEmail.matches(emailRegex)) {
            val msg = "Please enter a valid email address"
            _state.update { it.copy(error = msg, authState = AuthState.Error(msg)) }
            return
        }
        if (password.length < 8) {
            val msg = "Use a password with at least 8 characters"
            _state.update { it.copy(error = msg, authState = AuthState.Error(msg)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, authState = AuthState.Loading) }
            repository.register(trimmedEmail, password, trimmedName)
                .onSuccess { initiation ->
                    val cooldown = if (initiation.resendCooldown > 0) initiation.resendCooldown else 30
                    _state.update {
                        it.copy(
                            loading = false,
                            error = null,
                            authState = AuthState.Unauthenticated,
                            registrationPendingEmail = initiation.email,
                            registrationOtpSent = true,
                            otpError = null,
                            otpMessage = initiation.message,
                            otpResendCooldownSeconds = cooldown
                        )
                    }
                    startOtpCooldown(cooldown)
                    onNavigateToOtp?.invoke(initiation.email)
                }
                .onFailure { error ->
                    val msg = AuthErrorMapper.registrationMessage(error)
                    _state.update {
                        it.copy(
                            loading = false,
                            error = msg,
                            authState = AuthState.Error(msg)
                        )
                    }
                }
        }
    }

    fun startOtpCooldown(seconds: Int) {
        otpCountdownJob?.cancel()
        otpCountdownJob = viewModelScope.launch {
            var current = seconds
            while (current > 0) {
                _state.update { it.copy(otpResendCooldownSeconds = current) }
                delay(1000)
                current--
            }
            _state.update { it.copy(otpResendCooldownSeconds = 0) }
        }
    }

    fun verifyRegisterOtp(otp: String, onSuccess: (() -> Unit)? = null) {
        val email = _state.value.registrationPendingEmail
        if (email.isNullOrBlank()) {
            _state.update { it.copy(otpError = "Email không hợp lệ. Vui lòng đăng ký lại.") }
            return
        }
        if (otp.length != 6) {
            _state.update { it.copy(otpError = "Mã OTP phải gồm 6 chữ số.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(otpVerifying = true, otpError = null, otpMessage = null) }
            repository.verifyRegistrationOtp(email, otp)
                .onSuccess { token ->
                    otpCountdownJob?.cancel()
                    _state.value = AuthUiState(
                        authState = AuthState.Authenticated(token.userId, token.role, token.email),
                        authenticated = true,
                        role = token.role,
                        savedAccounts = repository.getSavedAccounts(),
                        registrationPendingEmail = null,
                        registrationOtpSent = false
                    )
                    onSuccess?.invoke()
                }
                .onFailure { error ->
                    val msg = AuthErrorMapper.otpErrorMessage(error)
                    _state.update {
                        it.copy(
                            otpVerifying = false,
                            otpError = msg
                        )
                    }
                }
        }
    }

    fun resendRegisterOtp() {
        val email = _state.value.registrationPendingEmail ?: return
        if (_state.value.otpResending || _state.value.otpResendCooldownSeconds > 0) return

        viewModelScope.launch {
            _state.update { it.copy(otpResending = true, otpError = null, otpMessage = null) }
            repository.resendRegistrationOtp(email)
                .onSuccess { res ->
                    val cd = if (res.resendCooldown > 0) res.resendCooldown else 30
                    _state.update {
                        it.copy(
                            otpResending = false,
                            otpMessage = "Mã OTP mới đã được gửi đến email của bạn.",
                            otpError = null,
                            otpResendCooldownSeconds = cd
                        )
                    }
                    startOtpCooldown(cd)
                }
                .onFailure { error ->
                    val msg = AuthErrorMapper.otpErrorMessage(error)
                    _state.update {
                        it.copy(
                            otpResending = false,
                            otpError = msg
                        )
                    }
                }
        }
    }

    fun clearRegistrationOtpState() {
        otpCountdownJob?.cancel()
        _state.update {
            it.copy(
                registrationPendingEmail = null,
                registrationOtpSent = false,
                otpVerifying = false,
                otpResending = false,
                otpError = null,
                otpMessage = null,
                otpResendCooldownSeconds = 0
            )
        }
    }

    fun logout() {
        chatWebSocketClient?.disconnect()
        repository.clearAuth()
        // Clear the provider's app-level credential session too. Otherwise the next
        // explicit Google sign-in can be forced down a stale re-authentication path.
        viewModelScope.launch { googleAuthManager?.clearCredentialState() }
        _state.value = AuthUiState(
            authState = AuthState.Unauthenticated,
            authenticated = false,
            role = null,
            savedAccounts = repository.getSavedAccounts()
        )
    }

    suspend fun refreshRole(): String? {
        return repository.refreshToken().fold(
            onSuccess = { token ->
                _state.update {
                    it.copy(
                        authState = AuthState.Authenticated(token.userId, token.role, token.email),
                        authenticated = true,
                        role = token.role
                    )
                }
                token.role
            },
            onFailure = { error ->
                if (error is HttpException && error.code() == 401) {
                    logout()
                } else {
                    _state.update { it.copy(role = null) }
                }
                null
            }
        )
    }
}
