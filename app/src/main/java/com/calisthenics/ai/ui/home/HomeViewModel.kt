package com.calisthenics.ai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.usecase.GetAnalysisHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoadingHistory: Boolean = false,
    val history: List<AnalysisResult> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getAnalysisHistoryUseCase: GetAnalysisHistoryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadHistory() }

    fun loadHistory() {
        if (!authRepository.isLoggedIn()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, error = null) }
            getAnalysisHistoryUseCase()
                .onSuccess { items -> _uiState.update { it.copy(isLoadingHistory = false, history = items) } }
                .onFailure { error -> _uiState.update { it.copy(isLoadingHistory = false, error = error.localizedMessage ?: "Could not load analysis history") } }
        }
    }
}
