package com.calisthenics.ai.ui.profile

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileType
import com.calisthenics.ai.domain.model.ConnectedCoachSummary
import com.calisthenics.ai.domain.model.UserUsage
import com.calisthenics.ai.ui.auth.GoogleAuthEntryPoint
import com.calisthenics.ai.ui.auth.GoogleAuthManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.EndRelationshipDialog
import com.calisthenics.ai.ui.components.GenderEditDialog
import com.calisthenics.ai.ui.components.GenderOptionSelector
import com.calisthenics.ai.ui.components.GenderProfileCard
import com.calisthenics.ai.ui.components.InfoRow
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.components.UserAvatar
import com.calisthenics.ai.ui.components.localizedLabel
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onSwitchAccount: () -> Unit = onLogout,
    onOpenSettings: () -> Unit = {},
    onViewCoach: (Long) -> Unit = {},
    onDiscoverCoaches: () -> Unit = {},
    onOpenChat: (Long, Long, String, String?) -> Unit = { _, _, _, _ -> },
    onViewPlan: (Long) -> Unit = {},
    onBrowsePlans: () -> Unit = {},
    onViewClient: (Long) -> Unit = {},
    onNavigateToBodyMetrics: () -> Unit = {},
    googleAuthManager: GoogleAuthManager? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedAvatarUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var isPreparingAvatar by remember { mutableStateOf(false) }
    var hasPendingAvatarUpload by remember { mutableStateOf(false) }
    var avatarPreviewError by remember { mutableStateOf<String?>(null) }
    val connectivityManager = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var isOnline by remember { mutableStateOf(hasInternetConnection(connectivityManager)) }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.clearAvatarUploadState()
            selectedAvatarUri = uri
            avatarPreviewError = null
            hasPendingAvatarUpload = false
        }
    }
    val launchAvatarPicker = {
        avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val authManager = googleAuthManager ?: remember(context) {
        try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                GoogleAuthEntryPoint::class.java
            ).googleAuthManager()
        } catch (_: Throwable) {
            GoogleAuthManager(context.applicationContext)
        }
    }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = hasInternetConnection(connectivityManager)
            }

            override fun onLost(network: Network) {
                isOnline = hasInternetConnection(connectivityManager)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    LaunchedEffect(state.avatarUploadSucceeded) {
        if (state.avatarUploadSucceeded) {
            selectedAvatarUri = null
            hasPendingAvatarUpload = false
            avatarPreviewError = null
        }
    }
    LaunchedEffect(state.avatarUploadError) {
        if (state.avatarUploadError != null) {
            hasPendingAvatarUpload = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var hasResumedOnce by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasResumedOnce) {
                    viewModel.loadUserUsage(forceRefresh = true)
                } else {
                    hasResumedOnce = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var confirmingRelationshipId by remember { mutableStateOf<Long?>(null) }

    EndRelationshipDialog(
        isOpen = confirmingRelationshipId != null,
        isLoading = confirmingRelationshipId != null && confirmingRelationshipId in state.busyRelationshipIds,
        onConfirm = {
            confirmingRelationshipId?.let { id ->
                viewModel.terminateRelationship(id) { success ->
                    if (success) {
                        confirmingRelationshipId = null
                    }
                }
            }
        },
        onDismiss = { confirmingRelationshipId = null }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp, bottom = FloatingBottomBarDefaults.ContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.profile),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = SecondaryText
                    )
                }
                IconButton(onClick = { viewModel.logout(); onLogout() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.sign_out),
                        tint = SecondaryText
                    )
                }
            }
        }

        // Tab Switcher (Athlete vs Coach)
        TabRow(
            selectedTabIndex = if (state.activeTab == ProfileTab.CLIENT) 0 else 1,
            containerColor = Elevated,
            contentColor = Accent,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (state.activeTab == ProfileTab.CLIENT) 0 else 1]),
                    color = Accent
                )
            }
        ) {
            Tab(
                selected = state.activeTab == ProfileTab.CLIENT,
                onClick = { viewModel.selectTab(ProfileTab.CLIENT) },
                text = {
                    Text(
                        stringResource(R.string.loc_athlete_profile),
                        fontWeight = if (state.activeTab == ProfileTab.CLIENT) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.activeTab == ProfileTab.CLIENT) Accent else SecondaryText
                    )
                }
            )
            Tab(
                selected = state.activeTab == ProfileTab.COACH,
                onClick = { viewModel.selectTab(ProfileTab.COACH) },
                text = {
                    Text(
                        stringResource(R.string.loc_coach_profile),
                        fontWeight = if (state.activeTab == ProfileTab.COACH) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.activeTab == ProfileTab.COACH) Accent else SecondaryText
                    )
                }
            )
        }

        // Feedback Banner
        val feedbackMessage = state.feedbackResId?.let { stringResource(it) } ?: state.feedback
        feedbackMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearFeedback() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(msg, color = Accent, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.loc_dismiss), tint = Accent, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Main Tab Content
        if (state.isLoading && state.clientProfile == null && state.coachProfile == null) {
            Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            when (state.activeTab) {
                ProfileTab.CLIENT -> {
                    ClientProfileView(
                        profile = state.clientProfile,
                        mediaList = state.athleteMedia,
                        isMediaLoading = state.isMediaLoading,
                        isMediaUploading = state.isMediaUploading,
                        onUploadMedia = { uris -> viewModel.uploadMedia(uris, ProfileType.ATHLETE) },
                        onDeleteMedia = { id -> viewModel.deleteMedia(id, ProfileType.ATHLETE) },
                        onReorderMedia = { ids -> viewModel.reorderMedia(ids, ProfileType.ATHLETE) },
                        onSetCover = { id -> viewModel.setPrimaryCover(id, ProfileType.ATHLETE) },
                        busyChatUserIds = state.busyChatUserIds,
                        busyRelationshipIds = state.busyRelationshipIds,
                        onChangeAvatar = launchAvatarPicker,
                        isAvatarUploading = state.isUploadingAvatar,
                        userUsage = state.userUsage,
                        isUsageLoading = state.isUsageLoading,
                        usageError = state.usageError,
                        isUsageOffline = state.isUsageOffline,
                        onRefreshUsage = { viewModel.loadUserUsage(forceRefresh = true) },
                        onEdit = viewModel::startEditingClient,
                        onEditGender = viewModel::startEditingGender,
                        onViewCoach = onViewCoach,
                        onDiscoverCoaches = onDiscoverCoaches,
                        onOpenChat = { coachId, name, avatar ->
                            viewModel.startChat(coachId) { convId ->
                                onOpenChat(convId, coachId, name, avatar)
                            }
                        },
                        onViewPlan = onViewPlan,
                        onBrowsePlans = onBrowsePlans,
                        onNavigateToBodyMetrics = onNavigateToBodyMetrics,
                        onEndRelationship = { confirmingRelationshipId = it }
                    )
                }
                ProfileTab.COACH -> {
                    CoachProfileView(
                        profile = state.coachProfile,
                        fallbackGender = state.clientProfile?.gender,
                        mediaList = state.coachMedia,
                        isMediaLoading = state.isMediaLoading,
                        isMediaUploading = state.isMediaUploading,
                        onUploadMedia = { uris -> viewModel.uploadMedia(uris, ProfileType.COACH) },
                        onDeleteMedia = { id -> viewModel.deleteMedia(id, ProfileType.COACH) },
                        onReorderMedia = { ids -> viewModel.reorderMedia(ids, ProfileType.COACH) },
                        onSetCover = { id -> viewModel.setPrimaryCover(id, ProfileType.COACH) },
                        activeClients = state.activeClients,
                        busyChatUserIds = state.busyChatUserIds,
                        busyRelationshipIds = state.busyRelationshipIds,
                        onChangeAvatar = launchAvatarPicker,
                        isAvatarUploading = state.isUploadingAvatar,
                        userUsage = state.userUsage,
                        isUsageLoading = state.isUsageLoading,
                        usageError = state.usageError,
                        isUsageOffline = state.isUsageOffline,
                        onRefreshUsage = { viewModel.loadUserUsage(forceRefresh = true) },
                        onEdit = viewModel::startEditingCoach,
                        onEditGender = viewModel::startEditingGender,
                        onViewClient = onViewClient,
                        onOpenChat = { clientId, name, avatar ->
                            viewModel.startChat(clientId) { convId ->
                                onOpenChat(convId, clientId, name, avatar)
                            }
                        },
                        onEndRelationship = { confirmingRelationshipId = it }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Switch Account Button
        Button(
            onClick = {
                viewModel.switchAccount()
                onSwitchAccount()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Elevated,
                contentColor = PrimaryText
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.SwitchAccount,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Accent
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.switch_account), fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        // Sign Out Button
        OutlinedButton(
            onClick = { viewModel.logout(); onLogout() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sign_out))
        }

        Spacer(Modifier.height(com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults.ContentBottomPadding))
    }

    // Edit Client Profile Dialog
    if (state.isEditingClient) {
        EditClientProfileDialog(
            form = state.clientForm,
            isSaving = state.isSaving,
            errorMessage = state.clientFormError,
            onUpdateField = viewModel::updateClientFormField,
            onSave = viewModel::saveClientProfile,
            onDismiss = viewModel::cancelEditingClient
        )
    }

    // Edit Coach Profile Dialog
    if (state.isEditingCoach) {
        EditCoachProfileDialog(
            form = state.coachForm,
            isSaving = state.isSaving,
            errorMessage = state.coachFormError,
            onUpdateField = viewModel::updateCoachFormField,
            onSave = viewModel::saveCoachProfile,
            onDismiss = viewModel::cancelEditingCoach
        )
    }

    // Edit Gender Dialog
    if (state.isEditingGender) {
        GenderEditDialog(
            initialGender = state.clientProfile?.gender ?: state.coachProfile?.gender,
            isSaving = state.isSavingGender,
            errorMessage = state.genderError,
            onSave = viewModel::saveGender,
            onDismiss = viewModel::cancelEditingGender
        )
    }

    selectedAvatarUri?.let { previewUri ->
        AvatarPreviewDialog(
            imageUri = previewUri,
            isBusy = isPreparingAvatar || hasPendingAvatarUpload || state.isUploadingAvatar,
            isOnline = isOnline,
            errorMessage = avatarPreviewError ?: state.avatarUploadError,
            onChooseAnother = launchAvatarPicker,
            onDismiss = {
                if (!isPreparingAvatar && !hasPendingAvatarUpload && !state.isUploadingAvatar) {
                    selectedAvatarUri = null
                    avatarPreviewError = null
                    viewModel.clearAvatarUploadState()
                }
            },
            onConfirm = {
                isOnline = hasInternetConnection(connectivityManager)
                if (!isOnline) {
                    avatarPreviewError = "Connect to the internet to upload your avatar. Your selection is kept."
                } else if (!isPreparingAvatar && !hasPendingAvatarUpload && !state.isUploadingAvatar) {
                    isPreparingAvatar = true
                    hasPendingAvatarUpload = true
                    avatarPreviewError = null
                    viewModel.clearAvatarUploadState()
                    coroutineScope.launch {
                        try {
                            val optimizedFile = withContext(Dispatchers.IO) {
                                AvatarImageProcessor.prepare(context, previewUri)
                            }
                            isPreparingAvatar = false
                            viewModel.uploadAvatar(optimizedFile)
                        } catch (exception: Exception) {
                            isPreparingAvatar = false
                            hasPendingAvatarUpload = false
                            avatarPreviewError = exception.localizedMessage ?: "Could not prepare this image."
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun EditableAvatar(
    avatarUrl: String?,
    isUploading: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(
                avatarUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = !isUploading, onClick = onClick)
            )
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Accent)
                        .border(2.dp, Elevated, androidx.compose.foundation.shape.CircleShape)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = stringResource(R.string.change_avatar),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        TextButton(onClick = onClick, enabled = !isUploading) {
            Text(if (isUploading) "Uploading…" else "Change avatar")
        }
    }
}

@Composable
private fun AvatarPreviewDialog(
    imageUri: Uri,
    isBusy: Boolean,
    isOnline: Boolean,
    errorMessage: String?,
    onChooseAnother: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.loc_preview_avatar),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                AsyncImage(
                    model = imageUri,
                    contentDescription = stringResource(R.string.loc_selected_avatar_preview),
                    modifier = Modifier
                        .size(168.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = stringResource(R.string.loc_avatar_crop_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    textAlign = TextAlign.Center
                )
                if (!isOnline) {
                    Text(
                        text = stringResource(R.string.loc_offline_selection),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        textAlign = TextAlign.Center
                    )
                }
                errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Error,
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    OutlinedButton(
                        onClick = onChooseAnother,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.choose_another))
                    }
                }
                Button(
                    onClick = onConfirm,
                    enabled = !isBusy && isOnline,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isBusy) "Preparing and uploading…" else "Use this photo")
                }
            }
        }
    }
}

