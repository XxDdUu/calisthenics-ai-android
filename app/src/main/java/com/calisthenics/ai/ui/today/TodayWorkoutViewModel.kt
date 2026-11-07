package com.calisthenics.ai.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.TodayWorkoutCandidate
import com.calisthenics.ai.domain.model.WorkoutSource
import com.calisthenics.ai.domain.usecase.GetWorkoutPlanDetailsUseCase
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayWorkoutUiState(
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val plans: List<UserWorkoutPlan> = emptyList(),
    val selection: TodayWorkoutSelection = TodayWorkoutSelection(TodayWorkoutKind.NO_PLANS),
    val activeSessions: List<WorkoutSession> = emptyList(),
    val activeSessionsError: String? = null,
    val error: String? = null,
    val isOnline: Boolean = true
)

@HiltViewModel
class TodayWorkoutViewModel @Inject constructor(
    private val getWorkoutPlanDetailsUseCase: GetWorkoutPlanDetailsUseCase,
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {
    private val _uiState = MutableStateFlow(TodayWorkoutUiState(isOnline = connectivityMonitor.isConnected()))
    val uiState: StateFlow<TodayWorkoutUiState> = _uiState.asStateFlow()
    private var selectedCandidates: Map<Long, TodayWorkoutCandidate> = emptyMap()

    init {
        loadPlans()
        observeConnectivity()
    }

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
        viewModelScope.launch {
            workoutPlanRepository.setActivePlanId(planId)
                .onSuccess {
                    if (candidate == null) {
                        _uiState.update {
                            it.copy(isLoading = false, error = "That workout is no longer available. Refresh today's workout.")
                        }
                    } else {
                        loadSelectedPlan(_uiState.value.plans, planId, source, assignmentId, coachName)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Could not save your active workout plan."
                        )
                    }
                }
        }
    }

    private fun loadPlans() {
        if (_uiState.value.isLoading) return
        selectedCandidates = emptyMap()
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }
        if (connectivityMonitor.isConnected()) {
            viewModelScope.launch {
                workoutSessionRepository.getActiveSessions()
                    .onSuccess { sessions ->
                        _uiState.update { it.copy(activeSessions = sessions, activeSessionsError = null) }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(activeSessionsError = error.localizedMessage ?: "Could not load unfinished workouts.")
                        }
                    }
            }
        } else {
            _uiState.update { it.copy(activeSessions = emptyList(), activeSessionsError = null) }
        }
        viewModelScope.launch {
            val date = _uiState.value.date
            val activePlanId = workoutPlanRepository.getActivePlanId().getOrNull()
            workoutPlanRepository.resolveTodayWorkout(date.toString(), activePlanId)
                .onSuccess { resolution ->
                    selectedCandidates = resolution.candidates.associateBy { it.plan.id }
                    val plans = resolution.candidates.map { it.plan }
                    val selectedId = resolution.selectedPlanId
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
                                resolution.source,
                                candidate.assignmentId,
                                candidate.coachName
                            )
                        }
                    }
                }
                .onFailure { error ->
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
        coachName: String?
    ) {
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
