package com.calisthenics.ai.ui.admin

import com.calisthenics.ai.domain.model.AdminAiChatResponse
import com.calisthenics.ai.domain.model.AdminAiConversationSummary
import com.calisthenics.ai.domain.model.AdminAiMessage
import com.calisthenics.ai.domain.model.AdminAiRecommendation
import com.calisthenics.ai.domain.model.AdminBucketCount
import com.calisthenics.ai.domain.model.AdminCoachClientStatistics
import com.calisthenics.ai.domain.model.AdminConfirmationDecision
import com.calisthenics.ai.domain.model.AdminExercise
import com.calisthenics.ai.domain.model.AdminExerciseDeleteResult
import com.calisthenics.ai.domain.model.AdminGlobalUserStatistics
import com.calisthenics.ai.domain.model.AdminGlobalUserSummary
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminPendingConfirmation
import com.calisthenics.ai.domain.model.AdminPlatformEngagement
import com.calisthenics.ai.domain.model.AdminTimeRange
import com.calisthenics.ai.domain.model.AdminToolExecution
import com.calisthenics.ai.domain.model.AdminUser
import com.calisthenics.ai.domain.model.AdminUserDetailDossier
import com.calisthenics.ai.domain.model.AdminUserSegmentFilter
import com.calisthenics.ai.domain.model.AdminUserSegments
import com.calisthenics.ai.domain.model.BulkExerciseGenerationItem
import com.calisthenics.ai.domain.model.BulkExerciseGenerationJob
import com.calisthenics.ai.domain.model.GettingStartedCampaignAnalytics
import com.calisthenics.ai.domain.model.GettingStartedCampaignSummary
import com.calisthenics.ai.domain.model.GettingStartedProgram
import com.calisthenics.ai.domain.model.GettingStartedProgramPreview
import com.calisthenics.ai.domain.model.TargetAudience
import com.calisthenics.ai.domain.repository.AdminRepository
import com.calisthenics.ai.domain.repository.AnalysisRepository
import com.calisthenics.ai.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val adminRepository: AdminRepository = mockk()
    private val authRepository: AuthRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesUnauthorizedHttpStatusesForNavigationHandling() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)

        listOf(401, 403).forEach { statusCode ->
            coEvery { adminRepository.getDashboard() } returns Result.failure(httpError(statusCode))

            viewModel.loadDashboard()
            advanceUntilIdle()

            assertEquals(statusCode, viewModel.uiState.value.authorizationErrorCode)
        }
    }

    @Test
    fun loadExercises_Success_UpdatesUiState() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val exercise = testExercise(1L, "Handstand", true)
        val page = AdminPage(listOf(exercise), 0, 20, 1, 1, false)

        coEvery {
            adminRepository.getExercises(0, 20, "hand", null, null, null, "name", "ASC")
        } returns Result.success(page)

        viewModel.loadExercises(page = 0, search = "hand")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.exercisePage?.content?.size)
        assertEquals("Handstand", viewModel.uiState.value.exercisePage?.content?.first()?.name)
    }

    @Test
    fun createExercise_Success_LoadsExercisesAndInvokesCallback() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val exercise = testExercise(5L, "L-Sit", true)
        var callbackInvoked = false

        coEvery {
            adminRepository.createExercise(
                name = "L-Sit",
                code = null,
                category = "CORE",
                difficulty = "BEGINNER",
                description = null,
                targetMuscles = "Abs",
                instructions = null,
                imageUrl = null,
                videoUrl = null,
                equipment = "None",
                active = true
            )
        } returns Result.success(exercise)

        coEvery {
            adminRepository.getExercises(0, 20, any(), any(), any(), any(), any(), any())
        } returns Result.success(AdminPage(listOf(exercise), 0, 20, 1, 1, false))

        viewModel.createExercise(
            name = "L-Sit",
            code = null,
            category = "CORE",
            difficulty = "BEGINNER",
            description = null,
            targetMuscles = "Abs",
            instructions = null,
            imageUrl = null,
            videoUrl = null,
            equipment = "None",
            active = true,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("Exercise created successfully.", viewModel.uiState.value.message)
    }

    @Test
    fun updateExerciseStatus_Success_UpdatesItemInPage() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val activeEx = testExercise(10L, "Planche", true)
        val inactiveEx = activeEx.copy(active = false)

        val initialPage = AdminPage(listOf(activeEx), 0, 20, 1, 1, false)
        coEvery {
            adminRepository.getExercises(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(initialPage)

        viewModel.loadExercises()
        advanceUntilIdle()

        coEvery { adminRepository.updateExerciseStatus(10L, false) } returns Result.success(inactiveEx)

        viewModel.updateExerciseStatus(10L, false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.exercisePage?.content?.first()?.active ?: true)
    }

    @Test
    fun deleteExercise_Success_RemovesFromPage() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val ex1 = testExercise(20L, "Old Exercise", true)
        val initialPage = AdminPage(listOf(ex1), 0, 20, 1, 1, false)

        coEvery {
            adminRepository.getExercises(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(initialPage)

        viewModel.loadExercises()
        advanceUntilIdle()

        coEvery {
            adminRepository.deleteExercise(20L, false)
        } returns Result.success(AdminExerciseDeleteResult(true, false, "Exercise deleted."))

        var successInvoked = false
        viewModel.deleteExercise(
            exerciseId = 20L,
            softDelete = false,
            onSuccess = { successInvoked = true },
            onConflict = {}
        )
        advanceUntilIdle()

        assertTrue(successInvoked)
        assertEquals(0, viewModel.uiState.value.exercisePage?.content?.size)
    }

    @Test
    fun deleteExercise_Conflict_InvokesOnConflict() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val conflictBody = "{\"message\":\"Cannot delete in-use exercise\",\"status\":409}".toResponseBody("application/json".toMediaType())
        val http409 = HttpException(Response.error<Unit>(409, conflictBody))

        coEvery {
            adminRepository.deleteExercise(30L, false)
        } returns Result.failure(http409)

        var conflictReceived: String? = null
        viewModel.deleteExercise(
            exerciseId = 30L,
            softDelete = false,
            onSuccess = {},
            onConflict = { conflictReceived = it }
        )
        advanceUntilIdle()

        assertNotNull(conflictReceived)
        assertTrue(conflictReceived!!.contains("Cannot delete in-use exercise"))
    }

    @Test
    fun sendAdminAiMessage_Success_UpdatesMessagesAndRecommendations() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val userMsg = AdminAiMessage(1L, 100L, "USER", "What exercises should we add?", "EXERCISE_RECOMMENDATION", null)
        val assistantMsg = AdminAiMessage(2L, 100L, "ASSISTANT", "We should add Tuck Planche.", "EXERCISE_RECOMMENDATION", null)
        val rec = AdminAiRecommendation(
            id = 50L,
            type = "EXERCISE",
            status = "PENDING_APPROVAL",
            priority = "HIGH",
            title = "Add Tuck Planche",
            summary = "Missing progression",
            rationale = "Needed for Planche progression",
            exerciseDetails = null,
            programDetails = null,
            operationalDetails = null,
            conversationId = 100L,
            createdResourceId = null,
            createdResourceType = null,
            reviewNotes = null,
            reviewedAt = null,
            createdAt = null
        )
        val chatResponse = AdminAiChatResponse(
            conversationId = 100L,
            conversationTitle = "Exercise Recommendations",
            userMessage = userMsg,
            assistantMessage = assistantMsg,
            intent = "EXERCISE_RECOMMENDATION",
            recommendations = listOf(rec),
            platformOverview = null,
            trainingAnalytics = null,
            suggestedFollowUps = listOf("Suggest new workout programs.")
        )

        coEvery {
            adminRepository.sendAdminAiChatIdempotent(null, "What exercises should we add?", "LAST_30_DAYS", null, null, any())
        } returns Result.success(chatResponse)
        coEvery { adminRepository.getAdminAiConversations() } returns Result.success(
            listOf(AdminAiConversationSummary(100L, "Exercise Recommendations", "We should add Tuck Planche.", "ASSISTANT", null, null, 2))
        )
        coEvery { adminRepository.getAdminAiRecommendations(null, null) } returns Result.success(listOf(rec))

        viewModel.sendAdminAiMessage("What exercises should we add?")
        advanceUntilIdle()

        assertEquals(100L, viewModel.uiState.value.activeAiConversationId)
        assertEquals(2, viewModel.uiState.value.aiMessages.size)
        assertEquals("We should add Tuck Planche.", viewModel.uiState.value.aiMessages.last().content)
        assertEquals(1, viewModel.uiState.value.aiRecommendations.size)
        assertEquals("PENDING_APPROVAL", viewModel.uiState.value.aiRecommendations.first().status)
    }

    @Test
    fun approveAndImplementAdminRecommendation_UpdatesRecommendationStatus() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val pendingRec = AdminAiRecommendation(
            id = 77L,
            type = "EXERCISE",
            status = "PENDING_APPROVAL",
            priority = "HIGH",
            title = "Add Straddle Planche",
            summary = null,
            rationale = null,
            exerciseDetails = null,
            programDetails = null,
            operationalDetails = null,
            conversationId = null,
            createdResourceId = null,
            createdResourceType = null,
            reviewNotes = null,
            reviewedAt = null,
            createdAt = null
        )
        val approvedRec = pendingRec.copy(status = "APPROVED", createdResourceId = 900L, createdResourceType = "EXERCISE")
        val implementedRec = approvedRec.copy(status = "IMPLEMENTED")

        coEvery { adminRepository.createAdminAiRecommendations("EXERCISE", "Planche", null, null) } returns Result.success(listOf(pendingRec))
        coEvery { adminRepository.getAdminAiRecommendations(null, null) } returns Result.success(listOf(pendingRec))
        coEvery { adminRepository.approveAdminAiRecommendation(77L, null, false) } returns Result.success(approvedRec)
        coEvery { adminRepository.implementAdminAiRecommendation(77L, null) } returns Result.success(implementedRec)

        viewModel.generateAdminRecommendations(type = "EXERCISE", targetSkill = "Planche")
        advanceUntilIdle()
        assertEquals("PENDING_APPROVAL", viewModel.uiState.value.aiRecommendations.first().status)

        viewModel.approveAdminRecommendation(77L, publishExercise = false)
        advanceUntilIdle()
        assertEquals("APPROVED", viewModel.uiState.value.aiRecommendations.first().status)
        assertEquals(900L, viewModel.uiState.value.aiRecommendations.first().createdResourceId)

        viewModel.implementAdminRecommendation(77L)
        advanceUntilIdle()
        assertEquals("IMPLEMENTED", viewModel.uiState.value.aiRecommendations.first().status)
    }

    @Test
    fun loadGlobalUserAnalytics_andSegmentFilter_andUserDossierDrillDown() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val range = AdminTimeRange("LAST_30_DAYS", "2026-10-14", "2026-11-13", 30)
        val userSummary = AdminGlobalUserSummary(
            userId = 42L,
            fullName = "Bob Inactive",
            displayName = "Bob B",
            maskedEmail = "bo***@test.com",
            email = null,
            role = "ROLE_USER",
            accountCategory = "CLIENT",
            experienceLevel = "BEGINNER",
            trainingExperience = null,
            primaryGoals = "Planche",
            trainingInterests = "Planche",
            preferredFrequency = "2 days/week",
            registeredAt = "2026-09-01T10:00:00Z",
            lastActivityAt = "2026-10-20T10:00:00Z",
            activityStatus = "INACTIVE",
            daysSinceLastWorkout = 24L,
            totalWorkouts = 3L,
            completedWorkouts = 1L,
            abandonedOrIncompleteWorkouts = 2L,
            workoutCompletionRate = 33.3,
            totalPersonalRecords = 1L,
            bestPrImprovementPercent = 12.5,
            lastPrAchievedAt = "2026-10-01T10:00:00Z",
            consistencyTrend = "DECLINING",
            coachRelationshipStatus = "NO_COACH",
            coachCount = 0L,
            activeClientCount = 0L,
            aiConversationsCount = 1L,
            chatMessagesSentCount = 0L
        )
        val globalStats = AdminGlobalUserStatistics(
            timeRange = range,
            totalRegisteredUsers = 100L,
            totalRegularUsers = 90L,
            totalAdmins = 2L,
            totalCoaches = 8L,
            verifiedCoaches = 6L,
            totalClients = 90L,
            newRegistrationsInPeriod = 12L,
            activeUsersToday = 15L,
            activeUsers7Days = 40L,
            activeUsers30Days = 70L,
            activeUsersInRange = 70L,
            inactiveUsers14PlusDays = 30L,
            atRiskUsers7To14Days = 10L,
            usersWithActiveCoach = 25L,
            usersWithPendingCoachInvite = 5L,
            usersWithoutCoach = 60L,
            totalWorkoutsInRange = 200L,
            completedWorkoutsInRange = 150L,
            incompleteOrAbandonedWorkoutsInRange = 50L,
            overallWorkoutCompletionRate = 75.0,
            totalPersonalRecordsAllTime = 310L,
            newPersonalRecordsInRange = 42L,
            usersWithStalePrs30PlusDays = 45L,
            experienceLevelDistribution = listOf(AdminBucketCount("BEGINNER", 50L, 50.0)),
            activityStatusDistribution = listOf(AdminBucketCount("INACTIVE", 30L, 30.0)),
            topFitnessGoals = listOf(AdminBucketCount("Planche", 22L, 22.0)),
            missingDataNotes = listOf("ClientProfile does not store structured available_equipment.")
        )
        val coachStats = AdminCoachClientStatistics(
            timeRange = range,
            totalCoaches = 8L,
            verifiedCoaches = 6L,
            totalRelationships = 30L,
            activeRelationships = 25L,
            pendingInvitations = 5L,
            clientsWithActiveCoach = 25L,
            totalAssignedPlans = 40L,
            activeAssignedPlans = 25L,
            totalSharedPrs = 18L,
            feedbackResponseRate = 83.3,
            topCoachNames = listOf("Coach Lin (10 active, 12 plans)"),
            unassignedCoachCandidates = listOf(userSummary),
            missingDataNotes = emptyList()
        )
        val engagement = AdminPlatformEngagement(
            timeRange = range,
            dauCount = 15L,
            wauCount = 40L,
            mauCount = 70L,
            stickinessDauToMauPercent = 21.4,
            experienceComparisons = emptyList(),
            topSkillImprovers = emptyList(),
            decliningConsistencyUsers = listOf(userSummary),
            stalePrUsers = listOf(userSummary),
            totalChatMessagesInRange = 420L,
            totalAiMessagesInRange = 310L,
            workoutPlannerRunsInRange = 55L,
            missingDataNotes = emptyList()
        )
        val segments = AdminUserSegments(
            timeRange = range,
            segmentSummary = "Inactive beginner Planche users",
            matchedCount = 1L,
            returnedCount = 1,
            page = 0,
            size = 20,
            totalPages = 1,
            users = listOf(userSummary),
            experienceBreakdown = emptyList(),
            activityBreakdown = emptyList(),
            goalBreakdown = emptyList(),
            coachStatusBreakdown = emptyList(),
            missingDataNotes = emptyList()
        )
        val dossier = AdminUserDetailDossier(
            timeRange = range,
            userId = 42L,
            fullName = "Bob Inactive",
            displayName = "Bob B",
            maskedEmail = "bo***@test.com",
            email = null,
            role = "ROLE_USER",
            accountCategory = "CLIENT",
            gender = null,
            bio = null,
            experienceLevel = "BEGINNER",
            trainingExperience = null,
            primaryGoals = "Planche",
            trainingInterests = "Planche",
            preferredFrequency = "2 days/week",
            coachingLanguage = null,
            heightCm = 175.0,
            latestWeightKg = 70.0,
            latestBodyFatPercentage = null,
            latestMeasurementAt = null,
            inferredAvailableEquipment = listOf("Parallettes"),
            registeredAt = "2026-09-01T10:00:00Z",
            coachProfile = null,
            workoutSummary = null,
            personalRecordsSummary = null,
            progressSummary = null,
            activitySummary = null,
            coachRelationships = emptyList(),
            missingDataFields = listOf("clientProfile.availableEquipment (not stored; inferred from workout exercises)")
        )

        coEvery { adminRepository.getGlobalUserStatistics("LAST_30_DAYS", null, null) } returns Result.success(globalStats)
        coEvery { adminRepository.getCoachClientStatistics("LAST_30_DAYS", null, null) } returns Result.success(coachStats)
        coEvery { adminRepository.getPlatformEngagement("LAST_30_DAYS", null, null) } returns Result.success(engagement)
        coEvery { adminRepository.analyzeUserSegments(any()) } returns Result.success(segments)
        coEvery { adminRepository.getGlobalUserDetail(42L, false, "LAST_30_DAYS", null, null) } returns Result.success(dossier)

        viewModel.loadGlobalUserAnalytics("LAST_30_DAYS")
        advanceUntilIdle()

        assertEquals(100L, viewModel.uiState.value.aiGlobalUserStatistics?.totalRegisteredUsers)
        assertEquals(25L, viewModel.uiState.value.aiCoachClientStatistics?.activeRelationships)
        assertEquals(1, viewModel.uiState.value.aiUserSegments?.users?.size)
        assertFalse(viewModel.uiState.value.isLoadingGlobalUsers)

        val filter = AdminUserSegmentFilter(
            activityStatus = "INACTIVE",
            experienceLevel = "BEGINNER",
            naturalLanguageQuery = "beginner Planche no coach"
        )
        viewModel.analyzeUserSegment(filter)
        advanceUntilIdle()
        coVerify { adminRepository.analyzeUserSegments(filter) }

        viewModel.inspectGlobalUser(42L)
        advanceUntilIdle()
        assertEquals(42L, viewModel.uiState.value.inspectedUserDetail?.userId)
        assertEquals(listOf("Parallettes"), viewModel.uiState.value.inspectedUserDetail?.inferredAvailableEquipment)

        viewModel.dismissInspectedUser()
        assertEquals(null, viewModel.uiState.value.inspectedUserDetail)
    }

    @Test
    fun sendAdminAiMessage_propagatesExecutedToolsAndMatchedUsers() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val conv = AdminAiConversationSummary(9L, "Inactive Users", null, null, null, null, 2)
        val userMsg = AdminAiMessage(1L, 9L, "USER", "Show inactive users", null, null)
        val assistantMsg = AdminAiMessage(2L, 9L, "ASSISTANT", "Found 1 inactive user.", "USER_SEGMENTATION_ANALYSIS", null)
        val matched = AdminGlobalUserSummary(
            userId = 7L,
            fullName = "Bob Inactive",
            displayName = null,
            maskedEmail = "bo***@test.com",
            email = null,
            role = "ROLE_USER",
            accountCategory = "CLIENT",
            experienceLevel = "BEGINNER",
            trainingExperience = null,
            primaryGoals = "Planche",
            trainingInterests = null,
            preferredFrequency = null,
            registeredAt = null,
            lastActivityAt = null,
            activityStatus = "INACTIVE",
            daysSinceLastWorkout = 21L,
            totalWorkouts = 1L,
            completedWorkouts = 1L,
            abandonedOrIncompleteWorkouts = 0L,
            workoutCompletionRate = 100.0,
            totalPersonalRecords = 0L,
            bestPrImprovementPercent = null,
            lastPrAchievedAt = null,
            consistencyTrend = "INSUFFICIENT_DATA",
            coachRelationshipStatus = "NO_COACH",
            coachCount = 0L,
            activeClientCount = 0L,
            aiConversationsCount = 0L,
            chatMessagesSentCount = 0L
        )
        val chatResponse = AdminAiChatResponse(
            conversationId = 9L,
            conversationTitle = "Inactive Users",
            userMessage = userMsg,
            assistantMessage = assistantMsg,
            intent = "USER_SEGMENTATION_ANALYSIS",
            recommendations = emptyList(),
            platformOverview = null,
            trainingAnalytics = null,
            suggestedFollowUps = listOf("Inspect Bob Inactive"),
            executedTools = listOf(AdminToolExecution("analyze_user_segments", "EXECUTED", null, "1 matched", 1L, 12L, null)),
            timeRangeUsed = AdminTimeRange("LAST_30_DAYS", "2026-10-14", "2026-11-13", 30L),
            matchedUsers = listOf(matched),
            missingDataNotes = listOf("equipment not stored")
        )

        coEvery {
            adminRepository.sendAdminAiChatIdempotent(null, "Show inactive users", "LAST_30_DAYS", null, null, any())
        } returns Result.success(chatResponse)
        coEvery { adminRepository.getAdminAiConversations() } returns Result.success(listOf(conv))
        coEvery { adminRepository.getAdminAiRecommendations(null, null) } returns Result.success(emptyList())

        viewModel.sendAdminAiMessage("Show inactive users")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.aiLastExecutedTools.size)
        assertEquals("analyze_user_segments", viewModel.uiState.value.aiLastExecutedTools.first().toolName)
        assertEquals(1, viewModel.uiState.value.aiMatchedChatUsers.size)
        assertEquals("Bob Inactive", viewModel.uiState.value.aiMatchedChatUsers.first().fullName)
        assertEquals("LAST_30_DAYS", viewModel.uiState.value.aiLastTimeRangeUsed?.range)
        assertEquals(listOf("equipment not stored"), viewModel.uiState.value.aiMissingDataNotes)
    }

    @Test
    fun sendAdminAiMessage_gettingStartedPublishConfirmation_thenResolveApprovedUpdatesCampaignState() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val draftProgram = GettingStartedProgram(
            id = 55L,
            title = "Getting Started – 3 Days",
            targetAudience = TargetAudience.LAST_14_DAYS,
            published = false,
            version = 1
        )
        val preview = GettingStartedProgramPreview(
            program = draftProgram,
            valid = true,
            validationErrors = emptyList(),
            targetAudience = "LAST_14_DAYS",
            audienceLabel = "New Members – Last 14 Days",
            eligibleAthleteCount = 7L,
            totalAthleteCount = 20L,
            estimatedAudienceSize = 7L
        )
        val pending = AdminPendingConfirmation(
            confirmationToken = "confirm-token-55",
            actionType = "PUBLISH_PROGRAM",
            status = "PENDING",
            programId = 55L,
            programTitle = "Getting Started – 3 Days",
            targetAudience = "LAST_14_DAYS",
            audienceLabel = "New Members – Last 14 Days",
            eligibleAthleteCount = 7L,
            summary = "Confirm publishing campaign to 7 eligible athletes.",
            createdAt = null,
            expiresAt = null
        )
        val analytics = GettingStartedCampaignAnalytics(
            programId = 55L,
            programTitle = "Getting Started – 3 Days",
            published = true,
            version = 1,
            targetAudience = "LAST_14_DAYS",
            audienceLabel = "New Members – Last 14 Days",
            eligibleAthleteCount = 7L,
            modalImpressions = 5L,
            modalDismissals = 1L,
            programDetailViews = 4L,
            enrollments = 3L,
            day1Completions = 3L,
            day2Completions = 2L,
            day3Completions = 1L,
            day1CompletionRate = 100.0,
            day2CompletionRate = 66.7,
            day3CompletionRate = 33.3,
            completedEnrollments = 1L,
            overallCompletionRate = 33.3
        )
        val conv = AdminAiConversationSummary(12L, "Getting Started Campaign", null, null, null, null, 2)
        val userMsg = AdminAiMessage(1L, 12L, "USER", "Publish for last 14 days", null, null)
        val assistantMsg = AdminAiMessage(2L, 12L, "ASSISTANT", "Please confirm publication.", "CAMPAIGN_PUBLISH_CONFIRMATION", null)
        val chatResponse = AdminAiChatResponse(
            conversationId = 12L,
            conversationTitle = "Getting Started Campaign",
            userMessage = userMsg,
            assistantMessage = assistantMsg,
            intent = "CAMPAIGN_PUBLISH_CONFIRMATION",
            recommendations = emptyList(),
            platformOverview = null,
            trainingAnalytics = null,
            suggestedFollowUps = listOf("Confirm publish"),
            executedTools = listOf(AdminToolExecution("publishProgram", "CONFIRMATION_REQUIRED", null, "Confirmation required", 7L, 5L, null)),
            gettingStartedProgram = draftProgram,
            gettingStartedPreview = preview,
            campaignAnalytics = analytics,
            pendingConfirmation = pending
        )

        coEvery {
            adminRepository.sendAdminAiChatIdempotent(null, "Publish for last 14 days", "LAST_30_DAYS", null, null, any())
        } returns Result.success(chatResponse)
        coEvery { adminRepository.getAdminAiConversations() } returns Result.success(listOf(conv))
        coEvery { adminRepository.getAdminAiRecommendations(null, null) } returns Result.success(emptyList())

        viewModel.sendAdminAiMessage("Publish for last 14 days")
        advanceUntilIdle()

        assertEquals(pending, viewModel.uiState.value.aiPendingConfirmation)
        assertEquals(preview, viewModel.uiState.value.aiGettingStartedPreview)
        assertEquals(analytics, viewModel.uiState.value.aiCampaignAnalytics)
        assertEquals(false, viewModel.uiState.value.aiGettingStartedProgram?.published)

        val publishedProgram = draftProgram.copy(published = true)
        val campaignSummary = GettingStartedCampaignSummary(
            programId = 55L,
            title = "Getting Started – 3 Days",
            description = null,
            published = true,
            version = 1,
            targetAudience = "LAST_14_DAYS",
            audienceLabel = "New Members – Last 14 Days",
            eligibleAthleteCount = 7L,
            enrollments = 0L,
            publishedAt = null,
            updatedAt = null,
            days = emptyList()
        )
        val decision = AdminConfirmationDecision(
            confirmationToken = "confirm-token-55",
            actionType = "PUBLISH_PROGRAM",
            status = "CONFIRMED",
            conversationId = 12L,
            reply = "Campaign published.",
            execution = AdminToolExecution("publishProgram", "EXECUTED", null, "Published", 7L, 8L, null),
            gettingStartedProgram = publishedProgram,
            activeCampaigns = listOf(campaignSummary)
        )
        coEvery {
            adminRepository.resolveAdminAiConfirmation("confirm-token-55", true, 12L)
        } returns Result.success(decision)
        coEvery { adminRepository.getAdminAiConversationMessages(12L) } returns Result.success(
            listOf(
                userMsg,
                assistantMsg,
                AdminAiMessage(3L, 12L, "ASSISTANT", "Campaign published.", "PUBLISH_PROGRAM", null)
            )
        )

        viewModel.resolvePendingConfirmation(true)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.aiPendingConfirmation)
        assertEquals(true, viewModel.uiState.value.aiGettingStartedProgram?.published)
        assertEquals(1, viewModel.uiState.value.aiActiveCampaigns.size)
        assertEquals("Campaign published.", viewModel.uiState.value.aiMessages.last().content)
    }

    @Test
    fun resolvePendingConfirmation_cancelledClearsPendingStateAndKeepsCampaignUnpublished() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val draftProgram = GettingStartedProgram(
            id = 56L,
            title = "Getting Started – 3 Days",
            targetAudience = TargetAudience.ALL_ATHLETES,
            published = false,
            version = 1
        )
        val pending = AdminPendingConfirmation(
            confirmationToken = "cancel-token-56",
            actionType = "PUBLISH_PROGRAM",
            status = "PENDING",
            programId = 56L,
            programTitle = "Getting Started – 3 Days",
            targetAudience = "ALL_ATHLETES",
            audienceLabel = "All Athletes",
            eligibleAthleteCount = 10L,
            summary = "Confirm publishing.",
            createdAt = null,
            expiresAt = null
        )
        val conv = AdminAiConversationSummary(13L, "Cancel Campaign", null, null, null, null, 2)
        val userMsg = AdminAiMessage(1L, 13L, "USER", "Publish campaign", null, null)
        val assistantMsg = AdminAiMessage(2L, 13L, "ASSISTANT", "Confirm?", "CAMPAIGN_PUBLISH_CONFIRMATION", null)

        coEvery {
            adminRepository.sendAdminAiChatIdempotent(null, "Publish campaign", "LAST_30_DAYS", null, null, any())
        } returns Result.success(
            AdminAiChatResponse(
                conversationId = 13L,
                conversationTitle = "Cancel Campaign",
                userMessage = userMsg,
                assistantMessage = assistantMsg,
                intent = "CAMPAIGN_PUBLISH_CONFIRMATION",
                recommendations = emptyList(),
                platformOverview = null,
                trainingAnalytics = null,
                suggestedFollowUps = emptyList(),
                gettingStartedProgram = draftProgram,
                pendingConfirmation = pending
            )
        )
        coEvery { adminRepository.getAdminAiConversations() } returns Result.success(listOf(conv))
        coEvery { adminRepository.getAdminAiRecommendations(null, null) } returns Result.success(emptyList())

        viewModel.sendAdminAiMessage("Publish campaign")
        advanceUntilIdle()

        val cancelDecision = AdminConfirmationDecision(
            confirmationToken = "cancel-token-56",
            actionType = "PUBLISH_PROGRAM",
            status = "CANCELLED",
            conversationId = 13L,
            reply = "Publication cancelled. Program remains an unpublished draft.",
            execution = null,
            gettingStartedProgram = draftProgram,
            activeCampaigns = emptyList()
        )
        coEvery {
            adminRepository.resolveAdminAiConfirmation("cancel-token-56", false, 13L)
        } returns Result.success(cancelDecision)
        coEvery { adminRepository.getAdminAiConversationMessages(13L) } returns Result.success(
            listOf(
                userMsg,
                assistantMsg,
                AdminAiMessage(3L, 13L, "ASSISTANT", "Publication cancelled. Program remains an unpublished draft.", "PUBLISH_PROGRAM", null)
            )
        )

        viewModel.resolvePendingConfirmation(false)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.aiPendingConfirmation)
        assertEquals(false, viewModel.uiState.value.aiGettingStartedProgram?.published)
        assertEquals("Publication cancelled. Program remains an unpublished draft.", viewModel.uiState.value.aiMessages.last().content)
    }

    @Test
    fun searchProUsers_grantAdminPro_andRevokeAdminPro_updateStateAndInvalidateUsageCache() = runTest {
        val analysisRepository: AnalysisRepository = mockk(relaxed = true)
        val viewModel = AdminViewModel(adminRepository, authRepository, analysisRepository)

        val freeUser = testAdminUser(id = 42L, email = "athlete@example.com", fullName = "Athlete Pro", paidPro = false, adminGrantedPro = false)
        val grantedUser = testAdminUser(id = 42L, email = "athlete@example.com", fullName = "Athlete Pro", paidPro = false, adminGrantedPro = true)
        val revokedUser = freeUser.copy(adminProStatus = "REVOKED")

        coEvery {
            adminRepository.getUsers(0, 8, "42", null)
        } returns Result.success(AdminPage(listOf(freeUser), 0, 8, 1, 1, false))
        viewModel.searchProUsers(query = "42", page = 0)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.proUsersPage?.content?.size)
        assertFalse(viewModel.uiState.value.proUsersPage!!.content.first().adminGrantedPro)

        coEvery { adminRepository.grantAdminPro(42L, "VIP grant") } returns Result.success(grantedUser)
        coEvery {
            adminRepository.getUsers(0, 8, "42", null)
        } returns Result.success(AdminPage(listOf(grantedUser), 0, 8, 1, 1, false))

        viewModel.grantAdminPro(42L, "VIP grant")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.proUsersPage!!.content.first().adminGrantedPro)
        assertTrue(viewModel.uiState.value.proUsersPage!!.content.first().unlimited)
        assertEquals("Granted unlimited PRO access to Athlete Pro.", viewModel.uiState.value.proActionMessage)
        coVerify(exactly = 1) { analysisRepository.invalidateUserUsageCache() }

        coEvery { adminRepository.revokeAdminPro(42L, null) } returns Result.success(revokedUser)
        coEvery {
            adminRepository.getUsers(0, 8, "42", null)
        } returns Result.success(AdminPage(listOf(revokedUser), 0, 8, 1, 1, false))

        viewModel.revokeAdminPro(42L, null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.proUsersPage!!.content.first().adminGrantedPro)
        assertEquals("Revoked admin-granted PRO for Athlete Pro.", viewModel.uiState.value.proActionMessage)
        coVerify(exactly = 2) { analysisRepository.invalidateUserUsageCache() }
    }

    @Test
    fun revokeAdminPro_whenUserHasPaidSubscription_reportsPaidSubPreserved() = runTest {
        val analysisRepository: AnalysisRepository = mockk(relaxed = true)
        val viewModel = AdminViewModel(adminRepository, authRepository, analysisRepository)
        val paidAfterRevoke = testAdminUser(
            id = 55L,
            email = "paid@example.com",
            fullName = "Paying Athlete",
            paidPro = true,
            adminGrantedPro = false
        ).copy(membershipTier = "PRO", membershipSource = "Subscription", adminProStatus = "REVOKED")

        coEvery { adminRepository.revokeAdminPro(55L, null) } returns Result.success(paidAfterRevoke)

        viewModel.revokeAdminPro(55L)
        advanceUntilIdle()

        assertEquals(
            "Revoked admin-granted PRO for Paying Athlete. Paid subscription remains active.",
            viewModel.uiState.value.proActionMessage
        )
    }

    @Test
    fun grantAdminPro_forbidden403_setsAuthorizationErrorCodeAndProError() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        coEvery { adminRepository.grantAdminPro(7L, null) } returns Result.failure(httpError(403))

        viewModel.grantAdminPro(7L)
        advanceUntilIdle()

        assertEquals(403, viewModel.uiState.value.authorizationErrorCode)
        assertNotNull(viewModel.uiState.value.proActionError)
    }

    @Test
    fun adminAiAgent_proConfirmationFlow_invalidatesUsageCacheAndRefreshesUser() = runTest {
        val analysisRepository: AnalysisRepository = mockk(relaxed = true)
        val viewModel = AdminViewModel(adminRepository, authRepository, analysisRepository)
        val conv = AdminAiConversationSummary(
            id = 88L,
            title = "PRO Management",
            lastMessagePreview = "Confirm grant PRO",
            lastMessageRole = "ASSISTANT",
            lastInteractionAt = null,
            createdAt = null,
            messageCount = 2
        )
        val targetBefore = testAdminUser(42L, "user@example.com", "User 123", paidPro = false, adminGrantedPro = false)
        val targetAfter = testAdminUser(42L, "user@example.com", "User 123", paidPro = false, adminGrantedPro = true)

        coEvery { adminRepository.getUser(42L) } returns Result.success(targetBefore) andThen Result.success(targetAfter)
        viewModel.loadUser(42L)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.user!!.adminGrantedPro)

        val pendingPro = AdminPendingConfirmation(
            confirmationToken = "pro-confirm-88",
            actionType = "GRANT_PRO",
            status = "PENDING",
            targetUserId = 42L,
            targetUserName = "User 123",
            targetUserEmail = "user@example.com",
            targetUserCurrentTier = "FREE",
            targetUserMembershipSource = "Default",
            targetUserPaidPro = false,
            targetUserAdminGrantedPro = false,
            summary = "Confirm grant PRO to User 123"
        )
        val userMsg = AdminAiMessage(1L, 88L, "USER", "Grant PRO to user@example.com", null, null)
        val assistantMsg = AdminAiMessage(2L, 88L, "ASSISTANT", "Confirm grant PRO", "CONFIRMATION_REQUIRED", null)

        coEvery {
            adminRepository.sendAdminAiChatIdempotent(null, "Grant PRO to user@example.com", "LAST_30_DAYS", null, null, any())
        } returns Result.success(
            AdminAiChatResponse(
                conversationId = 88L,
                conversationTitle = "PRO Management",
                userMessage = userMsg,
                assistantMessage = assistantMsg,
                intent = "CONFIRMATION_REQUIRED",
                recommendations = emptyList(),
                platformOverview = null,
                trainingAnalytics = null,
                suggestedFollowUps = emptyList(),
                pendingConfirmation = pendingPro
            )
        )
        coEvery { adminRepository.getAdminAiConversations() } returns Result.success(listOf(conv))
        coEvery { adminRepository.getAdminAiRecommendations(null, null) } returns Result.success(emptyList())

        viewModel.sendAdminAiMessage("Grant PRO to user@example.com")
        advanceUntilIdle()

        assertEquals("pro-confirm-88", viewModel.uiState.value.aiPendingConfirmation?.confirmationToken)
        coVerify(exactly = 0) { analysisRepository.invalidateUserUsageCache() }

        val confirmDecision = AdminConfirmationDecision(
            confirmationToken = "pro-confirm-88",
            actionType = "GRANT_PRO",
            status = "CONFIRMED",
            conversationId = 88L,
            reply = "PRO Access Granted for User 123",
            execution = AdminToolExecution(
                toolName = "grantPro",
                status = "EXECUTED",
                parametersSummary = "targetUserId=42",
                summary = "PRO Access Granted for User 123",
                recordCount = 1L,
                durationMs = 5L,
                executedAt = null
            ),
            gettingStartedProgram = null,
            activeCampaigns = emptyList(),
            updatedUser = targetAfter
        )
        coEvery { adminRepository.resolveAdminAiConfirmation("pro-confirm-88", true, 88L) } returns Result.success(confirmDecision)
        coEvery { adminRepository.getAdminAiConversationMessages(88L) } returns Result.success(
            listOf(userMsg, assistantMsg, AdminAiMessage(3L, 88L, "ASSISTANT", "PRO Access Granted", "PRO_MANAGEMENT", null))
        )

        viewModel.resolvePendingConfirmation(true)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.aiPendingConfirmation)
        assertTrue(viewModel.uiState.value.user!!.adminGrantedPro)
        assertTrue(viewModel.uiState.value.user!!.unlimited)
        coVerify(exactly = 1) { analysisRepository.invalidateUserUsageCache() }
    }

    @Test
    fun adminAiAgent_naturalLanguageConfirmRevoke_invalidatesUsageCacheAndUpdatesUser() = runTest {
        val analysisRepository: AnalysisRepository = mockk(relaxed = true)
        val viewModel = AdminViewModel(adminRepository, authRepository, analysisRepository)
        val paidAfterRevoke = testAdminUser(55L, "paid@example.com", "Paid Athlete", paidPro = true, adminGrantedPro = false)
            .copy(membershipTier = "PRO", membershipSource = "Subscription", adminProStatus = "REVOKED")

        val confirmMsg = AdminAiMessage(10L, 90L, "USER", "Confirm", null, null)
        val replyMsg = AdminAiMessage(11L, 90L, "ASSISTANT", "Admin-Granted PRO Revoked", "PRO_MANAGEMENT", null)
        coEvery {
            adminRepository.sendAdminAiChatIdempotent(90L, "Confirm", "LAST_30_DAYS", null, null, any())
        } returns Result.success(
            AdminAiChatResponse(
                conversationId = 90L,
                conversationTitle = "PRO Management",
                userMessage = confirmMsg,
                assistantMessage = replyMsg,
                intent = "PRO_MANAGEMENT",
                recommendations = emptyList(),
                platformOverview = null,
                trainingAnalytics = null,
                suggestedFollowUps = emptyList(),
                executedTools = listOf(
                    AdminToolExecution(
                        toolName = "revokePro",
                        status = "EXECUTED",
                        parametersSummary = "targetUserId=55",
                        summary = "Admin-Granted PRO Revoked",
                        recordCount = 1L,
                        durationMs = 4L,
                        executedAt = null
                    )
                ),
                updatedProUser = paidAfterRevoke
            )
        )
        coEvery { adminRepository.getAdminAiConversationMessages(90L) } returns Result.success(emptyList())
        coEvery { adminRepository.getAdminAiConversations() } returns Result.success(emptyList())
        coEvery { adminRepository.getAdminAiRecommendations(null, null) } returns Result.success(emptyList())

        viewModel.selectAdminAiConversation(90L)
        advanceUntilIdle()
        viewModel.sendAdminAiMessage("Confirm")
        advanceUntilIdle()

        coVerify(exactly = 1) { analysisRepository.invalidateUserUsageCache() }
        assertEquals("Revoked admin-granted PRO for Paid Athlete. Paid subscription remains active.", viewModel.uiState.value.proActionMessage)
    }

    @Test
    fun loadExercisesFiltered_andBulkPublish_updatesStateAndStats() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val draft1 = testExercise(101L, "Pistol Squat", false).copy(status = "DRAFT", origin = "AI_RECOMMENDED")
        val draft2 = testExercise(102L, "Cossack Squat", false).copy(status = "DRAFT", origin = "AI_RECOMMENDED")
        val published1 = draft1.copy(active = true, status = "PUBLISHED")
        val published2 = draft2.copy(active = true, status = "PUBLISHED")

        coEvery {
            adminRepository.getExercisesFiltered(0, 20, null, null, null, null, "DRAFT", "AI_RECOMMENDED", "name", "ASC")
        } returns Result.success(AdminPage(listOf(draft1, draft2), 0, 20, 2, 1, false))
        coEvery {
            adminRepository.getExerciseStats()
        } returns Result.success(
            com.calisthenics.ai.domain.model.AdminExerciseStats(totalCount = 5L, publishedCount = 3L, draftCount = 2L, aiRecommendedCount = 2L)
        )

        viewModel.loadExercises(page = 0, status = "DRAFT", origin = "AI_RECOMMENDED")
        viewModel.loadExerciseStats()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.exercisePage?.content?.size)
        assertEquals(2L, viewModel.uiState.value.exerciseStats?.draftCount)
        assertTrue(viewModel.uiState.value.exercisePage?.content?.first()?.isAiRecommended == true)

        viewModel.selectAllVisibleDraftExercises()
        assertEquals(setOf(101L, 102L), viewModel.uiState.value.selectedExerciseIds)

        coEvery { adminRepository.bulkUpdateExerciseStatus(listOf(101L, 102L), true) } returns Result.success(listOf(published1, published2))
        coEvery {
            adminRepository.getExercisesFiltered(0, 20, null, null, null, null, "DRAFT", "AI_RECOMMENDED", "name", "ASC")
        } returns Result.success(AdminPage(emptyList(), 0, 20, 0, 1, false))

        viewModel.bulkPublishSelectedExercises()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.selectedExerciseIds.isEmpty())
        assertEquals("Published 2 exercise(s) to the public catalog.", viewModel.uiState.value.message)
    }

    @Test
    fun sendAdminAiMessage_whenExercisesCreated_immediatelyRefreshesExercisesAndStats() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val createdDraft = testExercise(201L, "Bulgarian Split Squat", false).copy(
            category = "LEGS",
            status = "DRAFT",
            origin = "AI_RECOMMENDED"
        )
        val userMsg = AdminAiMessage(1L, 300L, "USER", "Add 3 leg exercises", "EXERCISE_DRAFT_CREATION", null)
        val assistantMsg = AdminAiMessage(2L, 300L, "ASSISTANT", "Added 1 draft.", "EXERCISE_DRAFT_CREATION", null)

        val response = AdminAiChatResponse(
            conversationId = 300L,
            conversationTitle = "Exercise Drafts Added",
            userMessage = userMsg,
            assistantMessage = assistantMsg,
            intent = "EXERCISE_DRAFT_CREATION",
            recommendations = emptyList(),
            platformOverview = null,
            trainingAnalytics = null,
            suggestedFollowUps = emptyList(),
            executedTools = listOf(
                AdminToolExecution("create_exercise_drafts", "EXECUTED", "category=LEGS", "Saved 1 draft", 1L, 10L, null)
            ),
            createdExercises = listOf(createdDraft)
        )

        coEvery {
            adminRepository.sendAdminAiChatIdempotent(null, "Add 3 leg exercises", "LAST_30_DAYS", null, null, any())
        } returns Result.success(response)
        coEvery { adminRepository.getAdminAiConversations() } returns Result.success(emptyList())
        coEvery { adminRepository.getAdminAiRecommendations(null, null) } returns Result.success(emptyList())
        coEvery {
            adminRepository.getExercises(0, 20, null, null, null, null, "name", "ASC")
        } returns Result.success(AdminPage(listOf(createdDraft), 0, 20, 1, 1, false))
        coEvery {
            adminRepository.getExerciseStats()
        } returns Result.success(
            com.calisthenics.ai.domain.model.AdminExerciseStats(totalCount = 1L, publishedCount = 0L, draftCount = 1L, aiRecommendedCount = 1L)
        )

        viewModel.sendAdminAiMessage("Add 3 leg exercises")
        advanceUntilIdle()

        coVerify(exactly = 1) { adminRepository.getExercises(0, 20, null, null, null, null, "name", "ASC") }
        assertEquals(1, viewModel.uiState.value.exercisePage?.content?.size)
        assertEquals("DRAFT", viewModel.uiState.value.exercisePage?.content?.first()?.status)
        assertEquals("AI_RECOMMENDED", viewModel.uiState.value.exercisePage?.content?.first()?.origin)
    }

    private fun testAdminUser(
        id: Long,
        email: String,
        fullName: String,
        paidPro: Boolean,
        adminGrantedPro: Boolean
    ) = AdminUser(
        id = id,
        email = email,
        fullName = fullName,
        role = "ROLE_USER",
        hasCoachProfile = false,
        hasClientProfile = true,
        createdAt = "2026-11-16T00:00:00Z",
        membershipTier = if (adminGrantedPro || paidPro) "PRO" else "FREE",
        membershipSource = when {
            adminGrantedPro -> "Admin Grant"
            paidPro -> "Subscription"
            else -> "Default"
        },
        unlimited = adminGrantedPro,
        paidPro = paidPro,
        adminGrantedPro = adminGrantedPro,
        adminProStatus = if (adminGrantedPro) "ACTIVE" else "NONE",
        adminProGrantSource = if (adminGrantedPro) "ADMIN_GRANT" else null,
        grantedByAdminId = if (adminGrantedPro) 1L else null,
        adminProGrantedAt = if (adminGrantedPro) "2026-11-17T00:00:00Z" else null
    )

    private fun testExercise(id: Long, name: String, active: Boolean) = AdminExercise(
        id = id,
        name = name,
        code = name.uppercase().replace(" ", "_"),
        category = "SKILL",
        difficulty = "ADVANCED",
        description = "Test description",
        targetMuscles = "Chest",
        instructions = "Step 1",
        imageUrl = null,
        videoUrl = null,
        equipment = "None",
        active = active,
        createdAt = "2026-11-04T12:00:00Z",
        planReferencesCount = 0L,
        sessionReferencesCount = 0L,
        canDelete = true
    )

    private fun httpError(statusCode: Int): HttpException {
        val body = "denied".toResponseBody("text/plain".toMediaType())
        return HttpException(Response.error<Unit>(statusCode, body))
    }

    @Test
    fun `first message failure preserves prompt and retryLastAiMessage reuses exact same idempotencyKey`() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        coEvery { adminRepository.getAdminAiConversations() } returns Result.success(
            listOf(AdminAiConversationSummary(42L, "Existing Session", "Hi", "ASSISTANT", null, null, 2))
        )
        coEvery { adminRepository.getAdminAiPlatformOverview(any(), any(), any()) } returns Result.failure(Exception("skip"))
        coEvery { adminRepository.getAdminAiTrainingAnalytics(any(), any(), any()) } returns Result.failure(Exception("skip"))
        coEvery { adminRepository.getAdminAiRecommendations(any(), any()) } returns Result.success(emptyList())
        coEvery { adminRepository.getAdminAiConversationMessages(42L) } returns Result.success(emptyList())

        val sentKeys = mutableListOf<String?>()
        var attempt = 0
        coEvery {
            adminRepository.sendAdminAiChatIdempotent(42L, "Summarize activity", "LAST_30_DAYS", null, null, any())
        } answers {
            sentKeys += arg<String?>(5)
            attempt++
            if (attempt == 1) {
                Result.failure(java.net.SocketTimeoutException("Temporary timeout"))
            } else {
                Result.success(
                    AdminAiChatResponse(
                        conversationId = 42L,
                        conversationTitle = "Existing Session",
                        userMessage = AdminAiMessage(1L, 42L, "USER", "Summarize activity", null, null),
                        assistantMessage = AdminAiMessage(2L, 42L, "ASSISTANT", "Done", "PLATFORM_OVERVIEW", null),
                        intent = "PLATFORM_OVERVIEW",
                        recommendations = emptyList(),
                        platformOverview = null,
                        trainingAnalytics = null,
                        suggestedFollowUps = emptyList()
                    )
                )
            }
        }

        viewModel.loadAdminAiWorkspace()
        viewModel.sendAdminAiMessage("Summarize activity")
        advanceUntilIdle()

        assertEquals("Summarize activity", viewModel.uiState.value.lastFailedAiPrompt)
        assertEquals("Summarize activity", viewModel.uiState.value.restoredAiDraftPrompt)

        viewModel.retryLastAiMessage()
        advanceUntilIdle()

        assertEquals(2, sentKeys.size)
        assertEquals(sentKeys[0], sentKeys[1])
        assertEquals(42L, viewModel.uiState.value.activeAiConversationId)
        assertEquals(null, viewModel.uiState.value.lastFailedAiPrompt)
        assertEquals(2, viewModel.uiState.value.aiMessages.size)
    }

    @Test
    fun `startBulkExerciseGeneration polls running job until completed and reloads items`() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val runningJob = BulkExerciseGenerationJob(
            id = 500L,
            jobUid = "BULK-TEST500",
            conversationId = 42L,
            prompt = "Generate 100 gym exercises",
            trainingTypeFilter = "GYM",
            categoriesFilter = null,
            requestedQuantity = 100,
            batchSize = 20,
            generatedCount = 20,
            duplicateCount = 0,
            savedCount = 20,
            failedCount = 0,
            processedBatches = 1,
            totalBatchesEstimated = 5,
            progressPercent = 20,
            status = "RUNNING",
            errorMessage = null,
            summaryPosted = false,
            canResume = false,
            statusMessage = "Running batch 1/5"
        )
        val completedJob = runningJob.copy(
            generatedCount = 100,
            savedCount = 100,
            processedBatches = 5,
            progressPercent = 100,
            status = "COMPLETED",
            summaryPosted = true,
            statusMessage = "Completed: 100 saved."
        )

        coEvery {
            adminRepository.startBulkExerciseGeneration(null, 100, "GYM", null, null, any(), any())
        } returns Result.success(runningJob)
        coEvery { adminRepository.getBulkExerciseGenerationJob(500L) } returns Result.success(completedJob)
        coEvery { adminRepository.getAdminAiConversationMessages(42L) } returns Result.success(emptyList())
        coEvery { adminRepository.getExercises(any(), any(), any(), any(), any(), any()) } returns Result.success(
            AdminPage(emptyList(), 0, 20, 0, 1, false)
        )
        coEvery {
            adminRepository.getBulkExerciseGenerationItems(500L, "SAVED", 0, 25)
        } returns Result.success(
            AdminPage(
                content = listOf(
                    BulkExerciseGenerationItem(1L, 1, 900L, "Barbell Front Squat", "BARBELL_FRONT_SQUAT", "GYM", "LEGS", "INTERMEDIATE", "SAVED", "Persisted", 1)
                ),
                page = 0,
                size = 25,
                totalElements = 1L,
                totalPages = 1,
                hasNext = false
            )
        )

        viewModel.startBulkExerciseGeneration(100, "GYM")
        dispatcher.scheduler.advanceTimeBy(1500L)
        advanceUntilIdle()

        assertEquals("COMPLETED", viewModel.uiState.value.activeBulkJob?.status)
        assertEquals(100, viewModel.uiState.value.activeBulkJob?.savedCount)

        viewModel.loadBulkJobItems(500L, "SAVED", 0)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.bulkJobItemsPage?.content?.size)
        assertEquals("Barbell Front Squat", viewModel.uiState.value.bulkJobItemsPage?.content?.first()?.exerciseName)
    }
}
