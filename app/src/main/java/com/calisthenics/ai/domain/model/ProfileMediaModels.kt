package com.calisthenics.ai.domain.model

enum class ProfileType {
    COACH,
    ATHLETE
}

enum class ProfileMediaType {
    IMAGE,
    VIDEO
}

data class ProfileMediaItem(
    val id: Long,
    val userId: Long,
    val profileType: ProfileType,
    val mediaType: ProfileMediaType,
    val url: String,
    val thumbnailUrl: String? = null,
    val displayOrder: Int = 0,
    val createdAt: String? = null
)
