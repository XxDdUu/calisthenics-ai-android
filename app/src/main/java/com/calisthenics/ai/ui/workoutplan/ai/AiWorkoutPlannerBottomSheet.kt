package com.calisthenics.ai.ui.workoutplan.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import com.calisthenics.ai.ui.components.PillBadge
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AiWorkoutExercisePreview
import com.calisthenics.ai.domain.model.AiWorkoutPlanPreview
import com.calisthenics.ai.domain.model.AiWorkoutSessionPreview
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.ui.components.markdown.AiMarkdownText
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Info
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.MutedText
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.SubtleBorder
import com.calisthenics.ai.ui.theme.Success
import com.calisthenics.ai.ui.theme.Surface
import com.calisthenics.ai.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWorkoutPlannerScreen(
    clientId: Long? = null,
    clientName: String? = null,
    onBackClick: () -> Unit,
    onPlanCommitted: (UserWorkoutPlan) -> Unit,
    viewModel: AiWorkoutPlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val resolvedError = when {
        uiState.errorResId != null && uiState.errorFormatArg != null ->
            stringResource(uiState.errorResId!!, uiState.errorFormatArg!!)
        uiState.errorResId != null ->
            stringResource(uiState.errorResId!!)
        else -> uiState.errorMessage
    }

    LaunchedEffect(clientId, clientName) {
        viewModel.initialize(clientId, clientName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.ai_workout_planner_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            )
                            if (uiState.clientName != null) {
                                Text(
                                    text = stringResource(R.string.ai_planning_for_client, uiState.clientName ?: ""),
                                    style = MaterialTheme.typography.bodySmall.copy(color = Accent)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        enabled = !uiState.isGenerating && !uiState.isCheckingConflict && !uiState.isCommitting
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink,
                    navigationIconContentColor = PrimaryText,
                    titleContentColor = PrimaryText
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Ink
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.step) {
                PlannerStep.INPUT, PlannerStep.GENERATING -> {
                    ChatInputScreen(
                        uiState = uiState,
                        resolvedError = resolvedError,
                        onPromptChange = viewModel::onPromptChange,
                        onApplyPreset = viewModel::applyPreset,
                        onGenerate = viewModel::generatePlan,
                        onCancel = viewModel::cancelGeneration
                    )
                }
                PlannerStep.PREVIEW -> {
                    uiState.planPreview?.let { preview ->
                        PreviewStepContent(
                            preview = preview,
                            selectedDayIndex = uiState.selectedDayIndex,
                            onSelectedDayIndexChange = viewModel::onSelectedDayIndexChange,
                            modifyPrompt = uiState.modifyPrompt,
                            onModifyPromptChange = viewModel::onModifyPromptChange,
                            isModifying = uiState.isModifying,
                            isCheckingConflict = uiState.isCheckingConflict,
                            isCommitting = uiState.isCommitting,
                            conflictPlanName = uiState.conflictPlanName ?: preview.metadata.existingActivePlanName,
                            onApplyModifyPreset = viewModel::applyModifyPreset,
                            onModify = viewModel::modifyPlan,
                            onCommit = viewModel::requestCommit,
                            onReset = viewModel::reset,
                            isCoachClient = uiState.clientId != null,
                            errorMessage = resolvedError,
                            hasAlreadyRevealed = uiState.hasRevealedPlan,
                            onPlanRevealCompleted = viewModel::onPlanRevealCompleted
                        )
                    }
                }
                PlannerStep.COMMITTING -> {
                    CommittingStepContent()
                }
                PlannerStep.SUCCESS -> {
                    SuccessStepContent(
                        committedPlan = uiState.committedPlan,
                        onDone = {
                            uiState.committedPlan?.let { onPlanCommitted(it) }
                                ?: onBackClick()
                        }
                    )
                }
            }
        }
    }

    // Active Schedule Conflict Confirmation Dialog
    if (uiState.showConflictDialog) {
        val existingName = uiState.conflictPlanName
            ?: uiState.planPreview?.metadata?.existingActivePlanName
            ?: "Current Schedule"
        AlertDialog(
            onDismissRequest = viewModel::dismissConflictDialog,
            title = {
                Text(
                    text = stringResource(R.string.ai_conflict_dialog_title),
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.ai_conflict_dialog_message, existingName),
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = viewModel::commitWithoutReplacing,
                        enabled = !uiState.isCommitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.ai_save_without_replacing),
                            color = Accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmConflictAndCommit,
                    enabled = !uiState.isCommitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Ink
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.ai_conflict_replace_confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissConflictDialog,
                    enabled = !uiState.isCommitting
                ) {
                    Text(stringResource(R.string.cancel), color = SecondaryText)
                }
            },
            containerColor = Elevated,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun AiWorkoutPlannerBottomSheet(
    clientId: Long? = null,
    clientName: String? = null,
    onDismiss: () -> Unit,
    onPlanCommitted: (UserWorkoutPlan) -> Unit,
    viewModel: AiWorkoutPlannerViewModel = hiltViewModel()
) {
    AiWorkoutPlannerScreen(
        clientId = clientId,
        clientName = clientName,
        onBackClick = onDismiss,
        onPlanCommitted = onPlanCommitted,
        viewModel = viewModel
    )
}

@Composable
private fun ChatInputScreen(
    uiState: AiWorkoutPlannerUiState,
    resolvedError: String?,
    onPromptChange: (String) -> Unit,
    onApplyPreset: (String) -> Unit,
    onGenerate: () -> Unit,
    onCancel: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.chatMessages.size, uiState.isGenerating, uiState.generationStatus) {
        val totalCount = uiState.chatMessages.size + (if (uiState.isGenerating) 1 else 0)
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Chat conversation area (scrollable, weight(1f))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Welcome message when no messages
            if (uiState.chatMessages.isEmpty()) {
                item {
                    WelcomeSection(gender = uiState.gender)
                }
            } else {
                items(
                    items = uiState.chatMessages,
                    key = { it.id }
                ) { msg ->
                    when (msg.role) {
                        MessageRole.USER -> UserMessageBubble(text = msg.text)
                        MessageRole.AI -> AiMessageBubble(text = msg.text)
                    }
                }
            }

            // AI status row (shown during generation)
            if (uiState.isGenerating) {
                item {
                    AiStatusRow(generationStatus = uiState.generationStatus)
                }
            }
        }

        // Suggestion chips (shown only when not generating and no messages yet, or always above input)
        if (uiState.chatMessages.isEmpty() && !uiState.isGenerating) {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                SuggestionChipsRow(
                    gender = uiState.gender,
                    onApplyPreset = onApplyPreset,
                    onGenerate = onGenerate
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat input composer (anchored flush above keyboard / nav bar)
        ChatComposer(
            prompt = uiState.prompt,
            onPromptChange = onPromptChange,
            onGenerate = onGenerate,
            onCancel = onCancel,
            isGenerating = uiState.isGenerating,
            resolvedError = resolvedError
        )
    }
}

@Composable
private fun WelcomeSection(gender: Gender?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AI Workout Planner",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Describe your fitness goals and I'll create a personalized workout plan for you.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = SecondaryText
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuggestionChipsRow(
    gender: Gender?,
    onApplyPreset: (String) -> Unit,
    onGenerate: () -> Unit
) {
    val suggestions = when (gender) {
        Gender.MALE -> listOf(
            "Build upper body strength – 4 weeks, starting today",
            "Develop chest & shoulders – 6 weeks, starting next Monday",
            "Build bigger arms – 4 weeks, starting today",
            "Build core strength – 4 weeks, starting today",
            "Learn calisthenics skills – 8 weeks, starting today",
            "Build a full body plan – 4 weeks, starting next Monday"
        )
        Gender.FEMALE -> listOf(
            "Build & strengthen glutes – 6 weeks, starting today",
            "Tone legs & lower body – 4 weeks, starting today",
            "Strengthen core & abs – 4 weeks, starting today",
            "Improve full body fitness – 4 weeks, starting next Monday",
            "Improve flexibility & mobility – 6 weeks, starting today",
            "Create a full body workout – 4 weeks, starting today"
        )
        null -> listOf(
            "Build a full body plan – 4 weeks, starting today",
            "Build upper body strength – 4 weeks, starting today",
            "Improve core strength – 4 weeks, starting today",
            "Increase flexibility – 6 weeks, starting today"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .clickable {
                        onApplyPreset(suggestion)
                        onGenerate()
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PrimaryText,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
    isGenerating: Boolean,
    resolvedError: String?
) {
    val accentColor = Accent
    val borderColor = Border

    // Single shared rotation with synchronized natural, irregular variable speed
    val infiniteTransition = rememberInfiniteTransition(label = "copilot")
    val sharedRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4200
                0f at 0 using FastOutSlowInEasing
                0.38f at 1200 using FastOutSlowInEasing // swift burst
                0.55f at 2200 using FastOutSlowInEasing // gentle slower cruise
                0.82f at 3100 using FastOutSlowInEasing // acceleration sweep
                1f at 4200 using FastOutSlowInEasing   // smooth arrival into loop
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "sharedRotation"
    )
    val fixedPhaseOffset = 0.5f // fixed 180° offset
    val arc1Phase = sharedRotation
    val arc2Phase = sharedRotation + fixedPhaseOffset

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Error banner
        if (!resolvedError.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = resolvedError,
                        color = Error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Single stationary composer container with border and animation drawn on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Surface, shape = RoundedCornerShape(28.dp))
                .drawWithContent {
                    // 1. Draw child content (text field, icons, background)
                    drawContent()

                    // 2. Draw border layer on top so it is always visible and never obscured by background
                    val strokePx = if (isGenerating) 2.5.dp.toPx() else 1.dp.toPx()
                    val cornerPx = 28.dp.toPx()
                    val inset = strokePx / 2f

                    val rrPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = inset,
                                top = inset,
                                right = size.width - inset,
                                bottom = size.height - inset,
                                radiusX = cornerPx,
                                radiusY = cornerPx
                            )
                        )
                    }

                    if (!isGenerating) {
                        // Static idle border
                        drawPath(
                            path = rrPath,
                            color = borderColor,
                            style = Stroke(width = strokePx)
                        )
                    } else {
                        // Subtle base track while generating
                        drawPath(
                            path = rrPath,
                            color = borderColor.copy(alpha = 0.35f),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Trace two independent animated gradient arcs along the rounded-rect perimeter
                        val pm = PathMeasure()
                        pm.setPath(rrPath, forceClosed = true)
                        val totalLength = pm.length

                        if (totalLength > 0f) {
                            val arcLength = totalLength * 0.22f // each arc spans ~22% of perimeter
                            val slices = 20 // gradient segments for smooth fade from tail to head

                            fun drawGlowingArc(phase: Float) {
                                val normPhase = ((phase % 1f) + 1f) % 1f
                                val startDist = normPhase * totalLength
                                val sliceLength = arcLength / slices
                                val slicePath = Path()

                                for (s in 0 until slices) {
                                    val t = (s + 1).toFloat() / slices // 0..1 from tail to head
                                    val alpha = (t * t).coerceIn(0.02f, 1f) // smooth quadratic gradient leading to head

                                    val d1 = (startDist + s * sliceLength) % totalLength
                                    val d2 = (startDist + (s + 1) * sliceLength) % totalLength

                                    slicePath.reset()
                                    if (d1 < d2) {
                                        pm.getSegment(d1, d2, slicePath, true)
                                    } else {
                                        // Wraps around the path boundary
                                        pm.getSegment(d1, totalLength, slicePath, true)
                                        pm.getSegment(0f, d2, slicePath, false)
                                    }

                                    drawPath(
                                        path = slicePath,
                                        color = accentColor.copy(alpha = alpha),
                                        style = Stroke(
                                            width = strokePx,
                                            cap = if (s == slices - 1) StrokeCap.Round else StrokeCap.Butt
                                        )
                                    )
                                }
                            }

                            // Arc 1 & Arc 2: perfectly synchronized with fixed 180° offset
                            drawGlowingArc(arc1Phase)
                            drawGlowingArc(arc2Phase)
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    enabled = !isGenerating,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = PrimaryText),
                    cursorBrush = SolidColor(Accent),
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (prompt.isEmpty()) {
                                Text(
                                    text = "Describe your fitness goals...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = SecondaryText.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Border.copy(alpha = 0.5f))
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    val isEnabled = prompt.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) Accent else Elevated)
                            .then(
                                if (isEnabled) Modifier.clickable(onClick = onGenerate)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = stringResource(R.string.ai_generate_button),
                            tint = if (isEnabled) Ink else MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentWidth(Alignment.End)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                .background(Accent)
                .padding(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Ink,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun AiMessageBubble(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentWidth(Alignment.Start)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(Elevated)
                .padding(12.dp)
        ) {
            AiMarkdownText(
                markdown = text,
                color = PrimaryText
            )
        }
    }
}

@Composable
private fun AiStatusRow(generationStatus: GenerationStatus) {
    val statusText = when (generationStatus) {
        GenerationStatus.SUBMITTING -> "Submitting your request..."
        GenerationStatus.THINKING -> "Thinking..."
        GenerationStatus.ANALYZING_PROFILE -> "Analyzing your fitness profile..."
        GenerationStatus.GENERATING_PLAN -> "Generating your workout plan..."
        GenerationStatus.IDLE,
        GenerationStatus.COMPLETED,
        GenerationStatus.ERROR,
        GenerationStatus.CANCELLED -> ""
    }

    if (statusText.isBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = alpha))
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Accent,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun CommittingStepContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Accent,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.ai_committing_message),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessStepContent(
    committedPlan: UserWorkoutPlan?,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Success.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.ai_plan_created_success, committedPlan?.name ?: "Workout Plan"),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your calendar schedule is populated. You can track progress and start your daily workouts anytime.",
            style = MaterialTheme.typography.bodyMedium.copy(color = SecondaryText),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Ink
            )
        ) {
            Text(stringResource(R.string.common_done), fontWeight = FontWeight.Bold)
        }
    }
}

