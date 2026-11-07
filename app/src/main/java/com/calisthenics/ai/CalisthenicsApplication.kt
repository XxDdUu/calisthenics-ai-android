package com.calisthenics.ai

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

private const val TAG = "GoogleSignIn"

@HiltAndroidApp
class CalisthenicsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Log.d("Environment", "Build type: ${BuildConfig.BUILD_TYPE}")
            Log.d("Environment", "API URL: ${BuildConfig.API_BASE_URL}")
            Log.d("Environment", "Application ID: ${BuildConfig.APPLICATION_ID}")

            Log.d(TAG, "========== GOOGLE OAUTH CONFIG ==========")
            Log.d(TAG, "Client ID loaded: ${BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()}")
            Log.d(TAG, "Client ID: ${BuildConfig.GOOGLE_WEB_CLIENT_ID}")
            Log.d(TAG, "Client ID length: ${BuildConfig.GOOGLE_WEB_CLIENT_ID.length}")
            Log.d(TAG, "=========================================")
        }
    }
}
