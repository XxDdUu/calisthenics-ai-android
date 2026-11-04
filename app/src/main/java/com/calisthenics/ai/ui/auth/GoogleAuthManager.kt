package com.calisthenics.ai.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.calisthenics.ai.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GoogleAuthEntryPoint {
    fun googleAuthManager(): GoogleAuthManager
}

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Singleton
open class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GoogleAuthManager"
    }

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    open suspend fun signIn(activityContext: Context): GoogleSignInResult {
        val activity = activityContext.findActivity()
            ?: return GoogleSignInResult.Error("Activity context is required for Google Sign-In.")

        val gmsStatus = checkGooglePlayServices(activity)
        if (gmsStatus != null) {
            return GoogleSignInResult.Error(gmsStatus)
        }

        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (serverClientId.isBlank() || serverClientId == "default-google-client-id") {
            Log.e(TAG, "Google Web Client ID is not configured.")
            return GoogleSignInResult.Error("Google Web Client ID is not configured. Please check application settings.")
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                if (idToken.isNotBlank()) {
                    GoogleSignInResult.Success(idToken)
                } else {
                    GoogleSignInResult.Error("Invalid Google credential: empty ID token received.")
                }
            } else {
                Log.w(TAG, "Unsupported credential type: ${credential.type}")
                GoogleSignInResult.Error("Unsupported credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In was cancelled by user.")
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialInterruptedException) {
            Log.d(TAG, "Google Sign-In was interrupted.")
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No credentials available: ${e.message}")
            GoogleSignInResult.Error(
                "No Google account found or Google Sign-In is unavailable. Please check your Google account and try again."
            )
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Credential provider configuration error: ${e.message}")
            GoogleSignInResult.Error("Google Play Services or Credential Provider is misconfigured on this device.")
        } catch (e: GetCredentialCustomException) {
            Log.w(TAG, "Custom credential exception [${e.type}]: ${e.message}")
            val msg = e.message.orEmpty()
            if (msg.contains("cancel", ignoreCase = true) || e.type.contains("cancel", ignoreCase = true)) {
                GoogleSignInResult.Cancelled
            } else if (msg.contains("16") || msg.contains("28433")) {
                GoogleSignInResult.Error(
                    "Google Sign-In verification failed. Please verify that your Google account is configured on this device."
                )
            } else {
                GoogleSignInResult.Error(e.localizedMessage ?: "Google sign-in could not be completed. Please try again.")
            }
        } catch (e: GetCredentialException) {
            Log.w(TAG, "GetCredentialException: ${e.message}")
            val msg = e.message.orEmpty()
            if (msg.contains("cancel", ignoreCase = true)) {
                GoogleSignInResult.Cancelled
            } else {
                GoogleSignInResult.Error(e.localizedMessage ?: "Google sign-in was not completed. Please try again.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Unexpected error during Google sign-in: ${e.javaClass.simpleName}: ${e.message}")
            GoogleSignInResult.Error(e.localizedMessage ?: "An unexpected error occurred during Google sign-in.")
        }
    }

    protected open fun checkGooglePlayServices(activity: Activity): String? {
        return try {
            val availabilityClass = Class.forName("com.google.android.gms.common.GoogleApiAvailability")
            val getInstanceMethod = availabilityClass.getMethod("getInstance")
            val instance = getInstanceMethod.invoke(null)
            val isAvailableMethod = availabilityClass.getMethod("isGooglePlayServicesAvailable", Context::class.java)
            val resultCode = isAvailableMethod.invoke(instance, activity) as? Int ?: 0
            if (resultCode != 0) { // 0 == ConnectionResult.SUCCESS
                when (resultCode) {
                    1 -> "Google Play Services is missing on this device." // SERVICE_MISSING
                    2 -> "Google Play Services must be updated to sign in with Google." // SERVICE_VERSION_UPDATE_REQUIRED
                    3 -> "Google Play Services is disabled on this device." // SERVICE_DISABLED
                    else -> "Google Play Services is unavailable on this device."
                }
            } else {
                null
            }
        } catch (_: Throwable) {
            // In unit tests or environments without Play Services classes, proceed gracefully
            null
        }
    }

    open suspend fun clearCredentialState() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to clear credential state: ${e.message}")
        }
    }
}