data class AiWorkoutSummary(
    val primaryFocus: String,
    val trainingApproach: String,
    val weeklyFrequency: String,
    val targetMuscleGroups: List<String>,
    val progressionStrategy: String,
    val recoveryRecommendations: String,
    val importantConsiderations: String
)

private fun buildAiWorkoutSummary(preview: AiWorkoutPlanPreview): AiWorkoutSummary {
    val totalTrainingDays = preview.trainingDaysPerWeek
    val totalRestDays = preview.restDaysPerWeek
    val duration = preview.durationWeeks

    // Deduplicate muscle groups targeted across session titles & exercise categories
    val muscleSet = linkedSetOf<String>()
    preview.sessions.forEach { session ->
        val titleLower = session.title.lowercase()
        if (titleLower.contains("push") || titleLower.contains("chest")) muscleSet.add("Chest & Triceps")
        if (titleLower.contains("pull") || titleLower.contains("back")) muscleSet.add("Back & Biceps")
        if (titleLower.contains("leg") || titleLower.contains("lower") || titleLower.contains("squat")) muscleSet.add("Legs & Glutes")
        if (titleLower.contains("core") || titleLower.contains("abs") || titleLower.contains("compression")) muscleSet.add("Core & Abs")
        if (titleLower.contains("shoulder") || titleLower.contains("handstand") || titleLower.contains("pike")) muscleSet.add("Shoulders & Scapula")
        if (titleLower.contains("planche")) muscleSet.add("Planche & Straight-Arm")
        if (titleLower.contains("front lever")) muscleSet.add("Front Lever & Lats")

        session.exercises.forEach { ex ->
            val cat = ex.category?.lowercase() ?: ""
            when {
                cat.contains("chest") || cat.contains("push") -> muscleSet.add("Chest")
                cat.contains("back") || cat.contains("pull") -> muscleSet.add("Back")
                cat.contains("shoulder") -> muscleSet.add("Shoulders")
                cat.contains("core") || cat.contains("ab") -> muscleSet.add("Core")
                cat.contains("leg") || cat.contains("squat") -> muscleSet.add("Legs")
                cat.contains("arm") || cat.contains("bicep") || cat.contains("tricep") -> muscleSet.add("Arms")
            }
        }
    }
    if (muscleSet.isEmpty()) {
        muscleSet.addAll(listOf("Upper Body", "Core", "Lower Body"))
    }

    val primaryFocus = if (!preview.description.isNullOrBlank()) {
        preview.description
    } else {
        "${preview.planName}: Progressive bodyweight strength development, movement quality, and calisthenics skill mastery."
    }

    val trainingApproach = "${preview.splitType.displayName} program across $duration weeks. Balances multi-joint compound strength exercises with targeted isometric skill practice."

    val weeklyFrequency = "$totalTrainingDays active training sessions and $totalRestDays dedicated recovery days per week."

    val progressionStrategy = "$duration-Week Progressive Overload: Focus on clean range of motion and consistent tempo in Weeks 1–2. Advance volume, leverage, or hold duration in Weeks 3–$duration as strength adapts."

    val restDayNames = preview.sessions.filter { it.isRestDay }.map { it.dayOfWeek.shortName }
    val recoveryRecommendations = if (restDayNames.isNotEmpty()) {
        "Take scheduled recovery on ${restDayNames.joinToString(", ")}. Prioritize 7–9 hours of sleep, adequate protein intake, and gentle mobility drills on off-days."
    } else {
        "Incorporate daily active recovery, hydration, and mobility drills to support high-frequency training volume."
    }

    val importantConsiderations = "Thoroughly warm up wrists, elbows, and rotator cuffs before every session. Prioritize strict form over volume; regress to an earlier variation if form breaks down."

    return AiWorkoutSummary(
        primaryFocus = primaryFocus,
        trainingApproach = trainingApproach,
        weeklyFrequency = weeklyFrequency,
        targetMuscleGroups = muscleSet.toList(),
        progressionStrategy = progressionStrategy,
        recoveryRecommendations = recoveryRecommendations,
        importantConsiderations = importantConsiderations
    )
}

