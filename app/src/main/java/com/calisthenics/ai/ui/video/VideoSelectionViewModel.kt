package com.calisthenics.ai.ui.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.QuotaEntitlement
import com.calisthenics.ai.domain.repository.AnalysisRepository
import com.calisthenics.ai.domain.usecase.AnalyzeImageUseCase
import com.calisthenics.ai.domain.usecase.AnalyzeVideoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.UUID
import javax.inject.Inject

enum class UploadProgressStep(
    val stepIndex: Int,
    val label: String,
    val progress: Float
) {
    IDLE(0, "", 0f),
    UPLOADING(1, "1. Uploading video", 0.30f),
    POSE_DETECTION(2, "2. AI Pose Detection", 0.62f),
    BIOMECHANICAL_ANALYSIS(3, "3. Biomechanical Analysis", 0.88f),
    COMPLETED(4, "Completed", 1.0f)
}

data class RecordingGuidance(
    val title: String,
    val cameraAngle: String,
    val distance: String,
    val framing: String,
    val lighting: String
)

data class VideoSelectionUiState(
    val exerciseId: Long = 0L,
    val exerciseCode: String = "",
    val progressionName: String = "",
    val sideView: Boolean = true,
    val selectedVideoUri: Uri? = null,
    val videoFileName: String? = null,
    val videoFileSizeFormatted: String? = null,
    val videoDurationMs: Long? = null,
    val videoDurationFormatted: String? = null,
    val mediaType: String = "VIDEO",
    val idempotencyKey: String? = null,
    val quota: QuotaEntitlement? = null,
    val isLoadingQuota: Boolean = false,
    val isUpdatingMembership: Boolean = false,
    val membershipError: String? = null,
    val isUploading: Boolean = false,
    val uploadStep: UploadProgressStep = UploadProgressStep.IDLE,
    val uploadStage: String = "",
    val analysisResult: AnalysisResult? = null,
    val isQuotaExhaustedError: Boolean = false,
    val isForbiddenError: Boolean = false,
    val quotaResetHint: String? = null,
    val error: String? = null
) {
    val recordingGuidance: RecordingGuidance
        get() = recordingGuidanceFor(exerciseCode)
}

fun recordingGuidanceFor(exerciseCode: String): RecordingGuidance {
    return when (exerciseCode.trim().uppercase()) {
        "SQUAT" -> RecordingGuidance(
            title = "Squat Recording Guide",
            cameraAngle = "Side-on (90°) or 45° diagonal profile",
            distance = "2–3 meters away so full body fits cleanly",
            framing = "Keep head, hips, knees, and ankles visible from start to bottom",
            lighting = "Even room or daylight; avoid dark clothes against dark walls"
        )
        "PUSH_UP" -> RecordingGuidance(
            title = "Push-up Recording Guide",
            cameraAngle = "Side-on profile at floor/mat height",
            distance = "2–2.5 meters away perpendicular to your body",
            framing = "Frame shoulders, elbows, wrists, hips, and ankles together",
            lighting = "Bright even lighting; avoid strong backlight behind you"
        )
        "PULL_UP" -> RecordingGuidance(
            title = "Pull-up Recording Guide",
            cameraAngle = "Front or 45° front-diagonal view",
            distance = "2.5–3.5 meters back so bar and body stay in frame",
            framing = "Include the pull-up bar, hands, chin, and hips from dead hang to top",
            lighting = "Clear contrast behind the bar; stabilize phone on a tripod/stand"
        )
        else -> RecordingGuidance(
            title = "Recording Guide",
            cameraAngle = "Side-on profile view",
            distance = "2–3 meters back with camera steady",
            framing = "Keep full body head-to-toe inside the frame",
            lighting = "Good, even lighting with minimal background clutter"
        )
    }
}

