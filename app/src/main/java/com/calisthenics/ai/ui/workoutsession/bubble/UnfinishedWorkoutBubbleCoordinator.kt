package com.calisthenics.ai.ui.workoutsession.bubble

import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.WorkoutExperiencePreferences
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionClassification
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnfinishedWorkoutBubbleCoordinator @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutExperiencePreferences: WorkoutExperiencePreferences,
    accountSessionManager: AccountSessionManager = AccountSessionManager()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(UnfinishedWorkoutBubbleState())
    val state: StateFlow<UnfinishedWorkoutBubbleState> = _state.asStateFlow()

    private var lastDismissedSessionId: Long? = null
    private var pollingJob: Job? = null

    private val sessionGuard = AccountSessionGuard(
        parentScope = scope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            _state.value = UnfinishedWorkoutBubbleState(isEnabled = false)
        }
    )

    init {
        sessionGuard.startObserving()
        observePreferences()
        observeActiveSessions()
        startPollingActiveSessions()
    }

    private fun observePreferences() {
        scope.launch {
            workoutExperiencePreferences.unfinishedWorkoutBubbleEnabled.collect { enabled ->
                _state.update { current ->
                    current.copy(
                        isEnabled = enabled,
                        isDismissed = if (enabled && current.isDismissed && lastDismissedSessionId == current.activeSession?.id) {
                            false
                        } else {
                            current.isDismissed
                        }
                    )
                }
            }
        }
    }

    private fun observeActiveSessions() {
        scope.launch {
            workoutSessionRepository.observeActiveSessions().collect { sessions ->
                processSessions(sessions)
            }
        }
    }

    private fun startPollingActiveSessions() {
        pollingJob = scope.launch {
            while (isActive) {
                checkActiveSessions()
                delay(8000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refresh() {
        scope.launch {
            checkActiveSessions()
        }
    }

    suspend fun checkActiveSessions() {
        workoutSessionRepository.getActiveSessions().onSuccess { sessions ->
            processSessions(sessions)
        }
    }

    fun processSessions(sessions: List<WorkoutSession>) {
        val today = LocalDate.now()
        val unfinished = sessions.firstOrNull {
            it.effectiveClassification(today) == WorkoutSessionClassification.UNFINISHED
        }
        if (unfinished == null) {
            _state.update {
                it.copy(
                    activeSession = null,
                    workoutTitle = "",
                    currentExerciseName = "",
                    completedSets = 0,
                    totalSets = 0,
                    progress = 0f
                )
            }
        } else {
            val totalSets = unfinished.exercises.sumOf { it.sets.size }
            val completedSets = unfinished.exercises.sumOf { ex -> ex.sets.count { it.completed } }
            val progress = if (totalSets > 0) completedSets.toFloat() / totalSets else 0f
            val currentExercise = unfinished.exercises.firstOrNull { ex ->
                ex.status != "COMPLETED" && ex.status != "SKIPPED"
            }?.exerciseName ?: unfinished.exercises.firstOrNull()?.exerciseName.orEmpty()

            val shouldResetDismissal = lastDismissedSessionId != unfinished.id

            _state.update { current ->
                current.copy(
                    activeSession = unfinished,
                    workoutTitle = unfinished.title,
                    currentExerciseName = currentExercise,
                    completedSets = completedSets,
                    totalSets = totalSets,
                    progress = progress,
                    isDismissed = if (shouldResetDismissal) false else current.isDismissed
                )
            }
        }
    }

    /**
     * Brings the floating bubble into focus:
     * - Resets dismissal (shows bubble even if previously dismissed)
     * - Expands if minimized
     * - Refreshes active sessions
     */
    fun openBubble() {
        lastDismissedSessionId = null
        _state.update { current ->
            current.copy(
                isDismissed = false,
                isMinimized = false
            )
        }
        refresh()
    }

    fun dismiss() {
        lastDismissedSessionId = _state.value.activeSession?.id
        _state.update { it.copy(isDismissed = true) }
    }

    fun toggleMinimized() {
        _state.update { it.copy(isMinimized = !it.isMinimized) }
    }
}
