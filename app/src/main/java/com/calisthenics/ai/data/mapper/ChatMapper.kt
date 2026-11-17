package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.ChatConversationDto
import com.calisthenics.ai.data.remote.dto.ChatMessageDto
import com.calisthenics.ai.data.remote.dto.CoachClientRelationshipDto
import com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto
import com.calisthenics.ai.data.remote.dto.UserPresenceDto
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.domain.model.UserPresenceStatus
import com.google.gson.Gson

private val chatGson = Gson()

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
    val parsedSharedPr = sharedPersonalRecord ?: if (messageType.equals("PR_SHARE", ignoreCase = true) && content.trim().startsWith("{")) {
        runCatching { chatGson.fromJson(content, SharedPersonalRecordDto::class.java) }.getOrNull()
    } else {
        null
    }
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
        deliveryStatus = com.calisthenics.ai.domain.model.MessageDeliveryStatus.SENT,
        sharedPr = parsedSharedPr
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
            roleLabel = "Client",
            avatarUrl = clientAvatarUrl,
            experienceLevel = clientExperienceLevel
        )
    } else if (currentUserId == clientId) {
        ChatContact(
            userId = coachId,
            displayName = coachName,
            roleLabel = "Coach",
            avatarUrl = coachAvatarUrl,
            experienceLevel = coachExperienceLevel
        )
    } else {
        null
    }
}
