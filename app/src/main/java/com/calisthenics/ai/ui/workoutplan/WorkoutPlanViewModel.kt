package com.calisthenics.ai.ui.workoutplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.PlannedExercise
import com.calisthenics.ai.domain.model.PlannedWorkout
import com.calisthenics.ai.domain.model.WorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutPlanSnapshot
import com.calisthenics.ai.domain.model.WorkoutSessionProgress
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.usecase.GetExercisesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject

private const val SELECTED_DATE_KEY = "workout_plan_selected_date"
private const val WEEK_START_KEY = "workout_plan_week_start"

data class WorkoutPlanUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val plan: WorkoutPlan? = null,
    val sessionProgress: List<WorkoutSessionProgress> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val weekStart: LocalDate = startOfWeek(LocalDate.now()),
    val planError: String? = null,
    val exercises: List<Exercise> = emptyList(),
    val isLoadingExercises: Boolean = false,
    val exerciseLoadError: String? = null,
    val editor: WorkoutEditorState? = null,
    val isExercisePickerOpen: Boolean = false
)

data class WorkoutEditorState(
    val workoutId: String? = null,
    val name: String = "",
    val category: String = "Custom",
    val weekday: Int = DayOfWeek.MONDAY.value,
    val estimatedDuration: String = "45",
    val notes: String = "",
    val exercises: List<PlannedExercise> = emptyList(),
    val nameError: String? = null,
    val durationError: String? = null,
    val isDuplicate: Boolean = false
)

data class WeeklyTrainingOverview(
    val sessions: Int,
    val exercises: Int,
    val completedSessions: Int,
    val completionPercent: Int
)

enum class WorkoutPlanTemplate(val label: String, val preview: String) {
    CUSTOM("Custom", "Start with an empty schedule"),
    PUSH_PULL_LEGS("Push / Pull / Legs", "Mon Push · Wed Pull · Fri Legs"),
    UPPER_LOWER("Upper / Lower", "Mon Upper · Tue Lower · Thu Upper · Fri Lower"),
    FULL_BODY("Full Body", "Mon · Wed · Fri Full Body")
}

