package com.calisthenics.ai.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import androidx.compose.material.icons.filled.Refresh
import com.calisthenics.ai.ui.profile.ProfileMediaGallery
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.EndRelationshipDialog
import com.calisthenics.ai.ui.components.GenderProfileCard
import com.calisthenics.ai.ui.components.InfoRow
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.components.UserAvatar
import com.calisthenics.ai.ui.report.ReportUserSheet
import com.calisthenics.ai.ui.review.CoachReviewsSection
import com.calisthenics.ai.ui.review.RatingChip
import com.calisthenics.ai.ui.review.formatRating
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Warning

private val tabStringIds = listOf(
    R.string.loc_tab_discover,
    R.string.loc_tab_my_coaches,
    R.string.loc_tab_pr_feedback,
    R.string.loc_tab_sent,
    R.string.loc_tab_incoming,
    R.string.loc_tab_my_clients
)

@Composable
fun CoachConnectionsScreen(
    onCoachClick: (Long) -> Unit,
    onChatClick: (Long, Long, String, String?) -> Unit,
    onClientClick: (Long) -> Unit = onCoachClick,
    onSharedPrClick: (com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto) -> Unit = {},
    viewModel: CoachConnectionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var assignmentSheetOpen by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumedBefore by remember { androidx.compose.runtime.mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (resumedBefore) viewModel.refresh() else resumedBefore = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.loc_coaching),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    stringResource(R.string.loc_connect_coaches_clients),
                    color = SecondaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(
                onClick = viewModel::refreshAll,
                enabled = !state.isLoading && !state.isRefreshing
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.loc_reload_all),
                        tint = Accent
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabStringIds.forEachIndexed { index, titleResId ->
                FilterChip(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    label = { Text(stringResource(titleResId)) }
                )
            }
        }

        if (state.isRefreshing || state.refreshingSections.isNotEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent)
        }

        state.feedback?.let { message ->
            Text(
                text = message,
                color = if (message.contains("sent") || message.contains("accepted") ||
                    message.contains("declined") || message.contains("canceled")
                ) Accent else MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearFeedback() }
                    .padding(vertical = 8.dp)
            )
        }

        when {
            state.isLoading && !state.hasLoaded -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
            state.error != null && state.currentUserId == null -> {
                EmptyState(
                    title = stringResource(R.string.connections_unavailable),
                    body = state.error ?: stringResource(R.string.loc_could_not_load_connections),
                    action = stringResource(R.string.common_retry),
                    onAction = viewModel::refresh
                )
            }
            else -> {
                if (state.error != null) {
                    TextButton(onClick = viewModel::refresh) {
                        Text("${state.error}  ${stringResource(R.string.common_retry)}", color = MaterialTheme.colorScheme.error)
                    }
                }
                when (selectedTab) {
                    0 -> DiscoverContent(
                        coaches = state.coaches.filter {
                            it.fullName.contains(search.trim(), ignoreCase = true) ||
                                (it.experienceLevel?.contains(search.trim(), ignoreCase = true) == true)
                        },
                        query = search,
                        onQueryChange = { search = it },
                        currentUserId = state.currentUserId,
                        relationships = state.relationships,
                        requestingCoachIds = state.requestingCoachIds,
                        isRefreshing = state.isSectionRefreshing(CoachSection.DISCOVER),
                        onReload = viewModel::refreshDiscover,
                        onCoachClick = onCoachClick,
                        onRequest = viewModel::requestConnection
                    )
                    1 -> ActiveConnectionsContent(
                        relationships = state.myCoaches,
                        isCoach = true,
                        busyChatUserIds = state.busyChatUserIds,
                        busyRelationshipIds = state.busyRelationshipIds,
                        isRefreshing = state.isSectionRefreshing(CoachSection.MY_COACHES),
                        onReload = viewModel::refreshMyCoaches,
                        onProfile = onCoachClick,
                        onDiscover = { selectedTab = 0 },
                        onChat = { id, name, avatar ->
                            viewModel.startChat(id) { conversationId ->
                                onChatClick(conversationId, id, name, avatar)
                            }
                        },
                        onEndRelationship = viewModel::terminateRelationship
                    )
                    2 -> CoachFeedbacksContent(
                        feedbacks = state.clientFeedbacks,
                        currentUserId = state.currentUserId,
                        isRefreshing = state.isSectionRefreshing(CoachSection.FEEDBACK),
                        error = state.feedbackSectionError,
                        submittingIds = state.feedbackSubmittingIds,
                        onReload = viewModel::refreshFeedbacks,
                        onMarkAsRead = viewModel::markPrShareAsRead,
                        onSubmitFeedback = viewModel::submitCoachPrFeedback,
                        onPrCardClick = onSharedPrClick
                    )
                    3 -> PendingConnectionsContent(
                        relationships = state.sentRequests,
                        currentUserId = state.currentUserId,
                        isIncoming = false,
                        busyIds = state.busyRelationshipIds,
                        isRefreshing = state.isSectionRefreshing(CoachSection.SENT),
                        onReload = viewModel::refreshSentRequests,
                        onProfile = onCoachClick,
                        onAccept = viewModel::acceptRequest,
                        onReject = viewModel::rejectRequest,
                        onCancel = viewModel::cancelRequest
                    )
                    4 -> PendingConnectionsContent(
                        relationships = state.incomingRequests,
                        currentUserId = state.currentUserId,
                        isIncoming = true,
                        busyIds = state.busyRelationshipIds,
                        isRefreshing = state.isSectionRefreshing(CoachSection.INCOMING),
                        onReload = viewModel::refreshIncomingRequests,
                        onProfile = onCoachClick,
                        onAccept = viewModel::acceptRequest,
                        onReject = viewModel::rejectRequest,
                        onCancel = viewModel::cancelRequest
                    )
                    else -> ActiveConnectionsContent(
                        relationships = state.myClients,
                        isCoach = false,
                        busyChatUserIds = state.busyChatUserIds,
                        busyRelationshipIds = state.busyRelationshipIds,
                        isRefreshing = state.isSectionRefreshing(CoachSection.MY_CLIENTS),
                        onReload = viewModel::refreshMyClients,
                        onProfile = onClientClick,
                        onChat = { id, name, avatar ->
                            viewModel.startChat(id) { conversationId ->
                                onChatClick(conversationId, id, name, avatar)
                            }
                        },
                        onEndRelationship = viewModel::terminateRelationship
                    )
                }
            }
        }
    }

}

