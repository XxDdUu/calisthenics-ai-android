package com.calisthenics.ai.ui.workoutsession

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.dto.PersonalRecordDto
import com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutHistoryUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val sessions: List<WorkoutSession> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val error: String? = null
)

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val repository: WorkoutSessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null, sessions = emptyList(), page = 0) }
        viewModelScope.launch {
            repository.getHistory(page = 0, size = PAGE_SIZE)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessions = result.content,
                            page = result.page,
                            totalPages = result.totalPages,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.localizedMessage ?: "Could not load workout history.")
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.page + 1 >= state.totalPages) return
        _uiState.update { it.copy(isLoadingMore = true, error = null) }
        viewModelScope.launch {
            repository.getHistory(page = state.page + 1, size = PAGE_SIZE)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            sessions = it.sessions + result.content,
                            page = result.page,
                            totalPages = result.totalPages
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = error.localizedMessage ?: "Could not load more workout history."
                        )
                    }
                }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

data class WorkoutSummaryUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSession? = null,
    val prs: List<PersonalRecordDto> = emptyList(),
    val sharedPrs: List<SharedPersonalRecordDto> = emptyList(),
    val shareSuccessMessage: String? = null,
    val isSharing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    private val repository: WorkoutSessionRepository,
    private val analyticsRepository: AnalyticsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val sessionId = savedStateHandle.get<Long>("sessionId")
        ?: savedStateHandle.get<String>("sessionId")?.toLongOrNull()
        ?: 0L
    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (sessionId <= 0L) {
            _uiState.update { it.copy(isLoading = false, error = "Workout session not found.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val sessionResult = repository.getSession(sessionId)
            val prsResult = analyticsRepository.getWorkoutSessionRecords(sessionId)
            val sharedPrsResult = analyticsRepository.getClientSharedPRs()

            sessionResult
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = session,
                            prs = prsResult.getOrDefault(emptyList()),
                            sharedPrs = sharedPrsResult.getOrDefault(emptyList())
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.localizedMessage ?: "Could not load workout summary.")
                    }
                }
        }
    }

    fun shareRecordWithCoach(prId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true) }
            analyticsRepository.sharePersonalRecord(prId)
                .onSuccess { shared ->
                    _uiState.update {
                        it.copy(
                            isSharing = false,
                            sharedPrs = it.sharedPrs + shared,
                            shareSuccessMessage = "Personal Record shared with your Coach!"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSharing = false,
                            error = error.localizedMessage ?: "Failed to share PR with coach."
                        )
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(shareSuccessMessage = null, error = null) }
    }
}
