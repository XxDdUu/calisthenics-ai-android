package com.calisthenics.ai.data.remote.api

import app.cash.turbine.test
import com.calisthenics.ai.data.local.SessionManager
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Response
import okio.BufferedSink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthenticatorTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var sessionManager: SessionManager
    private lateinit var authenticator: TokenAuthenticator

    @Before
    fun setup() {
        tokenManager = mockk(relaxed = true)
        sessionManager = SessionManager()
        authenticator = spyk(TokenAuthenticator(tokenManager, sessionManager))
    }

    private fun create401Response(
        url: String = "http://localhost:8080/api/v1/users/me",
        authorizationHeader: String? = "Bearer expired-token",
        priorResponse: Response? = null,
        body: RequestBody? = null
    ): Response {
        val requestBuilder = Request.Builder().url(url)
        if (authorizationHeader != null) {
            requestBuilder.header("Authorization", authorizationHeader)
        }
        if (body != null) {
            requestBuilder.post(body)
        }
        val request = requestBuilder.build()

        val responseBuilder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")

        if (priorResponse != null) {
            responseBuilder.priorResponse(
                priorResponse.newBuilder().body(null).build()
            )
        }

        return responseBuilder
            .body("{\"status\":401,\"message\":\"Token has expired\"}".toResponseBody("application/json".toMediaType()))
            .build()
    }

    @Test
    fun `successful refresh retries original request with new access token`() {
        every { tokenManager.getAccessToken() } returns "expired-token"
        every { tokenManager.getRefreshToken() } returns "valid-refresh-token"
        every { tokenManager.isRefreshTokenExpired() } returns false

        val refreshResponse = AuthResponseDto(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            userId = 42L,
            email = "user@test.com",
            fullName = "Test Athlete",
            role = "ROLE_USER"
        )
        every { authenticator.performRefresh("valid-refresh-token") } returns refreshResponse

        val response = create401Response()
        val retryRequest = authenticator.authenticate(null, response)

        assertNotNull(retryRequest)
        assertEquals("Bearer new-access-token", retryRequest?.header("Authorization"))
        verify {
            tokenManager.saveAuthToken("new-access-token", "new-refresh-token", 42L, "ROLE_USER")
        }
    }

    @Test
    fun `concurrent 401 requests trigger single refresh and reuse refreshed token`() {
        var currentToken = "expired-token"
        every { tokenManager.getAccessToken() } answers { currentToken }
        every { tokenManager.getRefreshToken() } returns "valid-refresh-token"
        every { tokenManager.isRefreshTokenExpired() } returns false

        val refreshCalls = AtomicInteger(0)
        every { authenticator.performRefresh("valid-refresh-token") } answers {
            refreshCalls.incrementAndGet()
            Thread.sleep(50) // simulate network latency
            AuthResponseDto(
                accessToken = "fresh-access-token",
                refreshToken = "fresh-refresh-token",
                userId = 10L,
                email = "user@test.com",
                fullName = "Test Athlete",
                role = "ROLE_USER"
            )
        }
        every { tokenManager.saveAuthToken(any(), any(), any(), any()) } answers {
            currentToken = firstArg()
        }

        val executor = Executors.newFixedThreadPool(4)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(4)
        val retryRequests = mutableListOf<Request?>()

        repeat(4) {
            executor.submit {
                startLatch.await()
                val response = create401Response()
                val retry = authenticator.authenticate(null, response)
                synchronized(retryRequests) {
                    retryRequests.add(retry)
                }
                doneLatch.countDown()
            }
        }

        startLatch.countDown()
        doneLatch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(4, retryRequests.size)
        // Ensure only ONE network refresh was actually made
        assertEquals(1, refreshCalls.get())
        // All requests received retry with the fresh token
        retryRequests.forEach { req ->
            assertNotNull(req)
            assertEquals("Bearer fresh-access-token", req?.header("Authorization"))
        }
    }

    @Test
    fun `failed refresh clears session and notifies session expired`() = runTest {
        every { tokenManager.getAccessToken() } returns "expired-token"
        every { tokenManager.getRefreshToken() } returns "revoked-refresh-token"
        every { tokenManager.isRefreshTokenExpired() } returns false
        every { authenticator.performRefresh("revoked-refresh-token") } returns null

        sessionManager.sessionExpiredEvent.test {
            val response = create401Response()
            val retryRequest = authenticator.authenticate(null, response)

            assertNull(retryRequest)
            verify { tokenManager.clear() }
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `network error during token refresh preserves session and does not notify session expired`() = runTest {
        every { tokenManager.getAccessToken() } returns "expired-token"
        every { tokenManager.getRefreshToken() } returns "valid-refresh-token"
        every { tokenManager.isRefreshTokenExpired() } returns false
        every { authenticator.performRefresh("valid-refresh-token") } answers {
            authenticator.lastRefreshWasNetworkError = true
            null
        }

        sessionManager.sessionExpiredEvent.test {
            val response = create401Response()
            val retryRequest = authenticator.authenticate(null, response)

            assertNull(retryRequest)
            // Session tokens must NOT be cleared when offline/network failure occurs
            verify(exactly = 0) { tokenManager.clear() }
            expectNoEvents()
        }
    }

    @Test
    fun `expired refresh token immediately clears session without calling refresh`() = runTest {
        every { tokenManager.getAccessToken() } returns "expired-token"
        every { tokenManager.getRefreshToken() } returns "expired-refresh-token"
        every { tokenManager.isRefreshTokenExpired() } returns true

        sessionManager.sessionExpiredEvent.test {
            val response = create401Response()
            val retryRequest = authenticator.authenticate(null, response)

            assertNull(retryRequest)
            verify(exactly = 0) { authenticator.performRefresh(any()) }
            verify { tokenManager.clear() }
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `auth endpoints are not retried on 401`() {
        val loginResponse = create401Response(url = "http://localhost:8080/api/v1/auth/login")
        val registerResponse = create401Response(url = "http://localhost:8080/api/v1/auth/register")
        val refreshResponse = create401Response(url = "http://localhost:8080/api/v1/auth/refresh")

        assertNull(authenticator.authenticate(null, loginResponse))
        assertNull(authenticator.authenticate(null, registerResponse))
        assertNull(authenticator.authenticate(null, refreshResponse))
        verify(exactly = 0) { authenticator.performRefresh(any()) }
    }

    @Test
    fun `requests that already failed once are not retried and do not clear a valid session`() = runTest {
        val priorResponse = create401Response()
        val secondResponse = create401Response(priorResponse = priorResponse)

        sessionManager.sessionExpiredEvent.test {
            val retryRequest = authenticator.authenticate(null, secondResponse)
            assertNull(retryRequest)
            verify(exactly = 0) { tokenManager.clear() }
            expectNoEvents()
        }
    }

    @Test
    fun `one shot non replayable request body is not retried`() {
        val oneShotBody = object : RequestBody() {
            override fun contentType() = "text/plain".toMediaType()
            override fun writeTo(sink: BufferedSink) {}
            override fun isOneShot() = true
        }
        val response = create401Response(body = oneShotBody)

        val retryRequest = authenticator.authenticate(null, response)
        assertNull(retryRequest)
        verify(exactly = 0) { authenticator.performRefresh(any()) }
    }
}
