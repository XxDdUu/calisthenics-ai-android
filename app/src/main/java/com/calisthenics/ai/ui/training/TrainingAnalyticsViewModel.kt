package com.calisthenics.ai.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainingAnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val overview: TrainingOverviewDto? = null,
    val weekly: WeeklyAnalyticsDto? = null,
    val monthly: MonthlyAnalyticsDto? = null,
    val insights: TrainingInsightsDto? = null,
    val sharedPrs: List<SharedPersonalRecordDto> = emptyList(),
    val shareSuccessMessage: String? = null
)

@HiltViewModel
class TrainingAnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingAnalyticsUiState())
    val uiState: StateFlow<TrainingAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val overviewResult = analyticsRepository.getTrainingOverview()
            val weeklyResult = analyticsRepository.getWeeklyAnalytics()
            val monthlyResult = analyticsRepository.getMonthlyAnalytics()
            val insightsResult = analyticsRepository.getTrainingInsights()
            val sharedPrsResult = analyticsRepository.getClientSharedPRs()

            if (overviewResult.isFailure && weeklyResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = overviewResult.exceptionOrNull()?.localizedMessage ?: "Failed to load analytics data."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    overview = overviewResult.getOrNull(),
                    weekly = weeklyResult.getOrNull(),
                    monthly = monthlyResult.getOrNull(),
                    insights = insightsResult.getOrNull(),
                    sharedPrs = sharedPrsResult.getOrDefault(emptyList())
                )
            }
        }
    }

    fun shareRecordWithCoach(prId: Long) {
        viewModelScope.launch {
            analyticsRepository.sharePersonalRecord(prId)
                .onSuccess { shared ->
                    _uiState.update {
                        it.copy(
                            sharedPrs = it.sharedPrs + shared,
                            shareSuccessMessage = "Shared PR successfully with your Coach!"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.localizedMessage ?: "Failed to share PR with coach."
                        )
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, shareSuccessMessage = null) }
    }
}

