package com.calisthenics.ai.ui.bodymetrics

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
                        "Body Metrics Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
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
                                        "Current Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText
                                    )
                                }

                                state.currentMetrics?.measuredAt?.let { ts ->
                                    PillBadge(
                                        text = "Updated: ${ts.take(10)}",
                                        isAccent = false
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCard(
                                    label = "Height",
                                    value = state.currentHeightCm?.let { "$it cm" } ?: "Not set",
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Latest Weight",
                                    value = state.latestWeightKg?.let { "$it kg" } ?: "Not recorded",
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
                                    Text("Edit Height")
                                }

                                Button(
                                    onClick = { viewModel.openAddRecordDialog() },
                                    modifier = Modifier.weight(1.3f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Record Entry")
                                }
                            }
                        }
                    }
                }

                // Interactive Progress Chart Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Progress Trends",
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
                                    label = { Text("${metric.displayName} (${metric.unit})") }
                                )
                            }
                        }

                        if (state.chartDataPoints.isNotEmpty()) {
                            InteractiveProgressChart(
                                dataPoints = state.chartDataPoints,
                                selectedFilter = state.selectedTimeFilter,
                                onFilterSelected = { viewModel.selectTimeFilter(it) },
                                unit = state.selectedMetricType.unit,
                                title = "${state.selectedMetricType.displayName} Progress"
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
                                        "No ${state.selectedMetricType.displayName.lowercase()} measurements recorded for this period.",
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
                        "Measurement History (${state.history.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }

                if (state.history.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No measurements recorded",
                            body = "Record your weight and circumference measurements to track your body changes over time.",
                            action = "Record Entry",
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
                    text = "${measurement.weightKg} kg",
                    isAccent = true
                )
            }

            // Optional metrics chips
            val optionalDetails = listOfNotNull(
                measurement.bodyFatPercentage?.let { "Body Fat: $it%" },
                measurement.waistCm?.let { "Waist: $it cm" },
                measurement.chestCm?.let { "Chest: $it cm" },
                measurement.armCm?.let { "Arm: $it cm" },
                measurement.thighCm?.let { "Thigh: $it cm" }
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
                    "Record Body Measurement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Weight (kg) *") },
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
                    label = { Text("Date (YYYY-MM-DD)") },
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
                        label = { Text("Body Fat %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = waistStr,
                        onValueChange = { waistStr = it },
                        label = { Text("Waist (cm)") },
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
                        label = { Text("Chest (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = armStr,
                        onValueChange = { armStr = it },
                        label = { Text("Arm (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = thighStr,
                    onValueChange = { thighStr = it },
                    label = { Text("Thigh (cm)") },
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
                        Text("Cancel", color = SecondaryText)
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
                            Text("Save")
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
                    "Update Height",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                OutlinedTextField(
                    value = heightStr,
                    onValueChange = { heightStr = it },
                    label = { Text("Height (cm) *") },
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
                        Text("Cancel", color = SecondaryText)
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
                            Text("Update")
                        }
                    }
                }
            }
        }
    }
}

