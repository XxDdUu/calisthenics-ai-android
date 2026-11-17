package com.calisthenics.ai.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.sync.Mutex
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GoogleAuthEntryPoint {
    fun googleAuthManager(): GoogleAuthManager
}

sealed class GoogleSignInResult {
    data class Success(val idToken: String, val requestId: String = "") : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
    data class Error(
        val message: String,
        val canAddAccount: Boolean = false,
        val requestId: String = ""
    ) : GoogleSignInResult()
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

internal fun mapGoogleCancellationException(
    errorMessage: String,
    requestId: String
): GoogleSignInResult {
    // Google Play Services returns status code [16] (CommonStatusCodes.CANCELED) with
    // messages like "[16] Cancelled by user." on normal user dismissal. Check explicit
    // cancellation keywords before interpreting "[16]" as an authorization/config failure.
    if (errorMessage.isBlank() ||
        errorMessage.contains("cancelled", ignoreCase = true) ||
        errorMessage.contains("canceled", ignoreCase = true)
    ) {
        return GoogleSignInResult.Cancelled
    }
    return when {
        errorMessage.contains("28433", ignoreCase = true) ||
            errorMessage.contains("Account reauth failed", ignoreCase = true) ||
            errorMessage.contains("restricted to users within its organization", ignoreCase = true) ||
            errorMessage.contains("[16]", ignoreCase = true) -> {
            GoogleSignInResult.Error(
                "Google Sign-In authorization failed ($errorMessage). Please ensure the SHA-1 fingerprint is registered in Firebase / Google Cloud Console.",
                canAddAccount = false,
                requestId = requestId
            )
        }
        else -> GoogleSignInResult.Cancelled
    }
}

@Singleton
open class GoogleAuthManager internal constructor(
    private val context: Context,
    private val firebaseAuthenticator: FirebaseAuthenticator,
    injectedCredentialManager: CredentialManager?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context = context,
        firebaseAuthenticator = DefaultFirebaseAuthenticator(context),
        injectedCredentialManager = null
    )

    companion object {
        private const val TAG = "GoogleSignIn"
    }

    private val signInMutex = Mutex()

    private val credentialManager: CredentialManager by lazy {
        injectedCredentialManager ?: CredentialManager.create(context)
    }

    internal fun resolveWebClientId(): String {
        val resourceId = try {
            context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        } catch (_: Throwable) {
            0
        }
        if (resourceId != 0) {
            val fromGoogleServices = try {
                context.getString(resourceId).trim()
            } catch (_: Throwable) {
                ""
            }
            if (fromGoogleServices.isNotBlank() && fromGoogleServices.contains(".apps.googleusercontent.com")) {
                return fromGoogleServices
            }
        }
        return BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
    }

    private fun buildGoogleIdRequest(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean
    ): GetCredentialRequest {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
    }

