package com.calisthenics.ai.ui.liveanalysis

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.domain.model.LiveAnalysisResult
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class LiveAnalysisUiState(
    val exerciseCode: String = "PLANCHE",
    val sessionId: String = UUID.randomUUID().toString().substring(0, 12),
    val isRunning: Boolean = false,
    val showSkeleton: Boolean = true,
    val flashEnabled: Boolean = false,
    val usingFrontCamera: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val liveResult: LiveAnalysisResult? = null
)

@HiltViewModel
class LiveAnalysisViewModel @Inject constructor(
    private val repository: LiveAnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveAnalysisUiState())
    val uiState: StateFlow<LiveAnalysisUiState> = _uiState.asStateFlow()

    private var lastFrameAtMs: Long = 0L

    fun setExercise(code: String) {
        _uiState.update {
            it.copy(
                exerciseCode = code,
                sessionId = it.sessionId.ifBlank { UUID.randomUUID().toString().substring(0, 12) }
            )
        }
    }

    fun toggleRunning() {
        _uiState.update { it.copy(isRunning = !it.isRunning) }
    }

    fun setRunning(running: Boolean) {
        _uiState.update { it.copy(isRunning = running) }
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
        _uiState.update {
            it.copy(
                sessionId = UUID.randomUUID().toString().substring(0, 12),
                liveResult = null,
                error = null,
                isLoading = false
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
        val now = SystemClock.elapsedRealtime()
        if (now - lastFrameAtMs < 200L && lastFrameAtMs != 0L) {
            imageFile.delete()
            return
        }
        lastFrameAtMs = now

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            liveResult = analysis,
                            error = null
                        )
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Live analysis request failed."
                        )
                    }
                    onNetworkFailure?.invoke()
                }
            } finally {
                imageFile.delete()
            }
        }
    }
}
