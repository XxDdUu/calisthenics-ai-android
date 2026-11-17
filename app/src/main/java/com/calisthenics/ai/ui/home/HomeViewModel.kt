package com.calisthenics.ai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.OnboardingSessionStore
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.GettingStartedDayStatus
import com.calisthenics.ai.domain.model.GettingStartedEnrollment
import com.calisthenics.ai.domain.model.GettingStartedProgram
import com.calisthenics.ai.domain.model.GettingStartedScheduleConflict
import com.calisthenics.ai.domain.model.GettingStartedScheduleConflictException
import com.calisthenics.ai.domain.model.OnboardingStatus
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.GettingStartedRepository
import com.calisthenics.ai.domain.repository.NoOpAnalyticsRepository
import com.calisthenics.ai.domain.usecase.GetAnalysisHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoadingHistory: Boolean = false,
    val history: List<AnalysisResult> = emptyList(),
    val error: String? = null,
    val activeUserId: Long? = null,
    val isAthlete: Boolean = false,
    val onboardingStatus: OnboardingStatus = OnboardingStatus.NOT_SHOWN,
    val isUpdatingOnboarding: Boolean = false,
    val showProgramDetails: Boolean = false,
    val publishedProgram: GettingStartedProgram? = null,
    val isEnrolling: Boolean = false,
    val showEnrollChoiceDialog: Boolean = false,
    val isCheckingConflicts: Boolean = false,
    val pendingScheduleConflicts: List<GettingStartedScheduleConflict> = emptyList(),
    val showConflictDialog: Boolean = false,
    val isStartingDaySession: Boolean = false,
    val enrollmentError: String? = null,
    val unreadNotificationCount: Long = 0L,
    val unreadCountAvailable: Boolean = false
) {
    val isGettingStartedCompleted: Boolean
        get() = publishedProgram?.enrollment?.isCompleted == true

    val shouldShowOnboardingModal: Boolean
        get() = isAthlete && (publishedProgram?.eligible == true) && !isGettingStartedCompleted &&
            (onboardingStatus == OnboardingStatus.NOT_SHOWN)

    val shouldShowOnboardingCard: Boolean
        get() = shouldShowOnboardingModal

    val shouldShowCompactBanner: Boolean
        get() = isAthlete && (publishedProgram?.eligible == true) && !isGettingStartedCompleted &&
            (onboardingStatus == OnboardingStatus.COMPACT_BANNER)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getAnalysisHistoryUseCase: GetAnalysisHistoryUseCase,
    private val onboardingSessionStore: OnboardingSessionStore,
    private val gettingStartedRepository: GettingStartedRepository,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager(),
    private val analyticsRepository: AnalyticsRepository = NoOpAnalyticsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var recordedImpressionKey: String? = null
    private var lastTrackedDetailProgramKey: String? = null

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = { _uiState.value = HomeUiState() },
        onSessionActive = {
            loadHistory()
            syncOnboardingState()
            refreshUnreadNotificationCount()
        }
    )

    init {
        sessionGuard.startObserving()
        loadHistory()
        syncOnboardingState()
        refreshUnreadNotificationCount()
        viewModelScope.launch {
            gettingStartedRepository.enrollmentUpdates.collect {
                refreshPublishedProgramOnly()
            }
        }
        viewModelScope.launch {
            analyticsRepository.notificationUpdates.collect {
                refreshUnreadNotificationCount()
            }
        }
    }

    fun refreshUnreadNotificationCount() {
        if (!authRepository.isLoggedIn()) {
            _uiState.update { it.copy(unreadNotificationCount = 0L, unreadCountAvailable = false) }
            return
        }
        sessionGuard.launchScoped { captured ->
            analyticsRepository.getUnreadNotificationsCount()
                .onSuccess { count ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            unreadNotificationCount = count.coerceAtLeast(0L),
                            unreadCountAvailable = true
                        )
                    }
                }
                .onFailure {
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(unreadCountAvailable = false) }
                }
        }
    }

    private fun refreshPublishedProgramOnly() {
        val role = authRepository.getUserRole()
        if (!authRepository.isLoggedIn() || role.equals("ROLE_ADMIN", ignoreCase = true)) return
        sessionGuard.launchScoped { captured ->
            gettingStartedRepository.getPublishedProgram()
                .onSuccess { program ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            publishedProgram = program,
                            onboardingStatus = program?.userDismissalState ?: it.onboardingStatus
                        )
                    }
                }
        }
    }

    fun syncOnboardingState() {
        if (!authRepository.isLoggedIn()) {
            _uiState.update { it.copy(isAthlete = false, onboardingStatus = OnboardingStatus.DISMISSED, publishedProgram = null) }
            return
        }

        val role = authRepository.getUserRole()
        val isAdmin = role.equals("ROLE_ADMIN", ignoreCase = true)
        if (isAdmin) {
            _uiState.update {
                it.copy(
                    isAthlete = false,
                    onboardingStatus = OnboardingStatus.DISMISSED,
                    publishedProgram = null
                )
            }
            return
        }

        val userId = authRepository.getUserId()

        _uiState.update {
            it.copy(
                activeUserId = userId,
                isAthlete = true
            )
        }

        sessionGuard.launchScoped { captured ->
            authRepository.getCurrentUser()
                .onSuccess { userProfile ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    val isStillAthlete = !userProfile.role.equals("ROLE_ADMIN", ignoreCase = true)
                    _uiState.update { current ->
                        current.copy(
                            activeUserId = userProfile.id,
                            isAthlete = isStillAthlete
                        )
                    }
                }

            gettingStartedRepository.getPublishedProgram()
                .onSuccess { publishedProg ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            publishedProgram = publishedProg,
                            onboardingStatus = publishedProg?.userDismissalState ?: it.onboardingStatus
                        )
                    }
                }
                .onFailure {
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(publishedProgram = null) }
                }
        }
    }

    fun loadHistory() {
        if (!authRepository.isLoggedIn()) return
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoadingHistory = true, error = null) }
            getAnalysisHistoryUseCase()
                .onSuccess { items ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { it.copy(isLoadingHistory = false, history = items) }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(isLoadingHistory = false, error = error.localizedMessage ?: "Could not load analysis history") }
                }
        }
    }

    fun dismissToCompactBanner() {
        _uiState.update {
            it.copy(
                onboardingStatus = OnboardingStatus.COMPACT_BANNER,
                publishedProgram = it.publishedProgram?.copy(userDismissalState = OnboardingStatus.COMPACT_BANNER),
                isUpdatingOnboarding = true
            )
        }
        sessionGuard.launchScoped { captured ->
            gettingStartedRepository.updateDismissalStatus(OnboardingStatus.COMPACT_BANNER)
                .onSuccess { updatedProgram ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            publishedProgram = updatedProgram,
                            onboardingStatus = updatedProgram.userDismissalState,
                            isUpdatingOnboarding = false
                        )
                    }
                    val currentUserId = _uiState.value.activeUserId ?: authRepository.getUserId()
                    if (currentUserId != null) {
                        onboardingSessionStore.publish(currentUserId, updatedProgram.userDismissalState)
                    }
                }
                .onFailure {
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(isUpdatingOnboarding = false) }
                }
        }
    }

    fun permanentlyDismissOnboarding() {
        _uiState.update {
            it.copy(
                onboardingStatus = OnboardingStatus.DISMISSED,
                publishedProgram = it.publishedProgram?.copy(userDismissalState = OnboardingStatus.DISMISSED),
                isUpdatingOnboarding = true
            )
        }
        sessionGuard.launchScoped { captured ->
            gettingStartedRepository.updateDismissalStatus(OnboardingStatus.DISMISSED)
                .onSuccess { updatedProgram ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            publishedProgram = updatedProgram,
                            onboardingStatus = updatedProgram.userDismissalState,
                            isUpdatingOnboarding = false
                        )
                    }
                    val currentUserId = _uiState.value.activeUserId ?: authRepository.getUserId()
                    if (currentUserId != null) {
                        onboardingSessionStore.publish(currentUserId, updatedProgram.userDismissalState)
                    }
                }
                .onFailure {
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(isUpdatingOnboarding = false) }
                }
        }
    }

    fun trackModalImpression() {
        val state = _uiState.value
        val program = state.publishedProgram ?: return
        if (!state.shouldShowOnboardingModal) return
        val key = "${program.id}:${program.version}"
        if (recordedImpressionKey == key) return
        recordedImpressionKey = key
        sessionGuard.launchScoped {
            gettingStartedRepository.trackCampaignEvent("MODAL_IMPRESSION", program.id)
        }
    }

    fun openProgramDetails() {
        _uiState.update { it.copy(showProgramDetails = true) }
        val currentProg = _uiState.value.publishedProgram
        if (_uiState.value.isAthlete && currentProg?.eligible == true) {
            val detailKey = "${currentProg.id}:${currentProg.version}"
            if (lastTrackedDetailProgramKey != detailKey) {
                lastTrackedDetailProgramKey = detailKey
                sessionGuard.launchScoped {
                    gettingStartedRepository.trackCampaignEvent("PROGRAM_DETAIL_VIEW", currentProg.id)
                }
            }
        }
        sessionGuard.launchScoped { captured ->
            gettingStartedRepository.getPublishedProgram()
                .onSuccess { program ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { it.copy(publishedProgram = program) }
                }
        }
    }

    fun closeProgramDetails() {
        _uiState.update { it.copy(showProgramDetails = false) }
    }

    fun requestEnroll() {
        val currentProgram = _uiState.value.publishedProgram ?: return
        if (currentProgram.enrollment != null) {
            if (!currentProgram.enrollment.addedToWorkoutPlan) {
                enrollAddToWorkoutPlan()
            }
            return
        }
        _uiState.update { it.copy(showEnrollChoiceDialog = true, enrollmentError = null) }
    }

    fun dismissEnrollChoiceDialog() {
        _uiState.update { it.copy(showEnrollChoiceDialog = false) }
    }

    fun enrollJustStart() {
        val currentProgram = _uiState.value.publishedProgram ?: return
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(showEnrollChoiceDialog = false, isEnrolling = true, enrollmentError = null) }
            gettingStartedRepository.enroll(addToWorkoutPlan = false, confirmConflicts = false)
                .onSuccess { enrollment ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isEnrolling = false,
                            publishedProgram = mergeEnrollmentIntoProgram(currentProgram, enrollment)
                        )
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isEnrolling = false,
                            enrollmentError = error.localizedMessage ?: "Could not enroll in Getting Started."
                        )
                    }
                }
        }
    }

    fun enrollAddToWorkoutPlan() {
        if (_uiState.value.publishedProgram == null) return
        sessionGuard.launchScoped { captured ->
            _uiState.update {
                it.copy(
                    showEnrollChoiceDialog = false,
                    isCheckingConflicts = true,
                    enrollmentError = null
                )
            }
            gettingStartedRepository.previewEnrollment()
                .onSuccess { preview ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    if (preview.hasConflicts && preview.conflicts.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                isCheckingConflicts = false,
                                pendingScheduleConflicts = preview.conflicts,
                                showConflictDialog = true
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isCheckingConflicts = false) }
                        performAddToWorkoutPlanEnrollment(confirmConflicts = false)
                    }
                }
                .onFailure {
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(isCheckingConflicts = false) }
                    performAddToWorkoutPlanEnrollment(confirmConflicts = false)
                }
        }
    }

    fun confirmConflictEnrollment() {
        _uiState.update { it.copy(showConflictDialog = false) }
        performAddToWorkoutPlanEnrollment(confirmConflicts = true)
    }

    fun dismissConflictDialog() {
        _uiState.update { it.copy(showConflictDialog = false, pendingScheduleConflicts = emptyList()) }
    }

    fun clearEnrollmentError() {
        _uiState.update { it.copy(enrollmentError = null) }
    }

    private fun performAddToWorkoutPlanEnrollment(confirmConflicts: Boolean) {
        val currentProgram = _uiState.value.publishedProgram ?: return
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isEnrolling = true, enrollmentError = null) }
            gettingStartedRepository.enroll(addToWorkoutPlan = true, confirmConflicts = confirmConflicts)
                .onSuccess { enrollment ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isEnrolling = false,
                            pendingScheduleConflicts = emptyList(),
                            showConflictDialog = false,
                            publishedProgram = mergeEnrollmentIntoProgram(currentProgram, enrollment)
                        )
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    if (error is GettingStartedScheduleConflictException) {
                        _uiState.update {
                            it.copy(
                                isEnrolling = false,
                                pendingScheduleConflicts = error.conflicts,
                                showConflictDialog = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isEnrolling = false,
                                enrollmentError = error.localizedMessage ?: "Failed to add Getting Started to Workout Plan."
                            )
                        }
                    }
                }
        }
    }

    fun startDayWorkout(dayNumber: Int, onSessionStarted: (WorkoutSession) -> Unit) {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isStartingDaySession = true, enrollmentError = null) }
            gettingStartedRepository.startDaySession(dayNumber)
                .onSuccess { session ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { state ->
                        val prog = state.publishedProgram
                        val updatedProgram = prog?.let { p ->
                            val updatedDays = p.days.map { day ->
                                if (day.dayNumber == dayNumber && day.status != GettingStartedDayStatus.COMPLETED) {
                                    day.copy(
                                        status = GettingStartedDayStatus.IN_PROGRESS,
                                        workoutSessionId = session.id
                                    )
                                } else day
                            }
                            p.copy(days = updatedDays)
                        }
                        state.copy(
                            isStartingDaySession = false,
                            showProgramDetails = false,
                            publishedProgram = updatedProgram
                        )
                    }
                    onSessionStarted(session)
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isStartingDaySession = false,
                            enrollmentError = error.localizedMessage ?: "Could not start Day $dayNumber workout."
                        )
                    }
                }
        }
    }

    fun enrollInProgram() {
        val currentProgram = _uiState.value.publishedProgram ?: return
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isEnrolling = true) }
            gettingStartedRepository.enroll()
                .onSuccess { enrollment ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isEnrolling = false,
                            publishedProgram = mergeEnrollmentIntoProgram(currentProgram, enrollment)
                        )
                    }
                }
                .onFailure {
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(isEnrolling = false) }
                }
        }
    }

    fun completeProgramDay(dayNumber: Int) {
        val currentProgram = _uiState.value.publishedProgram ?: return
        sessionGuard.launchScoped { captured ->
            gettingStartedRepository.completeDay(dayNumber)
                .onSuccess { updatedEnrollment ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update {
                        it.copy(
                            publishedProgram = mergeEnrollmentIntoProgram(currentProgram, updatedEnrollment).let { merged ->
                                merged.copy(
                                    days = merged.days.map { d ->
                                        if (d.dayNumber == dayNumber) d.copy(status = GettingStartedDayStatus.COMPLETED, completed = true) else d
                                    }
                                )
                            }
                        )
                    }
                }
                .onFailure { error ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update { it.copy(enrollmentError = error.localizedMessage) }
                }
        }
    }

    private fun mergeEnrollmentIntoProgram(
        program: GettingStartedProgram,
        enrollment: GettingStartedEnrollment
    ): GettingStartedProgram {
        val progressByDay = enrollment.dayProgress.associateBy { it.dayNumber }
        val completedSet = enrollment.completedDays.toSet()
        val updatedDays = program.days.map { day ->
            val progress = progressByDay[day.dayNumber]
            val resolvedStatus = when {
                progress != null -> progress.status
                day.dayNumber in completedSet -> GettingStartedDayStatus.COMPLETED
                else -> day.status
            }
            day.copy(
                status = resolvedStatus,
                completed = resolvedStatus == GettingStartedDayStatus.COMPLETED,
                scheduledDate = progress?.scheduledDate ?: day.scheduledDate,
                workoutSessionId = progress?.workoutSessionId ?: day.workoutSessionId,
                startedAt = progress?.startedAt ?: day.startedAt,
                completedAt = progress?.completedAt ?: day.completedAt
            )
        }
        return program.copy(days = updatedDays, enrollment = enrollment)
    }
}
