package com.calisthenics.ai.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.QuotaEntitlement
import com.calisthenics.ai.domain.repository.AnalysisRepository
import com.calisthenics.ai.domain.usecase.GetExercisesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoreVideoExercise(
    val code: String,
    val displayName: String,
    val subtitle: String,
    val cameraHint: String,
    val exerciseId: Long = 0L
)

enum class UpgradeDialogReason {
    QUOTA_BANNER,
    LIVE_ANALYSIS_LOCKED
}

enum class AnalysisSelectionTab {
    ALL,
    EXERCISES,
    HISTORY
}

data class ExerciseSelectionUiState(
    val isLoading: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    val coreVideoExercises: List<CoreVideoExercise> = defaultCoreVideoExercises(),
    val parentExercises: List<ParentExerciseGroup> = emptyList(),
    val quota: QuotaEntitlement? = null,
    val isLoadingQuota: Boolean = false,
    val quotaError: String? = null,
    val videoHistory: List<AnalysisResult> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val historyError: String? = null,
    val selectedTab: AnalysisSelectionTab = AnalysisSelectionTab.ALL,
    val showUpgradeDialog: Boolean = false,
    val upgradeDialogReason: UpgradeDialogReason = UpgradeDialogReason.QUOTA_BANNER,
    val isUpdatingMembership: Boolean = false,
    val membershipError: String? = null,
    val error: String? = null
)

data class ParentExerciseGroup(
    val name: String,
    val code: String,
    val count: Int
)

private fun defaultCoreVideoExercises(): List<CoreVideoExercise> = listOf(
    CoreVideoExercise(
        code = "SQUAT",
        displayName = "Squat",
        subtitle = "Depth, knee tracking & torso angle breakdown",
        cameraHint = "Side or 45° view • Full body head-to-toe"
    ),
    CoreVideoExercise(
        code = "PUSH_UP",
        displayName = "Push-up",
        subtitle = "Elbow flexion, lockout & plank body alignment",
        cameraHint = "Side-on profile at floor height"
    ),
    CoreVideoExercise(
        code = "PULL_UP",
        displayName = "Pull-up",
        subtitle = "Chin-over-bar ROM, symmetry & rep control",
        cameraHint = "Front or 45° view showing overhead bar"
    )
)

