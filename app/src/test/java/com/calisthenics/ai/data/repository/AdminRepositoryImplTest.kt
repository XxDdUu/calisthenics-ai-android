package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.remote.datasource.RemoteAdminDataSource
import com.calisthenics.ai.data.remote.dto.AdminAiChatResponseDto
import com.calisthenics.ai.data.remote.dto.AdminAiMessageDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException

class AdminRepositoryImplTest {

    private val remoteDataSource: RemoteAdminDataSource = mockk()
    private val repository = AdminRepositoryImpl.forTest(remoteDataSource, retryDelaysMs = listOf(0L, 0L))

    @Test
    fun `sendAdminAiChatIdempotent auto retries transient timeout and reuses identical idempotencyKey`() = runTest {
        val capturedKeys = mutableListOf<String?>()
        val responseDto = AdminAiChatResponseDto(
            conversationId = 55L,
            conversationTitle = "Admin Ops",
            userMessage = AdminAiMessageDto(1L, 55L, "USER", "Summarize activity", null, null),
            assistantMessage = AdminAiMessageDto(2L, 55L, "ASSISTANT", "Platform overview ready", "PLATFORM_OVERVIEW", null)
        )

        var callCount = 0
        coEvery {
            remoteDataSource.sendAdminAiChat(
                conversationId = any(),
                message = any(),
                range = any(),
                startDate = any(),
                endDate = any(),
                idempotencyKey = any()
            )
        } answers {
            capturedKeys += arg<String?>(5)
            callCount++
            if (callCount == 1) {
                throw SocketTimeoutException("First-request cold read timed out")
            }
            responseDto
        }

        val result = repository.sendAdminAiChatIdempotent(
            conversationId = null,
            message = "Summarize activity",
            range = "LAST_30_DAYS",
            idempotencyKey = "idem-first-msg-123"
        )

        assertTrue(result.isSuccess)
        assertEquals(55L, result.getOrThrow().conversationId)
        assertEquals(2, capturedKeys.size)
        assertEquals(listOf("idem-first-msg-123", "idem-first-msg-123"), capturedKeys)
    }

    @Test
    fun `sendAdminAiChatIdempotent does not retry non-transient 400 or 403 errors`() = runTest {
        val body = "forbidden".toResponseBody("text/plain".toMediaType())
        coEvery {
            remoteDataSource.sendAdminAiChat(any(), any(), any(), any(), any(), any())
        } throws HttpException(Response.error<Unit>(403, body))

        val result = repository.sendAdminAiChatIdempotent(
            conversationId = null,
            message = "Summarize activity",
            idempotencyKey = "idem-403-no-retry"
        )

        assertTrue(result.isFailure)
        coVerify(exactly = 1) {
            remoteDataSource.sendAdminAiChat(any(), any(), any(), any(), any(), "idem-403-no-retry")
        }
    }

    @Test
    fun `sendAdminAiChatIdempotent retries 503 service unavailable and succeeds cleanly`() = runTest {
        val body = "temporarily overloaded".toResponseBody("text/plain".toMediaType())
        val responseDto = AdminAiChatResponseDto(
            conversationId = 77L,
            conversationTitle = "Admin Ops",
            userMessage = AdminAiMessageDto(10L, 77L, "USER", "Hello", null, null),
            assistantMessage = AdminAiMessageDto(11L, 77L, "ASSISTANT", "Ready", "PLATFORM_OVERVIEW", null)
        )

        var attempt = 0
        coEvery {
            remoteDataSource.sendAdminAiChat(any(), any(), any(), any(), any(), any())
        } answers {
            attempt++
            if (attempt <= 2) throw HttpException(Response.error<Unit>(503, body))
            responseDto
        }

        val result = repository.sendAdminAiChatIdempotent(
            conversationId = null,
            message = "Hello",
            idempotencyKey = "idem-503-retry"
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 3) {
            remoteDataSource.sendAdminAiChat(any(), any(), any(), any(), any(), "idem-503-retry")
        }
    }
}
