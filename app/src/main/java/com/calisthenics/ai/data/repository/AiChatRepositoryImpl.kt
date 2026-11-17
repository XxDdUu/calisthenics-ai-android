package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.data.remote.dto.CreateAiConversationRequestDto
import com.calisthenics.ai.data.remote.dto.SendAiMessageRequestDto
import com.calisthenics.ai.domain.model.AiConversationDetail
import com.calisthenics.ai.domain.model.AiConversationSummary
import com.calisthenics.ai.domain.model.AiMessage
import com.calisthenics.ai.domain.repository.AiChatRepository
import javax.inject.Inject

class AiChatRepositoryImpl @Inject constructor(
    private val api: CalisthenicsApi
) : AiChatRepository {

    override suspend fun getConversations(): Result<List<AiConversationSummary>> {
        return runCatching {
            api.getAiConversations().map { it.toDomain() }
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun getLatestConversationSummary(): Result<AiConversationSummary?> {
        return runCatching {
            val response = api.getLatestAiConversation()
            if (response.isSuccessful) {
                response.body()?.toDomain()
            } else {
                null
            }
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun createConversation(title: String?): Result<AiConversationSummary> {
        return runCatching {
            api.createAiConversation(CreateAiConversationRequestDto(title)).toDomain()
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun getConversation(id: Long): Result<AiConversationDetail> {
        return runCatching {
            api.getAiConversation(id).toDomain()
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun deleteConversation(id: Long): Result<Unit> {
        return runCatching {
            val response = api.deleteAiConversation(id)
            if (!response.isSuccessful && response.code() != 404) {
                throw Exception("Failed to delete conversation: ${response.code()}")
            }
            Unit
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun sendMessage(
        conversationId: Long,
        message: String,
        timezone: String?
    ): Result<AiMessage> {
        return runCatching {
            api.sendAiMessage(
                id = conversationId,
                request = SendAiMessageRequestDto(message = message, timezone = timezone)
            ).toDomain()
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }

    override suspend fun sendQuickMessage(
        message: String,
        timezone: String?
    ): Result<AiMessage> {
        return runCatching {
            api.sendQuickAiMessage(
                request = SendAiMessageRequestDto(message = message, timezone = timezone)
            ).toDomain()
        }.recoverCatching {
            throw Exception(NetworkErrorParser.toUserMessage(it), it)
        }
    }
}
