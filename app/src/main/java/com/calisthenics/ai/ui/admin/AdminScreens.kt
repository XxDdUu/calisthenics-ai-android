package com.calisthenics.ai.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AdminDashboard
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminUser
import com.calisthenics.ai.ui.components.localizedLabel
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.CardBackground
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Info
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.LocalAppColors
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.SubtleBorder
import com.calisthenics.ai.ui.theme.Success
import com.calisthenics.ai.ui.theme.Surface
import com.calisthenics.ai.ui.theme.Warning

@Composable
fun AdminDashboardScreen(
    onUsers: () -> Unit,
    onUserClick: (Long) -> Unit = {},
    onExercises: () -> Unit = {},
    onWorkoutPlans: () -> Unit,
    onWorkoutSessions: () -> Unit,
    onReviews: () -> Unit = {},
    onUserReports: () -> Unit = {},
    onAdminAiAgent: () -> Unit = {},
    onGettingStartedProgram: () -> Unit = {},
    onLogout: () -> Unit,
    adminNavigation: AdminNavigationCallbacks = AdminNavigationCallbacks(
        onOverview = {},
        onAiAgent = onAdminAiAgent,
        onUsers = onUsers,
        onExercises = onExercises,
        onGettingStarted = onGettingStartedProgram,
        onWorkoutPlans = onWorkoutPlans,
        onWorkoutSessions = onWorkoutSessions,
        onReviews = onReviews,
        onUserReports = onUserReports,
        onLogout = onLogout
    ),
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    val effectiveNavigation = remember(adminNavigation, onLogout) {
        if (adminNavigation.onLogout != null) {
            adminNavigation
        } else {
            adminNavigation.copy(
                onAiAgent = onAdminAiAgent,
                onUsers = onUsers,
                onExercises = onExercises,
                onGettingStarted = onGettingStartedProgram,
                onWorkoutPlans = onWorkoutPlans,
                onWorkoutSessions = onWorkoutSessions,
                onReviews = onReviews,
                onUserReports = onUserReports,
                onLogout = {
                    viewModel.logout()
                    onLogout()
                }
            )
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadDashboard()
            kotlinx.coroutines.awaitCancellation()
        }
    }

    AdminConsoleScaffold(
        currentSection = AdminNavSection.OVERVIEW,
        title = stringResource(R.string.admin_dashboard),
        subtitle = "Platform metrics & operations overview",
        navigation = effectiveNavigation,
        hasFloatingBottomBar = true,
        headerActions = {
            if (state.isLoading && state.dashboard != null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Accent
                )
            }
            IconButton(
                onClick = viewModel::loadDashboard,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.common_refresh),
                    tint = PrimaryText
                )
            }
        }
    ) { isExpandedLayout ->
        val dashboard = state.dashboard
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isExpandedLayout) 28.dp else 18.dp)
                .padding(top = 18.dp, bottom = FloatingBottomBarDefaults.ContentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AdminError(state.error, onRetry = viewModel::loadDashboard)

            if (state.isLoading && dashboard == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = Accent)
                        Text(
                            text = "Loading dashboard telemetry...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }
            } else if (dashboard == null && state.error == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CardBackground,
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Overview data unavailable",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        Text(
                            text = "Refresh to fetch the latest user, plan, and workout statistics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        OutlinedButton(onClick = viewModel::loadDashboard) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            if (dashboard != null) {
                AdminOverviewGrid(
                    dashboard = dashboard,
                    isExpandedLayout = isExpandedLayout,
                    onUsers = onUsers,
                    onExercises = onExercises,
                    onWorkoutPlans = onWorkoutPlans,
                    onWorkoutSessions = onWorkoutSessions
                )
            }

            AdminAiAgentSpotlightCard(onOpenAiAgent = onAdminAiAgent)
        }
    }
}

