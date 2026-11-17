package com.calisthenics.ai.domain.model

enum class OnboardingStatus(val apiValue: String) {
    NOT_SHOWN("NOT_SHOWN"),
    COMPACT_BANNER("COMPACT_BANNER"),
    DISMISSED("DISMISSED");

    companion object {
        fun fromApi(value: String?): OnboardingStatus {
            return when (value?.trim()?.uppercase()) {
                "COMPACT_BANNER" -> COMPACT_BANNER
                "DISMISSED" -> DISMISSED
                else -> NOT_SHOWN
            }
        }
    }
}
