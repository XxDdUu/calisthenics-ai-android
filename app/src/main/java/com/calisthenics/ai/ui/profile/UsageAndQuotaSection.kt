package com.calisthenics.ai.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calisthenics.ai.domain.model.LiveAnalysisUsage
import com.calisthenics.ai.domain.model.MembershipUsage
import com.calisthenics.ai.domain.model.UserUsage
import com.calisthenics.ai.domain.model.VideoAnalysisUsage
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Info
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Warning
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun UsageAndQuotaSection(
    usage: UserUsage?,
    isLoading: Boolean,
    error: String?,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    DarkCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title + Refresh Button
            UsageHeader(
                lastUpdated = usage?.lastUpdated,
                isLoading = isLoading,
                isOffline = isOffline,
                onRefresh = onRefresh
            )

            // Offline Cached Banner
            if (isOffline && usage != null) {
                OfflineBanner()
            }

            // Error state when no usage cached
            if (error != null && usage == null) {
                UsageErrorCard(
                    errorMessage = error,
                    onRetry = onRefresh
                )
            } else if (isLoading && usage == null) {
                // Initial loading spinner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = Accent
                    )
                }
            } else if (usage != null) {
                // 1. Membership Card
                MembershipUsageCard(
                    membership = usage.membership,
                    isUnlimitedQuota = usage.videoAnalysis.unlimited
                )

                // 2. AI Video Analysis Card
                VideoAnalysisUsageCard(videoAnalysis = usage.videoAnalysis)

                // 3. Live Analysis Card
                LiveAnalysisUsageCard(
                    liveAnalysis = usage.liveAnalysis,
                    membership = usage.membership
                )
            }
        }
    }
}

