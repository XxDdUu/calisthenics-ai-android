package com.calisthenics.ai.ui.theme

enum class ThemeMode {
    LIGHT,
    DARK,
    DARK_V2,
    NEUTRAL,
    SYSTEM;

    companion object {
        fun fromName(name: String?): ThemeMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DARK
        }
    }
}
