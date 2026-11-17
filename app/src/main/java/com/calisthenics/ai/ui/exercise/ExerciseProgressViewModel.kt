package com.calisthenics.ai.ui.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.remote.dto.ExerciseProgressDto
import com.calisthenics.ai.data.remote.dto.ProgressChartDataPointDto
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseProgressUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val progress: ExerciseProgressDto? = null,
    val selectedFilter: String = "30d",
    val selectedMetric: ProgressMetricType = ProgressMetricType.AUTO
)

enum class ProgressMetricType(val label: String, val unit: String) {
    AUTO("Progress", ""),
    WEIGHT("Strength", "kg"),
    REPS("Reps", "reps"),
    DURATION("Duration", "s"),
    VOLUME("Volume", "kg")
}

@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    savedStateHandle: SavedStateHandle,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("exerciseId")
        ?: savedStateHandle.get<String>("exerciseId")?.toLongOrNull()
        ?: 0L

    private val _uiState = MutableStateFlow(ExerciseProgressUiState())
    val uiState: StateFlow<ExerciseProgressUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = { _uiState.value = ExerciseProgressUiState() },
        onSessionActive = { load(filter = _uiState.value.selectedFilter) }
    )

    init {
        sessionGuard.startObserving()
        load(filter = _uiState.value.selectedFilter)
    }

    fun load(filter: String = _uiState.value.selectedFilter) {
        if (exerciseId <= 0L) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid exercise ID.") }
            return
        }

        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, error = null, selectedFilter = filter) }
            analyticsRepository.getExerciseProgress(exerciseId, filter)
                .onSuccess { progress ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    // Determine best default metric based on exercise
                    val defaultMetric = when {
                        progress.maxWeightKg != null && progress.maxWeightKg > 0 -> ProgressMetricType.WEIGHT
                        progress.maxHoldDurationSeconds != null && progress.maxHoldDurationSeconds > 0 -> ProgressMetricType.DURATION
                        else -> ProgressMetricType.REPS
                    }

                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            progress = progress,
                            selectedMetric = if (current.selectedMetric == ProgressMetricType.AUTO) defaultMetric else current.selectedMetric
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Failed to load exercise progress."
                        )
                    }
                }
        }
    }

    fun onFilterSelected(filter: String) {
        load(filter)
    }

    fun onMetricSelected(metric: ProgressMetricType) {
        _uiState.update { it.copy(selectedMetric = metric) }
    }

    fun getMetricChartPoints(): List<ProgressChartDataPointDto> {
        val raw = _uiState.value.progress?.chartData ?: return emptyList()
        val metric = _uiState.value.selectedMetric

        return raw.map { dp ->
            val value = when (metric) {
                ProgressMetricType.WEIGHT -> dp.weightKg ?: dp.value
                ProgressMetricType.REPS -> (dp.reps ?: dp.value.toInt()).toDouble()
                ProgressMetricType.DURATION -> (dp.durationSeconds ?: dp.value.toInt()).toDouble()
                ProgressMetricType.VOLUME -> dp.totalVolume ?: dp.value
                ProgressMetricType.AUTO -> dp.value
            }
            dp.copy(value = value)
        }
    }
}

