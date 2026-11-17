package com.calisthenics.ai.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.data.remote.dto.NotificationDto
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.CardBackground
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error as ThemeError
import com.calisthenics.ai.ui.theme.Info
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.SubtleBorder
import com.calisthenics.ai.ui.theme.Success
import com.calisthenics.ai.ui.theme.Surface as ThemeSurface
import com.calisthenics.ai.ui.theme.Warning
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateTarget: (NotificationNavigationTarget) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val grouped = remember(uiState.notifications) {
        viewModel.groupedNotifications()
    }

    val shouldAutoLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            uiState.hasMore && !uiState.isLoadingMore && total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldAutoLoadMore) {
        if (shouldAutoLoadMore) {
            viewModel.loadMoreNotifications()
        }
    }

    uiState.selectedAnnouncement?.let { announcement ->
        AnnouncementDetailDialog(
            notification = announcement,
            onDismiss = viewModel::dismissAnnouncementDetail
        )
    }

    val hasAnyUnread = uiState.unreadCount > 0L || uiState.notifications.any { !it.isRead }

    Scaffold(
        containerColor = ThemeSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeSurface,
                    titleContentColor = PrimaryText,
                    navigationIconContentColor = PrimaryText
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (hasAnyUnread) {
                        TextButton(
                            onClick = viewModel::markAllAsRead,
                            enabled = !uiState.isMarkingAll
                        ) {
                            if (uiState.isMarkingAll) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Accent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.DoneAll,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mark All as Read",
                                    color = Accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isLoading && !uiState.isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh notifications",
                            tint = SecondaryText
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            // Filter Strip: All vs Unread
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotificationFilter.entries.forEach { filter ->
                    val selected = uiState.filter == filter
                    val chipLabel = if (filter == NotificationFilter.UNREAD && uiState.unreadCount > 0L) {
                        "${filter.label} (${uiState.unreadCount})"
                    } else {
                        filter.label
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                text = chipLabel,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent.copy(alpha = 0.18f),
                            selectedLabelColor = Accent,
                            containerColor = CardBackground,
                            labelColor = SecondaryText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            selectedBorderColor = Accent,
                            borderColor = Border
                        )
                    )
                }
            }

            if (!uiState.error.isNullOrBlank() && uiState.notifications.isNotEmpty()) {
                NotificationErrorBanner(
                    message = uiState.error.orEmpty(),
                    onRetry = { viewModel.loadNotifications(resetPage = true) },
                    onDismiss = viewModel::clearError
                )
            }

            HorizontalDivider(color = SubtleBorder)

            when {
                uiState.isLoading && uiState.notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Accent)
                            Text(
                                text = "Loading notifications...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText
                            )
                        }
                    }
                }

                !uiState.error.isNullOrBlank() && uiState.notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = ThemeError,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Could not load notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                            Text(
                                text = uiState.error.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadNotifications(resetPage = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                            ) {
                                Text("Try Again", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                uiState.notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Elevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsNone,
                                    contentDescription = null,
                                    tint = SecondaryText,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = if (uiState.filter == NotificationFilter.UNREAD) {
                                    "You're all caught up"
                                } else {
                                    "No notifications yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                            Text(
                                text = if (uiState.filter == NotificationFilter.UNREAD) {
                                    "Unread notifications from your coach and training programs will appear here."
                                } else {
                                    "Workout assignments, coach updates, training programs, and announcements will appear here."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        grouped.forEach { (group, groupItems) ->
                            item(key = "section_${group.name}") {
                                Text(
                                    text = group.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryText,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                                )
                            }
                            items(groupItems, key = { it.id }) { notification ->
                                NotificationRowCard(
                                    notification = notification,
                                    onClick = {
                                        viewModel.onNotificationTapped(notification) { target ->
                                            if (target !is NotificationNavigationTarget.AnnouncementDetail &&
                                                target !is NotificationNavigationTarget.None
                                            ) {
                                                onNavigateTarget(target)
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        if (!uiState.paginationError.isNullOrBlank()) {
                            item(key = "pagination_error") {
                                NotificationErrorBanner(
                                    message = uiState.paginationError.orEmpty(),
                                    onRetry = { viewModel.loadMoreNotifications() },
                                    onDismiss = viewModel::clearError
                                )
                            }
                        }

                        if (uiState.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Accent
                                    )
                                }
                            }
                        } else if (uiState.hasMore) {
                            item(key = "load_more_btn") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = viewModel::loadMoreNotifications,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Load More")
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

private data class NotificationCategoryVisual(
    val icon: ImageVector,
    val tint: Color,
    val label: String
)

@Composable
private fun categoryVisualFor(type: String): NotificationCategoryVisual {
    return when (type.trim().uppercase(Locale.US)) {
        "WORKOUT_ASSIGNED" -> NotificationCategoryVisual(
            icon = Icons.Outlined.FitnessCenter,
            tint = Accent,
            label = "Workout Assigned"
        )
        "PROGRAM_AVAILABLE" -> NotificationCategoryVisual(
            icon = Icons.Outlined.Explore,
            tint = Success,
            label = "New Program"
        )
        "COACH_CLIENT_REQUEST" -> NotificationCategoryVisual(
            icon = Icons.Outlined.Group,
            tint = Info,
            label = "Coaching"
        )
        "ADMIN_ANNOUNCEMENT" -> NotificationCategoryVisual(
            icon = Icons.Outlined.Campaign,
            tint = Warning,
            label = "Announcement"
        )
        "PR_SHARED", "COACH_FEEDBACK" -> NotificationCategoryVisual(
            icon = Icons.Outlined.EmojiEvents,
            tint = Accent,
            label = "Personal Record"
        )
        else -> NotificationCategoryVisual(
            icon = Icons.Outlined.Notifications,
            tint = SecondaryText,
            label = "Update"
        )
    }
}

@Composable
private fun NotificationRowCard(
    notification: NotificationDto,
    onClick: () -> Unit
) {
    val visual = categoryVisualFor(notification.resolvedType)
    val isUnread = !notification.isRead
    val cardColor = if (isUnread) Elevated else CardBackground
    val borderColor = if (isUnread) Accent.copy(alpha = 0.42f) else Border

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(visual.tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = visual.label,
                    tint = visual.tint,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = visual.tint.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = visual.label,
                            color = visual.tint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatNotificationTimestamp(notification.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                        if (isUnread) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Accent)
                            )
                        }
                    }
                }

                Text(
                    text = notification.title.ifBlank { visual.label },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (notification.message.isNotBlank()) {
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = ThemeError.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, ThemeError.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                color = ThemeError,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Retry", color = ThemeError, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = SecondaryText)
            }
        }
    }
}

@Composable
private fun AnnouncementDetailDialog(
    notification: NotificationDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Elevated,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Warning.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Campaign,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "PLATFORM ANNOUNCEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Warning,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = notification.title.ifBlank { "Announcement" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryText
                )
                HorizontalDivider(color = SubtleBorder)
                Text(
                    text = "Published ${formatNotificationTimestamp(notification.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
            ) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

private fun formatNotificationTimestamp(createdAt: String): String {
    val parsed = NotificationsViewModel.parseCreatedDateTime(createdAt) ?: return ""
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    val duration = Duration.between(parsed, now)
    if (!duration.isNegative) {
        val minutes = duration.toMinutes()
        if (minutes < 1L) return "Just now"
        if (minutes < 60L) return "${minutes}m ago"
        val hours = duration.toHours()
        if (hours < 24L && parsed.toLocalDate().isEqual(LocalDate.now())) {
            return "${hours}h ago"
        }
    }
    return parsed.format(DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault()))
}
