package com.calisthenics.ai.ui.pr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.calisthenics.ai.R
import com.calisthenics.ai.data.remote.dto.PersonalRecordDto
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.theme.*

data class ShareRecipientOption(
    val userId: Long,
    val name: String,
    val avatarUrl: String?,
    val roleLabel: String,
    val experienceLevel: String? = null
)

fun List<CoachClientRelationship>.toShareRecipients(currentUserId: Long?): List<ShareRecipientOption> {
    if (currentUserId == null) return emptyList()
    return this
        .filter { it.status == "ACTIVE" && (it.clientId == currentUserId || it.coachId == currentUserId) }
        .mapNotNull { rel ->
            if (rel.clientId == currentUserId) {
                ShareRecipientOption(
                    userId = rel.coachId,
                    name = rel.coachName.ifBlank { "Coach" },
                    avatarUrl = rel.coachAvatarUrl,
                    roleLabel = "Coach",
                    experienceLevel = rel.coachExperienceLevel
                )
            } else if (rel.coachId == currentUserId) {
                ShareRecipientOption(
                    userId = rel.clientId,
                    name = rel.clientName.ifBlank { "Client" },
                    avatarUrl = rel.clientAvatarUrl,
                    roleLabel = "Client",
                    experienceLevel = rel.clientExperienceLevel
                )
            } else {
                null
            }
        }
        .distinctBy { it.userId }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePrBottomSheet(
    pr: PersonalRecordDto,
    recipients: List<ShareRecipientOption>,
    alreadySharedRecipientIds: Set<Long> = emptySet(),
    isLoadingRecipients: Boolean = false,
    isSharing: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirmShare: (recipientId: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRecipientId by remember(recipients, alreadySharedRecipientIds) {
        val defaultOption = recipients.firstOrNull { it.userId !in alreadySharedRecipientIds }
            ?: recipients.firstOrNull()
        mutableStateOf(defaultOption?.userId)
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSharing) onDismiss() },
        sheetState = sheetState,
        containerColor = Elevated,
        contentColor = PrimaryText,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Share Personal Record",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Text(
                        text = "Select a coach or client to share this achievement in chat and PR Feedback.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isSharing) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = SecondaryText
                    )
                }
            }

            // PR Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Ink),
                border = BorderStroke(1.dp, Accent.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = pr.exerciseName.ifBlank { "Personal Record" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        PillBadge(
                            text = pr.recordType.replace("MAX_", "").replace('_', ' ').ifBlank { "PR" },
                            isAccent = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pr.displayFormattedValue,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Accent
                        )
                        pr.improvementPercentage?.let { pct ->
                            if (pct > 0) {
                                Text(
                                    text = "+$pct% improvement",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Accent
                                )
                            }
                        }
                    }

                    val prevFormatted = pr.formattedPrevious
                        ?: pr.previousRecordValue?.let { prev ->
                            val num = if (prev % 1.0 == 0.0) "${prev.toLong()}" else "$prev"
                            val u = pr.unit ?: ""
                            if (u.isNotBlank()) "$num $u" else num
                        }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pr.achievedAt.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.loc_achieved_date, pr.achievedAt.take(10)),
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                        if (!prevFormatted.isNullOrBlank()) {
                            Text(
                                text = "Previous: $prevFormatted",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Recipients List
            when {
                isLoadingRecipients -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Accent)
                    }
                }
                recipients.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink, RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You don't have any active coach or client connections yet. Connect with a coach or client first to share Personal Records.",
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Choose Recipient (${recipients.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryText
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recipients, key = { it.userId }) { recipient ->
                            val isAlreadyShared = recipient.userId in alreadySharedRecipientIds
                            val isSelected = selectedRecipientId == recipient.userId
                            RecipientRowItem(
                                recipient = recipient,
                                isSelected = isSelected,
                                isAlreadyShared = isAlreadyShared,
                                onClick = {
                                    if (!isAlreadyShared && !isSharing) {
                                        selectedRecipientId = recipient.userId
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Confirm / Cancel Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSharing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = PrimaryText)
                }

                val canConfirm = !isSharing &&
                    selectedRecipientId != null &&
                    selectedRecipientId !in alreadySharedRecipientIds &&
                    recipients.isNotEmpty()

                Button(
                    onClick = {
                        selectedRecipientId?.let { onConfirmShare(it) }
                    },
                    enabled = canConfirm,
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Ink
                    )
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Ink,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Sharing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm & Send to Chat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipientRowItem(
    recipient: ShareRecipientOption,
    isSelected: Boolean,
    isAlreadyShared: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected && !isAlreadyShared -> Accent
        else -> Border
    }
    val containerColor = when {
        isSelected && !isAlreadyShared -> Accent.copy(alpha = 0.1f)
        else -> Ink
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = !isAlreadyShared, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            RecipientAvatar(
                name = recipient.name,
                avatarUrl = recipient.avatarUrl,
                modifier = Modifier.size(42.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = recipient.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    PillBadge(
                        text = recipient.roleLabel,
                        isAccent = recipient.roleLabel.equals("Coach", ignoreCase = true)
                    )
                }
                recipient.experienceLevel?.takeIf { it.isNotBlank() }?.let { exp ->
                    Text(
                        text = exp.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }
        }

        if (isAlreadyShared) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Shared",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent
                )
            }
        } else {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Accent,
                    unselectedColor = SecondaryText
                )
            )
        }
    }
}

@Composable
private fun RecipientAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Accent.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}
