package com.calisthenics.ai.ui.pr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class PersonalRecordsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val prs: List<PersonalRecordDto> = emptyList(),
    val sharedPrs: List<SharedPersonalRecordDto> = emptyList(),
    val shareRecipients: List<ShareRecipientOption> = emptyList(),
    val isLoadingRecipients: Boolean = false,
    val sharingPr: PersonalRecordDto? = null,
    val isSharingPr: Boolean = false,
    val shareError: String? = null,
    val exercises: List<Exercise> = emptyList(),
    val selectedPrTypeFilter: String = "ALL",
    val selectedChartExerciseName: String? = null,
    val selectedChartExerciseId: Long? = null,
    val chartHistory: List<PersonalRecordDto> = emptyList(),
    val chartFilter: String = "All",
    val editingPr: PersonalRecordDto? = null,
    val isFormOpen: Boolean = false,
    val deletingPr: PersonalRecordDto? = null,
    val error: String? = null,
    val successMessage: String? = null
) {
    val filteredPrs: List<PersonalRecordDto>
        get() = if (selectedPrTypeFilter == "ALL") {
            prs
        } else {
            prs.filter { it.recordType.equals(selectedPrTypeFilter, ignoreCase = true) }
        }

    val prsGroupedByExercise: Map<String, List<PersonalRecordDto>>
        get() = filteredPrs.groupBy { it.exerciseName.ifBlank { "Other Exercises" } }

    val chartDataPoints: List<ProgressChartDataPointDto>
        get() = chartHistory.map { pr ->
            ProgressChartDataPointDto(
                date = pr.achievedAt.take(10),
                value = pr.recordValue,
                isPersonalRecord = true,
                sessionId = pr.workoutSessionId
            )
        }
}

