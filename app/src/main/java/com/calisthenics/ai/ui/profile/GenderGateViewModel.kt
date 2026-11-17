package com.calisthenics.ai.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.GenderSessionStore
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenderGateUiState(
    val activeUserId: Long? = null,
    val checkedOnlineForUserId: Long? = null,
    val resolvedGender: Gender? = null,
    val shouldShowModal: Boolean = false,
    val selectedGender: Gender? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class GenderGateViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val genderSessionStore: GenderSessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenderGateUiState())
    val uiState: StateFlow<GenderGateUiState> = _uiState.asStateFlow()

    private var checkJob: Job? = null
    private var checkingUserId: Long? = null

    init {
        viewModelScope.launch {
            genderSessionStore.state.collect { sessionState ->
                if (sessionState == null) {
                    checkJob?.cancel()
                    checkingUserId = null
                    _uiState.value = GenderGateUiState()
                    return@collect
                }

                val currentActive = _uiState.value.activeUserId ?: authRepository.getUserId()
                if (currentActive == null || sessionState.userId == currentActive) {
                    _uiState.update { current ->
                        val isSameUser = current.activeUserId == sessionState.userId
                        current.copy(
                            activeUserId = sessionState.userId,
                            checkedOnlineForUserId = if (sessionState.isBackendConfirmed) {
                                sessionState.userId
                            } else if (isSameUser) {
                                current.checkedOnlineForUserId
                            } else {
                                null
                            },
                            resolvedGender = sessionState.gender,
                            shouldShowModal = sessionState.isBackendConfirmed && sessionState.gender == null,
                            selectedGender = if (sessionState.gender != null) null else if (isSameUser) current.selectedGender else null,
                            isSaving = if (sessionState.gender != null) false else if (isSameUser) current.isSaving else false,
                            errorMessage = if (sessionState.gender != null) null else if (isSameUser) current.errorMessage else null
                        )
                    }
                }
            }
        }
    }

    fun syncSession(isAuthenticated: Boolean, isOnline: Boolean) {
        if (!isAuthenticated) {
            checkJob?.cancel()
            checkingUserId = null
            _uiState.value = GenderGateUiState()
            return
        }

        val userId = authRepository.getUserId()
        if (userId == null || userId <= 0L) return

        if (_uiState.value.activeUserId != userId) {
            checkJob?.cancel()
            checkingUserId = null
            val storeState = genderSessionStore.state.value?.takeIf { it.userId == userId }
            val cachedGender = authRepository.getCachedGender(userId)

            if (storeState != null) {
                _uiState.value = GenderGateUiState(
                    activeUserId = userId,
                    checkedOnlineForUserId = if (storeState.isBackendConfirmed) userId else null,
                    resolvedGender = storeState.gender,
                    shouldShowModal = storeState.isBackendConfirmed && storeState.gender == null
                )
            } else {
                _uiState.value = GenderGateUiState(
                    activeUserId = userId,
                    checkedOnlineForUserId = null,
                    resolvedGender = cachedGender,
                    shouldShowModal = false
                )
            }
        }

        if (_uiState.value.checkedOnlineForUserId != userId && isOnline) {
            verifyGenderWithBackend(userId)
        }
    }

    fun retryVerificationIfNeeded(isAuthenticated: Boolean, isOnline: Boolean) {
        if (!isAuthenticated || !isOnline) return
        val userId = authRepository.getUserId() ?: return
        if (_uiState.value.activeUserId != userId || _uiState.value.checkedOnlineForUserId != userId) {
            syncSession(isAuthenticated = true, isOnline = true)
        }
    }

    private fun verifyGenderWithBackend(userId: Long) {
        if (checkingUserId == userId && checkJob?.isActive == true) return
        checkJob?.cancel()
        checkingUserId = userId

        checkJob = viewModelScope.launch {
            authRepository.getCurrentUser()
                .onSuccess { profile ->
                    val resolvedId = profile.id.takeIf { it > 0L } ?: userId
                    _uiState.update { current ->
                        if (current.activeUserId != null && current.activeUserId != resolvedId) {
                            current
                        } else {
                            current.copy(
                                activeUserId = resolvedId,
                                checkedOnlineForUserId = resolvedId,
                                resolvedGender = profile.gender,
                                shouldShowModal = profile.gender == null,
                                errorMessage = if (profile.gender != null) null else current.errorMessage
                            )
                        }
                    }
                }
                .onFailure {
                    // Do not mark gender as configured on offline or API failures.
                }
            if (checkingUserId == userId) {
                checkingUserId = null
            }
        }
    }

    fun selectGender(gender: Gender) {
        _uiState.update {
            it.copy(selectedGender = gender, errorMessage = null)
        }
    }

    fun confirmGender() {
        val current = _uiState.value
        val gender = current.selectedGender ?: return
        if (current.isSaving) return

        val targetUserId = current.activeUserId ?: authRepository.getUserId()

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            authRepository.updateGender(gender)
                .onSuccess { profile ->
                    val confirmedGender = profile.gender ?: gender
                    val resolvedId = profile.id.takeIf { it > 0L } ?: targetUserId
                    _uiState.update {
                        it.copy(
                            activeUserId = resolvedId,
                            checkedOnlineForUserId = resolvedId,
                            resolvedGender = confirmedGender,
                            shouldShowModal = false,
                            selectedGender = null,
                            isSaving = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            shouldShowModal = true,
                            errorMessage = NetworkErrorParser.toUserMessage(throwable)
                        )
                    }
                }
        }
    }
}
