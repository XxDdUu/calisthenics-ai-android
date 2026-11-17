package com.calisthenics.ai.data.local

import com.calisthenics.ai.domain.model.Gender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class GenderSessionState(
    val userId: Long,
    val gender: Gender?,
    val isBackendConfirmed: Boolean
)

@Singleton
open class GenderSessionStore @Inject constructor() {
    private val _state = MutableStateFlow<GenderSessionState?>(null)
    open val state: StateFlow<GenderSessionState?> = _state.asStateFlow()

    open fun publish(userId: Long, gender: Gender?, isBackendConfirmed: Boolean = true) {
        if (userId <= 0L) return
        _state.value = GenderSessionState(
            userId = userId,
            gender = gender,
            isBackendConfirmed = isBackendConfirmed
        )
    }

    open fun clear() {
        _state.value = null
    }
}
