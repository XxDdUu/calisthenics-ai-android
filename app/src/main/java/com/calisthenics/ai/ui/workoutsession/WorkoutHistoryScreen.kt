package com.calisthenics.ai.ui.workoutsession

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.remote.dto.PersonalRecordDto
import com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto
import com.calisthenics.ai.domain.model.GettingStartedEnrollment
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.GettingStartedRepository
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import com.calisthenics.ai.domain.repository.NoOpGettingStartedRepository
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import com.calisthenics.ai.ui.pr.ShareRecipientOption
import com.calisthenics.ai.ui.pr.toShareRecipients
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class WorkoutHistoryFilter {
    ALL,
    COMPLETED,
    UNFINISHED,
    UPCOMING
}

data class WorkoutHistoryUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val sessions: List<WorkoutSession> = emptyList(),
    val unfinishedSessions: List<WorkoutSession> = emptyList(),
    val upcomingSessions: List<WorkoutSession> = emptyList(),
    val completedGettingStartedPrograms: List<GettingStartedEnrollment> = emptyList(),
    val selectedFilter: WorkoutHistoryFilter = WorkoutHistoryFilter.ALL,
    val page: Int = 0,
    val totalPages: Int = 0,
    val error: String? = null,
    val selectedTab: Int = 0,
    val liveAnalysisSessions: List<LiveSessionRecord> = emptyList(),
    val isLoadingLiveAnalysis: Boolean = false,
    val liveAnalysisError: String? = null
)

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val repository: WorkoutSessionRepository,
    private val liveAnalysisRepository: LiveAnalysisRepository,
    private val workoutPlanRepository: com.calisthenics.ai.domain.repository.WorkoutPlanRepository,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager(),
    private val gettingStartedRepository: GettingStartedRepository = NoOpGettingStartedRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = { _uiState.value = WorkoutHistoryUiState() },
        onSessionActive = {
            load()
            loadLiveAnalysis()
        }
    )

    init {
        sessionGuard.startObserving()
        load()
        loadLiveAnalysis()
        viewModelScope.launch {
            runCatching { workoutPlanRepository.planCacheInvalidations }.getOrNull()?.collect {
                load()
            }
        }
        viewModelScope.launch {
            runCatching { gettingStartedRepository.enrollmentUpdates }.getOrNull()?.collect {
                load()
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == 1 && _uiState.value.liveAnalysisSessions.isEmpty() && !_uiState.value.isLoadingLiveAnalysis) {
            loadLiveAnalysis()
        }
    }

    fun loadLiveAnalysis() {
        _uiState.update { it.copy(isLoadingLiveAnalysis = true, liveAnalysisError = null) }
        sessionGuard.launchScoped { captured ->
            liveAnalysisRepository.getSessions()
                .onSuccess { sessions ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isLoadingLiveAnalysis = false,
                            liveAnalysisSessions = sessions,
                            liveAnalysisError = null
                        )
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoadingLiveAnalysis = false,
                            liveAnalysisError = error.localizedMessage ?: "Could not load AI analysis history."
                        )
                    }
                }
        }
    }

    fun selectFilter(filter: WorkoutHistoryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun load() {
        if (_uiState.value.isLoading) return
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                sessions = emptyList(),
                unfinishedSessions = emptyList(),
                upcomingSessions = emptyList(),
                page = 0
            )
        }
        sessionGuard.launchScoped { captured ->
            val today = java.time.LocalDate.now()
            repository.getActiveSessions().onSuccess { active ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                val unfinished = active.filter {
                    it.effectiveClassification(today) == com.calisthenics.ai.domain.model.WorkoutSessionClassification.UNFINISHED
                }
                _uiState.update { it.copy(unfinishedSessions = unfinished) }
            }
            repository.getUpcomingSessions().onSuccess { upcoming ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                val filteredUpcoming = upcoming.filter {
                    it.effectiveClassification(today) == com.calisthenics.ai.domain.model.WorkoutSessionClassification.UPCOMING
                }
                _uiState.update { it.copy(upcomingSessions = filteredUpcoming) }
            }
            gettingStartedRepository.getUserEnrollments().onSuccess { enrollments ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                _uiState.update {
                    it.copy(completedGettingStartedPrograms = enrollments.filter { e -> e.isCompleted })
                }
            }
            repository.getHistory(page = 0, size = PAGE_SIZE)
                .onSuccess { result ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessions = result.content,
                            page = result.page,
                            totalPages = result.totalPages,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.localizedMessage ?: "Could not load workout history.")
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.page + 1 >= state.totalPages) return
        _uiState.update { it.copy(isLoadingMore = true, error = null) }
        viewModelScope.launch {
            repository.getHistory(page = state.page + 1, size = PAGE_SIZE)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            sessions = it.sessions + result.content,
                            page = result.page,
                            totalPages = result.totalPages
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = error.localizedMessage ?: "Could not load more workout history."
                        )
                    }
                }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

