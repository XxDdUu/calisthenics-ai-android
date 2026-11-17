package com.calisthenics.ai.ui.progression

import com.calisthenics.ai.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.media.ExerciseMediaBorder
import com.calisthenics.ai.ui.media.ExerciseMediaPreview
import com.calisthenics.ai.ui.media.ExerciseStoryViewer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionSelectionScreen(
    onProgressionConfirmed: (Long, String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: ProgressionSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.choose_progression)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (uiState.selectedProgression != null) {
                Button(
                    onClick = {
                        val selected = uiState.selectedProgression
                        if (selected != null) {
                            onProgressionConfirmed(selected.id, selected.name)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Continue with ${uiState.selectedProgression?.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingView(
                    message = stringResource(R.string.loc_loading_progressions),
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.error != null -> {
                ErrorView(
                    message = uiState.error ?: "Failed to load progressions",
                    onRetry = { viewModel.loadProgressions() },
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.progressions) { progression ->
                        ProgressionCard(
                            progression = progression,
                            isSelected = progression.id == uiState.selectedProgression?.id,
                            onSelect = { viewModel.selectProgression(progression) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressionCard(
    progression: Exercise,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    var storyStartIndex by rememberSaveable(progression.id) { mutableStateOf<Int?>(null) }

    ExerciseMediaBorder(
        hasMedia = progression.mediaItems.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
        )
        {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onSelect,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = progression.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = progression.difficulty.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (progression.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = progression.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 44.dp)
                )
            }

            if (progression.instructions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Key form: ${progression.instructions}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 44.dp)
                )
            }

            if (progression.mediaItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ExerciseMediaPreview(
                    mediaItems = progression.mediaItems,
                    onMediaClick = { storyStartIndex = it }
                )
            }
        }
        }
    }

    storyStartIndex?.let { index ->
        ExerciseStoryViewer(
        mediaItems = progression.mediaItems,
        initialIndex = index,
        onDismiss = { storyStartIndex = null }
        )
    }
}
