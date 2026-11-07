package com.calisthenics.ai.ui.media

sealed class MediaUploadState {
    data object Idle : MediaUploadState()
    data class Uploading(val progress: Float = 0f, val stage: String = "Uploading...") : MediaUploadState()
    data class Success(val url: String) : MediaUploadState()
    data class Error(val message: String) : MediaUploadState()
}
