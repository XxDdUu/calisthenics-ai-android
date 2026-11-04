package com.calisthenics.ai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.SessionManager
import com.calisthenics.ai.data.remote.mapper.AuthErrorMapper
import com.calisthenics.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val authenticated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    sessionManager: SessionManager? = null
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState(authenticated = repository.isLoggedIn()))
    val state = _state.asStateFlow()

    init {
        sessionManager?.let { sm ->
            viewModelScope.launch {
                sm.sessionExpiredEvent.collect {
                    _state.update {
                        it.copy(
                            authenticated = false,
                            error = "Session expired. Please sign in again."
                        )
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        _state.update { it.copy(error = null) }
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Enter your email and password") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.login(email.trim(), password)
                .onSuccess { _state.value = AuthUiState(authenticated = true) }
                .onFailure { error -> _state.value = AuthUiState(error = AuthErrorMapper.message(error)) }
        }
    }

    fun startGoogleSignIn() {
        _state.update { it.copy(loading = true, error = null) }
    }

    fun cancelGoogleSignIn() {
        _state.update { it.copy(loading = false) }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.loginWithGoogle(idToken)
                .onSuccess { _state.value = AuthUiState(authenticated = true) }
                .onFailure { error -> _state.value = AuthUiState(error = AuthErrorMapper.message(error)) }
        }
    }

    fun setErrorMessage(message: String) {
        _state.update { it.copy(error = message, loading = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Complete all fields to create your account") }
            return
        }
        if (password.length < 8) {
            _state.update { it.copy(error = "Use a password with at least 8 characters") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.register(email.trim(), password, name.trim())
                .onSuccess { _state.value = AuthUiState(authenticated = true) }
                .onFailure { error -> _state.value = AuthUiState(error = error.localizedMessage ?: "Unable to create account") }
        }
    }

    fun logout() {
        repository.clearAuth()
        _state.value = AuthUiState()
    }
}
