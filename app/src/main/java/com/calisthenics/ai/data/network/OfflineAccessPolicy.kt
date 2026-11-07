package com.calisthenics.ai.data.network

import com.calisthenics.ai.ui.navigation.Screen
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class OfflineAccessPolicy @Inject constructor(
    private val connectivityMonitor: ConnectivityMonitor
) {
    open fun isOffline(): Boolean = !connectivityMonitor.isConnected()

    open fun isRouteAccessibleOffline(route: String?): Boolean {
        if (route == null) return false
        return route == Screen.Home.route ||
                route == Screen.Training.route ||
                route.startsWith("workout_plans/") ||
                route == Screen.Splash.route ||
                route == Screen.AuthenticatedEntry.route
    }

    open fun isFeatureRestrictedOffline(route: String?): Boolean {
        return !isRouteAccessibleOffline(route)
    }
}

