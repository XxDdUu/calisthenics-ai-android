package com.calisthenics.ai.ui.liveanalysis

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.domain.model.LiveAnalysisResult
import com.calisthenics.ai.domain.model.LiveSessionSummary
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class LiveExerciseOption(
    val code: String,
    val displayName: String,
    val category: String,
    val recommendedView: String,
    val sideView: Boolean = true,
    val variations: List<String> = emptyList()
)

val DEFAULT_LIVE_EXERCISES: List<LiveExerciseOption> = listOf(
    LiveExerciseOption(
        code = "PUSH_UP",
        displayName = "Push-up",
        category = "REPETITION",
        recommendedView = "Side-on (45° or 90°) view at chest height",
        sideView = true,
        variations = listOf("STANDARD", "DIAMOND", "WIDE")
    ),
    LiveExerciseOption(
        code = "SQUAT",
        displayName = "Squat",
        category = "REPETITION",
        recommendedView = "Side or 45-degree angle showing full body",
        sideView = true,
        variations = listOf("REGULAR", "DEEP_SQUAT", "PISTOL")
    ),
    LiveExerciseOption(
        code = "PULL_UP",
        displayName = "Pull-up",
        category = "REPETITION",
        recommendedView = "Front or side view showing overhead bar",
        sideView = false,
        variations = listOf("OVERHAND", "CHIN_UP", "WIDE_GRIP")
    ),
    LiveExerciseOption(
        code = "DIPS",
        displayName = "Parallel Bar Dips",
        category = "REPETITION",
        recommendedView = "Side-on view showing torso and elbows",
        sideView = true,
        variations = listOf("BAR_DIP", "RING_DIP")
    ),
    LiveExerciseOption(
        code = "HANDSTAND",
        displayName = "Handstand & HSPU",
        category = "HOLD_AND_REPETITION",
        recommendedView = "Side or front-diagonal view showing full inversion",
        sideView = true,
        variations = listOf("FREESTANDING", "WALL_ASSISTED", "HSPU")
    ),
    LiveExerciseOption(
        code = "PLANCHE",
        displayName = "Planche",
        category = "ISOMETRIC_HOLD",
        recommendedView = "Side-on (profile) view at floor height",
        sideView = true,
        variations = listOf("TUCK", "ADVANCED_TUCK", "STRADDLE", "FULL")
    ),
    LiveExerciseOption(
        code = "FRONT_LEVER",
        displayName = "Front Lever",
        category = "ISOMETRIC_HOLD",
        recommendedView = "Side-on (profile) view at bar height",
        sideView = true,
        variations = listOf("TUCK", "ADVANCED_TUCK", "STRADDLE", "FULL")
    ),
    LiveExerciseOption(
        code = "PIKE_PUSH_UP",
        displayName = "Pike Push-up",
        category = "REPETITION",
        recommendedView = "Side-on view showing hips, shoulders, wrists",
        sideView = true,
        variations = listOf("FLOOR_PIKE", "ELEVATED_FEET")
    ),
    LiveExerciseOption(
        code = "L_SIT",
        displayName = "L-sit",
        category = "ISOMETRIC_HOLD",
        recommendedView = "Side-on (profile) view at floor height",
        sideView = true,
        variations = listOf("TUCK_L_SIT", "FULL_L_SIT", "V_SIT")
    ),
    LiveExerciseOption(
        code = "MUSCLE_UP",
        displayName = "Muscle-up",
        category = "REPETITION",
        recommendedView = "Front or side view showing bar transition",
        sideView = false,
        variations = listOf("BAR_MUSCLE_UP", "RING_MUSCLE_UP")
    )
)

