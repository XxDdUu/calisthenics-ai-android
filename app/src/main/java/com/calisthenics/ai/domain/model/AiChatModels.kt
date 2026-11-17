package com.calisthenics.ai.domain.model

data class AiConversationSummary(
    val id: Long,
    val title: String,
    val lastMessagePreview: String,
    val lastMessageRole: String?,
    val lastInteractionAt: String?,
    val createdAt: String?,
    val messageCount: Int
)

data class AiConversationDetail(
    val id: Long,
    val title: String,
    val messages: List<AiMessage>,
    val createdAt: String?,
    val updatedAt: String?
)

data class AiMessage(
    val id: Long,
    val conversationId: Long,
    val role: String, // "user", "assistant"
    val content: String,
    val intent: String?,
    val metadata: String?,
    val actionCard: AiActionCard? = null,
    val createdAt: String?
) {
    val isFromUser: Boolean get() = role.equals("user", ignoreCase = true)
}

sealed interface AiActionCard {
    data class PrRecorded(
        val prId: Long,
        val exerciseName: String,
        val recordType: String,
        val value: Double,
        val unit: String,
        val formattedValue: String?,
        val previousValue: Double?,
        val improvementPercentage: Double?
    ) : AiActionCard

    data class WorkoutPlan(
        val planId: Long?,
        val planName: String,
        val description: String?,
        val durationWeeks: Int,
        val splitType: String?
    ) : AiActionCard

    data class ProgressSummary(
        val totalPrs: Int,
        val completedSessionsCount: Int,
        val uniqueExercisesCount: Long
    ) : AiActionCard

    data class ProfileUpdate(
        val success: Boolean,
        val updatedFields: Map<String, String>,
        val displayName: String? = null,
        val heightCm: Double? = null,
        val weightKg: Double? = null,
        val experienceLevel: String? = null,
        val primaryGoals: String? = null,
        val bio: String? = null,
        val isAmbiguous: Boolean = false
    ) : AiActionCard
}
