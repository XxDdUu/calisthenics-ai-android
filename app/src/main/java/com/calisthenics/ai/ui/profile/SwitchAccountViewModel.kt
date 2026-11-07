package com.calisthenics.ai.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.websocket.ChatWebSocketClient
import com.calisthenics.ai.domain.model.SavedAccount
import com.calisthenics.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SwitchAccountUiState(
    val accounts: List<SavedAccount> = emptyList(),
    val activeUserId: Long? = null,
    val isLoading: Boolean = false,
    val switchingUserId: Long? = null,
    val accountPendingRemoval: SavedAccount? = null,
    val error: String? = null,
    val switchSuccess: Boolean = false
)

@HiltViewModel
class SwitchAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatWebSocketClient: ChatWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwitchAccountUiState())
    val uiState: StateFlow<SwitchAccountUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        val accounts = authRepository.getSavedAccounts()
        val active = accounts.firstOrNull { it.isActive }
        _uiState.update {
            it.copy(
                accounts = accounts,
                activeUserId = active?.userId,
                isLoading = false
            )
        }
    }

    fun onAccountSelected(account: SavedAccount) {
        if (account.isActive) return
        viewModelScope.launch {
            _uiState.update { it.copy(switchingUserId = account.userId, error = null) }
            // Terminate previous chat websocket and clean up active subscriptions
            chatWebSocketClient.disconnect()

            authRepository.switchToAccount(account.userId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            switchingUserId = null,
                            switchSuccess = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            switchingUserId = null,
                            error = error.localizedMessage ?: "Không thể chuyển sang tài khoản ${account.email}"
                        )
                    }
                }
        }
    }

    fun requestRemoveAccount(account: SavedAccount) {
        _uiState.update { it.copy(accountPendingRemoval = account) }
    }

    fun cancelRemoveAccount() {
        _uiState.update { it.copy(accountPendingRemoval = null) }
    }

    fun confirmRemoveAccount() {
        val account = _uiState.value.accountPendingRemoval ?: return
        val wasActive = account.isActive
        authRepository.removeAccount(account.userId)
        val remaining = authRepository.getSavedAccounts()

        _uiState.update {
            it.copy(
                accountPendingRemoval = null,
                accounts = remaining,
                activeUserId = remaining.firstOrNull { a -> a.isActive }?.userId
            )
        }

        if (wasActive) {
            if (remaining.isNotEmpty()) {
                onAccountSelected(remaining.first())
            } else {
                authRepository.clearAuth()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