@HiltViewModel
class PersonalRecordsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val exerciseRepository: ExerciseRepository,
    private val coachClientRepository: CoachClientRepository? = null,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalRecordsUiState())
    val uiState: StateFlow<PersonalRecordsUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = { _uiState.update { PersonalRecordsUiState(exercises = it.exercises) } },
        onSessionActive = { loadData() }
    )

    init {
        sessionGuard.startObserving()
        loadData()
    }

    fun loadData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        sessionGuard.launchScoped { captured ->
            // Load exercises
            exerciseRepository.getExercises().onSuccess { exList ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                _uiState.update { it.copy(exercises = exList) }
            }

            // Load existing PR shares & active recipients
            analyticsRepository.getClientSharedPRs().onSuccess { shared ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                _uiState.update { it.copy(sharedPrs = shared) }
            }
            coachClientRepository?.loadOverview()?.onSuccess { overview ->
                if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                val recipients = overview.relationships.toShareRecipients(overview.currentUserId)
                _uiState.update { it.copy(shareRecipients = recipients) }
            }

            // Load user PRs
            analyticsRepository.getUserPersonalRecords().fold(
                onSuccess = { prList ->
                    if (!sessionGuard.isCurrentSession(captured)) return@fold
                    val defaultChartExercise = prList.firstOrNull()?.exerciseName
                    val defaultChartExerciseId = prList.firstOrNull()?.exerciseId
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            prs = prList,
                            selectedChartExerciseName = it.selectedChartExerciseName ?: defaultChartExercise,
                            selectedChartExerciseId = it.selectedChartExerciseId ?: defaultChartExerciseId
                        )
                    }
                    loadChartProgression()
                },
                onFailure = { err ->
                    if (!sessionGuard.isCurrentSession(captured)) return@fold
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to load personal records"
                        )
                    }
                }
            )
        }
    }

    fun selectPrTypeFilter(filter: String) {
        _uiState.update { it.copy(selectedPrTypeFilter = filter) }
    }

    fun selectChartExercise(exerciseName: String, exerciseId: Long?) {
        _uiState.update {
            it.copy(
                selectedChartExerciseName = exerciseName,
                selectedChartExerciseId = exerciseId
            )
        }
        loadChartProgression()
    }

    fun setChartFilter(filter: String) {
        _uiState.update { it.copy(chartFilter = filter) }
    }

    private fun loadChartProgression() {
        val exerciseId = _uiState.value.selectedChartExerciseId
        val exerciseName = _uiState.value.selectedChartExerciseName

        if (exerciseId != null && exerciseId > 0) {
            viewModelScope.launch {
                analyticsRepository.getPrHistory(exerciseId).onSuccess { history ->
                    _uiState.update { it.copy(chartHistory = history) }
                }.onFailure {
                    // Fallback to local PRs with same exercise name
                    val local = _uiState.value.prs.filter { it.exerciseName.equals(exerciseName, ignoreCase = true) }
                        .sortedBy { it.achievedAt }
                    _uiState.update { it.copy(chartHistory = local) }
                }
            }
        } else if (!exerciseName.isNullOrBlank()) {
            val local = _uiState.value.prs.filter { it.exerciseName.equals(exerciseName, ignoreCase = true) }
                .sortedBy { it.achievedAt }
            _uiState.update { it.copy(chartHistory = local) }
        }
    }

    fun openAddPrForm(preselectedExerciseId: Long? = null, preselectedExerciseName: String? = null) {
        val exercise = preselectedExerciseId?.let { id -> _uiState.value.exercises.find { it.id == id } }
        val name = preselectedExerciseName ?: exercise?.name
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPr = null
            )
        }
    }

    fun openEditPrForm(pr: PersonalRecordDto) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingPr = pr
            )
        }
    }

    fun closeForm() {
        _uiState.update { it.copy(isFormOpen = false, editingPr = null) }
    }

    fun savePr(
        exerciseId: Long?,
        exerciseName: String,
        prType: String,
        value: Double,
        unit: String,
        achievedAt: String,
        notes: String?
    ) {
        if (value <= 0) {
            _uiState.update { it.copy(error = "Value must be greater than 0") }
            return
        }
        if (exerciseId == null && exerciseName.isBlank()) {
            _uiState.update { it.copy(error = "Please select or enter an exercise") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val editing = _uiState.value.editingPr
            if (editing != null) {
                // Update
                val request = UpdatePrRequestDto(
                    prType = prType,
                    value = value,
                    unit = unit,
                    achievedAt = achievedAt,
                    notes = notes
                )
                analyticsRepository.updatePersonalRecord(editing.id, request).fold(
                    onSuccess = { updated ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isFormOpen = false,
                                editingPr = null,
                                successMessage = "PR updated successfully!"
                            )
                        }
                        loadData()
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = err.message ?: "Failed to update PR"
                            )
                        }
                    }
                )
            } else {
                // Create
                val request = CreatePrRequestDto(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName.ifBlank { null },
                    prType = prType,
                    value = value,
                    unit = unit,
                    achievedAt = achievedAt,
                    notes = notes
                )
                analyticsRepository.createPersonalRecord(request).fold(
                    onSuccess = { created ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isFormOpen = false,
                                editingPr = null,
                                selectedChartExerciseName = created.exerciseName,
                                selectedChartExerciseId = created.exerciseId,
                                successMessage = "New PR created!"
                            )
                        }
                        loadData()
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = err.message ?: "Failed to save PR"
                            )
                        }
                    }
                )
            }
        }
    }

    fun promptDelete(pr: PersonalRecordDto) {
        _uiState.update { it.copy(deletingPr = pr) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deletingPr = null) }
    }

    fun confirmDelete() {
        val pr = _uiState.value.deletingPr ?: return
        _uiState.update { it.copy(deletingPr = null, isLoading = true) }
        viewModelScope.launch {
            analyticsRepository.deletePersonalRecord(pr.id).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "PR deleted successfully"
                        )
                    }
                    loadData()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to delete PR"
                        )
                    }
                }
            )
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
        viewModelScope.launch {
            analyticsRepository.getClientSharedPRs().onSuccess { shared ->
                _uiState.update { it.copy(sharedPrs = shared) }
            }
            coachClientRepository?.loadOverview()
                ?.onSuccess { overview ->
                    val recipients = overview.relationships.toShareRecipients(overview.currentUserId)
                    _uiState.update {
                        it.copy(
                            shareRecipients = recipients,
                            isLoadingRecipients = false
                        )
                    }
                }
                ?.onFailure { err ->
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

    fun shareWithCoach(
        prId: Long,
        recipientId: Long? = null,
        clientMessageId: String? = UUID.randomUUID().toString()
    ) {
        if (_uiState.value.isSharingPr) return
        _uiState.update { it.copy(isSharingPr = true, shareError = null, error = null) }
        viewModelScope.launch {
            analyticsRepository.sharePersonalRecord(prId, recipientId, clientMessageId).fold(
                onSuccess = { shared ->
                    val recipientName = _uiState.value.shareRecipients
                        .firstOrNull { it.userId == recipientId }?.name
                        ?: shared.coachName.ifBlank { "your coach" }
                    _uiState.update {
                        it.copy(
                            isSharingPr = false,
                            sharingPr = null,
                            sharedPrs = (it.sharedPrs + shared).distinctBy { s -> s.id },
                            successMessage = "PR shared with $recipientName and sent to chat!"
                        )
                    }
                },
                onFailure = { err ->
                    val msg = err.message ?: "Could not share PR"
                    _uiState.update {
                        it.copy(
                            isSharingPr = false,
                            shareError = msg,
                            error = if (it.sharingPr == null) msg else null
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null, shareError = null) }
    }
}

