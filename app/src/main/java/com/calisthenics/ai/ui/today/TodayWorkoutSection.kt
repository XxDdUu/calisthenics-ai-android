package com.calisthenics.ai.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSource
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

@Composable
fun TodayWorkoutSection(
    onViewDetails: (Long, WorkoutDayOfWeek) -> Unit,
    onStartWorkout: (Long, Long, String, Long?) -> Unit,
    onViewPlans: () -> Unit,
    onViewHistory: () -> Unit,
    onResumeSession: (WorkoutSession) -> Unit,
    viewModel: TodayWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onAppResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Today's Workout", style = MaterialTheme.typography.headlineMedium)
                Text(
                    state.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Your training plan for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh today's workout")
            }
        }

        when {
            state.isLoading -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Loading today's workout…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.error != null -> MessageCard(
                title = "Workout unavailable",
                body = state.error ?: "Could not load today's workout.",
                action = "Retry",
                onAction = viewModel::refresh
            )
            state.selection.kind == TodayWorkoutKind.NO_PLANS -> MessageCard(
                title = "No workout plan yet.",
                body = "Create a plan to see your scheduled training here.",
                action = "View workout plans",
                onAction = onViewPlans
            )
            state.selection.kind == TodayWorkoutKind.PLAN_SELECTION_REQUIRED -> PlanChoiceCard(
                plans = state.plans,
                source = state.selection.source,
                onSelect = viewModel::selectPlan
            )
            else -> {
                state.selection.plan?.let { plan ->
                    if (state.plans.size > 1) {
                        ActivePlanPicker(
                            selectedPlan = plan,
                            plans = state.plans,
                            onSelect = viewModel::selectPlan
                        )
                    }
                }
                when (state.selection.kind) {
                    TodayWorkoutKind.REST_DAY -> RestDayCard(
                        plan = requireNotNull(state.selection.plan),
                        day = requireNotNull(state.selection.day),
                        source = state.selection.source,
                        coachName = state.selection.coachName,
                        onViewDetails = onViewDetails
                    )
                    TodayWorkoutKind.NOT_SCHEDULED -> NotScheduledCard(
                        plan = requireNotNull(state.selection.plan),
                        dayOfWeek = state.date.dayOfWeek.value.let(WorkoutDayOfWeek::fromOrder),
                        onViewDetails = onViewDetails
                    )
                    TodayWorkoutKind.SCHEDULED -> {
                        val plan = requireNotNull(state.selection.plan)
                        val day = requireNotNull(state.selection.day)
                        ScheduledWorkoutCard(
                            plan = plan,
                            day = day,
                            date = state.date.toString(),
                            source = state.selection.source,
                            coachName = state.selection.coachName,
                            assignmentId = state.selection.assignmentId,
                            onViewDetails = onViewDetails,
                            onStartWorkout = onStartWorkout
                        )
                    }
                    else -> Unit
                }
            }
        }
        if (state.activeSessions.isNotEmpty()) {
            ActiveWorkoutRecoveryCard(state.activeSessions, onResumeSession)
        } else if (state.activeSessionsError != null) {
            Text(
                state.activeSessionsError ?: "Unfinished workouts are unavailable.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(onClick = onViewHistory) { Text("Workout history") }

        when {
            state.selection.plan != null -> {
                val plan = requireNotNull(state.selection.plan)
                WeeklyWorkoutScheduleSection(
                    plan = plan,
                    today = state.date,
                    onDayClick = { day -> onViewDetails(plan.id, day) }
                )
            }
            state.isLoading -> WeeklyScheduleEmptySection(state.date, "Loading your weekly schedule…")
            state.error != null -> WeeklyScheduleEmptySection(state.date, "Your schedule is unavailable. Retry above.")
            state.selection.kind == TodayWorkoutKind.PLAN_SELECTION_REQUIRED ->
                WeeklyScheduleEmptySection(state.date, "Choose an active plan to see its weekly schedule.")
            else -> WeeklyScheduleEmptySection(state.date, "Create a workout plan to see your weekly schedule.")
        }
    }
}

@Composable
private fun ActiveWorkoutRecoveryCard(
    sessions: List<WorkoutSession>,
    onResumeSession: (WorkoutSession) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Unfinished workouts", style = MaterialTheme.typography.titleLarge)
            Text(
                "Choose a session to resume. Older sessions remain available and are not replaced.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            sessions.forEach { session ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(session.title, fontWeight = FontWeight.Medium)
                        Text(
                            session.workoutDate ?: "Date unavailable",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedButton(onClick = { onResumeSession(session) }) { Text("Resume") }
                }
            }
        }
    }
}

@Composable
private fun PlanChoiceCard(
    plans: List<UserWorkoutPlan>,
    source: WorkoutSource,
    onSelect: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (source == WorkoutSource.COACH) "Choose a Coach-assigned plan" else "Choose your personal plan",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "You have multiple plans. Select which one to use for today's workout.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            plans.forEach { plan ->
                OutlinedButton(onClick = { onSelect(plan.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(plan.name)
                }
            }
        }
    }
}

