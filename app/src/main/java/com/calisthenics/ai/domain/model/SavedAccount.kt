package com.calisthenics.ai.domain.model

data class SavedAccount(
    val userId: Long,
    val email: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val role: String = "ROLE_USER",
    val isActive: Boolean = false,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

