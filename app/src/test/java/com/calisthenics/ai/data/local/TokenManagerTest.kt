package com.calisthenics.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class TokenManagerTest {

    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var tokenManager: TokenManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns preferences
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor

        // Create a testable TokenManager subclass or mock encryption to avoid Keystore in unit test
        tokenManager = spyk(TokenManager(context), recordPrivateCalls = true)
    }

    private fun createJwtWithExpiry(expirySeconds: Long): String {
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payloadJson = "{\"sub\":\"123\",\"exp\":$expirySeconds,\"type\":\"ACCESS\"}"
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray(StandardCharsets.UTF_8))
        return "$header.$payload.dummySignature"
    }

    @Test
    fun `isTokenExpired returns true for expired token`() {
        val pastExpiry = (System.currentTimeMillis() / 1000) - 60
        val expiredToken = createJwtWithExpiry(pastExpiry)

        assertTrue(tokenManager.isTokenExpired(expiredToken))
    }

    @Test
    fun `isTokenExpired returns false for non-expired token`() {
        val futureExpiry = (System.currentTimeMillis() / 1000) + 3600
        val validToken = createJwtWithExpiry(futureExpiry)

        assertFalse(tokenManager.isTokenExpired(validToken))
    }

    @Test
    fun `isTokenExpired returns true for null or blank token`() {
        assertTrue(tokenManager.isTokenExpired(null))
        assertTrue(tokenManager.isTokenExpired(""))
        assertTrue(tokenManager.isTokenExpired("   "))
        assertTrue(tokenManager.isTokenExpired("invalid.token"))
    }

    @Test
    fun `saveAuthToken preserves existing userId when null is passed`() {
        val storedUserId = slot<Long>()
        every { editor.putLong("user_id", capture(storedUserId)) } returns editor
        every { preferences.getLong("user_id", -1L) } returns 99L

        // Mock encrypt to simply return the string in test
        every { tokenManager["encrypt"](any<String>()) } answers { "enc." + firstArg<String>() }

        tokenManager.saveAuthToken(accessToken = "new-access", refreshToken = "new-refresh", userId = null)

        assertEquals(99L, storedUserId.captured)
    }
}

