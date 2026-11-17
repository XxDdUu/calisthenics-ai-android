package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.OnboardingStatus
import com.calisthenics.ai.domain.model.SavedAccount
import com.calisthenics.ai.domain.model.UserProfile

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthToken>
    suspend fun loginWithFirebase(idToken: String): Result<AuthToken> = loginWithGoogle(idToken)
    suspend fun loginWithGoogle(idToken: String): Result<AuthToken> =
        Result.failure(UnsupportedOperationException())
    suspend fun register(email: String, password: String, fullName: String): Result<RegistrationInitiation>
    suspend fun verifyRegistrationOtp(email: String, otp: String): Result<AuthToken> =
        Result.failure(UnsupportedOperationException())
    suspend fun resendRegistrationOtp(email: String): Result<RegistrationInitiation> =
        Result.failure(UnsupportedOperationException())
    suspend fun requestPasswordResetOtp(email: String): Result<String> =
        Result.failure(UnsupportedOperationException())
    suspend fun verifyPasswordResetOtp(email: String, otp: String): Result<PasswordResetAuthorization> =
        Result.failure(UnsupportedOperationException())
    suspend fun resetPassword(resetToken: String, newPassword: String, confirmPassword: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())
    suspend fun refreshToken(): Result<AuthToken> = Result.failure(UnsupportedOperationException())
    suspend fun getCurrentUser(): Result<UserProfile>
    suspend fun updateGender(gender: Gender): Result<UserProfile> =
        Result.failure(UnsupportedOperationException())
    fun getCachedGender(userId: Long? = getUserId()): Gender? = null
    suspend fun updateOnboardingStatus(status: OnboardingStatus): Result<UserProfile> =
        Result.failure(UnsupportedOperationException())
    fun getCachedOnboardingStatus(userId: Long? = getUserId()): OnboardingStatus =
        OnboardingStatus.NOT_SHOWN
    fun isLoggedIn(): Boolean
    fun getUserRole(): String? = null
    fun getUserId(): Long? = null
    fun getUserEmail(): String? = null
    fun saveToken(token: AuthToken)
    fun getAccessToken(): String?
    fun clearAuth()
    fun deactivateActiveSession() {}

    fun getSavedAccounts(): List<SavedAccount> = emptyList()
    suspend fun switchToAccount(userId: Long): Result<SavedAccount> =
        Result.failure(UnsupportedOperationException())
    fun removeAccount(userId: Long): Boolean = false
    fun updateAccountProfile(userId: Long, email: String? = null, fullName: String? = null, avatarUrl: String? = null) {}
}

data class PasswordResetAuthorization(val token: String, val expiresIn: Int)

data class RegistrationInitiation(
    val email: String,
    val message: String,
    val expiresIn: Int,
    val resendCooldown: Int
)
