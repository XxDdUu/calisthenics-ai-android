package com.calisthenics.ai.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.InfoRow
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.components.UserAvatar
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

private val tabs = listOf("Discover", "My Coaches", "Sent", "Incoming", "My Clients")

@Composable
fun CoachConnectionsScreen(
    onCoachClick: (Long) -> Unit,
    onChatClick: (Long, Long, String, String?) -> Unit,
    onClientClick: (Long) -> Unit = onCoachClick,
    viewModel: CoachConnectionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
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
                    "Coaching",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    "Connect with coaches and clients",
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
                        contentDescription = "Reload all",
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
            tabs.forEachIndexed { index, title ->
                FilterChip(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    label = { Text(title) }
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
                    title = "Connections unavailable",
                    body = state.error ?: "Could not load connections",
                    action = "Retry",
                    onAction = viewModel::refresh
                )
            }
            else -> {
                if (state.error != null) {
                    TextButton(onClick = viewModel::refresh) {
                        Text("${state.error}  Retry", color = MaterialTheme.colorScheme.error)
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
                        isRefreshing = state.isSectionRefreshing(CoachSection.MY_COACHES),
                        onReload = viewModel::refreshMyCoaches,
                        onProfile = onCoachClick,
                        onDiscover = { selectedTab = 0 },
                        onChat = { id, name, avatar ->
                            viewModel.startChat(id) { conversationId ->
                                onChatClick(conversationId, id, name, avatar)
                            }
                        }
                    )
                    2 -> PendingConnectionsContent(
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
                    3 -> PendingConnectionsContent(
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
                        isRefreshing = state.isSectionRefreshing(CoachSection.MY_CLIENTS),
                        onReload = viewModel::refreshMyClients,
                        onProfile = onClientClick,
                        onChat = { id, name, avatar ->
                            viewModel.startChat(id) { conversationId ->
                                onChatClick(conversationId, id, name, avatar)
                            }
                        }
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
                text = "Discover Coaches (${coaches.size})",
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
                        contentDescription = "Reload coaches",
                        tint = SecondaryText
                    )
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            label = { Text("Search coaches") },
            singleLine = true
        )
        if (coaches.isEmpty()) {
            EmptyState(
                title = if (query.isBlank()) "No coaches found" else "No matching coaches",
                body = if (query.isBlank()) "There are no other user profiles available yet." else "Try another name or experience level."
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
                        "PENDING" -> "Request pending"
                        "ACTIVE" -> "Connected"
                        "REJECTED" -> "Previously declined"
                        "CANCELLED" -> "Request canceled"
                        else -> "Not connected"
                    }
                    val isSubmitting = coach.id in requestingCoachIds
                    val canRequest = currentUserId != coach.id &&
                        relation?.status !in setOf("PENDING", "ACTIVE") &&
                        !isSubmitting
                    PersonCard(
                        name = coach.fullName,
                        avatarUrl = coach.avatarUrl,
                        detail = coach.experienceLevel?.takeIf(String::isNotBlank)?.replace('_', ' ')
                            ?.lowercase()?.replaceFirstChar(Char::uppercase),
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
                                ) { Text("Pending") }
                                relation?.status == "ACTIVE" -> OutlinedButton(
                                    onClick = {},
                                    enabled = false
                                ) { Text("Connected") }
                                else -> Button(
                                    onClick = { onRequest(coach.id) },
                                    enabled = canRequest,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Accent,
                                        contentColor = Ink
                                    )
                                ) { Text("Connect") }
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
    isRefreshing: Boolean = false,
    onReload: () -> Unit,
    onProfile: (Long) -> Unit,
    onDiscover: (() -> Unit)? = null,
    onChat: (Long, String, String?) -> Unit
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
                text = if (isCoach) "My Coaches (${relationships.size})" else "My Clients (${relationships.size})",
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
                        contentDescription = if (isCoach) "Reload my coaches" else "Reload my clients",
                        tint = SecondaryText
                    )
                }
            }
        }

        if (relationships.isEmpty()) {
            EmptyState(
                title = if (isCoach) "No coaches yet" else "No clients yet",
                body = if (isCoach) "Discover coaches to start building your coaching team."
                else "Accepted client connections will appear here.",
                action = if (isCoach && onDiscover != null) "Discover coaches" else null,
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
                        status = "Connected",
                        onClick = { onProfile(person.id) },
                        action = {
                            Button(
                                onClick = { onChat(person.id, person.name, person.avatar) },
                                enabled = person.id !in busyChatUserIds,
                                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                            ) {
                                if (person.id in busyChatUserIds) {
                                    CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                                } else {
                                    Text("Message")
                                }
                            }
                        }
                    )
                }
            }
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
                text = if (isIncoming) "Incoming Requests (${relationships.size})" else "Sent Requests (${relationships.size})",
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
                        contentDescription = if (isIncoming) "Reload incoming requests" else "Reload sent requests",
                        tint = SecondaryText
                    )
                }
            }
        }

        if (relationships.isEmpty()) {
            EmptyState(
                title = if (isIncoming) "No incoming requests" else "No sent requests",
                body = if (isIncoming) "New connection requests will appear here."
                else "Connection requests you send will appear here."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = FloatingBottomBarDefaults.ContentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(relationships, key = { it.id }) { relationship ->
                    val requesterId = relationship.requestedBy
                    val person = if (requesterId == relationship.coachId) {
                        relationship.person(isCoach = false)
                    } else {
                        relationship.person(isCoach = true)
                    }
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
                                status = "Requested ${relationship.createdAt?.take(10) ?: "recently"}",
                                modifier = Modifier.clickable { onProfile(person.id) }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        ) else Text("Accept")
                                    }
                                    OutlinedButton(
                                        onClick = { onReject(relationship.id) },
                                        enabled = !isBusy
                                    ) { Text("Reject") }
                                } else {
                                    OutlinedButton(
                                        onClick = { onCancel(relationship.id) },
                                        enabled = !isBusy
                                    ) {
                                        if (isBusy) CircularProgressIndicator(
                                            Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp
                                        ) else Text("Cancel request")
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

@Composable
fun CoachProfileScreen(
    coachId: Long,
    onBack: () -> Unit,
    onChatClick: (Long, Long, String, String?) -> Unit,
    viewModel: CoachConnectionsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(coachId) { viewModel.loadCoachProfile(coachId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
            }
            Text(
                "Coach Profile",
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
                        contentDescription = "Reload profile",
                        tint = Accent
                    )
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
                    title = "Profile unavailable",
                    body = state.profileError ?: "Could not load this profile.",
                    action = "Retry",
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
                        UserAvatar(coach.avatarUrl, modifier = Modifier.size(100.dp))

                        Text(
                            text = coach.displayName.ifBlank { coach.fullName },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )

                        // Badges Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (coach.isVerified) {
                                PillBadge("Verified Coach", isAccent = true)
                            }
                            if (coach.isAcceptingClients) {
                                PillBadge("Accepting Clients", isAccent = true)
                            } else {
                                PillBadge("Not Accepting Clients")
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
                                text = "$years ${if (years == 1) "year" else "years"} coaching experience",
                                style = MaterialTheme.typography.labelMedium,
                                color = Accent
                            )
                        }
                    }
                }

                // Specialties Section
                coach.specialties?.takeIf(String::isNotBlank)?.let { specs ->
                    val specList = specs.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (specList.isNotEmpty()) {
                        DarkCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Specialties",
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
                        "Coaching Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Active Clients",
                            value = "${coach.activeClientsCount}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Completed",
                            value = "${coach.completedClientsCount}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Plans Assigned",
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
                                "About Coaching",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                            if (hasPhilosophy) InfoRow("Training Philosophy", coach.trainingPhilosophy)
                            if (hasDescription) InfoRow("Coaching Overview", coach.coachingDescription)
                            if (hasLanguages) InfoRow("Languages", coach.languages)
                            if (hasCerts) InfoRow("Certifications & Qualifications", coach.certifications)
                            if (hasSocial) InfoRow("Social & Portfolio", coach.socialLinks)
                        }
                    }
                }

                // Connection Action Card
                DarkCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Coaching Connection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )

                        val statusText = when {
                            currentUserId == coach.id -> "This is your public coach profile."
                            relation?.status == "ACTIVE" -> "You are actively connected with this coach."
                            relation?.status == "PENDING" && relation.requestedBy == currentUserId ->
                                "Connection request sent and pending coach approval."
                            relation?.status == "PENDING" && relation.requestedBy != currentUserId ->
                                "This coach sent you a connection request."
                            relation?.status == "REJECTED" ->
                                "Your previous connection request was declined. You can send a new request."
                            relation?.status == "CANCELLED" ->
                                "Your previous request was canceled. You can send a new request."
                            !coach.isAcceptingClients ->
                                "This coach is currently not accepting new clients."
                            else ->
                                "Connect with this coach to receive tailored training plans, workout reviews, and 1-on-1 guidance."
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (relation?.status == "ACTIVE") Accent else SecondaryText
                        )

                        when {
                            currentUserId == coach.id -> Unit
                            relation?.status == "ACTIVE" -> {
                                Button(
                                    onClick = {
                                        viewModel.startChat(coach.id) { conversationId ->
                                            onChatClick(conversationId, coach.id, coach.displayName.ifBlank { coach.fullName }, coach.avatarUrl)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                ) {
                                    Text("Message Coach")
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
                                        Text("Accept")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.rejectRequest(responseRelationship.id) },
                                        enabled = responseRelationship.id !in state.busyRelationshipIds,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject")
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
                                        Text("Pending")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.cancelRequest(relation.id) },
                                        enabled = relation.id !in state.busyRelationshipIds,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel Request")
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
                                        Text(if (coach.isAcceptingClients) "Connect with Coach" else "Not Accepting Clients")
                                    }
                                }
                            }
                        }
                    }
                }

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
                contentDescription = "Profile avatar",
                tint = SecondaryText,
                modifier = Modifier.size(30.dp)
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = "Profile avatar",
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
