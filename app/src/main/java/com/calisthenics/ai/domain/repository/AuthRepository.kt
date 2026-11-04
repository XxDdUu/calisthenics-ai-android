package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.UserProfile

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthToken>
    suspend fun loginWithGoogle(idToken: String): Result<AuthToken> =
        Result.failure(UnsupportedOperationException())
    suspend fun register(email: String, password: String, fullName: String): Result<AuthToken>
    suspend fun requestPasswordResetOtp(email: String): Result<String> =
        Result.failure(UnsupportedOperationException())
    suspend fun verifyPasswordResetOtp(email: String, otp: String): Result<PasswordResetAuthorization> =
        Result.failure(UnsupportedOperationException())
    suspend fun resetPassword(resetToken: String, newPassword: String, confirmPassword: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())
    suspend fun refreshToken(): Result<AuthToken> = Result.failure(UnsupportedOperationException())
    suspend fun getCurrentUser(): Result<UserProfile>
    fun isLoggedIn(): Boolean
    fun saveToken(token: AuthToken)
    fun getAccessToken(): String?
    fun clearAuth()
}

data class PasswordResetAuthorization(val token: String, val expiresIn: Int)