data class LiveAnalysisUiState(
    val exerciseCode: String = "PLANCHE",
    val availableExercises: List<LiveExerciseOption> = DEFAULT_LIVE_EXERCISES,
    val isExerciseDropdownExpanded: Boolean = false,
    val sessionId: String = UUID.randomUUID().toString().substring(0, 12),
    val isRunning: Boolean = false,
    val showSkeleton: Boolean = true,
    val flashEnabled: Boolean = false,
    val usingFrontCamera: Boolean = false,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val liveResult: LiveAnalysisResult? = null,
    val showSummaryDialog: Boolean = false,
    val lastSessionSummary: LiveSessionSummary? = null,
    val completedRepCount: Int = 0,
    val completedSessionId: String? = null,
    val completedExerciseName: String = "",
    val showCompletionDialog: Boolean = false,
    val navigateToSummarySessionId: String? = null,
    val isSavingHistory: Boolean = false,
    val historySaved: Boolean = false,
    val historySaveError: String? = null,
    val isProRequiredError: Boolean = false
) {
    val currentExerciseOption: LiveExerciseOption
        get() = availableExercises.find { it.code.equals(exerciseCode, ignoreCase = true) }
            ?: DEFAULT_LIVE_EXERCISES.firstOrNull { it.code.equals(exerciseCode, ignoreCase = true) }
            ?: LiveExerciseOption(
                code = exerciseCode,
                displayName = exerciseCode.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() },
                category = "REPETITION",
                recommendedView = "Side-on view recommended",
                sideView = true
            )

    val isHoldExercise: Boolean
        get() = currentExerciseOption.category.contains("ISOMETRIC", ignoreCase = true)
}