sealed interface PlanRevealItem {
    data object PlanIntro : PlanRevealItem
    data class SessionHeader(val sessionIndex: Int) : PlanRevealItem
    data class Exercise(val sessionIndex: Int, val exerciseIndex: Int) : PlanRevealItem
    data class RestDay(val sessionIndex: Int) : PlanRevealItem
    data object Summary : PlanRevealItem
    data object Actions : PlanRevealItem
}

@Composable
private fun AiWorkoutSummaryCard(
    summary: AiWorkoutSummary,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "AI Workout Summary",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )
                }
                PillBadge("Program Overview", isAccent = true)
            }

            // Summary Section: Primary Focus
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "🎯 Primary Training Focus",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = summary.primaryFocus,
                    style = MaterialTheme.typography.bodySmall.copy(color = PrimaryText)
                )
            }

            // Summary Section: Training Approach
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "🔄 Training Approach",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = summary.trainingApproach,
                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                )
            }

            // Summary Section: Weekly Schedule & Frequency
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "📅 Weekly Schedule & Frequency",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = summary.weeklyFrequency,
                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                )
            }

            // Summary Section: Muscle Groups Targeted
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "💪 Targeted Muscle Groups",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    summary.targetMuscleGroups.forEach { muscle ->
                        PillBadge(text = muscle, isAccent = false)
                    }
                }
            }

            // Summary Section: Progression Strategy
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "📈 Progression & Overload Strategy",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = summary.progressionStrategy,
                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                )
            }

            // Summary Section: Recovery Recommendations
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "🛌 Recovery & Regeneration",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = summary.recoveryRecommendations,
                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                )
            }

            // Summary Section: Important Considerations
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "⚠️ Key Considerations & Safety",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = summary.importantConsiderations,
                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                )
            }
        }
    }
}

