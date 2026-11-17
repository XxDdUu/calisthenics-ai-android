package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import com.calisthenics.ai.data.remote.dto.UserProfileResponseDto
import com.calisthenics.ai.domain.model.AuthToken
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.OnboardingStatus
import com.calisthenics.ai.domain.model.UserProfile

fun AuthResponseDto.toDomain(): AuthToken {
    return AuthToken(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        email = email,
        fullName = fullName,
        role = role,
        gender = Gender.fromApi(gender),
        onboardingStatus = OnboardingStatus.fromApi(onboardingStatus)
    )
}

fun UserProfileResponseDto.toDomain(): UserProfile {
    val tier = membershipTier ?: if (role?.equals("ROLE_ADMIN", ignoreCase = true) == true) "ADMIN" else "FREE"
    val isAdmin = role?.equals("ROLE_ADMIN", ignoreCase = true) == true || tier.equals("ADMIN", ignoreCase = true)
    val isUnlimited = isAdmin || (unlimited == true) || (adminGrantedPro == true)
    val resolvedSource = membershipSource?.takeIf { it.isNotBlank() } ?: when {
        isAdmin -> "ADMIN Role"
        adminGrantedPro == true -> "Admin Grant"
        tier.equals("PRO", ignoreCase = true) -> "Subscription"
        else -> "Default"
    }
    return UserProfile(
        id = id,
        email = email,
        fullName = fullName,
        experienceLevel = experienceLevel ?: "INTERMEDIATE",
        role = role ?: "ROLE_USER",
        gender = Gender.fromApi(gender),
        onboardingStatus = OnboardingStatus.fromApi(onboardingStatus),
        membershipTier = tier.uppercase(),
        dailyQuotaLimit = if (isUnlimited) null else (dailyQuotaLimit ?: if (tier.equals("PRO", ignoreCase = true)) 10 else 1),
        dailyQuotaUsed = dailyQuotaUsed ?: 0,
        dailyQuotaRemaining = if (isUnlimited) null else dailyQuotaRemaining,
        liveAnalysisUnlocked = if (isUnlimited) true else (liveAnalysisUnlocked ?: tier.equals("PRO", ignoreCase = true)),
        unlimited = isUnlimited,
        membershipSource = resolvedSource,
        adminGrantedPro = adminGrantedPro == true
    )
}
