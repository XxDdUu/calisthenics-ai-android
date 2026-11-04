package com.calisthenics.ai.ui.coach

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientProgressUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val overview: TrainingOverviewDto? = null,
    val clientPrs: List<PersonalRecordDto> = emptyList(),
    val clientPrHistory: List<PersonalRecordDto> = emptyList(),
    val sharedPrs: List<SharedPersonalRecordDto> = emptyList(),
    val exercises: List<com.calisthenics.ai.domain.model.Exercise> = emptyList(),
    val selectedExerciseId: Long? = null,
    val selectedPrTypeFilter: String? = null,
    val exerciseProgress: ExerciseProgressDto? = null,
    val selectedFilter: String = "30d",
    val feedbackSubmitting: Boolean = false,
    val feedbackSuccessMessage: String? = null
) {
    val clientPrsGrouped: Map<String, List<PersonalRecordDto>>
        get() = clientPrs
            .filter { selectedPrTypeFilter == null || it.recordType.equals(selectedPrTypeFilter, ignoreCase = true) }
            .groupBy { it.exerciseName.ifBlank { "Other Exercises" } }

    val clientPrChartPoints: List<ProgressChartDataPointDto>
        get() = clientPrHistory.map { pr ->
            ProgressChartDataPointDto(
                date = pr.achievedAt.take(10),
                value = pr.recordValue,
                isPersonalRecord = true,
                sessionId = pr.workoutSessionId
            )
        }
}

@HiltViewModel
class ClientProgressViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val clientId: Long = savedStateHandle.get<Long>("clientId")
        ?: savedStateHandle.get<String>("clientId")?.toLongOrNull()
        ?: 0L

    private val _uiState = MutableStateFlow(ClientProgressUiState())
    val uiState: StateFlow<ClientProgressUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        if (clientId <= 0L) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid client ID.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val overviewResult = analyticsRepository.getCoachClientOverview(clientId)
            val prsResult = analyticsRepository.getCoachClientPrRecords(clientId)
                .recoverCatching { analyticsRepository.getCoachClientPRs(clientId).getOrThrow() }
            val coachSharedPrsResult = analyticsRepository.getCoachSharedPRs()
            val exercisesResult = exerciseRepository.getExercises()

            val sharedForClient = coachSharedPrsResult.getOrDefault(emptyList())
                .filter { it.clientUserId == clientId }

            val exerciseList = exercisesResult.getOrDefault(emptyList())
            val loadedPrs = prsResult.getOrDefault(emptyList())
            val firstExerciseId = exerciseList.firstOrNull()?.id
            val firstPrExerciseId = loadedPrs.firstOrNull()?.exerciseId ?: firstExerciseId

            _uiState.update {
                it.copy(
                    isLoading = false,
                    overview = overviewResult.getOrNull(),
                    clientPrs = loadedPrs,
                    sharedPrs = sharedForClient,
                    exercises = exerciseList,
                    selectedExerciseId = firstExerciseId
                )
            }

            if (firstExerciseId != null) {
                loadExerciseProgress(firstExerciseId, _uiState.value.selectedFilter)
            }
            if (firstPrExerciseId != null) {
                loadClientPrHistory(firstPrExerciseId)
            }
        }
    }

    fun onExerciseSelected(exerciseId: Long) {
        _uiState.update { it.copy(selectedExerciseId = exerciseId) }
        loadExerciseProgress(exerciseId, _uiState.value.selectedFilter)
        loadClientPrHistory(exerciseId)
    }

    fun onPrTypeFilterSelected(prType: String?) {
        _uiState.update { it.copy(selectedPrTypeFilter = prType) }
    }

    fun onFilterSelected(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        val exId = _uiState.value.selectedExerciseId
        if (exId != null) {
            loadExerciseProgress(exId, filter)
        }
    }

    private fun loadClientPrHistory(exerciseId: Long) {
        viewModelScope.launch {
            analyticsRepository.getCoachClientPrHistory(clientId, exerciseId).onSuccess { history ->
                _uiState.update { it.copy(clientPrHistory = history) }
            }.onFailure {
                val local = _uiState.value.clientPrs.filter { it.exerciseId == exerciseId }.sortedBy { it.achievedAt }
                _uiState.update { it.copy(clientPrHistory = local) }
            }
        }
    }

    private fun loadExerciseProgress(exerciseId: Long, filter: String) {
        viewModelScope.launch {
            analyticsRepository.getCoachClientExerciseProgress(clientId, exerciseId, filter)
                .onSuccess { progress ->
                    _uiState.update { it.copy(exerciseProgress = progress) }
                }
                .onFailure {
                    // Fail silently or fallback
                }
        }
    }

    fun submitFeedback(shareId: Long, feedback: String) {
        if (feedback.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(feedbackSubmitting = true) }
            analyticsRepository.addCoachFeedback(shareId, feedback)
                .onSuccess { updatedShare ->
                    _uiState.update { current ->
                        val updatedList = current.sharedPrs.map {
                            if (it.id == updatedShare.id) updatedShare else it
                        }
                        current.copy(
                            feedbackSubmitting = false,
                            sharedPrs = updatedList,
                            feedbackSuccessMessage = "Feedback submitted to client!"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            feedbackSubmitting = false,
                            error = error.localizedMessage ?: "Failed to submit feedback."
                        )
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, feedbackSuccessMessage = null) }
    }
}