@Composable
private fun PlanOverviewCard(preview: AiWorkoutPlanPreview) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Accent.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = preview.splitType.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Accent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${preview.durationWeeks} Weeks",
                        style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
                    )
                }
            }

            Text(
                text = preview.planName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            )

            if (!preview.description.isNullOrBlank()) {
                Text(
                    text = preview.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                )
            }

            // Schedule summary row: weekly training days vs rest days vs duration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.loc_training_days_per_week, preview.trainingDaysPerWeek),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = stringResource(R.string.rest_days_per_week, preview.restDaysPerWeek),
                    style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
                )
            }
        }
    }
}

@Composable
private fun RevealableItem(
    visible: Boolean,
    isAlreadyRevealed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (isAlreadyRevealed) {
        if (visible) {
            Box(modifier = modifier) {
                content()
            }
        }
    } else {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(40)) + slideInVertically(initialOffsetY = { 12 }, animationSpec = tween(40)),
            modifier = modifier
        ) {
            content()
        }
    }
}

@Composable
private fun DaySelectorBar(
    preview: AiWorkoutPlanPreview,
    selectedDayFilter: Int?,
    onSelectDayFilter: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedDayFilter == null,
            onClick = { onSelectDayFilter(null) },
            label = { Text("All Days") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Accent.copy(alpha = 0.2f),
                selectedLabelColor = Accent,
                containerColor = Surface,
                labelColor = SecondaryText
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedDayFilter == null,
                borderColor = if (selectedDayFilter == null) Accent else Border
            )
        )
        preview.sessions.forEachIndexed { sIdx, session ->
            val isSelected = selectedDayFilter == sIdx
            FilterChip(
                selected = isSelected,
                onClick = { onSelectDayFilter(sIdx) },
                label = {
                    Text(
                        text = "${session.dayOfWeek.shortName}${if (session.isRestDay) " (Rest)" else ""}",
                        maxLines = 1
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent.copy(alpha = 0.2f),
                    selectedLabelColor = Accent,
                    containerColor = Surface,
                    labelColor = SecondaryText
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) Accent else Border
                )
            )
        }
        FilterChip(
            selected = selectedDayFilter == -1,
            onClick = { onSelectDayFilter(-1) },
            label = { Text("AI Summary") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Accent.copy(alpha = 0.2f),
                selectedLabelColor = Accent,
                containerColor = Surface,
                labelColor = SecondaryText
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedDayFilter == -1,
                borderColor = if (selectedDayFilter == -1) Accent else Border
            )
        )
    }
}

