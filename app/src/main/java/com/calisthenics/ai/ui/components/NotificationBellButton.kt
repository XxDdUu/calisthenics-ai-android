package com.calisthenics.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Surface as ThemeSurface

@Composable
fun HomeTopBarActions(
    unreadCount: Long,
    unreadCountAvailable: Boolean,
    avatarUrl: String? = null,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationBellButton(
            unreadCount = unreadCount,
            countAvailable = unreadCountAvailable,
            onClick = onNotificationsClick
        )
        ProfileCircleActionButton(
            avatarUrl = avatarUrl,
            onClick = onProfileClick
        )
    }
}

@Composable
fun NotificationBellButton(
    unreadCount: Long,
    countAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasUnread = countAvailable && unreadCount > 0L
    val badgeText = if (unreadCount > 99L) "99+" else unreadCount.toString()
    val accessibilityLabel = if (hasUnread) {
        "Notifications, $unreadCount unread"
    } else {
        "Notifications"
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .semantics { contentDescription = accessibilityLabel },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Elevated)
                .border(1.dp, if (hasUnread) Accent.copy(alpha = 0.55f) else Border, CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = PrimaryText,
                modifier = Modifier.size(21.dp)
            )
        }

        if (hasUnread) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp),
                shape = CircleShape,
                color = Accent
            ) {
                Text(
                    text = badgeText,
                    color = Ink,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .border(1.5.dp, ThemeSurface, CircleShape)
                        .padding(horizontal = if (badgeText.length == 1) 5.dp else 4.dp, vertical = 1.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SecondaryText.copy(alpha = 0.6f))
                    .border(1.5.dp, ThemeSurface, CircleShape)
            )
        }
    }
}

@Composable
private fun ProfileCircleActionButton(
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!avatarUrl.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
        ) {
            UserAvatar(
                url = avatarUrl,
                modifier = Modifier.size(44.dp)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Elevated)
                .border(1.dp, Border, CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "Profile" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = PrimaryText,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}
