package com.calisthenics.ai.data.remote.api

import com.calisthenics.ai.BuildConfig
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

        // 2. Prevent infinite retry recursion: retry at most once
        if (responseCount(response) >= 2) {
            handleSessionExpired()
            return null
        }

        // 3. Do not retry requests with non-replayable bodies
        if (request.body?.isOneShot() == true) {
            return null
        }

        // 4. Synchronized single-flight refresh block to prevent concurrent requests
        // from triggering multiple simultaneous refresh operations
        synchronized(refreshLock) {
            val currentAccessToken = tokenManager.getAccessToken()
            val requestToken = request.header("Authorization")?.removePrefix("Bearer ")?.trim()

            // If another thread already refreshed the token while this thread was waiting,
            // retry with the newly acquired token immediately
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestToken) {
                return request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // Verify refresh token is available and not expired
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken.isNullOrBlank() || tokenManager.isRefreshTokenExpired()) {
                handleSessionExpired()
                return null
            }

            // Perform the refresh operation
            val refreshResult = performRefresh(refreshToken)
            return if (refreshResult != null) {
                tokenManager.saveAuthToken(
                    accessToken = refreshResult.accessToken,
                    refreshToken = refreshResult.refreshToken,
                    userId = refreshResult.userId
                )
                request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResult.accessToken}")
                    .build()
            } else {
                handleSessionExpired()
                null
            }
        }
    }

    open fun performRefresh(refreshToken: String): AuthResponseDto? {
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

            val refreshResponse = refreshHttpClient.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string()
                if (!responseBody.isNullOrBlank()) {
                    gson.fromJson(responseBody, AuthResponseDto::class.java)
                } else null
            } else {
                null
            }
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
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
                path.contains("/api/v1/auth/firebase")
    }
}

