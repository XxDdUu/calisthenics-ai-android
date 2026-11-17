package com.calisthenics.ai.ui.bodymetrics

import com.calisthenics.ai.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.data.remote.dto.BodyMeasurementDto
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.InteractiveProgressChart
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    onBack: () -> Unit,
    viewModel: BodyMetricsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.successMessage) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.loc_body_metrics_tracking),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = PrimaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Ink
    ) { padding ->
        if (state.isLoading && state.currentMetrics == null && state.history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header / Current Metrics Card
                item {
                    DarkCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = Accent)
                                    Text(
                                        stringResource(R.string.loc_current_status),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText
                                    )
                                }

                                state.currentMetrics?.measuredAt?.let { ts ->
                                    PillBadge(
                                        text = stringResource(R.string.loc_updated_date, ts.take(10)),
                                        isAccent = false
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCard(
                                    label = stringResource(R.string.loc_height),
                                    value = state.currentHeightCm?.let { "$it cm" } ?: stringResource(R.string.loc_not_set),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = stringResource(R.string.latest_weight),
                                    value = state.latestWeightKg?.let { "$it kg" } ?: stringResource(R.string.loc_not_recorded),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.openEditHeightDialog() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Height, contentDescription = null, modifier = Modifier.size(16.dp), tint = Accent)
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.edit_height))
                                }

                                Button(
                                    onClick = { viewModel.openAddRecordDialog() },
                                    modifier = Modifier.weight(1.3f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.record_entry))
                                }
                            }
                        }
                    }
                }

                // Interactive Progress Chart Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.loc_progress_trends),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )

                        // Metric switcher tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BodyMetricType.entries.forEach { metric ->
                                FilterChip(
                                    selected = state.selectedMetricType == metric,
                                    onClick = { viewModel.selectMetricType(metric) },
                                    label = { Text(stringResource(R.string.loc_metric_label, metric.displayName, metric.unit)) }
                                )
                            }
                        }

                        if (state.chartDataPoints.isNotEmpty()) {
                            InteractiveProgressChart(
                                dataPoints = state.chartDataPoints,
                                selectedFilter = state.selectedTimeFilter,
                                onFilterSelected = { viewModel.selectTimeFilter(it) },
                                unit = state.selectedMetricType.unit,
                                title = stringResource(R.string.loc_progress_named_title, state.selectedMetricType.displayName)
                            )
                        } else {
                            DarkCard(Modifier.fillMaxWidth()) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(R.string.loc_no_measurements_period, state.selectedMetricType.displayName.lowercase()),
                                        color = SecondaryText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Measurement History List Header
                item {
                    Text(
                        stringResource(R.string.loc_measurement_history_count, state.history.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }

                if (state.history.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.no_measurements),
                            body = stringResource(R.string.loc_body_tracking_description),
                            action = stringResource(R.string.record_entry),
                            onAction = { viewModel.openAddRecordDialog() }
                        )
                    }
                } else {
                    items(state.history, key = { it.id }) { item ->
                        MeasurementHistoryItem(measurement = item)
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (state.isAddRecordOpen) {
        RecordMeasurementDialog(
            isSaving = state.isSaving,
            onDismiss = { viewModel.closeAddRecordDialog() },
            onConfirm = { w, bf, waist, chest, arm, thigh, date ->
                viewModel.recordMeasurement(w, bf, waist, chest, arm, thigh, date)
            }
        )
    }

    if (state.isEditHeightOpen) {
        EditHeightDialog(
            currentHeight = state.currentHeightCm,
            isSaving = state.isSaving,
            onDismiss = { viewModel.closeEditHeightDialog() },
            onConfirm = { h -> viewModel.updateHeight(h) }
        )
    }
}

@Composable
private fun MeasurementHistoryItem(
    measurement: BodyMeasurementDto,
    modifier: Modifier = Modifier
) {
    DarkCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = measurement.measuredAt.take(10),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                PillBadge(
                    text = stringResource(R.string.loc_weight_kg_value, measurement.weightKg.toString()),
                    isAccent = true
                )
            }

            // Optional metrics chips
            val optionalDetails = listOfNotNull(
                measurement.bodyFatPercentage?.let { stringResource(R.string.loc_body_fat_label, it.toString()) },
                measurement.waistCm?.let { stringResource(R.string.loc_waist_label, it.toString()) },
                measurement.chestCm?.let { stringResource(R.string.loc_chest_label, it.toString()) },
                measurement.armCm?.let { stringResource(R.string.loc_arm_label, it.toString()) },
                measurement.thighCm?.let { stringResource(R.string.loc_thigh_label, it.toString()) }
            )

            if (optionalDetails.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    optionalDetails.forEach { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            modifier = Modifier
                                .background(Elevated, RoundedCornerShape(6.dp))
                                .border(1.dp, Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordMeasurementDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, String?, String?, String?, String?) -> Unit
) {
    var weightStr by remember { mutableStateOf("") }
    var bodyFatStr by remember { mutableStateOf("") }
    var waistStr by remember { mutableStateOf("") }
    var chestStr by remember { mutableStateOf("") }
    var armStr by remember { mutableStateOf("") }
    var thighStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(LocalDate.now().toString()) }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    stringResource(R.string.loc_record_body_measurement),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text(stringResource(R.string.weight_kg_required)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        focusedLabelColor = Accent
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text(stringResource(R.string.date_yyyy_mm_dd)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        focusedLabelColor = Accent
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bodyFatStr,
                        onValueChange = { bodyFatStr = it },
                        label = { Text(stringResource(R.string.body_fat_percent)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = waistStr,
                        onValueChange = { waistStr = it },
                        label = { Text(stringResource(R.string.waist_cm)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chestStr,
                        onValueChange = { chestStr = it },
                        label = { Text(stringResource(R.string.chest_cm)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = armStr,
                        onValueChange = { armStr = it },
                        label = { Text(stringResource(R.string.arm_cm)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = thighStr,
                    onValueChange = { thighStr = it },
                    label = { Text(stringResource(R.string.thigh_cm)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text(stringResource(R.string.cancel), color = SecondaryText)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                weightStr,
                                bodyFatStr.ifBlank { null },
                                waistStr.ifBlank { null },
                                chestStr.ifBlank { null },
                                armStr.ifBlank { null },
                                thighStr.ifBlank { null },
                                dateStr.ifBlank { null }
                            )
                        },
                        enabled = !isSaving && weightStr.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Ink, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.common_save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditHeightDialog(
    currentHeight: Double?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var heightStr by remember { mutableStateOf(currentHeight?.toString().orEmpty()) }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.loc_update_height),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                OutlinedTextField(
                    value = heightStr,
                    onValueChange = { heightStr = it },
                    label = { Text(stringResource(R.string.height_cm_required)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        focusedLabelColor = Accent
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text(stringResource(R.string.cancel), color = SecondaryText)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(heightStr) },
                        enabled = !isSaving && heightStr.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Ink, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.common_update))
                        }
                    }
                }
            }
        }
    }
}