@Composable
private fun DiscoverContent(
    coaches: List<CoachProfile>,
    query: String,
    onQueryChange: (String) -> Unit,
    currentUserId: Long?,
    relationships: List<CoachClientRelationship>,
    requestingCoachIds: Set<Long>,
    isRefreshing: Boolean = false,
    onReload: () -> Unit,
    onCoachClick: (Long) -> Unit,
    onRequest: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.loc_discover_coaches, coaches.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            IconButton(
                onClick = onReload,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.loc_reload_coaches),
                        tint = SecondaryText
                    )
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            label = { Text(stringResource(R.string.search_coaches)) },
            singleLine = true
        )
        if (coaches.isEmpty()) {
            EmptyState(
                title = if (query.isBlank()) stringResource(R.string.loc_no_coaches_found) else stringResource(R.string.loc_no_matching_coaches),
                body = if (query.isBlank()) stringResource(R.string.loc_no_profiles_available) else stringResource(R.string.loc_try_another_search)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = FloatingBottomBarDefaults.ContentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(coaches, key = { it.id }) { coach ->
                    val relation = relationships
                        .filter { it.coachId == coach.id && it.clientId == currentUserId }
                        .maxByOrNull { it.createdAt.orEmpty() }
                    val status = when (relation?.status) {
                        "PENDING" -> stringResource(R.string.loc_request_pending)
                        "ACTIVE" -> stringResource(R.string.common_connected)
                        "REJECTED" -> stringResource(R.string.loc_previously_declined)
                        "CANCELLED" -> stringResource(R.string.loc_request_canceled)
                        "TERMINATED" -> stringResource(R.string.loc_relationship_ended)
                        else -> stringResource(R.string.loc_not_connected)
                    }
                    val isSubmitting = coach.id in requestingCoachIds
                    val canRequest = currentUserId != coach.id &&
                        relation?.status !in setOf("PENDING", "ACTIVE") &&
                        !isSubmitting
                    val level = coach.experienceLevel?.takeIf(String::isNotBlank)?.replace('_', ' ')
                        ?.lowercase()?.replaceFirstChar(Char::uppercase)
                    val ratingSummary = if ((coach.reviewCount ?: 0L) > 0L) {
                        stringResource(
                            R.string.coach_rating_badge,
                            formatRating(coach.averageRating),
                            coach.reviewCount ?: 0L
                        )
                    } else null
                    PersonCard(
                        name = coach.fullName,
                        avatarUrl = coach.avatarUrl,
                        detail = listOfNotNull(level, ratingSummary).joinToString(" • ").ifBlank { null },
                        status = status,
                        onClick = { onCoachClick(coach.id) },
                        action = {
                            when {
                                isSubmitting -> CircularProgressIndicator(
                                    Modifier.size(22.dp),
                                    color = Accent,
                                    strokeWidth = 2.dp
                                )
                                relation?.status == "PENDING" -> OutlinedButton(
                                    onClick = {},
                                    enabled = false
                                ) { Text(stringResource(R.string.common_pending)) }
                                relation?.status == "ACTIVE" -> OutlinedButton(
                                    onClick = {},
                                    enabled = false
                                ) { Text(stringResource(R.string.common_connected)) }
                                else -> Button(
                                    onClick = { onRequest(coach.id) },
                                    enabled = canRequest,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Accent,
                                        contentColor = Ink
                                    )
                                ) { Text(stringResource(R.string.common_connect)) }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveConnectionsContent(
    relationships: List<CoachClientRelationship>,
    isCoach: Boolean,
    busyChatUserIds: Set<Long>,
    busyRelationshipIds: Set<Long> = emptySet(),
    isRefreshing: Boolean = false,
    onReload: () -> Unit,
    onProfile: (Long) -> Unit,
    onDiscover: (() -> Unit)? = null,
    onChat: (Long, String, String?) -> Unit,
    onEndRelationship: (Long) -> Unit = {}
) {
    var confirmingRelationshipId by remember { mutableStateOf<Long?>(null) }
    var reportingPerson by remember { mutableStateOf<RelationshipPerson?>(null) }

    reportingPerson?.let { target ->
        ReportUserSheet(
            userId = target.id,
            userName = target.name,
            onDismiss = { reportingPerson = null }
        )
    }

    EndRelationshipDialog(
        isOpen = confirmingRelationshipId != null,
        isLoading = confirmingRelationshipId != null && confirmingRelationshipId in busyRelationshipIds,
        onConfirm = {
            confirmingRelationshipId?.let { id ->
                onEndRelationship(id)
                confirmingRelationshipId = null
            }
        },
        onDismiss = { confirmingRelationshipId = null }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCoach) stringResource(R.string.loc_my_coaches, relationships.size) else stringResource(R.string.loc_my_clients, relationships.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onReload,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isCoach) stringResource(R.string.loc_reload_my_coaches) else stringResource(R.string.loc_reload_my_clients),
                            tint = SecondaryText
                        )
                    }
                }
            }
        }

        if (relationships.isEmpty()) {
            EmptyState(
                title = if (isCoach) stringResource(R.string.loc_no_coaches_yet) else stringResource(R.string.loc_no_clients_yet),
                body = if (isCoach) stringResource(R.string.loc_discover_coaches_hint)
                else stringResource(R.string.loc_accepted_connections),
                action = if (isCoach && onDiscover != null) stringResource(R.string.loc_discover_coaches) else null,
                onAction = { onDiscover?.invoke() }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = FloatingBottomBarDefaults.ContentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(relationships, key = { it.id }) { relationship ->
                    val person = relationship.person(isCoach)
                    PersonCard(
                        name = person.name,
                        avatarUrl = person.avatar,
                        detail = person.experience,
                        status = stringResource(R.string.common_connected),
                        onClick = { onProfile(person.id) },
                        action = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { onChat(person.id, person.name, person.avatar) },
                                    enabled = person.id !in busyChatUserIds,
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                ) {
                                    if (person.id in busyChatUserIds) {
                                        CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                                    } else {
                                        Text(stringResource(R.string.common_message))
                                    }
                                }
                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        enabled = relationship.id !in busyRelationshipIds
                                    ) {
                                        if (relationship.id in busyRelationshipIds) {
                                            CircularProgressIndicator(Modifier.size(16.dp), color = SecondaryText, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), tint = SecondaryText)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(Elevated)
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.report_user),
                                                    color = PrimaryText
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                reportingPerson = person
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.end_relationship),
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                confirmingRelationshipId = relationship.id
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentBottomSheet(
    relationships: List<CoachClientRelationship>,
    plans: List<UserWorkoutPlan>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onAssign: (Long, Long, () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var athleteQuery by rememberSaveable { mutableStateOf("") }
    var selectedRelationshipId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedPlanId by rememberSaveable { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filteredRelationships = relationships.filter {
        it.clientName.contains(athleteQuery.trim(), ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.assign_workout),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.assign_workout_subtitle),
                color = SecondaryText,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Accent)
                }
            } else {
                Text(
                    text = stringResource(R.string.select_athlete),
                    style = MaterialTheme.typography.labelLarge,
                    color = SecondaryText,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = athleteQuery,
                    onValueChange = { athleteQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_athletes)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                if (filteredRelationships.isEmpty()) {
                    Text(
                        text = stringResource(
                            if (relationships.isEmpty()) R.string.no_connected_athletes
                            else R.string.no_matching_athletes
                        ),
                        color = SecondaryText,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredRelationships, key = { it.id }) { relationship ->
                            val selected = relationship.id == selectedRelationshipId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedRelationshipId = relationship.id },
                                color = if (selected) Accent.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        url = relationship.clientAvatarUrl,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            relationship.clientName,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        relationship.clientExperienceLevel
                                            ?.takeIf(String::isNotBlank)
                                            ?.let { Text(it, color = SecondaryText, fontSize = 12.sp) }
                                    }
                                    RadioButton(
                                        selected = selected,
                                        onClick = { selectedRelationshipId = relationship.id }
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.select_workout_plan),
                    style = MaterialTheme.typography.labelLarge,
                    color = SecondaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (plans.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_available_workout_plans),
                        color = SecondaryText,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(plans, key = { it.id }) { plan ->
                            PlanSelectionRow(
                                plan = plan,
                                selected = plan.id == selectedPlanId,
                                onClick = { selectedPlanId = plan.id }
                            )
                        }
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp)
                )
                TextButton(onClick = onRetry, enabled = !isSubmitting) {
                    Text(stringResource(R.string.common_retry))
                }
            }
            Button(
                onClick = {
                    val relationshipId = selectedRelationshipId
                    val planId = selectedPlanId
                    if (relationshipId != null && planId != null) {
                        onAssign(relationshipId, planId, onDismiss)
                    }
                },
                enabled = selectedRelationshipId != null && selectedPlanId != null && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Ink,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.assign_workout))
                }
            }
        }
    }
}