@Composable
private fun AdminOverviewGrid(
    dashboard: AdminDashboard,
    isExpandedLayout: Boolean,
    onUsers: () -> Unit,
    onExercises: () -> Unit,
    onWorkoutPlans: () -> Unit,
    onWorkoutSessions: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        AdminSectionBlock(
            title = "Accounts & Community",
            description = "Registered athletes, coaches, and administrators"
        ) {
            val metrics = listOf(
                AdminMetricSpec("Total Users", dashboard.totalUsers.toString(), Icons.Default.People, Info, onUsers),
                AdminMetricSpec("Clients", dashboard.totalClients.toString(), Icons.Default.Person, Accent, onUsers),
                AdminMetricSpec("Coaches", dashboard.totalCoaches.toString(), Icons.Default.Sports, Success, onUsers),
                AdminMetricSpec("Admins", dashboard.totalAdmins.toString(), Icons.Default.AdminPanelSettings, Warning, onUsers)
            )
            AdminMetricGrid(
                items = metrics,
                columns = if (isExpandedLayout) 4 else 2
            )
        }

        AdminSectionBlock(
            title = "Training & Exercise Catalog",
            description = "Structured workout plans, completed sessions, and catalog size"
        ) {
            val metrics = listOf(
                AdminMetricSpec("Workout Plans", dashboard.totalWorkoutPlans.toString(), Icons.Default.CalendarMonth, Info, onWorkoutPlans),
                AdminMetricSpec("Workout Sessions", dashboard.totalWorkoutSessions.toString(), Icons.Default.History, Accent, onWorkoutSessions),
                AdminMetricSpec("Exercises", dashboard.totalExercises.toString(), Icons.Default.FitnessCenter, Success, onExercises)
            )
            AdminMetricGrid(
                items = metrics,
                columns = if (isExpandedLayout) 3 else 2
            )
        }
    }
}

private data class AdminMetricSpec(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
private fun AdminSectionBlock(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }
        content()
    }
}