@HiltViewModel
class ExerciseSelectionViewModel @Inject constructor(
    private val getExercisesUseCase: GetExercisesUseCase,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseSelectionUiState())
    val uiState: StateFlow<ExerciseSelectionUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    fun loadExercises() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isLoadingQuota = true,
                    isLoadingHistory = true,
                    error = null,
                    quotaError = null,
                    historyError = null
                )
            }
            coroutineScope {
                val exercisesDeferred = async { getExercisesUseCase() }
                val quotaDeferred = async { analysisRepository.getVideoAnalysisQuota() }
                val historyDeferred = async { analysisRepository.getVideoAnalysisHistory() }

                val exercisesResult = exercisesDeferred.await()
                val quotaResult = quotaDeferred.await()
                val historyResult = historyDeferred.await()

                val quota = quotaResult.getOrNull()
                val quotaErr = quotaResult.exceptionOrNull()?.let { NetworkErrorParser.toUserMessage(it) }
                val history = historyResult.getOrNull()?.sortedByDescending { it.createdAt } ?: _uiState.value.videoHistory
                val historyErr = historyResult.exceptionOrNull()?.let { NetworkErrorParser.toUserMessage(it) }

                exercisesResult
                    .onSuccess { exerciseList ->
                        val coreExercises = mapCoreVideoExercises(exerciseList)
                        val groups = exerciseList
                            .filterNot { isCoreVideoExercise(it) }
                            .map { exercise -> extractParentExercise(exercise.name, exercise.code) }
                            .distinctBy { it.code }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoadingQuota = false,
                                isLoadingHistory = false,
                                exercises = exerciseList,
                                coreVideoExercises = coreExercises,
                                parentExercises = groups,
                                quota = quota ?: it.quota,
                                quotaError = quotaErr,
                                videoHistory = history,
                                historyError = historyErr,
                                error = null
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoadingQuota = false,
                                isLoadingHistory = false,
                                quota = quota ?: it.quota,
                                quotaError = quotaErr,
                                videoHistory = history,
                                historyError = historyErr,
                                error = NetworkErrorParser.toUserMessage(throwable)
                            )
                        }
                    }
            }
        }
    }

    fun refreshQuotaAndHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuota = true, isLoadingHistory = true) }
            coroutineScope {
                val quotaDeferred = async { analysisRepository.getVideoAnalysisQuota() }
                val historyDeferred = async { analysisRepository.getVideoAnalysisHistory() }

                val quotaResult = quotaDeferred.await()
                val historyResult = historyDeferred.await()

                _uiState.update { state ->
                    state.copy(
                        isLoadingQuota = false,
                        isLoadingHistory = false,
                        quota = quotaResult.getOrNull() ?: state.quota,
                        quotaError = quotaResult.exceptionOrNull()?.let { NetworkErrorParser.toUserMessage(it) },
                        videoHistory = historyResult.getOrNull()?.sortedByDescending { it.createdAt } ?: state.videoHistory,
                        historyError = historyResult.exceptionOrNull()?.let { NetworkErrorParser.toUserMessage(it) }
                    )
                }
            }
        }
    }

    fun selectTab(tab: AnalysisSelectionTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openUpgradeDialog(reason: UpgradeDialogReason = UpgradeDialogReason.QUOTA_BANNER) {
        _uiState.update {
            it.copy(
                showUpgradeDialog = true,
                upgradeDialogReason = reason,
                membershipError = null
            )
        }
    }

    fun dismissUpgradeDialog() {
        _uiState.update {
            it.copy(
                showUpgradeDialog = false,
                membershipError = null
            )
        }
    }

    fun updateMembershipTier(tier: String) {
        val normalizedTier = tier.trim().uppercase()
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingMembership = true, membershipError = null) }
            analysisRepository.updateMembershipTier(normalizedTier)
                .onSuccess { updatedQuota ->
                    _uiState.update {
                        it.copy(
                            isUpdatingMembership = false,
                            quota = updatedQuota,
                            quotaError = null,
                            showUpgradeDialog = false,
                            membershipError = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isUpdatingMembership = false,
                            membershipError = NetworkErrorParser.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    private fun mapCoreVideoExercises(exercises: List<Exercise>): List<CoreVideoExercise> {
        return defaultCoreVideoExercises().map { core ->
            val matched = findCatalogMatch(exercises, core.code)
            if (matched != null) {
                core.copy(
                    exerciseId = matched.id,
                    displayName = matched.name.takeIf { it.isNotBlank() } ?: core.displayName
                )
            } else {
                core
            }
        }
    }

    private fun findCatalogMatch(exercises: List<Exercise>, coreCode: String): Exercise? {
        val targetNorm = normalizeToken(coreCode)
        return exercises.firstOrNull { normalizeToken(it.code) == targetNorm }
            ?: exercises.firstOrNull { normalizeToken(it.name) == targetNorm }
            ?: when (coreCode) {
                "SQUAT" -> exercises.firstOrNull {
                    it.code.contains("SQUAT", ignoreCase = true) || it.name.contains("Squat", ignoreCase = true)
                }
                "PUSH_UP" -> exercises.firstOrNull {
                    normalizeToken(it.code).contains("PUSHUP") || normalizeToken(it.name).contains("PUSHUP")
                }
                "PULL_UP" -> exercises.firstOrNull {
                    normalizeToken(it.code).contains("PULLUP") || normalizeToken(it.name).contains("PULLUP")
                }
                else -> null
            }
    }

    private fun isCoreVideoExercise(exercise: Exercise): Boolean {
        val normCode = normalizeToken(exercise.code)
        val normName = normalizeToken(exercise.name)
        return normCode == "SQUAT" || normName == "SQUAT" ||
            normCode == "PUSHUP" || normName == "PUSHUP" ||
            normCode == "PULLUP" || normName == "PULLUP"
    }

    private fun normalizeToken(raw: String): String {
        return raw.uppercase().replace("_", "").replace("-", "").replace(" ", "").trim()
    }

    private fun extractParentExercise(name: String, code: String): ParentExerciseGroup {
        return when {
            code.contains("PLANCHE", ignoreCase = true) || name.contains("Planche", ignoreCase = true) ->
                ParentExerciseGroup("Planche", "PLANCHE", 4)
            code.contains("LEVER", ignoreCase = true) || name.contains("Lever", ignoreCase = true) ->
                ParentExerciseGroup("Front Lever", "FRONT_LEVER", 1)
            code.contains("HANDSTAND", ignoreCase = true) || name.contains("Handstand", ignoreCase = true) ->
                ParentExerciseGroup("Handstand", "HANDSTAND", 1)
            code.contains("DIP", ignoreCase = true) || name.contains("Dip", ignoreCase = true) ->
                ParentExerciseGroup("Dips", "DIPS", 1)
            code.contains("MUSCLE_UP", ignoreCase = true) || name.contains("Muscle-Up", ignoreCase = true) ->
                ParentExerciseGroup("Muscle-Up", "MUSCLE_UP", 1)
            else -> ParentExerciseGroup(name, code, 1)
        }
    }
}
