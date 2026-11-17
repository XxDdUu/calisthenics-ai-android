package com.calisthenics.ai.ui.workoutplan.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.GettingStartedDayStatus
import com.calisthenics.ai.domain.model.GettingStartedProgram
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutPlanDayConfiguration
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.model.WorkoutPlanAssignment
import com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus
import com.calisthenics.ai.domain.repository.ExerciseRepository
import com.calisthenics.ai.domain.repository.GettingStartedRepository
import com.calisthenics.ai.domain.repository.NoOpExerciseRepository
import com.calisthenics.ai.domain.repository.NoOpGettingStartedRepository
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

data class ConfiguredAssignmentExercise(
    val exercise: Exercise,
    val sets: Int = 3,
    val reps: Int? = 10,
    val durationSeconds: Int? = null,
    val restSeconds: Int = 60,
    val notes: String = ""
)

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
    val isDeleting: Boolean = false,
    val clients: List<CoachClientRelationship> = emptyList(),
    val selectedClientId: Long? = null,
    val clientPlans: List<UserWorkoutPlan> = emptyList(),
    val isLoadingClientPlans: Boolean = false,
    val clientPlanError: String? = null,
    val assignmentMessage: String? = null,
    val assigningPlanId: Long? = null,
    val availableExercises: List<Exercise> = emptyList(),
    val isLoadingExercises: Boolean = false,
    val exercisesError: String? = null,
    val assignments: List<WorkoutPlanAssignment> = emptyList(),
    val coachAssignments: List<WorkoutPlanAssignment> = emptyList(),
    val assignmentError: String? = null,
    val assignmentToDeactivate: WorkoutPlanAssignment? = null,
    val updatingAssignmentId: Long? = null,
    val activeGettingStartedProgram: GettingStartedProgram? = null,
    val startingGettingStartedDay: Int? = null,
    val gettingStartedError: String? = null
)

