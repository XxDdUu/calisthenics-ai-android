package com.calisthenics.ai.data.remote.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Collections
import java.util.WeakHashMap

class ApiHttpException(
    val statusCode: Int,
    override val message: String,
    val errorCode: String? = null,
    val resetsAt: String? = null,
    val timezone: String? = null,
    val membershipTier: String? = null,
    val remainingToday: Int? = null,
    val dailyLimit: Int? = null,
    cause: HttpException? = null
) : RuntimeException(message, cause)

data class ParsedApiError(
    val statusCode: Int? = null,
    val userMessage: String,
    val errorCode: String? = null,
    val resetsAt: String? = null,
    val timezone: String? = null,
    val membershipTier: String? = null,
    val remainingToday: Int? = null,
    val dailyLimit: Int? = null
)

object NetworkErrorParser {

    private val parsedHttpCache: MutableMap<HttpException, ParsedApiError> =
        Collections.synchronizedMap(WeakHashMap())

    fun toUserMessage(throwable: Throwable): String {
        return parseApiError(throwable).userMessage
    }

    fun findHttpException(throwable: Throwable?): HttpException? {
        var current = throwable
        val visited = HashSet<Throwable>()
        while (current != null && visited.add(current)) {
            if (current is HttpException) return current
            current = current.cause
        }
        return null
    }

    fun httpStatusCode(throwable: Throwable?): Int? {
        var current = throwable
        val visited = HashSet<Throwable>()
        while (current != null && visited.add(current)) {
            when (current) {
                is ApiHttpException -> return current.statusCode
                is HttpException -> return current.code()
            }
            current = current.cause
        }
        return null
    }

    fun toPreservedException(throwable: Throwable): Throwable {
        if (throwable is ApiHttpException) return throwable
        val httpEx = findHttpException(throwable)
        if (httpEx != null) {
            val parsed = parseHttpException(httpEx)
            return ApiHttpException(
                statusCode = httpEx.code(),
                message = parsed.userMessage,
                errorCode = parsed.errorCode,
                resetsAt = parsed.resetsAt,
                timezone = parsed.timezone,
                membershipTier = parsed.membershipTier,
                remainingToday = parsed.remainingToday,
                dailyLimit = parsed.dailyLimit,
                cause = httpEx
            )
        }
        return Exception(toUserMessage(throwable), throwable)
    }

    fun parseApiError(throwable: Throwable): ParsedApiError {
        if (throwable is ApiHttpException) {
            return ParsedApiError(
                statusCode = throwable.statusCode,
                userMessage = throwable.message,
                errorCode = throwable.errorCode,
                resetsAt = throwable.resetsAt,
                timezone = throwable.timezone,
                membershipTier = throwable.membershipTier,
                remainingToday = throwable.remainingToday,
                dailyLimit = throwable.dailyLimit
            )
        }

        val httpEx = findHttpException(throwable)
        if (httpEx != null) {
            return parseHttpException(httpEx)
        }

        val message = when (throwable) {
            is ConnectException -> {
                "Cannot connect to server. Please ensure the backend is running and reachable."
            }
            is UnknownHostException -> {
                "Unable to reach server. Please check your internet connection."
            }
            is SocketTimeoutException -> {
                "Connection timed out. Please try again."
            }
            is IOException -> {
                "Network connection error. Please check your connection and try again."
            }
            else -> {
                throwable.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "An unexpected error occurred. Please try again."
            }
        }
        return ParsedApiError(userMessage = message)
    }

    private fun parseHttpException(exception: HttpException): ParsedApiError {
        parsedHttpCache[exception]?.let { return it }

        val code = exception.code()
        val errorBody = try {
            exception.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        val jsonObj = parseJsonObject(errorBody)
        val serverMessage = extractString(jsonObj, "message")
            ?: extractString(jsonObj, "detail")
            ?: extractString(jsonObj, "error")

        val quotaObj = jsonObj?.get("quota")?.takeIf { it.isJsonObject }?.asJsonObject
        val errorCode = extractString(jsonObj, "code") ?: extractString(jsonObj, "errorCode")
        val resetsAt = extractString(jsonObj, "resetsAt")
            ?: extractString(jsonObj, "resets_at")
            ?: extractString(quotaObj, "resetsAt")
            ?: extractString(quotaObj, "resets_at")
        val timezone = extractString(jsonObj, "timezone") ?: extractString(quotaObj, "timezone")
        val membershipTier = extractString(jsonObj, "membershipTier")
            ?: extractString(jsonObj, "membership_tier")
            ?: extractString(quotaObj, "membershipTier")
            ?: extractString(quotaObj, "membership_tier")
        val remainingToday = extractInt(jsonObj, "remainingToday")
            ?: extractInt(jsonObj, "remaining_today")
            ?: extractInt(quotaObj, "remainingToday")
            ?: extractInt(quotaObj, "remaining_today")
        val dailyLimit = extractInt(jsonObj, "dailyLimit")
            ?: extractInt(jsonObj, "daily_limit")
            ?: extractInt(quotaObj, "dailyLimit")
            ?: extractInt(quotaObj, "daily_limit")

        val userMessage = when (code) {
            400 -> serverMessage ?: "Invalid request. Please check your input and try again."
            401 -> "Session expired. Please log in again."
            403 -> serverMessage ?: "Access denied. You do not have permission for this action."
            404 -> serverMessage ?: "Requested resource was not found."
            408 -> "Request timed out on server. Please try again."
            409 -> serverMessage ?: "Action conflicts with your current workout state. Please refresh and try again."
            429 -> serverMessage ?: "Daily analysis limit reached or too many requests. Please try again later."
            in 500..599 -> serverMessage ?: "Server error (${code}). Please try again later."
            else -> serverMessage ?: "Request failed with status code ${code}."
        }

        val parsed = ParsedApiError(
            statusCode = code,
            userMessage = userMessage,
            errorCode = errorCode,
            resetsAt = resetsAt,
            timezone = timezone,
            membershipTier = membershipTier,
            remainingToday = remainingToday,
            dailyLimit = dailyLimit
        )
        parsedHttpCache[exception] = parsed
        return parsed
    }

    private fun parseJsonObject(body: String?): JsonObject? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = JsonParser.parseString(body)
            if (json.isJsonObject) json.asJsonObject else null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractString(obj: JsonObject?, key: String): String? {
        if (obj == null || !obj.has(key)) return null
        val element = obj.get(key)
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            element.asString.takeIf { it.isNotBlank() }
        } else null
    }

    private fun extractInt(obj: JsonObject?, key: String): Int? {
        if (obj == null || !obj.has(key)) return null
        val element = obj.get(key)
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) {
            runCatching { element.asInt }.getOrNull()
        } else null
    }
}