@HiltViewModel
class WorkoutPlanViewModel @Inject constructor(
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val getExercisesUseCase: GetExercisesUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {
    private val restoredSelectedDate = savedStateHandle.get<String>(SELECTED_DATE_KEY)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: LocalDate.now()
    private val restoredWeekStart = savedStateHandle.get<String>(WEEK_START_KEY)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: startOfWeek(restoredSelectedDate)

    private val _uiState = MutableStateFlow(
        WorkoutPlanUiState(selectedDate = restoredSelectedDate, weekStart = restoredWeekStart)
    )
    val uiState: StateFlow<WorkoutPlanUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            _uiState.update {
                WorkoutPlanUiState(
                    isLoading = false,
                    selectedDate = LocalDate.now(),
                    weekStart = startOfWeek(LocalDate.now()),
                    exercises = it.exercises
                )
            }
        },
        onSessionActive = { loadPlan() }
    )

    init {
        sessionGuard.startObserving()
        loadPlan()
        loadExerciseCatalog()
    }

    fun loadPlan() {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, planError = null) }
            workoutPlanRepository.getSnapshot()
                .onSuccess { snapshot ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    applySnapshot(snapshot)
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            planError = error.localizedMessage ?: "Could not load your workout plan."
                        )
                    }
                }
        }
    }

    fun loadExerciseCatalog() {
        if (_uiState.value.isLoadingExercises) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExercises = true, exerciseLoadError = null) }
            getExercisesUseCase()
                .onSuccess { exercises ->
                    _uiState.update { it.copy(isLoadingExercises = false, exercises = exercises) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingExercises = false,
                            exerciseLoadError = error.localizedMessage ?: "Exercise library is unavailable."
                        )
                    }
                }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, weekStart = startOfWeek(date)) }
        persistCalendarState(date, startOfWeek(date))
    }

    fun showPreviousWeek() = moveWeek(-1)
    fun showNextWeek() = moveWeek(1)

    fun returnToCurrentWeek() = selectDate(LocalDate.now())

    private fun moveWeek(offset: Long) {
        val old = _uiState.value
        val newWeek = old.weekStart.plusWeeks(offset)
        val newDate = old.selectedDate.plusWeeks(offset)
        _uiState.update { it.copy(weekStart = newWeek, selectedDate = newDate) }
        persistCalendarState(newDate, newWeek)
    }

    fun createPlan(name: String, template: WorkoutPlanTemplate = WorkoutPlanTemplate.CUSTOM) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(planError = "Give your workout plan a name.") }
            return
        }
        val plan = WorkoutPlan(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            workouts = templateWorkouts(template)
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, planError = null) }
            // Replacing a plan is only invoked after the UI confirmation. Remove stale progress too.
            workoutPlanRepository.clearPlan()
                .fold(
                    onSuccess = {
                        workoutPlanRepository.savePlan(plan)
                            .onSuccess { applySnapshot(WorkoutPlanSnapshot(plan, emptyList()), false) }
                            .onFailure(::showSaveError)
                    },
                    onFailure = ::showSaveError
                )
        }
    }

    fun deletePlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, planError = null) }
            workoutPlanRepository.clearPlan()
                .onSuccess { applySnapshot(WorkoutPlanSnapshot(null, emptyList()), false) }
                .onFailure(::showSaveError)
        }
    }

    fun openCreateWorkout() {
        _uiState.update {
            it.copy(
                editor = WorkoutEditorState(weekday = it.selectedDate.dayOfWeek.value),
                isExercisePickerOpen = false
            )
        }
    }

    fun openEditWorkout(workout: PlannedWorkout) {
        _uiState.update {
            it.copy(
                editor = WorkoutEditorState(
                    workoutId = workout.id,
                    name = workout.name,
                    category = workout.category,
                    weekday = workout.weekday,
                    estimatedDuration = workout.estimatedDurationMinutes.toString(),
                    notes = workout.notes,
                    exercises = workout.exercises
                ),
                isExercisePickerOpen = false
            )
        }
    }

    fun openDuplicateWorkout(workout: PlannedWorkout) {
        _uiState.update {
            it.copy(
                editor = WorkoutEditorState(
                    name = "${workout.name} copy",
                    category = workout.category,
                    weekday = it.selectedDate.dayOfWeek.value,
                    estimatedDuration = workout.estimatedDurationMinutes.toString(),
                    notes = workout.notes,
                    exercises = workout.exercises.map { exercise -> exercise.copy(id = UUID.randomUUID().toString()) },
                    isDuplicate = true
                ),
                isExercisePickerOpen = false
            )
        }
    }

    fun dismissEditor() = _uiState.update { it.copy(editor = null, isExercisePickerOpen = false) }

    fun updateEditor(transform: (WorkoutEditorState) -> WorkoutEditorState) {
        _uiState.update { state -> state.editor?.let { state.copy(editor = transform(it)) } ?: state }
    }

    fun openExercisePicker() = _uiState.update { it.copy(isExercisePickerOpen = true) }
    fun closeExercisePicker() = _uiState.update { it.copy(isExercisePickerOpen = false) }

    fun addExercisesToDraft(exerciseIds: Set<Long>) {
        val state = _uiState.value
        val editor = state.editor ?: return
        val additions = state.exercises
            .filter { it.id in exerciseIds && editor.exercises.none { draft -> draft.exerciseId == it.id } }
            .map { exercise ->
                PlannedExercise(
                    id = UUID.randomUUID().toString(),
                    exerciseId = exercise.id,
                    name = exercise.name,
                    category = exercise.category.name.lowercase().replaceFirstChar(Char::titlecase),
                    repetitions = if (exercise.category.name == "ISOMETRIC") null else 8,
                    holdDurationSeconds = if (exercise.category.name == "ISOMETRIC") 10 else null
                )
            }
        updateEditor { it.copy(exercises = it.exercises + additions) }
        closeExercisePicker()
    }

    fun updateDraftExercise(updatedExercise: PlannedExercise) {
        updateEditor { editor ->
            editor.copy(exercises = editor.exercises.map {
                if (it.id == updatedExercise.id) updatedExercise else it
            })
        }
    }

    fun removeDraftExercise(exerciseId: String) {
        updateEditor { it.copy(exercises = it.exercises.filterNot { exercise -> exercise.id == exerciseId }) }
    }

    fun moveDraftExercise(exerciseId: String, offset: Int) {
        updateEditor { editor ->
            val fromIndex = editor.exercises.indexOfFirst { it.id == exerciseId }
            val toIndex = fromIndex + offset
            if (fromIndex == -1 || toIndex !in editor.exercises.indices) editor else {
                editor.copy(exercises = editor.exercises.toMutableList().also { list ->
                    val moved = list.removeAt(fromIndex)
                    list.add(toIndex, moved)
                })
            }
        }
    }

    fun saveWorkout() {
        val state = _uiState.value
        val plan = state.plan ?: return
        val editor = state.editor ?: return
        val nameError = if (editor.name.trim().isBlank()) "Workout name is required." else null
        val duration = editor.estimatedDuration.toIntOrNull()
        val durationError = if (duration == null || duration !in 1..360) "Enter a duration from 1 to 360 minutes." else null
        if (nameError != null || durationError != null) {
            _uiState.update {
                it.copy(editor = editor.copy(nameError = nameError, durationError = durationError))
            }
            return
        }

        val workout = PlannedWorkout(
            id = editor.workoutId ?: UUID.randomUUID().toString(),
            weekday = editor.weekday,
            name = editor.name.trim(),
            category = editor.category.trim().ifBlank { "Custom" },
            estimatedDurationMinutes = requireNotNull(duration),
            notes = editor.notes.trim(),
            exercises = editor.exercises
        )
        val newPlan = plan.copy(
            workouts = plan.workouts.filterNot { it.id == workout.id } + workout
        )
        savePlan(newPlan) { current -> current.copy(editor = null, isExercisePickerOpen = false) }
    }

    fun removeWorkout(workoutId: String) {
        val plan = _uiState.value.plan ?: return
        savePlan(plan.copy(workouts = plan.workouts.filterNot { it.id == workoutId }))
    }

    fun toggleExerciseCompleted(workout: PlannedWorkout, exerciseId: String) {
        val state = _uiState.value
        val date = state.selectedDate.toString()
        val current = progressFor(state, workout.id, date)
        val nextIds = current.completedExerciseIds.toMutableSet().also { ids ->
            if (!ids.add(exerciseId)) ids.remove(exerciseId)
        }
        saveProgress(current.copy(completedExerciseIds = nextIds))
    }

    fun markWorkoutCompleted(workout: PlannedWorkout) {
        val state = _uiState.value
        val date = state.selectedDate.toString()
        val current = progressFor(state, workout.id, date)
        saveProgress(
            current.copy(
                completedExerciseIds = current.completedExerciseIds + workout.exercises.map { it.id },
                isWorkoutCompleted = true
            )
        )
    }

    fun selectedWorkout(): PlannedWorkout? {
        val state = _uiState.value
        return state.plan?.workouts?.firstOrNull { it.weekday == state.selectedDate.dayOfWeek.value }
    }

    fun isWorkoutCompleted(workoutId: String, date: LocalDate = _uiState.value.selectedDate): Boolean =
        progressFor(_uiState.value, workoutId, date.toString()).isWorkoutCompleted

    fun isExerciseCompleted(workoutId: String, exerciseId: String): Boolean =
        progressFor(_uiState.value, workoutId, _uiState.value.selectedDate.toString()).completedExerciseIds.contains(exerciseId)

    fun weeklyOverview(): WeeklyTrainingOverview = calculateWeeklyOverview(
        plan = _uiState.value.plan,
        progress = _uiState.value.sessionProgress,
        weekStart = _uiState.value.weekStart
    )

    private fun savePlan(plan: WorkoutPlan, transform: (WorkoutPlanUiState) -> WorkoutPlanUiState = { it }) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, planError = null) }
            workoutPlanRepository.savePlan(plan)
                .onSuccess {
                    _uiState.update { state -> transform(state.copy(isSaving = false, plan = plan)) }
                }
                .onFailure(::showSaveError)
        }
    }

    private fun saveProgress(progress: WorkoutSessionProgress) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, planError = null) }
            workoutPlanRepository.saveSessionProgress(progress)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            sessionProgress = state.sessionProgress.filterNot {
                                it.workoutId == progress.workoutId && it.date == progress.date
                            } + progress
                        )
                    }
                }
                .onFailure(::showSaveError)
        }
    }

    private fun applySnapshot(snapshot: WorkoutPlanSnapshot, clearLoading: Boolean = true) {
        _uiState.update {
            it.copy(
                isLoading = if (clearLoading) false else false,
                isSaving = false,
                plan = snapshot.plan,
                sessionProgress = snapshot.sessionProgress,
                planError = null
            )
        }
    }

    private fun showSaveError(error: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isSaving = false,
                planError = error.localizedMessage ?: "Your changes could not be saved on this device."
            )
        }
    }

    private fun templateWorkouts(template: WorkoutPlanTemplate): List<PlannedWorkout> {
        fun scheduled(weekday: Int, name: String, category: String) = PlannedWorkout(
            id = UUID.randomUUID().toString(),
            weekday = weekday,
            name = name,
            category = category,
            estimatedDurationMinutes = 45
        )
        return when (template) {
            WorkoutPlanTemplate.CUSTOM -> emptyList()
            WorkoutPlanTemplate.PUSH_PULL_LEGS -> listOf(
                scheduled(1, "Push", "Push"),
                scheduled(3, "Pull", "Pull"),
                scheduled(5, "Legs", "Legs")
            )
            WorkoutPlanTemplate.UPPER_LOWER -> listOf(
                scheduled(1, "Upper", "Upper"),
                scheduled(2, "Lower", "Lower"),
                scheduled(4, "Upper", "Upper"),
                scheduled(5, "Lower", "Lower")
            )
            WorkoutPlanTemplate.FULL_BODY -> listOf(
                scheduled(1, "Full Body", "Full Body"),
                scheduled(3, "Full Body", "Full Body"),
                scheduled(5, "Full Body", "Full Body")
            )
        }
    }

    private fun progressFor(state: WorkoutPlanUiState, workoutId: String, date: String): WorkoutSessionProgress =
        state.sessionProgress.firstOrNull { it.workoutId == workoutId && it.date == date }
            ?: WorkoutSessionProgress(workoutId = workoutId, date = date)

    private fun persistCalendarState(selectedDate: LocalDate, weekStart: LocalDate) {
        savedStateHandle[SELECTED_DATE_KEY] = selectedDate.toString()
        savedStateHandle[WEEK_START_KEY] = weekStart.toString()
    }
}

fun calculateWeeklyOverview(
    plan: WorkoutPlan?,
    progress: List<WorkoutSessionProgress>,
    weekStart: LocalDate
): WeeklyTrainingOverview {
    val workouts = plan?.workouts.orEmpty()
    val completed = workouts.count { workout ->
        val date = weekStart.plusDays((workout.weekday - 1).toLong()).toString()
        progress.any { it.workoutId == workout.id && it.date == date && it.isWorkoutCompleted }
    }
    val sessions = workouts.size
    return WeeklyTrainingOverview(
        sessions = sessions,
        exercises = workouts.sumOf { it.exercises.size },
        completedSessions = completed,
        completionPercent = if (sessions == 0) 0 else (completed * 100) / sessions
    )
}

fun startOfWeek(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
