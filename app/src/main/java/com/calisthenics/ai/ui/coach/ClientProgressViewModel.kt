package com.calisthenics.ai.ui.coach

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.BodyMetricsRepository
import com.calisthenics.ai.domain.repository.ExerciseRepository
import com.calisthenics.ai.ui.bodymetrics.BodyMetricType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    val feedbackSuccessMessage: String? = null,
    val bodyMetrics: CoachClientBodyMetricsDto? = null,
    val selectedBodyMetric: BodyMetricType = BodyMetricType.WEIGHT,
    val selectedBodyMetricFilter: String = "All"
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

    val bodyMetricChartPoints: List<ProgressChartDataPointDto>
        get() {
            val measurements = bodyMetrics?.measurements.orEmpty()
            val sorted = measurements.sortedBy { it.measuredAt }
            val now = LocalDate.now()
            val cutoff = when (selectedBodyMetricFilter) {
                "7d" -> now.minusDays(7)
                "30d" -> now.minusDays(30)
                "90d" -> now.minusDays(90)
                "6m" -> now.minusDays(180)
                "1y" -> now.minusDays(365)
                else -> null
            }
            val filtered = if (cutoff != null) {
                sorted.filter {
                    try {
                        LocalDate.parse(it.measuredAt.take(10)) >= cutoff
                    } catch (_: Exception) {
                        true
                    }
                }
            } else {
                sorted
            }

            return filtered.mapNotNull { m ->
                val v = when (selectedBodyMetric) {
                    BodyMetricType.WEIGHT -> m.weightKg
                    BodyMetricType.BODY_FAT -> m.bodyFatPercentage
                    BodyMetricType.WAIST -> m.waistCm
                    BodyMetricType.CHEST -> m.chestCm
                    BodyMetricType.ARM -> m.armCm
                    BodyMetricType.THIGH -> m.thighCm
                }
                v?.let {
                    ProgressChartDataPointDto(
                        date = m.measuredAt.take(10),
                        value = it
                    )
                }
            }
        }
}

@HiltViewModel
class ClientProgressViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val exerciseRepository: ExerciseRepository,
    private val bodyMetricsRepository: BodyMetricsRepository,
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
            loadBodyMetrics()
        }
    }

    fun onBodyMetricSelected(metric: BodyMetricType) {
        _uiState.update { it.copy(selectedBodyMetric = metric) }
    }

    fun onBodyMetricFilterSelected(filter: String) {
        _uiState.update { it.copy(selectedBodyMetricFilter = filter) }
    }

    fun loadBodyMetrics() {
        viewModelScope.launch {
            bodyMetricsRepository.getCoachClientBodyMetrics(clientId, sort = "asc")
                .onSuccess { data ->
                    _uiState.update { it.copy(bodyMetrics = data) }
                }
                .onFailure {
                    // Handled gracefully or fallback
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

    fun submitFeedback(shareId: Long, feedback: String, rating: Int? = null) {
        if (feedback.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(feedbackSubmitting = true) }
            analyticsRepository.addCoachFeedback(shareId, feedback, rating)
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

    fun markShareAsRead(shareId: Long) {
        viewModelScope.launch {
            analyticsRepository.markPrShareAsRead(shareId)
                .onSuccess { updatedShare ->
                    _uiState.update { current ->
                        val updatedList = current.sharedPrs.map {
                            if (it.id == updatedShare.id) updatedShare else it
                        }
                        current.copy(sharedPrs = updatedList)
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, feedbackSuccessMessage = null) }
    }
}
