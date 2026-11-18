package com.calisthenics.ai.data.remote.api

import com.calisthenics.ai.BuildConfig
import com.calisthenics.ai.data.local.AccountSession
import com.calisthenics.ai.data.local.AuthRequestSession
import com.calisthenics.ai.data.local.SessionManager
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) : Authenticator {

    // Dedicated OkHttpClient without custom interceptors/authenticator to prevent recursion
    private val refreshHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        val path = request.url.encodedPath

        // 1. Never retry authentication endpoints
        if (isAuthPath(path)) {
            return null
        }

        // 2. Prevent infinite retry recursion: retry at most once.
        // A second 401 means this endpoint rejected the call after a refresh attempt.
        // That is not proof the refresh token is invalid, so keep the stored session.
        if (responseCount(response) >= 2) {
            return null
        }

        // 3. Do not retry requests with non-replayable bodies
        if (request.body?.isOneShot() == true) {
            return null
        }

        val requestSession = request.tag(AuthRequestSession::class.java)
        val requestToken = request.header("Authorization")?.removePrefix("Bearer ")?.trim()
        if (!isRequestForActiveSession(requestSession, requestToken)) {
            return null
        }

        // 4. Synchronized single-flight refresh block to prevent concurrent requests
        // from triggering multiple simultaneous refresh operations
        synchronized(refreshLock) {
            if (!isRequestForActiveSession(requestSession, requestToken)) {
                return null
            }
            val startSession = tokenManager.currentSessionSnapshot()
            val currentAccessToken = tokenManager.getAccessToken()

            // If another thread already refreshed the token while this thread was waiting,
            // retry with the newly acquired token immediately
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestToken) {
                return request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // Verify refresh token is available and not expired
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                if (!requestToken.isNullOrBlank() && isRequestForActiveSession(requestSession, requestToken)) {
                    handleSessionExpired()
                }
                return null
            }
            if (tokenManager.isRefreshTokenExpired()) {
                if (isRequestForActiveSession(requestSession, requestToken)) {
                    handleSessionExpired()
                }
                return null
            }

            // Perform the refresh operation
            val refreshResult = performRefresh(refreshToken)
            return if (refreshResult != null) {
                if (!isRequestForActiveSession(requestSession, requestToken)) {
                    return null
                }
                val expectedUserId = startSession.activeUserId?.takeIf { it > 0L }
                    ?: requestSession?.userId?.takeIf { it > 0L }
                if (expectedUserId != null && refreshResult.userId > 0L && refreshResult.userId != expectedUserId) {
                    return null
                }
                tokenManager.saveAuthToken(
                    accessToken = refreshResult.accessToken,
                    refreshToken = refreshResult.refreshToken,
                    userId = refreshResult.userId,
                    role = refreshResult.role
                )
                request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResult.accessToken}")
                    .build()
            } else if (lastRefreshWasNetworkError) {
                // Device is offline or network timeout: preserve session tokens and do not force logout
                null
            } else {
                if (isRequestForActiveSession(requestSession, requestToken)) {
                    handleSessionExpired()
                }
                null
            }
        }
    }

    private fun isRequestForActiveSession(
        requestSession: AuthRequestSession?,
        requestToken: String?
    ): Boolean {
        val currentSession = tokenManager.currentSessionSnapshot()
        val taggedUserId = requestSession?.userId?.takeIf { it > 0L }
        if (requestSession != null && taggedUserId != null) {
            val activeUserId = currentSession.activeUserId?.takeIf { it > 0L }
                ?: tokenManager.getUserId()?.takeIf { it > 0L }
            return taggedUserId == activeUserId &&
                requestSession.generation == currentSession.generation
        }
        val requestSub = tokenManager.extractUserIdFromToken(requestToken)?.takeIf { it > 0L }
        val currentUserId = currentSession.activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        if (requestSub != null && currentUserId != null && requestSub != currentUserId) {
            return false
        }
        return true
    }

    @Volatile
    var lastRefreshWasNetworkError = false
        internal set

    open fun performRefresh(refreshToken: String): AuthResponseDto? {
        lastRefreshWasNetworkError = false
        return try {
            val jsonBody = gson.toJson(mapOf("refreshToken" to refreshToken))
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)

            val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
            val refreshUrl = "$baseUrl/api/v1/auth/refresh"

            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post(body)
                .build()

            refreshHttpClient.newCall(refreshRequest).execute().use { refreshResponse ->
                if (refreshResponse.isSuccessful) {
                    val responseBody = refreshResponse.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        gson.fromJson(responseBody, AuthResponseDto::class.java)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            lastRefreshWasNetworkError = true
            null
        } catch (e: Exception) {
            lastRefreshWasNetworkError = true
            null
        }
    }

    private fun handleSessionExpired() {
        tokenManager.clear()
        sessionManager.notifySessionExpired()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private fun isAuthPath(path: String): Boolean {
        return path.contains("/api/v1/auth/register") ||
                path.contains("/api/v1/auth/login") ||
                path.contains("/api/v1/auth/refresh") ||
                path.contains("/api/v1/auth/firebase") ||
                path.contains("/api/v1/auth/google")
    }
}
