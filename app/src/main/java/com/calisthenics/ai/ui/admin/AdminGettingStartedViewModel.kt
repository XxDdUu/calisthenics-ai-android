package com.calisthenics.ai.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.domain.model.*
import com.calisthenics.ai.domain.repository.ExerciseRepository
import com.calisthenics.ai.domain.repository.GettingStartedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminGettingStartedUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val program: GettingStartedProgram? = null,
    val draft: AdminProgramDraft = createDefaultDraft(),
    val availableExercises: List<Exercise> = emptyList(),
    val selectedDayIndex: Int = 0,
    val isEditing: Boolean = false
) {
    companion object {
        fun createDefaultDraft(): AdminProgramDraft {
            return AdminProgramDraft(
                title = "Getting Started – 3 Days",
                description = "A beginner-friendly 3-day full-body workout program designed to build foundational calisthenics strength.",
                days = listOf(
                    AdminDayDraft(dayNumber = 1, title = "Day 1: Full-Body Foundation", description = "Upper body push & core endurance", exercises = emptyList()),
                    AdminDayDraft(dayNumber = 2, title = "Day 2: Pull & Core Fundamentals", description = "Posterior chain, overhead mobility & grip", exercises = emptyList()),
                    AdminDayDraft(dayNumber = 3, title = "Day 3: Lower Body & Flow", description = "Lower chain strength & full-body balance", exercises = emptyList())
                )
            )
        }
    }
}

@HiltViewModel
class AdminGettingStartedViewModel @Inject constructor(
    private val gettingStartedRepository: GettingStartedRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminGettingStartedUiState())
    val uiState: StateFlow<AdminGettingStartedUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val exResult = exerciseRepository.getExercises()
            val programResult = gettingStartedRepository.getAdminProgram()

            val exercises = exResult.getOrDefault(emptyList())
            val program = programResult.getOrNull()

            _uiState.update { current ->
                val draft = if (program != null) {
                    AdminProgramDraft(
                        title = program.title,
                        description = program.description ?: "",
                        targetAudience = program.targetAudience,
                        days = (1..3).map { dayNum ->
                            val existingDay = program.days.find { it.dayNumber == dayNum }
                            AdminDayDraft(
                                dayNumber = dayNum,
                                title = existingDay?.title ?: "Day $dayNum",
                                description = existingDay?.description ?: "",
                                exercises = existingDay?.exercises?.map { ex ->
                                    AdminExerciseDraft(
                                        exerciseId = ex.exerciseId,
                                        exerciseName = ex.exerciseName,
                                        sets = ex.sets,
                                        reps = ex.reps,
                                        durationSeconds = ex.durationSeconds,
                                        restSeconds = ex.restSeconds,
                                        instructions = ex.instructions ?: "",
                                        displayOrder = ex.displayOrder
                                    )
                                } ?: emptyList()
                            )
                        }
                    )
                } else {
                    AdminGettingStartedUiState.createDefaultDraft()
                }

                current.copy(
                    isLoading = false,
                    program = program,
                    draft = draft,
                    availableExercises = exercises
                )
            }
        }
    }

    fun selectDay(index: Int) {
        if (index in 0..2) {
            _uiState.update { it.copy(selectedDayIndex = index) }
        }
    }

    fun updateProgramTitle(title: String) {
        _uiState.update { it.copy(draft = it.draft.copy(title = title)) }
    }

    fun updateProgramDescription(description: String) {
        _uiState.update { it.copy(draft = it.draft.copy(description = description)) }
    }

    fun updateTargetAudience(audience: TargetAudience) {
        _uiState.update { it.copy(draft = it.draft.copy(targetAudience = audience)) }
    }

    fun updateDayTitle(dayIndex: Int, title: String) {
        _uiState.update { current ->
            val updatedDays = current.draft.days.mapIndexed { idx, day ->
                if (idx == dayIndex) day.copy(title = title) else day
            }
            current.copy(draft = current.draft.copy(days = updatedDays))
        }
    }

    fun updateDayDescription(dayIndex: Int, description: String) {
        _uiState.update { current ->
            val updatedDays = current.draft.days.mapIndexed { idx, day ->
                if (idx == dayIndex) day.copy(description = description) else day
            }
            current.copy(draft = current.draft.copy(days = updatedDays))
        }
    }

    fun addOrUpdateExercise(dayIndex: Int, exercise: AdminExerciseDraft, existingIndex: Int?) {
        _uiState.update { current ->
            val updatedDays = current.draft.days.mapIndexed { idx, day ->
                if (idx == dayIndex) {
                    val updatedExercises = day.exercises.toMutableList()
                    if (existingIndex != null && existingIndex in updatedExercises.indices) {
                        updatedExercises[existingIndex] = exercise.copy(displayOrder = existingIndex)
                    } else {
                        updatedExercises.add(exercise.copy(displayOrder = updatedExercises.size))
                    }
                    day.copy(exercises = updatedExercises)
                } else {
                    day
                }
            }
            current.copy(draft = current.draft.copy(days = updatedDays))
        }
    }

    fun removeExercise(dayIndex: Int, exerciseIndex: Int) {
        _uiState.update { current ->
            val updatedDays = current.draft.days.mapIndexed { idx, day ->
                if (idx == dayIndex) {
                    val updatedExercises = day.exercises.toMutableList()
                    if (exerciseIndex in updatedExercises.indices) {
                        updatedExercises.removeAt(exerciseIndex)
                        // re-index
                        val reindexed = updatedExercises.mapIndexed { i, ex -> ex.copy(displayOrder = i) }
                        day.copy(exercises = reindexed)
                    } else day
                } else day
            }
            current.copy(draft = current.draft.copy(days = updatedDays))
        }
    }

    fun saveProgram() {
        val draft = _uiState.value.draft
        if (draft.title.isBlank()) {
            _uiState.update { it.copy(error = "Program title cannot be blank") }
            return
        }
        for (day in draft.days) {
            if (day.title.isBlank()) {
                _uiState.update { it.copy(error = "Day ${day.dayNumber} title cannot be blank") }
                return
            }
            if (day.exercises.isEmpty()) {
                _uiState.update { it.copy(error = "Day ${day.dayNumber} must have at least one exercise") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            gettingStartedRepository.saveAdminProgram(draft)
                .onSuccess { savedProgram ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            program = savedProgram,
                            successMessage = "Program saved successfully!"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = err.localizedMessage ?: "Failed to save program"
                        )
                    }
                }
        }
    }

    fun publishProgram() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            gettingStartedRepository.publishProgram()
                .onSuccess { program ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            program = program,
                            successMessage = "Program published! Athletes can now view and enroll."
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = err.localizedMessage ?: "Failed to publish program"
                        )
                    }
                }
        }
    }

    fun unpublishProgram() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            gettingStartedRepository.unpublishProgram()
                .onSuccess { program ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            program = program,
                            successMessage = "Program unpublished. Athletes will no longer see it."
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = err.localizedMessage ?: "Failed to unpublish program"
                        )
                    }
                }
        }
    }

    fun deleteProgram() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            gettingStartedRepository.deleteProgram()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            program = null,
                            draft = AdminGettingStartedUiState.createDefaultDraft(),
                            successMessage = "Program deleted."
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = err.localizedMessage ?: "Failed to delete program"
                        )
                    }
                }
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
