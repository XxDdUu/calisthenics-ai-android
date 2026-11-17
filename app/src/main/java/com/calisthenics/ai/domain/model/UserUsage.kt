package com.calisthenics.ai.domain.model

data class UserUsage(
    val membership: MembershipUsage = MembershipUsage(),
    val videoAnalysis: VideoAnalysisUsage = VideoAnalysisUsage(),
    val liveAnalysis: LiveAnalysisUsage = LiveAnalysisUsage(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class MembershipUsage(
    val tier: String = "FREE",
    val status: String = "ACTIVE",
    val expiresAt: String? = null,
    val source: String? = null
) {
    val isAdmin: Boolean get() = tier.equals("ADMIN", ignoreCase = true)
    val isPro: Boolean get() = isAdmin || tier.equals("PRO", ignoreCase = true)
}

data class VideoAnalysisUsage(
    val used: Int = 0,
    val limit: Int? = 1,
    val remaining: Int? = 1,
    val unlimited: Boolean = false,
    val resetAt: String? = null,
    val timezone: String = "Asia/Ho_Chi_Minh"
) {
    val progress: Float
        get() = if (unlimited || limit == null || limit <= 0) {
            0f
        } else {
            (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
        }
}

data class LiveAnalysisUsage(
    val available: Boolean = false,
    val unlimited: Boolean = false
)
