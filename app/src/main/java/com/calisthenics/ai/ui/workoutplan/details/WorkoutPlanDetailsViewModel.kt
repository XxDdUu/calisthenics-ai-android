package com.calisthenics.ai.ui.workoutplan.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.usecase.AddExerciseToPlanDayUseCase
import com.calisthenics.ai.domain.usecase.GetExercisesUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlanDayExercisesUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlanDetailsUseCase
import com.calisthenics.ai.domain.usecase.RemoveExerciseFromPlanDayUseCase
import com.calisthenics.ai.domain.usecase.UpdatePlanDayExerciseUseCase
import com.calisthenics.ai.domain.usecase.UpdatePlanDayExercisesUseCase
import com.calisthenics.ai.domain.usecase.UpdateWorkoutPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExercisePrescriptionType {
    REPETITIONS,
    DURATION
}

data class WorkoutPlanDetailsUiState(
    val planId: Long = 0L,
    val isLoading: Boolean = false,
    val plan: UserWorkoutPlan? = null,
    val selectedDayOfWeek: WorkoutDayOfWeek = WorkoutDayOfWeek.MONDAY,
    val dayExercises: Map<Long, List<UserWorkoutPlanExercise>> = emptyMap(),
    val error: String? = null,
    val isReorderingExercises: Boolean = false,
    // Add / Edit Exercise Sheet
    val isAddExerciseSheetOpen: Boolean = false,
    val assignmentToEdit: UserWorkoutPlanExercise? = null,
    val availableExercises: List<Exercise> = emptyList(),
    val isLoadingCatalog: Boolean = false,
    val catalogError: String? = null,
    val searchQuery: String = "",
    val selectedCategory: ExerciseCategory? = null,
    val selectedExerciseForAdd: Exercise? = null,
    // Configuration Fields
    val prescriptionType: ExercisePrescriptionType = ExercisePrescriptionType.REPETITIONS,
    val setsInput: String = "3",
    val repsInput: String = "8",
    val holdDurationInput: String = "",
    val restSecondsInput: String = "60",
    val notesInput: String = "",
    val validationError: String? = null,
    val isSubmittingExercise: Boolean = false,
    // Delete Confirmation
    val exerciseToDelete: UserWorkoutPlanExercise? = null,
    val isDeletingExercise: Boolean = false,
    // Edit Plan
    val isEditPlanDialogOpen: Boolean = false,
    val editPlanName: String = "",
    val isUpdatingPlan: Boolean = false,
    val editPlanError: String? = null
) {
    val selectedDay: UserWorkoutPlanDay?
        get() = plan?.days?.firstOrNull { it.dayOfWeek == selectedDayOfWeek }

    val currentDayExercises: List<UserWorkoutPlanExercise>
        get() {
            val dayId = selectedDay?.id ?: return emptyList()
            return dayExercises[dayId].orEmpty()
        }

    val filteredCatalogExercises: List<Exercise>
        get() = availableExercises.filter { exercise ->
            val matchesQuery = searchQuery.isBlank() ||
                    exercise.name.contains(searchQuery, ignoreCase = true) ||
                    exercise.targetMuscles.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || exercise.category == selectedCategory
            matchesQuery && matchesCategory
        }
}

