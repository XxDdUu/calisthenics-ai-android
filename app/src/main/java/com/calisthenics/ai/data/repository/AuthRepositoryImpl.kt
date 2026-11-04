package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.datasource.RemoteAuthDataSource
import com.calisthenics.ai.data.remote.dto.LoginRequestDto
import com.calisthenics.ai.data.remote.dto.RegisterRequestDto
import com.calisthenics.ai.domain.repository.PasswordResetAuthorization
import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.UserProfile
import com.calisthenics.ai.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthToken> {
        return runCatching {
            val response = remoteAuthDataSource.login(LoginRequestDto(email, password))
            tokenManager.saveAuthToken(response.accessToken, response.refreshToken, response.userId)
            response.toDomain()
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthToken> {
        return runCatching {
            val response = remoteAuthDataSource.authenticateWithGoogle(idToken)
            tokenManager.saveAuthToken(response.accessToken, response.refreshToken, response.userId)
            response.toDomain()
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String
    ): Result<AuthToken> {
        return runCatching {
            val response = remoteAuthDataSource.register(RegisterRequestDto(email, password, fullName))
            tokenManager.saveAuthToken(response.accessToken, response.refreshToken, response.userId)
            response.toDomain()
        }
    }

    override suspend fun requestPasswordResetOtp(email: String): Result<String> = runCatching {
        remoteAuthDataSource.requestPasswordResetOtp(email).message
    }

    override suspend fun verifyPasswordResetOtp(email: String, otp: String): Result<PasswordResetAuthorization> =
        runCatching {
            remoteAuthDataSource.verifyPasswordResetOtp(email, otp).let {
                PasswordResetAuthorization(it.resetToken, it.expiresIn)
            }
        }

    override suspend fun resetPassword(
        resetToken: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> = runCatching {
        remoteAuthDataSource.resetPassword(resetToken, newPassword, confirmPassword)
    }

    override suspend fun refreshToken(): Result<AuthToken> {
        return runCatching {
            val refreshToken = tokenManager.getRefreshToken()
                ?: throw IllegalStateException("No refresh token available")
            val response = remoteAuthDataSource.refreshToken(refreshToken)
            tokenManager.saveAuthToken(response.accessToken, response.refreshToken, response.userId)
            response.toDomain()
        }
    }

    override suspend fun getCurrentUser(): Result<UserProfile> {
        return runCatching {
            remoteAuthDataSource.getCurrentUser().toDomain()
        }
    }

    override fun isLoggedIn(): Boolean {
        return tokenManager.hasValidSession()
    }

    override fun saveToken(token: AuthToken) {
        tokenManager.saveAuthToken(token.accessToken, token.refreshToken, token.userId)
    }

    override fun getAccessToken(): String? {
        return tokenManager.getAccessToken()
    }

    override fun clearAuth() {
        tokenManager.clear()
    }
}
