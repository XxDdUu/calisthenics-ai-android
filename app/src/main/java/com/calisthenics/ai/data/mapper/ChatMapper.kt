package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.ChatConversationDto
import com.calisthenics.ai.data.remote.dto.ChatMessageDto
import com.calisthenics.ai.data.remote.dto.CoachClientRelationshipDto
import com.calisthenics.ai.data.remote.dto.UserPresenceDto
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.domain.model.UserPresenceStatus

fun ChatConversationDto.toDomain(): ChatConversation {
    return ChatConversation(
        id = id,
        otherUserId = otherUserId,
        otherUserName = otherUserName,
        otherUserAvatarUrl = otherUserAvatarUrl,
        lastMessagePreview = lastMessagePreview,
        unreadCount = unreadCount,
        lastMessageAt = lastMessageAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ChatMessageDto.toDomain(currentUserId: Long? = null): ChatMessage {
    return ChatMessage(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderName = senderName,
        senderAvatarUrl = senderAvatarUrl,
        content = content,
        messageType = messageType,
        createdAt = createdAt,
        isFromCurrentUser = currentUserId != null && currentUserId == senderId,
        clientMessageId = clientMessageId,
        deliveryStatus = com.calisthenics.ai.domain.model.MessageDeliveryStatus.SENT
    )
}

fun UserPresenceDto.toDomain(): UserPresence {
    val parsedStatus = when (status.uppercase()) {
        "ONLINE" -> UserPresenceStatus.ONLINE
        else -> UserPresenceStatus.OFFLINE
    }
    return UserPresence(
        userId = userId,
        status = parsedStatus,
        lastSeen = lastSeen,
        version = version
    )
}

fun CoachClientRelationshipDto.toContact(currentUserId: Long): ChatContact? {
    // Only return the other participant in active relationships
    if (status != "ACTIVE") return null
    return if (currentUserId == coachId) {
        ChatContact(
            userId = clientId,
            displayName = clientName,
            roleLabel = "Client"
        )
    } else if (currentUserId == clientId) {
        ChatContact(
            userId = coachId,
            displayName = coachName,
            roleLabel = "Coach"
        )
    } else {
        null
    }
}