@Composable
private fun PreviewStepContent(
    preview: AiWorkoutPlanPreview,
    selectedDayIndex: Int,
    onSelectedDayIndexChange: (Int) -> Unit,
    modifyPrompt: String,
    onModifyPromptChange: (String) -> Unit,
    isModifying: Boolean,
    isCheckingConflict: Boolean,
    isCommitting: Boolean,
    conflictPlanName: String?,
    onApplyModifyPreset: (String) -> Unit,
    onModify: () -> Unit,
    onCommit: () -> Unit,
    onReset: () -> Unit,
    isCoachClient: Boolean,
    errorMessage: String?,
    hasAlreadyRevealed: Boolean = false,
    onPlanRevealCompleted: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Build ordered list of all sequential items to progressively reveal
    val allRevealItems = remember(preview) {
        buildList {
            add(PlanRevealItem.PlanIntro)
            preview.sessions.forEachIndexed { sIdx, session ->
                add(PlanRevealItem.SessionHeader(sIdx))
                if (session.isRestDay) {
                    add(PlanRevealItem.RestDay(sIdx))
                } else {
                    session.exercises.forEachIndexed { eIdx, _ ->
                        add(PlanRevealItem.Exercise(sIdx, eIdx))
                    }
                }
            }
            add(PlanRevealItem.Summary)
            add(PlanRevealItem.Actions)
        }
    }

    var isRevealed by rememberSaveable(preview.planName, preview.sessions.size) {
        mutableStateOf(hasAlreadyRevealed)
    }
    var currentStep by rememberSaveable(preview.planName, preview.sessions.size) {
        mutableIntStateOf(if (hasAlreadyRevealed || isRevealed) allRevealItems.size else 0)
    }
    var selectedDayFilter by rememberSaveable(preview.planName) {
        mutableStateOf<Int?>(null)
    }
    var shouldScrollToBottomOnDone by rememberSaveable(preview.planName) {
        mutableStateOf(!hasAlreadyRevealed && !isRevealed)
    }

    var containerTopOnScreen by remember { mutableFloatStateOf(0f) }
    var inlineBarTopOnScreen by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val isStickyVisible by remember {
        derivedStateOf {
            inlineBarTopOnScreen <= containerTopOnScreen
        }
    }

    // When progressive reveal completes, instantly scroll to the bottom
    LaunchedEffect(isRevealed) {
        if (isRevealed && shouldScrollToBottomOnDone) {
            shouldScrollToBottomOnDone = false
            delay(40)
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // Step-by-step progressive reveal animation sequence (5x faster)
    LaunchedEffect(preview, hasAlreadyRevealed) {
        if (hasAlreadyRevealed || isRevealed || currentStep >= allRevealItems.size) {
            currentStep = allRevealItems.size
            isRevealed = true
            return@LaunchedEffect
        }

        try {
            while (currentStep < allRevealItems.size) {
                val nextItem = allRevealItems[currentStep]
                val delayTime = when (nextItem) {
                    PlanRevealItem.PlanIntro -> 18L
                    is PlanRevealItem.SessionHeader -> 14L
                    is PlanRevealItem.RestDay -> 10L
                    is PlanRevealItem.Exercise -> 8L
                    PlanRevealItem.Summary -> 22L
                    PlanRevealItem.Actions -> 10L
                }
                delay(delayTime)
                currentStep++
            }
        } catch (_: CancellationException) {
            // Handled in finally block
        } finally {
            currentStep = allRevealItems.size
            isRevealed = true
            onPlanRevealCompleted()
            delay(40)
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                containerTopOnScreen = coordinates.positionInWindow().y
            }
    ) {
        // Scrollable workout plan content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isRevealed) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures {
                                currentStep = allRevealItems.size
                                isRevealed = true
                                selectedDayFilter = null
                                onPlanRevealCompleted()
                            }
                        }
                    } else Modifier
                )
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Initial Plan Introduction Card
            RevealableItem(
                visible = currentStep > allRevealItems.indexOf(PlanRevealItem.PlanIntro),
                isAlreadyRevealed = isRevealed
            ) {
                PlanOverviewCard(preview = preview)
            }

            // Active conflict warning if applicable
            if (!conflictPlanName.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = Warning)
                        Text(
                            text = stringResource(R.string.ai_active_conflict_banner, conflictPlanName),
                            color = Warning,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Inline Day Navigation Filter Chips (original position)
            if (currentStep > allRevealItems.indexOf(PlanRevealItem.PlanIntro) || isRevealed) {
                DaySelectorBar(
                    preview = preview,
                    selectedDayFilter = selectedDayFilter,
                    onSelectDayFilter = { selectedDayFilter = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            inlineBarTopOnScreen = coordinates.positionInWindow().y
                        }
                )
            }

        // 2. Sequential Workout Days and Progressively Revealed Exercises
        preview.sessions.forEachIndexed { sIdx, session ->
            val matchesFilter = selectedDayFilter == null || selectedDayFilter == sIdx
            val sessionHeaderIdx = allRevealItems.indexOf(PlanRevealItem.SessionHeader(sIdx))
            val isSessionVisible = currentStep > sessionHeaderIdx

            if (matchesFilter && isSessionVisible) {
                RevealableItem(
                    visible = true,
                    isAlreadyRevealed = isRevealed
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Elevated),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Session Day Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${session.dayOfWeek.displayName}: ${session.title}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText
                                    )
                                )

                                if (session.isRestDay) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Surface)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Rest Day",
                                            style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${session.exercises.size} Exercises",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Accent)
                                    )
                                }
                            }

                            if (session.isRestDay) {
                                val restDayIdx = allRevealItems.indexOf(PlanRevealItem.RestDay(sIdx))
                                val isRestVisible = currentStep > restDayIdx
                                RevealableItem(
                                    visible = isRestVisible,
                                    isAlreadyRevealed = isRevealed
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Surface)
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(SecondaryText.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Hotel, contentDescription = null, tint = SecondaryText)
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = stringResource(R.string.ai_rest_day_title),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = PrimaryText
                                                )
                                            )
                                            Text(
                                                text = stringResource(R.string.ai_rest_day_desc),
                                                style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Progressive exercises within this workout day
                                session.exercises.forEachIndexed { eIdx, exercise ->
                                    val exerciseIdx = allRevealItems.indexOf(PlanRevealItem.Exercise(sIdx, eIdx))
                                    val isExerciseVisible = currentStep > exerciseIdx
                                    RevealableItem(
                                        visible = isExerciseVisible,
                                        isAlreadyRevealed = isRevealed
                                    ) {
                                        ExercisePreviewItem(index = eIdx + 1, exercise = exercise)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Dedicated AI Workout Summary Section (appears after all workout days)
        val summaryIdx = allRevealItems.indexOf(PlanRevealItem.Summary)
        val isSummaryVisible = (selectedDayFilter == null || selectedDayFilter == -1) && (currentStep > summaryIdx)
        RevealableItem(
            visible = isSummaryVisible,
            isAlreadyRevealed = isRevealed
        ) {
            AiWorkoutSummaryCard(summary = buildAiWorkoutSummary(preview))
        }

        // 4. Follow-up modification and completion action controls
        val isActionsVisible = isRevealed || currentStep >= allRevealItems.size
        RevealableItem(
            visible = isActionsVisible,
            isAlreadyRevealed = isRevealed
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Natural Language Follow-up Modification Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = Elevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_modify_label),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                        )

                        // Quick modification suggestion pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                stringResource(R.string.ai_modify_preset_replace_legs),
                                stringResource(R.string.ai_modify_preset_add_planche_sets),
                                stringResource(R.string.ai_modify_preset_shorter),
                                stringResource(R.string.ai_modify_preset_core)
                            ).forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Surface)
                                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                                        .clickable { onApplyModifyPreset(preset) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryText)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = modifyPrompt,
                                onValueChange = onModifyPromptChange,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.ai_modify_hint),
                                        color = SecondaryText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PrimaryText,
                                    unfocusedTextColor = PrimaryText,
                                    focusedBorderColor = Accent,
                                    unfocusedBorderColor = Border,
                                    focusedContainerColor = Surface,
                                    unfocusedContainerColor = Surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Button(
                                onClick = onModify,
                                enabled = modifyPrompt.isNotBlank() && !isModifying,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Ink
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                if (isModifying) {
                                    CircularProgressIndicator(
                                        color = Ink,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(stringResource(R.string.ai_modify_button), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = Error)
                            Text(
                                text = errorMessage,
                                color = Error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Action Buttons: Commit and Start Over
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onReset,
                        enabled = !isCheckingConflict && !isCommitting && !isModifying,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.ai_start_over), color = SecondaryText)
                    }

                    Button(
                        onClick = onCommit,
                        enabled = !isCheckingConflict && !isCommitting && !isModifying,
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Ink
                        )
                    ) {
                        if (isCheckingConflict) {
                            CircularProgressIndicator(
                                color = Ink,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCoachClient) stringResource(R.string.ai_commit_coach_button) else stringResource(R.string.ai_commit_button),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Sticky Day Navigation Filter Chips (Appears on top only when scrolling down past the original bar)
    AnimatedVisibility(
        visible = isStickyVisible && (currentStep > allRevealItems.indexOf(PlanRevealItem.PlanIntro) || isRevealed),
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(120)),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ink)
                .padding(bottom = 6.dp)
        ) {
            DaySelectorBar(
                preview = preview,
                selectedDayFilter = selectedDayFilter,
                onSelectDayFilter = { selectedDayFilter = it }
            )
        }
    }
}
}

