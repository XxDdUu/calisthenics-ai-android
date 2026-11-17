package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AiConversationDetail
import com.calisthenics.ai.domain.model.AiConversationSummary
import com.calisthenics.ai.domain.model.AiMessage

interface AiChatRepository {

    suspend fun getConversations(): Result<List<AiConversationSummary>>

    suspend fun getLatestConversationSummary(): Result<AiConversationSummary?>

    suspend fun createConversation(title: String? = null): Result<AiConversationSummary>

    suspend fun getConversation(id: Long): Result<AiConversationDetail>

    suspend fun deleteConversation(id: Long): Result<Unit>

    suspend fun sendMessage(conversationId: Long, message: String, timezone: String? = null): Result<AiMessage>

    suspend fun sendQuickMessage(message: String, timezone: String? = null): Result<AiMessage>
}
