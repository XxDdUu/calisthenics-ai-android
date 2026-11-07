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

    // Section 6 standard OTP messages
    const val OTP_INVALID_MESSAGE = "Invalid verification code. Please try again."
    const val OTP_EXPIRED_MESSAGE = "This code has expired. Please request a new one."
    const val OTP_ALREADY_USED_MESSAGE = "This code has already been used."
    const val OTP_SENT_SUCCESS_MESSAGE = "A new verification code has been sent."
    const val OTP_NETWORK_ERROR_MESSAGE = "Unable to connect. Please try again."
    const val OTP_TOO_MANY_ATTEMPTS_MESSAGE = "Too many attempts. Please try again later."
    const val OTP_VERIFY_SUCCESS_MESSAGE = "Email verified successfully."

    fun registrationMessage(error: Throwable): String = when (error) {
        is HttpException -> {
            if (error.code() >= 500) SERVER_ERROR_MESSAGE
            else {
                val parsed = parseErrorBody(error)
                parsed.message ?: when (error.code()) {
                    409 -> "Email này đã được sử dụng. Vui lòng chọn email khác hoặc đăng nhập."
                    429 -> "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau."
                    else -> "Đăng ký thất bại. Vui lòng kiểm tra lại thông tin."
                }
            }
        }
        is IOException -> NETWORK_ERROR_MESSAGE
        else -> error.localizedMessage ?: "Đăng ký thất bại. Vui lòng thử lại."
    }

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

    fun otpErrorMessage(error: Throwable): String = when (error) {
        is IOException -> OTP_NETWORK_ERROR_MESSAGE
        is HttpException -> {
            if (error.code() >= 500) {
                OTP_NETWORK_ERROR_MESSAGE
            } else if (error.code() == 429) {
                OTP_TOO_MANY_ATTEMPTS_MESSAGE
            } else {
                val parsed = parseErrorBody(error)
                when (parsed.code) {
                    "INVALID_OTP" -> OTP_INVALID_MESSAGE
                    "OTP_EXPIRED" -> OTP_EXPIRED_MESSAGE
                    "OTP_ALREADY_USED" -> OTP_ALREADY_USED_MESSAGE
                    "OTP_ATTEMPTS_EXCEEDED", "RATE_LIMIT_EXCEEDED" -> OTP_TOO_MANY_ATTEMPTS_MESSAGE
                    else -> {
                        val msg = (parsed.message ?: "").lowercase()
                        when {
                            msg.contains("hết hạn") || msg.contains("expired") -> OTP_EXPIRED_MESSAGE
                            msg.contains("quá nhiều") || msg.contains("too many") || msg.contains("attempts") -> OTP_TOO_MANY_ATTEMPTS_MESSAGE
                            msg.contains("sử dụng") || msg.contains("already used") -> OTP_ALREADY_USED_MESSAGE
                            msg.contains("không hợp lệ") || msg.contains("invalid") -> OTP_INVALID_MESSAGE
                            else -> OTP_INVALID_MESSAGE
                        }
                    }
                }
            }
        }
        else -> OTP_INVALID_MESSAGE
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
