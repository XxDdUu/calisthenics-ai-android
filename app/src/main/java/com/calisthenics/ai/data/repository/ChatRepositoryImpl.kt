package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.mapper.toContact
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.data.remote.dto.CreateChatConversationRequestDto
import com.calisthenics.ai.data.remote.dto.SendChatMessageRequestDto
import com.calisthenics.ai.data.remote.websocket.ChatWebSocketClient
import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: CalisthenicsApi,
    private val tokenManager: TokenManager,
    private val webSocketClient: ChatWebSocketClient
) : ChatRepository {

    override suspend fun getConversations(): Result<List<ChatConversation>> {
        return runCatching {
            api.getChatConversations().map { it.toDomain() }
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun createOrGetConversation(otherUserId: Long): Result<ChatConversation> {
        return runCatching {
            api.createOrGetChatConversation(CreateChatConversationRequestDto(otherUserId)).toDomain()
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun getMessages(
        conversationId: Long,
        page: Int,
        size: Int
    ): Result<List<ChatMessage>> {
        return runCatching {
            val currentUserId = getCurrentUserId()
            api.getChatMessages(conversationId, page, size, "desc")
                .map { it.toDomain(currentUserId) }
                .reversed() // Returns in chronological order (oldest to newest within this batch)
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun sendMessage(
        conversationId: Long,
        content: String,
        clientMessageId: String?
    ): Result<ChatMessage> {
        return runCatching {
            val currentUserId = getCurrentUserId()
            api.sendChatMessage(
                conversationId,
                SendChatMessageRequestDto(
                    conversationId = conversationId,
                    content = content,
                    clientMessageId = clientMessageId
                )
            ).toDomain(currentUserId)
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun markAsRead(conversationId: Long): Result<Unit> {
        return runCatching {
            api.markChatConversationAsRead(conversationId)
            Unit
        }
    }

    override suspend fun getUnreadCount(): Result<Long> {
        return runCatching {
            api.getChatUnreadCount().unreadCount
        }
    }

    override suspend fun getConversationPresence(): Result<List<UserPresence>> {
        return runCatching { api.getChatPresence().map { it.toDomain() } }
    }

    override suspend fun getAvailableContacts(): Result<List<ChatContact>> {
        return runCatching {
            val currentUserId = getCurrentUserId() ?: return@runCatching emptyList()
            api.getCoachClientRelationships()
                .mapNotNull { it.toContact(currentUserId) }
                .distinctBy { it.userId }
        }
    }

    override fun getIncomingMessages(): Flow<ChatMessage> {
        return webSocketClient.incomingMessages.map { it.toDomain(getCurrentUserId()) }
    }

    override fun getPresenceUpdates(): Flow<UserPresence> {
        return webSocketClient.presenceUpdates.map { it.toDomain() }
    }

    override fun getWebSocketState(): StateFlow<WebSocketState> {
        return webSocketClient.connectionState
    }

    override fun connectWebSocket() {
        webSocketClient.connect()
        webSocketClient.subscribeToUserQueue()
        webSocketClient.subscribeToPresence()
    }

    override fun disconnectWebSocket() {
        webSocketClient.disconnect()
    }

    override fun subscribeToConversation(conversationId: Long) {
        webSocketClient.subscribeToConversation(conversationId)
    }

    override fun unsubscribeFromConversation(conversationId: Long) {
        webSocketClient.unsubscribeFromConversation(conversationId)
    }

    override fun getCurrentUserId(): Long? {
        return tokenManager.currentSessionSnapshot().activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
    }
}
