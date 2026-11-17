package com.calisthenics.ai.domain.model

enum class Gender(val apiValue: String) {
    MALE("MALE"),
    FEMALE("FEMALE");

    companion object {
        fun fromApi(value: String?): Gender? = when (value?.trim()?.uppercase()) {
            "MALE" -> MALE
            "FEMALE" -> FEMALE
            else -> null
        }
    }
}