@Composable
private fun PlanSelectionRow(
    plan: UserWorkoutPlan,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (selected) Accent.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = stringResource(R.string.plan_assignment_summary, plan.trainingDayCount, plan.splitType.displayName),
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun PendingConnectionsContent(
    relationships: List<CoachClientRelationship>,
    currentUserId: Long?,
    isIncoming: Boolean,
    busyIds: Set<Long>,
    isRefreshing: Boolean = false,
    onReload: () -> Unit,
    onProfile: (Long) -> Unit,
    onAccept: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onCancel: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isIncoming) stringResource(R.string.loc_incoming_requests, relationships.size) else stringResource(R.string.loc_sent_requests, relationships.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            IconButton(
                onClick = onReload,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isIncoming) stringResource(R.string.loc_reload_incoming) else stringResource(R.string.loc_reload_sent),
                        tint = SecondaryText
                    )
                }
            }
        }

        if (relationships.isEmpty()) {
            EmptyState(
                title = if (isIncoming) stringResource(R.string.loc_no_incoming_requests) else stringResource(R.string.loc_no_sent_requests),
                body = if (isIncoming) stringResource(R.string.loc_new_connection_requests)
                else stringResource(R.string.loc_sent_requests_description)
            )
        } else {
            var reportingPerson by remember { mutableStateOf<RelationshipPerson?>(null) }
            reportingPerson?.let { target ->
                ReportUserSheet(
                    userId = target.id,
                    userName = target.name,
                    onDismiss = { reportingPerson = null }
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = FloatingBottomBarDefaults.ContentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(relationships, key = { it.id }) { relationship ->
                    val requesterId = relationship.requestedBy
                    val person = if (requesterId == relationship.coachId) {
                        relationship.person(isCoach = true)
                    } else {
                        relationship.person(isCoach = false)
                    }
                    val otherParty = if (currentUserId == relationship.coachId) {
                        relationship.person(isCoach = false)
                    } else {
                        relationship.person(isCoach = true)
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    val isBusy = relationship.id in busyIds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Elevated)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PersonHeader(
                                name = person.name,
                                avatarUrl = person.avatar,
                                detail = person.experience,
                                status = stringResource(R.string.loc_requested_recently, relationship.createdAt?.take(10) ?: stringResource(R.string.loc_recently)),
                                modifier = Modifier.clickable { onProfile(person.id) }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isIncoming) {
                                    Button(
                                        onClick = { onAccept(relationship.id) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Accent,
                                            contentColor = Ink
                                        )
                                    ) {
                                        if (isBusy) CircularProgressIndicator(
                                            Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp
                                        ) else Text(stringResource(R.string.common_accept))
                                    }
                                    OutlinedButton(
                                        onClick = { onReject(relationship.id) },
                                        enabled = !isBusy
                                    ) { Text(stringResource(R.string.common_reject)) }
                                } else {
                                    OutlinedButton(
                                        onClick = { onCancel(relationship.id) },
                                        enabled = !isBusy
                                    ) {
                                        if (isBusy) CircularProgressIndicator(
                                            Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp
                                        ) else Text(stringResource(R.string.cancel_request))
                                    }
                                }
                                if (currentUserId != null && otherParty.id != currentUserId) {
                                    Spacer(Modifier.weight(1f))
                                    Box {
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.more_options),
                                                tint = SecondaryText
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false },
                                            modifier = Modifier.background(Elevated)
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.report_user),
                                                        color = PrimaryText
                                                    )
                                                },
                                                onClick = {
                                                    menuExpanded = false
                                                    reportingPerson = otherParty
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachProfileScreen(
    coachId: Long,
    onBack: () -> Unit,
    onChatClick: (Long, Long, String, String?) -> Unit,
    viewModel: CoachConnectionsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(coachId) { viewModel.loadCoachProfile(coachId) }

    var showEndDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    val currentUserId = state.currentUserId
    val reportName = state.selectedProfile
        ?.takeIf { state.selectedProfileId == coachId }
        ?.let { it.displayName.ifBlank { it.fullName } }
        ?: stringResource(R.string.loc_coach_profile)
    if (showReportSheet && currentUserId != null && currentUserId != coachId) {
        ReportUserSheet(
            userId = coachId,
            userName = reportName,
            onDismiss = { showReportSheet = false }
        )
    }
    val relation = state.relationships
        .filter {
            (it.coachId == coachId && it.clientId == currentUserId) ||
                (it.clientId == coachId && it.coachId == currentUserId)
        }
        .maxByOrNull { it.createdAt.orEmpty() }

    EndRelationshipDialog(
        isOpen = showEndDialog,
        isLoading = relation?.id != null && relation.id in state.busyRelationshipIds,
        onConfirm = {
            relation?.let { rel ->
                viewModel.terminateRelationship(rel.id) { success ->
                    if (success) {
                        showEndDialog = false
                    }
                }
            }
        },
        onDismiss = { showEndDialog = false }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = PrimaryText)
            }
            Text(
                stringResource(R.string.loc_coach_profile),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { viewModel.loadCoachProfile(coachId) },
                enabled = !state.isProfileLoading
            ) {
                if (state.isProfileLoading && state.selectedProfileId == coachId) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.loc_reload_profile),
                        tint = Accent
                    )
                }
            }
            if (currentUserId != null && currentUserId != coachId) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), tint = SecondaryText)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Elevated)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.report_user),
                                    color = PrimaryText
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showReportSheet = true
                            }
                        )
                        if (relation?.status == "ACTIVE") {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.end_relationship),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                enabled = relation.id !in state.busyRelationshipIds,
                                onClick = {
                                    menuExpanded = false
                                    showEndDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        when {
            state.isProfileLoading && state.selectedProfileId == coachId && state.selectedProfile == null -> {
                Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
            state.profileError != null && state.selectedProfileId == coachId && state.selectedProfile == null -> {
                EmptyState(
                    title = stringResource(R.string.loc_profile_unavailable),
                    body = state.profileError ?: stringResource(R.string.loc_could_not_load_profile),
                    action = stringResource(R.string.common_retry),
                    onAction = { viewModel.loadCoachProfile(coachId) }
                )
            }
            state.selectedProfileId == coachId && state.selectedProfile != null -> {
                val coach = state.selectedProfile!!
                val currentUserId = state.currentUserId
                val relation = state.relationships
                    .filter {
                        (it.coachId == coach.id && it.clientId == currentUserId) ||
                            (it.clientId == coach.id && it.coachId == currentUserId)
                    }
                    .maxByOrNull { it.createdAt.orEmpty() }
                val submitting = coach.id in state.requestingCoachIds
                val responseRelationship = relation?.takeIf {
                    it.status == "PENDING" && it.requestedBy != currentUserId
                }

                // Profile Header Card
                DarkCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    UserAvatar(coach.avatarUrl, modifier = Modifier.size(100.dp))
}

                        Text(
                            text = coach.displayName.ifBlank { coach.fullName },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )

                        // Badges Row
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if ((coach.reviewCount ?: 0L) > 0L) {
                                RatingChip(averageRating = coach.averageRating, reviewCount = coach.reviewCount)
                            }
                            if (coach.isVerified) {
                                PillBadge(stringResource(R.string.loc_verified_coach), isAccent = true)
                            }
                            if (coach.isAcceptingClients) {
                                PillBadge(stringResource(R.string.loc_accepting_clients), isAccent = true)
                            } else {
                                PillBadge(stringResource(R.string.loc_not_accepting_clients))
                            }
                            coach.experienceLevel?.takeIf(String::isNotBlank)?.let {
                                PillBadge(it.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase))
                            }
                        }

                        coach.bio?.takeIf(String::isNotBlank)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        coach.yearsExperience?.let { years ->
                            Text(
                                text = if (years == 1) stringResource(R.string.loc_year_coaching_experience, years) else stringResource(R.string.loc_years_coaching_experience, years),
                                style = MaterialTheme.typography.labelMedium,
                                color = Accent
                            )
                        }
                    }
                }

                // Gender Row
                GenderProfileCard(gender = coach.gender)

                // Coach Media Gallery (view-only for visitors)
                ProfileMediaGallery(
                    mediaList = state.coachMedia,
                    isOwner = coach.userId == currentUserId,
                    isLoading = state.isCoachMediaLoading
                )

                // Specialties Section
                coach.specialties?.takeIf(String::isNotBlank)?.let { specs ->
                    val specList = specs.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (specList.isNotEmpty()) {
                        DarkCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    stringResource(R.string.loc_specialties),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    specList.forEach { specialty ->
                                        PillBadge(specialty, isAccent = true)
                                    }
                                }
                            }
                        }
                    }
                }

                // Real Coaching Statistics Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.loc_coaching_statistics),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = stringResource(R.string.loc_active_clients),
                            value = "${coach.activeClientsCount}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.loc_completed),
                            value = "${coach.completedClientsCount}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.loc_plans_assigned),
                            value = "${coach.assignedPlansCount}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Coaching Details Card
                val hasPhilosophy = !coach.trainingPhilosophy.isNullOrBlank()
                val hasDescription = !coach.coachingDescription.isNullOrBlank()
                val hasLanguages = !coach.languages.isNullOrBlank()
                val hasCerts = !coach.certifications.isNullOrBlank()
                val hasSocial = !coach.socialLinks.isNullOrBlank()

                if (hasPhilosophy || hasDescription || hasLanguages || hasCerts || hasSocial) {
                    DarkCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                stringResource(R.string.loc_about_coaching),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                            if (hasPhilosophy) InfoRow(stringResource(R.string.loc_training_philosophy), coach.trainingPhilosophy)
                            if (hasDescription) InfoRow(stringResource(R.string.loc_coaching_overview), coach.coachingDescription)
                            if (hasLanguages) InfoRow(stringResource(R.string.loc_languages), coach.languages)
                            if (hasCerts) InfoRow(stringResource(R.string.loc_certifications), coach.certifications)
                            if (hasSocial) InfoRow(stringResource(R.string.loc_social_portfolio), coach.socialLinks)
                        }
                    }
                }

                // Connection Action Card
                DarkCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.loc_coaching_connection),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )

                        val statusText = when {
                            currentUserId == coach.id -> stringResource(R.string.loc_public_coach_profile)
                            relation?.status == "ACTIVE" -> stringResource(R.string.loc_active_connected_coach)
                            relation?.status == "PENDING" && relation.requestedBy == currentUserId ->
                                stringResource(R.string.loc_request_sent_pending)
                            relation?.status == "PENDING" && relation.requestedBy != currentUserId ->
                                stringResource(R.string.loc_coach_sent_request)
                            relation?.status == "REJECTED" ->
                                stringResource(R.string.loc_previous_request_declined)
                            relation?.status == "CANCELLED" ->
                                stringResource(R.string.loc_previous_request_canceled)
                            relation?.status == "TERMINATED" ->
                                stringResource(R.string.loc_relationship_ended_request)
                            !coach.isAcceptingClients ->
                                stringResource(R.string.loc_coach_not_accepting)
                            else ->
                                stringResource(R.string.loc_connect_coach_guidance)
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (relation?.status == "ACTIVE") Accent else SecondaryText
                        )

                        when {
                            currentUserId == coach.id -> Unit
                            relation?.status == "ACTIVE" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.startChat(coach.id) { conversationId ->
                                                onChatClick(conversationId, coach.id, coach.displayName.ifBlank { coach.fullName }, coach.avatarUrl)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                    ) {
                                        Text(stringResource(R.string.loc_message_coach))
                                    }
                                    TextButton(
                                        onClick = { showEndDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = relation.id !in state.busyRelationshipIds
                                    ) {
                                        Text(stringResource(R.string.end_relationship), color = SecondaryText)
                                    }
                                }
                            }
                            responseRelationship != null -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.acceptRequest(responseRelationship.id) },
                                        enabled = responseRelationship.id !in state.busyRelationshipIds,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                    ) {
                                        Text(stringResource(R.string.common_accept))
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.rejectRequest(responseRelationship.id) },
                                        enabled = responseRelationship.id !in state.busyRelationshipIds,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.common_reject))
                                    }
                                }
                            }
                            relation?.status == "PENDING" && relation.requestedBy == currentUserId -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.common_pending))
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.cancelRequest(relation.id) },
                                        enabled = relation.id !in state.busyRelationshipIds,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.loc_cancel_request))
                                    }
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { viewModel.requestConnection(coach.id) },
                                    enabled = !submitting && currentUserId != null && coach.isAcceptingClients,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                ) {
                                    if (submitting) {
                                        CircularProgressIndicator(Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp)
                                    } else {
                                        Text(if (coach.isAcceptingClients) stringResource(R.string.loc_connect_with_coach) else stringResource(R.string.loc_not_accepting_clients))
                                    }
                                }
                            }
                        }
                    }
                }

                CoachReviewsSection(
                    coachId = coach.id,
                    isOwnProfile = currentUserId == coach.id
                )

                state.feedback?.let {
                    Text(it, color = Accent, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PersonCard(
    name: String,
    avatarUrl: String?,
    detail: String?,
    status: String,
    onClick: () -> Unit,
    action: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PersonHeaderContent(name, avatarUrl, detail, status, Modifier.weight(1f))
            action()
        }
    }
}

