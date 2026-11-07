package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.datasource.RemoteAuthDataSource
import com.calisthenics.ai.data.remote.dto.LoginRequestDto
import com.calisthenics.ai.data.remote.dto.RegisterRequestDto
import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.SavedAccount
import com.calisthenics.ai.domain.model.UserProfile
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.PasswordResetAuthorization
import com.calisthenics.ai.domain.repository.RegistrationInitiation
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthToken> {
        return runCatching {
            val response = remoteAuthDataSource.login(LoginRequestDto(email, password))
            tokenManager.saveAuthToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                userId = response.userId,
                role = response.role,
                email = response.email,
                fullName = response.fullName
            )
            response.toDomain()
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthToken> {
        return runCatching {
            val response = remoteAuthDataSource.authenticateWithGoogle(idToken)
            tokenManager.saveAuthToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                userId = response.userId,
                role = response.role,
                email = response.email,
                fullName = response.fullName
            )
            response.toDomain()
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String
    ): Result<RegistrationInitiation> = runCatching {
        val response = remoteAuthDataSource.register(RegisterRequestDto(email, password, fullName))
        RegistrationInitiation(
            email = response.email,
            message = response.message,
            expiresIn = response.expiresIn,
            resendCooldown = response.resendCooldown
        )
    }

    override suspend fun verifyRegistrationOtp(email: String, otp: String): Result<AuthToken> = runCatching {
        val response = remoteAuthDataSource.verifyRegistrationOtp(email, otp)
        tokenManager.saveAuthToken(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.userId,
            role = response.role,
            email = response.email,
            fullName = response.fullName
        )
        response.toDomain()
    }

    override suspend fun resendRegistrationOtp(email: String): Result<RegistrationInitiation> = runCatching {
        val response = remoteAuthDataSource.resendRegistrationOtp(email)
        RegistrationInitiation(
            email = response.email,
            message = response.message,
            expiresIn = response.expiresIn,
            resendCooldown = response.resendCooldown
        )
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
            tokenManager.saveAuthToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                userId = response.userId,
                role = response.role,
                email = response.email,
                fullName = response.fullName
            )
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
        tokenManager.saveAuthToken(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            userId = token.userId,
            role = token.role,
            email = token.email,
            fullName = token.fullName
        )
    }

    override fun getAccessToken(): String? {
        return tokenManager.getAccessToken()
    }

    override fun getUserRole(): String? = tokenManager.getUserRole()

    override fun getUserId(): Long? = tokenManager.getUserId()

    override fun getUserEmail(): String? = tokenManager.getUserEmail()

    override fun clearAuth() {
        tokenManager.clear()
    }

    override fun deactivateActiveSession() {
        tokenManager.deactivateActiveSession()
    }

    override fun getSavedAccounts(): List<SavedAccount> {
        return tokenManager.getSavedAccounts()
    }

    override suspend fun switchToAccount(userId: Long): Result<SavedAccount> = runCatching {
        val success = tokenManager.switchToAccount(userId)
        if (!success) {
            throw IllegalStateException("Tài khoản không tồn tại trên thiết bị này.")
        }

        // Verify token freshness; refresh if access token expired
        if (tokenManager.isAccessTokenExpired()) {
            if (tokenManager.isRefreshTokenExpired()) {
                throw IllegalStateException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
            }
            val refToken = tokenManager.getRefreshToken()
                ?: throw IllegalStateException("Không tìm thấy refresh token.")
            val response = remoteAuthDataSource.refreshToken(refToken)
            tokenManager.saveAuthToken(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                userId = response.userId,
                role = response.role,
                email = response.email,
                fullName = response.fullName
            )
        }

        tokenManager.getActiveAccount()
            ?: throw IllegalStateException("Không thể kích hoạt phiên làm việc của tài khoản này.")
    }

    override fun removeAccount(userId: Long): Boolean {
        return tokenManager.removeAccount(userId)
    }

    override fun updateAccountProfile(
        userId: Long,
        email: String?,
        fullName: String?,
        avatarUrl: String?
    ) {
        tokenManager.updateAccountProfile(userId, email, fullName, avatarUrl)
    }
}