@Composable
private fun UsageHeader(
    lastUpdated: Long?,
    isLoading: Boolean,
    isOffline: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Usage & Quota",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }
            if (lastUpdated != null) {
                val formattedTime = remember(lastUpdated) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUpdated))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Last updated: $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    if (isOffline) {
                        Text(
                            text = "• Offline - Cached",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Warning
                        )
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Accent
            )
        } else {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh usage",
                    tint = SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Warning.copy(alpha = 0.12f))
            .border(1.dp, Warning.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = Warning,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Offline - showing cached data",
            style = MaterialTheme.typography.bodySmall,
            color = Warning,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun UsageErrorCard(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Error.copy(alpha = 0.1f))
            .border(1.dp, Error.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Error,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = PrimaryText,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Elevated, contentColor = PrimaryText),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Border)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Accent
            )
            Spacer(Modifier.width(6.dp))
            Text("Retry", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MembershipUsageCard(
    membership: MembershipUsage,
    isUnlimitedQuota: Boolean = false
) {
    val tierColor = when {
        membership.isAdmin -> Color(0xFFA855F7) // Royal Violet for ADMIN
        membership.isPro -> Accent // Vibrant Green for PRO
        else -> SecondaryText // Neutral grey for FREE
    }

    val isAdminGrantedPro = !membership.isAdmin && membership.isPro &&
        (isUnlimitedQuota || membership.source.equals("Admin Grant", ignoreCase = true))

    val tierLabel = when {
        membership.isAdmin -> "ADMIN"
        isAdminGrantedPro -> "PRO • UNLIMITED"
        else -> membership.tier.uppercase(Locale.getDefault())
    }

    val formattedExpires = remember(membership.expiresAt) {
        membership.expiresAt?.let { parseDisplayDate(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(14.dp)
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
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = tierColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Membership",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                }

                // Tier Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(tierColor.copy(alpha = 0.16f))
                        .border(1.dp, tierColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tierLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                }
            }

            // Status, Source, Expiration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: ${membership.status.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                membership.source?.let {
                    Text(
                        text = "Source: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }

            if (formattedExpires != null) {
                Text(
                    text = "Expires: $formattedExpires",
                    style = MaterialTheme.typography.bodySmall,
                    color = Accent,
                    fontWeight = FontWeight.Medium
                )
            } else if (isAdminGrantedPro) {
                Text(
                    text = "Unlimited PRO quota active (Admin Granted)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun VideoAnalysisUsageCard(videoAnalysis: VideoAnalysisUsage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AI Video Analysis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                }

                // Remaining Uploads Badge
                val remainingText = if (videoAnalysis.unlimited) {
                    "Unlimited"
                } else {
                    "${videoAnalysis.remaining ?: 0} remaining"
                }
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (videoAnalysis.unlimited || (videoAnalysis.remaining ?: 0) > 0) Accent else Error
                )
            }

            // Usage Count
            val usedText = if (videoAnalysis.unlimited) {
                "Used: ${videoAnalysis.used} / Unlimited"
            } else {
                "Used: ${videoAnalysis.used} / ${videoAnalysis.limit ?: 1}"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = usedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Medium
                )
                if (!videoAnalysis.unlimited && videoAnalysis.limit != null && videoAnalysis.limit > 0) {
                    val percent = (videoAnalysis.progress * 100).toInt()
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { if (videoAnalysis.unlimited) 1f else videoAnalysis.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Accent,
                trackColor = Elevated
            )

            // Reset Time Indicator
            val resetLabel = if (videoAnalysis.unlimited) {
                "Unlimited daily uploads available"
            } else {
                val formattedReset = remember(videoAnalysis.resetAt, videoAnalysis.timezone) {
                    formatResetTime(videoAnalysis.resetAt, videoAnalysis.timezone)
                }
                "Resets at $formattedReset"
            }
            Text(
                text = resetLabel,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun LiveAnalysisUsageCard(
    liveAnalysis: LiveAnalysisUsage,
    membership: MembershipUsage
) {
    val isAvailable = liveAnalysis.available || liveAnalysis.unlimited || membership.isPro
    val isAdmin = membership.isAdmin
    val isPro = membership.isPro

    val (badgeText, badgeColor, description, icon) = when {
        isAdmin -> {
            Quad(
                "Available",
                Info,
                "Available - Unlimited business-level access.",
                Icons.Default.CheckCircle
            )
        }
        isPro && liveAnalysis.unlimited -> {
            Quad(
                "Available",
                Accent,
                "Available - Unlimited PRO access (Admin Granted).",
                Icons.Default.CheckCircle
            )
        }
        isPro || isAvailable -> {
            Quad(
                "Available",
                Accent,
                "Available - Unlocked with PRO.",
                Icons.Default.CheckCircle
            )
        }
        else -> {
            Quad(
                "Locked",
                SecondaryText,
                "Locked - PRO required to unlock live AI pose analysis.",
                Icons.Default.Lock
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(14.dp)
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Live Pose Analysis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isAvailable) PrimaryText else SecondaryText
            )
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private fun parseDisplayDate(dateStr: String): String {
    return try {
        val zdt = ZonedDateTime.parse(dateStr)
        zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
    } catch (_: Exception) {
        try {
            val instant = Instant.parse(dateStr)
            val zdt = instant.atZone(ZoneId.systemDefault())
            zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        } catch (_: Exception) {
            dateStr.take(10)
        }
    }
}

private fun formatResetTime(resetAt: String?, timezone: String): String {
    if (resetAt.isNullOrBlank()) return "00:00 ($timezone)"
    return try {
        val zdt = ZonedDateTime.parse(resetAt)
        val timeFormatted = zdt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
        "$timeFormatted ($timezone)"
    } catch (_: Exception) {
        try {
            val instant = Instant.parse(resetAt)
            val zoneId = try { ZoneId.of(timezone) } catch (_: Exception) { ZoneId.systemDefault() }
            val zdt = instant.atZone(zoneId)
            val timeFormatted = zdt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
            "$timeFormatted ($timezone)"
        } catch (_: Exception) {
            "00:00 ($timezone)"
        }
    }
}
