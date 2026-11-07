package com.calisthenics.ai.domain.model

data class ChatConversation(
    val id: Long,
    val otherUserId: Long,
    val otherUserName: String,
    val otherUserAvatarUrl: String?,
    val lastMessagePreview: String?,
    val unreadCount: Long,
    val lastMessageAt: String?,
    val createdAt: String?,
    val updatedAt: String?
)

enum class MessageDeliveryStatus {
    SENDING,
    SENT,
    FAILED
}

data class ChatMessage(
    val id: Long,
    val conversationId: Long,
    val senderId: Long,
    val senderName: String,
    val senderAvatarUrl: String?,
    val content: String,
    val messageType: String,
    val createdAt: String,
    val isFromCurrentUser: Boolean = false,
    val clientMessageId: String? = null,
    val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT
)

data class ChatContact(
    val userId: Long,
    val displayName: String,
    val roleLabel: String
)

enum class UserPresenceStatus {
    ONLINE,
    OFFLINE
}

data class UserPresence(
    val userId: Long,
    val status: UserPresenceStatus,
    val lastSeen: String? = null,
    val version: Long = 0L
)
