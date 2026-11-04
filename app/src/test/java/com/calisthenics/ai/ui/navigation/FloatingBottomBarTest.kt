package com.calisthenics.ai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingBottomBarTest {

    @Test
    fun floatingBottomBarItemPreservesAllProperties() {
        val item = FloatingBottomBarItem(
            route = "home",
            icon = Icons.Default.Home,
            label = "Home",
            badgeCount = 0
        )

        assertEquals("home", item.route)
        assertEquals(Icons.Default.Home, item.icon)
        assertEquals("Home", item.label)
        assertEquals(0, item.badgeCount)
    }

    @Test
    fun floatingBottomBarItemSupportsOptionalLabelAndBadge() {
        val itemWithoutLabel = FloatingBottomBarItem(
            route = "profile",
            icon = Icons.Default.Person
        )

        assertEquals("profile", itemWithoutLabel.route)
        assertEquals(null, itemWithoutLabel.label)
        assertEquals(0, itemWithoutLabel.badgeCount)

        val itemWithBadge = FloatingBottomBarItem(
            route = "coach",
            icon = Icons.Default.Groups,
            label = "Coaching",
            badgeCount = 5
        )

        assertEquals(5, itemWithBadge.badgeCount)
    }

    @Test
    fun destinationsMapToExpectedNavigationItems() {
        val destinations = listOf(
            Screen.Home to (Icons.Default.Home to "Home"),
            Screen.Training to (Icons.Default.FitnessCenter to "Training"),
            Screen.Analysis to (Icons.Default.AutoGraph to "AI Analysis"),
            Screen.Coach to (Icons.Default.Groups to "Coaching"),
            Screen.Chat to (Icons.Default.ChatBubble to "Chat"),
            Screen.Profile to (Icons.Default.Person to "Profile")
        )

        val incomingRequestCount = 3
        val hasLoaded = true

        val items = destinations.map { (screen, item) ->
            val (icon, label) = item
            val badge = if (screen == Screen.Coach && hasLoaded) {
                incomingRequestCount
            } else {
                0
            }
            FloatingBottomBarItem(
                route = screen.route,
                icon = icon,
                label = label,
                badgeCount = badge
            )
        }

        assertEquals(6, items.size)
        assertEquals(Screen.Home.route, items[0].route)
        assertEquals("Home", items[0].label)
        assertEquals(0, items[0].badgeCount)

        assertEquals(Screen.Training.route, items[1].route)
        assertEquals("Training", items[1].label)

        assertEquals(Screen.Analysis.route, items[2].route)
        assertEquals("AI Analysis", items[2].label)

        assertEquals(Screen.Coach.route, items[3].route)
        assertEquals("Coaching", items[3].label)
        assertEquals(3, items[3].badgeCount)

        assertEquals(Screen.Chat.route, items[4].route)
        assertEquals("Chat", items[4].label)

        assertEquals(Screen.Profile.route, items[5].route)
        assertEquals("Profile", items[5].label)
        assertEquals(0, items[5].badgeCount)
    }

    @Test
    fun selectedStateMatchesCurrentRouteCorrectly() {
        val currentRoute = Screen.Training.route
        val items = listOf(
            FloatingBottomBarItem(Screen.Home.route, Icons.Default.Home, "Home"),
            FloatingBottomBarItem(Screen.Training.route, Icons.Default.FitnessCenter, "Training"),
            FloatingBottomBarItem(Screen.Profile.route, Icons.Default.Person, "Profile")
        )

        assertFalse(items[0].route == currentRoute)
        assertTrue(items[1].route == currentRoute)
        assertFalse(items[2].route == currentRoute)
    }
}

