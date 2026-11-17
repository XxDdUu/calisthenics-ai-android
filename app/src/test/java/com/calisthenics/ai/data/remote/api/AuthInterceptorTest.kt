package com.calisthenics.ai.data.remote.api

import com.calisthenics.ai.data.local.TokenManager
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var authInterceptor: AuthInterceptor

    @Before
    fun setup() {
        tokenManager = mockk(relaxed = true)
        authInterceptor = AuthInterceptor(tokenManager)
    }

    @Test
    fun `attaches bearer token to protected endpoints`() {
        every { tokenManager.getAccessToken() } returns "valid-test-access-token"

        val originalRequest = Request.Builder()
            .url("http://localhost:8080/api/v1/users/me")
            .build()

        var interceptedRequest: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } answers {
            interceptedRequest = firstArg()
            Response.Builder()
                .request(interceptedRequest!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        authInterceptor.intercept(chain)

        assertEquals("Bearer valid-test-access-token", interceptedRequest?.header("Authorization"))
    }

    @Test
    fun `skips authorization header for login endpoint`() {
        every { tokenManager.getAccessToken() } returns "existing-token"

        val loginRequest = Request.Builder()
            .url("http://localhost:8080/api/v1/auth/login")
            .build()

        var interceptedRequest: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns loginRequest
        every { chain.proceed(any()) } answers {
            interceptedRequest = firstArg()
            Response.Builder()
                .request(interceptedRequest!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        authInterceptor.intercept(chain)

        assertNull(interceptedRequest?.header("Authorization"))
    }

    @Test
    fun `skips authorization header for refresh endpoint`() {
        every { tokenManager.getAccessToken() } returns "expired-access-token"

        val refreshRequest = Request.Builder()
            .url("http://localhost:8080/api/v1/auth/refresh")
            .build()

        var interceptedRequest: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns refreshRequest
        every { chain.proceed(any()) } answers {
            interceptedRequest = firstArg()
            Response.Builder()
                .request(interceptedRequest!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        authInterceptor.intercept(chain)

        assertNull(interceptedRequest?.header("Authorization"))
    }

    @Test
    fun `attaches correlation id header to all requests`() {
        val request = Request.Builder()
            .url("http://localhost:8080/api/v1/chat/conversations")
            .build()

        var interceptedRequest: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            interceptedRequest = firstArg()
            Response.Builder()
                .request(interceptedRequest!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        authInterceptor.intercept(chain)

        org.junit.Assert.assertNotNull(interceptedRequest?.header("X-Correlation-ID"))
    }

    @Test
    fun `skips authorization header for firebase auth endpoint`() {
        every { tokenManager.getAccessToken() } returns "existing-token"

        val firebaseRequest = Request.Builder()
            .url("http://localhost:8080/api/v1/auth/firebase")
            .build()

        var interceptedRequest: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns firebaseRequest
        every { chain.proceed(any()) } answers {
            interceptedRequest = firstArg()
            Response.Builder()
                .request(interceptedRequest!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        authInterceptor.intercept(chain)

        assertNull(interceptedRequest?.header("Authorization"))
    }
}

