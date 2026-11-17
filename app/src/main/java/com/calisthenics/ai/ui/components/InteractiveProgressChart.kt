package com.calisthenics.ai.ui.components

import com.calisthenics.ai.R

import androidx.compose.ui.res.stringResource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calisthenics.ai.data.remote.dto.ProgressChartDataPointDto
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Warning
import kotlin.math.abs

@Composable
fun InteractiveProgressChart(
    dataPoints: List<ProgressChartDataPointDto>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    unit: String = "kg",
    title: String = "Progress Over Time"
) {
    val goldPr = Warning
    val filters = listOf("7d", "30d", "90d", "6m", "1y", "All")
    var selectedPointIndex by remember(dataPoints) {
        mutableStateOf(if (dataPoints.isNotEmpty()) dataPoints.size - 1 else -1)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Time Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }

            // Filter pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = filter.equals(selectedFilter, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) Accent else Ink,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Accent else Border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .pointerInput(filter) {
                                detectTapGestures { onFilterSelected(filter) }
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Ink else SecondaryText
                        )
                    }
                }
            }

            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.loc_no_workout_logs),
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Selected Data Point Preview Card
                val selectedPoint = dataPoints.getOrNull(selectedPointIndex)
                if (selectedPoint != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink, RoundedCornerShape(10.dp))
                            .border(1.dp, if (selectedPoint.isPersonalRecord) goldPr else Border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = selectedPoint.date,
                                    color = SecondaryText,
                                    fontSize = 12.sp
                                )
                                if (selectedPoint.isPersonalRecord) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(goldPr.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = stringResource(R.string.loc_pr_short),
                                            tint = goldPr,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            stringResource(R.string.loc_pr_short),
                                            color = goldPr,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            val detailsText = buildString {
                                if (selectedPoint.weightKg != null && selectedPoint.weightKg > 0) {
                                    append("+${selectedPoint.weightKg} kg")
                                }
                                if (selectedPoint.reps != null && selectedPoint.reps > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append("${selectedPoint.reps} reps")
                                }
                                if (selectedPoint.durationSeconds != null && selectedPoint.durationSeconds > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append("${selectedPoint.durationSeconds}s")
                                }
                                if (selectedPoint.totalVolume != null && selectedPoint.totalVolume > 0) {
                                    if (isNotEmpty()) append(" · Vol: ")
                                    append("${selectedPoint.totalVolume.toInt()} kg")
                                }
                            }
                            if (detailsText.isNotEmpty()) {
                                Text(
                                    text = detailsText,
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = "${if (selectedPoint.value % 1.0 == 0.0) selectedPoint.value.toInt().toString() else "%.1f".format(selectedPoint.value)} $unit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedPoint.isPersonalRecord) goldPr else Accent
                        )
                    }
                }

                // Interactive Chart Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    val minValue = (dataPoints.minOfOrNull { it.value } ?: 0.0).coerceAtLeast(0.0)
                    val maxValue = ((dataPoints.maxOfOrNull { it.value } ?: 10.0) * 1.15).coerceAtLeast(minValue + 5.0)
                    val chartBorder = Border
                    val chartAccent = Accent
                    val chartPrimaryText = PrimaryText

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(dataPoints) {
                                detectTapGestures { tapOffset ->
                                    val width = size.width
                                    val count = dataPoints.size
                                    if (count > 1) {
                                        val step = width / (count - 1)
                                        val clickedIndex = (tapOffset.x / step).toInt().coerceIn(0, count - 1)
                                        selectedPointIndex = clickedIndex
                                    } else {
                                        selectedPointIndex = 0
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val paddingTop = 16.dp.toPx()
                        val paddingBottom = 24.dp.toPx()
                        val chartHeight = canvasHeight - paddingTop - paddingBottom

                        // Draw horizontal subtle grid lines
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = paddingTop + (chartHeight / gridLines) * i
                            drawLine(
                                color = chartBorder.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        if (dataPoints.size == 1) {
                            // Single point
                            val cx = canvasWidth / 2f
                            val cy = paddingTop + chartHeight / 2f
                            val dp = dataPoints[0]
                            drawCircle(
                                color = if (dp.isPersonalRecord) goldPr else chartAccent,
                                radius = 7.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                            return@Canvas
                        }

                        // Calculate points
                        val stepX = canvasWidth / (dataPoints.size - 1)
                        val points = dataPoints.mapIndexed { idx, dp ->
                            val x = idx * stepX
                            val normalized = ((dp.value - minValue) / (maxValue - minValue)).toFloat().coerceIn(0f, 1f)
                            val y = paddingTop + (1f - normalized) * chartHeight
                            Offset(x, y)
                        }

                        // Fill Path below the curve
                        val fillPath = Path().apply {
                            moveTo(points.first().x, canvasHeight - paddingBottom)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, canvasHeight - paddingBottom)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(chartAccent.copy(alpha = 0.35f), Color.Transparent),
                                startY = paddingTop,
                                endY = canvasHeight - paddingBottom
                            )
                        )

                        // Line Path
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = chartAccent,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw points and PR indicators
                        points.forEachIndexed { idx, pt ->
                            val dp = dataPoints[idx]
                            val isSelected = idx == selectedPointIndex

                            if (dp.isPersonalRecord) {
                                // Draw PR gold halo
                                drawCircle(
                                    color = goldPr.copy(alpha = 0.4f),
                                    radius = if (isSelected) 12.dp.toPx() else 9.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = goldPr,
                                    radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                                    center = pt
                                )
                            } else {
                                drawCircle(
                                    color = if (isSelected) chartPrimaryText else chartAccent,
                                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                    center = pt
                                )
                            }

                            // Selected indicator vertical dashed line
                            if (isSelected) {
                                drawLine(
                                    color = chartPrimaryText.copy(alpha = 0.5f),
                                    start = Offset(pt.x, paddingTop),
                                    end = Offset(pt.x, canvasHeight - paddingBottom),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
