package com.calisthenics.ai.ui.workoutplan.ai

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.R
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.GenderSessionStore
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.domain.model.AiWorkoutPlanPreview
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.repository.ProfileRepository
import com.calisthenics.ai.domain.usecase.CommitAiWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.GenerateAiWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlansUseCase
import com.calisthenics.ai.domain.usecase.ModifyAiWorkoutPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject

enum class GenerationStatus {
    IDLE, SUBMITTING, THINKING, ANALYZING_PROFILE, GENERATING_PLAN, COMPLETED, ERROR, CANCELLED
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole { USER, AI }

enum class StartDateOption {
    NEXT_MONDAY,
    TOMORROW,
    TODAY;

    fun toLocalDate(): LocalDate {
        val today = LocalDate.now()
        return when (this) {
            NEXT_MONDAY -> {
                if (today.dayOfWeek == DayOfWeek.MONDAY) {
                    today.plusWeeks(1)
                } else {
                    today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                }
            }
            TOMORROW -> today.plusDays(1)
            TODAY -> today
        }
    }
}

enum class PlannerStep {
    INPUT,
    GENERATING,
    PREVIEW,
    COMMITTING,
    SUCCESS
}

data class AiWorkoutPlannerUiState(
    val step: PlannerStep = PlannerStep.INPUT,
    val prompt: String = "",
    val fitnessLevel: String = "INTERMEDIATE",
    val durationWeeks: Int = 4,
    val startDateOption: StartDateOption = StartDateOption.NEXT_MONDAY,
    val clientId: Long? = null,
    val clientName: String? = null,
    val isGenerating: Boolean = false,
    val isModifying: Boolean = false,
    val isCheckingConflict: Boolean = false,
    val isCommitting: Boolean = false,
    val planPreview: AiWorkoutPlanPreview? = null,
    val selectedDayIndex: Int = 0,
    val modifyPrompt: String = "",
    val showConflictDialog: Boolean = false,
    val conflictPlanId: Long? = null,
    val conflictPlanName: String? = null,
    val pendingIdempotencyKey: String? = null,
    val errorMessage: String? = null,
    @StringRes val errorResId: Int? = null,
    val errorFormatArg: String? = null,
    val committedPlan: UserWorkoutPlan? = null,
    val gender: Gender? = null,
    val generationStatus: GenerationStatus = GenerationStatus.IDLE,
    val chatMessages: List<ChatMessage> = emptyList(),
    val hasRevealedPlan: Boolean = false
)

@HiltViewModel
class AiWorkoutPlannerViewModel @Inject constructor(
    private val generateAiWorkoutPlanUseCase: GenerateAiWorkoutPlanUseCase,
    private val modifyAiWorkoutPlanUseCase: ModifyAiWorkoutPlanUseCase,
    private val commitAiWorkoutPlanUseCase: CommitAiWorkoutPlanUseCase,
    private val getWorkoutPlansUseCase: GetWorkoutPlansUseCase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager(),
    private val genderSessionStore: GenderSessionStore? = null,
    private val tokenManager: TokenManager? = null,
    private val profileRepository: ProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiWorkoutPlannerUiState())
    val uiState: StateFlow<AiWorkoutPlannerUiState> = _uiState.asStateFlow()

