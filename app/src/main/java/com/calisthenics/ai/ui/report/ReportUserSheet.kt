package com.calisthenics.ai.ui.report

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.MAX_REPORT_DESCRIPTION_LENGTH
import com.calisthenics.ai.domain.model.MAX_REPORT_EVIDENCE
import com.calisthenics.ai.domain.model.ReportReason
import com.calisthenics.ai.ui.review.featureErrorText
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun reportReasonLabel(reason: ReportReason?): String = stringResource(
    when (reason) {
        ReportReason.HARASSMENT -> R.string.report_reason_harassment
        ReportReason.INAPPROPRIATE_BEHAVIOR -> R.string.report_reason_inappropriate_behavior
        ReportReason.SPAM_OR_SCAM -> R.string.report_reason_spam_or_scam
        ReportReason.ABUSIVE_LANGUAGE -> R.string.report_reason_abusive_language
        ReportReason.FAKE_ACCOUNT -> R.string.report_reason_fake_account
        ReportReason.INAPPROPRIATE_PROFILE_CONTENT -> R.string.report_reason_inappropriate_profile
        ReportReason.FRAUD -> R.string.report_reason_fraud
        ReportReason.OTHER -> R.string.report_reason_other
        null -> R.string.report_reason_unknown
    }
)

/** Bottom sheet that lets the current user report [userId]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserSheet(
    userId: Long,
    userName: String,
    onDismiss: () -> Unit,
    viewModel: ReportUserViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(userId) { viewModel.start(userId) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { EvidenceImageProcessor.prepare(context, uri) } }
                    .onSuccess(viewModel::addEvidence)
                    .onFailure { viewModel.onEvidencePreparationFailed(it.localizedMessage) }
            }
        }
    }

    val dismiss = {
        if (!state.isSubmitting) {
            viewModel.reset()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { dismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.submitted) {
                Text(stringResource(R.string.report_submitted_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryText)
                Text(stringResource(R.string.report_submitted_body), color = SecondaryText)
                Button(
                    onClick = { dismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                ) { Text(stringResource(R.string.common_done)) }
                return@Column
            }

            Text(stringResource(R.string.report_title, userName), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryText)
            Text(stringResource(R.string.report_subtitle), color = SecondaryText, style = MaterialTheme.typography.bodyMedium)

            Text(stringResource(R.string.report_reason_label), style = MaterialTheme.typography.labelLarge, color = SecondaryText)
            ReportReason.entries.forEach { reason ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !state.isSubmitting, role = Role.RadioButton) { viewModel.setReason(reason) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.reason == reason, onClick = null, modifier = Modifier.padding(8.dp))
                    Text(reportReasonLabel(reason), color = PrimaryText)
                }
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.report_description_label)) },
                supportingText = {
                    Text(
                        stringResource(R.string.review_comment_counter, state.description.length, MAX_REPORT_DESCRIPTION_LENGTH),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                minLines = 3,
                maxLines = 6,
                enabled = !state.isSubmitting
            )

            Text(
                stringResource(R.string.report_evidence_label, state.evidence.size, MAX_REPORT_EVIDENCE),
                style = MaterialTheme.typography.labelLarge,
                color = SecondaryText
            )
            Row(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState(), enabled = false),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.evidence.forEach { item ->
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, Border, RoundedCornerShape(10.dp))) {
                        AsyncImage(
                            model = java.io.File(item.previewPath),
                            contentDescription = stringResource(R.string.report_evidence_image),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp)
                        )
                        if (item.isUploading) {
                            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = Accent, strokeWidth = 2.dp)
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.removeEvidence(item.id) },
                                enabled = !state.isSubmitting,
                                modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.report_remove_evidence), tint = PrimaryText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                if (state.canAddEvidence) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Border, RoundedCornerShape(10.dp))
                            .clickable(enabled = !state.isSubmitting, role = Role.Button) {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = stringResource(R.string.report_add_evidence), tint = Accent)
                    }
                }
            }
            state.uploadError?.let {
                Text(featureErrorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let {
                Text(featureErrorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { dismiss() }, enabled = !state.isSubmitting, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = viewModel::requestConfirm,
                    enabled = state.canSubmit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.report_submit))
                    }
                }
            }
        }
    }

    if (state.showConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirm,
            containerColor = Elevated,
            title = { Text(stringResource(R.string.report_confirm_title)) },
            text = { Text(stringResource(R.string.report_confirm_body, userName)) },
            confirmButton = {
                TextButton(onClick = viewModel::submit) {
                    Text(stringResource(R.string.report_confirm_submit), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissConfirm) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
