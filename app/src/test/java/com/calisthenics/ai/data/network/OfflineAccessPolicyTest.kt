package com.calisthenics.ai.data.network

import com.calisthenics.ai.ui.navigation.Screen
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineAccessPolicyTest {

    private lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var policy: OfflineAccessPolicy

    @Before
    fun setup() {
        connectivityMonitor = mockk()
        policy = OfflineAccessPolicy(connectivityMonitor)
    }

    @Test
    fun isOffline_reflectsConnectivityMonitor() {
        every { connectivityMonitor.isConnected() } returns true
        assertFalse(policy.isOffline())

        every { connectivityMonitor.isConnected() } returns false
        assertTrue(policy.isOffline())
    }

    @Test
    fun isRouteAccessibleOffline_allowsExpectedRoutes() {
        assertTrue(policy.isRouteAccessibleOffline(Screen.Home.route))
        assertTrue(policy.isRouteAccessibleOffline(Screen.Training.route))
        assertTrue(policy.isRouteAccessibleOffline(Screen.Splash.route))
        assertTrue(policy.isRouteAccessibleOffline(Screen.AuthenticatedEntry.route))
        assertTrue(policy.isRouteAccessibleOffline("workout_plans/123/MONDAY"))
    }

    @Test
    fun isRouteAccessibleOffline_blocksRestrictedRoutes() {
        assertFalse(policy.isRouteAccessibleOffline(Screen.Profile.route))
        assertFalse(policy.isRouteAccessibleOffline(Screen.Coach.route))
        assertFalse(policy.isRouteAccessibleOffline(Screen.Chat.route))
        assertFalse(policy.isRouteAccessibleOffline(Screen.Analysis.route))
        assertFalse(policy.isRouteAccessibleOffline(Screen.AdminDashboard.route))
        assertFalse(policy.isRouteAccessibleOffline(null))
    }

    @Test
    fun isFeatureRestrictedOffline_invertsAccessibility() {
        assertTrue(policy.isFeatureRestrictedOffline(Screen.Profile.route))
        assertTrue(policy.isFeatureRestrictedOffline(Screen.Coach.route))
        assertTrue(policy.isFeatureRestrictedOffline(Screen.Chat.route))
        assertFalse(policy.isFeatureRestrictedOffline(Screen.Home.route))
        assertFalse(policy.isFeatureRestrictedOffline(Screen.Training.route))
    }
}

