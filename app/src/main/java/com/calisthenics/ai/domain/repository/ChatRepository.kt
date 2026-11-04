package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.UserPresence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

interface ChatRepository {
    suspend fun getConversations(): Result<List<ChatConversation>>
    suspend fun createOrGetConversation(otherUserId: Long): Result<ChatConversation>
    suspend fun getMessages(conversationId: Long, page: Int = 0, size: Int = 50): Result<List<ChatMessage>>
    suspend fun sendMessage(conversationId: Long, content: String, clientMessageId: String?): Result<ChatMessage>
    suspend fun sendMessage(conversationId: Long, content: String): Result<ChatMessage> = sendMessage(conversationId, content, null)
    suspend fun markAsRead(conversationId: Long): Result<Unit>
    suspend fun getUnreadCount(): Result<Long>
    suspend fun getConversationPresence(): Result<List<UserPresence>> = Result.success(emptyList())
    suspend fun getAvailableContacts(): Result<List<ChatContact>>
    fun getIncomingMessages(): Flow<ChatMessage>
    fun getPresenceUpdates(): Flow<UserPresence> = emptyFlow()
    fun getWebSocketState(): StateFlow<WebSocketState>
    fun connectWebSocket()
    fun disconnectWebSocket()
    fun subscribeToConversation(conversationId: Long)
    fun unsubscribeFromConversation(conversationId: Long)
    fun getCurrentUserId(): Long?
}
