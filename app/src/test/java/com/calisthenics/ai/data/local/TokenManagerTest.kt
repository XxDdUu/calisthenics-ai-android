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
        every { preferences.getLong(any(), any()) } answers { secondArg() }
        every { preferences.getString(any(), any()) } answers { secondArg() }
        every { preferences.getStringSet(any(), any()) } returns null
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

    @Test
    fun `switchToAccount returns false when account is not in saved accounts`() {
        every { preferences.getStringSet("saved_account_ids", any()) } returns setOf("10", "20")
        every { preferences.contains("account_99_access_token") } returns false

        val result = tokenManager.switchToAccount(99L)
        assertFalse(result)
    }

    @Test
    fun `switchToAccount updates active user id and legacy keys when account exists`() {
        val capturedActiveId = slot<Long>()
        every { preferences.getStringSet("saved_account_ids", any()) } returns setOf("10", "20")
        every { preferences.getString("account_20_access_token", null) } returns "enc.token20"
        every { preferences.getString("account_20_refresh_token", null) } returns "enc.ref20"
        every { preferences.getString("account_20_role", null) } returns "ROLE_ADMIN"
        every { editor.putLong("active_user_id", capture(capturedActiveId)) } returns editor
        every { editor.putString("access_token", "enc.token20") } returns editor
        every { editor.putString("refresh_token", "enc.ref20") } returns editor
        every { editor.putLong("user_id", 20L) } returns editor
        every { editor.putString("user_role", "ROLE_ADMIN") } returns editor

        val result = tokenManager.switchToAccount(20L)

        assertTrue(result)
        assertEquals(20L, capturedActiveId.captured)
    }

    @Test
    fun `removeAccount removes account from saved accounts set`() {
        val capturedSet = slot<Set<String>>()
        every { preferences.getStringSet("saved_account_ids", any()) } returns setOf("10", "20")
        every { editor.putStringSet("saved_account_ids", capture(capturedSet)) } returns editor
        every { editor.remove(any()) } returns editor
        every { preferences.getLong("active_user_id", -1L) } returns 10L

        val result = tokenManager.removeAccount(20L)

        assertTrue(result)
        assertEquals(setOf("10"), capturedSet.captured)
    }

    @Test
    fun `deactivateActiveSession clears active user id and legacy keys without deleting saved accounts`() {
        val removedKeys = mutableListOf<String>()
        every { editor.remove(capture(removedKeys)) } returns editor

        tokenManager.deactivateActiveSession()

        assertTrue(removedKeys.contains("active_user_id"))
        assertTrue(removedKeys.contains("access_token"))
        assertTrue(removedKeys.contains("refresh_token"))
        assertTrue(removedKeys.contains("user_id"))
        assertTrue(removedKeys.contains("user_role"))
    }
}

