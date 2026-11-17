package com.calisthenics.ai.ui.workoutsession

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.local.SetLoggingMode
import com.calisthenics.ai.data.local.WorkoutExperiencePreferences
import com.calisthenics.ai.data.local.WorkoutTimerPreferences
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionExercise
import com.calisthenics.ai.domain.model.WorkoutSessionSet
import com.calisthenics.ai.domain.model.WorkoutSessionSetUpdate
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import com.calisthenics.ai.ui.workoutsession.timer.ActiveTimerState
import com.calisthenics.ai.ui.workoutsession.timer.ExerciseTimerConfig
import com.calisthenics.ai.ui.workoutsession.timer.TimeSource
import com.calisthenics.ai.ui.workoutsession.timer.TimerStatus
import com.calisthenics.ai.ui.workoutsession.timer.WorkoutAudioHapticHelper
import com.calisthenics.ai.ui.workoutsession.timer.WorkoutNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

data class WorkoutSessionUiState(
    val isLoading: Boolean = true,
    val isFinishing: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsyncedChanges: Boolean = false,
    val hasSyncConflict: Boolean = false,
    val session: WorkoutSession? = null,
    val elapsedSeconds: Long = 0,
    val savingSetIds: Set<Long> = emptySet(),
    val isFinishConfirmationVisible: Boolean = false,
    val error: String? = null,
    val activeTimer: ActiveTimerState = ActiveTimerState(),
    val isTimerSettingsDialogVisible: Boolean = false,
    val activeTimerConfig: ExerciseTimerConfig = ExerciseTimerConfig(),
    val activeConfigExerciseName: String = "",
    val loggingMode: SetLoggingMode = SetLoggingMode.STEP_BY_STEP,
    val selectedExerciseIndex: Int = 0,
    val selectedSetIndex: Int = 0
)

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val sessionRepository: WorkoutSessionRepository,
    savedStateHandle: SavedStateHandle,
    private val timerPreferences: WorkoutTimerPreferences,
    private val audioHapticHelper: WorkoutAudioHapticHelper,
    private val notificationHelper: WorkoutNotificationHelper,
    private val timeSource: TimeSource,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager(),
    private val workoutExperiencePreferences: WorkoutExperiencePreferences? = null
) : ViewModel() {

    private val date = checkNotNull(savedStateHandle.get<String>("date"))
    private val planId = savedStateHandle.get<Long>("planId")
        ?: savedStateHandle.get<String>("planId")?.toLongOrNull()
        ?: 0L
    private val dayId = savedStateHandle.get<Long>("dayId")
        ?: savedStateHandle.get<String>("dayId")?.toLongOrNull()
        ?: 0L
    private val assignmentId = savedStateHandle.get<Long>("assignmentId")?.takeIf { it > 0L }
    private val existingSessionId = savedStateHandle.get<Long>("sessionId")?.takeIf { it > 0L }
    private val _uiState = MutableStateFlow(WorkoutSessionUiState())
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState.asStateFlow()
    private val saveJobs = mutableMapOf<Long, Job>()
    private val draftJobs = mutableMapOf<Long, Job>()
    private var timerJob: Job? = null

    private var countdownTickerJob: Job? = null
    private var holdTickerJob: Job? = null
    private var restTickerJob: Job? = null

    private var holdStartMonotonicMs: Long = 0L
    private var restTargetMonotonicMs: Long = 0L
    private var restRemainingMillisOnPause: Long = 0L

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            saveJobs.values.forEach { it.cancel() }
            saveJobs.clear()
            draftJobs.values.forEach { it.cancel() }
            draftJobs.clear()
            timerJob?.cancel()
            countdownTickerJob?.cancel()
            holdTickerJob?.cancel()
            restTickerJob?.cancel()
            _uiState.value = WorkoutSessionUiState(isLoading = false)
        }
    )

    init {
        sessionGuard.startObserving()
        observePreferences()
        load()
    }

    private fun observePreferences() {
        workoutExperiencePreferences?.let { prefs ->
            viewModelScope.launch {
                prefs.setLoggingMode.collect { mode ->
                    _uiState.update { it.copy(loggingMode = mode) }
                }
            }
        }
    }

    private fun load() {
        sessionGuard.launchScoped { captured ->
            val result = if (existingSessionId != null) {
                sessionRepository.getSession(existingSessionId).fold(
                    onSuccess = { loaded ->
                        if (loaded.status.equals("CANCELLED", ignoreCase = true) ||
                            loaded.status.equals("CANCELED", ignoreCase = true)
                        ) {
                            Result.failure(
                                IllegalStateException(
                                    "This coach-assigned workout was cancelled because the coaching relationship ended."
                                )
                            )
                        } else {
                            val pId = loaded.workoutPlanId
                            val dId = loaded.workoutPlanDayId
                            if (loaded.exercises.isEmpty() && pId != null && pId > 0L && dId != null && dId > 0L) {
                                sessionRepository.startOrResume(
                                    pId,
                                    dId,
                                    loaded.workoutDate?.takeIf(String::isNotBlank) ?: date,
                                    loaded.assignmentId
                                )
                            } else if (
                                loaded.status.equals("SCHEDULED", ignoreCase = true) &&
                                (loaded.source == "GETTING_STARTED" ||
                                    ((pId == null || pId <= 0L) && (dId == null || dId <= 0L)))
                            ) {
                                sessionRepository.startScheduledSession(loaded.id)
                            } else {
                                Result.success(loaded)
                            }
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            } else if (planId > 0L && dayId > 0L) {
                sessionRepository.startOrResume(planId, dayId, date, assignmentId)
            } else {
                Result.failure(IllegalStateException("This workout is missing its scheduled plan information."))
            }
            if (!sessionGuard.isCurrentSession(captured)) return@launchScoped
            result.onSuccess { session ->
                val initialExIndex = session.exercises.indexOfFirst { ex ->
                    ex.status != "COMPLETED" && ex.status != "SKIPPED"
                }.let { if (it == -1) 0 else it }
                val targetEx = session.exercises.getOrNull(initialExIndex)
                val initialSetIndex = targetEx?.sets?.indexOfFirst { !it.completed }?.let {
                    if (it == -1) 0 else it
                } ?: 0

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        elapsedSeconds = elapsedSeconds(session.startedAt),
                        hasUnsyncedChanges = sessionRepository.hasPendingChanges(session.id),
                        hasSyncConflict = sessionRepository.hasSyncConflict(session.id),
                        selectedExerciseIndex = initialExIndex,
                        selectedSetIndex = initialSetIndex,
                        error = null
                    )
                }
                startElapsedTimer()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = error.localizedMessage ?: "Could not load this workout.")
                }
            }
        }
    }

    fun updateSet(
        exerciseId: Long,
        setId: Long,
        update: WorkoutSessionSetUpdate,
        immediate: Boolean = false
    ) {
        val current = _uiState.value.session ?: return
        val versionedUpdate = update.copy(
            expectedVersion = current.exercises
                .firstOrNull { it.id == exerciseId }
                ?.sets?.firstOrNull { it.id == setId }
                ?.version ?: return
        )
        val optimistic = current.updateSet(exerciseId, setId, versionedUpdate) ?: return
        _uiState.update {
            it.copy(
                session = optimistic,
                savingSetIds = it.savingSetIds + setId,
                isSaving = true,
                error = null
            )
        }
        draftJobs.remove(setId)?.cancel()
        draftJobs[setId] = viewModelScope.launch {
            sessionRepository.saveSetDraft(current.id, exerciseId, setId, versionedUpdate)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = "Could not save this change on the device: ${error.localizedMessage ?: "storage unavailable"}",
                            hasUnsyncedChanges = true
                        )
                    }
                }
        }
        saveJobs.remove(setId)?.cancel()
        saveJobs[setId] = viewModelScope.launch {
            if (!immediate && !versionedUpdate.completed) {
                delay(450)
            }
            sessionRepository.updateSet(current.id, exerciseId, setId, versionedUpdate)
                .onSuccess { response ->
                    val saving = _uiState.value.savingSetIds - setId
                    val hasPending = sessionRepository.hasPendingChanges(response.id)
                    val merged = mergeServerResponse(response, _uiState.value.session, saving)
                    _uiState.update {
                        it.copy(
                            session = merged,
                            savingSetIds = saving,
                            isSaving = saving.isNotEmpty(),
                            hasUnsyncedChanges = saving.isNotEmpty() || hasPending,
                            hasSyncConflict = sessionRepository.hasSyncConflict(response.id),
                            error = null
                        )
                    }
                    if (versionedUpdate.completed) {
                        checkAndTriggerRestTimer(exerciseId, setId, merged)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            savingSetIds = it.savingSetIds - setId,
                            isSaving = it.savingSetIds.size > 1,
                            hasUnsyncedChanges = true,
                            hasSyncConflict = sessionRepository.hasSyncConflict(current.id),
                            error = "Change saved on this device and will sync when the connection returns. " +
                                (error.localizedMessage ?: "")
                        )
                    }
                    if (versionedUpdate.completed) {
                        val currentSession = _uiState.value.session ?: optimistic
                        checkAndTriggerRestTimer(exerciseId, setId, currentSession)
                    }
                }
        }
    }

    fun toggleExerciseSkipped(exercise: WorkoutSessionExercise) {
        val session = _uiState.value.session ?: return
        val status = if (exercise.status == "SKIPPED") "IN_PROGRESS" else "SKIPPED"
        _uiState.update { state ->
            state.copy(
                session = state.session?.updateExerciseStatus(exercise.id, status),
                isSaving = true,
                error = null
            )
        }
        viewModelScope.launch {
            sessionRepository.updateExercise(session.id, exercise.id, status, exercise.version)
                .onSuccess { response ->
                    val hasPending = sessionRepository.hasPendingChanges(response.id)
                    _uiState.update {
                        it.copy(
                            session = response,
                            isSaving = false,
                            hasUnsyncedChanges = hasPending,
                            hasSyncConflict = sessionRepository.hasSyncConflict(response.id),
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasUnsyncedChanges = true,
                            hasSyncConflict = sessionRepository.hasSyncConflict(session.id),
                            error = "Exercise state saved on this device and will sync when possible. " +
                                (error.localizedMessage ?: "")
                        )
                    }
                }
        }
    }

    fun showFinishConfirmation() {
        _uiState.update { it.copy(isFinishConfirmationVisible = true) }
    }

    fun dismissFinishConfirmation() {
        _uiState.update { it.copy(isFinishConfirmationVisible = false) }
    }

    fun finish(confirmPartial: Boolean, onFinished: (Long) -> Unit) {
        val session = _uiState.value.session ?: return
        if (_uiState.value.isFinishing) return
        cancelTimer()
        viewModelScope.launch {
            _uiState.update { it.copy(isFinishing = true, isFinishConfirmationVisible = false, error = null) }
            draftJobs.values.toList().forEach { it.join() }
            saveJobs.values.toList().forEach { it.cancel() }
            sessionRepository.finish(session.id, confirmPartial)
                .onSuccess { finished ->
                    _uiState.update { it.copy(session = finished, isFinishing = false, isSaving = false) }
                    onFinished(finished.id)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isFinishing = false,
                            hasUnsyncedChanges = true,
                            error = error.localizedMessage ?: "Could not finish this workout."
                        )
                    }
                }
        }
    }

    fun retrySync() {
        val sessionId = _uiState.value.session?.id
        if (sessionId == null) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            load()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            sessionRepository.getSession(sessionId)
                .onSuccess { session ->
                    val hasPending = sessionRepository.hasPendingChanges(session.id)
                    _uiState.update {
                        it.copy(
                            session = session,
                            isSaving = false,
                            hasUnsyncedChanges = hasPending,
                            hasSyncConflict = sessionRepository.hasSyncConflict(session.id),
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasUnsyncedChanges = true,
                            error = error.localizedMessage ?: "Could not synchronize saved changes."
                        )
                    }
                }
        }
    }

    fun reapplyConflictedChanges() {
        val sessionId = _uiState.value.session?.id ?: return
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            sessionRepository.reapplyConflictedChanges(sessionId)
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            session = session,
                            isSaving = false,
                            hasUnsyncedChanges = sessionRepository.hasPendingChanges(session.id),
                            hasSyncConflict = sessionRepository.hasSyncConflict(session.id),
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasUnsyncedChanges = true,
                            hasSyncConflict = sessionRepository.hasSyncConflict(sessionId),
                            error = error.localizedMessage ?: "Could not reapply your local workout changes."
                        )
                    }
                }
        }
    }

    // --- Hold Timer Actions ---

    fun startHoldTimer(exercise: WorkoutSessionExercise, set: WorkoutSessionSet) {
        countdownTickerJob?.cancel()
        holdTickerJob?.cancel()
        restTickerJob?.cancel()
        notificationHelper.cancelRestNotification()

        val config = timerPreferences.getTimerConfig(exercise.exerciseName, exercise.restSeconds)

        if (config.countdownEnabled && config.countdownDurationSeconds > 0) {
            val totalSec = config.countdownDurationSeconds
            _uiState.update {
                it.copy(
                    activeTimer = ActiveTimerState(
                        status = TimerStatus.COUNTDOWN,
                        exerciseId = exercise.id,
                        setId = set.id,
                        exerciseName = exercise.exerciseName,
                        setNumber = set.setNumber,
                        targetHoldDurationSeconds = exercise.targetHoldDurationSeconds,
                        countdownTotalSeconds = totalSec,
                        countdownRemainingSeconds = totalSec
                    )
                )
            }
            startCountdownTicker(exercise, set, config)
        } else {
            startHolding(exercise, set, config)
        }
    }

    private fun startCountdownTicker(
        exercise: WorkoutSessionExercise,
        set: WorkoutSessionSet,
        config: ExerciseTimerConfig
    ) {
        val totalSec = config.countdownDurationSeconds
        val startMonotonic = timeSource.elapsedRealtime()
        val targetMonotonic = startMonotonic + totalSec * 1000L
        var lastAnnouncedSec = totalSec

        audioHapticHelper.playCountdownTick(config.soundEnabled, config.vibrationEnabled)

        countdownTickerJob?.cancel()
        countdownTickerJob = viewModelScope.launch {
            while (isActive) {
                val now = timeSource.elapsedRealtime()
                val remainingMillis = targetMonotonic - now
                val remainingSec = ((remainingMillis + 999) / 1000).toInt()

                if (remainingSec <= 0) {
                    startHolding(exercise, set, config)
                    break
                } else {
                    if (remainingSec < lastAnnouncedSec) {
                        lastAnnouncedSec = remainingSec
                        audioHapticHelper.playCountdownTick(config.soundEnabled, config.vibrationEnabled)
                    }
                    _uiState.update {
                        it.copy(
                            activeTimer = it.activeTimer.copy(
                                countdownRemainingSeconds = remainingSec
                            )
                        )
                    }
                    delay(100)
                }
            }
        }
    }

    private fun startHolding(
        exercise: WorkoutSessionExercise,
        set: WorkoutSessionSet,
        config: ExerciseTimerConfig
    ) {
        audioHapticHelper.playHoldStart(config.soundEnabled, config.vibrationEnabled)
        holdStartMonotonicMs = timeSource.elapsedRealtime()

        _uiState.update {
            it.copy(
                activeTimer = ActiveTimerState(
                    status = TimerStatus.HOLDING,
                    exerciseId = exercise.id,
                    setId = set.id,
                    exerciseName = exercise.exerciseName,
                    setNumber = set.setNumber,
                    targetHoldDurationSeconds = exercise.targetHoldDurationSeconds,
                    holdElapsedMillis = 0L
                )
            )
        }

        holdTickerJob?.cancel()
        holdTickerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = timeSource.elapsedRealtime() - holdStartMonotonicMs
                _uiState.update {
                    it.copy(
                        activeTimer = it.activeTimer.copy(
                            holdElapsedMillis = elapsed
                        )
                    )
                }
                delay(50)
            }
        }
    }

    fun stopHoldTimer() {
        val currentTimer = _uiState.value.activeTimer
        if (currentTimer.status != TimerStatus.HOLDING) return
        holdTickerJob?.cancel()

        val elapsed = timeSource.elapsedRealtime() - holdStartMonotonicMs
        val durationSec = elapsed / 1000.0
        val config = timerPreferences.getTimerConfig(currentTimer.exerciseName)

        audioHapticHelper.playHoldStop(config.soundEnabled, config.vibrationEnabled)

        _uiState.update {
            it.copy(
                activeTimer = it.activeTimer.copy(
                    status = TimerStatus.SET_COMPLETED,
                    holdElapsedMillis = elapsed,
                    finalHoldDurationSeconds = durationSec
                )
            )
        }
    }

    fun retryHoldTimer() {
        val currentTimer = _uiState.value.activeTimer
        val session = _uiState.value.session ?: return
        val exercise = session.exercises.firstOrNull { it.id == currentTimer.exerciseId } ?: return
        val set = exercise.sets.firstOrNull { it.id == currentTimer.setId } ?: return
        startHoldTimer(exercise, set)
    }

    fun cancelTimer() {
        countdownTickerJob?.cancel()
        holdTickerJob?.cancel()
        restTickerJob?.cancel()
        notificationHelper.cancelRestNotification()
        _uiState.update { it.copy(activeTimer = ActiveTimerState()) }
    }

    fun saveSetFromTimer(
        notes: String? = null,
        weightKg: Double? = null,
        assistanceWeightKg: Double? = null
    ) {
        val currentTimer = _uiState.value.activeTimer
        if (currentTimer.status != TimerStatus.SET_COMPLETED) return
        val session = _uiState.value.session ?: return
        val exercise = session.exercises.firstOrNull { it.id == currentTimer.exerciseId } ?: return
        val set = exercise.sets.firstOrNull { it.id == currentTimer.setId } ?: return

        val holdSec = Math.round(currentTimer.finalHoldDurationSeconds).toInt().coerceAtLeast(0)
        val update = WorkoutSessionSetUpdate(
            reps = set.reps,
            holdDurationSeconds = holdSec,
            weightKg = weightKg ?: set.weightKg,
            assistanceWeightKg = assistanceWeightKg ?: set.assistanceWeightKg,
            completed = true,
            notes = notes ?: set.notes
        )

        _uiState.update { it.copy(activeTimer = ActiveTimerState()) }
        updateSet(exercise.id, set.id, update, immediate = true)
    }

    // --- Rest Timer Actions ---

    private fun checkAndTriggerRestTimer(exerciseId: Long, setId: Long, session: WorkoutSession) {
        val exercise = session.exercises.firstOrNull { it.id == exerciseId } ?: return
        val config = timerPreferences.getTimerConfig(exercise.exerciseName, exercise.restSeconds)
        if (!config.restTimerEnabled) return

        val next = findNextUncompletedSet(session, exerciseId, setId) ?: return
        val (nextExercise, nextSet) = next

        triggerRestTimer(
            restDurationSeconds = config.restDurationSeconds,
            config = config,
            nextExerciseName = nextExercise.exerciseName,
            nextSetNumber = nextSet.setNumber
        )
    }

    fun triggerRestTimer(
        restDurationSeconds: Int,
        config: ExerciseTimerConfig,
        nextExerciseName: String,
        nextSetNumber: Int
    ) {
        countdownTickerJob?.cancel()
        holdTickerJob?.cancel()
        restTickerJob?.cancel()
        notificationHelper.cancelRestNotification()

        val totalSec = restDurationSeconds.coerceAtLeast(1)
        restTargetMonotonicMs = timeSource.elapsedRealtime() + totalSec * 1000L

        _uiState.update {
            it.copy(
                activeTimer = ActiveTimerState(
                    status = TimerStatus.RESTING,
                    restTotalSeconds = totalSec,
                    restRemainingSeconds = totalSec,
                    nextExerciseName = nextExerciseName,
                    nextSetNumber = nextSetNumber
                )
            )
        }

        startRestTicker(totalSec, config)
    }

    private fun startRestTicker(totalSec: Int, config: ExerciseTimerConfig) {
        restTickerJob?.cancel()
        restTickerJob = viewModelScope.launch {
            while (isActive) {
                val now = timeSource.elapsedRealtime()
                val remainingMillis = restTargetMonotonicMs - now
                val remainingSec = ((remainingMillis + 999) / 1000).toInt()

                if (remainingSec <= 0) {
                    _uiState.update {
                        it.copy(
                            activeTimer = it.activeTimer.copy(
                                status = TimerStatus.REST_COMPLETED,
                                restRemainingSeconds = 0
                            )
                        )
                    }
                    audioHapticHelper.playRestCompleted(config.soundEnabled, config.vibrationEnabled)
                    notificationHelper.showRestCompleteNotification(
                        _uiState.value.activeTimer.nextExerciseName,
                        _uiState.value.activeTimer.nextSetNumber
                    )
                    break
                } else {
                    _uiState.update {
                        it.copy(
                            activeTimer = it.activeTimer.copy(
                                restRemainingSeconds = remainingSec
                            )
                        )
                    }
                    delay(200)
                }
            }
        }
    }

    fun pauseRestTimer() {
        if (_uiState.value.activeTimer.status != TimerStatus.RESTING) return
        restTickerJob?.cancel()
        val now = timeSource.elapsedRealtime()
        restRemainingMillisOnPause = (restTargetMonotonicMs - now).coerceAtLeast(0L)
        _uiState.update {
            it.copy(
                activeTimer = it.activeTimer.copy(status = TimerStatus.REST_PAUSED)
            )
        }
    }

    fun resumeRestTimer() {
        if (_uiState.value.activeTimer.status != TimerStatus.REST_PAUSED) return
        val exerciseName = _uiState.value.activeTimer.nextExerciseName
        val config = timerPreferences.getTimerConfig(exerciseName)

        restTargetMonotonicMs = timeSource.elapsedRealtime() + restRemainingMillisOnPause
        _uiState.update {
            it.copy(
                activeTimer = it.activeTimer.copy(status = TimerStatus.RESTING)
            )
        }
        startRestTicker(_uiState.value.activeTimer.restTotalSeconds, config)
    }

    fun addRestSeconds(seconds: Int) {
        val currentTimer = _uiState.value.activeTimer
        val config = timerPreferences.getTimerConfig(currentTimer.nextExerciseName)

        when (currentTimer.status) {
            TimerStatus.RESTING -> {
                restTargetMonotonicMs += seconds * 1000L
                val now = timeSource.elapsedRealtime()
                val remaining = ((restTargetMonotonicMs - now + 999) / 1000).toInt().coerceAtLeast(0)
                val newTotal = currentTimer.restTotalSeconds + seconds
                _uiState.update {
                    it.copy(
                        activeTimer = it.activeTimer.copy(
                            restTotalSeconds = newTotal,
                            restRemainingSeconds = remaining
                        )
                    )
                }
            }
            TimerStatus.REST_PAUSED -> {
                restRemainingMillisOnPause += seconds * 1000L
                val remaining = ((restRemainingMillisOnPause + 999) / 1000).toInt().coerceAtLeast(0)
                val newTotal = currentTimer.restTotalSeconds + seconds
                _uiState.update {
                    it.copy(
                        activeTimer = it.activeTimer.copy(
                            restTotalSeconds = newTotal,
                            restRemainingSeconds = remaining
                        )
                    )
                }
            }
            TimerStatus.REST_COMPLETED -> {
                notificationHelper.cancelRestNotification()
                restTargetMonotonicMs = timeSource.elapsedRealtime() + seconds * 1000L
                _uiState.update {
                    it.copy(
                        activeTimer = it.activeTimer.copy(
                            status = TimerStatus.RESTING,
                            restTotalSeconds = seconds,
                            restRemainingSeconds = seconds
                        )
                    )
                }
                startRestTicker(seconds, config)
            }
            else -> Unit
        }
    }

    fun skipRestTimer() {
        restTickerJob?.cancel()
        notificationHelper.cancelRestNotification()
        _uiState.update { it.copy(activeTimer = ActiveTimerState()) }
    }

    fun startNextSet() {
        restTickerJob?.cancel()
        notificationHelper.cancelRestNotification()
        _uiState.update { it.copy(activeTimer = ActiveTimerState()) }
    }

    fun onAppForegrounded() {
        val currentTimer = _uiState.value.activeTimer
        if (currentTimer.status == TimerStatus.RESTING) {
            val now = timeSource.elapsedRealtime()
            val remainingMillis = restTargetMonotonicMs - now
            val remainingSec = ((remainingMillis + 999) / 1000).toInt()
            if (remainingSec <= 0) {
                val config = timerPreferences.getTimerConfig(currentTimer.nextExerciseName)
                _uiState.update {
                    it.copy(
                        activeTimer = it.activeTimer.copy(
                            status = TimerStatus.REST_COMPLETED,
                            restRemainingSeconds = 0
                        )
                    )
                }
                audioHapticHelper.playRestCompleted(config.soundEnabled, config.vibrationEnabled)
                notificationHelper.showRestCompleteNotification(
                    currentTimer.nextExerciseName,
                    currentTimer.nextSetNumber
                )
            } else {
                _uiState.update {
                    it.copy(
                        activeTimer = it.activeTimer.copy(
                            restRemainingSeconds = remainingSec
                        )
                    )
                }
            }
        }
    }

    // --- Settings Actions ---

    fun openTimerSettings(exerciseName: String, fallbackRestSeconds: Int? = null) {
        val config = timerPreferences.getTimerConfig(exerciseName, fallbackRestSeconds)
        _uiState.update {
            it.copy(
                isTimerSettingsDialogVisible = true,
                activeTimerConfig = config,
                activeConfigExerciseName = exerciseName
            )
        }
    }

    fun dismissTimerSettings() {
        _uiState.update { it.copy(isTimerSettingsDialogVisible = false) }
    }

    fun saveTimerConfig(config: ExerciseTimerConfig) {
        val exerciseName = _uiState.value.activeConfigExerciseName
        if (exerciseName.isNotBlank()) {
            timerPreferences.saveTimerConfig(exerciseName, config)
        }
        _uiState.update {
            it.copy(
                isTimerSettingsDialogVisible = false,
                activeTimerConfig = config
            )
        }
    }

    // --- Helpers ---

    private fun findNextUncompletedSet(
        session: WorkoutSession,
        currentExerciseId: Long,
        currentSetId: Long
    ): Pair<WorkoutSessionExercise, WorkoutSessionSet>? {
        val activeExercises = session.exercises.filter { it.status != "SKIPPED" }.sortedBy { it.orderIndex }
        val currentExercise = activeExercises.firstOrNull { it.id == currentExerciseId }

        val nextInCurrent = currentExercise?.sets?.sortedBy { it.setNumber }
            ?.firstOrNull { !it.completed && it.id != currentSetId }
        if (currentExercise != null && nextInCurrent != null) {
            return currentExercise to nextInCurrent
        }

        val subsequent = activeExercises.filter { it.orderIndex > (currentExercise?.orderIndex ?: -1) }
        for (ex in subsequent) {
            val nextSet = ex.sets.sortedBy { it.setNumber }.firstOrNull { !it.completed }
            if (nextSet != null) {
                return ex to nextSet
            }
        }

        for (ex in activeExercises) {
            val nextSet = ex.sets.sortedBy { it.setNumber }.firstOrNull { !it.completed && it.id != currentSetId }
            if (nextSet != null) {
                return ex to nextSet
            }
        }

        return null
    }

    fun stopElapsedTimer() {
        timerJob?.cancel()
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val startedAt = _uiState.value.session?.startedAt
                _uiState.update { it.copy(elapsedSeconds = elapsedSeconds(startedAt)) }
                delay(1_000)
            }
        }
    }

    private fun elapsedSeconds(startedAt: String?): Long {
        val start = startedAt?.let { runCatching { ZonedDateTime.parse(it).toEpochSecond() }.getOrNull() } ?: return 0
        return (System.currentTimeMillis() / 1000 - start).coerceAtLeast(0)
    }

    private fun mergeServerResponse(
        response: WorkoutSession,
        current: WorkoutSession?,
        stillSaving: Set<Long>
    ): WorkoutSession {
        if (current == null || stillSaving.isEmpty()) return response
        val localSets = current.exercises.flatMap { it.sets }.filter { it.id in stillSaving }.associateBy { it.id }
        return response.copy(exercises = response.exercises.map { exercise ->
            val currentExercise = current.exercises.firstOrNull { it.id == exercise.id }
            exercise.copy(sets = exercise.sets.map { localSets[it.id] ?: it })
                .let { merged ->
                    if (currentExercise != null && currentExercise.sets.any { it.id in stillSaving }) {
                        merged.copy(status = currentExercise.status)
                    } else {
                        merged
                    }
                }
        })
    }

    fun setLoggingMode(mode: SetLoggingMode) {
        _uiState.update { it.copy(loggingMode = mode) }
        viewModelScope.launch {
            workoutExperiencePreferences?.setSetLoggingMode(mode)
        }
    }

    fun selectExercise(index: Int) {
        val exercises = _uiState.value.session?.exercises ?: return
        if (exercises.isEmpty()) return
        val clamped = index.coerceIn(0, exercises.size - 1)
        _uiState.update {
            it.copy(
                selectedExerciseIndex = clamped,
                selectedSetIndex = 0
            )
        }
    }

    fun selectSet(index: Int) {
        val session = _uiState.value.session ?: return
        val currentEx = session.exercises.getOrNull(_uiState.value.selectedExerciseIndex) ?: return
        if (currentEx.sets.isEmpty()) return
        val clamped = index.coerceIn(0, currentEx.sets.size - 1)
        _uiState.update { it.copy(selectedSetIndex = clamped) }
    }

    fun nextSet() {
        val session = _uiState.value.session ?: return
        val currentEx = session.exercises.getOrNull(_uiState.value.selectedExerciseIndex) ?: return
        if (_uiState.value.selectedSetIndex < currentEx.sets.size - 1) {
            _uiState.update { it.copy(selectedSetIndex = it.selectedSetIndex + 1) }
        }
    }

    fun prevSet() {
        if (_uiState.value.selectedSetIndex > 0) {
            _uiState.update { it.copy(selectedSetIndex = it.selectedSetIndex - 1) }
        }
    }

    fun nextExercise() {
        val session = _uiState.value.session ?: return
        if (_uiState.value.selectedExerciseIndex < session.exercises.size - 1) {
            _uiState.update {
                it.copy(
                    selectedExerciseIndex = it.selectedExerciseIndex + 1,
                    selectedSetIndex = 0
                )
            }
        }
    }

    fun prevExercise() {
        if (_uiState.value.selectedExerciseIndex > 0) {
            _uiState.update {
                it.copy(
                    selectedExerciseIndex = it.selectedExerciseIndex - 1,
                    selectedSetIndex = 0
                )
            }
        }
    }

    fun completeSetAndAdvance(
        exerciseId: Long,
        setId: Long,
        update: WorkoutSessionSetUpdate
    ) {
        updateSet(exerciseId, setId, update.copy(completed = true), immediate = true)

        val session = _uiState.value.session ?: return
        val exIndex = session.exercises.indexOfFirst { it.id == exerciseId }
        if (exIndex == -1) return
        val exercise = session.exercises[exIndex]
        val setIndex = exercise.sets.indexOfFirst { it.id == setId }

        if (setIndex in 0 until exercise.sets.size - 1) {
            _uiState.update {
                it.copy(
                    selectedExerciseIndex = exIndex,
                    selectedSetIndex = setIndex + 1
                )
            }
        } else if (exIndex in 0 until session.exercises.size - 1) {
            _uiState.update {
                it.copy(
                    selectedExerciseIndex = exIndex + 1,
                    selectedSetIndex = 0
                )
            }
        }
    }

    fun addSet(exerciseId: Long) {
        val current = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            sessionRepository.addSet(current.id, exerciseId)
                .onSuccess { updatedSession ->
                    val exIndex = updatedSession.exercises.indexOfFirst { it.id == exerciseId }
                    val targetEx = updatedSession.exercises.getOrNull(exIndex)
                    val newSetIndex = (targetEx?.sets?.size ?: 1) - 1
                    _uiState.update {
                        it.copy(
                            session = updatedSession,
                            isSaving = false,
                            selectedExerciseIndex = if (exIndex != -1) exIndex else it.selectedExerciseIndex,
                            selectedSetIndex = newSetIndex.coerceAtLeast(0)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.localizedMessage ?: "Could not add set."
                        )
                    }
                }
        }
    }

    fun deleteSet(exerciseId: Long, setId: Long) {
        val current = _uiState.value.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            sessionRepository.deleteSet(current.id, exerciseId, setId)
                .onSuccess { updatedSession ->
                    val exIndex = updatedSession.exercises.indexOfFirst { it.id == exerciseId }
                    val targetEx = updatedSession.exercises.getOrNull(exIndex)
                    val setsCount = targetEx?.sets?.size ?: 1
                    _uiState.update {
                        it.copy(
                            session = updatedSession,
                            isSaving = false,
                            selectedSetIndex = it.selectedSetIndex.coerceIn(0, (setsCount - 1).coerceAtLeast(0))
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.localizedMessage ?: "Could not delete set."
                        )
                    }
                }
        }
    }

    public override fun onCleared() {
        super.onCleared()
        countdownTickerJob?.cancel()
        holdTickerJob?.cancel()
        restTickerJob?.cancel()
        timerJob?.cancel()
        notificationHelper.cancelRestNotification()
    }
}
