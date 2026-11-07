package com.calisthenics.ai.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticatedStartRouteTest {

    @Test
    fun administratorStartsAtAdminDashboard() {
        assertEquals(Screen.AdminDashboard.route, authenticatedStartRoute("ROLE_ADMIN"))
    }

    @Test
    fun ordinaryOrUnknownRoleFallsBackToExistingHome() {
        assertEquals(Screen.Home.route, authenticatedStartRoute("ROLE_USER"))
        assertEquals(Screen.Home.route, authenticatedStartRoute("ROLE_COACH"))
        assertEquals(Screen.Home.route, authenticatedStartRoute(null))
    }

    @Test
    fun onlyAdministratorsReceiveAnAdditionalBottomNavigationDestination() {
        val existingRoutes = listOf(
            Screen.Home.route,
            Screen.Training.route,
            Screen.Analysis.route,
            Screen.Coach.route,
            Screen.Chat.route,
            Screen.Profile.route
        )

        assertEquals(existingRoutes, visibleBottomNavigationScreens("ROLE_USER").map(Screen::route))
        assertEquals(existingRoutes, visibleBottomNavigationScreens("ROLE_COACH").map(Screen::route))
        assertEquals(
            existingRoutes + Screen.AdminDashboard.route,
            visibleBottomNavigationScreens("ROLE_ADMIN").map(Screen::route)
        )
    }
}
