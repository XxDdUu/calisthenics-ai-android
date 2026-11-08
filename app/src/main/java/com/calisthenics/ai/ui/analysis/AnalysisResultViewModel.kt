package com.calisthenics.ai.ui.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.KeypointFrameModel
import com.calisthenics.ai.domain.usecase.GetAnalysisByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

enum class VideoDisplayMode {
    ORIGINAL,
    ANNOTATED
}

data class AnalysisResultUiState(
    val isLoading: Boolean = false,
    val result: AnalysisResult? = null,
    val error: String? = null,
    val authToken: String? = null,
    val selectedVideoMode: VideoDisplayMode = VideoDisplayMode.ANNOTATED,
    val currentPlaybackPositionMs: Long = 0L,
    val currentKeypointFrame: KeypointFrameModel? = null,
    val showSkeletonOnOriginal: Boolean = true,
    val isFullscreen: Boolean = false
)

@HiltViewModel
class AnalysisResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnalysisByIdUseCase: GetAnalysisByIdUseCase,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val analysisId: Long = savedStateHandle.get<String>("analysisId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(
        AnalysisResultUiState(authToken = tokenManager.getAccessToken())
    )
    val uiState: StateFlow<AnalysisResultUiState> = _uiState.asStateFlow()

    private val _seekRequestMs = MutableStateFlow<Long?>(null)
    val seekRequestMs: StateFlow<Long?> = _seekRequestMs.asStateFlow()

    init {
        loadAnalysisResult()
    }

    fun loadAnalysisResult() {
        if (analysisId <= 0L) {
            _uiState.update { it.copy(error = "Invalid analysis ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val token = tokenManager.getAccessToken()

            var attempts = 0
            val maxAttempts = 30
            var finalResult: AnalysisResult? = null

            while (attempts < maxAttempts) {
                val fetchResult = getAnalysisByIdUseCase(analysisId)
                if (fetchResult.isSuccess) {
                    val analysisResult = fetchResult.getOrThrow()
                    if (analysisResult.status == "PROCESSING" || analysisResult.status == "PENDING") {
                        _uiState.update { it.copy(isLoading = true, result = analysisResult, authToken = token) }
                        kotlinx.coroutines.delay(2000)
                        attempts++
                        continue
                    } else {
                        finalResult = analysisResult
                        break
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = fetchResult.exceptionOrNull()?.localizedMessage ?: "Failed to retrieve analysis report"
                        )
                    }
                    return@launch
                }
            }

            if (finalResult != null) {
                val firstFrame = finalResult.keypointsData?.frames?.firstOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = finalResult,
                        authToken = token,
                        currentKeypointFrame = firstFrame,
                        currentPlaybackPositionMs = 0L
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Analysis is still processing. Please refresh in a moment."
                    )
                }
            }
        }
    }

    fun setVideoMode(mode: VideoDisplayMode) {
        _uiState.update { it.copy(selectedVideoMode = mode) }
    }

    fun toggleSkeletonOnOriginal() {
        _uiState.update { it.copy(showSkeletonOnOriginal = !it.showSkeletonOnOriginal) }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }

    fun requestSeek(timestampMs: Long) {
        _seekRequestMs.value = timestampMs
    }

    fun clearSeekRequest() {
        _seekRequestMs.value = null
    }

    fun updatePlaybackPosition(positionMs: Long) {
        val frames = _uiState.value.result?.keypointsData?.frames
        val matchingFrame = if (!frames.isNullOrEmpty()) {
            frames.minByOrNull { abs(it.timestampMs - positionMs) }
        } else null

        _uiState.update {
            it.copy(
                currentPlaybackPositionMs = positionMs,
                currentKeypointFrame = matchingFrame ?: it.currentKeypointFrame
            )
        }
    }
}