    open suspend fun signIn(
        activityContext: Context,
        requestId: String = UUID.randomUUID().toString().take(8)
    ): GoogleSignInResult {
        if (!signInMutex.tryLock()) {
            Log.w(TAG, "[$requestId] DUPLICATE_SIGN_IN_IGNORED: Another Google sign-in flow is currently active")
            return GoogleSignInResult.Cancelled
        }

        try {
            Log.d(TAG, "[$requestId] SIGN_IN_STARTED")

            val activity = activityContext.findActivity()
            if (activity == null) {
                Log.e(TAG, "[$requestId] Activity context is required for Google Sign-In.")
                Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                return GoogleSignInResult.Error("Activity context is required for Google Sign-In.", requestId = requestId)
            }

            val gmsStatus = checkGooglePlayServices(activity)
            if (gmsStatus != null) {
                Log.e(TAG, "[$requestId] Google Play Services check failed: $gmsStatus")
                Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                return GoogleSignInResult.Error(gmsStatus, requestId = requestId)
            }

            val serverClientId = resolveWebClientId()
            if (serverClientId.isBlank() || serverClientId == "default-google-client-id") {
                Log.e(TAG, "[$requestId] Google/Firebase Web Client ID is not configured.")
                Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                return GoogleSignInResult.Error(
                    "Google Sign-In Web Client ID is not configured. Please check Firebase google-services configuration.",
                    requestId = requestId
                )
            }

            return try {
                Log.d(TAG, "[$requestId] REQUEST_CREDENTIAL_STAGE_1 (GetSignInWithGoogleOption)")
                val siwgOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
                val siwgRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(siwgOption)
                    .build()

                val result = try {
                    credentialManager.getCredential(
                        request = siwgRequest,
                        context = activity
                    )
                } catch (e: NoCredentialException) {
                    Log.d(TAG, "[$requestId] STAGE_1_NO_CREDENTIAL: ${e.message}, attempting STAGE_2 (filterByAuthorizedAccounts=false)")
                    try {
                        credentialManager.getCredential(
                            request = buildGoogleIdRequest(serverClientId, filterByAuthorizedAccounts = false),
                            context = activity
                        )
                    } catch (e2: NoCredentialException) {
                        Log.d(TAG, "[$requestId] STAGE_2_NO_CREDENTIAL: ${e2.message}, attempting STAGE_3 (filterByAuthorizedAccounts=true)")
                        credentialManager.getCredential(
                            request = buildGoogleIdRequest(serverClientId, filterByAuthorizedAccounts = true),
                            context = activity
                        )
                    }
                } catch (e: GetCredentialCustomException) {
                    Log.d(TAG, "[$requestId] STAGE_1_CUSTOM_EXCEPTION [${e.type}]: ${e.message}, falling back to STAGE_2 (filterByAuthorizedAccounts=false)")
                    credentialManager.getCredential(
                        request = buildGoogleIdRequest(serverClientId, filterByAuthorizedAccounts = false),
                        context = activity
                    )
                }

                val credential = result.credential
                Log.d(TAG, "[$requestId] CREDENTIAL_RECEIVED (type=${credential.type})")

                if (credential is CustomCredential &&
                    (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL)
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken
                    if (googleIdToken.isBlank()) {
                        Log.e(TAG, "[$requestId] Invalid Google credential: empty ID token received.")
                        Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                        return GoogleSignInResult.Error("Invalid Google credential: empty ID token received.", requestId = requestId)
                    }

                    Log.d(TAG, "[$requestId] GOOGLE_ID_TOKEN_PARSED (length=${googleIdToken.length})")
                    when (val firebaseResult = firebaseAuthenticator.authenticateWithGoogleIdToken(googleIdToken, requestId)) {
                        is FirebaseAuthExchangeResult.Success -> {
                            Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=FirebaseSuccess)")
                            GoogleSignInResult.Success(idToken = firebaseResult.idToken, requestId = requestId)
                        }
                        is FirebaseAuthExchangeResult.Error -> {
                            Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=FirebaseError)")
                            GoogleSignInResult.Error(firebaseResult.message, requestId = requestId)
                        }
                    }
                } else {
                    Log.w(TAG, "[$requestId] Unsupported credential type: ${credential.type}")
                    Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                    GoogleSignInResult.Error("Unsupported credential type received.", requestId = requestId)
                }
            } catch (e: GetCredentialCancellationException) {
                val errorMessage = e.message.orEmpty()
                Log.d(TAG, "[$requestId] Google Sign-In cancellation/aborted: $errorMessage")
                val mapped = mapGoogleCancellationException(errorMessage, requestId)
                if (mapped is GoogleSignInResult.Error) {
                    Log.e(TAG, "[$requestId] Google account authorization failed: $errorMessage", e)
                    Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                } else {
                    Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Cancelled)")
                }
                mapped
            } catch (e: GetCredentialInterruptedException) {
                Log.w(TAG, "[$requestId] Google Sign-In was interrupted: ${e.message}", e)
                Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                GoogleSignInResult.Error("Google Sign-In was interrupted. Please try again.", requestId = requestId)
            } catch (e: NoCredentialException) {
                Log.w(TAG, "[$requestId] No credentials available: ${e.message}", e)
                Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                GoogleSignInResult.Error(
                    "Google Sign-In could not retrieve an authorized account. Please select your Google account to continue.",
                    canAddAccount = false,
                    requestId = requestId
                )
            } catch (e: GetCredentialProviderConfigurationException) {
                Log.e(TAG, "[$requestId] Credential provider configuration error: ${e.message}", e)
                Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                GoogleSignInResult.Error(
                    "Google Play Services or Credential Provider is misconfigured on this device.",
                    requestId = requestId
                )
            } catch (e: GetCredentialCustomException) {
                Log.w(TAG, "[$requestId] Custom credential exception [${e.type}]: ${e.message}", e)
                Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                val msg = e.message.orEmpty()
                if ((msg.contains("[16]") || msg.contains("28433")) &&
                    !msg.contains("cancelled", ignoreCase = true) &&
                    !msg.contains("canceled", ignoreCase = true)
                ) {
                    GoogleSignInResult.Error(
                        "Google Sign-In verification failed (code 16/28433). Please ensure the SHA-1 certificate fingerprint is registered in Firebase / Google Cloud Console and matches the package name.",
                        canAddAccount = false,
                        requestId = requestId
                    )
                } else {
                    GoogleSignInResult.Error(e.localizedMessage ?: "Google sign-in could not be completed. Please try again.", requestId = requestId)
                }
            } catch (e: GetCredentialException) {
                Log.w(TAG, "[$requestId] GetCredentialException: ${e.message}", e)
                Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                GoogleSignInResult.Error(e.localizedMessage ?: "Google sign-in was not completed. Please try again.", requestId = requestId)
            } catch (e: Throwable) {
                Log.e(TAG, "[$requestId] Unexpected error during Google sign-in: ${e.javaClass.simpleName}: ${e.message}", e)
                Log.d(TAG, "[$requestId] SIGN_IN_FINISHED (status=Error)")
                GoogleSignInResult.Error(e.localizedMessage ?: "An unexpected error occurred during Google sign-in.", requestId = requestId)
            }
        } finally {
            signInMutex.unlock()
        }
    }

    open fun openAccountSettings(context: Context) {
        val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
            putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                context.startActivity(Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Throwable) {}
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
                try {
                    val isUserResolvableMethod = availabilityClass.getMethod("isUserResolvableError", Int::class.javaPrimitiveType)
                    val isResolvable = isUserResolvableMethod.invoke(instance, resultCode) as? Boolean ?: false
                    if (isResolvable) {
                        val showErrorDialogMethod = availabilityClass.getMethod(
                            "showErrorDialogFragment",
                            Activity::class.java,
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType
                        )
                        showErrorDialogMethod.invoke(instance, activity, resultCode, 9000)
                    }
                } catch (_: Throwable) {}

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
        Log.d(TAG, "SESSION_RESET_STARTED")
        firebaseAuthenticator.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Log.d(TAG, "SESSION_RESET_FINISHED (status=Success)")
        } catch (e: Throwable) {
            Log.w(TAG, "SESSION_RESET_FINISHED (status=Failed, error=${e.message})", e)
        }
    }
}
