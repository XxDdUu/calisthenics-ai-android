package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import com.calisthenics.ai.data.remote.dto.FirebaseLoginRequestDto
import com.calisthenics.ai.data.remote.dto.ForgotPasswordRequestDto
import com.calisthenics.ai.data.remote.dto.ForgotPasswordResponseDto
import com.calisthenics.ai.data.remote.dto.GoogleLoginRequestDto
import com.calisthenics.ai.data.remote.dto.LoginRequestDto
import com.calisthenics.ai.data.remote.dto.ResetPasswordRequestDto
import com.calisthenics.ai.data.remote.dto.RefreshTokenRequestDto
import com.calisthenics.ai.data.remote.dto.RegisterRequestDto
import com.calisthenics.ai.data.remote.dto.RegisterInitiationResponseDto
import com.calisthenics.ai.data.remote.dto.VerifyRegistrationOtpRequestDto
import com.calisthenics.ai.data.remote.dto.ResendRegistrationOtpRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateGenderRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateOnboardingStatusRequestDto
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

    suspend fun authenticateWithFirebase(idToken: String): AuthResponseDto {
        return api.authenticateWithFirebase(FirebaseLoginRequestDto(idToken))
    }

    suspend fun register(registerRequest: RegisterRequestDto): RegisterInitiationResponseDto {
        return api.register(registerRequest)
    }

    suspend fun verifyRegistrationOtp(email: String, otp: String): AuthResponseDto {
        return api.verifyRegistrationOtp(VerifyRegistrationOtpRequestDto(email, otp))
    }

    suspend fun resendRegistrationOtp(email: String): RegisterInitiationResponseDto {
        return api.resendRegistrationOtp(ResendRegistrationOtpRequestDto(email))
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

    suspend fun updateGender(gender: String): UserProfileResponseDto {
        return api.updateCurrentUserGender(UpdateGenderRequestDto(gender))
    }

    suspend fun updateOnboardingStatus(status: String): UserProfileResponseDto {
        return api.updateCurrentUserOnboardingStatus(UpdateOnboardingStatusRequestDto(status))
    }
}
