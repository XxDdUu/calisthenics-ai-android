package com.calisthenics.ai.data.remote.mapper

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

object AuthErrorMapper {
    const val EMAIL_NOT_FOUND_MESSAGE = "Email này chưa được đăng ký."
    const val INVALID_PASSWORD_MESSAGE = "Mật khẩu không chính xác."
    const val NETWORK_ERROR_MESSAGE = "Không thể kết nối đến máy chủ. Vui lòng thử lại."
    const val SERVER_ERROR_MESSAGE = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."
    const val GENERIC_ERROR_MESSAGE = "Đăng nhập thất bại. Vui lòng thử lại."
    const val RESET_GENERIC_ERROR_MESSAGE = "Không thể hoàn tất yêu cầu. Vui lòng thử lại."

    fun message(error: Throwable): String = when (error) {
        is HttpException -> httpErrorMessage(error)
        is IOException -> NETWORK_ERROR_MESSAGE
        else -> GENERIC_ERROR_MESSAGE
    }

    fun passwordResetMessage(error: Throwable): String = when (error) {
        is HttpException -> resetHttpErrorMessage(error)
        is IOException -> NETWORK_ERROR_MESSAGE
        else -> RESET_GENERIC_ERROR_MESSAGE
    }

    private fun resetHttpErrorMessage(error: HttpException): String {
        if (error.code() >= 500) return SERVER_ERROR_MESSAGE
        val parsed = parseErrorBody(error)
        val code = parsed.code ?: return if (error.code() == 429) {
            "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau."
        } else {
            RESET_GENERIC_ERROR_MESSAGE
        }
        return when (code) {
            "INVALID_OTP" -> "Mã OTP không chính xác."
            "OTP_EXPIRED" -> "Mã OTP đã hết hạn. Hãy gửi mã mới."
            "OTP_ATTEMPTS_EXCEEDED" -> "Bạn đã nhập sai OTP quá nhiều lần. Hãy gửi mã mới."
            "RESET_TOKEN_INVALID" -> "Phiên đặt lại mật khẩu không hợp lệ. Hãy xác minh lại OTP."
            "RESET_TOKEN_EXPIRED" -> "Phiên đặt lại mật khẩu đã hết hạn. Hãy xác minh lại OTP."
            "PASSWORD_MISMATCH" -> "Mật khẩu xác nhận không khớp."
            "WEAK_PASSWORD" -> "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt."
            "RATE_LIMIT_EXCEEDED" -> "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau."
            else -> RESET_GENERIC_ERROR_MESSAGE
        }
    }

    private fun httpErrorMessage(error: HttpException): String {
        if (error.code() >= 500) return SERVER_ERROR_MESSAGE
        val parsed = parseErrorBody(error)
        if (error.code() == 409) {
            return parsed.message ?: "Email này đã được đăng ký bằng mật khẩu. Vui lòng đăng nhập bằng mật khẩu."
        }
        if (error.code() != 401) {
            return parsed.message ?: GENERIC_ERROR_MESSAGE
        }

        return when (parsed.code) {
            "EMAIL_NOT_FOUND" -> EMAIL_NOT_FOUND_MESSAGE
            "INVALID_PASSWORD" -> INVALID_PASSWORD_MESSAGE
            else -> parsed.message ?: GENERIC_ERROR_MESSAGE
        }
    }

    private data class ParsedError(val code: String?, val message: String?)

    private fun parseErrorBody(error: HttpException): ParsedError {
        val body = try {
            error.response()?.errorBody()?.use { it.string() }
        } catch (_: IOException) {
            null
        } ?: return ParsedError(null, null)

        return try {
            val json = JsonParser.parseString(body)
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                val code = obj.get("code")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                val message = obj.get("message")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                ParsedError(code, message)
            } else {
                ParsedError(null, null)
            }
        } catch (_: Throwable) {
            ParsedError(null, null)
        }
    }
}
