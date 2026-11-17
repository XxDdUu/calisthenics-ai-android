package com.calisthenics.ai.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSession
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.data.local.TodayWorkoutPreferences
import com.calisthenics.ai.data.local.WorkoutExperiencePreferences
import com.calisthenics.ai.domain.model.GettingStartedProgram
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.TodayWorkoutCandidate
import com.calisthenics.ai.domain.model.WorkoutSource
import com.calisthenics.ai.domain.usecase.GetWorkoutPlanDetailsUseCase
import com.calisthenics.ai.domain.repository.GettingStartedRepository
import com.calisthenics.ai.domain.repository.NoOpGettingStartedRepository
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import com.calisthenics.ai.ui.workoutsession.bubble.UnfinishedWorkoutBubbleCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayWorkoutUiState(
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val plans: List<UserWorkoutPlan> = emptyList(),
    val selection: TodayWorkoutSelection = TodayWorkoutSelection(TodayWorkoutKind.NO_PLANS),
    val activeSessions: List<WorkoutSession> = emptyList(),
    val upcomingSessions: List<WorkoutSession> = emptyList(),
    val activeGettingStartedProgram: GettingStartedProgram? = null,
    val startingGettingStartedDay: Int? = null,
    val activeSessionsError: String? = null,
    val error: String? = null,
    val isOnline: Boolean = true,
    val isBubbleEnabled: Boolean = true
)

