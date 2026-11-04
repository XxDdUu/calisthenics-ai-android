package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import com.calisthenics.ai.data.remote.dto.ForgotPasswordRequestDto
import com.calisthenics.ai.data.remote.dto.ForgotPasswordResponseDto
import com.calisthenics.ai.data.remote.dto.GoogleLoginRequestDto
import com.calisthenics.ai.data.remote.dto.LoginRequestDto
import com.calisthenics.ai.data.remote.dto.ResetPasswordRequestDto
import com.calisthenics.ai.data.remote.dto.RefreshTokenRequestDto
import com.calisthenics.ai.data.remote.dto.RegisterRequestDto
import com.calisthenics.ai.data.remote.dto.UserProfileResponseDto
import com.calisthenics.ai.data.remote.dto.VerifyOtpRequestDto
import com.calisthenics.ai.data.remote.dto.VerifyOtpResponseDto
import javax.inject.Inject

class RemoteAuthDataSource @Inject constructor(
    private val api: CalisthenicsApi
) {
    suspend fun login(loginRequest: LoginRequestDto): AuthResponseDto {
        return api.login(loginRequest)
    }

    suspend fun authenticateWithGoogle(idToken: String): AuthResponseDto {
        return api.authenticateWithGoogle(GoogleLoginRequestDto(idToken))
    }

    suspend fun register(registerRequest: RegisterRequestDto): AuthResponseDto {
        return api.register(registerRequest)
    }

    suspend fun requestPasswordResetOtp(email: String): ForgotPasswordResponseDto {
        return api.requestPasswordResetOtp(ForgotPasswordRequestDto(email))
    }

    suspend fun verifyPasswordResetOtp(email: String, otp: String): VerifyOtpResponseDto {
        return api.verifyPasswordResetOtp(VerifyOtpRequestDto(email, otp))
    }

    suspend fun resetPassword(resetToken: String, newPassword: String, confirmPassword: String) {
        api.resetPassword(ResetPasswordRequestDto(resetToken, newPassword, confirmPassword))
    }

    suspend fun refreshToken(refreshToken: String): AuthResponseDto {
        return api.refreshToken(RefreshTokenRequestDto(refreshToken))
    }

    suspend fun logout(): Boolean {
        return runCatching {
            api.logout().isSuccessful
        }.getOrDefault(false)
    }

    suspend fun getCurrentUser(): UserProfileResponseDto {
        return api.getCurrentUser()
    }
}
