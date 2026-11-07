package com.calisthenics.ai.ui.video

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.usecase.AnalyzeImageUseCase
import com.calisthenics.ai.domain.usecase.AnalyzeVideoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class VideoSelectionUiState(
    val exerciseId: Long = 0L,
    val progressionName: String = "",
    val selectedVideoUri: Uri? = null,
    val videoFileName: String? = null,
    val videoFileSizeFormatted: String? = null,
    val mediaType: String = "VIDEO",
    val isUploading: Boolean = false,
    val uploadStage: String = "",
    val analysisResult: AnalysisResult? = null,
    val error: String? = null
)

@HiltViewModel
class VideoSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyzeVideoUseCase: AnalyzeVideoUseCase,
    private val analyzeImageUseCase: AnalyzeImageUseCase
) : ViewModel() {
    private val exerciseId = savedStateHandle.get<String>("exerciseId")?.toLongOrNull() ?: 0L
    private val progressionName = java.net.URLDecoder.decode(savedStateHandle.get<String>("progressionName") ?: "", "UTF-8")
    private val _uiState = MutableStateFlow(VideoSelectionUiState(exerciseId = exerciseId, progressionName = progressionName))
    val uiState: StateFlow<VideoSelectionUiState> = _uiState.asStateFlow()

    fun onMediaSelected(context: Context, uri: Uri, type: String) {
        _uiState.update { it.copy(selectedVideoUri = uri, videoFileName = getFileName(context, uri), videoFileSizeFormatted = formatFileSize(getFileSize(context, uri)), mediaType = type, error = null) }
    }

    fun onVideoSelected(context: Context, uri: Uri) = onMediaSelected(context, uri, "VIDEO")

    fun analyzeVideo(context: Context, onAnalysisSuccess: (Long) -> Unit) {
        val uri = _uiState.value.selectedVideoUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadStage = "Preparing ${_uiState.value.mediaType.lowercase()} upload...", error = null) }
            val cacheFile = try { copyUriToCacheFile(context, uri) } catch (error: Exception) {
                _uiState.update { it.copy(isUploading = false, uploadStage = "", error = error.localizedMessage ?: "Could not read selected media") }
                return@launch
            }
            _uiState.update { it.copy(uploadStage = "Analyzing pose biomechanics & form score...") }
            val result = if (_uiState.value.mediaType == "IMAGE") analyzeImageUseCase(cacheFile, exerciseId.takeIf { it > 0 }) else analyzeVideoUseCase(cacheFile, exerciseId.takeIf { it > 0 })
            cacheFile.delete()
            result.onSuccess { analysis ->
                _uiState.update { it.copy(isUploading = false, uploadStage = "", analysisResult = analysis) }
                onAnalysisSuccess(analysis.id)
            }.onFailure { error ->
                _uiState.update { it.copy(isUploading = false, uploadStage = "", error = error.localizedMessage ?: "Analysis failed. Please try again.") }
            }
        }
    }

    private fun copyUriToCacheFile(context: Context, uri: Uri): File {
        val extension = if (_uiState.value.mediaType == "IMAGE") ".jpg" else ".mp4"
        val file = File.createTempFile("upload_media_", extension, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
            ?: throw IllegalArgumentException("Cannot open selected media")
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
