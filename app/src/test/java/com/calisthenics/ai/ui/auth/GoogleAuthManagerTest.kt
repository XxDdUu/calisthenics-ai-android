package com.calisthenics.ai.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthManagerTest {

    @Test
    fun googleSignInResultSuccessHoldsIdToken() {
        val result = GoogleSignInResult.Success("test-id-token-xyz")
        assertEquals("test-id-token-xyz", result.idToken)
    }

    @Test
    fun googleSignInResultCancelledRepresentsUserCancellation() {
        val result: GoogleSignInResult = GoogleSignInResult.Cancelled
        assertEquals(GoogleSignInResult.Cancelled, result)
    }

    @Test
    fun googleSignInResultErrorHoldsActionableMessage() {
        val result = GoogleSignInResult.Error("Actionable error message")
        assertEquals("Actionable error message", result.message)
    }

    @Test
    fun findActivityResolvesDirectActivity() {
        val activity = mockk<Activity>()
        assertEquals(activity, activity.findActivity())
    }

    @Test
    fun findActivityUnwrapsContextWrapper() {
        val activity = mockk<Activity>()
        val wrapper = mockk<ContextWrapper>()
        io.mockk.every { wrapper.baseContext } returns activity

        assertEquals(activity, wrapper.findActivity())
    }

    @Test
    fun findActivityReturnsNullForNonActivityContext() {
        val nonActivity = mockk<Context>()
        val wrapper = mockk<ContextWrapper>()
        io.mockk.every { wrapper.baseContext } returns nonActivity

        assertNull(wrapper.findActivity())
    }

    @Test
    fun signInWithNonActivityContextReturnsError() = runTest {
        val appContext = mockk<Context>(relaxed = true)
        val manager = GoogleAuthManager(appContext)

        val nonActivityContext = mockk<Context>(relaxed = true)
        val result = manager.signIn(nonActivityContext)

        assertTrue(result is GoogleSignInResult.Error)
        assertEquals("Activity context is required for Google Sign-In.", (result as GoogleSignInResult.Error).message)
    }
}
