package com.calisthenics.ai.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.usecase.GetExercisesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseSelectionUiState(
    val isLoading: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    val parentExercises: List<ParentExerciseGroup> = emptyList(),
    val error: String? = null
)

data class ParentExerciseGroup(
    val name: String,
    val code: String,
    val count: Int
)

@HiltViewModel
class ExerciseSelectionViewModel @Inject constructor(
    private val getExercisesUseCase: GetExercisesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseSelectionUiState())
    val uiState: StateFlow<ExerciseSelectionUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    fun loadExercises() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getExercisesUseCase()
                .onSuccess { exerciseList ->
                    // Group exercises dynamically into top-level exercise families
                    val groups = exerciseList.map { exercise ->
                        extractParentExercise(exercise.name, exercise.code)
                    }.distinctBy { it.code }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            exercises = exerciseList,
                            parentExercises = groups
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Failed to fetch exercises"
                        )
                    }
                }
        }
    }

    private fun extractParentExercise(name: String, code: String): ParentExerciseGroup {
        return when {
            code.contains("PLANCHE", ignoreCase = true) || name.contains("Planche", ignoreCase = true) ->
                ParentExerciseGroup("Planche", "PLANCHE", 4)
            code.contains("LEVER", ignoreCase = true) || name.contains("Lever", ignoreCase = true) ->
                ParentExerciseGroup("Front Lever", "FRONT_LEVER", 1)
            code.contains("HANDSTAND", ignoreCase = true) || name.contains("Handstand", ignoreCase = true) ->
                ParentExerciseGroup("Handstand", "HANDSTAND", 1)
            code.contains("DIP", ignoreCase = true) || name.contains("Dip", ignoreCase = true) ->
                ParentExerciseGroup("Dips", "DIPS", 1)
            code.contains("MUSCLE_UP", ignoreCase = true) || name.contains("Muscle-Up", ignoreCase = true) ->
                ParentExerciseGroup("Muscle-Up", "MUSCLE_UP", 1)
            else -> ParentExerciseGroup(name, code, 1)
        }
    }
}
