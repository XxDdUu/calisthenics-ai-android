package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("experienceLevel") val experienceLevel: String = "INTERMEDIATE"
)

data class RegisterInitiationResponseDto(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String,
    @SerializedName("email") val email: String,
    @SerializedName("expiresIn") val expiresIn: Int,
    @SerializedName("resendCooldown") val resendCooldown: Int
)

data class VerifyRegistrationOtpRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class ResendRegistrationOtpRequestDto(
    @SerializedName("email") val email: String
)

data class RefreshTokenRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)

data class GoogleLoginRequestDto(
    @SerializedName("idToken") val idToken: String
)

data class FirebaseLoginRequestDto(
    @SerializedName("idToken") val idToken: String
)

data class AuthResponseDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("tokenType") val tokenType: String = "Bearer",
    @SerializedName("userId") val userId: Long,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("role") val role: String,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("onboardingStatus") val onboardingStatus: String? = null
)

data class UserProfileResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("experienceLevel") val experienceLevel: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("onboardingStatus") val onboardingStatus: String? = null,
    @SerializedName("membershipTier") val membershipTier: String? = null,
    @SerializedName("dailyQuotaLimit") val dailyQuotaLimit: Int? = null,
    @SerializedName("dailyQuotaUsed") val dailyQuotaUsed: Int? = null,
    @SerializedName("dailyQuotaRemaining") val dailyQuotaRemaining: Int? = null,
    @SerializedName("liveAnalysisUnlocked") val liveAnalysisUnlocked: Boolean? = null,
    @SerializedName("unlimited") val unlimited: Boolean? = null,
    @SerializedName("membershipSource") val membershipSource: String? = null,
    @SerializedName("adminGrantedPro") val adminGrantedPro: Boolean? = null
)

data class UpdateUserProfileRequestDto(
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("experienceLevel") val experienceLevel: String? = null,
    @SerializedName("gender") val gender: String? = null
)

data class UpdateGenderRequestDto(
    @SerializedName("gender") val gender: String
)

data class UpdateOnboardingStatusRequestDto(
    @SerializedName("status") val status: String
)

data class ForgotPasswordRequestDto(
    @SerializedName("email") val email: String
)

data class ForgotPasswordResponseDto(
    @SerializedName("message") val message: String
)

data class VerifyOtpRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class VerifyOtpResponseDto(
    @SerializedName("resetToken") val resetToken: String,
    @SerializedName("expiresIn") val expiresIn: Int
)

data class ResetPasswordRequestDto(
    @SerializedName("resetToken") val resetToken: String,
    @SerializedName("newPassword") val newPassword: String,
    @SerializedName("confirmPassword") val confirmPassword: String
)

data class ResetPasswordResponseDto(
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String
)