@Composable
private fun PersonHeader(
    name: String,
    avatarUrl: String?,
    detail: String?,
    status: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PersonHeaderContent(name, avatarUrl, detail, status, Modifier.weight(1f))
    }
}

@Composable
private fun PersonHeaderContent(
    name: String,
    avatarUrl: String?,
    detail: String?,
    status: String,
    modifier: Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CoachAvatar(avatarUrl)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                name,
                color = PrimaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            detail?.takeIf(String::isNotBlank)?.let {
                Text(it, color = SecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Text(status, color = Accent, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CoachAvatar(url: String?, modifier: Modifier = Modifier.size(54.dp)) {
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier.clip(CircleShape).background(Ink),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.Person,
                contentDescription = stringResource(R.string.profile_avatar_content_description),
                tint = SecondaryText,
                modifier = Modifier.size(30.dp)
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = stringResource(R.string.profile_avatar_content_description),
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

private data class RelationshipPerson(
    val id: Long,
    val name: String,
    val avatar: String?,
    val experience: String?
)

private fun CoachClientRelationship.person(isCoach: Boolean) = if (isCoach) {
    RelationshipPerson(coachId, coachName, coachAvatarUrl, coachExperienceLevel)
} else {
    RelationshipPerson(clientId, clientName, clientAvatarUrl, clientExperienceLevel)
}

@Composable
private fun CoachFeedbacksContent(
    feedbacks: List<com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto>,
    currentUserId: Long?,
    isRefreshing: Boolean,
    error: String?,
    submittingIds: Set<Long>,
    onReload: () -> Unit,
    onMarkAsRead: (Long) -> Unit,
    onSubmitFeedback: (Long, String, Int?) -> Unit,
    onPrCardClick: (com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto) -> Unit
) {
    if (error != null && feedbacks.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.connections_unavailable),
            body = error,
            action = stringResource(R.string.common_retry),
            onAction = onReload
        )
        return
    }

    if (feedbacks.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.loc_no_coach_feedback),
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.loc_coach_feedback_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onReload, enabled = !isRefreshing) {
                Text(stringResource(R.string.common_refresh))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = FloatingBottomBarDefaults.ContentBottomPadding
            )
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.loc_coach_feedback_count, feedbacks.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    IconButton(onClick = onReload, enabled = !isRefreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh), tint = Accent)
                    }
                }
            }

            if (error != null) {
                item {
                    TextButton(onClick = onReload) {
                        Text("$error  ${stringResource(R.string.common_retry)}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            items(feedbacks, key = { it.id }) { item ->
                val isCoachViewer = currentUserId != null && item.coachUserId == currentUserId
                var feedbackInput by remember(item.id, item.coachFeedback) {
                    mutableStateOf(item.coachFeedback ?: "")
                }
                var selectedRating by remember(item.id, item.rating) {
                    mutableIntStateOf(item.rating ?: 5)
                }
                val isSubmitting = item.id in submittingIds

                LaunchedEffect(item.id, item.isRead, isCoachViewer) {
                    if (isCoachViewer && !item.isRead) {
                        onMarkAsRead(item.id)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPrCardClick(item) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Elevated)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top header: Client or Coach info + Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                CoachAvatar(
                                    url = if (isCoachViewer) item.clientAvatarUrl else null,
                                    modifier = Modifier.size(36.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isCoachViewer) {
                                            item.clientName.ifEmpty { "Athlete" }
                                        } else {
                                            stringResource(R.string.loc_coach_named, item.coachName.ifEmpty { stringResource(R.string.loc_coach) })
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryText
                                    )
                                    if (item.sharedAt.isNotBlank()) {
                                        Text(
                                            text = stringResource(R.string.loc_shared_date, item.sharedAt.take(16).replace('T', ' ')),
                                            fontSize = 11.sp,
                                            color = SecondaryText
                                        )
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCoachViewer && !item.isRead) {
                                    PillBadge(stringResource(R.string.loc_new_badge), isAccent = true)
                                }
                                PillBadge(
                                    item.personalRecord.recordType.replace("MAX_", "").replace('_', ' ').ifEmpty { "PR" },
                                    isAccent = true
                                )
                            }
                        }

                        // Exercise & Value
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.personalRecord.exerciseName.ifEmpty { stringResource(R.string.loc_exercise_pr_fallback) },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                if (item.personalRecord.achievedAt.isNotBlank()) {
                                    Text(
                                        stringResource(R.string.loc_achieved_date, item.personalRecord.achievedAt.take(10)),
                                        fontSize = 12.sp,
                                        color = SecondaryText
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    item.personalRecord.displayFormattedValue,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Accent
                                )
                                item.personalRecord.improvementPercentage?.let { pct ->
                                    if (pct > 0) {
                                        Text(
                                            text = stringResource(R.string.loc_improvement_percent, pct.toString()),
                                            fontSize = 11.sp,
                                            color = Accent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Previous record if available
                        val prevFormatted = item.personalRecord.formattedPrevious
                            ?: item.personalRecord.previousRecordValue?.let { prev ->
                                val num = if (prev % 1.0 == 0.0) "${prev.toLong()}" else "$prev"
                                val u = item.personalRecord.unit ?: ""
                                if (u.isNotBlank()) "$num $u" else num
                            }
                        if (!prevFormatted.isNullOrBlank()) {
                            Text(
                                text = "Previous: $prevFormatted",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }

                        item.personalRecord.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                            Text(
                                text = notes,
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }

                        if (isCoachViewer) {
                            HorizontalDivider(color = Border.copy(alpha = 0.6f))
                            // Coach inline rating & feedback editor
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.loc_performance_rating),
                                    fontSize = 12.sp,
                                    color = SecondaryText
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    (1..5).forEach { star ->
                                        val isSelected = star <= selectedRating
                                        Text(
                                            text = if (isSelected) "★" else "☆",
                                            color = if (isSelected) Warning else SecondaryText,
                                            fontSize = 18.sp,
                                            modifier = Modifier.clickable { selectedRating = star }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = feedbackInput,
                                onValueChange = { feedbackInput = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.loc_leave_coach_feedback),
                                        fontSize = 12.sp,
                                        color = SecondaryText
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!item.feedbackAt.isNullOrBlank()) {
                                    Text(
                                        stringResource(R.string.loc_reviewed_on_date, item.feedbackAt.take(16).replace('T', ' ')),
                                        fontSize = 11.sp,
                                        color = SecondaryText
                                    )
                                } else {
                                    Spacer(Modifier.width(1.dp))
                                }
                                Button(
                                    onClick = { onSubmitFeedback(item.id, feedbackInput, selectedRating) },
                                    enabled = !isSubmitting && feedbackInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Ink,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = if (!item.coachFeedback.isNullOrBlank()) {
                                                stringResource(R.string.loc_submit_or_update_feedback)
                                            } else {
                                                stringResource(R.string.loc_submit_feedback_button)
                                            },
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // Client view of shared PR & coach feedback
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (!item.coachFeedback.isNullOrBlank()) "Reviewed by Coach" else "Awaiting Coach Feedback",
                                    fontSize = 12.sp,
                                    color = if (!item.coachFeedback.isNullOrBlank()) Accent else SecondaryText,
                                    fontWeight = FontWeight.Medium
                                )
                                if (item.rating != null && item.rating > 0) {
                                    Text(
                                        "★".repeat(item.rating),
                                        fontSize = 14.sp,
                                        color = Warning
                                    )
                                }
                            }

                            if (!item.coachFeedback.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Ink, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        "\"${item.coachFeedback}\"",
                                        fontSize = 13.sp,
                                        color = PrimaryText
                                    )
                                }
                            }

                            if (!item.feedbackAt.isNullOrBlank()) {
                                Text(
                                    stringResource(R.string.loc_reviewed_on_date, item.feedbackAt.take(16).replace('T', ' ')),
                                    fontSize = 11.sp,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
