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

data class RefreshTokenRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)

data class GoogleLoginRequestDto(
    @SerializedName("idToken") val idToken: String
)

data class AuthResponseDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("tokenType") val tokenType: String = "Bearer",
    @SerializedName("userId") val userId: Long,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("role") val role: String
)

data class UserProfileResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("experienceLevel") val experienceLevel: String?,
    @SerializedName("role") val role: String?
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