@HiltViewModel
class WorkoutPlanListViewModel @Inject constructor(
    private val getWorkoutPlansUseCase: GetWorkoutPlansUseCase,
    private val createWorkoutPlanUseCase: CreateWorkoutPlanUseCase,
    private val deleteWorkoutPlanUseCase: DeleteWorkoutPlanUseCase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val coachClientRepository: com.calisthenics.ai.domain.repository.CoachClientRepository,
    private val tokenManager: TokenManager,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager(),
    private val gettingStartedRepository: GettingStartedRepository = NoOpGettingStartedRepository,
    private val exerciseRepository: ExerciseRepository = NoOpExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WorkoutPlanListUiState(isOnline = connectivityMonitor.isConnected())
    )
    val uiState: StateFlow<WorkoutPlanListUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            _uiState.value = WorkoutPlanListUiState(isOnline = connectivityMonitor.isConnected())
        },
        onSessionActive = {
            loadAllForActiveUser()
        }
    )

    init {
        sessionGuard.startObserving()
        loadAllForActiveUser()
        observeConnectivity()
        observePlanInvalidations()
        observeGettingStartedUpdates()
    }

    private fun observeGettingStartedUpdates() {
        viewModelScope.launch {
            runCatching { gettingStartedRepository.enrollmentUpdates }.getOrNull()?.collect {
                loadGettingStartedProgram()
            }
        }
    }

    private fun observePlanInvalidations() {
        viewModelScope.launch {
            runCatching { getWorkoutPlansUseCase.planCacheInvalidations }.getOrNull()?.collect {
                loadAllForActiveUser()
            }
        }
    }

    fun refreshAll() {
        loadAllForActiveUser()
    }

    private fun loadAllForActiveUser() {
        loadPlans()
        loadConnectedClients()
        loadAssignments()
        loadGettingStartedProgram()
        loadExercisesCatalog()
    }

    fun loadExercisesCatalog() {
        if (exerciseRepository === NoOpExerciseRepository) return
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoadingExercises = true, exercisesError = null) }
            exerciseRepository.getExercises()
                .onSuccess { exercises ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isLoadingExercises = false,
                            availableExercises = exercises,
                            exercisesError = null
                        )
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoadingExercises = false,
                            exercisesError = NetworkErrorParser.toUserMessage(error)
                        )
                    }
                }
        }
    }

    fun loadGettingStartedProgram() {
        sessionGuard.launchScoped { captured ->
            gettingStartedRepository.getPublishedProgram()
                .onSuccess { program ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    val activeProgram = program?.takeIf { prog ->
                        val enrollment = prog.enrollment
                        enrollment != null && enrollment.addedToWorkoutPlan && !enrollment.isCompleted
                    }
                    _uiState.update { it.copy(activeGettingStartedProgram = activeProgram, gettingStartedError = null) }
                }
        }
    }

    fun startGettingStartedDay(dayNumber: Int, onSessionStarted: (WorkoutSession) -> Unit) {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(startingGettingStartedDay = dayNumber, gettingStartedError = null) }
            gettingStartedRepository.startDaySession(dayNumber)
                .onSuccess { session ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { state ->
                        val updatedProgram = state.activeGettingStartedProgram?.let { prog ->
                            prog.copy(
                                days = prog.days.map { d ->
                                    if (d.dayNumber == dayNumber && d.status != GettingStartedDayStatus.COMPLETED) {
                                        d.copy(status = GettingStartedDayStatus.IN_PROGRESS, workoutSessionId = session.id)
                                    } else d
                                }
                            )
                        }
                        state.copy(
                            startingGettingStartedDay = null,
                            activeGettingStartedProgram = updatedProgram
                        )
                    }
                    onSessionStarted(session)
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            startingGettingStartedDay = null,
                            gettingStartedError = error.localizedMessage ?: "Could not start Getting Started Day $dayNumber."
                        )
                    }
                }
        }
    }

    private fun loadAssignments() {
        sessionGuard.launchScoped { captured ->
            getWorkoutPlansUseCase.getMyAssignments()
                .onSuccess { assignments ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { state ->
                        state.copy(assignments = assignments.filterNot { it.status.isCancelledOrEnded })
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(assignmentError = error.localizedMessage ?: "Could not load assigned plans.") }
                }
            getWorkoutPlansUseCase.getMyCoachAssignments()
                .onSuccess { assignments ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { state ->
                        state.copy(coachAssignments = assignments.filterNot { it.status.isCancelledOrEnded })
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(assignmentError = error.localizedMessage ?: "Could not load coach assignments.") }
                }
        }
    }

    fun requestAssignmentStatusChange(assignment: WorkoutPlanAssignment) {
        if (assignment.status.isCancelledOrEnded) return
        if (assignment.status == WorkoutPlanAssignmentStatus.ACTIVE) {
            _uiState.update { it.copy(assignmentToDeactivate = assignment, assignmentError = null) }
        } else if (assignment.status == WorkoutPlanAssignmentStatus.INACTIVE) {
            updateAssignmentStatus(assignment, WorkoutPlanAssignmentStatus.ACTIVE)
        }
    }

    fun dismissAssignmentDeactivation() {
        _uiState.update { it.copy(assignmentToDeactivate = null) }
    }

    fun confirmAssignmentDeactivation() {
        val assignment = _uiState.value.assignmentToDeactivate ?: return
        _uiState.update { it.copy(assignmentToDeactivate = null) }
        updateAssignmentStatus(assignment, WorkoutPlanAssignmentStatus.INACTIVE)
    }

    private fun updateAssignmentStatus(
        assignment: WorkoutPlanAssignment,
        status: WorkoutPlanAssignmentStatus
    ) {
        if (!connectivityMonitor.isConnected()) {
            _uiState.update { it.copy(assignmentError = "Assignment status changes require an internet connection.") }
            return
        }
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(updatingAssignmentId = assignment.id, assignmentError = null) }
            getWorkoutPlansUseCase.updateAssignmentStatus(assignment.id, status)
                .onSuccess { updated ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { state ->
                        state.copy(
                            assignments = state.assignments.replaceAssignment(updated),
                            coachAssignments = state.coachAssignments.replaceAssignment(updated),
                            updatingAssignmentId = null,
                            assignmentError = null
                        )
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            updatingAssignmentId = null,
                            assignmentError = error.localizedMessage ?: "Could not update assignment status."
                        )
                    }
                }
        }
    }

    private fun List<WorkoutPlanAssignment>.replaceAssignment(
        updated: WorkoutPlanAssignment
    ): List<WorkoutPlanAssignment> = map { if (it.id == updated.id) updated else it }

    private fun loadConnectedClients() {
        sessionGuard.launchScoped { captured ->
            coachClientRepository.loadRelationships().onSuccess { relationships ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                val userId = captured.activeUserId ?: tokenManager.getUserId()
                _uiState.update {
                    it.copy(
                        clients = relationships.filter { rel ->
                            val isConnected = rel.status.equals("ACTIVE", ignoreCase = true) ||
                                rel.status.equals("CONNECTED", ignoreCase = true)
                            val isCoach = userId == null || rel.coachId == userId
                            isConnected && isCoach
                        }
                    )
                }
            }
        }
    }

    fun selectClient(clientId: Long?) {
        if (clientId == null) {
            _uiState.update {
                it.copy(
                    selectedClientId = null,
                    clientPlans = emptyList(),
                    isLoadingClientPlans = false,
                    clientPlanError = null,
                    assignmentMessage = null
                )
            }
            return
        }
        _uiState.update {
            it.copy(selectedClientId = clientId, isLoadingClientPlans = true, clientPlanError = null, assignmentMessage = null)
        }
        sessionGuard.launchScoped { captured ->
            getWorkoutPlansUseCase.getClientPersonalPlans(clientId)
                .onSuccess { plans ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { it.copy(clientPlans = plans, isLoadingClientPlans = false) }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoadingClientPlans = false,
                            clientPlanError = error.localizedMessage ?: "Could not load this client's personal plans."
                        )
                    }
                }
        }
    }

    fun clearAssignmentMessage() {
        _uiState.update { it.copy(assignmentMessage = null) }
    }

    fun assignPlanToClient(clientId: Long?, planId: Long, onSuccess: () -> Unit = {}) {
        assignPlanWithDetailsToClient(
            clientId = clientId,
            planId = planId,
            startDate = null,
            endDate = null,
            notes = null,
            onSuccess = onSuccess
        )
    }

    fun assignPlanWithDetailsToClient(
        clientId: Long?,
        planId: Long,
        startDate: String?,
        endDate: String?,
        notes: String?,
        onSuccess: () -> Unit = {}
    ) {
        if (clientId == null) {
            _uiState.update { it.copy(assignmentMessage = "Select a connected client first.") }
            return
        }
        val client = _uiState.value.clients.firstOrNull {
            it.clientId == clientId || it.id == clientId
        } ?: run {
            _uiState.update { it.copy(assignmentMessage = "Select a connected client first.") }
            return
        }
        val cleanStart = startDate?.trim()?.ifBlank { null }
        val cleanEnd = endDate?.trim()?.ifBlank { null }
        val cleanNotes = notes?.trim()?.ifBlank { null }
        if (cleanStart != null && cleanEnd != null && cleanEnd < cleanStart) {
            _uiState.update { it.copy(assignmentMessage = "Deadline cannot be before the start date.") }
            return
        }
        if (_uiState.value.assigningPlanId != null) return
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(assigningPlanId = planId, assignmentMessage = null) }
            val result = if (cleanStart == null && cleanEnd == null && cleanNotes == null) {
                getWorkoutPlansUseCase.assignPlanToClient(client.id, planId)
            } else {
                getWorkoutPlansUseCase.assignPlanToClient(client.id, planId, cleanStart, cleanEnd, cleanNotes)
            }
            result
                .onSuccess {
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(assigningPlanId = null, assignmentMessage = "Plan assigned to ${client.clientName}.")
                    }
                    loadAssignments()
                    onSuccess()
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            assigningPlanId = null,
                            assignmentMessage = NetworkErrorParser.toUserMessage(error)
                        )
                    }
                }
        }
    }

    fun createCustomWorkoutAndAssignToClient(
        clientId: Long?,
        workoutName: String,
        sessionType: WorkoutSessionType = WorkoutSessionType.FULL_BODY,
        targetDay: WorkoutDayOfWeek = WorkoutDayOfWeek.MONDAY,
        configuredExercises: List<ConfiguredAssignmentExercise>,
        startDate: String? = null,
        endDate: String? = null,
        notes: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        if (clientId == null) {
            _uiState.update { it.copy(assignmentMessage = "Select a connected client first.") }
            return
        }
        val client = _uiState.value.clients.firstOrNull {
            it.clientId == clientId || it.id == clientId
        } ?: run {
            _uiState.update { it.copy(assignmentMessage = "Select a connected client first.") }
            return
        }
        val cleanName = workoutName.trim()
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(assignmentMessage = "Enter a workout name before assigning.") }
            return
        }
        if (configuredExercises.isEmpty()) {
            _uiState.update { it.copy(assignmentMessage = "Add at least one exercise to the workout.") }
            return
        }
        val cleanStart = startDate?.trim()?.ifBlank { null }
        val cleanEnd = endDate?.trim()?.ifBlank { null }
        val cleanNotes = notes?.trim()?.ifBlank { null }
        if (cleanStart != null && cleanEnd != null && cleanEnd < cleanStart) {
            _uiState.update { it.copy(assignmentMessage = "Deadline cannot be before the start date.") }
            return
        }
        if (_uiState.value.assigningPlanId != null) return

        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(assigningPlanId = -1L, assignmentMessage = null) }
            val effectiveSessionType = if (sessionType.isRest) WorkoutSessionType.FULL_BODY else sessionType
            val days = WorkoutDayOfWeek.entries.map { day ->
                day to if (day == targetDay) effectiveSessionType else WorkoutSessionType.REST
            }
            val createdPlan = createWorkoutPlanUseCase(
                name = cleanName,
                splitType = WorkoutSplitType.CUSTOM,
                days = days
            ).getOrElse { error ->
                if (sessionGuard.isCurrentSession(captured)) {
                    _uiState.update {
                        it.copy(
                            assigningPlanId = null,
                            assignmentMessage = NetworkErrorParser.toUserMessage(error)
                        )
                    }
                }
                return@launchScoped
            }

            if (!sessionGuard.isCurrentSession(captured)) return@launchScoped

            var planWithConfiguredDay = createdPlan
            val initialDay = createdPlan.days.firstOrNull { it.dayOfWeek == targetDay }
            if (initialDay == null || initialDay.isRestDay || initialDay.sessionType.isRest) {
                val dayConfigs = WorkoutDayOfWeek.entries.map { day ->
                    val isTarget = day == targetDay
                    WorkoutPlanDayConfiguration(
                        dayOfWeek = day,
                        workoutType = if (isTarget) effectiveSessionType else WorkoutSessionType.REST,
                        name = if (isTarget) cleanName else null,
                        description = if (isTarget) cleanNotes else null,
                        isRestDay = !isTarget,
                        displayOrder = day.order
                    )
                }
                getWorkoutPlansUseCase.updatePlanDays(createdPlan.id, dayConfigs)
                    .onSuccess { updated -> planWithConfiguredDay = updated }
            }

            val targetPlanDay = planWithConfiguredDay.days.firstOrNull { it.dayOfWeek == targetDay }
                ?: planWithConfiguredDay.days.firstOrNull()
            if (targetPlanDay == null) {
                _uiState.update {
                    it.copy(
                        assigningPlanId = null,
                        assignmentMessage = "Created workout plan does not have a configurable day."
                    )
                }
                return@launchScoped
            }

            for ((index, item) in configuredExercises.withIndex()) {
                val addResult = getWorkoutPlansUseCase.addExercise(
                    planId = planWithConfiguredDay.id,
                    dayId = targetPlanDay.id,
                    exerciseId = item.exercise.id,
                    sets = item.sets.coerceAtLeast(1),
                    reps = item.reps?.takeIf { it > 0 },
                    durationSeconds = item.durationSeconds?.takeIf { it > 0 },
                    restSeconds = item.restSeconds.coerceAtLeast(0),
                    displayOrder = index + 1,
                    notes = item.notes.trim().ifBlank { null }
                )
                if (addResult.isFailure) {
                    if (sessionGuard.isCurrentSession(captured)) {
                        val err = addResult.exceptionOrNull()
                        _uiState.update {
                            it.copy(
                                assigningPlanId = null,
                                assignmentMessage = err?.let { e -> NetworkErrorParser.toUserMessage(e) }
                                    ?: "Failed to add ${item.exercise.name} to workout."
                            )
                        }
                    }
                    return@launchScoped
                }
            }

            val assignResult = if (cleanStart == null && cleanEnd == null && cleanNotes == null) {
                getWorkoutPlansUseCase.assignPlanToClient(client.id, planWithConfiguredDay.id)
            } else {
                getWorkoutPlansUseCase.assignPlanToClient(
                    client.id,
                    planWithConfiguredDay.id,
                    cleanStart,
                    cleanEnd,
                    cleanNotes
                )
            }
            assignResult
                .onSuccess {
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            assigningPlanId = null,
                            assignmentMessage = "Plan assigned to ${client.clientName}."
                        )
                    }
                    loadPlans()
                    loadAssignments()
                    onSuccess()
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            assigningPlanId = null,
                            assignmentMessage = NetworkErrorParser.toUserMessage(error)
                        )
                    }
                }
        }
    }

    fun assignPlanToSelectedClient(planId: Long, onSuccess: () -> Unit = {}) {
        val clientId = _uiState.value.selectedClientId ?: run {
            _uiState.update { it.copy(assignmentMessage = "Select a connected client first.") }
            return
        }
        assignPlanToClient(clientId, planId, onSuccess)
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
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, error = null) }
            getWorkoutPlansUseCase()
                .onSuccess { list ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { it.copy(isLoading = false, plans = list) }
                }
                .onFailure { throwable ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
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
