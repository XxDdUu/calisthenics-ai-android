package com.calisthenics.ai.ui.workoutsession.bubble

import androidx.lifecycle.ViewModel
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.WorkoutExperiencePreferences
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class UnfinishedWorkoutBubbleState(
    val isEnabled: Boolean = true,
    val activeSession: WorkoutSession? = null,
    val workoutTitle: String = "",
    val currentExerciseName: String = "",
    val completedSets: Int = 0,
    val totalSets: Int = 0,
    val progress: Float = 0f,
    val isDismissed: Boolean = false,
    val isMinimized: Boolean = false
) {
    val isVisible: Boolean
        get() = isEnabled && !isDismissed && activeSession != null
}

@HiltViewModel
class UnfinishedWorkoutBubbleViewModel @Inject constructor(
    private val coordinator: UnfinishedWorkoutBubbleCoordinator
) : ViewModel() {

    constructor(
        workoutSessionRepository: WorkoutSessionRepository,
        workoutExperiencePreferences: WorkoutExperiencePreferences,
        accountSessionManager: AccountSessionManager = AccountSessionManager()
    ) : this(
        UnfinishedWorkoutBubbleCoordinator(
            workoutSessionRepository,
            workoutExperiencePreferences,
            accountSessionManager
        )
    )

    val state: StateFlow<UnfinishedWorkoutBubbleState> = coordinator.state

    fun openBubble() = coordinator.openBubble()

    fun dismiss() = coordinator.dismiss()

    fun toggleMinimized() = coordinator.toggleMinimized()

    fun refresh() = coordinator.refresh()

    fun stopPolling() = coordinator.stopPolling()

    suspend fun checkActiveSessions() = coordinator.checkActiveSessions()

    fun processSessions(sessions: List<WorkoutSession>) = coordinator.processSessions(sessions)
}
