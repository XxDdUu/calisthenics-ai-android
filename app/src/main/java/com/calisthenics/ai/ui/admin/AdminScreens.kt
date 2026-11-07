package com.calisthenics.ai.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.AdminDashboard
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminUser
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.theme.Ink

@Composable
fun AdminDashboardScreen(
    onUsers: () -> Unit,
    onExercises: () -> Unit = {},
    onWorkoutPlans: () -> Unit,
    onWorkoutSessions: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadDashboard()
            kotlinx.coroutines.awaitCancellation()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Admin dashboard", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = {
                viewModel.logout()
                onLogout()
            }) { Text("Log out") }
        }
        AdminError(state.error, onRetry = viewModel::loadDashboard)
        if (state.isLoading && state.dashboard == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        state.dashboard?.let { AdminOverviewGrid(it) }
        Text("Management", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onUsers, modifier = Modifier.fillMaxWidth()) { Text("Manage users") }
        OutlinedButton(onClick = onExercises, modifier = Modifier.fillMaxWidth()) { Text("Manage exercises") }
        OutlinedButton(onClick = onWorkoutPlans, modifier = Modifier.fillMaxWidth()) { Text("Workout plans") }
        OutlinedButton(onClick = onWorkoutSessions, modifier = Modifier.fillMaxWidth()) { Text("Workout sessions") }
        if (state.isLoading && state.dashboard != null) {
            CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.CenterHorizontally))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminOverviewGrid(dashboard: AdminDashboard) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Total users", dashboard.totalUsers.toString(), Modifier.weight(1f))
        StatCard("Total clients", dashboard.totalClients.toString(), Modifier.weight(1f))
        StatCard("Total coaches", dashboard.totalCoaches.toString(), Modifier.weight(1f))
        StatCard("Total admins", dashboard.totalAdmins.toString(), Modifier.weight(1f))
        StatCard("Workout sessions", dashboard.totalWorkoutSessions.toString(), Modifier.weight(1f))
        StatCard("Workout plans", dashboard.totalWorkoutPlans.toString(), Modifier.weight(1f))
        StatCard("Total exercises", dashboard.totalExercises.toString(), Modifier.weight(1f))
    }
}

