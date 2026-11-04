package com.calisthenics.ai.data.remote.api

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkErrorParser {

    fun toUserMessage(throwable: Throwable): String {
        return when (throwable) {
            is ConnectException -> {
                "Cannot connect to server. Please ensure the backend is running and reachable."
            }
            is UnknownHostException -> {
                "Unable to reach server. Please check your internet connection."
            }
            is SocketTimeoutException -> {
                "Connection timed out. Please try again."
            }
            is HttpException -> {
                parseHttpError(throwable)
            }
            is IOException -> {
                "Network connection error. Please check your connection and try again."
            }
            else -> {
                throwable.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "An unexpected error occurred. Please try again."
            }
        }
    }

    private fun parseHttpError(exception: HttpException): String {
        val code = exception.code()
        val errorBody = try {
            exception.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        val serverMessage = parseJsonMessage(errorBody)

        return when (code) {
            400 -> serverMessage ?: "Invalid message request."
            401 -> "Session expired. Please log in again."
            403 -> serverMessage ?: "Access denied. You are not a participant in this conversation."
            404 -> serverMessage ?: "Conversation or recipient not found."
            408 -> "Request timed out on server. Please try again."
            409 -> serverMessage ?: "Conflict occurred while sending message. Please retry."
            429 -> "Too many requests. Please slow down and try again shortly."
            in 500..599 -> "Server error (${code}). Please try again later."
            else -> serverMessage ?: "Request failed with status code ${code}."
        }
    }

    private fun parseJsonMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = JsonParser.parseString(body)
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                when {
                    obj.has("message") && obj.get("message").isJsonPrimitive -> obj.get("message").asString
                    obj.has("error") && obj.get("error").isJsonPrimitive -> obj.get("error").asString
                    else -> null
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