    private var hasInitialized: Boolean = false
    private var initializedClientId: Long? = null
    private var generationJob: Job? = null

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            hasInitialized = false
            initializedClientId = null
            _uiState.value = AiWorkoutPlannerUiState()
        }
    )

    init {
        sessionGuard.startObserving()
        genderSessionStore?.state?.let { stateFlow ->
            viewModelScope.launch {
                stateFlow.collect { sessionState ->
                    if (sessionState?.gender != null) {
                        _uiState.update { it.copy(gender = sessionState.gender) }
                    }
                }
            }
        }
    }

    fun initialize(clientId: Long? = null, clientName: String? = null) {
        // Avoid resetting active generation/preview/error state across Compose recompositions
        if (hasInitialized && initializedClientId == clientId && _uiState.value.step != PlannerStep.SUCCESS) {
            return
        }
        hasInitialized = true
        initializedClientId = clientId

        val initialGender = genderSessionStore?.state?.value?.gender
            ?: tokenManager?.getUserId()?.let { userId ->
                Gender.fromApi(tokenManager.getAccountGender(userId))
            }

        _uiState.update {
            AiWorkoutPlannerUiState(
                clientId = clientId,
                clientName = clientName,
                gender = initialGender
            )
        }

        if (profileRepository != null) {
            viewModelScope.launch {
                try {
                    val profileResult = if (clientId != null) {
                        profileRepository.getClientProfile(clientId)
                    } else {
                        profileRepository.getMyClientProfile()
                    }
                    profileResult.getOrNull()?.let { profile ->
                        val level = profile.experienceLevel?.takeIf { it.isNotBlank() }?.uppercase()
                        _uiState.update { current ->
                            current.copy(
                                fitnessLevel = level ?: current.fitnessLevel,
                                gender = current.gender ?: profile.gender
                            )
                        }
                    }
                } catch (_: Throwable) {
                    // Ignored, defaults retained
                }
            }
        }
    }

    fun onPromptChange(newPrompt: String) {
        _uiState.update {
            it.copy(prompt = newPrompt, errorMessage = null, errorResId = null, errorFormatArg = null)
        }
    }

    fun onFitnessLevelChange(level: String) {
        _uiState.update { it.copy(fitnessLevel = level) }
    }

    fun onDurationWeeksChange(weeks: Int) {
        _uiState.update { it.copy(durationWeeks = weeks) }
    }

    fun onStartDateOptionChange(option: StartDateOption) {
        _uiState.update { it.copy(startDateOption = option) }
    }

    fun applyPreset(presetPrompt: String) {
        val weeks = when {
            presetPrompt.contains("12 weeks", ignoreCase = true) -> 12
            presetPrompt.contains("8 weeks", ignoreCase = true) -> 8
            presetPrompt.contains("6 weeks", ignoreCase = true) -> 6
            presetPrompt.contains("4 weeks", ignoreCase = true) -> 4
            presetPrompt.contains("2 weeks", ignoreCase = true) -> 2
            presetPrompt.contains("1 week", ignoreCase = true) -> 1
            else -> null
        }
        val startOpt = when {
            presetPrompt.contains("starting next monday", ignoreCase = true) -> StartDateOption.NEXT_MONDAY
            presetPrompt.contains("starting tomorrow", ignoreCase = true) -> StartDateOption.TOMORROW
            presetPrompt.contains("starting today", ignoreCase = true) -> StartDateOption.TODAY
            else -> null
        }
        _uiState.update {
            it.copy(
                prompt = presetPrompt,
                durationWeeks = weeks ?: it.durationWeeks,
                startDateOption = startOpt ?: it.startDateOption,
                errorMessage = null,
                errorResId = null,
                errorFormatArg = null
            )
        }
    }

    fun onSelectedDayIndexChange(index: Int) {
        _uiState.update { it.copy(selectedDayIndex = index) }
    }

    fun onModifyPromptChange(newPrompt: String) {
        _uiState.update {
            it.copy(modifyPrompt = newPrompt, errorMessage = null, errorResId = null, errorFormatArg = null)
        }
    }

    fun applyModifyPreset(presetText: String) {
        _uiState.update {
            it.copy(modifyPrompt = presetText, errorMessage = null, errorResId = null, errorFormatArg = null)
        }
    }

    fun generatePlan(explicitPrompt: String? = null) {
        val current = _uiState.value
        if (current.isGenerating || current.isModifying || current.isCheckingConflict || current.isCommitting) {
            return
        }
        val prompt = explicitPrompt?.trim() ?: current.prompt.trim()
        if (prompt.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Please enter your workout schedule or goals.",
                    errorResId = R.string.ai_error_blank_prompt,
                    errorFormatArg = null
                )
            }
            return
        }
        if (!connectivityMonitor.isConnected()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Internet connection required for AI generation.",
                    errorResId = R.string.ai_error_offline,
                    errorFormatArg = null
                )
            }
            return
        }

        val userMessage = ChatMessage(role = MessageRole.USER, text = prompt)

        _uiState.update {
            it.copy(
                prompt = prompt,
                isGenerating = true,
                generationStatus = GenerationStatus.SUBMITTING,
                chatMessages = it.chatMessages + userMessage,
                step = PlannerStep.GENERATING,
                hasRevealedPlan = false,
                errorMessage = null,
                errorResId = null,
                errorFormatArg = null
            )
        }

        val startDateStr = current.startDateOption.toLocalDate().toString()

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val statusJob = launch {
                delay(200)
                _uiState.update { it.copy(generationStatus = GenerationStatus.THINKING) }
                delay(1500)
                _uiState.update { it.copy(generationStatus = GenerationStatus.ANALYZING_PROFILE) }
                delay(2000)
                _uiState.update { it.copy(generationStatus = GenerationStatus.GENERATING_PLAN) }
            }

            try {
                generateAiWorkoutPlanUseCase(
                    prompt = prompt,
                    clientId = current.clientId,
                    fitnessLevel = current.fitnessLevel,
                    preferredSplit = null,
                    startDate = startDateStr,
                    durationWeeks = current.durationWeeks
                ).onSuccess { preview ->
                    statusJob.cancel()
                    val aiMessage = ChatMessage(
                        role = MessageRole.AI,
                        text = "✅ Your workout plan is ready! Tap **Review & Save** to view and save it."
                    )
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            generationStatus = GenerationStatus.COMPLETED,
                            chatMessages = it.chatMessages + aiMessage,
                            planPreview = preview,
                            step = PlannerStep.PREVIEW,
                            selectedDayIndex = 0,
                            pendingIdempotencyKey = UUID.randomUUID().toString(),
                            conflictPlanId = null,
                            conflictPlanName = preview.metadata.existingActivePlanName,
                            errorMessage = null,
                            errorResId = null,
                            errorFormatArg = null
                        )
                    }
                }.onFailure { throwable ->
                    statusJob.cancel()
                    val userMsg = NetworkErrorParser.toUserMessage(throwable)
                    val errorText = userMsg.ifBlank { "Unable to generate workout plan. Please try again." }
                    val aiMessage = ChatMessage(
                        role = MessageRole.AI,
                        text = "❌ $errorText"
                    )
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            generationStatus = GenerationStatus.ERROR,
                            chatMessages = it.chatMessages + aiMessage,
                            step = PlannerStep.INPUT,
                            errorMessage = errorText,
                            errorResId = if (throwable is HttpException || userMsg.isNotBlank()) null else R.string.ai_error_generate_failed,
                            errorFormatArg = null
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                statusJob.cancel()
                throw e
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update {
            it.copy(
                isGenerating = false,
                generationStatus = GenerationStatus.CANCELLED,
                step = PlannerStep.INPUT
            )
        }
    }

    fun onPlanRevealCompleted() {
        _uiState.update {
            it.copy(
                isGenerating = false,
                generationStatus = GenerationStatus.COMPLETED,
                hasRevealedPlan = true
            )
        }
    }

    fun modifyPlan() {
        val current = _uiState.value
        if (current.isModifying || current.isGenerating || current.isCheckingConflict || current.isCommitting) {
            return
        }
        val preview = current.planPreview ?: return
        val modifyText = current.modifyPrompt.trim()
        if (modifyText.isBlank()) return

        if (!connectivityMonitor.isConnected()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Internet connection required for AI modification.",
                    errorResId = R.string.ai_error_offline,
                    errorFormatArg = null
                )
            }
            return
        }

        val userMsg = ChatMessage(role = MessageRole.USER, text = modifyText)

        _uiState.update {
            it.copy(
                isModifying = true,
                chatMessages = it.chatMessages + userMsg,
                errorMessage = null,
                errorResId = null,
                errorFormatArg = null
            )
        }

        viewModelScope.launch {
            modifyAiWorkoutPlanUseCase(
                prompt = modifyText,
                currentPlan = preview,
                clientId = current.clientId
            ).onSuccess { updatedPreview ->
                val aiMsg = ChatMessage(
                    role = MessageRole.AI,
                    text = "✅ Updated your workout plan! Tap **Review & Save** to view and save it."
                )
                _uiState.update {
                    it.copy(
                        isModifying = false,
                        planPreview = updatedPreview,
                        modifyPrompt = "",
                        chatMessages = it.chatMessages + aiMsg,
                        pendingIdempotencyKey = UUID.randomUUID().toString(),
                        conflictPlanName = updatedPreview.metadata.existingActivePlanName,
                        errorMessage = null,
                        errorResId = null,
                        errorFormatArg = null
                    )
                }
            }.onFailure { throwable ->
                val userMsg = NetworkErrorParser.toUserMessage(throwable)
                val errorText = userMsg.ifBlank { throwable.localizedMessage ?: "Failed to modify routine." }
                val aiMsg = ChatMessage(
                    role = MessageRole.AI,
                    text = "❌ $errorText"
                )
                _uiState.update {
                    it.copy(
                        isModifying = false,
                        chatMessages = it.chatMessages + aiMsg,
                        errorMessage = errorText,
                        errorResId = if (throwable is HttpException || userMsg.isNotBlank()) null else R.string.ai_error_modify_failed,
                        errorFormatArg = null
                    )
                }
            }
        }
    }

    fun requestCommit() {
        val current = _uiState.value
        if (current.isCheckingConflict || current.isCommitting || current.isGenerating || current.isModifying || current.showConflictDialog) {
            return
        }
        current.planPreview ?: return

        if (!connectivityMonitor.isConnected()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Internet connection required to save plan.",
                    errorResId = R.string.ai_error_offline,
                    errorFormatArg = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isCheckingConflict = true,
                errorMessage = null,
                errorResId = null,
                errorFormatArg = null
            )
        }

        viewModelScope.launch {
            getWorkoutPlansUseCase.getLatestActivePlan(current.clientId)
                .onSuccess { activePlan ->
                    if (activePlan != null) {
                        _uiState.update {
                            it.copy(
                                isCheckingConflict = false,
                                conflictPlanId = activePlan.id,
                                conflictPlanName = activePlan.name,
                                showConflictDialog = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isCheckingConflict = false,
                                conflictPlanId = null,
                                conflictPlanName = null
                            )
                        }
                        executeCommit(replaceExisting = true, expectedActivePlanId = 0L)
                    }
                }
                .onFailure { throwable ->
                    val userMsg = NetworkErrorParser.toUserMessage(throwable)
                    _uiState.update {
                        it.copy(
                            isCheckingConflict = false,
                            errorMessage = userMsg.ifBlank { "Unable to verify active plans. Please try again." },
                            errorResId = if (throwable is HttpException || userMsg.isNotBlank()) null else R.string.ai_error_commit_failed,
                            errorFormatArg = null
                        )
                    }
                }
        }
    }

    fun dismissConflictDialog() {
        if (_uiState.value.isCommitting) return
        _uiState.update { it.copy(showConflictDialog = false) }
    }

    fun confirmConflictAndCommit() {
        val current = _uiState.value
        if (current.isCommitting) return
        val expectedId = current.conflictPlanId
        _uiState.update { it.copy(showConflictDialog = false) }
        executeCommit(replaceExisting = true, expectedActivePlanId = expectedId)
    }

    fun commitWithoutReplacing() {
        if (_uiState.value.isCommitting) return
        _uiState.update { it.copy(showConflictDialog = false) }
        executeCommit(replaceExisting = false, expectedActivePlanId = null)
    }

    private fun executeCommit(replaceExisting: Boolean, expectedActivePlanId: Long?) {
        val current = _uiState.value
        if (current.isCommitting) return
        val preview = current.planPreview ?: return

        if (!connectivityMonitor.isConnected()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Internet connection required to save plan.",
                    errorResId = R.string.ai_error_offline,
                    errorFormatArg = null
                )
            }
            return
        }

        val idempotencyKey = current.pendingIdempotencyKey ?: UUID.randomUUID().toString()

        _uiState.update {
            it.copy(
                isCommitting = true,
                step = PlannerStep.COMMITTING,
                pendingIdempotencyKey = idempotencyKey,
                errorMessage = null,
                errorResId = null,
                errorFormatArg = null
            )
        }

        val startDateStr = current.startDateOption.toLocalDate().toString()

        viewModelScope.launch {
            commitAiWorkoutPlanUseCase(
                plan = preview,
                idempotencyKey = idempotencyKey,
                startDate = startDateStr,
                replaceExisting = replaceExisting,
                clientId = current.clientId,
                expectedActivePlanId = expectedActivePlanId
            ).onSuccess { savedPlan ->
                _uiState.update {
                    it.copy(
                        isCommitting = false,
                        committedPlan = savedPlan,
                        step = PlannerStep.SUCCESS,
                        pendingIdempotencyKey = null,
                        errorMessage = null,
                        errorResId = null,
                        errorFormatArg = null
                    )
                }
            }.onFailure { throwable ->
                if (throwable is HttpException && throwable.code() == 409) {
                    val latestActive = getWorkoutPlansUseCase.getLatestActivePlan(current.clientId).getOrNull()
                    val serverMsg = NetworkErrorParser.toUserMessage(throwable)
                    if (latestActive != null) {
                        _uiState.update {
                            it.copy(
                                isCommitting = false,
                                step = PlannerStep.PREVIEW,
                                conflictPlanId = latestActive.id,
                                conflictPlanName = latestActive.name,
                                showConflictDialog = true,
                                errorMessage = serverMsg,
                                errorResId = R.string.ai_error_conflict_changed,
                                errorFormatArg = latestActive.name
                            )
                        }
                        return@launch
                    }
                }
                val userMsg = NetworkErrorParser.toUserMessage(throwable)
                _uiState.update {
                    it.copy(
                        isCommitting = false,
                        step = PlannerStep.PREVIEW,
                        errorMessage = userMsg.ifBlank { throwable.localizedMessage ?: "Failed to commit routine." },
                        errorResId = if (throwable is HttpException || userMsg.isNotBlank()) null else R.string.ai_error_commit_failed,
                        errorFormatArg = null
                    )
                }
            }
        }
    }

    fun reset() {
        val initialGender = genderSessionStore?.state?.value?.gender
            ?: tokenManager?.getUserId()?.let { userId ->
                Gender.fromApi(tokenManager.getAccountGender(userId))
            }
        _uiState.update {
            AiWorkoutPlannerUiState(
                clientId = it.clientId,
                clientName = it.clientName,
                fitnessLevel = it.fitnessLevel,
                gender = initialGender ?: it.gender
            )
        }
    }
}