@Composable
private fun AdminMetricGrid(
    items: List<AdminMetricSpec>,
    columns: Int
) {
    val rows = items.chunked(columns.coerceAtLeast(1))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { metric ->
                    AdminDashboardMetricCard(
                        metric = metric,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat((columns - rowItems.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AdminDashboardMetricCard(
    metric: AdminMetricSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = metric.onClick),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = metric.tint.copy(alpha = 0.14f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = metric.icon,
                            contentDescription = null,
                            tint = metric.tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = SecondaryText.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AdminAiAgentSpotlightCard(onOpenAiAgent: () -> Unit) {
    val colors = LocalAppColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenAiAgent),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Admin AI Agent & PRO Operations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                    Text(
                        text = "Grant or revoke user PRO access with audited confirmation, inspect athlete cohorts, and orchestrate Getting Started campaigns.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onOpenAiAgent,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = colors.onAccent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Open AI Agent", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
    adminNavigation: AdminNavigationCallbacks = AdminNavigationCallbacks(onOverview = onBack),
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var search by rememberSaveable { mutableStateOf("") }
    var roleFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(search, roleFilter, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            kotlinx.coroutines.delay(300)
            viewModel.loadUsers(page = 0, search = search, role = roleFilter)
            kotlinx.coroutines.awaitCancellation()
        }
    }

    AdminConsoleScaffold(
        currentSection = AdminNavSection.USERS,
        title = "Users",
        subtitle = "Account directory & role administration",
        navigation = adminNavigation,
        onBack = onBack,
        headerActions = {
            IconButton(
                onClick = { viewModel.loadUsers(0, search, roleFilter) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.common_refresh),
                    tint = PrimaryText
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_email_name)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SecondaryText)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = roleFilter == null,
                    onClick = { roleFilter = null },
                    label = { Text(stringResource(R.string.common_all)) }
                )
                FilterChip(
                    selected = roleFilter == "ROLE_USER",
                    onClick = { roleFilter = "ROLE_USER" },
                    label = { Text(stringResource(R.string.users)) }
                )
                FilterChip(
                    selected = roleFilter == "ROLE_ADMIN",
                    onClick = { roleFilter = "ROLE_ADMIN" },
                    label = { Text(stringResource(R.string.admins)) }
                )
            }

            AdminError(state.error, onRetry = { viewModel.loadUsers(0, search, roleFilter) })

            state.message?.let {
                Text(it, color = Accent, style = MaterialTheme.typography.bodySmall)
            }

            if (state.isLoading && state.userPage == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            } else {
                val page = state.userPage
                if (page != null) {
                    if (page.content.isEmpty() && !state.isLoading) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.no_users_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(page.content, key = { it.id }) { user ->
                                AdminUserCard(user, onClick = { onUserClick(user.id) })
                            }
                        }
                    }
                    AdminPageControls(page, onPageChange = { viewModel.loadUsers(it, search, roleFilter) })
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AdminUserDetailScreen(
    userId: Long,
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var pendingRole by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(userId) { viewModel.loadUser(userId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AdminHeader("User details", onBack, onRefresh = { viewModel.loadUser(userId) })
        AdminError(state.error, onRetry = { viewModel.loadUser(userId) })

        state.message?.let {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Accent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    color = Accent,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
            LaunchedEffect(it) {
                kotlinx.coroutines.delay(3_000)
                viewModel.clearMessage()
            }
        }

        if (state.isLoading && state.user == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        }

        state.user?.let { user ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText
                            )
                        }
                        AdminSubscriptionPill(user = user)
                    }

                    HorizontalDivider(color = SubtleBorder)

                    Text(
                        text = stringResource(R.string.loc_admin_user_id, user.id),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Text(
                        text = stringResource(R.string.loc_admin_role, roleLabel(user.role)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText
                    )
                    Text(
                        text = stringResource(R.string.loc_admin_gender, user.gender.localizedLabel()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText
                    )
                    Text(
                        text = listOfNotNull(
                            if (user.hasCoachProfile) "Coach profile" else null,
                            if (user.hasClientProfile) "Client profile" else null
                        ).ifEmpty { listOf("No coach/client profile") }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    user.createdAt?.let {
                        Text(
                            text = stringResource(R.string.loc_admin_created, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }

                    HorizontalDivider(color = SubtleBorder)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = if (user.unlimited || user.isProActive) Accent else SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Membership: ${user.subscriptionStatusLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryText
                        )
                    }
                    Text(
                        text = "Quota: " + when {
                            user.unlimited -> "Unlimited video & live analysis"
                            user.isProActive -> "10 daily videos, live analysis unlocked"
                            else -> "1 daily video, live analysis locked"
                        } + " · PRO changes are managed via Admin AI Agent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )

                    Spacer(Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            pendingRole = if (user.role == "ROLE_ADMIN") "ROLE_USER" else "ROLE_ADMIN"
                        },
                        enabled = !state.isUpdatingRole,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (user.role == "ROLE_ADMIN") "Change to user" else "Promote to admin")
                    }
                    if (state.isUpdatingRole) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Accent)
                    }
                }
            }
        }
    }

    pendingRole?.let { newRole ->
        AlertDialog(
            onDismissRequest = { pendingRole = null },
            title = { Text(stringResource(R.string.loc_confirm_role_change)) },
            text = { Text(stringResource(R.string.loc_change_role_question, roleLabel(newRole))) },
            confirmButton = {
                Button(
                    enabled = !state.isUpdatingRole,
                    onClick = {
                        pendingRole = null
                        viewModel.updateUserRole(userId, newRole)
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRole = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun AdminWorkoutPlansScreen(
    onBack: () -> Unit,
    onPlanClick: (Long) -> Unit,
    adminNavigation: AdminNavigationCallbacks = AdminNavigationCallbacks(onOverview = onBack),
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadWorkoutPlans() }

    AdminConsoleScaffold(
        currentSection = AdminNavSection.WORKOUT_PLANS,
        title = stringResource(R.string.workout_plans),
        subtitle = "User and coach workout splits",
        navigation = adminNavigation,
        onBack = onBack,
        headerActions = {
            IconButton(
                onClick = { viewModel.loadWorkoutPlans(state.planPage?.page ?: 0) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.common_refresh),
                    tint = PrimaryText
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminError(state.error, onRetry = { viewModel.loadWorkoutPlans() })
            if (state.isLoading && state.planPage == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            } else {
                val page = state.planPage
                if (page != null) {
                    if (page.content.isEmpty() && !state.isLoading) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.loc_no_workout_plans_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(page.content, key = { it.id }) { plan ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onPlanClick(plan.id) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = CardBackground,
                                    border = BorderStroke(1.dp, SubtleBorder)
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = plan.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryText
                                        )
                                        Text(
                                            text = stringResource(R.string.loc_owner_label, plan.userFullName, plan.userEmail),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryText
                                        )
                                        Text(
                                            text = "${plan.splitType} · Plan #${plan.id}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Info
                                        )
                                    }
                                }
                            }
                        }
                    }
                    AdminPageControls(page, onPageChange = { viewModel.loadWorkoutPlans(it) })
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AdminWorkoutPlanDetailScreen(
    planId: Long,
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(planId) { viewModel.loadWorkoutPlan(planId) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdminHeader("Workout plan", onBack, onRefresh = { viewModel.loadWorkoutPlan(planId) })
        AdminError(state.error, onRetry = { viewModel.loadWorkoutPlan(planId) })
        if (state.isLoading && state.plan == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Accent)
        }
        state.plan?.let { detail ->
            val plan = detail.plan
            Text(plan.name, style = MaterialTheme.typography.headlineSmall, color = PrimaryText)
            Text(stringResource(R.string.loc_owner_label, detail.userFullName, detail.userEmail), color = SecondaryText)
            Text("${plan.splitType.displayName} · ${plan.days.size} scheduled days", color = Info)
            plan.updatedAt?.let { updated ->
                Text(
                    text = stringResource(R.string.loc_updated_at, updated),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            plan.days.forEach { day ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CardBackground,
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${day.dayOfWeek.displayName} · ${day.sessionType.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryText
                        )
                        day.name?.takeIf { it.isNotBlank() }?.let { dayTitle ->
                            Text(dayTitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                        }
                        if (day.isRestDay) {
                            Text(stringResource(R.string.rest_day), color = SecondaryText)
                        } else if (day.exercises.isEmpty()) {
                            Text(stringResource(R.string.loc_no_exercises_assigned_period), color = SecondaryText)
                        } else {
                            day.exercises.forEach { ex ->
                                Text(
                                    text = "• ${ex.exercise.name} — ${ex.prescription}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrimaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminWorkoutSessionsScreen(
    onBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
    adminNavigation: AdminNavigationCallbacks = AdminNavigationCallbacks(onOverview = onBack),
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadWorkoutSessions() }

    AdminConsoleScaffold(
        currentSection = AdminNavSection.WORKOUT_SESSIONS,
        title = stringResource(R.string.workout_sessions),
        subtitle = "Athlete session execution logs",
        navigation = adminNavigation,
        onBack = onBack,
        headerActions = {
            IconButton(
                onClick = { viewModel.loadWorkoutSessions(state.sessionPage?.page ?: 0) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.common_refresh),
                    tint = PrimaryText
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminError(state.error, onRetry = { viewModel.loadWorkoutSessions() })
            if (state.isLoading && state.sessionPage == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            } else {
                val page = state.sessionPage
                if (page != null) {
                    if (page.content.isEmpty() && !state.isLoading) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.loc_no_recent_workouts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(page.content, key = { it.id }) { session ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSessionClick(session.id) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = CardBackground,
                                    border = BorderStroke(1.dp, SubtleBorder)
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryText
                                        )
                                        Text(
                                            text = stringResource(R.string.loc_owner_label, session.userFullName, session.userEmail),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryText
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.loc_status_date,
                                                session.status,
                                                session.workoutDate ?: stringResource(R.string.date_unavailable)
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Info
                                        )
                                    }
                                }
                            }
                        }
                    }
                    AdminPageControls(page, onPageChange = { viewModel.loadWorkoutSessions(it) })
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AdminWorkoutSessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(sessionId) { viewModel.loadWorkoutSession(sessionId) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdminHeader("Workout session", onBack, onRefresh = { viewModel.loadWorkoutSession(sessionId) })
        AdminError(state.error, onRetry = { viewModel.loadWorkoutSession(sessionId) })
        if (state.isLoading && state.session == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Accent)
        }
        state.session?.let { detail ->
            val session = detail.session
            Text(session.title, style = MaterialTheme.typography.headlineSmall, color = PrimaryText)
            Text(stringResource(R.string.loc_owner_label, detail.userFullName, detail.userEmail), color = SecondaryText)
            Text(
                stringResource(R.string.loc_status_date, session.status, session.workoutDate ?: stringResource(R.string.date_unavailable)),
                color = PrimaryText
            )
            Text(
                text = "${session.completedExercises} completed · ${session.skippedExercises} skipped · " +
                    "${session.completedSets} completed sets",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
            if (session.exercises.isEmpty()) Text(stringResource(R.string.loc_no_exercise_details), color = SecondaryText)
            session.exercises.forEach { exercise ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CardBackground,
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium, color = PrimaryText)
                        Text(stringResource(R.string.loc_target_sets_label, exercise.status, exercise.targetSets), color = SecondaryText)
                        exercise.sets.forEach { set ->
                            Text(
                                text = "Set ${set.setNumber}: ${set.reps?.let { "$it reps" } ?: "—"}" +
                                    (if (set.completed) " · complete" else " · incomplete"),
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserCard(user: AdminUser, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                AdminSubscriptionPill(user = user)
            }
            Text(user.email, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listOfNotNull(
                        roleLabel(user.role),
                        if (user.hasCoachProfile) "Coach" else null,
                        if (user.hasClientProfile) "Client" else null
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Text(
                    text = stringResource(R.string.loc_id_simple, user.id),
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
private fun AdminSubscriptionPill(user: AdminUser) {
    val pillColor = when {
        user.isAdminRole -> Warning
        user.adminGrantedPro -> Accent
        user.paidPro || user.membershipTier.equals("PRO", ignoreCase = true) -> Info
        else -> SecondaryText
    }
    val label = when {
        user.isAdminRole -> "ADMIN • Unlimited"
        user.adminGrantedPro -> "PRO • Unlimited"
        user.paidPro || user.membershipTier.equals("PRO", ignoreCase = true) -> "PRO • Paid"
        else -> "FREE"
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = pillColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, pillColor.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = pillColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AdminHeader(title: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = PrimaryText
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRefresh, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.common_refresh),
                tint = PrimaryText
            )
        }
    }
}

@Composable
private fun AdminPageControls(page: AdminPage<*>, onPageChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            enabled = page.page > 0,
            onClick = { onPageChange(page.page - 1) },
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.common_previous))
        }
        Text(
            text = stringResource(R.string.loc_page_count, page.page + 1, page.totalPages.coerceAtLeast(1)),
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText
        )
        OutlinedButton(
            enabled = page.hasNext,
            onClick = { onPageChange(page.page + 1) },
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.common_next))
        }
    }
}

@Composable
private fun AdminError(message: String?, onRetry: () -> Unit) {
    if (message != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Error.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, Error.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.common_retry), color = Error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun roleLabel(role: String): String = when (role) {
    "ROLE_ADMIN" -> "Admin"
    "ROLE_USER" -> "User"
    else -> "Unknown role"
}