@Composable
private fun ActivePlanPicker(
    selectedPlan: UserWorkoutPlan,
    plans: List<UserWorkoutPlan>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Active plan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(4.dp))
        TextButton(onClick = { expanded = true }) { Text(selectedPlan.name) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            plans.forEach { plan ->
                DropdownMenuItem(
                    text = { Text(plan.name) },
                    onClick = {
                        expanded = false
                        if (plan.id != selectedPlan.id) onSelect(plan.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun RestDayCard(
    plan: UserWorkoutPlan,
    day: UserWorkoutPlanDay,
    source: WorkoutSource,
    coachName: String?,
    onViewDetails: (Long, WorkoutDayOfWeek) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("REST DAY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Recovery is part of progress.", style = MaterialTheme.typography.titleLarge)
            Text(
                sourceLabel(source, coachName),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text("No exercises are scheduled for today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onViewDetails(plan.id, day.dayOfWeek) }) { Text("View workout details") }
        }
    }
}

@Composable
private fun NotScheduledCard(
    plan: UserWorkoutPlan,
    dayOfWeek: WorkoutDayOfWeek,
    onViewDetails: (Long, WorkoutDayOfWeek) -> Unit
) {
    MessageCard(
        title = "Nothing scheduled for today.",
        body = "This day is not configured as a workout or an explicit rest day in ${plan.name}.",
        action = "View weekly schedule",
        onAction = { onViewDetails(plan.id, dayOfWeek) }
    )
}

@Composable
private fun ScheduledWorkoutCard(
    plan: UserWorkoutPlan,
    day: UserWorkoutPlanDay,
    date: String,
    source: WorkoutSource,
    coachName: String?,
    assignmentId: Long?,
    onViewDetails: (Long, WorkoutDayOfWeek) -> Unit,
    onStartWorkout: (Long, Long, String, Long?) -> Unit
) {
    val exercises = day.exercises.sortedBy { it.displayOrder }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TODAY'S FOCUS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(day.sessionType.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                sourceLabel(source, coachName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${plan.name} · ${plan.splitType.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${exercises.size} ${if (exercises.size == 1) "exercise" else "exercises"} · Scheduled",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onViewDetails(plan.id, day.dayOfWeek) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View workout details")
                }
                OutlinedButton(
                    onClick = { onStartWorkout(plan.id, day.id, date, assignmentId) },
                    enabled = exercises.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start workout")
                }
            }
        }
    }
    if (exercises.isEmpty()) {
        MessageCard(
            title = "No exercises assigned",
            body = "This workout is scheduled, but it does not have any exercises yet.",
            action = null,
            onAction = {}
        )
    } else {
        Text("Exercises", style = MaterialTheme.typography.titleLarge)
        exercises.forEach { exercise ->
            ExerciseCard(exercise)
        }
    }
}

private fun sourceLabel(source: WorkoutSource, coachName: String?): String =
    when (source) {
        WorkoutSource.COACH -> coachName?.let { "Assigned by Coach $it" } ?: "Assigned by your Coach"
        WorkoutSource.PERSONAL -> "From your personal plan"
        WorkoutSource.NONE -> "No workout source"
    }

@Composable
private fun WeeklyWorkoutScheduleSection(
    plan: UserWorkoutPlan,
    today: LocalDate,
    onDayClick: (WorkoutDayOfWeek) -> Unit
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    val weekDates = datesForWeek(today, weekOffset.toLong())
    val weekStart = weekDates.first()
    val weekEnd = weekStart.plusDays(6)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val weekRange = if (weekStart.year == weekEnd.year) {
        "${weekStart.format(dateFormatter)} – ${weekEnd.format(dateFormatter)}"
    } else {
        val fullDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        "${weekStart.format(fullDateFormatter)} – ${weekEnd.format(fullDateFormatter)}"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("This Week", style = MaterialTheme.typography.titleLarge)
            Text("Your weekly training schedule", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { weekOffset-- }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous week")
            }
            Text(weekRange, style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = { weekOffset++ }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next week")
            }
        }

        weekDates.forEach { date ->
            val weekday = WorkoutDayOfWeek.fromOrder(date.dayOfWeek.value)
            val day = plan.days.firstOrNull { it.dayOfWeek == weekday }
            WeeklyWorkoutDayCard(
                date = date,
                day = day,
                isToday = date == today,
                onClick = { onDayClick(weekday) }
            )
        }
    }
}

@Composable
private fun WeeklyScheduleEmptySection(today: LocalDate, message: String) {
    val weekStart = datesForWeek(today).first()
    val weekEnd = weekStart.plusDays(6)
    val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val range = if (weekStart.year == weekEnd.year) {
        "${weekStart.format(shortDateFormatter)} – ${weekEnd.format(shortDateFormatter)}"
    } else {
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        "${weekStart.format(formatter)} – ${weekEnd.format(formatter)}"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("This Week", style = MaterialTheme.typography.titleLarge)
            Text("Your weekly training schedule", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(range, style = MaterialTheme.typography.titleSmall)
        MessageCard(
            title = "Weekly schedule not shown",
            body = message,
            action = null,
            onAction = {}
        )
    }
}

@Composable
private fun WeeklyWorkoutDayCard(
    date: LocalDate,
    day: UserWorkoutPlanDay?,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val dayLabel = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(Locale.getDefault())
    val title = when {
        day == null -> "Nothing scheduled"
        day.sessionType.isRest -> "Rest Day"
        else -> day.sessionType.displayName
    }
    val subtitle = when {
        day == null -> "Not configured in this plan"
        day.sessionType.isRest -> "Recovery day"
        else -> "${day.exercises.size} ${if (day.exercises.size == 1) "exercise" else "exercises"}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isToday) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                ) {
                    Text(
                        "TODAY",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: UserWorkoutPlanExercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(exercise.exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(exercise.prescription, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Rest: ${exercise.restSeconds} seconds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                exercise.notes?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String,
    action: String?,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}
