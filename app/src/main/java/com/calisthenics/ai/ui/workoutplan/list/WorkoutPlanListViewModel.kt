package com.calisthenics.ai.ui.workoutplan.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.usecase.CreateWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.DeleteWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlansUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutPlanListUiState(
    val isLoading: Boolean = false,
    val plans: List<UserWorkoutPlan> = emptyList(),
    val error: String? = null,
    val isOnline: Boolean = true,
    val isCreateDialogOpen: Boolean = false,
    val createPlanName: String = "",
    val createSplitType: WorkoutSplitType = WorkoutSplitType.PPL,
    val isCreating: Boolean = false,
    val createError: String? = null,
    val planToDelete: UserWorkoutPlan? = null,
    val isDeleting: Boolean = false
)

@HiltViewModel
class WorkoutPlanListViewModel @Inject constructor(
    private val getWorkoutPlansUseCase: GetWorkoutPlansUseCase,
    private val createWorkoutPlanUseCase: CreateWorkoutPlanUseCase,
    private val deleteWorkoutPlanUseCase: DeleteWorkoutPlanUseCase,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WorkoutPlanListUiState(isOnline = connectivityMonitor.isConnected())
    )
    val uiState: StateFlow<WorkoutPlanListUiState> = _uiState.asStateFlow()

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
                    loadPlans()
                }
            }
        }
    }

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getWorkoutPlansUseCase()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, plans = list) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (!connectivityMonitor.isConnected()) null else (throwable.localizedMessage ?: "Failed to load workout plans")
                        )
                    }
                }
        }
    }

    fun openCreateDialog() {
        if (!connectivityMonitor.isConnected()) {
            _uiState.update { it.copy(error = "Cannot create workout plans while offline.") }
            return
        }
        _uiState.update {
            it.copy(
                isCreateDialogOpen = true,
                createPlanName = "",
                createSplitType = WorkoutSplitType.PPL,
                createError = null
            )
        }
    }

    fun closeCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false, createError = null) }
    }

    fun updateCreatePlanName(name: String) {
        _uiState.update { it.copy(createPlanName = name, createError = null) }
    }

    fun updateCreateSplitType(splitType: WorkoutSplitType) {
        _uiState.update { it.copy(createSplitType = splitType) }
    }

    fun submitCreatePlan(onSuccess: (Long) -> Unit = {}) {
        if (!connectivityMonitor.isConnected()) {
            _uiState.update { it.copy(createError = "Cannot create workout plans while offline.") }
            return
        }
        val state = _uiState.value
        val name = state.createPlanName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(createError = "Plan name cannot be empty") }
            return
        }

        val days = defaultDaysForSplit(state.createSplitType)

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            createWorkoutPlanUseCase(name, state.createSplitType, days)
                .onSuccess { createdPlan ->
                    _uiState.update { it.copy(isCreating = false, isCreateDialogOpen = false) }
                    loadPlans()
                    onSuccess(createdPlan.id)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            createError = throwable.localizedMessage ?: "Could not create workout plan"
                        )
                    }
                }
        }
    }

    fun confirmDeletePlan(plan: UserWorkoutPlan) {
        if (!connectivityMonitor.isConnected()) {
            _uiState.update { it.copy(error = "Cannot delete workout plans while offline.") }
            return
        }
        _uiState.update { it.copy(planToDelete = plan) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(planToDelete = null) }
    }

    fun executeDeletePlan() {
        if (!connectivityMonitor.isConnected()) {
            _uiState.update { it.copy(error = "Cannot delete workout plans while offline.", planToDelete = null) }
            return
        }
        val plan = _uiState.value.planToDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            deleteWorkoutPlanUseCase(plan.id)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, planToDelete = null) }
                    loadPlans()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            planToDelete = null,
                            error = throwable.localizedMessage ?: "Failed to delete workout plan"
                        )
                    }
                }
        }
    }

    private fun defaultDaysForSplit(splitType: WorkoutSplitType): List<Pair<WorkoutDayOfWeek, WorkoutSessionType>> {
        return when (splitType) {
            WorkoutSplitType.PPL -> listOf(
                WorkoutDayOfWeek.MONDAY to WorkoutSessionType.PUSH,
                WorkoutDayOfWeek.TUESDAY to WorkoutSessionType.PULL,
                WorkoutDayOfWeek.WEDNESDAY to WorkoutSessionType.LEGS,
                WorkoutDayOfWeek.THURSDAY to WorkoutSessionType.PUSH,
                WorkoutDayOfWeek.FRIDAY to WorkoutSessionType.PULL,
                WorkoutDayOfWeek.SATURDAY to WorkoutSessionType.LEGS,
                WorkoutDayOfWeek.SUNDAY to WorkoutSessionType.REST
            )
            WorkoutSplitType.UPPER_LOWER -> listOf(
                WorkoutDayOfWeek.MONDAY to WorkoutSessionType.UPPER,
                WorkoutDayOfWeek.TUESDAY to WorkoutSessionType.LOWER,
                WorkoutDayOfWeek.WEDNESDAY to WorkoutSessionType.REST,
                WorkoutDayOfWeek.THURSDAY to WorkoutSessionType.UPPER,
                WorkoutDayOfWeek.FRIDAY to WorkoutSessionType.LOWER,
                WorkoutDayOfWeek.SATURDAY to WorkoutSessionType.REST,
                WorkoutDayOfWeek.SUNDAY to WorkoutSessionType.REST
            )
            WorkoutSplitType.FULL_BODY -> listOf(
                WorkoutDayOfWeek.MONDAY to WorkoutSessionType.FULL_BODY,
                WorkoutDayOfWeek.TUESDAY to WorkoutSessionType.REST,
                WorkoutDayOfWeek.WEDNESDAY to WorkoutSessionType.FULL_BODY,
                WorkoutDayOfWeek.THURSDAY to WorkoutSessionType.REST,
                WorkoutDayOfWeek.FRIDAY to WorkoutSessionType.FULL_BODY,
                WorkoutDayOfWeek.SATURDAY to WorkoutSessionType.REST,
                WorkoutDayOfWeek.SUNDAY to WorkoutSessionType.REST
            )
            WorkoutSplitType.CUSTOM -> WorkoutDayOfWeek.entries.map { it to WorkoutSessionType.REST }
        }
    }
}
