package com.calisthenics.ai.data.local

import com.calisthenics.ai.domain.model.OnboardingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class OnboardingSessionState(
    val userId: Long,
    val status: OnboardingStatus,
    val isBackendConfirmed: Boolean
)

@Singleton
open class OnboardingSessionStore @Inject constructor() {
    private val _state = MutableStateFlow<OnboardingSessionState?>(null)
    open val state: StateFlow<OnboardingSessionState?> = _state.asStateFlow()

    open fun publish(userId: Long, status: OnboardingStatus, isBackendConfirmed: Boolean = true) {
        if (userId <= 0L) return
        _state.value = OnboardingSessionState(
            userId = userId,
            status = status,
            isBackendConfirmed = isBackendConfirmed
        )
    }

    open fun clear() {
        _state.value = null
    }
}
