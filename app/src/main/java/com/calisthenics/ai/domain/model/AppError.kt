package com.calisthenics.ai.domain.model

sealed interface AppError {
    data object Network : AppError
    data object Timeout : AppError
    data object Unauthorized : AppError
    data class Server(val message: String?) : AppError
    data class Validation(val message: String) : AppError
    data class Unknown(val throwable: Throwable) : AppError

    fun getUserMessage(): String {
        return when (this) {
            is Network -> "Network error. Please check your internet connection and try again."
            is Timeout -> "Connection timed out. AI analysis may take a moment, please try again."
            is Unauthorized -> "Authentication required. Please sign in to analyze form."
            is Server -> message ?: "Server error occurred while analyzing form."
            is Validation -> message
            is Unknown -> throwable.localizedMessage ?: "An unexpected error occurred."
        }
    }
}
