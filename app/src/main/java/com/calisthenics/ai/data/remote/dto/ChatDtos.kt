package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatConversationDto(
    @SerializedName("id") val id: Long,
    @SerializedName("otherUserId") val otherUserId: Long,
    @SerializedName("otherUserName") val otherUserName: String,
    @SerializedName("otherUserAvatarUrl") val otherUserAvatarUrl: String? = null,
    @SerializedName("lastMessagePreview") val lastMessagePreview: String? = null,
    @SerializedName("unreadCount") val unreadCount: Long = 0,
    @SerializedName("lastMessageAt") val lastMessageAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class ChatMessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("senderId") val senderId: Long,
    @SerializedName("senderName") val senderName: String,
    @SerializedName("senderAvatarUrl") val senderAvatarUrl: String? = null,
    @SerializedName("content") val content: String,
    @SerializedName("clientMessageId") val clientMessageId: String? = null,
    @SerializedName("messageType") val messageType: String = "TEXT",
    @SerializedName("sharedPersonalRecord") val sharedPersonalRecord: SharedPersonalRecordDto? = null,
    @SerializedName("createdAt") val createdAt: String
)

data class CreateChatConversationRequestDto(
    @SerializedName("otherUserId") val otherUserId: Long
)

data class SendChatMessageRequestDto(
    @SerializedName("conversationId") val conversationId: Long? = null,
    @SerializedName("content") val content: String,
    @SerializedName("clientMessageId") val clientMessageId: String? = null
)

data class UnreadCountDto(
    @SerializedName("unreadCount") val unreadCount: Long
)

data class UserPresenceDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("status") val status: String,
    @SerializedName("lastSeen") val lastSeen: String? = null,
    @SerializedName("version") val version: Long = 0L
)

data class CoachClientRelationshipDto(
    @SerializedName("id") val id: Long,
    @SerializedName("coachId") val coachId: Long,
    @SerializedName("coachName") val coachName: String,
    @SerializedName("coachAvatarUrl") val coachAvatarUrl: String? = null,
    @SerializedName("coachExperienceLevel") val coachExperienceLevel: String? = null,
    @SerializedName("clientId") val clientId: Long,
    @SerializedName("clientName") val clientName: String,
    @SerializedName("clientAvatarUrl") val clientAvatarUrl: String? = null,
    @SerializedName("clientExperienceLevel") val clientExperienceLevel: String? = null,
    @SerializedName("requestedBy") val requestedBy: Long,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("coachGender") val coachGender: String? = null,
    @SerializedName("clientGender") val clientGender: String? = null
)
