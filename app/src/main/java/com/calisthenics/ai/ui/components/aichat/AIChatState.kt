package com.calisthenics.ai.ui.components.aichat

/**
 * Standardized AI state model across all AI chat interfaces in Calisthenics AI.
 * Follows the AI Workout Planner lifecycle and interaction semantics.
 */
sealed interface AIChatState {
    data object Idle : AIChatState
    data object Thinking : AIChatState
    data object Generating : AIChatState
    data class ExecutingTool(val label: String? = null) : AIChatState
    data object Completed : AIChatState
    data class Error(val message: String) : AIChatState
    data object Cancelled : AIChatState

    val isGeneratingOrBusy: Boolean
        get() = this is Thinking || this is Generating || this is ExecutingTool

    val statusText: String?
        get() = when (this) {
            is Thinking -> "Thinking..."
            is Generating -> "Generating response..."
            is ExecutingTool -> label ?: "Executing tool..."
            else -> null
        }
}
