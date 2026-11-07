package com.calisthenics.ai.ui.progression

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.usecase.GetProgressionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressionUiState(
    val isLoading: Boolean = false,
    val parentExerciseCode: String = "",
    val progressions: List<Exercise> = emptyList(),
    val selectedProgression: Exercise? = null,
    val error: String? = null
)

@HiltViewModel
class ProgressionSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProgressionsUseCase: GetProgressionsUseCase
) : ViewModel() {

    private val parentExerciseCode: String = savedStateHandle["exerciseCode"] ?: "PLANCHE"

    private val _uiState = MutableStateFlow(ProgressionUiState(parentExerciseCode = parentExerciseCode))
    val uiState: StateFlow<ProgressionUiState> = _uiState.asStateFlow()

    init {
        loadProgressions()
    }

    fun loadProgressions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getProgressionsUseCase(parentExerciseCode)
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressions = list,
                            selectedProgression = list.firstOrNull()
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Failed to fetch progressions"
                        )
                    }
                }
        }
    }

    fun selectProgression(progression: Exercise) {
        _uiState.update { it.copy(selectedProgression = progression) }
    }
}
