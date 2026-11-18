package com.calisthenics.ai.ui.components.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserPlainTextTest {

    @Test
    fun toPlainText_stripsMarkdownArtifactsAndPreservesReadableFormatting() {
        val markdown = """
            ### Monday — Push & Isometric Hold

            Focus on **clean lockouts** and *scapular protraction* with `3s` pauses.

            1. **Push-Up** — 4 sets × 12 reps
            2. **Tuck Planche** — 4 sets × 15s hold

            - Warm up wrists thoroughly
            - Check [form guide](https://calisthenics.ai/guides/planche)

            ```kotlin
            val holdSeconds = 15
            ```
        """.trimIndent()

        val plain = MarkdownParser.toPlainText(markdown)

        assertFalse(plain.contains("###"))
        assertFalse(plain.contains("**"))
        assertFalse(plain.contains("```"))
        assertFalse(plain.contains("`"))
        assertTrue(plain.contains("Monday — Push & Isometric Hold"))
        assertTrue(plain.contains("Focus on clean lockouts and scapular protraction with 3s pauses."))
        assertTrue(plain.contains("1. Push-Up — 4 sets × 12 reps"))
        assertTrue(plain.contains("2. Tuck Planche — 4 sets × 15s hold"))
        assertTrue(plain.contains("• Warm up wrists thoroughly"))
        assertTrue(plain.contains("Check form guide (https://calisthenics.ai/guides/planche)"))
        assertTrue(plain.contains("val holdSeconds = 15"))
    }

    @Test
    fun toPlainText_handlesBlankInputGracefully() {
        assertEquals("", MarkdownParser.toPlainText(""))
        assertEquals("Plain message", MarkdownParser.toPlainText("Plain message"))
    }
}