private fun hasInternetConnection(connectivityManager: ConnectivityManager): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

// ---------------------------------------------------------------------------
// Client Profile View
// ---------------------------------------------------------------------------

@Composable
private fun ClientProfileView(
    profile: ClientProfile?,
    mediaList: List<ProfileMediaItem> = emptyList(),
    isMediaLoading: Boolean = false,
    isMediaUploading: Boolean = false,
    onUploadMedia: (List<Uri>) -> Unit = {},
    onDeleteMedia: (Long) -> Unit = {},
    onReorderMedia: (List<Long>) -> Unit = {},
    onSetCover: (Long) -> Unit = {},
    busyChatUserIds: Set<Long>,
    busyRelationshipIds: Set<Long> = emptySet(),
    onChangeAvatar: () -> Unit,
    isAvatarUploading: Boolean,
    userUsage: UserUsage? = null,
    isUsageLoading: Boolean = false,
    usageError: String? = null,
    isUsageOffline: Boolean = false,
    onRefreshUsage: () -> Unit = {},
    onEdit: () -> Unit,
    onEditGender: () -> Unit,
    onViewCoach: (Long) -> Unit,
    onDiscoverCoaches: () -> Unit,
    onOpenChat: (Long, String, String?) -> Unit,
    onViewPlan: (Long) -> Unit,
    onBrowsePlans: () -> Unit,
    onNavigateToBodyMetrics: () -> Unit = {},
    onEndRelationship: (Long) -> Unit = {}
) {
    if (profile == null) {
        EmptyState(
            title = stringResource(R.string.loc_client_profile_not_found),
            body = stringResource(R.string.loc_athlete_profile_load_failed)
        )
        return
    }

    // Header Card
    DarkCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EditableAvatar(
                avatarUrl = profile.avatarUrl,
                isUploading = isAvatarUploading,
                onClick = onChangeAvatar
            )

            Text(
                text = profile.displayName.ifBlank { profile.fullName },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                profile.experienceLevel?.takeIf(String::isNotBlank)?.let {
                    PillBadge(
                        text = it.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase),
                        isAccent = true
                    )
                }
                userUsage?.membership?.let { membership ->
                    val badgeLabel = when {
                        membership.isAdmin -> "ADMIN • Unlimited"
                        membership.isPro && userUsage.videoAnalysis.unlimited -> "PRO • Unlimited"
                        membership.isPro -> "PRO"
                        else -> "FREE"
                    }
                    PillBadge(
                        text = badgeLabel,
                        isAccent = membership.isPro
                    )
                }
            }

            profile.bio?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = Elevated, contentColor = PrimaryText),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.loc_edit_athlete_profile))
            }
        }
    }

    // Usage & Quota Section
    UsageAndQuotaSection(
        usage = userUsage,
        isLoading = isUsageLoading,
        error = usageError,
        isOffline = isUsageOffline,
        onRefresh = onRefreshUsage
    )

    // Gender Row
    GenderProfileCard(
        gender = profile.gender,
        onEditClick = onEditGender
    )

    // Athlete Profile Media Gallery
    ProfileMediaGallery(
        mediaList = mediaList,
        isOwner = true,
        isLoading = isMediaLoading,
        isUploading = isMediaUploading,
        onUploadMedia = onUploadMedia,
        onDeleteMedia = onDeleteMedia,
        onReorderMedia = onReorderMedia,
        onSetCover = onSetCover
    )

    // Connected Coach Card
    DarkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.loc_connected_coach),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                if (profile.connectedCoach?.relationshipId != null) {
                    var menuExpanded by remember { mutableStateOf(false) }
                    val relId = profile.connectedCoach.relationshipId
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            enabled = relId !in busyRelationshipIds
                        ) {
                            if (relId in busyRelationshipIds) {
                                CircularProgressIndicator(Modifier.size(16.dp), color = SecondaryText, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), tint = SecondaryText)
                            }
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Elevated)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.end_relationship),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEndRelationship(relId)
                                }
                            )
                        }
                    }
                }
            }

            if (profile.connectedCoach != null) {
                val coach = profile.connectedCoach
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserAvatar(coach.coachAvatarUrl, modifier = Modifier.size(54.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = coach.coachName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        coach.coachExperienceLevel?.let {
                            Text(
                                text = it.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase),
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                        PillBadge("Active Coach", isAccent = true, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onViewCoach(coach.coachId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.loc_view_profile))
                    }
                    Button(
                        onClick = { onOpenChat(coach.coachId, coach.coachName, coach.coachAvatarUrl) },
                        modifier = Modifier.weight(1f),
                        enabled = coach.coachId !in busyChatUserIds,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                    ) {
                        if (coach.coachId in busyChatUserIds) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.common_message))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.loc_no_coach_connected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                    Button(
                        onClick = onDiscoverCoaches,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.discover_coaches))
                    }
                }
            }
        }
    }

    // Training Overview Card (Real Metrics)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.loc_training_overview),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = stringResource(R.string.loc_active_plans),
                value = "${profile.activePlansCount}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.loc_completed_sessions),
                value = "${profile.completedSessionsCount}",
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Current Workout Plan Card
    DarkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Accent)
                Text(
                    stringResource(R.string.loc_current_workout_plan),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
            }
            if (!profile.currentPlanName.isNullOrBlank()) {
                Text(
                    text = profile.currentPlanName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryText
                )
                if (profile.currentPlanId != null) {
                    OutlinedButton(
                        onClick = { onViewPlan(profile.currentPlanId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.view_plan_details))
                    }
                }
            } else {
                Text(
                    stringResource(R.string.loc_you_no_active_plan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                OutlinedButton(
                    onClick = onBrowsePlans,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.browse_workout_plans))
                }
            }
        }
    }

    // Body Metrics Tracking Card
    DarkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = Accent)
                Text(
                    stringResource(R.string.loc_body_metrics_tracking),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
            }
            Text(
                stringResource(R.string.loc_track_body_metrics_description),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
            Button(
                onClick = onNavigateToBodyMetrics,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Elevated, contentColor = PrimaryText),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Text(stringResource(R.string.view_body_metrics))
            }
        }
    }

    // Training & Experience Card
    DarkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.loc_training_experience),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            InfoRow("Experience Level", profile.experienceLevel?.replace('_', ' ')?.lowercase()?.replaceFirstChar(Char::uppercase))
            InfoRow(stringResource(R.string.gender), profile.gender.localizedLabel())
            InfoRow("Primary Goals", profile.primaryGoals)
            InfoRow("Training Background", profile.trainingExperience)
            InfoRow("Specific Interests", profile.trainingInterests)
            InfoRow("Preferred Frequency", profile.preferredFrequency)
            InfoRow("Preferred Coaching Language", profile.coachingLanguage)
        }
    }
}

