package com.calisthenics.ai.data.remote.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorParserTest {

    @Test
    fun `maps ConnectException to clear server unreachable message`() {
        val error = ConnectException("Connection refused")
        val message = NetworkErrorParser.toUserMessage(error)
        assertTrue(message.contains("Cannot connect to server"))
    }

    @Test
    fun `maps UnknownHostException to network connection message`() {
        val error = UnknownHostException("Unable to resolve host")
        val message = NetworkErrorParser.toUserMessage(error)
        assertTrue(message.contains("Unable to reach server"))
    }

    @Test
    fun `maps SocketTimeoutException to timeout message`() {
        val error = SocketTimeoutException("timeout")
        val message = NetworkErrorParser.toUserMessage(error)
        assertTrue(message.contains("timed out"))
    }

    @Test
    fun `maps 401 HttpException to session expired message`() {
        val response = Response.error<Any>(401, "".toResponseBody("application/json".toMediaType()))
        val error = HttpException(response)
        val message = NetworkErrorParser.toUserMessage(error)
        assertTrue(message.contains("Session expired"))
    }

    @Test
    fun `maps 403 HttpException to access denied message`() {
        val response = Response.error<Any>(403, "".toResponseBody("application/json".toMediaType()))
        val error = HttpException(response)
        val message = NetworkErrorParser.toUserMessage(error)
        assertTrue(message.contains("Access denied"))
    }

    @Test
    fun `maps 404 HttpException to conversation not found message`() {
        val response = Response.error<Any>(404, "".toResponseBody("application/json".toMediaType()))
        val error = HttpException(response)
        val message = NetworkErrorParser.toUserMessage(error)
        assertTrue(message.contains("not found"))
    }

    @Test
    fun `parses custom server error json message if provided`() {
        val json = """{"message":"Custom server rejection reason"}"""
        val response = Response.error<Any>(400, json.toResponseBody("application/json".toMediaType()))
        val error = HttpException(response)
        val message = NetworkErrorParser.toUserMessage(error)
        assertEquals("Custom server rejection reason", message)
    }
}

