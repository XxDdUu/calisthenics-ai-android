package com.calisthenics.ai.ui.profile

import com.calisthenics.ai.data.local.GenderSessionStore
import com.calisthenics.ai.data.remote.dto.ClientProfileRequestDto
import com.calisthenics.ai.data.remote.dto.CoachProfileRequestDto
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.ClientTrainingOverview
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachConnectionOverview
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.model.ConnectedCoachSummary
import com.calisthenics.ai.domain.model.Gender
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileMediaType
import com.calisthenics.ai.domain.model.ProfileType
import com.calisthenics.ai.domain.model.UserProfile
import com.calisthenics.ai.domain.model.UserUsage
import com.calisthenics.ai.domain.model.MembershipUsage
import com.calisthenics.ai.domain.model.VideoAnalysisUsage
import com.calisthenics.ai.domain.model.LiveAnalysisUsage
import com.calisthenics.ai.domain.repository.AnalysisRepository
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.ChatRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.ProfileRepository
import com.calisthenics.ai.ui.media.MediaUploadManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val analysisRepository: AnalysisRepository = mockk(relaxed = true)

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var coachClientRepository: FakeCoachClientRepository
    private lateinit var genderSessionStore: GenderSessionStore
    private lateinit var viewModel: ProfileViewModel

    private val sampleClient = ClientProfile(
        id = 1L,
        fullName = "Test Athlete",
        displayName = "Athlete One",
        avatarUrl = "https://example.com/avatar.png",
        bio = "Calisthenics enthusiast",
        experienceLevel = "INTERMEDIATE",
        trainingExperience = "2 years bodyweight training",
        primaryGoals = "Master muscle-up and handstand",
        trainingInterests = "Handstand, Muscle-up",
        preferredFrequency = "4 days/week",
        coachingLanguage = "English",
        activePlansCount = 1L,
        completedSessionsCount = 14L,
        currentPlanId = 101L,
        currentPlanName = "Strength Foundation",
        connectedCoach = ConnectedCoachSummary(
            coachId = 2L,
            coachName = "Coach Dave",
            coachAvatarUrl = null,
            coachExperienceLevel = "ADVANCED"
        )
    )

    private val sampleCoach = CoachProfile(
        id = 1L,
        fullName = "Coach Dave",
        displayName = "Coach Dave Elite",
        avatarUrl = null,
        experienceLevel = "ADVANCED",
        bio = "Professional gymnastics and calisthenics coach",
        specialties = "Planche, Front Lever, Handstand",
        yearsExperience = 6,
        trainingPhilosophy = "Form over reps, progressive overload",
        coachingDescription = "Personalized online calisthenics coaching",
        languages = "English, Spanish",
        certifications = "NASM-CPT, Gymnastic Bodies L1",
        socialLinks = "instagram.com/coachdave",
        isPublic = true,
        isAcceptingClients = true,
        isVerified = true,
        activeClientsCount = 4L,
        completedClientsCount = 12L,
        assignedPlansCount = 8L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        genderSessionStore = GenderSessionStore()
        profileRepository = FakeProfileRepository(
            clientProfile = sampleClient,
            coachProfile = sampleCoach
        )
        coachClientRepository = FakeCoachClientRepository()
        coEvery { analysisRepository.getUserUsage() } returns Result.success(UserUsage())
        every { analysisRepository.usageUpdateEvents } returns MutableSharedFlow()
        every { analysisRepository.getCachedUserUsage() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = ProfileViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
            coachClientRepository = coachClientRepository,
            chatRepository = chatRepository,
            mediaUploadManager = mockk(relaxed = true),
            analysisRepository = analysisRepository,
            genderSessionStore = genderSessionStore
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `loads client and coach profiles on init`() = runTest {
        createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(sampleClient, state.clientProfile)
        assertEquals(sampleCoach, state.coachProfile)
        assertEquals("Athlete One", state.clientForm.displayName)
        assertEquals("Coach Dave Elite", state.coachForm.displayName)
        assertEquals("6", state.coachForm.yearsExperience)
    }

    @Test
    fun `switches tabs and clears feedback`() = runTest {
        createViewModel()

        viewModel.selectTab(ProfileTab.COACH)
        assertEquals(ProfileTab.COACH, viewModel.uiState.value.activeTab)

        viewModel.selectTab(ProfileTab.CLIENT)
        assertEquals(ProfileTab.CLIENT, viewModel.uiState.value.activeTab)
    }

    @Test
    fun `client form validation rejects too long display name or bio`() = runTest {
        createViewModel()

        viewModel.startEditingClient()
        assertTrue(viewModel.uiState.value.isEditingClient)

        // Name > 100 characters
        viewModel.updateClientFormField { it.copy(displayName = "A".repeat(101)) }
        viewModel.saveClientProfile()
        assertNotNull(viewModel.uiState.value.clientFormError)
        assertTrue(viewModel.uiState.value.clientFormError!!.contains("100"))

        // Bio > 500 characters
        viewModel.updateClientFormField { it.copy(displayName = "Valid Name", bio = "B".repeat(501)) }
        viewModel.saveClientProfile()
        assertNotNull(viewModel.uiState.value.clientFormError)
        assertTrue(viewModel.uiState.value.clientFormError!!.contains("500"))
    }

    @Test
    fun `saves client profile successfully and updates ui state`() = runTest {
        createViewModel()

        viewModel.startEditingClient()
        viewModel.updateClientFormField {
            it.copy(
                displayName = "Updated Athlete",
                bio = "Updated bio message"
            )
        }

        viewModel.saveClientProfile()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditingClient)
        assertFalse(state.isSaving)
        assertNotNull(state.feedback)
        assertEquals("Updated Athlete", state.clientProfile?.displayName)
        assertEquals("Updated bio message", state.clientProfile?.bio)
    }

    @Test
    fun `coach form validation rejects invalid years of experience`() = runTest {
        createViewModel()

        viewModel.startEditingCoach()
        assertTrue(viewModel.uiState.value.isEditingCoach)

        viewModel.updateCoachFormField { it.copy(yearsExperience = "-3") }
        viewModel.saveCoachProfile()

        assertNotNull(viewModel.uiState.value.coachFormError)
        assertTrue(viewModel.uiState.value.coachFormError!!.contains("non-negative"))

        viewModel.updateCoachFormField { it.copy(yearsExperience = "invalid_number") }
        viewModel.saveCoachProfile()

        assertNotNull(viewModel.uiState.value.coachFormError)
        assertTrue(viewModel.uiState.value.coachFormError!!.contains("non-negative"))
    }

    @Test
    fun `saves coach profile successfully and updates ui state`() = runTest {
        createViewModel()

        viewModel.startEditingCoach()
        viewModel.updateCoachFormField {
            it.copy(
                displayName = "Coach Dave Master",
                yearsExperience = "10",
                specialties = "Planche, Handstand, Iron Cross"
            )
        }

        viewModel.saveCoachProfile()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditingCoach)
        assertFalse(state.isSaving)
        assertNotNull(state.feedback)
        assertEquals("Coach Dave Master", state.coachProfile?.displayName)
        assertEquals(10, state.coachProfile?.yearsExperience)
        assertEquals("Planche, Handstand, Iron Cross", state.coachProfile?.specialties)
    }

    @Test
    fun `startChat invokes chatRepository and executes callback`() = runTest {
        val conversation = ChatConversation(
            id = 99L,
            otherUserId = 2L,
            otherUserName = "Coach Dave",
            otherUserAvatarUrl = null,
            lastMessagePreview = null,
            unreadCount = 0L,
            lastMessageAt = null,
            createdAt = null,
            updatedAt = null
        )
        coEvery { chatRepository.createOrGetConversation(2L) } returns Result.success(conversation)

        createViewModel()

        var callbackConversationId: Long? = null
        viewModel.startChat(2L) { id ->
            callbackConversationId = id
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(99L, callbackConversationId)
        assertFalse(viewModel.uiState.value.busyChatUserIds.contains(2L))
    }

    @Test
    fun `avatar upload updates both profile states and cached account`() = runTest {
        createViewModel()
        val avatarUrl = "https://cdn.example.com/new-avatar.jpg"
        profileRepository.avatarUploadResult = Result.success(avatarUrl)
        val file = File.createTempFile("avatar-test", ".jpg")

        viewModel.uploadAvatar(file)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(avatarUrl, state.clientProfile?.avatarUrl)
        assertEquals(avatarUrl, state.coachProfile?.avatarUrl)
        assertFalse(state.isUploadingAvatar)
        assertTrue(state.avatarUploadSucceeded)
        assertFalse(file.exists())
        verify {
            authRepository.updateAccountProfile(
                userId = sampleClient.userId,
                fullName = sampleClient.displayName,
                avatarUrl = avatarUrl
            )
        }
    }

    @Test
    fun `failed avatar upload leaves current profile images unchanged`() = runTest {
        createViewModel()
        profileRepository.avatarUploadResult = Result.failure(IllegalStateException("Network unavailable"))
        val file = File.createTempFile("avatar-test", ".jpg")

        viewModel.uploadAvatar(file)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sampleClient.avatarUrl, state.clientProfile?.avatarUrl)
        assertEquals(sampleCoach.avatarUrl, state.coachProfile?.avatarUrl)
        assertFalse(state.isUploadingAvatar)
        assertEquals("Network unavailable", state.avatarUploadError)
        assertFalse(file.exists())
    }

    @Test
    fun switchAccountResetsUiStateWithoutClearingAuth() {
        createViewModel()
        viewModel.switchAccount()

        assertNull(viewModel.uiState.value.clientProfile)
        verify { authRepository.deactivateActiveSession() }
    }

    @Test
    fun `terminateRelationship succeeds and sets feedback message`() = runTest {
        createViewModel()
        coachClientRepository.terminateResult = Result.success(
            CoachClientRelationship(
                id = 42L,
                coachId = 1L,
                coachName = "Coach",
                coachAvatarUrl = null,
                coachExperienceLevel = "ADVANCED",
                clientId = 2L,
                clientName = "Client",
                clientAvatarUrl = null,
                clientExperienceLevel = "BEGINNER",
                requestedBy = 2L,
                status = "TERMINATED",
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-02T00:00:00Z"
            )
        )

        var completionSuccess: Boolean? = null
        viewModel.terminateRelationship(42L) { success ->
            completionSuccess = success
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, completionSuccess)
        assertEquals("Relationship ended.", viewModel.uiState.value.feedback)
        assertFalse(viewModel.uiState.value.busyRelationshipIds.contains(42L))
        assertEquals(listOf(42L), coachClientRepository.terminatedIds)
    }

    @Test
    fun `terminateRelationship failure sets error message`() = runTest {
        createViewModel()
        coachClientRepository.terminateResult = Result.failure(IllegalStateException("Network error"))

        var completionSuccess: Boolean? = null
        viewModel.terminateRelationship(42L) { success ->
            completionSuccess = success
        }
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, completionSuccess)
        assertEquals("Network error", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.busyRelationshipIds.contains(42L))
    }

    @Test
    fun `loads athlete media and keeps coach media separate`() = runTest {
        val athleteItem = ProfileMediaItem(id = 10L, userId = 1L, profileType = ProfileType.ATHLETE, mediaType = ProfileMediaType.IMAGE, url = "athlete.jpg")
        val coachItem = ProfileMediaItem(id = 20L, userId = 1L, profileType = ProfileType.COACH, mediaType = ProfileMediaType.VIDEO, url = "coach.mp4")
        profileRepository.mediaItems.addAll(listOf(athleteItem, coachItem))

        createViewModel()

        val state = viewModel.uiState.value
        assertEquals(1, state.athleteMedia.size)
        assertEquals(10L, state.athleteMedia[0].id)
        assertEquals(ProfileType.ATHLETE, state.athleteMedia[0].profileType)

        assertEquals(1, state.coachMedia.size)
        assertEquals(20L, state.coachMedia[0].id)
        assertEquals(ProfileType.COACH, state.coachMedia[0].profileType)
    }

    @Test
    fun `reorderMedia updates ordering`() = runTest {
        val item1 = ProfileMediaItem(id = 1L, userId = 1L, profileType = ProfileType.ATHLETE, mediaType = ProfileMediaType.IMAGE, url = "1.jpg", displayOrder = 0)
        val item2 = ProfileMediaItem(id = 2L, userId = 1L, profileType = ProfileType.ATHLETE, mediaType = ProfileMediaType.IMAGE, url = "2.jpg", displayOrder = 1)
        profileRepository.mediaItems.addAll(listOf(item1, item2))

        createViewModel()
        viewModel.reorderMedia(listOf(2L, 1L), ProfileType.ATHLETE)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2L, state.athleteMedia[0].id)
        assertEquals(0, state.athleteMedia[0].displayOrder)
        assertEquals(1L, state.athleteMedia[1].id)
        assertEquals(1, state.athleteMedia[1].displayOrder)
    }

    @Test
    fun `setPrimaryCover moves item to order 0`() = runTest {
        val item1 = ProfileMediaItem(id = 1L, userId = 1L, profileType = ProfileType.COACH, mediaType = ProfileMediaType.IMAGE, url = "1.jpg", displayOrder = 0)
        val item2 = ProfileMediaItem(id = 2L, userId = 1L, profileType = ProfileType.COACH, mediaType = ProfileMediaType.VIDEO, url = "2.mp4", displayOrder = 1)
        profileRepository.mediaItems.addAll(listOf(item1, item2))

        createViewModel()
        viewModel.setPrimaryCover(2L, ProfileType.COACH)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2L, state.coachMedia[0].id)
        assertEquals(0, state.coachMedia[0].displayOrder)
    }

    @Test
    fun `deleteMedia removes item from list`() = runTest {
        val item1 = ProfileMediaItem(id = 1L, userId = 1L, profileType = ProfileType.ATHLETE, mediaType = ProfileMediaType.IMAGE, url = "1.jpg")
        profileRepository.mediaItems.add(item1)

        createViewModel()
        assertEquals(1, viewModel.uiState.value.athleteMedia.size)

        viewModel.deleteMedia(1L, ProfileType.ATHLETE)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.athleteMedia.size)
    }

    @Test
    fun `saveGender updates both client and coach profiles immediately`() = runTest {
        coEvery { authRepository.updateGender(Gender.FEMALE) } returns Result.success(
            UserProfile(
                id = 1L,
                email = "test@example.com",
                fullName = "Test Athlete",
                experienceLevel = "INTERMEDIATE",
                role = "ROLE_USER",
                gender = Gender.FEMALE
            )
        )

        createViewModel()
        viewModel.startEditingGender()
        assertTrue(viewModel.uiState.value.isEditingGender)

        viewModel.saveGender(Gender.FEMALE)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditingGender)
        assertFalse(state.isSavingGender)
        assertNull(state.genderError)
        assertEquals(Gender.FEMALE, state.clientProfile?.gender)
        assertEquals(Gender.FEMALE, state.coachProfile?.gender)
        assertEquals(Gender.FEMALE, state.clientForm.gender)
        assertEquals(Gender.FEMALE, state.coachForm.gender)
        assertNotNull(state.feedbackResId)
    }

    @Test
    fun `saveGender failure keeps edit dialog open and preserves previous gender`() = runTest {
        coEvery { authRepository.updateGender(Gender.MALE) } returns Result.failure(
            java.io.IOException("Offline")
        )

        createViewModel()
        viewModel.startEditingGender()
        viewModel.saveGender(Gender.MALE)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditingGender)
        assertFalse(state.isSavingGender)
        assertNotNull(state.genderError)
        assertNull(state.clientProfile?.gender)
        assertNull(state.coachProfile?.gender)
    }

    @Test
    fun `genderSessionStore update synchronizes client and coach profiles`() = runTest {
        createViewModel()
        assertNull(viewModel.uiState.value.clientProfile?.gender)

        genderSessionStore.publish(userId = 1L, gender = Gender.MALE, isBackendConfirmed = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Gender.MALE, viewModel.uiState.value.clientProfile?.gender)
        assertEquals(Gender.MALE, viewModel.uiState.value.coachProfile?.gender)
    }

    @Test
    fun `saving client profile with gender synchronizes coach profile gender`() = runTest {
        createViewModel()

        viewModel.startEditingClient()
        viewModel.updateClientFormField { it.copy(gender = Gender.MALE) }
        viewModel.saveClientProfile()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Gender.MALE, viewModel.uiState.value.clientProfile?.gender)
        assertEquals(Gender.MALE, viewModel.uiState.value.coachProfile?.gender)
    }

    @Test
    fun `loads user usage successfully on init`() = runTest {
        val expectedUsage = UserUsage(
            membership = MembershipUsage(tier = "PRO", status = "ACTIVE"),
            videoAnalysis = VideoAnalysisUsage(used = 3, limit = 10, remaining = 7, unlimited = false),
            liveAnalysis = LiveAnalysisUsage(available = true, unlimited = true)
        )
        coEvery { analysisRepository.getUserUsage() } returns Result.success(expectedUsage)

        createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isUsageLoading)
        assertFalse(state.isUsageOffline)
        assertNull(state.usageError)
        assertEquals(expectedUsage, state.userUsage)
    }

    @Test
    fun `loadUserUsage sets error when request fails and no cache exists`() = runTest {
        coEvery { analysisRepository.getUserUsage() } returns Result.failure(RuntimeException("Network error"))
        every { analysisRepository.getCachedUserUsage() } returns null

        createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isUsageLoading)
        assertFalse(state.isUsageOffline)
        assertNull(state.userUsage)
        assertEquals("Network error", state.usageError)
    }

    @Test
    fun `loadUserUsage falls back to cached usage and marks offline when network fails`() = runTest {
        val initialUsage = UserUsage(
            membership = MembershipUsage(tier = "FREE", status = "ACTIVE"),
            videoAnalysis = VideoAnalysisUsage(used = 1, limit = 1, remaining = 0)
        )
        coEvery { analysisRepository.getUserUsage() } returns Result.success(initialUsage)
        createViewModel()
        assertEquals(initialUsage, viewModel.uiState.value.userUsage)
        assertFalse(viewModel.uiState.value.isUsageOffline)

        // Subsequent failure
        coEvery { analysisRepository.getUserUsage() } returns Result.failure(RuntimeException("Offline"))
        every { analysisRepository.getCachedUserUsage() } returns initialUsage

        viewModel.loadUserUsage(forceRefresh = true)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isUsageLoading)
        assertTrue(state.isUsageOffline)
        assertNull(state.usageError)
        assertEquals(initialUsage, state.userUsage)
    }

    @Test
    fun `usageUpdateEvents trigger automatic usage reload`() = runTest {
        val usageEvents = MutableSharedFlow<UserUsage?>()
        every { analysisRepository.usageUpdateEvents } returns usageEvents
        val initialUsage = UserUsage(membership = MembershipUsage(tier = "FREE"))
        val updatedUsage = UserUsage(membership = MembershipUsage(tier = "PRO"))
        coEvery { analysisRepository.getUserUsage() } returns Result.success(initialUsage)

        createViewModel()
        assertEquals(initialUsage, viewModel.uiState.value.userUsage)

        coEvery { analysisRepository.getUserUsage() } returns Result.success(updatedUsage)
        usageEvents.emit(null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(updatedUsage, viewModel.uiState.value.userUsage)
    }

    @Test
    fun `usageUpdateEvents emitting admin-granted unlimited PRO updates profile immediately without relogin`() = runTest {
        val usageEvents = MutableSharedFlow<UserUsage?>()
        every { analysisRepository.usageUpdateEvents } returns usageEvents
        val freeUsage = UserUsage(membership = MembershipUsage(tier = "FREE", source = "Default"))
        val adminProUsage = UserUsage(
            membership = MembershipUsage(tier = "PRO", status = "ACTIVE", expiresAt = null, source = "Admin Grant"),
            videoAnalysis = VideoAnalysisUsage(used = 2, limit = null, remaining = null, unlimited = true),
            liveAnalysis = LiveAnalysisUsage(available = true, unlimited = true)
        )
        coEvery { analysisRepository.getUserUsage() } returns Result.success(freeUsage)

        createViewModel()
        assertEquals("FREE", viewModel.uiState.value.userUsage?.membership?.tier)
        assertFalse(viewModel.uiState.value.userUsage?.videoAnalysis?.unlimited ?: true)

        usageEvents.emit(adminProUsage)
        dispatcher.scheduler.advanceUntilIdle()

        val refreshed = viewModel.uiState.value.userUsage
        assertNotNull(refreshed)
        assertTrue(refreshed!!.membership.isPro)
        assertEquals("Admin Grant", refreshed.membership.source)
        assertTrue(refreshed.videoAnalysis.unlimited)
        assertNull(refreshed.videoAnalysis.limit)
        assertTrue(refreshed.liveAnalysis.available)
        assertTrue(refreshed.liveAnalysis.unlimited)
    }
}

