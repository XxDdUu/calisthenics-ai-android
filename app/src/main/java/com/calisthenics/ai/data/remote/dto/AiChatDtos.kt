package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AiConversationSummaryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("lastMessagePreview") val lastMessagePreview: String? = null,
    @SerializedName("lastMessageRole") val lastMessageRole: String? = null,
    @SerializedName("lastInteractionAt") val lastInteractionAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("messageCount") val messageCount: Int = 0
)

data class AiConversationDetailDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("messages") val messages: List<AiMessageDto> = emptyList(),
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class AiMessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
    @SerializedName("intent") val intent: String? = null,
    @SerializedName("metadata") val metadata: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class SendAiMessageRequestDto(
    @SerializedName("message") val message: String,
    @SerializedName("timezone") val timezone: String? = null
)

data class CreateAiConversationRequestDto(
    @SerializedName("title") val title: String? = null
)