// ---------------------------------------------------------------------------
// Coach Profile View
// ---------------------------------------------------------------------------

@Composable
private fun CoachProfileView(
    profile: CoachProfile?,
    fallbackGender: Gender? = null,
    mediaList: List<ProfileMediaItem> = emptyList(),
    isMediaLoading: Boolean = false,
    isMediaUploading: Boolean = false,
    onUploadMedia: (List<Uri>) -> Unit = {},
    onDeleteMedia: (Long) -> Unit = {},
    onReorderMedia: (List<Long>) -> Unit = {},
    onSetCover: (Long) -> Unit = {},
    activeClients: List<CoachClientRelationship>,
    busyChatUserIds: Set<Long>,
    busyRelationshipIds: Set<Long> = emptySet(),
    onChangeAvatar: () -> Unit,
    isAvatarUploading: Boolean,
    userUsage: UserUsage? = null,
    isUsageLoading: Boolean = false,
    usageError: String? = null,
    isUsageOffline: Boolean = false,
    onRefreshUsage: () -> Unit = {},
    onEdit: () -> Unit,
    onEditGender: () -> Unit,
    onViewClient: (Long) -> Unit,
    onOpenChat: (Long, String, String?) -> Unit,
    onEndRelationship: (Long) -> Unit = {}
) {
    if (profile == null) {
        DarkCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    stringResource(R.string.loc_become_coach),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    stringResource(R.string.loc_setup_coach_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.set_up_coach_profile))
                }
            }
        }
        GenderProfileCard(
            gender = fallbackGender,
            onEditClick = onEditGender
        )
        return
    }

    // Header Card
    DarkCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EditableAvatar(
                avatarUrl = profile.avatarUrl,
                isUploading = isAvatarUploading,
                onClick = onChangeAvatar
            )

            Text(
                text = profile.displayName.ifBlank { profile.fullName },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            // Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile.isVerified) {
                    PillBadge("Verified Coach", isAccent = true)
                }
                if (profile.isAcceptingClients) {
                    PillBadge("Accepting Clients", isAccent = true)
                } else {
                    PillBadge("Not Accepting Clients")
                }
                PillBadge(if (profile.isPublic) "Public" else "Private")
            }

            profile.bio?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            profile.yearsExperience?.let { years ->
                Text(
                    text = "$years ${if (years == 1) "year" else "years"} coaching experience",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent
                )
            }

            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = Elevated, contentColor = PrimaryText),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.edit_coach_profile))
            }
        }
    }

    // Gender Row
    GenderProfileCard(
        gender = profile.gender ?: fallbackGender,
        onEditClick = onEditGender
    )

    // Usage & Quota Section
    UsageAndQuotaSection(
        usage = userUsage,
        isLoading = isUsageLoading,
        error = usageError,
        isOffline = isUsageOffline,
        onRefresh = onRefreshUsage
    )

    // Coach Profile Media Gallery
    ProfileMediaGallery(
        mediaList = mediaList,
        isOwner = true,
        isLoading = isMediaLoading,
        isUploading = isMediaUploading,
        onUploadMedia = onUploadMedia,
        onDeleteMedia = onDeleteMedia,
        onReorderMedia = onReorderMedia,
        onSetCover = onSetCover
    )

    // Specialties Chips Card
    profile.specialties?.takeIf(String::isNotBlank)?.let { specs ->
        val specList = specs.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (specList.isNotEmpty()) {
            DarkCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.loc_coaching_specialties),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        specList.forEach { specialty ->
                            PillBadge(specialty, isAccent = true)
                        }
                    }
                }
            }
        }
    }

    // Real Coaching Statistics Section
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.loc_coaching_statistics),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = stringResource(R.string.loc_active_clients),
                value = "${profile.activeClientsCount}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.loc_completed),
                value = "${profile.completedClientsCount}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.loc_plans_assigned),
                value = "${profile.assignedPlansCount}",
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Philosophy & Coaching Description Card
    val hasPhilosophy = !profile.trainingPhilosophy.isNullOrBlank()
    val hasDescription = !profile.coachingDescription.isNullOrBlank()
    val hasLanguages = !profile.languages.isNullOrBlank()
    val hasCerts = !profile.certifications.isNullOrBlank()
    val hasSocial = !profile.socialLinks.isNullOrBlank()

    if (hasPhilosophy || hasDescription || hasLanguages || hasCerts || hasSocial) {
        DarkCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.loc_philosophy_details),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                if (hasPhilosophy) InfoRow("Training Philosophy", profile.trainingPhilosophy)
                if (hasDescription) InfoRow("Coaching Description", profile.coachingDescription)
                if (hasLanguages) InfoRow("Languages", profile.languages)
                if (hasCerts) InfoRow("Certifications", profile.certifications)
                if (hasSocial) InfoRow("Social / Portfolio", profile.socialLinks)
            }
        }
    }

    // Active Clients Section
    DarkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Active Clients (${activeClients.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )

            if (activeClients.isNotEmpty()) {
                activeClients.forEach { client ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Ink),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                UserAvatar(client.clientAvatarUrl, modifier = Modifier.size(48.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = client.clientName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryText
                                    )
                                    client.clientExperienceLevel?.let {
                                        Text(
                                            text = it.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryText
                                        )
                                    }
                                }
                                PillBadge("Connected", isAccent = true)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onViewClient(client.clientId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.loc_view_profile))
                                }
                                Button(
                                    onClick = { onOpenChat(client.clientId, client.clientName, client.clientAvatarUrl) },
                                    modifier = Modifier.weight(1f),
                                    enabled = client.clientId !in busyChatUserIds,
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                ) {
                                    if (client.clientId in busyChatUserIds) {
                                        CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                                    } else {
                                        Text(stringResource(R.string.common_message))
                                    }
                                }
                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        enabled = client.id !in busyRelationshipIds
                                    ) {
                                        if (client.id in busyRelationshipIds) {
                                            CircularProgressIndicator(Modifier.size(16.dp), color = SecondaryText, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), tint = SecondaryText)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(Elevated)
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.end_relationship),
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onEndRelationship(client.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.loc_no_active_clients),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Edit Client Profile Dialog
// ---------------------------------------------------------------------------

