package com.calisthenics.ai.ui.pr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PersonalRecordsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val prs: List<PersonalRecordDto> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val selectedPrTypeFilter: String = "ALL",
    val selectedChartExerciseName: String? = null,
    val selectedChartExerciseId: Long? = null,
    val chartHistory: List<PersonalRecordDto> = emptyList(),
    val chartFilter: String = "All",
    val editingPr: PersonalRecordDto? = null,
    val isFormOpen: Boolean = false,
    val deletingPr: PersonalRecordDto? = null,
    val error: String? = null,
    val successMessage: String? = null
) {
    val filteredPrs: List<PersonalRecordDto>
        get() = if (selectedPrTypeFilter == "ALL") {
            prs
        } else {
            prs.filter { it.recordType.equals(selectedPrTypeFilter, ignoreCase = true) }
        }

    val prsGroupedByExercise: Map<String, List<PersonalRecordDto>>
        get() = filteredPrs.groupBy { it.exerciseName.ifBlank { "Other Exercises" } }

    val chartDataPoints: List<ProgressChartDataPointDto>
        get() = chartHistory.map { pr ->
            ProgressChartDataPointDto(
                date = pr.achievedAt.take(10),
                value = pr.recordValue,
                isPersonalRecord = true,
                sessionId = pr.workoutSessionId
            )
        }
}

@HiltViewModel
class PersonalRecordsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalRecordsUiState())
    val uiState: StateFlow<PersonalRecordsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // Load exercises
            exerciseRepository.getExercises().onSuccess { exList ->
                _uiState.update { it.copy(exercises = exList) }
            }

            // Load user PRs
            analyticsRepository.getUserPersonalRecords().fold(
                onSuccess = { prList ->
                    val defaultChartExercise = prList.firstOrNull()?.exerciseName
                    val defaultChartExerciseId = prList.firstOrNull()?.exerciseId
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            prs = prList,
                            selectedChartExerciseName = it.selectedChartExerciseName ?: defaultChartExercise,
                            selectedChartExerciseId = it.selectedChartExerciseId ?: defaultChartExerciseId
                        )
                    }
                    loadChartProgression()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to load personal records"
                        )
                    }
                }
            )
        }
    }

    fun selectPrTypeFilter(filter: String) {
        _uiState.update { it.copy(selectedPrTypeFilter = filter) }
    }

    fun selectChartExercise(exerciseName: String, exerciseId: Long?) {
        _uiState.update {
            it.copy(
                selectedChartExerciseName = exerciseName,
                selectedChartExerciseId = exerciseId
            )
        }
        loadChartProgression()
    }

    fun setChartFilter(filter: String) {
        _uiState.update { it.copy(chartFilter = filter) }
    }

    private fun loadChartProgression() {
        val exerciseId = _uiState.value.selectedChartExerciseId
        val exerciseName = _uiState.value.selectedChartExerciseName

        if (exerciseId != null && exerciseId > 0) {
            viewModelScope.launch {
                analyticsRepository.getPrHistory(exerciseId).onSuccess { history ->
                    _uiState.update { it.copy(chartHistory = history) }
                }.onFailure {
                    // Fallback to local PRs with same exercise name
                    val local = _uiState.value.prs.filter { it.exerciseName.equals(exerciseName, ignoreCase = true) }
                        .sortedBy { it.achievedAt }
                    _uiState.update { it.copy(chartHistory = local) }
                }
            }
        } else if (!exerciseName.isNullOrBlank()) {
            val local = _uiState.value.prs.filter { it.exerciseName.equals(exerciseName, ignoreCase = true) }
                .sortedBy { it.achievedAt }
            _uiState.update { it.copy(chartHistory = local) }
        }
    }

    fun openAddPrForm(preselectedExerciseId: Long? = null, preselectedExerciseName: String? = null) {
        val exercise = preselectedExerciseId?.let { id -> _uiState.value.exercises.find { it.id == id } }
        val name = preselectedExerciseName ?: exercise?.name
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPr = null
            )
        }
    }

    fun openEditPrForm(pr: PersonalRecordDto) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPr = pr
            )
        }
    }

    fun closeForm() {
        _uiState.update { it.copy(isFormOpen = false, editingPr = null) }
    }

    fun savePr(
        exerciseId: Long?,
        exerciseName: String,
        prType: String,
        value: Double,
        unit: String,
        achievedAt: String,
        notes: String?
    ) {
        if (value <= 0) {
            _uiState.update { it.copy(error = "Value must be greater than 0") }
            return
        }
        if (exerciseId == null && exerciseName.isBlank()) {
            _uiState.update { it.copy(error = "Please select or enter an exercise") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val editing = _uiState.value.editingPr
            if (editing != null) {
                // Update
                val request = UpdatePrRequestDto(
                    prType = prType,
                    value = value,
                    unit = unit,
                    achievedAt = achievedAt,
                    notes = notes
                )
                analyticsRepository.updatePersonalRecord(editing.id, request).fold(
                    onSuccess = { updated ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isFormOpen = false,
                                editingPr = null,
                                successMessage = "PR updated successfully!"
                            )
                        }
                        loadData()
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = err.message ?: "Failed to update PR"
                            )
                        }
                    }
                )
            } else {
                // Create
                val request = CreatePrRequestDto(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName.ifBlank { null },
                    prType = prType,
                    value = value,
                    unit = unit,
                    achievedAt = achievedAt,
                    notes = notes
                )
                analyticsRepository.createPersonalRecord(request).fold(
                    onSuccess = { created ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isFormOpen = false,
                                editingPr = null,
                                selectedChartExerciseName = created.exerciseName,
                                selectedChartExerciseId = created.exerciseId,
                                successMessage = "New PR created!"
                            )
                        }
                        loadData()
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = err.message ?: "Failed to save PR"
                            )
                        }
                    }
                )
            }
        }
    }

    fun promptDelete(pr: PersonalRecordDto) {
        _uiState.update { it.copy(deletingPr = pr) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deletingPr = null) }
    }

    fun confirmDelete() {
        val pr = _uiState.value.deletingPr ?: return
        _uiState.update { it.copy(deletingPr = null, isLoading = true) }
        viewModelScope.launch {
            analyticsRepository.deletePersonalRecord(pr.id).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "PR deleted successfully"
                        )
                    }
                    loadData()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to delete PR"
                        )
                    }
                }
            )
        }
    }

    fun shareWithCoach(prId: Long) {
        viewModelScope.launch {
            analyticsRepository.sharePersonalRecord(prId).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "PR shared with your coach!") }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(error = err.message ?: "Could not share PR with coach") }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

