package com.calisthenics.ai.ui.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class FirebaseAuthExchangeResult {
    data class Success(
        val idToken: String,
        val uid: String,
        val email: String?
    ) : FirebaseAuthExchangeResult()

    data class Error(val message: String) : FirebaseAuthExchangeResult()
}

interface FirebaseAuthenticator {
    suspend fun authenticateWithGoogleIdToken(
        googleIdToken: String,
        requestId: String
    ): FirebaseAuthExchangeResult

    fun signOut()
}

internal class DefaultFirebaseAuthenticator(
    private val appContext: Context
) : FirebaseAuthenticator {

    companion object {
        private const val TAG = "GoogleSignIn"
    }

    override suspend fun authenticateWithGoogleIdToken(
        googleIdToken: String,
        requestId: String
    ): FirebaseAuthExchangeResult {
        return try {
            val firebaseApp = ensureFirebaseAppInitialized(requestId)
                ?: return FirebaseAuthExchangeResult.Error(
                    "Firebase Authentication is not configured for this build."
                )

            val firebaseAuth = FirebaseAuth.getInstance(firebaseApp)
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)

            Log.d(TAG, "[$requestId] FIREBASE_SIGN_IN_STARTED")
            val authResult = firebaseAuth.signInWithCredential(credential).awaitTask()
            val user = authResult.user ?: firebaseAuth.currentUser
                ?: return FirebaseAuthExchangeResult.Error(
                    "Firebase Authentication did not return a user session."
                )

            val tokenResult = user.getIdToken(true).awaitTask()
            val firebaseIdToken = tokenResult.token
            if (firebaseIdToken.isNullOrBlank()) {
                Log.e(TAG, "[$requestId] FIREBASE_ID_TOKEN_EMPTY")
                return FirebaseAuthExchangeResult.Error(
                    "Unable to retrieve Firebase authentication token. Please try again."
                )
            }

            Log.d(TAG, "[$requestId] FIREBASE_ID_TOKEN_ACQUIRED (length=${firebaseIdToken.length})")
            FirebaseAuthExchangeResult.Success(
                idToken = firebaseIdToken,
                uid = user.uid,
                email = user.email
            )
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "[$requestId] FIREBASE_AUTH_EXCEPTION (code=${e.errorCode}): ${e.message}", e)
            FirebaseAuthExchangeResult.Error(mapFirebaseAuthException(e))
        } catch (e: FirebaseNetworkException) {
            Log.w(TAG, "[$requestId] FIREBASE_NETWORK_EXCEPTION: ${e.message}", e)
            FirebaseAuthExchangeResult.Error(
                "Network error while signing in with Firebase. Please check your internet connection."
            )
        } catch (e: IllegalStateException) {
            Log.e(TAG, "[$requestId] FIREBASE_NOT_INITIALIZED: ${e.message}", e)
            FirebaseAuthExchangeResult.Error(
                "Firebase Authentication is not initialized on this build."
            )
        } catch (e: Throwable) {
            Log.e(TAG, "[$requestId] FIREBASE_SIGN_IN_FAILED (${e.javaClass.simpleName}): ${e.message}", e)
            FirebaseAuthExchangeResult.Error(
                e.localizedMessage ?: "Firebase Authentication failed. Please try again."
            )
        }
    }

    override fun signOut() {
        try {
            val apps = FirebaseApp.getApps(appContext)
            if (apps.isNotEmpty()) {
                FirebaseAuth.getInstance(apps.first()).signOut()
                Log.d(TAG, "FIREBASE_SIGN_OUT_SUCCESS")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FIREBASE_SIGN_OUT_SKIPPED: ${e.message}")
        }
    }

    private fun ensureFirebaseAppInitialized(requestId: String): FirebaseApp? {
        val existing = FirebaseApp.getApps(appContext)
        if (existing.isNotEmpty()) {
            return existing.first()
        }
        val initialized = FirebaseApp.initializeApp(appContext)
        if (initialized == null) {
            Log.e(TAG, "[$requestId] FirebaseApp.initializeApp returned null (check google-services.json)")
        }
        return initialized
    }

    private fun mapFirebaseAuthException(e: FirebaseAuthException): String {
        return when (e.errorCode) {
            "ERROR_USER_DISABLED" ->
                "This account has been disabled."
            "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" ->
                "Your sign-in session expired. Please try again."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL",
            "ERROR_CREDENTIAL_ALREADY_IN_USE",
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "An account already exists using a different sign-in method for this email."
            "ERROR_INVALID_CREDENTIAL" ->
                "The Google sign-in credential could not be verified by Firebase."
            "ERROR_NETWORK_REQUEST_FAILED" ->
                "Network error while signing in with Firebase. Please check your internet connection."
            else ->
                "Firebase sign-in could not be completed (${e.errorCode}). Please try again."
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}
