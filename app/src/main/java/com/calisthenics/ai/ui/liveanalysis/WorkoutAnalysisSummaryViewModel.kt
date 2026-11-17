package com.calisthenics.ai.ui.liveanalysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.domain.model.AiPerformanceSummary
import com.calisthenics.ai.domain.model.LiveRepRecord
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class WorkoutAnalysisSummaryUiState(
    val isLoading: Boolean = true,
    val session: LiveSessionRecord? = null,
    val error: String? = null,
    val selectedRep: LiveRepRecord? = null,
    val isAiLoading: Boolean = false,
    val aiSummary: AiPerformanceSummary? = null,
    val aiStatus: String = "PENDING" // PENDING, GENERATING, COMPLETED, UNAVAILABLE
)

@HiltViewModel
class WorkoutAnalysisSummaryViewModel @Inject constructor(
    private val repository: LiveAnalysisRepository,
    private val snapshotCache: LiveSessionSnapshotCache,
    savedStateHandle: SavedStateHandle,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId") ?: ""

    private val _uiState = MutableStateFlow(WorkoutAnalysisSummaryUiState())
    val uiState: StateFlow<WorkoutAnalysisSummaryUiState> = _uiState.asStateFlow()

    private var aiRequestedOnce = false

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = {
            aiRequestedOnce = false
            _uiState.value = WorkoutAnalysisSummaryUiState(isLoading = false)
        }
    )

    init {
        sessionGuard.startObserving()
        loadSummary()
    }

    fun loadSummary() {
        if (sessionId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Session ID is missing.") }
            return
        }

        // Immediately hydrate from in-memory snapshot if available (zero blocking delay)
        val cachedSnapshot = snapshotCache.get(sessionId)
        if (cachedSnapshot != null) {
            val cachedAi = cachedSnapshot.aiRecommendations
            val alreadyHasAi = cachedSnapshot.aiSummaryStatus.equals("COMPLETED", ignoreCase = true) && cachedAi != null
            _uiState.update {
                it.copy(
                    isLoading = false,
                    session = cachedSnapshot,
                    error = null,
                    aiSummary = cachedAi,
                    aiStatus = if (alreadyHasAi) "COMPLETED" else cachedSnapshot.aiSummaryStatus,
                    isAiLoading = false
                )
            }
            if (alreadyHasAi) {
                aiRequestedOnce = true
            }
        } else {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }

        viewModelScope.launch {
            // If backend completion request is still running after live Stop press, wait before querying DB / insights
            snapshotCache.awaitCompletionIfInFlight(sessionId)

            repository.getSessionDetails(sessionId)
                .onSuccess { persisted ->
                    val merged = mergePreservingSnapshotIfNeeded(persisted, snapshotCache.get(sessionId))
                    snapshotCache.put(merged)

                    val cachedAi = merged.aiRecommendations
                    val isAiDone = merged.aiSummaryStatus.equals("COMPLETED", ignoreCase = true) && cachedAi != null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = merged,
                            error = null,
                            aiSummary = cachedAi ?: it.aiSummary,
                            aiStatus = if (isAiDone) "COMPLETED" else merged.aiSummaryStatus
                        )
                    }

                    if (isAiDone) {
                        aiRequestedOnce = true
                    } else if (!aiRequestedOnce) {
                        fetchAiSummaryAsync()
                    }
                }
                .onFailure { throwable ->
                    val fallbackSnapshot = _uiState.value.session ?: snapshotCache.get(sessionId)
                    if (fallbackSnapshot != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                aiStatus = if (it.aiSummary == null) "UNAVAILABLE" else it.aiStatus
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.localizedMessage ?: "Could not load workout summary."
                            )
                        }
                    }
                }
        }
    }

    fun fetchAiSummaryAsync(forceRetry: Boolean = false) {
        if (sessionId.isBlank()) return
        if (aiRequestedOnce && !forceRetry) return
        val currentSession = _uiState.value.session
        if (currentSession?.aiSummaryStatus.equals("COMPLETED", ignoreCase = true) &&
            currentSession?.aiRecommendations != null
        ) {
            aiRequestedOnce = true
            return
        }

        aiRequestedOnce = true
        _uiState.update { it.copy(isAiLoading = true, aiStatus = "GENERATING") }

        viewModelScope.launch {
            repository.generateSessionAiSummary(sessionId)
                .onSuccess { aiSummary ->
                    val resolvedStatus = aiSummary.status.uppercase(Locale.US)
                    _uiState.update { state ->
                        val updatedSession = state.session?.copy(
                            aiSummary = aiSummary.overallAssessment,
                            aiRecommendations = aiSummary,
                            aiSummaryStatus = resolvedStatus
                        )
                        if (updatedSession != null) {
                            snapshotCache.put(updatedSession)
                        }
                        state.copy(
                            session = updatedSession ?: state.session,
                            isAiLoading = false,
                            aiSummary = aiSummary,
                            aiStatus = resolvedStatus
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            aiStatus = "UNAVAILABLE"
                        )
                    }
                }
        }
    }

    fun selectRep(rep: LiveRepRecord?) {
        _uiState.update { it.copy(selectedRep = rep) }
    }

    fun buildShareSummaryText(session: LiveSessionRecord): String = buildString {
        append("Calisthenics AI Workout Summary\n")
        append("Exercise: ${session.exerciseDisplayName}\n")
        append("Duration: ${formatSummaryDuration(session.durationSeconds)}\n")

        val overall = session.resolvedOverallScore
        append("Overall Form Score: ${overall?.let { String.format(Locale.US, "%.1f/100", it) } ?: "Unavailable"}\n")

        if (session.totalReps > 0) {
            append("Total Reps: ${session.totalReps} (${session.correctReps} valid / ${session.incorrectReps} failed)\n")
        } else if (session.peakHoldDurationSeconds != null && session.peakHoldDurationSeconds > 0) {
            append("Peak Hold: ${String.format(Locale.US, "%.1fs", session.peakHoldDurationSeconds)}\n")
        } else {
            append("Total Reps: 0\n")
        }

        session.analysisConfidence?.let { conf ->
            append("Analysis Confidence: ${(conf * 100).toInt()}%\n")
        }

        val validMetrics = session.exerciseMetrics.filter { it.score != null && !it.status.equals("UNAVAILABLE", ignoreCase = true) }
        if (validMetrics.isNotEmpty()) {
            append("\nForm Analysis:\n")
            for (m in validMetrics) {
                val valStr = if (m.value != null && m.unit.isNotBlank()) " (${String.format(Locale.US, "%.1f", m.value)}${m.unit})" else ""
                append("- ${m.label}: ${m.score?.toInt()}%$valStr\n")
            }
        }

        if (session.detectedErrors.isNotEmpty()) {
            append("\nForm Issues:\n")
            for (err in session.detectedErrors) {
                append("- ${err.errorName}: ${err.occurrences}x [${err.severity.name}]\n")
            }
        }

        val ai = session.aiRecommendations
        if (ai != null && ai.status.equals("COMPLETED", ignoreCase = true) && !ai.overallAssessment.isNullOrBlank()) {
            append("\nAI Coach Assessment:\n${ai.overallAssessment}\n")
            if (ai.recommendations.isNotEmpty()) {
                append("Recommendations:\n")
                ai.recommendations.forEachIndexed { idx, rec ->
                    append("${idx + 1}. $rec\n")
                }
            }
        }
    }.trimEnd()

    private fun mergePreservingSnapshotIfNeeded(
        persisted: LiveSessionRecord,
        snapshot: LiveSessionRecord?
    ): LiveSessionRecord {
        if (snapshot == null) return persisted
        return persisted.copy(
            exerciseMetrics = persisted.exerciseMetrics.ifEmpty { snapshot.exerciseMetrics },
            detectedErrors = persisted.detectedErrors.ifEmpty { snapshot.detectedErrors },
            overallScore = persisted.overallScore ?: snapshot.overallScore,
            analysisConfidence = persisted.analysisConfidence ?: snapshot.analysisConfidence,
            peakHoldDurationSeconds = persisted.peakHoldDurationSeconds ?: snapshot.peakHoldDurationSeconds,
            reps = persisted.reps.ifEmpty { snapshot.reps }
        )
    }

    private fun formatSummaryDuration(seconds: Double): String {
        val total = seconds.toLong().coerceAtLeast(0L)
        val mins = total / 60
        val secs = total % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
