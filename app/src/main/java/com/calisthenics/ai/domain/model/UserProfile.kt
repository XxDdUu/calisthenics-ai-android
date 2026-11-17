package com.calisthenics.ai.domain.model

data class UserProfile(
    val id: Long,
    val email: String,
    val fullName: String,
    val experienceLevel: String,
    val role: String,
    val gender: Gender? = null,
    val onboardingStatus: OnboardingStatus = OnboardingStatus.NOT_SHOWN,
    val membershipTier: String = "FREE",
    val dailyQuotaLimit: Int? = 1,
    val dailyQuotaUsed: Int = 0,
    val dailyQuotaRemaining: Int? = 1,
    val liveAnalysisUnlocked: Boolean = false,
    val unlimited: Boolean = false,
    val membershipSource: String = "Default",
    val adminGrantedPro: Boolean = false
) {
    val isAdmin: Boolean
        get() = role.equals("ROLE_ADMIN", ignoreCase = true) || membershipTier.equals("ADMIN", ignoreCase = true)

    val isPro: Boolean
        get() = isAdmin || membershipTier.equals("PRO", ignoreCase = true)
}

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
    val email: String,
    val fullName: String,
    val role: String = "ROLE_USER",
    val gender: Gender? = null,
    val onboardingStatus: OnboardingStatus = OnboardingStatus.NOT_SHOWN
)
