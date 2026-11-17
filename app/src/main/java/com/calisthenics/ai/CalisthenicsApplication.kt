package com.calisthenics.ai

import android.app.Application
import android.util.Log
import com.calisthenics.ai.data.local.AccountSessionCleanupCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private const val TAG = "GoogleSignIn"

@HiltAndroidApp
class CalisthenicsApplication : Application() {

    @Inject
    lateinit var accountSessionCleanupCoordinator: AccountSessionCleanupCoordinator

    override fun onCreate() {
        super.onCreate()
        accountSessionCleanupCoordinator.start()
        if (BuildConfig.DEBUG) {
            Log.d("Environment", "Build type: ${BuildConfig.BUILD_TYPE}")
            Log.d("Environment", "API URL: ${BuildConfig.API_BASE_URL}")
            Log.d("Environment", "Application ID: ${BuildConfig.APPLICATION_ID}")

            val defaultWebClientIdRes = resources.getIdentifier("default_web_client_id", "string", packageName)
            val firebaseWebClientId = if (defaultWebClientIdRes != 0) getString(defaultWebClientIdRes) else ""
            Log.d(TAG, "========== FIREBASE / GOOGLE AUTH CONFIG ==========")
            Log.d(TAG, "Firebase default_web_client_id present: ${firebaseWebClientId.isNotBlank()}")
            Log.d(TAG, "BuildConfig Web Client ID loaded: ${BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()}")
            Log.d(TAG, "===================================================")
        }
    }
}
