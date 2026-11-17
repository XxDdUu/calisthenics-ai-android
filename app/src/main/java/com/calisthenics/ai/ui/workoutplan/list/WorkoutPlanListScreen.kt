package com.calisthenics.ai.ui.workoutplan.list

import com.calisthenics.ai.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutPlanAssignment
import com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.home.GettingStartedActivePlanCard
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Surface
import com.calisthenics.ai.ui.workoutplan.ai.AiWorkoutPlannerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlanListScreen(
    onPlanClick: (Long) -> Unit,
    onOpenAiPlanner: ((Long?, String?) -> Unit)? = null,
    onPlanClickWithDate: ((Long, String?) -> Unit)? = null,
    onStartGettingStartedSession: (WorkoutSession) -> Unit = {},
    viewModel: WorkoutPlanListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isAiPlannerOpen by rememberSaveable { mutableStateOf(false) }

    val openAiPlanner = {
        val selectedClient = uiState.clients.firstOrNull { it.clientId == uiState.selectedClientId }
        val clientId = if (selectedTab == 1) uiState.selectedClientId else null
        val clientName = if (selectedTab == 1) selectedClient?.clientName else null
        if (onOpenAiPlanner != null) {
            onOpenAiPlanner(clientId, clientName)
        } else {
            isAiPlannerOpen = true
        }
    }
    var isAssignmentSheetOpen by rememberSaveable { mutableStateOf(false) }
    // A coach may have assignments even when there are no currently active client
    // relationships, so assignment ownership is also a signal for the coach view.
    val hasCoachAssignments = uiState.coachAssignments.isNotEmpty() || uiState.clients.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.loc_workout_plans_title),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )
                },
                actions = {
                    if (selectedTab == 2) {
                        Button(
                            onClick = {
                                viewModel.clearAssignmentMessage()
                                if (uiState.availableExercises.isEmpty()) {
                                    viewModel.loadExercisesCatalog()
                                }
                                isAssignmentSheetOpen = true
                            },
                            modifier = Modifier.padding(end = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Ink
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(stringResource(R.string.assign_workout), fontSize = 12.sp)
                        }
                    }
                    IconButton(
                        onClick = { openAiPlanner() },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.ai_workout_planner_title),
                            tint = Accent
                        )
                    }

                    IconButton(
                        onClick = { viewModel.openCreateDialog() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(Surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.create_plan),
                            tint = Accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink,
                    titleContentColor = PrimaryText
                )
            )
        },
        containerColor = Ink
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Ink,
                    contentColor = PrimaryText,
                    edgePadding = 16.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.loc_your_personal_plan), maxLines = 1) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.loc_client_personal_plan), maxLines = 1) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(stringResource(R.string.loc_assignment_plan), maxLines = 1) }
                    )
                }

                when {
                selectedTab == 1 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        if (uiState.clients.isEmpty()) {
                            Text(stringResource(R.string.loc_no_connected_clients), color = SecondaryText)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.clients.forEach { client ->
                                    FilterChip(
                                        selected = uiState.selectedClientId == client.clientId,
                                        onClick = { viewModel.selectClient(client.clientId) },
                                        label = { Text(client.clientName, maxLines = 1) }
                                    )
                                }
                            }
                            if (uiState.selectedClientId != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { openAiPlanner() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Accent,
                                            contentColor = Ink
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.ai_planner_cta_button), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                            when {
                                uiState.isLoadingClientPlans -> CircularProgressIndicator()
                                uiState.clientPlanError != null -> Text(
                                    uiState.clientPlanError ?: "",
                                    color = Error,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                uiState.clientPlans.isEmpty() -> Text(
                                    stringResource(R.string.loc_this_client_no_personal_plans),
                                    color = SecondaryText,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                else -> LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(uiState.clientPlans, key = { it.id }) { plan ->
                                        WorkoutPlanCard(plan, { onPlanClick(plan.id) }, {})
                                    }
                                }
                            }
                        }
                    }
                }
                selectedTab == 2 -> {
                    if (!hasCoachAssignments) {
                        if (uiState.assignments.isEmpty()) {
                            Text(stringResource(R.string.loc_no_assigned_workout_plans), color = SecondaryText, modifier = Modifier.padding(20.dp))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                items(uiState.assignments, key = { it.id }) { assignment ->
                                    WorkoutPlanCard(
                                        assignment.workoutPlan,
                                        {
                                            if (assignment.status == WorkoutPlanAssignmentStatus.ACTIVE) {
                                                if (onPlanClickWithDate != null) {
                                                    onPlanClickWithDate(assignment.workoutPlan.id, assignment.startDate)
                                                } else {
                                                    onPlanClick(assignment.workoutPlan.id)
                                                }
                                            }
                                        },
                                        null,
                                        subtitle = (assignment.coachName?.let { stringResource(R.string.assigned_by_coach, it) }
                                            ?: stringResource(R.string.assigned_by_your_coach)) +
                                            (assignment.startDate?.let { " • ${stringResource(R.string.loc_starts_date, it)}" } ?: ""),
                                        assignment = assignment
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp)
                        ) {
                            uiState.assignmentError?.let { errorMessage ->
                                Text(
                                    text = errorMessage,
                                    color = Error,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Text(
                                        stringResource(R.string.loc_coach_assignments),
                                        color = PrimaryText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (uiState.coachAssignments.isEmpty()) {
                                    item {
                                        Text(
                                            stringResource(R.string.loc_no_assigned_workout_plans),
                                            color = SecondaryText
                                        )
                                    }
                                } else {
                                    items(uiState.coachAssignments, key = { it.id }) { assignment ->
                                        WorkoutPlanCard(
                                            plan = assignment.workoutPlan,
                                            onClick = {
                                                if (onPlanClickWithDate != null) {
                                                    onPlanClickWithDate(assignment.workoutPlan.id, assignment.startDate)
                                                } else {
                                                    onPlanClick(assignment.workoutPlan.id)
                                                }
                                            },
                                            onDeleteClick = null,
                                            subtitle = assignment.clientName?.let {
                                                stringResource(R.string.loc_assignment_for_athlete, it)
                                            },
                                            assignment = assignment,
                                            onAssignmentToggle = {
                                                viewModel.requestAssignmentStatusChange(assignment)
                                            },
                                            isAssignmentUpdating = uiState.updatingAssignmentId == assignment.id
                                        )
                                    }
                                }
                                uiState.assignmentMessage?.let {
                                    item {
                                        Text(
                                            it,
                                            color = if (it.startsWith("Plan assigned")) PrimaryText else Error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                uiState.isLoading && uiState.plans.isEmpty() && uiState.activeGettingStartedProgram == null -> {
                    LoadingView(message = stringResource(R.string.loc_loading_workout_plans))
                }
                uiState.error != null && uiState.plans.isEmpty() && uiState.activeGettingStartedProgram == null -> {
                    ErrorView(
                        message = uiState.error ?: stringResource(R.string.loc_unable_load_workout_plans),
                        onRetry = { viewModel.loadPlans() }
                    )
                }
                uiState.plans.isEmpty() && uiState.activeGettingStartedProgram == null -> {
                    EmptyWorkoutPlansView(
                        isOnline = uiState.isOnline,
                        onAiPlannerClick = { openAiPlanner() },
                        onCreateClick = { viewModel.openCreateDialog() },
                        onRetryClick = { viewModel.loadPlans() }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentPadding = PaddingValues(bottom = FloatingBottomBarDefaults.ContentBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            AiWorkoutPlannerBanner(
                                onPlanWithAiClick = { openAiPlanner() }
                            )
                        }
                        uiState.activeGettingStartedProgram?.let { program ->
                            item(key = "getting-started-active-program") {
                                GettingStartedActivePlanCard(
                                    program = program,
                                    startingDayNumber = uiState.startingGettingStartedDay,
                                    onStartDay = { dayNumber ->
                                        viewModel.startGettingStartedDay(
                                            dayNumber = dayNumber,
                                            onSessionStarted = onStartGettingStartedSession
                                        )
                                    }
                                )
                            }
                        }
                        uiState.gettingStartedError?.let { errorMsg ->
                            item(key = "getting-started-error") {
                                Text(
                                    text = errorMsg,
                                    color = Error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (uiState.plans.isEmpty()) {
                            item(key = "create-personal-plan-cta") {
                                OutlinedButton(
                                    onClick = { viewModel.openCreateDialog() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.create_plan))
                                }
                            }
                        } else {
                            items(uiState.plans, key = { it.id }) { plan ->
                                WorkoutPlanCard(
                                    plan = plan,
                                    onClick = { onPlanClick(plan.id) },
                                    onDeleteClick = { viewModel.confirmDeletePlan(plan) }
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                    }
                }
            }
        }
    }

    uiState.assignmentToDeactivate?.let { assignment ->
        AlertDialog(
            onDismissRequest = viewModel::dismissAssignmentDeactivation,
            title = {
                Text(
                    text = stringResource(R.string.loc_deactivate_assignment_question),
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.loc_deactivate_assignment_description),
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmAssignmentDeactivation,
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    enabled = uiState.updatingAssignmentId == null
                ) {
                    Text(stringResource(R.string.loc_deactivate_assignment))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAssignmentDeactivation) {
                    Text(stringResource(R.string.cancel), color = SecondaryText)
                }
            },
            containerColor = Elevated,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // AI Workout Planner full screen (fallback if not navigated via NavController)
    if (isAiPlannerOpen) {
        val selectedClient = uiState.clients.firstOrNull { it.clientId == uiState.selectedClientId }
        AiWorkoutPlannerScreen(
            clientId = if (selectedTab == 1) uiState.selectedClientId else null,
            clientName = if (selectedTab == 1) selectedClient?.clientName else null,
            onBackClick = { isAiPlannerOpen = false },
            onPlanCommitted = { committedPlan ->
                isAiPlannerOpen = false
                viewModel.loadPlans()
                if (selectedTab == 1 && uiState.selectedClientId != null) {
                    viewModel.selectClient(uiState.selectedClientId!!)
                }
                onPlanClick(committedPlan.id)
            }
        )
    }

    // Create Plan Dialog
    if (uiState.isCreateDialogOpen) {
        CreatePlanDialog(
            name = uiState.createPlanName,
            splitType = uiState.createSplitType,
            isCreating = uiState.isCreating,
            errorMessage = uiState.createError,
            onNameChange = viewModel::updateCreatePlanName,
            onSplitTypeChange = viewModel::updateCreateSplitType,
            onDismiss = viewModel::closeCreateDialog,
            onConfirm = { viewModel.submitCreatePlan(onSuccess = onPlanClick) }
        )
    }

    // Delete Confirmation Dialog
    uiState.planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = {
                Text(
                    text = stringResource(R.string.loc_delete_plan_question),
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.loc_delete_plan_confirm, plan.name),
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::executeDeletePlan,
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    enabled = !uiState.isDeleting
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(
                            color = PrimaryText,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.common_delete), color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text(stringResource(R.string.cancel), color = SecondaryText)
                }
            },
            containerColor = Elevated,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (isAssignmentSheetOpen) {
        CoachAssignmentBottomSheet(
            relationships = uiState.clients,
            plans = uiState.plans,
            selectedClientId = uiState.selectedClientId,
            onSelectClient = viewModel::selectClient,
            isSubmitting = uiState.assigningPlanId != null,
            error = uiState.assignmentMessage,
            availableExercises = uiState.availableExercises,
            isLoadingExercises = uiState.isLoadingExercises,
            exercisesError = uiState.exercisesError,
            onRetryLoadExercises = viewModel::loadExercisesCatalog,
            onClearError = viewModel::clearAssignmentMessage,
            onAssign = { clientId, planId, _ ->
                viewModel.assignPlanToClient(clientId, planId) {
                    isAssignmentSheetOpen = false
                }
            },
            onAssignWithDetails = { clientId, planId, startDate, endDate, notes, _ ->
                viewModel.assignPlanWithDetailsToClient(clientId, planId, startDate, endDate, notes) {
                    isAssignmentSheetOpen = false
                }
            },
            onCreateAndAssignCustomWorkout = { clientId, name, sessionType, targetDay, exercises, startDate, endDate, notes, _ ->
                viewModel.createCustomWorkoutAndAssignToClient(
                    clientId = clientId,
                    workoutName = name,
                    sessionType = sessionType,
                    targetDay = targetDay,
                    configuredExercises = exercises,
                    startDate = startDate,
                    endDate = endDate,
                    notes = notes,
                    onSuccess = { isAssignmentSheetOpen = false }
                )
            },
            onDismiss = {
                if (uiState.assigningPlanId == null) {
                    isAssignmentSheetOpen = false
                    viewModel.clearAssignmentMessage()
                }
            }
        )
    }
}

@Composable
fun WorkoutPlanCard(
    plan: UserWorkoutPlan,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    subtitle: String? = null,
    assignment: WorkoutPlanAssignment? = null,
    onAssignmentToggle: (() -> Unit)? = null,
    isAssignmentUpdating: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            subtitle?.let { Text(it, color = SecondaryText, style = MaterialTheme.typography.bodySmall) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Split Type Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = plan.splitType.displayName,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            color = Accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                onDeleteClick?.let { delete ->
                    IconButton(
                        onClick = delete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.loc_delete_plan_content_description),
                            tint = SecondaryText.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            assignment?.let { currentAssignment ->
                val isActive = currentAssignment.status == WorkoutPlanAssignmentStatus.ACTIVE
                val isInactive = currentAssignment.status == WorkoutPlanAssignmentStatus.INACTIVE
                val isToggleable = isActive || isInactive
                val statusLabel = when (currentAssignment.status) {
                    WorkoutPlanAssignmentStatus.ACTIVE -> R.string.loc_assignment_status_active
                    WorkoutPlanAssignmentStatus.INACTIVE -> R.string.loc_assignment_status_inactive
                    WorkoutPlanAssignmentStatus.CANCELLED -> R.string.loc_assignment_status_cancelled
                    WorkoutPlanAssignmentStatus.COMPLETED -> R.string.completed_status
                    WorkoutPlanAssignmentStatus.UPCOMING -> R.string.upcoming_status
                    else -> null
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    statusLabel?.let { labelRes ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background((if (isActive) Accent else Error).copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                color = if (isActive) Accent else Error,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    onAssignmentToggle?.takeIf { isToggleable }?.let { toggle ->
                        TextButton(onClick = toggle, enabled = !isAssignmentUpdating) {
                            if (isAssignmentUpdating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    stringResource(
                                        if (isActive) R.string.loc_deactivate_assignment
                                        else R.string.loc_reactivate_assignment
                                    ),
                                    color = if (isActive) Error else Accent
                                )
                            }
                        }
                    }
                }
                if (isInactive && onAssignmentToggle == null) {
                    Text(
                        text = stringResource(R.string.loc_inactive_assignment_history),
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Plan Name
            Text(
                text = plan.name,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Training days count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.loc_training_days_count, plan.trainingDayCount),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                color = SecondaryText
                            )
                        )
                    }

                    // Exercises count
                    if (plan.totalExerciseCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.exercises_count_label, plan.totalExerciseCount),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                color = SecondaryText
                            )
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.loc_open_plan),
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
}

@Composable
fun EmptyWorkoutPlansView(
    isOnline: Boolean = true,
    onAiPlannerClick: () -> Unit = {},
    onCreateClick: () -> Unit,
    onRetryClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (!isOnline) stringResource(R.string.loc_offline_mode) else stringResource(R.string.loc_no_workout_plans_yet),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (!isOnline)
                stringResource(R.string.no_cached_schedule)
            else
                stringResource(R.string.loc_create_plan_description),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                color = SecondaryText
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isOnline) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onAiPlannerClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Ink
                    ),
                    modifier = Modifier.height(48.dp).fillMaxWidth(0.85f)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ai_planner_cta_button),
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onCreateClick,
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.loc_create_first_plan),
                        color = SecondaryText
                    )
                }
            }
        } else {
            Button(
                onClick = onRetryClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Ink
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.loc_retry_connection),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AiWorkoutPlannerBanner(
    onPlanWithAiClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlanWithAiClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.ai_planner_banner_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Accent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.ai_planner_banner_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                )

                Button(
                    onClick = onPlanWithAiClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Ink
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_planner_cta_button),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePlanDialog(
    name: String,
    splitType: WorkoutSplitType,
    isCreating: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onSplitTypeChange: (WorkoutSplitType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.loc_new_workout_plan),
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.loc_plan_name_label),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                        color = SecondaryText
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = { Text(stringResource(R.string.loc_example_plan_focus), color = SecondaryText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border,
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.loc_weekly_split_structure),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                        color = SecondaryText
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutSplitType.entries.forEach { split ->
                        val isSelected = split == splitType
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Surface else Elevated)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Accent else Border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSplitTypeChange(split) }
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = split.displayName,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Accent else PrimaryText
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (split) {
                                        WorkoutSplitType.PPL -> stringResource(R.string.loc_split_ppl)
                                        WorkoutSplitType.UPPER_LOWER -> stringResource(R.string.loc_split_upper_lower)
                                        WorkoutSplitType.FULL_BODY -> stringResource(R.string.loc_split_full_body)
                                        WorkoutSplitType.CUSTOM -> stringResource(R.string.loc_split_custom)
                                    },
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                        color = SecondaryText
                                    )
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = Error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isCreating && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Ink
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        color = Ink,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.create_plan), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = SecondaryText)
            }
        },
        containerColor = Elevated,
        shape = RoundedCornerShape(20.dp)
    )
}
