package com.calisthenics.ai.data.remote.dto

import com.calisthenics.ai.domain.model.LiveAnalysisUsage
import com.calisthenics.ai.domain.model.MembershipUsage
import com.calisthenics.ai.domain.model.UserUsage
import com.calisthenics.ai.domain.model.VideoAnalysisUsage
import com.google.gson.annotations.SerializedName

data class UserUsageResponseDto(
    @SerializedName("membership")
    val membership: MembershipUsageDto? = null,
    @SerializedName(value = "videoAnalysis", alternate = ["video_analysis"])
    val videoAnalysis: VideoAnalysisUsageDto? = null,
    @SerializedName(value = "liveAnalysis", alternate = ["live_analysis"])
    val liveAnalysis: LiveAnalysisUsageDto? = null
) {
    fun toDomain(): UserUsage {
        return UserUsage(
            membership = membership?.toDomain() ?: MembershipUsage(),
            videoAnalysis = videoAnalysis?.toDomain() ?: VideoAnalysisUsage(),
            liveAnalysis = liveAnalysis?.toDomain() ?: LiveAnalysisUsage()
        )
    }
}

data class MembershipUsageDto(
    @SerializedName("tier")
    val tier: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName(value = "expiresAt", alternate = ["expires_at"])
    val expiresAt: String? = null,
    @SerializedName("source")
    val source: String? = null
) {
    fun toDomain(): MembershipUsage {
        val tierValue = tier?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: "FREE"
        val statusValue = status?.trim()?.takeIf { it.isNotBlank() } ?: "ACTIVE"
        return MembershipUsage(
            tier = tierValue,
            status = statusValue,
            expiresAt = expiresAt,
            source = source?.takeIf { it.isNotBlank() }
        )
    }
}

data class VideoAnalysisUsageDto(
    @SerializedName("used")
    val used: Int? = null,
    @SerializedName("limit")
    val limit: Int? = null,
    @SerializedName("remaining")
    val remaining: Int? = null,
    @SerializedName(value = "unlimited", alternate = ["isUnlimited", "is_unlimited"])
    val unlimited: Boolean? = null,
    @SerializedName(value = "resetAt", alternate = ["reset_at", "resetsAt", "resets_at"])
    val resetAt: String? = null,
    @SerializedName("timezone")
    val timezone: String? = null
) {
    fun toDomain(): VideoAnalysisUsage {
        val isUnlimited = unlimited == true || (limit != null && limit < 0)
        val resolvedUsed = (used ?: 0).coerceAtLeast(0)
        val resolvedLimit = if (isUnlimited) null else limit
        val resolvedRemaining = if (isUnlimited) null else (remaining ?: resolvedLimit?.minus(resolvedUsed)?.coerceAtLeast(0))
        return VideoAnalysisUsage(
            used = resolvedUsed,
            limit = resolvedLimit,
            remaining = resolvedRemaining,
            unlimited = isUnlimited,
            resetAt = resetAt,
            timezone = timezone?.takeIf { it.isNotBlank() } ?: "Asia/Ho_Chi_Minh"
        )
    }
}

data class LiveAnalysisUsageDto(
    @SerializedName(value = "available", alternate = ["isAvailable", "is_available"])
    val available: Boolean? = null,
    @SerializedName(value = "unlimited", alternate = ["isUnlimited", "is_unlimited"])
    val unlimited: Boolean? = null
) {
    fun toDomain(): LiveAnalysisUsage {
        return LiveAnalysisUsage(
            available = available ?: false,
            unlimited = unlimited ?: false
        )
    }
}
