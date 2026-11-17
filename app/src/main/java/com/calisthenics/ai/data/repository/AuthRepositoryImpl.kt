package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.local.GenderSessionStore
import com.calisthenics.ai.data.local.OnboardingSessionStore
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.datasource.RemoteAuthDataSource
import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import com.calisthenics.ai.data.remote.dto.LoginRequestDto
import com.calisthenics.ai.data.remote.dto.RegisterRequestDto
import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.OnboardingStatus
import com.calisthenics.ai.domain.model.SavedAccount
import com.calisthenics.ai.domain.model.UserProfile
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.PasswordResetAuthorization
import com.calisthenics.ai.domain.repository.RegistrationInitiation
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val tokenManager: TokenManager,
    private val genderSessionStore: GenderSessionStore = GenderSessionStore(),
    private val onboardingSessionStore: OnboardingSessionStore = OnboardingSessionStore()
) : AuthRepository {

    private fun persistAuthResponse(response: AuthResponseDto): AuthToken {
        tokenManager.saveAuthToken(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.userId,
            role = response.role,
            email = response.email,
            fullName = response.fullName
        )
        tokenManager.saveAccountGender(response.userId, response.gender)
        genderSessionStore.publish(response.userId, Gender.fromApi(response.gender), isBackendConfirmed = true)
        tokenManager.saveAccountOnboardingStatus(response.userId, response.onboardingStatus)
        onboardingSessionStore.publish(
            response.userId,
            OnboardingStatus.fromApi(response.onboardingStatus),
            isBackendConfirmed = true
        )
        return response.toDomain()
    }

    override suspend fun login(email: String, password: String): Result<AuthToken> {
        return runCatching {
            persistAuthResponse(remoteAuthDataSource.login(LoginRequestDto(email, password)))
        }
    }

    override suspend fun loginWithFirebase(idToken: String): Result<AuthToken> {
        return runCatching {
            persistAuthResponse(remoteAuthDataSource.authenticateWithFirebase(idToken))
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthToken> {
        return runCatching {
            persistAuthResponse(remoteAuthDataSource.authenticateWithGoogle(idToken))
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
        persistAuthResponse(remoteAuthDataSource.verifyRegistrationOtp(email, otp))
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
            tokenManager.saveAccountGender(response.userId, response.gender)
            genderSessionStore.publish(response.userId, Gender.fromApi(response.gender), isBackendConfirmed = true)
            tokenManager.saveAccountOnboardingStatus(response.userId, response.onboardingStatus)
            onboardingSessionStore.publish(response.userId, OnboardingStatus.fromApi(response.onboardingStatus), isBackendConfirmed = true)
            response.toDomain()
        }
    }

    override suspend fun getCurrentUser(): Result<UserProfile> {
        return runCatching {
            val profile = remoteAuthDataSource.getCurrentUser().toDomain()
            tokenManager.saveAccountGender(profile.id, profile.gender?.apiValue)
            genderSessionStore.publish(profile.id, profile.gender, isBackendConfirmed = true)
            tokenManager.saveAccountOnboardingStatus(profile.id, profile.onboardingStatus.apiValue)
            onboardingSessionStore.publish(profile.id, profile.onboardingStatus, isBackendConfirmed = true)
            profile
        }
    }

    override suspend fun updateGender(gender: Gender): Result<UserProfile> {
        return runCatching {
            val profile = remoteAuthDataSource.updateGender(gender.apiValue).toDomain()
            tokenManager.saveAccountGender(profile.id, profile.gender?.apiValue)
            genderSessionStore.publish(profile.id, profile.gender, isBackendConfirmed = true)
            profile
        }
    }

    override suspend fun updateOnboardingStatus(status: OnboardingStatus): Result<UserProfile> {
        return runCatching {
            val profile = remoteAuthDataSource.updateOnboardingStatus(status.apiValue).toDomain()
            tokenManager.saveAccountOnboardingStatus(profile.id, profile.onboardingStatus.apiValue)
            onboardingSessionStore.publish(profile.id, profile.onboardingStatus, isBackendConfirmed = true)
            profile
        }
    }

    override fun getCachedOnboardingStatus(userId: Long?): OnboardingStatus {
        val id = userId ?: tokenManager.getUserId() ?: return OnboardingStatus.NOT_SHOWN
        return OnboardingStatus.fromApi(tokenManager.getAccountOnboardingStatus(id))
    }

    override fun getCachedGender(userId: Long?): Gender? {
        val id = userId ?: tokenManager.getUserId() ?: return null
        return Gender.fromApi(tokenManager.getAccountGender(id))
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
        genderSessionStore.clear()
    }

    override fun deactivateActiveSession() {
        tokenManager.deactivateActiveSession()
        genderSessionStore.clear()
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
            tokenManager.saveAccountGender(response.userId, response.gender)
            genderSessionStore.publish(response.userId, Gender.fromApi(response.gender), isBackendConfirmed = true)
            tokenManager.saveAccountOnboardingStatus(response.userId, response.onboardingStatus)
            onboardingSessionStore.publish(response.userId, OnboardingStatus.fromApi(response.onboardingStatus), isBackendConfirmed = true)
        } else {
            genderSessionStore.clear()
            val cachedStatus = getCachedOnboardingStatus(userId)
            onboardingSessionStore.publish(userId, cachedStatus, isBackendConfirmed = false)
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