@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 10.dp)
    ) {
        AdminHeader("Users", onBack, onRefresh = { viewModel.loadUsers(0, search, roleFilter) })
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search email or name") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            FilterChip(selected = roleFilter == null, onClick = { roleFilter = null }, label = { Text("All") })
            FilterChip(selected = roleFilter == "ROLE_USER", onClick = { roleFilter = "ROLE_USER" }, label = { Text("Users") })
            FilterChip(selected = roleFilter == "ROLE_ADMIN", onClick = { roleFilter = "ROLE_ADMIN" }, label = { Text("Admins") })
        }
        AdminError(state.error, onRetry = { viewModel.loadUsers(0, search, roleFilter) })
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
        }
        if (state.isLoading && state.userPage == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        val page = state.userPage
        if (page != null) {
            if (page.content.isEmpty() && !state.isLoading) {
                Text("No users found.", modifier = Modifier.padding(16.dp))
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
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AdminHeader("User details", onBack, onRefresh = { viewModel.loadUser(userId) })
        AdminError(state.error, onRetry = { viewModel.loadUser(userId) })
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
            LaunchedEffect(it) {
                kotlinx.coroutines.delay(3_000)
                viewModel.clearMessage()
            }
        }
        if (state.isLoading && state.user == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        state.user?.let { user ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(user.fullName, style = MaterialTheme.typography.titleLarge)
                    Text(user.email)
                    Text("User ID: ${user.id}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Role: ${roleLabel(user.role)}")
                    Text(
                        listOfNotNull(
                            if (user.hasCoachProfile) "Coach profile" else null,
                            if (user.hasClientProfile) "Client profile" else null
                        ).ifEmpty { listOf("No coach/client profile") }.joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    user.createdAt?.let { Text("Created: $it", style = MaterialTheme.typography.bodySmall) }
                    OutlinedButton(
                        onClick = {
                            pendingRole = if (user.role == "ROLE_ADMIN") "ROLE_USER" else "ROLE_ADMIN"
                        },
                        enabled = !state.isUpdatingRole
                    ) {
                        Text(if (user.role == "ROLE_ADMIN") "Change to user" else "Promote to admin")
                    }
                    if (state.isUpdatingRole) CircularProgressIndicator(Modifier.size(22.dp))
                }
            }
        }
    }

    pendingRole?.let { newRole ->
        AlertDialog(
            onDismissRequest = { pendingRole = null },
            title = { Text("Confirm role change") },
            text = { Text("Change this account's role to ${roleLabel(newRole)}?") },
            confirmButton = {
                Button(
                    enabled = !state.isUpdatingRole,
                    onClick = {
                        pendingRole = null
                        viewModel.updateUserRole(userId, newRole)
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRole = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminWorkoutPlansScreen(
    onBack: () -> Unit,
    onPlanClick: (Long) -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadWorkoutPlans() }
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 16.dp)
    ) {
        AdminHeader("Workout plans", onBack, onRefresh = { viewModel.loadWorkoutPlans(state.planPage?.page ?: 0) })
        AdminError(state.error, onRetry = { viewModel.loadWorkoutPlans(state.planPage?.page ?: 0) })
        if (state.isLoading && state.planPage == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        val page = state.planPage
        if (page != null) {
            if (page.content.isEmpty()) {
                Text("No workout plans found.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(page.content, key = { it.id }) { plan ->
                        Card(
                            Modifier.fillMaxWidth().clickable { onPlanClick(plan.id) }
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(plan.name, style = MaterialTheme.typography.titleMedium)
                                Text("${plan.userFullName} · ${plan.userEmail}")
                                Text("${plan.splitType} · Plan #${plan.id}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            AdminPageControls(page, onPageChange = viewModel::loadWorkoutPlans)
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
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        state.plan?.let { detail ->
            Text(detail.plan.name, style = MaterialTheme.typography.headlineSmall)
            Text("Owner: ${detail.userFullName} · ${detail.userEmail}")
            Text("${detail.plan.splitType.displayName} · ${detail.plan.days.size} scheduled days")
            if (detail.plan.days.isEmpty()) Text("This plan has no scheduled days.")
            detail.plan.days.forEach { day ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${day.dayOfWeek.displayName} · ${day.sessionType.displayName}", style = MaterialTheme.typography.titleMedium)
                        day.name?.let { Text(it) }
                        if (day.exercises.isEmpty()) Text("No exercises assigned.")
                        day.exercises.forEach { exercise ->
                            Text("• ${exercise.exercise.name} — ${exercise.prescription}")
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
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadWorkoutSessions() }
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 16.dp)
    ) {
        AdminHeader("Workout sessions", onBack, onRefresh = { viewModel.loadWorkoutSessions(state.sessionPage?.page ?: 0) })
        AdminError(state.error, onRetry = { viewModel.loadWorkoutSessions(state.sessionPage?.page ?: 0) })
        if (state.isLoading && state.sessionPage == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        val page = state.sessionPage
        if (page != null) {
            if (page.content.isEmpty()) {
                Text("No workout sessions found.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(page.content, key = { it.id }) { session ->
                        Card(Modifier.fillMaxWidth().clickable { onSessionClick(session.id) }) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(session.title, style = MaterialTheme.typography.titleMedium)
                                Text("${session.userFullName} · ${session.userEmail}")
                                Text("${session.status} · ${session.workoutDate ?: "Date unavailable"}")
                            }
                        }
                    }
                }
            }
            AdminPageControls(page, onPageChange = viewModel::loadWorkoutSessions)
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
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        state.session?.let { detail ->
            val session = detail.session
            Text(session.title, style = MaterialTheme.typography.headlineSmall)
            Text("Owner: ${detail.userFullName} · ${detail.userEmail}")
            Text("${session.status} · ${session.workoutDate ?: "Date unavailable"}")
            Text(
                "${session.completedExercises} completed · ${session.skippedExercises} skipped · " +
                    "${session.completedSets} completed sets"
            )
            if (session.exercises.isEmpty()) Text("No exercise details are available.")
            session.exercises.forEach { exercise ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                        Text("${exercise.status} · ${exercise.targetSets} sets")
                        exercise.sets.forEach { set ->
                            Text(
                                "Set ${set.setNumber}: ${set.reps?.let { "$it reps" } ?: "—"}" +
                                    (if (set.completed) " · complete" else " · incomplete")
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
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(user.fullName, style = MaterialTheme.typography.titleMedium)
            Text(user.email)
            Text(
                listOfNotNull(
                    roleLabel(user.role),
                    if (user.hasCoachProfile) "Coach" else null,
                    if (user.hasClientProfile) "Client" else null
                ).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("ID: ${user.id}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AdminHeader(title: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("Back") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onRefresh) { Text("Refresh") }
    }
}

@Composable
private fun AdminPageControls(page: AdminPage<*>, onPageChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(enabled = page.page > 0, onClick = { onPageChange(page.page - 1) }) { Text("Previous") }
        Text("${page.page + 1} / ${page.totalPages.coerceAtLeast(1)}")
        TextButton(enabled = page.hasNext, onClick = { onPageChange(page.page + 1) }) { Text("Next") }
    }
}

@Composable
private fun AdminError(message: String?, onRetry: () -> Unit) {
    if (message != null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

private fun roleLabel(role: String): String = when (role) {
    "ROLE_ADMIN" -> "Admin"
    "ROLE_USER" -> "User"
    else -> "Unknown role"
}
