package com.calisthenics.ai.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.R
import com.calisthenics.ai.ui.locale.AppLanguage
import com.calisthenics.ai.ui.locale.LanguageViewModel
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Surface
import com.calisthenics.ai.ui.theme.ThemeMode
import com.calisthenics.ai.ui.theme.ThemeViewModel
import com.calisthenics.ai.domain.model.WorkoutSource
import com.calisthenics.ai.data.local.SetLoggingMode
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel(),
    languageViewModel: LanguageViewModel = hiltViewModel()
) {
    val currentTheme by viewModel.themeMode.collectAsState()
    val currentLanguage by languageViewModel.language.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val todayWorkoutViewModel: TodayWorkoutSettingsViewModel = hiltViewModel()
    val todayWorkoutPriority by todayWorkoutViewModel.priority.collectAsState()
    val workoutExpViewModel: WorkoutExperienceSettingsViewModel = hiltViewModel()
    val bubbleEnabled by workoutExpViewModel.bubbleEnabled.collectAsState()
    val loggingMode by workoutExpViewModel.loggingMode.collectAsState()
    var selectedTab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsTab(
                    title = stringResource(R.string.appearance),
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                SettingsTab(
                    title = stringResource(R.string.today_workout),
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                SettingsTab(
                    title = stringResource(R.string.workout_experience),
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.appearance),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.theme_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
            }

            item {
                LanguageOptionCard(
                    language = currentLanguage,
                    onClick = { showLanguageDialog = true }
                )
            }

            item {
                ThemeOptionCard(
                    title = stringResource(R.string.theme_light),
                    subtitle = stringResource(R.string.theme_light_description),
                    icon = Icons.Default.LightMode,
                    iconTint = Color(0xFFF59E0B),
                    isSelected = currentTheme == ThemeMode.LIGHT,
                    previewColors = listOf(Color(0xFFF8F9FA), Color(0xFFFFFFFF), Color(0xFF16A34A)),
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                )
            }

            item {
                ThemeOptionCard(
                    title = stringResource(R.string.theme_dark),
                    subtitle = stringResource(R.string.theme_dark_description),
                    icon = Icons.Default.DarkMode,
                    iconTint = Color(0xFFB7F56A),
                    isSelected = currentTheme == ThemeMode.DARK,
                    previewColors = listOf(Color(0xFF0D0D0D), Color(0xFF171717), Color(0xFFB7F56A)),
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                )
            }

            item {
                ThemeOptionCard(
                    title = stringResource(R.string.theme_dark_v2),
                    subtitle = stringResource(R.string.theme_dark_v2_description),
                    icon = Icons.Default.DarkMode,
                    iconTint = Color(0xFF8FA9A1),
                    isSelected = currentTheme == ThemeMode.DARK_V2,
                    previewColors = listOf(Color(0xFF181A1B), Color(0xFF202324), Color(0xFF8FA9A1)),
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK_V2) }
                )
            }

            item {
                ThemeOptionCard(
                    title = stringResource(R.string.theme_neutral),
                    subtitle = stringResource(R.string.theme_neutral_description),
                    icon = Icons.Default.Contrast,
                    iconTint = Color(0xFF6B7280),
                    isSelected = currentTheme == ThemeMode.NEUTRAL,
                    previewColors = listOf(Color(0xFFE5E7EB), Color(0xFFF3F4F6), Color(0xFF2E7D32)),
                    onClick = { viewModel.setThemeMode(ThemeMode.NEUTRAL) }
                )
            }
        }
    }
    1 -> {
        TodayWorkoutPreferenceContent(
            priority = todayWorkoutPriority,
            onPriorityChanged = todayWorkoutViewModel::setPriority
        )
    }
    else -> {
        WorkoutExperienceSettingsContent(
            bubbleEnabled = bubbleEnabled,
            onBubbleEnabledChanged = workoutExpViewModel::setBubbleEnabled,
            loggingMode = loggingMode,
            onLoggingModeChanged = workoutExpViewModel::setLoggingMode
        )
    }
}
}
}

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            selectedLanguage = currentLanguage,
            onLanguageSelected = { languageViewModel.setLanguage(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun LanguageOptionCard(
    language: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PrimaryText
                )
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.language_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            Icon(Icons.Default.Check, contentDescription = null, tint = Accent)
        }
    }
}

@Composable
private fun LanguageSelectionDialog(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppLanguage.values().forEach { language ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onLanguageSelected(language); onDismiss() }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == selectedLanguage,
                            onClick = { onLanguageSelected(language); onDismiss() },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent)
                        )
                        Text(language.nativeName, style = MaterialTheme.typography.bodyLarge)
                        if (language == selectedLanguage) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.language_selected), style = MaterialTheme.typography.labelMedium, color = Accent)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun SettingsTab(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Accent else Border
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Surface else Elevated
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) PrimaryText else SecondaryText
            )
        }
    }
}

    @Composable
    private fun TodayWorkoutPreferenceContent(
        priority: WorkoutSource,
        onPriorityChanged: (WorkoutSource) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.today_workout), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = PrimaryText)
            Text(
                stringResource(R.string.today_workout_description),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
            TodayWorkoutPriorityCard(
                title = stringResource(R.string.coach_plan_first),
                subtitle = stringResource(R.string.coach_plan_first_description),
                selected = priority == WorkoutSource.COACH,
                onClick = { onPriorityChanged(WorkoutSource.COACH) }
            )
            TodayWorkoutPriorityCard(
                title = stringResource(R.string.personal_plan_first),
                subtitle = stringResource(R.string.personal_plan_first_description),
                selected = priority == WorkoutSource.PERSONAL,
                onClick = { onPriorityChanged(WorkoutSource.PERSONAL) }
            )
        }
    }

    @Composable
    private fun TodayWorkoutPriorityCard(
        title: String,
        subtitle: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) Accent else Border
            ),
            colors = CardDefaults.cardColors(containerColor = Elevated)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = PrimaryText)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = SecondaryText)
                )
            }
        }
    }
@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    previewColors: List<Color>,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Accent else Border,
        animationSpec = tween(durationMillis = 200),
        label = "ThemeOptionBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Elevated
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Surface)
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    previewColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Border, CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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
private fun WorkoutExperienceSettingsContent(
    bubbleEnabled: Boolean,
    onBubbleEnabledChanged: (Boolean) -> Unit,
    loggingMode: SetLoggingMode,
    onLoggingModeChanged: (SetLoggingMode) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.workout_experience),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.set_logging_mode_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(containerColor = Elevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.unfinished_workout_bubble),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.unfinished_workout_bubble_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = bubbleEnabled,
                        onCheckedChange = onBubbleEnabledChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryText,
                            checkedTrackColor = Accent,
                            uncheckedTrackColor = Surface
                        )
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.set_logging_mode),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = PrimaryText,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            TodayWorkoutPriorityCard(
                title = stringResource(R.string.step_by_step_sets),
                subtitle = stringResource(R.string.step_by_step_sets_desc),
                selected = loggingMode == SetLoggingMode.STEP_BY_STEP,
                onClick = { onLoggingModeChanged(SetLoggingMode.STEP_BY_STEP) }
            )
        }

        item {
            TodayWorkoutPriorityCard(
                title = stringResource(R.string.show_all_sets),
                subtitle = stringResource(R.string.show_all_sets_desc),
                selected = loggingMode == SetLoggingMode.SHOW_ALL,
                onClick = { onLoggingModeChanged(SetLoggingMode.SHOW_ALL) }
            )
        }
    }
}
