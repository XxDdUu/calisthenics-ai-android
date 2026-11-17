package com.calisthenics.ai.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.R
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.SubtleBorder
import com.calisthenics.ai.ui.theme.Surface
import kotlinx.coroutines.launch

enum class AdminNavGroup(val title: String) {
    WORKSPACE("Workspace"),
    OPERATIONS("Platform & Content"),
    MODERATION("Trust & Moderation")
}

enum class AdminNavSection(
    val label: String,
    val group: AdminNavGroup,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    OVERVIEW(
        label = "Overview",
        group = AdminNavGroup.WORKSPACE,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    AI_AGENT(
        label = "AI Agent",
        group = AdminNavGroup.WORKSPACE,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    ),
    USERS(
        label = "Users",
        group = AdminNavGroup.OPERATIONS,
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    ),
    EXERCISES(
        label = "Exercises",
        group = AdminNavGroup.OPERATIONS,
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    ),
    GETTING_STARTED(
        label = "Getting Started",
        group = AdminNavGroup.OPERATIONS,
        selectedIcon = Icons.Filled.RocketLaunch,
        unselectedIcon = Icons.Outlined.RocketLaunch
    ),
    WORKOUT_PLANS(
        label = "Workout Plans",
        group = AdminNavGroup.OPERATIONS,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ),
    WORKOUT_SESSIONS(
        label = "Sessions",
        group = AdminNavGroup.OPERATIONS,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    ),
    REVIEWS(
        label = "Reviews",
        group = AdminNavGroup.MODERATION,
        selectedIcon = Icons.Filled.RateReview,
        unselectedIcon = Icons.Outlined.RateReview
    ),
    USER_REPORTS(
        label = "Reports",
        group = AdminNavGroup.MODERATION,
        selectedIcon = Icons.Filled.Flag,
        unselectedIcon = Icons.Outlined.Flag
    )
}

data class AdminNavigationCallbacks(
    val onOverview: () -> Unit = {},
    val onAiAgent: () -> Unit = {},
    val onUsers: () -> Unit = {},
    val onExercises: () -> Unit = {},
    val onGettingStarted: () -> Unit = {},
    val onWorkoutPlans: () -> Unit = {},
    val onWorkoutSessions: () -> Unit = {},
    val onReviews: () -> Unit = {},
    val onUserReports: () -> Unit = {},
    val onLogout: (() -> Unit)? = null
) {
    fun navigate(section: AdminNavSection) {
        when (section) {
            AdminNavSection.OVERVIEW -> onOverview()
            AdminNavSection.AI_AGENT -> onAiAgent()
            AdminNavSection.USERS -> onUsers()
            AdminNavSection.EXERCISES -> onExercises()
            AdminNavSection.GETTING_STARTED -> onGettingStarted()
            AdminNavSection.WORKOUT_PLANS -> onWorkoutPlans()
            AdminNavSection.WORKOUT_SESSIONS -> onWorkoutSessions()
            AdminNavSection.REVIEWS -> onReviews()
            AdminNavSection.USER_REPORTS -> onUserReports()
        }
    }
}

/**
 * Responsive Admin shell:
 * - Tablet/desktop (>= 600dp): persistent docked left sidebar + top header + workspace pane.
 * - Mobile (< 600dp): collapsible modal navigation drawer triggered from the compact top bar.
 */
@Composable
fun AdminConsoleScaffold(
    currentSection: AdminNavSection,
    title: String,
    subtitle: String? = null,
    navigation: AdminNavigationCallbacks,
    hasFloatingBottomBar: Boolean = false,
    onBack: (() -> Unit)? = null,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (isExpandedLayout: Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        val isExpandedLayout = maxWidth >= 600.dp

        if (isExpandedLayout) {
            Row(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .width(236.dp)
                        .fillMaxHeight(),
                    color = Surface
                ) {
                    AdminSidebarContent(
                        currentSection = currentSection,
                        hasFloatingBottomBar = hasFloatingBottomBar,
                        onSelectSection = { section ->
                            if (section != currentSection) {
                                navigation.navigate(section)
                            }
                        },
                        onLogout = navigation.onLogout,
                        modifier = Modifier.statusBarsPadding()
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(SubtleBorder)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .statusBarsPadding()
                ) {
                    AdminTopBar(
                        title = title,
                        subtitle = subtitle,
                        showMenuButton = false,
                        onOpenMenu = {},
                        onBack = onBack,
                        actions = headerActions
                    )
                    HorizontalDivider(color = SubtleBorder)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content(true)
                    }
                }
            }
        } else {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(276.dp),
                        drawerContainerColor = Surface,
                        drawerContentColor = PrimaryText
                    ) {
                        AdminSidebarContent(
                            currentSection = currentSection,
                            hasFloatingBottomBar = false,
                            onSelectSection = { section ->
                                scope.launch { drawerState.close() }
                                if (section != currentSection) {
                                    navigation.navigate(section)
                                }
                            },
                            onLogout = navigation.onLogout?.let { logout ->
                                {
                                    scope.launch { drawerState.close() }
                                    logout()
                                }
                            },
                            modifier = Modifier
                        )
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Ink)
                        .statusBarsPadding()
                ) {
                    AdminTopBar(
                        title = title,
                        subtitle = subtitle,
                        showMenuButton = true,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onBack = onBack,
                        actions = headerActions
                    )
                    HorizontalDivider(color = SubtleBorder)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content(false)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTopBar(
    title: String,
    subtitle: String?,
    showMenuButton: Boolean,
    onOpenMenu: () -> Unit,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showMenuButton) {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open navigation menu",
                    tint = PrimaryText
                )
            }
        }
        if (onBack != null && !showMenuButton) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = PrimaryText
                )
            }
        } else if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = SecondaryText
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = actions
        )
    }
}

@Composable
private fun AdminSidebarContent(
    currentSection: AdminNavSection,
    hasFloatingBottomBar: Boolean,
    onSelectSection: (AdminNavSection) -> Unit,
    onLogout: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Accent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Admin Console",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Calisthenics AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = SubtleBorder)
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AdminNavGroup.entries.forEachIndexed { idx, group ->
                if (idx > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = group.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
                AdminNavSection.entries
                    .filter { it.group == group }
                    .forEach { section ->
                        AdminSidebarItem(
                            section = section,
                            selected = section == currentSection,
                            onClick = { onSelectSection(section) }
                        )
                    }
            }
        }

        if (onLogout != null) {
            HorizontalDivider(color = SubtleBorder)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.log_out),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Error
                )
            }
        }

        if (hasFloatingBottomBar) {
            Spacer(modifier = Modifier.height(FloatingBottomBarDefaults.ContentBottomPadding))
        }
    }
}

@Composable
private fun AdminSidebarItem(
    section: AdminNavSection,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) Accent.copy(alpha = 0.13f) else Color.Transparent
    val borderColor = if (selected) Accent.copy(alpha = 0.28f) else Color.Transparent
    val iconColor = if (selected) Accent else SecondaryText
    val textColor = if (selected) PrimaryText else SecondaryText

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (selected) Accent else Color.Transparent)
            )
            Icon(
                imageVector = if (selected) section.selectedIcon else section.unselectedIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = section.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
