package com.calisthenics.ai.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.runtime.rememberCoroutineScope
import com.calisthenics.ai.ui.media.ImageSelectorSection
import com.calisthenics.ai.ui.media.ExerciseMediaCollectionSection
import com.calisthenics.ai.ui.media.MediaUploadManager
import com.calisthenics.ai.ui.media.MediaUploadState
import com.calisthenics.ai.ui.media.VideoSelectorSection
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.model.ExerciseMediaType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.calisthenics.ai.domain.model.AdminExercise
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.ui.theme.Ink

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdminMediaUploadEntryPoint {
    fun mediaUploadManager(): MediaUploadManager
}

private val EXERCISE_CATEGORIES = listOf("PUSH", "PULL", "CORE", "LEGS", "SKILL", "ISOMETRIC")
private val DIFFICULTY_LEVELS = listOf("BEGINNER", "INTERMEDIATE", "ADVANCED", "ELITE")

@Composable
fun AdminExercisesScreen(
    onBack: () -> Unit,
    onCreateExercise: () -> Unit,
    onEditExercise: (Long) -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var search by rememberSaveable { mutableStateOf("") }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var difficultyFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var activeFilter by rememberSaveable { mutableStateOf<Boolean?>(null) }

    var pendingDeleteExercise by remember { mutableStateOf<AdminExercise?>(null) }
    var conflictMessage by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(search, categoryFilter, difficultyFilter, activeFilter, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            kotlinx.coroutines.delay(300)
            viewModel.loadExercises(
                page = 0,
                search = search,
                category = categoryFilter,
                difficulty = difficultyFilter,
                active = activeFilter
            )
            kotlinx.coroutines.awaitCancellation()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 10.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text("Exercises", style = MaterialTheme.typography.titleLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    viewModel.loadExercises(0, search, categoryFilter, difficultyFilter, activeFilter)
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                Button(onClick = onCreateExercise) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        // Search Input
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            label = { Text("Search exercises by name, code, or muscle") },
            singleLine = true
        )

        // Filters
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = categoryFilter == null,
                onClick = { categoryFilter = null },
                label = { Text("All Categories") }
            )
            EXERCISE_CATEGORIES.forEach { cat ->
                FilterChip(
                    selected = categoryFilter == cat,
                    onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                    label = { Text(cat) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = difficultyFilter == null,
                onClick = { difficultyFilter = null },
                label = { Text("All Difficulties") }
            )
            DIFFICULTY_LEVELS.forEach { diff ->
                FilterChip(
                    selected = difficultyFilter == diff,
                    onClick = { difficultyFilter = if (difficultyFilter == diff) null else diff },
                    label = { Text(diff) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeFilter == null,
                onClick = { activeFilter = null },
                label = { Text("All Status") }
            )
            FilterChip(
                selected = activeFilter == true,
                onClick = { activeFilter = if (activeFilter == true) null else true },
                label = { Text("Active") }
            )
            FilterChip(
                selected = activeFilter == false,
                onClick = { activeFilter = if (activeFilter == false) null else false },
                label = { Text("Inactive") }
            )
        }

        // Error and Status Messages
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp))
        }
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
            LaunchedEffect(it) {
                kotlinx.coroutines.delay(4_000)
                viewModel.clearMessage()
            }
        }

        if (state.isLoading && state.exercisePage == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
        }

        val page = state.exercisePage
        if (page != null) {
            if (page.content.isEmpty() && !state.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No exercises found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(page.content, key = { it.id }) { exercise ->
                        AdminExerciseCard(
                            exercise = exercise,
                            onEdit = { onEditExercise(exercise.id) },
                            onToggleStatus = { viewModel.updateExerciseStatus(exercise.id, !exercise.active) },
                            onDelete = { pendingDeleteExercise = exercise }
                        )
                    }
                }
            }
            AdminPageControls(page, onPageChange = {
                viewModel.loadExercises(it, search, categoryFilter, difficultyFilter, activeFilter)
            })
        } else {
            Spacer(Modifier.weight(1f))
        }
    }

    // Delete / Deactivate Safety Dialog
    pendingDeleteExercise?.let { exercise ->
        if (exercise.canDelete) {
            AlertDialog(
                onDismissRequest = { pendingDeleteExercise = null },
                title = { Text("Delete exercise") },
                text = { Text("Are you sure you want to permanently delete '${exercise.name}'? This cannot be undone.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            val id = exercise.id
                            pendingDeleteExercise = null
                            viewModel.deleteExercise(
                                exerciseId = id,
                                softDelete = false,
                                onSuccess = {},
                                onConflict = { conflictMessage = it }
                            )
                        }
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteExercise = null }) { Text("Cancel") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { pendingDeleteExercise = null },
                title = { Text("Exercise in use") },
                text = {
                    Text(
                        "Cannot permanently delete '${exercise.name}' because it is referenced in " +
                                "${exercise.planReferencesCount} workout plan(s) and ${exercise.sessionReferencesCount} session(s).\n\n" +
                                "To protect workout history from corruption or missing data, permanent deletion is disabled. Would you like to deactivate this exercise instead?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = exercise.id
                            pendingDeleteExercise = null
                            viewModel.deleteExercise(
                                exerciseId = id,
                                softDelete = true,
                                onSuccess = {},
                                onConflict = { conflictMessage = it }
                            )
                        }
                    ) { Text("Deactivate instead") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteExercise = null }) { Text("Cancel") }
                }
            )
        }
    }

    // Conflict error dialog
    conflictMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { conflictMessage = null },
            title = { Text("Cannot delete") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { conflictMessage = null }) { Text("OK") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminExerciseCard(
    exercise: AdminExercise,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (exercise.active) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (!exercise.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = exercise.imageUrl,
                            contentDescription = exercise.name,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Code: ${exercise.code}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = if (exercise.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (exercise.active) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (exercise.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(exercise.category) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(exercise.difficulty) }
                )
                if (!exercise.equipment.isNullOrBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(exercise.equipment) }
                    )
                }
            }

            if (!exercise.targetMuscles.isNullOrBlank()) {
                Text(
                    "Muscles: ${exercise.targetMuscles}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!exercise.description.isNullOrBlank()) {
                Text(
                    exercise.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }

            // References info
            if (!exercise.canDelete) {
                Text(
                    "Used in ${exercise.planReferencesCount} plans · ${exercise.sessionReferencesCount} sessions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Card Action Buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(if (exercise.active) "Deactivate" else "Activate")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

    }
}

@Composable
fun AdminExerciseFormScreen(
    exerciseId: Long? = null,
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isEditMode = exerciseId != null

    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(EXERCISE_CATEGORIES.first()) }
    var difficulty by rememberSaveable { mutableStateOf(DIFFICULTY_LEVELS.first()) }
    var targetMuscles by rememberSaveable { mutableStateOf("") }
    var equipment by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var instructions by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var videoUrl by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(true) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUploadState by remember { mutableStateOf<MediaUploadState>(MediaUploadState.Idle) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoUploadState by remember { mutableStateOf<MediaUploadState>(MediaUploadState.Idle) }
    var mediaItems by remember { mutableStateOf<List<ExerciseMedia>>(emptyList()) }
    var isUploadingAdditionalMedia by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaUploadManager = remember {
        val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            AdminMediaUploadEntryPoint::class.java
        )
        entryPoint.mediaUploadManager()
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(exerciseId) {
        if (exerciseId != null) {
            viewModel.loadExercise(exerciseId)
        }
    }

    LaunchedEffect(state.exercise) {
        if (isEditMode && !isInitialized && state.exercise != null && state.exercise?.id == exerciseId) {
            val ex = state.exercise!!
            name = ex.name
            code = ex.code
            category = ex.category
            difficulty = ex.difficulty
            targetMuscles = ex.targetMuscles ?: ""
            equipment = ex.equipment ?: ""
            description = ex.description ?: ""
            instructions = ex.instructions ?: ""
            imageUrl = ex.imageUrl ?: ""
            videoUrl = ex.videoUrl ?: ""
            mediaItems = ex.mediaItems.filterNot { item ->
                (item.type == ExerciseMediaType.IMAGE && item.url == ex.imageUrl) ||
                    (item.type == ExerciseMediaType.VIDEO && item.url == ex.videoUrl)
            }
            active = ex.active
            isInitialized = true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (isEditMode) "Edit Exercise" else "Create Exercise",
                style = MaterialTheme.typography.titleLarge
            )
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        // Basic Information Section
        Text("Basic Information", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (it.isNotBlank()) nameError = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Exercise name *") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            singleLine = true
        )

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Code (optional)") },
            supportingText = { Text("Leave empty to auto-generate from name") },
            singleLine = true
        )

        // Classification Section
        Text("Classification", style = MaterialTheme.typography.titleMedium)

        Text("Category *", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EXERCISE_CATEGORIES.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat) }
                )
            }
        }

        Text("Difficulty Level *", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DIFFICULTY_LEVELS.forEach { diff ->
                FilterChip(
                    selected = difficulty == diff,
                    onClick = { difficulty = diff },
                    label = { Text(diff) }
                )
            }
        }

        OutlinedTextField(
            value = targetMuscles,
            onValueChange = { targetMuscles = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Target muscles") },
            placeholder = { Text("e.g. Chest, Anterior Deltoids, Triceps") },
            singleLine = true
        )

        OutlinedTextField(
            value = equipment,
            onValueChange = { equipment = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Equipment") },
            placeholder = { Text("e.g. Pull-up bar, Parallettes, None") },
            singleLine = true
        )

        // Description & Instructions
        Text("Details & Execution", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") },
            minLines = 2,
            maxLines = 4
        )

        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Execution steps & instructions") },
            minLines = 3,
            maxLines = 6
        )

        // Media Section
        Text("Media", style = MaterialTheme.typography.titleMedium)

        ImageSelectorSection(
            currentImageUrl = imageUrl.ifBlank { null },
            selectedUri = selectedImageUri,
            uploadState = imageUploadState,
            onSelectImage = { uri ->
                selectedImageUri = uri
                imageUploadState = MediaUploadState.Uploading(0f, "Đang tải ảnh lên...")
                coroutineScope.launch {
                    val result = mediaUploadManager.uploadMedia(uri)
                    result.onSuccess { uploadedUrl ->
                        imageUrl = uploadedUrl
                        if (mediaItems.none { it.url == uploadedUrl }) {
                            mediaItems = mediaItems + ExerciseMedia(
                                type = ExerciseMediaType.IMAGE,
                                url = uploadedUrl,
                                displayOrder = (mediaItems.maxOfOrNull { it.displayOrder } ?: -1) + 1
                            )
                        }
                        imageUploadState = MediaUploadState.Success(uploadedUrl)
                    }.onFailure { err ->
                        imageUploadState = MediaUploadState.Error(err.localizedMessage ?: "Tải ảnh thất bại")
                    }
                }
            },
            onRemoveImage = {
                selectedImageUri = null
                imageUrl = ""
                imageUploadState = MediaUploadState.Idle
            }
        )

        VideoSelectorSection(
            currentVideoUrl = videoUrl.ifBlank { null },
            selectedUri = selectedVideoUri,
            uploadState = videoUploadState,
            uploadManager = mediaUploadManager,
            onSelectVideo = { uri ->
                selectedVideoUri = uri
                videoUploadState = MediaUploadState.Uploading(0f, "Đang tải video lên...")
                coroutineScope.launch {
                    val result = mediaUploadManager.uploadMedia(uri)
                    result.onSuccess { uploadedUrl ->
                        videoUrl = uploadedUrl
                        if (mediaItems.none { it.url == uploadedUrl }) {
                            mediaItems = mediaItems + ExerciseMedia(
                                type = ExerciseMediaType.VIDEO,
                                url = uploadedUrl,
                                displayOrder = (mediaItems.maxOfOrNull { it.displayOrder } ?: -1) + 1
                            )
                        }
                        videoUploadState = MediaUploadState.Success(uploadedUrl)
                    }.onFailure { err ->
                        videoUploadState = MediaUploadState.Error(err.localizedMessage ?: "Tải video thất bại")
                    }
                }
            },
            onRemoveVideo = {
                selectedVideoUri = null
                videoUrl = ""
                videoUploadState = MediaUploadState.Idle
            }
        )

        ExerciseMediaCollectionSection(
            mediaItems = mediaItems,
            uploadManager = mediaUploadManager,
            exerciseId = exerciseId,
            onMediaItemsChange = { mediaItems = it },
            onUploadStateChange = { isUploadingAdditionalMedia = it }
        )

        // Active Switch
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Active status", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Inactive exercises are hidden from client workouts but preserved in history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = active,
                onCheckedChange = { active = it }
            )
        }

        Spacer(Modifier.height(10.dp))

        // Save Button
        Button(
            onClick = {
                var hasError = false
                if (name.isBlank()) {
                    nameError = "Exercise name is required"
                    hasError = true
                }
                if (imageUploadState is MediaUploadState.Uploading ||
                    videoUploadState is MediaUploadState.Uploading ||
                    isUploadingAdditionalMedia
                ) {
                    hasError = true
                }

                if (!hasError) {
                    if (isEditMode) {
                        viewModel.updateExercise(
                            exerciseId = exerciseId!!,
                            name = name,
                            code = code.ifBlank { null },
                            category = category,
                            difficulty = difficulty,
                            description = description.ifBlank { null },
                            targetMuscles = targetMuscles.ifBlank { null },
                            instructions = instructions.ifBlank { null },
                            imageUrl = imageUrl.ifBlank { null },
                            videoUrl = videoUrl.ifBlank { null },
                            equipment = equipment.ifBlank { null },
                            active = active,
                            mediaItems = mediaItems,
                            onSuccess = onBack
                        )
                    } else {
                        viewModel.createExercise(
                            name = name,
                            code = code.ifBlank { null },
                            category = category,
                            difficulty = difficulty,
                            description = description.ifBlank { null },
                            targetMuscles = targetMuscles.ifBlank { null },
                            instructions = instructions.ifBlank { null },
                            imageUrl = imageUrl.ifBlank { null },
                            videoUrl = videoUrl.ifBlank { null },
                            equipment = equipment.ifBlank { null },
                            active = active,
                            mediaItems = mediaItems,
                            onSuccess = onBack
                        )
                    }
                }
            },
            enabled = !state.isSavingExercise &&
                    imageUploadState !is MediaUploadState.Uploading &&
                    videoUploadState !is MediaUploadState.Uploading &&
                    !isUploadingAdditionalMedia,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (state.isSavingExercise) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(if (isEditMode) "Save Changes" else "Create Exercise")
            }
        }
    }
}

@Composable
private fun AdminPageControls(page: AdminPage<*>, onPageChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(enabled = page.page > 0, onClick = { onPageChange(page.page - 1) }) { Text("Previous") }
        Text("${page.page + 1} / ${page.totalPages.coerceAtLeast(1)}")
        TextButton(enabled = page.hasNext, onClick = { onPageChange(page.page + 1) }) { Text("Next") }
    }
}