@HiltViewModel
class LiveAnalysisViewModel @Inject constructor(
    private val repository: LiveAnalysisRepository,
    private val snapshotCache: LiveSessionSnapshotCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveAnalysisUiState())
    val uiState: StateFlow<LiveAnalysisUiState> = _uiState.asStateFlow()

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private var lastFrameAtMs: Long = 0L
    private var lastProcessedFrameSequence: Long = -1L
    internal var timeProvider: () -> Long = { SystemClock.elapsedRealtime() }
    private var sessionStartTimeMs: Long = 0L
    private val accumulator = LiveSessionSummaryAccumulator(exerciseCode = _uiState.value.exerciseCode)

    init {
        loadCatalog()
    }

    private fun startSession(exerciseCode: String, sessionId: String) {
        sessionStartTimeMs = timeProvider()
        accumulator.reset(exerciseCode = exerciseCode, startTimeMs = sessionStartTimeMs)
        viewModelScope.launch {
            repository.createSession(exerciseCode = exerciseCode, sessionId = sessionId)
                .onFailure { throwable ->
                    handleLiveApiFailure(throwable)
                }
        }
    }

    private fun finishSession(sessionId: String) {
        val duration = if (sessionStartTimeMs > 0L) {
            ((timeProvider() - sessionStartTimeMs) / 1000.0).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val snapshot = accumulator.buildSessionSnapshot(
            sessionId = sessionId,
            durationSeconds = duration,
            isHoldExercise = _uiState.value.isHoldExercise
        )
        // Store snapshot immediately so WorkoutAnalysisSummaryScreen displays instantly without blocking
        snapshotCache.put(snapshot)
        snapshotCache.markCompletionStarted(sessionId)

        _uiState.update { it.copy(isSavingHistory = true, historySaved = false, historySaveError = null) }
        viewModelScope.launch {
            repository.completeSession(
                sessionId = sessionId,
                totalReps = snapshot.totalReps,
                durationSeconds = snapshot.durationSeconds,
                overallScore = snapshot.overallScore,
                averageFormScore = snapshot.averageScore,
                bestFormScore = snapshot.peakScore,
                lowestFormScore = snapshot.lowestScore,
                analysisConfidence = snapshot.analysisConfidence,
                peakHoldDurationSeconds = snapshot.peakHoldDurationSeconds,
                averageKneeAngle = snapshot.averageKneeAngle,
                deepestKneeAngle = snapshot.deepestKneeAngle,
                averageJointAngle = snapshot.averageJointAngle,
                deepestJointAngle = snapshot.deepestJointAngle,
                correctReps = snapshot.correctReps,
                incorrectReps = snapshot.incorrectReps,
                exerciseMetrics = snapshot.exerciseMetrics,
                detectedErrors = snapshot.detectedErrors,
                originalVideoUrl = snapshot.originalVideoUrl,
                annotatedVideoUrl = snapshot.annotatedVideoUrl,
                reps = snapshot.reps
            ).onSuccess { savedRecord ->
                snapshotCache.put(savedRecord)
                snapshotCache.markCompletionFinished(sessionId, true)
                _uiState.update { it.copy(isSavingHistory = false, historySaved = true) }
            }.onFailure { err ->
                snapshotCache.markCompletionFinished(sessionId, false)
                _uiState.update { it.copy(isSavingHistory = false, historySaveError = err.localizedMessage) }
            }
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            repository.getSupportedExercises().onSuccess { catalog ->
                if (catalog.isNotEmpty()) {
                    val updated = DEFAULT_LIVE_EXERCISES.map { def ->
                        val remote = catalog.find { it.exercise.equals(def.code, ignoreCase = true) }
                        if (remote != null) {
                            def.copy(
                                displayName = if (remote.name.isNotBlank()) remote.name else def.displayName,
                                category = if (remote.type.isNotBlank()) remote.type.uppercase() else def.category,
                                recommendedView = if (remote.cameraView.isNotBlank()) remote.cameraView else def.recommendedView,
                                variations = if (remote.variations.isNotEmpty()) remote.variations else def.variations
                            )
                        } else {
                            def
                        }
                    }
                    _uiState.update { it.copy(availableExercises = updated) }
                }
            }.onFailure { throwable ->
                if (NetworkErrorParser.httpStatusCode(throwable) == 403) {
                    handleLiveApiFailure(throwable)
                }
            }
        }
    }

    fun setExercise(code: String) {
        val normalized = code.trim().uppercase()
        if (_uiState.value.exerciseCode.equals(normalized, ignoreCase = true)) return
        lastFrameAtMs = 0L
        lastProcessedFrameSequence = -1L
        accumulator.reset(normalized, timeProvider())
        _uiState.update {
            it.copy(
                exerciseCode = normalized,
                sessionId = UUID.randomUUID().toString().substring(0, 12),
                liveResult = null,
                error = null,
                isProRequiredError = false,
                lastSessionSummary = null,
                showSummaryDialog = false,
                navigateToSummarySessionId = null,
                isExerciseDropdownExpanded = false
            )
        }
    }

    fun selectExercise(option: LiveExerciseOption) {
        if (_uiState.value.exerciseCode.equals(option.code, ignoreCase = true)) {
            _uiState.update { it.copy(isExerciseDropdownExpanded = false) }
            return
        }
        lastFrameAtMs = 0L
        lastProcessedFrameSequence = -1L
        accumulator.reset(option.code, timeProvider())
        _uiState.update {
            it.copy(
                exerciseCode = option.code,
                sessionId = UUID.randomUUID().toString().substring(0, 12),
                liveResult = null,
                error = null,
                isProRequiredError = false,
                lastSessionSummary = null,
                showSummaryDialog = false,
                navigateToSummarySessionId = null,
                isExerciseDropdownExpanded = false
            )
        }
    }

    fun setDropdownExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isExerciseDropdownExpanded = expanded) }
    }

    fun toggleRunning() {
        setRunning(!_uiState.value.isRunning)
    }

    fun setRunning(running: Boolean) {
        if (!running) {
            val hadBeenRunning = _uiState.value.isRunning
            val currentSessionId = _uiState.value.sessionId
            val reps = accumulator.getTotalRepCount()
            val exerciseName = _uiState.value.currentExerciseOption.displayName
            if (hadBeenRunning) {
                finishSession(currentSessionId)
            }
            val summary = _uiState.value.liveResult?.sessionSummary
            _uiState.update {
                it.copy(
                    isRunning = false,
                    showSummaryDialog = false,
                    showCompletionDialog = false,
                    navigateToSummarySessionId = if (hadBeenRunning) currentSessionId else null,
                    completedRepCount = reps,
                    completedSessionId = currentSessionId,
                    completedExerciseName = exerciseName,
                    lastSessionSummary = summary ?: it.lastSessionSummary
                )
            }
        } else {
            val wasRunning = _uiState.value.isRunning
            _uiState.update {
                it.copy(
                    isRunning = true,
                    showCompletionDialog = false,
                    navigateToSummarySessionId = null,
                    isProRequiredError = false,
                    error = null
                )
            }
            if (!wasRunning) {
                startSession(_uiState.value.exerciseCode, _uiState.value.sessionId)
            }
        }
    }

    fun consumeSummaryNavigation() {
        _uiState.update { it.copy(navigateToSummarySessionId = null) }
    }

    fun dismissCompletionDialog() {
        _uiState.update { it.copy(showCompletionDialog = false) }
    }

    fun dismissSummaryDialog() {
        _uiState.update { it.copy(showSummaryDialog = false) }
    }

    fun dismissProRequiredError() {
        _uiState.update { it.copy(isProRequiredError = false) }
    }

    private fun handleLiveApiFailure(throwable: Throwable): Boolean {
        return when (NetworkErrorParser.httpStatusCode(throwable)) {
            401 -> {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        isProRequiredError = false,
                        error = "Session expired or unauthorized. Please re-authenticate."
                    )
                }
                true
            }
            403 -> {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        isRunning = false,
                        isProRequiredError = true,
                        error = "Live Analysis requires a PRO membership. Upgrade to PRO to unlock real-time camera analysis."
                    )
                }
                true
            }
            429 -> {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        isRunning = false,
                        isProRequiredError = false,
                        error = NetworkErrorParser.toUserMessage(throwable)
                    )
                }
                true
            }
            else -> false
        }
    }

    fun toggleSkeleton() {
        _uiState.update { it.copy(showSkeleton = !it.showSkeleton) }
    }

    fun toggleFlash() {
        _uiState.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun toggleCamera() {
        _uiState.update { it.copy(usingFrontCamera = !it.usingFrontCamera) }
    }

    fun resetSession() {
        lastFrameAtMs = 0L
        lastProcessedFrameSequence = -1L
        sessionStartTimeMs = timeProvider()
        val newSessionId = UUID.randomUUID().toString().substring(0, 12)
        accumulator.reset(_uiState.value.exerciseCode, sessionStartTimeMs)
        if (_uiState.value.isRunning) {
            startSession(_uiState.value.exerciseCode, newSessionId)
        }
        _uiState.update {
            it.copy(
                sessionId = newSessionId,
                liveResult = null,
                error = null,
                isProRequiredError = false,
                isLoading = false,
                showSummaryDialog = false,
                showCompletionDialog = false,
                navigateToSummarySessionId = null,
                completedRepCount = 0,
                completedSessionId = null,
                lastSessionSummary = null,
                isSavingHistory = false,
                historySaved = false,
                historySaveError = null
            )
        }
    }

    fun onFrameCaptured(
        imageFile: File,
        frameSequence: Long,
        timestampMs: Long,
        sideView: Boolean,
        exerciseCode: String,
        onNetworkFailure: (() -> Unit)? = null
    ) {
        val now = timeProvider()
        if (now - lastFrameAtMs < 200L && lastFrameAtMs != 0L) {
            imageFile.delete()
            return
        }
        lastFrameAtMs = now

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            try {
                val result = withContext(ioDispatcher) {
                    repository.analyzeFrame(
                        imageFile = imageFile,
                        exercise = exerciseCode,
                        sessionId = _uiState.value.sessionId,
                        frameSequence = frameSequence,
                        timestampMs = timestampMs,
                        sideView = sideView,
                        drawOverlay = _uiState.value.showSkeleton,
                        debug = false
                    )
                }

                result.onSuccess { analysis ->
                    val frameNow = timeProvider()
                    val newlyCompleted = accumulator.recordFrame(
                        analysis = analysis,
                        nowMs = frameNow,
                        isHoldExercise = _uiState.value.isHoldExercise
                    )
                    val currentSession = _uiState.value.sessionId
                    for (repRecord in newlyCompleted) {
                        viewModelScope.launch {
                            repository.recordRep(currentSession, repRecord)
                        }
                    }

                    if (frameSequence >= lastProcessedFrameSequence) {
                        lastProcessedFrameSequence = frameSequence
                        _uiState.update {
                            it.copy(
                                isAnalyzing = false,
                                liveResult = analysis,
                                lastSessionSummary = analysis.sessionSummary ?: it.lastSessionSummary,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isAnalyzing = false) }
                    }
                }.onFailure { throwable ->
                    if (!handleLiveApiFailure(throwable)) {
                        val errorMessage = NetworkErrorParser.toUserMessage(throwable)
                        _uiState.update {
                            it.copy(
                                isAnalyzing = false,
                                isProRequiredError = false,
                                error = errorMessage
                            )
                        }
                    }
                    onNetworkFailure?.invoke()
                }
            } finally {
                imageFile.delete()
            }
        }
    }
}