@Composable
private fun EditClientProfileDialog(
    form: ClientFormState,
    isSaving: Boolean,
    errorMessage: String?,
    onUpdateField: ((ClientFormState) -> ClientFormState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.loc_edit_athlete_profile),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = SecondaryText)
                    }
                }

                errorMessage?.let {
                    Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
                }

                // Display Name
                OutlinedTextField(
                    value = form.displayName,
                    onValueChange = { value -> onUpdateField { it.copy(displayName = value) } },
                    label = { Text(stringResource(R.string.display_name)) },
                    supportingText = { Text("${form.displayName.length} / 100", color = SecondaryText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Bio
                OutlinedTextField(
                    value = form.bio,
                    onValueChange = { value -> onUpdateField { it.copy(bio = value) } },
                    label = { Text(stringResource(R.string.bio_introduction)) },
                    supportingText = { Text("${form.bio.length} / 500", color = SecondaryText) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Experience Level Selector
                Text(
                    stringResource(R.string.loc_experience_level),
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("BEGINNER", "INTERMEDIATE", "ADVANCED").forEach { level ->
                        val selected = form.experienceLevel == level
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateField { it.copy(experienceLevel = level) } },
                            label = { Text(level.lowercase().replaceFirstChar(Char::uppercase)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent.copy(alpha = 0.2f),
                                selectedLabelColor = Accent
                            )
                        )
                    }
                }

                // Gender Selector
                Text(
                    stringResource(R.string.gender),
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryText
                )
                GenderOptionSelector(
                    selectedGender = form.gender,
                    onSelect = { gender -> onUpdateField { it.copy(gender = gender) } },
                    enabled = !isSaving,
                    compact = true
                )

                // Primary Goals
                OutlinedTextField(
                    value = form.primaryGoals,
                    onValueChange = { value -> onUpdateField { it.copy(primaryGoals = value) } },
                    label = { Text(stringResource(R.string.primary_training_goals)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Training Experience
                OutlinedTextField(
                    value = form.trainingExperience,
                    onValueChange = { value -> onUpdateField { it.copy(trainingExperience = value) } },
                    label = { Text(stringResource(R.string.training_background_experience)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Training Interests
                OutlinedTextField(
                    value = form.trainingInterests,
                    onValueChange = { value -> onUpdateField { it.copy(trainingInterests = value) } },
                    label = { Text(stringResource(R.string.specific_interests)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Preferred Frequency
                OutlinedTextField(
                    value = form.preferredFrequency,
                    onValueChange = { value -> onUpdateField { it.copy(preferredFrequency = value) } },
                    label = { Text(stringResource(R.string.preferred_frequency)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Coaching Language
                OutlinedTextField(
                    value = form.coachingLanguage,
                    onValueChange = { value -> onUpdateField { it.copy(coachingLanguage = value) } },
                    label = { Text(stringResource(R.string.preferred_coaching_language)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Height and Weight (Optional)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = form.heightCm,
                        onValueChange = { value -> onUpdateField { it.copy(heightCm = value) } },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border
                        )
                    )
                    OutlinedTextField(
                        value = form.weightKg,
                        onValueChange = { value -> onUpdateField { it.copy(weightKg = value) } },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border
                        )
                    )
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.common_save))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Edit Coach Profile Dialog
// ---------------------------------------------------------------------------

@Composable
private fun EditCoachProfileDialog(
    form: CoachFormState,
    isSaving: Boolean,
    errorMessage: String?,
    onUpdateField: ((CoachFormState) -> CoachFormState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val commonSpecialties = listOf("Planche", "Front Lever", "Handstand", "Muscle-up", "Back Lever", "Human Flag")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.loc_edit_coach_profile),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = SecondaryText)
                    }
                }

                errorMessage?.let {
                    Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
                }

                // Display Name
                OutlinedTextField(
                    value = form.displayName,
                    onValueChange = { value -> onUpdateField { it.copy(displayName = value) } },
                    label = { Text(stringResource(R.string.display_name)) },
                    supportingText = { Text("${form.displayName.length} / 100", color = SecondaryText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Bio
                OutlinedTextField(
                    value = form.bio,
                    onValueChange = { value -> onUpdateField { it.copy(bio = value) } },
                    label = { Text(stringResource(R.string.loc_bio_professional_intro)) },
                    supportingText = { Text("${form.bio.length} / 500", color = SecondaryText) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Gender Selector
                Text(
                    stringResource(R.string.gender),
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryText
                )
                GenderOptionSelector(
                    selectedGender = form.gender,
                    onSelect = { gender -> onUpdateField { it.copy(gender = gender) } },
                    enabled = !isSaving,
                    compact = true
                )

                // Years of Experience
                OutlinedTextField(
                    value = form.yearsExperience,
                    onValueChange = { value -> onUpdateField { it.copy(yearsExperience = value) } },
                    label = { Text(stringResource(R.string.years_coaching_experience)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Specialties Field + Quick chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = form.specialties,
                        onValueChange = { value -> onUpdateField { it.copy(specialties = value) } },
                        label = { Text(stringResource(R.string.specialties)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonSpecialties.forEach { spec ->
                            val currentSpecs = form.specialties.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val isIncluded = currentSpecs.any { it.equals(spec, ignoreCase = true) }
                            FilterChip(
                                selected = isIncluded,
                                onClick = {
                                    val newSpecs = if (isIncluded) {
                                        currentSpecs.filterNot { it.equals(spec, ignoreCase = true) }
                                    } else {
                                        currentSpecs + spec
                                    }
                                    onUpdateField { it.copy(specialties = newSpecs.joinToString(", ")) }
                                },
                                label = { Text(spec) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Accent.copy(alpha = 0.2f),
                                    selectedLabelColor = Accent
                                )
                            )
                        }
                    }
                }

                // Training Philosophy
                OutlinedTextField(
                    value = form.trainingPhilosophy,
                    onValueChange = { value -> onUpdateField { it.copy(trainingPhilosophy = value) } },
                    label = { Text(stringResource(R.string.training_philosophy)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Coaching Description
                OutlinedTextField(
                    value = form.coachingDescription,
                    onValueChange = { value -> onUpdateField { it.copy(coachingDescription = value) } },
                    label = { Text(stringResource(R.string.loc_detailed_coaching_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Languages
                OutlinedTextField(
                    value = form.languages,
                    onValueChange = { value -> onUpdateField { it.copy(languages = value) } },
                    label = { Text(stringResource(R.string.languages_spoken)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Certifications
                OutlinedTextField(
                    value = form.certifications,
                    onValueChange = { value -> onUpdateField { it.copy(certifications = value) } },
                    label = { Text(stringResource(R.string.loc_certifications_qualifications)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Social Links
                OutlinedTextField(
                    value = form.socialLinks,
                    onValueChange = { value -> onUpdateField { it.copy(socialLinks = value) } },
                    label = { Text(stringResource(R.string.loc_social_portfolio_links)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.public_coach_profile), color = PrimaryText, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.allow_discover), color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = form.isPublic,
                        onCheckedChange = { value -> onUpdateField { it.copy(isPublic = value) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Accent)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.accepting_new_clients), color = PrimaryText, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.allow_connection_requests), color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = form.isAcceptingClients,
                        onCheckedChange = { value -> onUpdateField { it.copy(isAcceptingClients = value) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Accent)
                    )
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.common_save))
                        }
                    }
                }
            }
        }
    }
}