data class WorkoutSummaryUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSession? = null,
    val prs: List<PersonalRecordDto> = emptyList(),
    val sharedPrs: List<SharedPersonalRecordDto> = emptyList(),
    val shareRecipients: List<ShareRecipientOption> = emptyList(),
    val sharingPr: PersonalRecordDto? = null,
    val shareError: String? = null,
    val coachName: String? = null,
    val hasActiveCoach: Boolean = false,
    val shareSuccessMessage: String? = null,
    val isSharing: Boolean = false,
    val gettingStartedCompletedProgram: GettingStartedEnrollment? = null,
    val error: String? = null
)

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    private val repository: WorkoutSessionRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val coachClientRepository: CoachClientRepository,
    savedStateHandle: SavedStateHandle,
    private val gettingStartedRepository: GettingStartedRepository = NoOpGettingStartedRepository
) : ViewModel() {
    private val sessionId = savedStateHandle.get<Long>("sessionId")
        ?: savedStateHandle.get<String>("sessionId")?.toLongOrNull()
        ?: 0L
    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (sessionId <= 0L) {
            _uiState.update { it.copy(isLoading = false, error = "Workout session not found.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val sessionResult = repository.getSession(sessionId)
            val prsResult = analyticsRepository.getWorkoutSessionRecords(sessionId)
            val sharedPrsResult = analyticsRepository.getClientSharedPRs()
            val coachOverviewResult = coachClientRepository.loadOverview()

            val overview = coachOverviewResult.getOrNull()
            val recipients = overview?.relationships?.toShareRecipients(overview.currentUserId).orEmpty()
            val activeCoach = overview?.relationships?.firstOrNull {
                it.status == "ACTIVE" && it.clientId == overview.currentUserId
            }

            sessionResult
                .onSuccess { session ->
                    val completedProgram = if (session.source == "GETTING_STARTED") {
                        gettingStartedRepository.getUserEnrollments().getOrNull()
                            ?.firstOrNull { enrollment ->
                                enrollment.isCompleted &&
                                    (session.gettingStartedEnrollmentId == null || enrollment.id == session.gettingStartedEnrollmentId)
                            }
                    } else null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = session,
                            prs = prsResult.getOrDefault(emptyList()),
                            sharedPrs = sharedPrsResult.getOrDefault(emptyList()),
                            shareRecipients = recipients,
                            coachName = activeCoach?.coachName ?: recipients.firstOrNull()?.name,
                            hasActiveCoach = activeCoach != null || recipients.isNotEmpty(),
                            gettingStartedCompletedProgram = completedProgram
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.localizedMessage ?: "Could not load workout summary.")
                    }
                }
        }
    }

    fun openShareSheet(pr: PersonalRecordDto) {
        _uiState.update { it.copy(sharingPr = pr, shareError = null) }
    }

    fun dismissShareSheet() {
        if (_uiState.value.isSharing) return
        _uiState.update { it.copy(sharingPr = null, shareError = null) }
    }

    fun shareRecordWithCoach(
        prId: Long,
        recipientId: Long? = null,
        clientMessageId: String? = UUID.randomUUID().toString()
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true, shareError = null) }
            analyticsRepository.sharePersonalRecord(prId, recipientId, clientMessageId)
                .onSuccess { shared ->
                    val recipientName = _uiState.value.shareRecipients
                        .firstOrNull { it.userId == recipientId }?.name
                        ?: shared.coachName.ifBlank { "your Coach" }
                    _uiState.update {
                        it.copy(
                            isSharing = false,
                            sharingPr = null,
                            sharedPrs = (it.sharedPrs + shared).distinctBy { s -> s.id },
                            shareSuccessMessage = "Personal Record shared with $recipientName and sent to chat!"
                        )
                    }
                }
                .onFailure { error ->
                    val msg = error.localizedMessage ?: "Failed to share PR with coach."
                    _uiState.update {
                        it.copy(
                            isSharing = false,
                            shareError = msg,
                            error = if (it.sharingPr == null) msg else null
                        )
                    }
                }
        }
    }

    fun shareAllRecordsWithCoach(recipientId: Long? = null) {
        val unsharedIds = _uiState.value.prs
            .filter { pr -> _uiState.value.sharedPrs.none { it.personalRecord.id == pr.id && (recipientId == null || it.coachUserId == recipientId) } }
            .map { it.id }

        if (unsharedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true) }
            analyticsRepository.batchSharePRs(unsharedIds, recipientId, UUID.randomUUID().toString())
                .onSuccess { sharedList ->
                    _uiState.update { current ->
                        val updatedSharedPrs = (current.sharedPrs + sharedList).distinctBy { it.id }
                        current.copy(
                            isSharing = false,
                            sharedPrs = updatedSharedPrs,
                            shareSuccessMessage = "All ${sharedList.size} PRs shared with your Coach!"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSharing = false,
                            error = error.localizedMessage ?: "Failed to share PRs with coach."
                        )
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(shareSuccessMessage = null, error = null, shareError = null) }
    }
}
