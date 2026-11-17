package com.calisthenics.ai.data.remote.api

import com.calisthenics.ai.data.local.AuthRequestSession
import com.calisthenics.ai.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        val requestBuilder = request.newBuilder()

        // Always attach a correlation ID and user's local timezone for consistent date classification
        if (request.header("X-Correlation-ID").isNullOrBlank()) {
            requestBuilder.header("X-Correlation-ID", java.util.UUID.randomUUID().toString())
        }
        if (request.header("X-Timezone").isNullOrBlank()) {
            requestBuilder.header("X-Timezone", java.time.ZoneId.systemDefault().id)
        }

        // Skip adding Authorization header for public authentication endpoints
        if (isPublicAuthPath(path)) {
            return chain.proceed(requestBuilder.build())
        }

        val session = tokenManager.currentSessionSnapshot()
        val resolvedUserId = session.activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        val token = if (resolvedUserId != null) {
            tokenManager.getAccessTokenFor(resolvedUserId)?.takeIf { it.isNotBlank() }
                ?: tokenManager.getAccessToken()
        } else {
            tokenManager.getAccessToken()
        }
        if (resolvedUserId != null || session.generation > 0L) {
            requestBuilder.tag(
                AuthRequestSession::class.java,
                AuthRequestSession(resolvedUserId, session.generation)
            )
        }
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }

    private fun isPublicAuthPath(path: String): Boolean {
        return path.contains("/api/v1/auth/register") ||
                path.contains("/api/v1/auth/login") ||
                path.contains("/api/v1/auth/firebase") ||
                path.contains("/api/v1/auth/google") ||
                path.contains("/api/v1/auth/refresh")
    }
}
