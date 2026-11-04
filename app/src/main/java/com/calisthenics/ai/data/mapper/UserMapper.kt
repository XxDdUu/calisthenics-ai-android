package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import com.calisthenics.ai.data.remote.dto.UserProfileResponseDto
import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.UserProfile

fun AuthResponseDto.toDomain(): AuthToken {
    return AuthToken(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        email = email,
        fullName = fullName
    )
}

fun UserProfileResponseDto.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        email = email,
        fullName = fullName,
        experienceLevel = experienceLevel ?: "INTERMEDIATE",
        role = role ?: "ROLE_USER"
    )
}
