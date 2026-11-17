package com.calisthenics.ai.ui.locale

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `all supported language tags are stable`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.fromTag("vi"))
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag("zh"))
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromTag("ko"))
    }

    @Test
    fun `unsupported persisted language is ignored`() {
        assertNull(AppLanguage.fromTag("fr"))
        assertNull(AppLanguage.fromTag(null))
    }

    @Test
    fun `regional locales resolve to supported language`() {
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromLocale(Locale.forLanguageTag("zh-CN")))
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromLocale(Locale.forLanguageTag("ko-KR")))
        assertEquals(AppLanguage.VIETNAMESE, AppLanguage.fromLocale(Locale.forLanguageTag("vi-VN")))
    }
}
