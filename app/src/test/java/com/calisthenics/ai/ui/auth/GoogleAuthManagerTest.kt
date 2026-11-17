package com.calisthenics.ai.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Bundle
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.calisthenics.ai.BuildConfig
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
        org.junit.Assert.assertFalse(result.canAddAccount)
    }

    @Test
    fun googleSignInResultErrorCanAddAccountFalsePreventsMisleadingDialog() {
        val result = GoogleSignInResult.Error(
            message = "Google Sign-In could not retrieve an authorized account.",
            canAddAccount = false,
            requestId = "req-123"
        )
        org.junit.Assert.assertFalse(result.canAddAccount)
        assertEquals("req-123", result.requestId)
    }

    @Test
    fun googleSignInResultSuccessHoldsRequestId() {
        val result = GoogleSignInResult.Success("id-token-abc", requestId = "req-456")
        assertEquals("id-token-abc", result.idToken)
        assertEquals("req-456", result.requestId)
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

    @Test
    fun mapGoogleCancellationExceptionTreatsCode16UserCancelAsCancelled() {
        assertEquals(
            GoogleSignInResult.Cancelled,
            mapGoogleCancellationException("[16] Cancelled by user.", "req-cancel-1")
        )
        assertEquals(
            GoogleSignInResult.Cancelled,
            mapGoogleCancellationException("User canceled the operation", "req-cancel-2")
        )
        assertEquals(
            GoogleSignInResult.Cancelled,
            mapGoogleCancellationException("", "req-cancel-3")
        )
    }

    @Test
    fun mapGoogleCancellationExceptionMapsAuthOrConfigFailuresWithoutAddAccountPrompt() {
        val result = mapGoogleCancellationException(
            "[16] Account reauth failed.",
            "req-err-1"
        )
        assertTrue(result is GoogleSignInResult.Error)
        val err = result as GoogleSignInResult.Error
        org.junit.Assert.assertFalse(err.canAddAccount)
        assertEquals("req-err-1", err.requestId)
        assertTrue(err.message.contains("SHA-1"))
    }

    @Test
    fun resolveWebClientIdPrefersGoogleServicesResourceAndFallsBackToBuildConfig() {
        val resources = mockk<Resources>()
        val context = mockk<Context>(relaxed = true)
        every { context.packageName } returns "com.calisthenics.ai"
        every { context.resources } returns resources
        every { resources.getIdentifier("default_web_client_id", "string", "com.calisthenics.ai") } returns 42
        every { context.getString(42) } returns "401410597082-webclient.apps.googleusercontent.com"

        val manager = GoogleAuthManager(context, mockk(relaxed = true), mockk(relaxed = true))
        assertEquals("401410597082-webclient.apps.googleusercontent.com", manager.resolveWebClientId())

        every { resources.getIdentifier("default_web_client_id", "string", "com.calisthenics.ai") } returns 0
        assertEquals(BuildConfig.GOOGLE_WEB_CLIENT_ID.trim(), manager.resolveWebClientId())
    }

    @Test
    fun signInExchangesGoogleIdTokenForFirebaseIdToken() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val credentialManager = mockk<CredentialManager>()
        val firebaseAuthenticator = mockk<FirebaseAuthenticator>()

        val customCred = googleIdTokenCustomCredential(
            type = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
            email = "athlete@example.com",
            idToken = "google-oauth-id-token"
        )

        coEvery { credentialManager.getCredential(activity, any<GetCredentialRequest>()) } returns GetCredentialResponse(customCred)
        coEvery {
            firebaseAuthenticator.authenticateWithGoogleIdToken("google-oauth-id-token", "req-fb-1")
        } returns FirebaseAuthExchangeResult.Success(
            idToken = "firebase-id-token-xyz",
            uid = "firebase-uid-1",
            email = "athlete@example.com"
        )

        val manager = GoogleAuthManager(activity, firebaseAuthenticator, credentialManager)
        val result = manager.signIn(activity, "req-fb-1")

        assertEquals(GoogleSignInResult.Success("firebase-id-token-xyz", "req-fb-1"), result)
        coVerify(exactly = 1) {
            firebaseAuthenticator.authenticateWithGoogleIdToken("google-oauth-id-token", "req-fb-1")
        }
    }

    @Test
    fun signInReturnsErrorWhenFirebaseExchangeFails() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        val credentialManager = mockk<CredentialManager>()
        val firebaseAuthenticator = mockk<FirebaseAuthenticator>()

        val customCred = googleIdTokenCustomCredential(
            type = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            email = "athlete@example.com",
            idToken = "google-oauth-id-token"
        )

        coEvery { credentialManager.getCredential(activity, any<GetCredentialRequest>()) } returns GetCredentialResponse(customCred)
        coEvery {
            firebaseAuthenticator.authenticateWithGoogleIdToken("google-oauth-id-token", "req-fb-2")
        } returns FirebaseAuthExchangeResult.Error("This account has been disabled.")

        val manager = GoogleAuthManager(activity, firebaseAuthenticator, credentialManager)
        val result = manager.signIn(activity, "req-fb-2")

        assertEquals(
            GoogleSignInResult.Error("This account has been disabled.", canAddAccount = false, requestId = "req-fb-2"),
            result
        )
    }

    private fun googleIdTokenCustomCredential(
        type: String,
        email: String,
        idToken: String
    ): CustomCredential {
        val data = mockk<Bundle>(relaxed = true)
        every { data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID") } returns email
        every { data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN") } returns idToken
        @Suppress("DEPRECATION")
        every { data.getParcelable<android.os.Parcelable>(any()) } returns null
        return CustomCredential(type = type, data = data)
    }

    @Test
    fun clearCredentialStateSignsOutFirebaseAndClearsCredentialManager() = runTest {
        val context = mockk<Context>(relaxed = true)
        val credentialManager = mockk<CredentialManager>(relaxed = true)
        val firebaseAuthenticator = mockk<FirebaseAuthenticator>(relaxed = true)

        val manager = GoogleAuthManager(context, firebaseAuthenticator, credentialManager)
        manager.clearCredentialState()

        verify(exactly = 1) { firebaseAuthenticator.signOut() }
        coVerify(exactly = 1) { credentialManager.clearCredentialState(any<ClearCredentialStateRequest>()) }
    }
}