@HiltViewModel
class VideoSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyzeVideoUseCase: AnalyzeVideoUseCase,
    private val analyzeImageUseCase: AnalyzeImageUseCase,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val exerciseId = savedStateHandle.get<String>("exerciseId")?.toLongOrNull() ?: 0L
    private val progressionName = runCatching {
        URLDecoder.decode(savedStateHandle.get<String>("progressionName") ?: "", "UTF-8")
    }.getOrDefault(savedStateHandle.get<String>("progressionName") ?: "")
    private val rawExerciseCode = savedStateHandle.get<String>("exerciseCode")
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw) }
        ?.trim()
        ?.uppercase()

    private val resolvedExerciseCode = resolveExerciseCode(rawExerciseCode, progressionName)

    private val _uiState = MutableStateFlow(
        VideoSelectionUiState(
            exerciseId = exerciseId,
            exerciseCode = resolvedExerciseCode,
            progressionName = progressionName,
            sideView = !resolvedExerciseCode.equals("PULL_UP", ignoreCase = true)
        )
    )
    val uiState: StateFlow<VideoSelectionUiState> = _uiState.asStateFlow()

    init {
        loadQuota()
    }

    fun loadQuota() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuota = true) }
            runCatching { analysisRepository.getVideoAnalysisQuota() }
                .getOrNull()
                ?.onSuccess { quota ->
                    _uiState.update { state ->
                        state.copy(
                            isLoadingQuota = false,
                            quota = quota,
                            isQuotaExhaustedError = if (!quota.isExhausted) false else state.isQuotaExhaustedError
                        )
                    }
                }
                ?.onFailure {
                    _uiState.update { it.copy(isLoadingQuota = false) }
                }
                ?: _uiState.update { it.copy(isLoadingQuota = false) }
        }
    }

    fun upgradeToPro() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingMembership = true, membershipError = null) }
            analysisRepository.updateMembershipTier("PRO")
                .onSuccess { updatedQuota ->
                    _uiState.update {
                        it.copy(
                            isUpdatingMembership = false,
                            quota = updatedQuota,
                            isQuotaExhaustedError = false,
                            isForbiddenError = false,
                            error = null,
                            quotaResetHint = null,
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

    fun setSideView(enabled: Boolean) {
        _uiState.update { it.copy(sideView = enabled) }
    }

    fun onMediaSelected(context: Context, uri: Uri, type: String) {
        val durationMs = if (type == "VIDEO") extractVideoDurationMs(context, uri) else null
        val freshKey = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(
                selectedVideoUri = uri,
                videoFileName = getFileName(context, uri),
                videoFileSizeFormatted = formatFileSize(getFileSize(context, uri)),
                videoDurationMs = durationMs,
                videoDurationFormatted = durationMs?.let(::formatDurationMs),
                mediaType = type,
                idempotencyKey = freshKey,
                uploadStep = UploadProgressStep.IDLE,
                uploadStage = "",
                isQuotaExhaustedError = false,
                isForbiddenError = false,
                quotaResetHint = null,
                error = null
            )
        }
    }

    fun onVideoSelected(context: Context, uri: Uri) = onMediaSelected(context, uri, "VIDEO")

    fun analyzeVideo(context: Context, onAnalysisSuccess: (Long) -> Unit) {
        val currentState = _uiState.value
        val uri = currentState.selectedVideoUri ?: return
        val stableIdempotencyKey = currentState.idempotencyKey ?: UUID.randomUUID().toString()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    idempotencyKey = stableIdempotencyKey,
                    isUploading = true,
                    uploadStep = UploadProgressStep.UPLOADING,
                    uploadStage = "1. Uploading video...",
                    isQuotaExhaustedError = false,
                    isForbiddenError = false,
                    error = null
                )
            }

            val cacheFile = try {
                copyUriToCacheFile(context, uri)
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadStep = UploadProgressStep.IDLE,
                        uploadStage = "",
                        error = error.localizedMessage ?: "Could not read selected media"
                    )
                }
                return@launch
            }

            val result = try {
                if (_uiState.value.mediaType == "IMAGE") {
                    analyzeImageUseCase(cacheFile, exerciseId.takeIf { it > 0 })
                } else {
                    analyzeVideoUseCase(
                        videoFile = cacheFile,
                        exerciseId = exerciseId.takeIf { it > 0 },
                        exerciseType = _uiState.value.exerciseCode.takeIf { it.isNotBlank() },
                        idempotencyKey = stableIdempotencyKey,
                        sideView = _uiState.value.sideView
                    )
                }
            } finally {
                cacheFile.delete()
            }

            result.onSuccess { initialAnalysis ->
                initialAnalysis.quota?.let { q ->
                    _uiState.update { it.copy(quota = q) }
                }

                if (_uiState.value.mediaType == "VIDEO" &&
                    (initialAnalysis.status.equals("PROCESSING", ignoreCase = true) ||
                        initialAnalysis.status.equals("PENDING", ignoreCase = true))
                ) {
                    _uiState.update {
                        it.copy(
                            uploadStep = UploadProgressStep.POSE_DETECTION,
                            uploadStage = "2. AI Pose Detection..."
                        )
                    }
                    var current = initialAnalysis
                    var attempts = 0
                    val maxAttempts = 45 // 45 * 2s = 90s
                    var consecutivePollFailures = 0

                    while ((current.status.equals("PROCESSING", ignoreCase = true) ||
                            current.status.equals("PENDING", ignoreCase = true)) &&
                        attempts < maxAttempts
                    ) {
                        delay(2000)
                        attempts++
                        val nextStep = if (attempts <= 2 || current.status.equals("PENDING", ignoreCase = true)) {
                            UploadProgressStep.POSE_DETECTION
                        } else {
                            UploadProgressStep.BIOMECHANICAL_ANALYSIS
                        }
                        _uiState.update {
                            it.copy(
                                uploadStep = nextStep,
                                uploadStage = "${nextStep.label} (${attempts * 2}s)..."
                            )
                        }

                        val pollResult = analysisRepository.getVideoAnalysisById(initialAnalysis.id)
                        if (pollResult.isSuccess) {
                            consecutivePollFailures = 0
                            current = pollResult.getOrThrow()
                            current.quota?.let { q -> _uiState.update { it.copy(quota = q) } }
                        } else {
                            consecutivePollFailures++
                            val pollErr = pollResult.exceptionOrNull()
                            val code = NetworkErrorParser.httpStatusCode(pollErr)
                            if (code == 401 || code == 403 || code == 404 || code == 429 || consecutivePollFailures >= 5) {
                                handleAnalysisFailure(pollErr ?: Exception("Polling failed"))
                                return@launch
                            }
                        }
                    }

                    if (isTerminalFailureStatus(current.status)) {
                        val errMsg = current.errorMessage?.takeIf { it.isNotBlank() }
                            ?: current.formFeedbackList.firstOrNull()
                            ?: current.status.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadStep = UploadProgressStep.IDLE,
                                uploadStage = "",
                                error = errMsg
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadStep = UploadProgressStep.COMPLETED,
                            uploadStage = "Completed",
                            analysisResult = current
                        )
                    }
                    onAnalysisSuccess(current.id)
                } else if (isTerminalFailureStatus(initialAnalysis.status)) {
                    val errMsg = initialAnalysis.errorMessage?.takeIf { it.isNotBlank() }
                        ?: initialAnalysis.formFeedbackList.firstOrNull()
                        ?: "AI Analysis could not evaluate video"
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadStep = UploadProgressStep.IDLE,
                            uploadStage = "",
                            error = errMsg
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadStep = UploadProgressStep.COMPLETED,
                            uploadStage = "Completed",
                            analysisResult = initialAnalysis
                        )
                    }
                    onAnalysisSuccess(initialAnalysis.id)
                }
            }.onFailure { error ->
                handleAnalysisFailure(error)
            }
        }
    }

    private fun handleAnalysisFailure(throwable: Throwable) {
        val parsed = NetworkErrorParser.parseApiError(throwable)
        when (parsed.statusCode) {
            429 -> {
                val resetHint = formatResetHint(
                    resetsAt = parsed.resetsAt ?: _uiState.value.quota?.resetsAt,
                    timezone = parsed.timezone ?: _uiState.value.quota?.timezone ?: "Asia/Ho_Chi_Minh"
                )
                _uiState.update { state ->
                    state.copy(
                        isUploading = false,
                        uploadStep = UploadProgressStep.IDLE,
                        uploadStage = "",
                        isQuotaExhaustedError = true,
                        isForbiddenError = false,
                        quotaResetHint = resetHint,
                        quota = state.quota?.copy(
                            remainingToday = parsed.remainingToday ?: 0,
                            dailyLimit = parsed.dailyLimit ?: state.quota.dailyLimit
                        ),
                        error = parsed.userMessage.ifBlank {
                            "Daily video analysis quota reached. Upgrade to PRO for 10 analyses/day."
                        }
                    )
                }
            }
            403 -> {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadStep = UploadProgressStep.IDLE,
                        uploadStage = "",
                        isQuotaExhaustedError = false,
                        isForbiddenError = true,
                        error = parsed.userMessage
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadStep = UploadProgressStep.IDLE,
                        uploadStage = "",
                        isQuotaExhaustedError = false,
                        isForbiddenError = false,
                        error = parsed.userMessage
                    )
                }
            }
        }
    }

    private fun isTerminalFailureStatus(status: String?): Boolean {
        val upper = status?.uppercase() ?: return false
        return upper == "FAILED" ||
            upper == "NO_PERSON_DETECTED" ||
            upper == "INSUFFICIENT_POSE_QUALITY" ||
            upper == "INVALID_INPUT"
    }

    private fun formatResetHint(resetsAt: String?, timezone: String): String {
        return if (!resetsAt.isNullOrBlank()) {
            "Resets at $resetsAt ($timezone)"
        } else {
            "Resets at 00:00 ($timezone)"
        }
    }

    private fun resolveExerciseCode(rawCode: String?, name: String): String {
        if (!rawCode.isNullOrBlank()) return rawCode
        val lower = name.lowercase()
        return when {
            lower.contains("squat") -> "SQUAT"
            lower.contains("push") -> "PUSH_UP"
            lower.contains("pull") -> "PULL_UP"
            lower.contains("planche") -> "PLANCHE"
            lower.contains("lever") -> "FRONT_LEVER"
            lower.contains("handstand") -> "HANDSTAND"
            lower.contains("dip") -> "DIPS"
            lower.contains("muscle") -> "MUSCLE_UP"
            else -> name.uppercase().replace(" ", "_").replace("-", "_").takeIf { it.isNotBlank() } ?: "SQUAT"
        }
    }

    private fun extractVideoDurationMs(context: Context, uri: Uri): Long? {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()
    }

    private fun formatDurationMs(durationMs: Long): String {
        val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
        val minutes = totalSec / 60L
        val seconds = totalSec % 60L
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun copyUriToCacheFile(context: Context, uri: Uri): File {
        val extension = if (_uiState.value.mediaType == "IMAGE") ".jpg" else ".mp4"
        val file = File.createTempFile("upload_media_", extension, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("Cannot open selected media")
        return file
    }

    private fun getFileName(context: Context, uri: Uri): String? = if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let(cursor::getString) else null
        }
    } else uri.path?.substringAfterLast('/')

    private fun getFileSize(context: Context, uri: Uri): Long = if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let(cursor::getLong) ?: 0L else 0L
        } ?: 0L
    } else 0L

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown size"
        val mb = bytes / 1024.0 / 1024.0
        return if (mb >= 1) String.format("%.2f MB", mb) else String.format("%.2f KB", bytes / 1024.0)
    }
}
