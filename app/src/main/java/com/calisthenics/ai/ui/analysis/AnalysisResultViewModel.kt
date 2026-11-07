package com.calisthenics.ai.ui.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.usecase.GetAnalysisByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisResultUiState(
    val isLoading: Boolean = false,
    val result: AnalysisResult? = null,
    val error: String? = null
)

@HiltViewModel
class AnalysisResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnalysisByIdUseCase: GetAnalysisByIdUseCase
) : ViewModel() {

    private val analysisId: Long = savedStateHandle.get<String>("analysisId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(AnalysisResultUiState())
    val uiState: StateFlow<AnalysisResultUiState> = _uiState.asStateFlow()

    init {
        loadAnalysisResult()
    }

    fun loadAnalysisResult() {
        if (analysisId <= 0L) {
            _uiState.update { it.copy(error = "Invalid analysis ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getAnalysisByIdUseCase(analysisId)
                .onSuccess { analysisResult ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            result = analysisResult
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Failed to retrieve analysis report"
                        )
                    }
                }
        }
    }
}
