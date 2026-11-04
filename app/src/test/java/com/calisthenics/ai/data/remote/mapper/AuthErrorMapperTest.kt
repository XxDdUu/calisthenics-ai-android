package com.calisthenics.ai.data.remote.mapper

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AuthErrorMapperTest {
    @Test
    fun mapsEmailNotFoundResponse() {
        assertEquals(
            AuthErrorMapper.EMAIL_NOT_FOUND_MESSAGE,
            AuthErrorMapper.message(httpError(401, """{"status":401,"code":"EMAIL_NOT_FOUND","message":"ignored"}"""))
        )
    }

    @Test
    fun mapsInvalidPasswordResponse() {
        assertEquals(
            AuthErrorMapper.INVALID_PASSWORD_MESSAGE,
            AuthErrorMapper.message(httpError(401, """{"status":401,"code":"INVALID_PASSWORD","message":"ignored"}"""))
        )
    }

    @Test
    fun mapsConflictResponse() {
        assertEquals(
            "An account with this email already exists with password authentication.",
            AuthErrorMapper.message(httpError(409, """{"status":409,"message":"An account with this email already exists with password authentication."}"""))
        )
    }

    @Test
    fun mapsNetworkAndServerFailuresToSafeMessages() {
        assertEquals(
            AuthErrorMapper.NETWORK_ERROR_MESSAGE,
            AuthErrorMapper.message(IOException("raw socket details"))
        )
        assertEquals(
            AuthErrorMapper.SERVER_ERROR_MESSAGE,
            AuthErrorMapper.message(httpError(500, """{"message":"database password leaked"}"""))
        )
    }

    @Test
    fun mapsPasswordResetErrorsWithoutExposingBackendDetails() {
        assertEquals(
            "Mã OTP đã hết hạn. Hãy gửi mã mới.",
            AuthErrorMapper.passwordResetMessage(
                httpError(400, """{"status":400,"code":"OTP_EXPIRED","message":"ignored"}""")
            )
        )
        assertEquals(
            "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.",
            AuthErrorMapper.passwordResetMessage(
                httpError(429, """{"status":429,"code":"RATE_LIMIT_EXCEEDED","message":"ignored"}""")
            )
        )
        assertEquals(
            AuthErrorMapper.RESET_GENERIC_ERROR_MESSAGE,
            AuthErrorMapper.passwordResetMessage(
                httpError(400, """{"status":400,"message":"sensitive SQL details"}""")
            )
        )
    }

    private fun httpError(status: Int, body: String): HttpException {
        val response = Response.error<Any>(
            status,
            body.toResponseBody("application/json".toMediaType())
        )
        return HttpException(response)
    }
}
