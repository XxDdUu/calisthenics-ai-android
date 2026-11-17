package com.calisthenics.ai.ui.locale

import java.util.Locale

/** Languages supported by the application. The tag is also the Android resource qualifier. */
enum class AppLanguage(
    val languageTag: String,
    val nativeName: String,
    val locale: Locale
) {
    ENGLISH("en", "English", Locale.ENGLISH),
    VIETNAMESE("vi", "Tiếng Việt", Locale.forLanguageTag("vi")),
    CHINESE("zh", "简体中文", Locale.SIMPLIFIED_CHINESE),
    KOREAN("ko", "한국어", Locale.KOREAN);

    companion object {
        fun fromTag(tag: String?): AppLanguage? = values().firstOrNull { it.languageTag == tag }

        fun fromLocale(locale: Locale): AppLanguage = fromTag(locale.language)
            ?: ENGLISH
    }
}