@HiltViewModel
class TodayWorkoutViewModel @Inject constructor(
    private val getWorkoutPlanDetailsUseCase: GetWorkoutPlanDetailsUseCase,
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val todayWorkoutPreferences: TodayWorkoutPreferences,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager(),
    private val workoutExperiencePreferences: WorkoutExperiencePreferences? = null,
    private val bubbleCoordinator: UnfinishedWorkoutBubbleCoordinator? = null,
    private val gettingStartedRepository: GettingStartedRepository = NoOpGettingStartedRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TodayWorkoutUiState(isOnline = connectivityMonitor.isConnected()))
    val uiState: StateFlow<TodayWorkoutUiState> = _uiState.asStateFlow()
    private var selectedCandidates: Map<Long, TodayWorkoutCandidate> = emptyMap()
    private var loadSessionsJob: Job? = null
    private var loadTodayPlanJob: Job? = null

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            loadSessionsJob?.cancel()
            loadTodayPlanJob?.cancel()
            selectedCandidates = emptyMap()
            _uiState.value = TodayWorkoutUiState(
                date = _uiState.value.date,
                isOnline = connectivityMonitor.isConnected()
            )
        },
        onSessionActive = {
            loadPlans(force = true)
        }
    )

    init {
        sessionGuard.startObserving()
        loadPlans()
        observeConnectivity()
        observePlanInvalidations()
        observeGettingStartedUpdates()
        observeBubblePreference()
    }

    private fun observeGettingStartedUpdates() {
        viewModelScope.launch {
            runCatching { gettingStartedRepository.enrollmentUpdates }.getOrNull()?.collect {
                loadPlans(force = true)
            }
        }
    }

    fun startGettingStartedDay(dayNumber: Int, onStarted: (WorkoutSession) -> Unit) {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(startingGettingStartedDay = dayNumber) }
            gettingStartedRepository.startDaySession(dayNumber)
                .onSuccess { session ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { it.copy(startingGettingStartedDay = null) }
                    onStarted(session)
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            startingGettingStartedDay = null,
                            activeSessionsError = error.localizedMessage ?: "Could not start Day $dayNumber."
                        )
                    }
                }
        }
    }

    private fun observeBubblePreference() {
        val prefs = workoutExperiencePreferences ?: return
        viewModelScope.launch {
            prefs.unfinishedWorkoutBubbleEnabled.collect { enabled ->
                _uiState.update { it.copy(isBubbleEnabled = enabled) }
            }
        }
    }

    fun openWorkoutBubble(): Boolean {
        if (!_uiState.value.isBubbleEnabled) {
            return false
        }
        if (_uiState.value.activeSessions.isEmpty()) {
            return false
        }
        bubbleCoordinator?.openBubble()
        return true
    }

    private fun observePlanInvalidations() {
        viewModelScope.launch {
            runCatching { workoutPlanRepository.planCacheInvalidations }.getOrNull()?.collect {
                loadPlans(force = true)
            }
        }
    }

    private fun candidateSource(candidate: TodayWorkoutCandidate): WorkoutSource =
        if (candidate.assignmentId != null) WorkoutSource.COACH else WorkoutSource.PERSONAL

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityMonitor.isOnline.collect { online ->
                val wasOffline = !_uiState.value.isOnline
                _uiState.update { it.copy(isOnline = online) }
                if (online && wasOffline) {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        loadPlans()
    }

    fun onAppResumed() {
        val today = LocalDate.now()
        if (_uiState.value.isLoading) return
        if (today != _uiState.value.date) _uiState.update { it.copy(date = today) }
        loadPlans()
    }

    fun selectPlan(planId: Long) {
        if (_uiState.value.isLoading) return
        val source = _uiState.value.selection.source
        val candidate = _uiState.value.plans.firstOrNull { it.id == planId }
        val assignmentId = selectedCandidates[planId]?.assignmentId
            ?: _uiState.value.selection.takeIf { it.plan?.id == planId }?.assignmentId
        val coachName = selectedCandidates[planId]?.coachName
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadTodayPlanJob = sessionGuard.launchScoped { captured ->
            workoutPlanRepository.setActivePlanId(planId)
                .onSuccess {
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    if (candidate == null) {
                        _uiState.update {
                            it.copy(isLoading = false, error = "That workout is no longer available. Refresh today's workout.")
                        }
                    } else {
                        loadSelectedPlan(_uiState.value.plans, planId, source, assignmentId, coachName, captured)
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Could not save your active workout plan."
                        )
                    }
                }
        }
    }

    private fun loadPlans(force: Boolean = false) {
        if (_uiState.value.isLoading && !force) return
        if (force) {
            loadSessionsJob?.cancel()
            loadTodayPlanJob?.cancel()
        }
        selectedCandidates = emptyMap()
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }
        if (connectivityMonitor.isConnected()) {
            loadSessionsJob = sessionGuard.launchScoped { captured ->
                workoutSessionRepository.getActiveSessions()
                    .onSuccess { sessions ->
                        if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                        val today = _uiState.value.date
                        val unfinished = sessions.filter {
                            it.effectiveClassification(today) == com.calisthenics.ai.domain.model.WorkoutSessionClassification.UNFINISHED
                        }
                        _uiState.update { it.copy(activeSessions = unfinished, activeSessionsError = null) }
                    }
                    .onFailure { error ->
                        if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                        _uiState.update {
                            it.copy(activeSessionsError = error.localizedMessage ?: "Could not load unfinished workouts.")
                        }
                    }
                workoutSessionRepository.getUpcomingSessions()
                    .onSuccess { sessions ->
                        if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                        val today = _uiState.value.date
                        val upcoming = sessions.filter {
                            it.effectiveClassification(today) == com.calisthenics.ai.domain.model.WorkoutSessionClassification.UPCOMING
                        }
                        _uiState.update { it.copy(upcomingSessions = upcoming) }
                    }
                gettingStartedRepository.getPublishedProgram()
                    .onSuccess { program ->
                        if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                        val activeProgram = program?.takeIf { prog ->
                            val enrollment = prog.enrollment
                            enrollment != null && enrollment.addedToWorkoutPlan && !enrollment.isCompleted
                        }
                        _uiState.update { it.copy(activeGettingStartedProgram = activeProgram) }
                    }
            }
        } else {
            _uiState.update {
                it.copy(
                    activeSessions = emptyList(),
                    upcomingSessions = emptyList(),
                    activeSessionsError = null
                )
            }
        }
        loadTodayPlanJob = sessionGuard.launchScoped { captured ->
            val date = _uiState.value.date
            val activePlanId = workoutPlanRepository.getActivePlanId().getOrNull()
            workoutPlanRepository.resolveTodayWorkout(date.toString(), activePlanId)
                .onSuccess { resolution ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    val preferredSource = todayWorkoutPreferences.priority.first()
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    val orderedCandidates = resolution.candidates.sortedBy {
                        if (candidateSource(it) == preferredSource) 0 else 1
                    }
                    selectedCandidates = orderedCandidates.associateBy { it.plan.id }
                    val plans = orderedCandidates.map { it.plan }
                    val hasMultipleSources = orderedCandidates
                        .map(::candidateSource)
                        .distinct()
                        .size > 1
                    val selectedId = if (hasMultipleSources) {
                        orderedCandidates.firstOrNull()?.plan?.id
                    } else {
                        resolution.selectedPlanId
                    }
                    if (selectedId == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                plans = plans,
                                selection = selectTodayWorkout(
                                    plans, date, null, resolution.source
                                )
                            )
                        }

                    } else {
                        val candidate = selectedCandidates[selectedId]
                        if (candidate == null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    plans = plans,
                                    selection = selectTodayWorkout(plans, date, selectedId, resolution.source)
                                )
                            }
                        } else {
                            loadSelectedPlan(
                                plans,
                                selectedId,
                                candidateSource(candidate),
                                candidate.assignmentId,
                                candidate.coachName,
                                captured
                            )
                        }
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (!connectivityMonitor.isConnected()) null else (error.localizedMessage ?: "Could not resolve today's workout.")
                        )
                    }
                }
        }
    }

    private suspend fun loadSelectedPlan(
        plans: List<UserWorkoutPlan>,
        planId: Long,
        source: WorkoutSource,
        assignmentId: Long?,
        coachName: String?,
        captured: AccountSession = sessionGuard.currentSession()
    ) {
        if (!sessionGuard.isCurrentSession(captured)) return
        val date = _uiState.value.date
        val shallowPlan = plans.firstOrNull { it.id == planId }
        if (shallowPlan == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    plans = plans,
                    selection = selectTodayWorkout(plans, date, null, source)
                )
            }
            return
        }

        getWorkoutPlanDetailsUseCase(planId)
            .onSuccess { plan ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                val selection = selectTodayWorkout(
                    listOf(plan), date, plan.id, source, assignmentId, coachName
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        plans = plans,
                        selection = selection
                    )
                }
            }
            .onFailure { error ->
                if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        plans = plans,
                        error = error.localizedMessage ?: "Could not load today's workout details."
                    )
                }
            }
    }
}
