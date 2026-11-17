package com.calisthenics.ai.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.ui.pr.ShareRecipientOption
import com.calisthenics.ai.ui.pr.toShareRecipients
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

data class TrainingAnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val overview: TrainingOverviewDto? = null,
    val weekly: WeeklyAnalyticsDto? = null,
    val monthly: MonthlyAnalyticsDto? = null,
    val insights: TrainingInsightsDto? = null,
    val sharedPrs: List<SharedPersonalRecordDto> = emptyList(),
    val shareRecipients: List<ShareRecipientOption> = emptyList(),
    val isLoadingRecipients: Boolean = false,
    val sharingPr: PersonalRecordDto? = null,
    val isSharingPr: Boolean = false,
    val shareError: String? = null,
    val shareSuccessMessage: String? = null
)

@HiltViewModel
class TrainingAnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val coachClientRepository: CoachClientRepository? = null,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingAnalyticsUiState())
    val uiState: StateFlow<TrainingAnalyticsUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = { _uiState.value = TrainingAnalyticsUiState() },
        onSessionActive = { loadAll() }
    )

    init {
        sessionGuard.startObserving()
        loadAll()
    }

    fun loadAll() {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, error = null) }
            val overviewResult = analyticsRepository.getTrainingOverview()
            val weeklyResult = analyticsRepository.getWeeklyAnalytics()
            val monthlyResult = analyticsRepository.getMonthlyAnalytics()
            val insightsResult = analyticsRepository.getTrainingInsights()
            val sharedPrsResult = analyticsRepository.getClientSharedPRs()
            val coachOverview = coachClientRepository?.loadOverview()?.getOrNull()
            if (!sessionGuard.isCurrentSession(captured)) return@launchScoped

            if (overviewResult.isFailure && weeklyResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = overviewResult.exceptionOrNull()?.localizedMessage ?: "Failed to load analytics data."
                    )
                }
                return@launchScoped
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    overview = overviewResult.getOrNull(),
                    weekly = weeklyResult.getOrNull(),
                    monthly = monthlyResult.getOrNull(),
                    insights = insightsResult.getOrNull(),
                    sharedPrs = sharedPrsResult.getOrDefault(emptyList()),
                    shareRecipients = coachOverview?.relationships?.toShareRecipients(coachOverview.currentUserId)
                        ?: it.shareRecipients
                )
            }
        }
    }

    fun openShareSheet(pr: PersonalRecordDto) {
        _uiState.update {
            it.copy(
                sharingPr = pr,
                shareError = null,
                isLoadingRecipients = it.shareRecipients.isEmpty() && coachClientRepository != null
            )
        }
        sessionGuard.launchScoped { captured ->
            coachClientRepository?.loadOverview()?.onSuccess { overview ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                _uiState.update {
                    it.copy(
                        shareRecipients = overview.relationships.toShareRecipients(overview.currentUserId),
                        isLoadingRecipients = false
                    )
                }
            }?.onFailure { err ->
                if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                _uiState.update {
                    it.copy(
                        isLoadingRecipients = false,
                        shareError = err.localizedMessage
                    )
                }
            }
        }
    }

    fun dismissShareSheet() {
        if (_uiState.value.isSharingPr) return
        _uiState.update { it.copy(sharingPr = null, shareError = null) }
    }

    fun shareRecordWithCoach(
        prId: Long,
        recipientId: Long? = null,
        clientMessageId: String? = UUID.randomUUID().toString()
    ) {
        if (_uiState.value.isSharingPr) return
        _uiState.update { it.copy(isSharingPr = true, shareError = null, error = null) }
        sessionGuard.launchScoped { captured ->
            analyticsRepository.sharePersonalRecord(prId, recipientId, clientMessageId)
                .onSuccess { shared ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    val recipientName = _uiState.value.shareRecipients
                        .firstOrNull { it.userId == recipientId }?.name
                        ?: shared.coachName.ifBlank { "your Coach" }
                    _uiState.update {
                        it.copy(
                            isSharingPr = false,
                            sharingPr = null,
                            sharedPrs = (it.sharedPrs + shared).distinctBy { s -> s.id },
                            shareSuccessMessage = "Shared PR with $recipientName and sent to chat!"
                        )
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    val msg = error.localizedMessage ?: "Failed to share PR with coach."
                    _uiState.update {
                        it.copy(
                            isSharingPr = false,
                            shareError = msg,
                            error = if (it.sharingPr == null) msg else null
                        )
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, shareSuccessMessage = null, shareError = null) }
    }
}

