package com.calisthenics.ai.domain.model

data class UserProfile(
    val id: Long,
    val email: String,
    val fullName: String,
    val experienceLevel: String,
    val role: String
)

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val email: String,
    val fullName: String
)