// ---------------------------------------------------------------------------
// Test Fakes
// ---------------------------------------------------------------------------

private class FakeProfileRepository(
    var clientProfile: ClientProfile,
    var coachProfile: CoachProfile
) : ProfileRepository {
    val mediaItems = mutableListOf<ProfileMediaItem>()
    var avatarUploadResult: Result<String> = Result.success("https://cdn.example.com/avatar.jpg")

    override suspend fun getMyCoachProfile(): Result<CoachProfile> = Result.success(coachProfile)

    override suspend fun updateMyCoachProfile(request: CoachProfileRequestDto): Result<CoachProfile> {
        coachProfile = coachProfile.copy(
            displayName = request.displayName ?: coachProfile.displayName,
            avatarUrl = request.avatarUrl ?: coachProfile.avatarUrl,
            bio = request.bio ?: coachProfile.bio,
            specialties = request.specialties ?: coachProfile.specialties,
            yearsExperience = request.yearsExperience ?: coachProfile.yearsExperience,
            trainingPhilosophy = request.trainingPhilosophy ?: coachProfile.trainingPhilosophy,
            coachingDescription = request.coachingDescription ?: coachProfile.coachingDescription,
            languages = request.languages ?: coachProfile.languages,
            certifications = request.certifications ?: coachProfile.certifications,
            socialLinks = request.socialLinks ?: coachProfile.socialLinks,
            isPublic = request.isPublic ?: coachProfile.isPublic,
            isAcceptingClients = request.isAcceptingClients ?: coachProfile.isAcceptingClients,
            gender = Gender.fromApi(request.gender) ?: coachProfile.gender
        )
        return Result.success(coachProfile)
    }

    override suspend fun getPublicCoachProfile(id: Long): Result<CoachProfile> = Result.success(coachProfile)

    override suspend fun getMyClientProfile(): Result<ClientProfile> = Result.success(clientProfile)

    override suspend fun updateMyClientProfile(request: ClientProfileRequestDto): Result<ClientProfile> {
        clientProfile = clientProfile.copy(
            displayName = request.displayName ?: clientProfile.displayName,
            avatarUrl = request.avatarUrl ?: clientProfile.avatarUrl,
            bio = request.bio ?: clientProfile.bio,
            experienceLevel = request.experienceLevel ?: clientProfile.experienceLevel,
            trainingExperience = request.trainingExperience ?: clientProfile.trainingExperience,
            primaryGoals = request.primaryGoals ?: clientProfile.primaryGoals,
            trainingInterests = request.trainingInterests ?: clientProfile.trainingInterests,
            preferredFrequency = request.preferredFrequency ?: clientProfile.preferredFrequency,
            coachingLanguage = request.coachingLanguage ?: clientProfile.coachingLanguage,
            heightCm = request.heightCm ?: clientProfile.heightCm,
            weightKg = request.weightKg ?: clientProfile.weightKg,
            gender = Gender.fromApi(request.gender) ?: clientProfile.gender
        )
        return Result.success(clientProfile)
    }

    override suspend fun getClientProfile(id: Long): Result<ClientProfile> = Result.success(clientProfile)

    override suspend fun getClientOverview(id: Long): Result<ClientTrainingOverview> =
        Result.success(ClientTrainingOverview(userId = id))

    override suspend fun getMyClientOverview(): Result<ClientTrainingOverview> =
        Result.success(ClientTrainingOverview(userId = clientProfile.userId))

    override suspend fun uploadAvatar(file: File): Result<String> = avatarUploadResult

    override suspend fun getProfileMedia(userId: Long, profileType: ProfileType): Result<List<ProfileMediaItem>> =
        Result.success(mediaItems.filter { it.userId == userId && it.profileType == profileType }.sortedBy { it.displayOrder })

    override suspend fun uploadProfileMedia(profileType: ProfileType, file: File, thumbnail: File?, mimeType: String): Result<ProfileMediaItem> {
        val item = ProfileMediaItem(
            id = (mediaItems.maxOfOrNull { it.id } ?: 0L) + 1L,
            userId = 1L,
            profileType = profileType,
            mediaType = ProfileMediaType.IMAGE,
            url = "https://cdn.example.com/item.jpg",
            displayOrder = mediaItems.count { it.profileType == profileType }
        )
        mediaItems.add(item)
        return Result.success(item)
    }

    override suspend fun deleteProfileMedia(id: Long): Result<Unit> {
        mediaItems.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun reorderProfileMedia(profileType: ProfileType, mediaIds: List<Long>): Result<List<ProfileMediaItem>> {
        val current = mediaItems.filter { it.profileType == profileType }.associateBy { it.id }
        mediaIds.forEachIndexed { index, id ->
            current[id]?.let {
                val idx = mediaItems.indexOf(it)
                if (idx != -1) {
                    mediaItems[idx] = it.copy(displayOrder = index)
                }
            }
        }
        return Result.success(mediaItems.filter { it.profileType == profileType }.sortedBy { it.displayOrder })
    }

    override suspend fun setProfileMediaCover(id: Long): Result<List<ProfileMediaItem>> {
        val target = mediaItems.find { it.id == id } ?: return Result.failure(NoSuchElementException())
        val sameType = mediaItems.filter { it.profileType == target.profileType }
        var order = 1
        sameType.forEach {
            val idx = mediaItems.indexOf(it)
            if (it.id == id) {
                mediaItems[idx] = it.copy(displayOrder = 0)
            } else {
                mediaItems[idx] = it.copy(displayOrder = order++)
            }
        }
        return Result.success(mediaItems.filter { it.profileType == target.profileType }.sortedBy { it.displayOrder })
    }
}

private class FakeCoachClientRepository : CoachClientRepository {
    var terminateResult: Result<CoachClientRelationship> = Result.failure(NotImplementedError())
    val terminatedIds = mutableListOf<Long>()

    override suspend fun loadOverview(): Result<CoachConnectionOverview> =
        Result.success(CoachConnectionOverview(currentUserId = 1L, coaches = emptyList(), relationships = emptyList()))

    override suspend fun getCoachProfile(id: Long): Result<CoachProfile> =
        Result.failure(NotImplementedError())

    override suspend fun requestConnection(coachId: Long): Result<CoachClientRelationship> =
        Result.failure(NotImplementedError())

    override suspend fun acceptRequest(relationshipId: Long): Result<CoachClientRelationship> =
        Result.failure(NotImplementedError())

    override suspend fun rejectRequest(relationshipId: Long): Result<CoachClientRelationship> =
        Result.failure(NotImplementedError())

    override suspend fun cancelRequest(relationshipId: Long): Result<CoachClientRelationship> =
        Result.failure(NotImplementedError())

    override suspend fun terminateRelationship(relationshipId: Long): Result<CoachClientRelationship> {
        terminatedIds.add(relationshipId)
        return terminateResult
    }
}
