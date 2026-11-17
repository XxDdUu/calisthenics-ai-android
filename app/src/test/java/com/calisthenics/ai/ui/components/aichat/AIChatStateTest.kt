package com.calisthenics.ai.ui.components.aichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIChatStateTest {

    @Test
    fun testIdleState() {
        val state: AIChatState = AIChatState.Idle
        assertFalse(state.isGeneratingOrBusy)
        assertNull(state.statusText)
    }

    @Test
    fun testThinkingState() {
        val state: AIChatState = AIChatState.Thinking
        assertTrue(state.isGeneratingOrBusy)
        assertEquals("Thinking...", state.statusText)
    }

    @Test
    fun testGeneratingState() {
        val state: AIChatState = AIChatState.Generating
        assertTrue(state.isGeneratingOrBusy)
        assertEquals("Generating response...", state.statusText)
    }

    @Test
    fun testExecutingToolStateWithCustomAndDefaultLabel() {
        val customTool: AIChatState = AIChatState.ExecutingTool(label = "Fetching Planche workouts...")
        assertTrue(customTool.isGeneratingOrBusy)
        assertEquals("Fetching Planche workouts...", customTool.statusText)

        val defaultTool: AIChatState = AIChatState.ExecutingTool(label = null)
        assertTrue(defaultTool.isGeneratingOrBusy)
        assertEquals("Executing tool...", defaultTool.statusText)
    }

    @Test
    fun testCompletedState() {
        val state: AIChatState = AIChatState.Completed
        assertFalse(state.isGeneratingOrBusy)
        assertNull(state.statusText)
    }

    @Test
    fun testErrorState() {
        val state: AIChatState = AIChatState.Error("Failed to reach AI service")
        assertFalse(state.isGeneratingOrBusy)
        assertNull(state.statusText)
        assertEquals("Failed to reach AI service", (state as AIChatState.Error).message)
    }

    @Test
    fun testCancelledState() {
        val state: AIChatState = AIChatState.Cancelled
        assertFalse(state.isGeneratingOrBusy)
        assertNull(state.statusText)
    }
}
