package com.calisthenics.ai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.domain.model.UserPresenceStatus
import com.calisthenics.ai.ui.theme.LocalAppColors
import com.calisthenics.ai.ui.theme.SecondaryText
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val OnlineGreen = Color(0xFF4CAF50)
private val OfflineGray = Color(0xFF808080)

@Composable
fun PresenceIndicator(
    presence: UserPresence?,
    connectionState: WebSocketState,
    isLoading: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val onlineColor = if (appColors.isDark) appColors.success else OnlineGreen
    val offlineColor = if (appColors.isDark) appColors.mutedText else OfflineGray
    val label = when {
        connectionState == WebSocketState.CONNECTING -> "Reconnecting…"
        connectionState == WebSocketState.ERROR -> "Presence unavailable"
        connectionState == WebSocketState.DISCONNECTED -> "Presence unavailable"
        presence == null && isLoading -> "Updating status…"
        presence == null && error != null -> "Presence unavailable"
        presence == null -> "Presence unavailable"
        presence.status == UserPresenceStatus.ONLINE -> "Online"
        else -> formatLastSeen(presence.lastSeen)?.let { "Last seen $it" } ?: "Offline"
    }
    val indicatorColor = when {
        connectionState != WebSocketState.CONNECTED -> offlineColor
        presence?.status == UserPresenceStatus.ONLINE -> onlineColor
        else -> offlineColor
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(end = 5.dp)
                .size(7.dp)
                .background(indicatorColor, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (presence?.status == UserPresenceStatus.ONLINE &&
                connectionState == WebSocketState.CONNECTED
            ) onlineColor else SecondaryText
        )
    }
}

private fun formatLastSeen(timestamp: String?): String? {
    if (timestamp.isNullOrBlank()) return null
    val instant = runCatching { Instant.parse(timestamp) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(timestamp).toInstant() }.getOrNull()
        ?: runCatching { ZonedDateTime.parse(timestamp).toInstant() }.getOrNull()
        ?: return null
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