@HiltViewModel
class WorkoutPlanDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWorkoutPlanDetailsUseCase: GetWorkoutPlanDetailsUseCase,
    private val getWorkoutPlanDayExercisesUseCase: GetWorkoutPlanDayExercisesUseCase,
    private val addExerciseToPlanDayUseCase: AddExerciseToPlanDayUseCase,
    private val updatePlanDayExerciseUseCase: UpdatePlanDayExerciseUseCase,
    private val updatePlanDayExercisesUseCase: UpdatePlanDayExercisesUseCase,
    private val removeExerciseFromPlanDayUseCase: RemoveExerciseFromPlanDayUseCase,
    private val updateWorkoutPlanUseCase: UpdateWorkoutPlanUseCase,
    private val getExercisesUseCase: GetExercisesUseCase
) : ViewModel() {

    private val planId: Long = savedStateHandle.get<String>("planId")?.toLongOrNull()
        ?: savedStateHandle.get<Long>("planId")
        ?: 0L
    private val initialDay = savedStateHandle.get<String>("dayOfWeek")
        ?.let(WorkoutDayOfWeek::fromName)
        ?: WorkoutDayOfWeek.MONDAY

    private val _uiState = MutableStateFlow(
        WorkoutPlanDetailsUiState(planId = planId, selectedDayOfWeek = initialDay)
    )
    val uiState: StateFlow<WorkoutPlanDetailsUiState> = _uiState.asStateFlow()

    init {
        loadPlanDetails()
        loadExerciseCatalog()
    }

    fun loadPlanDetails() {
        if (planId <= 0L) {
            _uiState.update { it.copy(error = "Invalid workout plan ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getWorkoutPlanDetailsUseCase(planId)
                .onSuccess { plan ->
                    val exercisesMap = plan.days.associate { day ->
                        day.id to day.exercises
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            plan = plan,
                            dayExercises = exercisesMap
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Failed to retrieve workout plan details"
                        )
                    }
                }
        }
    }

    fun selectDay(dayOfWeek: WorkoutDayOfWeek) {
        _uiState.update { it.copy(selectedDayOfWeek = dayOfWeek) }
        val day = _uiState.value.plan?.days?.firstOrNull { it.dayOfWeek == dayOfWeek }
        if (day != null && day.id > 0) {
            refreshDayExercises(day.id)
        }
    }

    private fun refreshDayExercises(dayId: Long) {
        viewModelScope.launch {
            getWorkoutPlanDayExercisesUseCase(planId, dayId)
                .onSuccess { exercises ->
                    _uiState.update { state ->
                        val updatedMap = state.dayExercises.toMutableMap()
                        updatedMap[dayId] = exercises
                        state.copy(dayExercises = updatedMap)
                    }
                }
        }
    }

    fun loadExerciseCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCatalog = true, catalogError = null) }
            getExercisesUseCase()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoadingCatalog = false, availableExercises = list) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingCatalog = false,
                            catalogError = throwable.localizedMessage ?: "Could not load exercise catalog"
                        )
                    }
                }
        }
    }

    // --- Add / Edit Exercise Bottom Sheet Actions ---

    fun openAddExerciseSheet() {
        val selectedDay = _uiState.value.selectedDay ?: return
        if (selectedDay.sessionType.isRest) {
            _uiState.update { it.copy(error = "Cannot add exercises to a Rest day.") }
            return
        }

        _uiState.update {
            it.copy(
                isAddExerciseSheetOpen = true,
                assignmentToEdit = null,
                selectedExerciseForAdd = null,
                searchQuery = "",
                selectedCategory = null,
                prescriptionType = ExercisePrescriptionType.REPETITIONS,
                setsInput = "3",
                repsInput = "8",
                holdDurationInput = "",
                restSecondsInput = "60",
                notesInput = "",
                validationError = null
            )
        }
    }

    fun openEditExerciseSheet(assignment: UserWorkoutPlanExercise) {
        val isDuration = assignment.durationSeconds != null && assignment.durationSeconds > 0
        val pType = if (isDuration) ExercisePrescriptionType.DURATION else ExercisePrescriptionType.REPETITIONS
        _uiState.update {
            it.copy(
                isAddExerciseSheetOpen = true,
                assignmentToEdit = assignment,
                selectedExerciseForAdd = assignment.exercise,
                searchQuery = "",
                selectedCategory = null,
                prescriptionType = pType,
                setsInput = assignment.sets.toString(),
                repsInput = assignment.reps?.toString().orEmpty(),
                holdDurationInput = assignment.durationSeconds?.toString().orEmpty(),
                restSecondsInput = assignment.restSeconds.toString(),
                notesInput = assignment.notes.orEmpty(),
                validationError = null
            )
        }
    }

    fun setPrescriptionType(type: ExercisePrescriptionType) {
        _uiState.update { current ->
            if (current.prescriptionType == type) return@update current
            if (type == ExercisePrescriptionType.DURATION) {
                val duration = current.holdDurationInput.ifBlank { "15" }
                current.copy(
                    prescriptionType = type,
                    holdDurationInput = duration,
                    repsInput = "",
                    validationError = null
                )
            } else {
                val reps = current.repsInput.ifBlank { "8" }
                current.copy(
                    prescriptionType = type,
                    repsInput = reps,
                    holdDurationInput = "",
                    validationError = null
                )
            }
        }
    }

    fun closeAddExerciseSheet() {
        _uiState.update {
            it.copy(
                isAddExerciseSheetOpen = false,
                assignmentToEdit = null,
                selectedExerciseForAdd = null,
                validationError = null
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategoryFilterSelected(category: ExerciseCategory?) {
        _uiState.update {
            it.copy(selectedCategory = if (it.selectedCategory == category) null else category)
        }
    }

    fun onExerciseSelectedForAdd(exercise: Exercise) {
        val isHold = exercise.category == ExerciseCategory.ISOMETRIC ||
                exercise.category == ExerciseCategory.SKILL
        val pType = if (isHold) ExercisePrescriptionType.DURATION else ExercisePrescriptionType.REPETITIONS
        val defaultHold = if (isHold) "15" else ""
        val defaultReps = if (isHold) "" else "8"

        _uiState.update {
            it.copy(
                selectedExerciseForAdd = exercise,
                prescriptionType = pType,
                holdDurationInput = defaultHold,
                repsInput = defaultReps,
                validationError = null
            )
        }
    }

    fun onBackToCatalogSelection() {
        _uiState.update { it.copy(selectedExerciseForAdd = null, validationError = null) }
    }

    fun updateSets(input: String) {
        _uiState.update { it.copy(setsInput = input, validationError = null) }
    }

    fun updateReps(input: String) {
        _uiState.update { it.copy(repsInput = input, validationError = null) }
    }

    fun updateHoldDuration(input: String) {
        _uiState.update { it.copy(holdDurationInput = input, validationError = null) }
    }

    fun updateRestSeconds(input: String) {
        _uiState.update { it.copy(restSecondsInput = input, validationError = null) }
    }

    fun updateNotes(input: String) {
        _uiState.update { it.copy(notesInput = input) }
    }

    fun moveExerciseUp(assignment: UserWorkoutPlanExercise) {
        val day = _uiState.value.selectedDay ?: return
        val currentList = _uiState.value.currentDayExercises.toMutableList()
        val index = currentList.indexOfFirst { it.id == assignment.id }
        if (index <= 0) return

        val item = currentList.removeAt(index)
        currentList.add(index - 1, item)
        val reordered = currentList.mapIndexed { idx, ex ->
            ex.copy(displayOrder = idx + 1)
        }
        updateDayExercisesBatch(day.id, reordered)
    }

    fun moveExerciseDown(assignment: UserWorkoutPlanExercise) {
        val day = _uiState.value.selectedDay ?: return
        val currentList = _uiState.value.currentDayExercises.toMutableList()
        val index = currentList.indexOfFirst { it.id == assignment.id }
        if (index < 0 || index >= currentList.size - 1) return

        val item = currentList.removeAt(index)
        currentList.add(index + 1, item)
        val reordered = currentList.mapIndexed { idx, ex ->
            ex.copy(displayOrder = idx + 1)
        }
        updateDayExercisesBatch(day.id, reordered)
    }

    private fun updateDayExercisesBatch(dayId: Long, reorderedExercises: List<UserWorkoutPlanExercise>) {
        _uiState.update { state ->
            val updatedMap = state.dayExercises.toMutableMap()
            updatedMap[dayId] = reorderedExercises
            state.copy(dayExercises = updatedMap, isReorderingExercises = true)
        }

        viewModelScope.launch {
            updatePlanDayExercisesUseCase(planId, dayId, reorderedExercises)
                .onSuccess { syncedExercises ->
                    _uiState.update { state ->
                        val updatedMap = state.dayExercises.toMutableMap()
                        updatedMap[dayId] = syncedExercises
                        state.copy(dayExercises = updatedMap, isReorderingExercises = false)
                    }
                }
                .onFailure { throwable ->
                    refreshDayExercises(dayId)
                    _uiState.update {
                        it.copy(
                            isReorderingExercises = false,
                            error = throwable.localizedMessage ?: "Failed to reorder exercises"
                        )
                    }
                }
        }
    }

    fun submitExerciseAssignment() {
        val state = _uiState.value
        val day = state.selectedDay ?: return
        val dayId = day.id
        val exercise = state.selectedExerciseForAdd ?: run {
            _uiState.update { it.copy(validationError = "Please select an exercise.") }
            return
        }

        val sets = state.setsInput.toIntOrNull()
        if (sets == null || sets <= 0) {
            _uiState.update { it.copy(validationError = "Sets must be greater than zero.") }
            return
        }

        val isDuration = state.prescriptionType == ExercisePrescriptionType.DURATION
        val reps = if (isDuration) null else state.repsInput.toIntOrNull()?.takeIf { it > 0 }
        val durationSeconds = if (isDuration) state.holdDurationInput.toIntOrNull()?.takeIf { it > 0 } else null

        if (!isDuration && reps == null) {
            _uiState.update { it.copy(validationError = "Please specify a positive number of repetitions.") }
            return
        }
        if (isDuration && durationSeconds == null) {
            _uiState.update { it.copy(validationError = "Please specify hold duration in seconds.") }
            return
        }

        val restSeconds = state.restSecondsInput.toIntOrNull()
        if (restSeconds == null || restSeconds < 0) {
            _uiState.update { it.copy(validationError = "Rest seconds cannot be negative.") }
            return
        }

        val assignmentToEdit = state.assignmentToEdit

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingExercise = true, validationError = null) }

            if (assignmentToEdit != null) {
                // Edit existing assignment
                updatePlanDayExerciseUseCase(
                    planId = planId,
                    dayId = dayId,
                    assignmentId = assignmentToEdit.id,
                    exerciseId = exercise.id,
                    sets = sets,
                    reps = reps,
                    durationSeconds = durationSeconds,
                    restSeconds = restSeconds,
                    displayOrder = assignmentToEdit.displayOrder,
                    notes = state.notesInput.trim()
                ).onSuccess { updatedAssignment ->
                    _uiState.update { s ->
                        val updatedList = s.currentDayExercises.map {
                            if (it.id == updatedAssignment.id) updatedAssignment else it
                        }
                        val map = s.dayExercises.toMutableMap()
                        map[dayId] = updatedList
                        s.copy(
                            isSubmittingExercise = false,
                            isAddExerciseSheetOpen = false,
                            assignmentToEdit = null,
                            selectedExerciseForAdd = null,
                            dayExercises = map
                        )
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmittingExercise = false,
                            validationError = throwable.localizedMessage ?: "Failed to update exercise"
                        )
                    }
                }
            } else {
                // Add new assignment
                val nextOrder = (state.currentDayExercises.maxOfOrNull { it.displayOrder } ?: 0) + 1
                addExerciseToPlanDayUseCase(
                    planId = planId,
                    dayId = dayId,
                    exerciseId = exercise.id,
                    sets = sets,
                    reps = reps,
                    durationSeconds = durationSeconds,
                    restSeconds = restSeconds,
                    displayOrder = nextOrder,
                    notes = state.notesInput.trim()
                ).onSuccess { createdAssignment ->
                    _uiState.update { s ->
                        val updatedList = s.currentDayExercises + createdAssignment
                        val map = s.dayExercises.toMutableMap()
                        map[dayId] = updatedList
                        s.copy(
                            isSubmittingExercise = false,
                            isAddExerciseSheetOpen = false,
                            selectedExerciseForAdd = null,
                            dayExercises = map
                        )
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmittingExercise = false,
                            validationError = throwable.localizedMessage ?: "Failed to add exercise"
                        )
                    }
                }
            }
        }
    }

    // --- Delete Exercise Actions ---

    fun confirmDeleteExercise(assignment: UserWorkoutPlanExercise) {
        _uiState.update { it.copy(exerciseToDelete = assignment) }
    }

    fun dismissDeleteExerciseDialog() {
        _uiState.update { it.copy(exerciseToDelete = null) }
    }

    fun executeDeleteExercise() {
        val assignment = _uiState.value.exerciseToDelete ?: return
        val day = _uiState.value.selectedDay ?: return
        val dayId = day.id

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingExercise = true) }
            removeExerciseFromPlanDayUseCase(planId, dayId, assignment.id)
                .onSuccess {
                    _uiState.update { s ->
                        val updatedList = s.currentDayExercises.filterNot { it.id == assignment.id }
                        val map = s.dayExercises.toMutableMap()
                        map[dayId] = updatedList
                        s.copy(
                            isDeletingExercise = false,
                            exerciseToDelete = null,
                            dayExercises = map
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeletingExercise = false,
                            exerciseToDelete = null,
                            error = throwable.localizedMessage ?: "Failed to delete exercise"
                        )
                    }
                }
        }
    }

    // --- Edit Plan Actions ---

    fun openEditPlanDialog() {
        val plan = _uiState.value.plan ?: return
        _uiState.update {
            it.copy(
                isEditPlanDialogOpen = true,
                editPlanName = plan.name,
                editPlanError = null
            )
        }
    }

    fun closeEditPlanDialog() {
        _uiState.update { it.copy(isEditPlanDialogOpen = false, editPlanError = null) }
    }

    fun updateEditPlanName(name: String) {
        _uiState.update { it.copy(editPlanName = name, editPlanError = null) }
    }

    fun submitUpdatePlan() {
        val plan = _uiState.value.plan ?: return
        val newName = _uiState.value.editPlanName.trim()
        if (newName.isBlank()) {
            _uiState.update { it.copy(editPlanError = "Plan name cannot be empty") }
            return
        }

        val days = plan.days.map { it.dayOfWeek to it.sessionType }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPlan = true, editPlanError = null) }
            updateWorkoutPlanUseCase(plan.id, newName, plan.splitType, days)
                .onSuccess { updatedPlan ->
                    _uiState.update {
                        it.copy(
                            isUpdatingPlan = false,
                            isEditPlanDialogOpen = false,
                            plan = updatedPlan
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isUpdatingPlan = false,
                            editPlanError = throwable.localizedMessage ?: "Failed to update plan"
                        )
                    }
                }
        }
    }
}