@Composable
private fun SessionDetailCard(session: AiWorkoutSessionPreview) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${session.dayOfWeek.displayName}: ${session.title}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                )

                if (session.isRestDay) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Rest Day",
                            style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
                        )
                    }
                } else {
                    Text(
                        text = "${session.exercises.size} Exercises",
                        style = MaterialTheme.typography.labelSmall.copy(color = Accent)
                    )
                }
            }

            if (session.isRestDay) {
                // Rest Day View
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SecondaryText.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Hotel, contentDescription = null, tint = SecondaryText)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.ai_rest_day_title),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                        )
                        Text(
                            text = stringResource(R.string.ai_rest_day_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                        )
                    }
                }
            } else {
                // Exercise List
                session.exercises.forEachIndexed { index, exercise ->
                    ExercisePreviewItem(index = index + 1, exercise = exercise)
                }
            }
        }
    }
}

@Composable
private fun ExercisePreviewItem(
    index: Int,
    exercise: AiWorkoutExercisePreview
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Text(
                        text = "$index.",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = SecondaryText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!exercise.category.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Elevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = exercise.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SecondaryText,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Prescription badges: Sets x Reps/Hold • Rest
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Accent.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = exercise.prescription,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Elevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${exercise.restSeconds}s rest",
                            style = MaterialTheme.typography.labelSmall.copy(color = SecondaryText)
                        )
                    }
                }

                if (exercise.requiresReview) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Warning.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Review",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Warning,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Coaching notes / cues
            if (!exercise.notes.isNullOrBlank()) {
                Text(
                    text = "💡 ${exercise.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
