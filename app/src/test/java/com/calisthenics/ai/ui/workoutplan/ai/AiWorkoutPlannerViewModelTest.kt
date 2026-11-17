package com.calisthenics.ai.ui.workoutplan.ai

import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.domain.model.AiWorkoutMetadata
import com.calisthenics.ai.domain.model.AiWorkoutPlanPreview
import com.calisthenics.ai.domain.model.AiWorkoutSessionPreview
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.usecase.CommitAiWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.GenerateAiWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlansUseCase
import com.calisthenics.ai.domain.usecase.ModifyAiWorkoutPlanUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AiWorkoutPlannerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val generateUseCase: GenerateAiWorkoutPlanUseCase = mockk()
    private val modifyUseCase: ModifyAiWorkoutPlanUseCase = mockk()
    private val commitUseCase: CommitAiWorkoutPlanUseCase = mockk()
    private val getWorkoutPlansUseCase: GetWorkoutPlansUseCase = mockk()
    private val connectivityMonitor: ConnectivityMonitor = mockk()

    private lateinit var viewModel: AiWorkoutPlannerViewModel

    private val samplePreview = AiWorkoutPlanPreview(
        planName = "6-Day Planche Routine",
        description = "Focus on planche progression",
        durationWeeks = 4,
        startDate = "2026-11-16",
        splitType = WorkoutSplitType.CUSTOM,
        sessions = listOf(
            AiWorkoutSessionPreview(
                dayOfWeek = WorkoutDayOfWeek.MONDAY,
                title = "Planche Strength",
                isRestDay = false,
                exercises = emptyList()
            )
        ),
        metadata = AiWorkoutMetadata(
            modelUsed = "gemini-flash-lite-latest",
            inputTokens = 1000,
            outputTokens = 500,
            latencyMs = 850L,
            hasConflicts = true,
            existingActivePlanName = "Active Plan A"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { connectivityMonitor.isConnected() } returns true
        coEvery { getWorkoutPlansUseCase.getLatestActivePlan(any()) } returns Result.success(
            UserWorkoutPlan(
                id = 55L,
                name = "Active Plan A",
                splitType = WorkoutSplitType.PPL,
                isActive = true
            )
        )

        viewModel = AiWorkoutPlannerViewModel(
            generateAiWorkoutPlanUseCase = generateUseCase,
            modifyAiWorkoutPlanUseCase = modifyUseCase,
            commitAiWorkoutPlanUseCase = commitUseCase,
            getWorkoutPlansUseCase = getWorkoutPlansUseCase,
            connectivityMonitor = connectivityMonitor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun generatePlan_blankPrompt_showsValidationError() = runTest {
        viewModel.onPromptChange("   ")
        viewModel.generatePlan()

        val state = viewModel.uiState.value
        assertEquals(PlannerStep.INPUT, state.step)
        assertNotNull(state.errorMessage)
        assertNotNull(state.errorResId)
    }

    @Test
    fun generatePlan_offline_showsOfflineError() = runTest {
        every { connectivityMonitor.isConnected() } returns false
        viewModel.onPromptChange("6-Day Planche split")
        viewModel.generatePlan()

        val state = viewModel.uiState.value
        assertEquals(PlannerStep.INPUT, state.step)
        assertTrue(state.errorMessage?.contains("Internet") == true)
    }

    @Test
    fun generatePlan_duplicateClicksAndRecomposeInitialize_doNotDuplicateRequestsOrWipePreview() = runTest {
        val gate = CompletableDeferred<Result<AiWorkoutPlanPreview>>()
        coEvery {
            generateUseCase(any(), any(), any(), any(), any(), any())
        } coAnswers { gate.await() }

        viewModel.initialize(clientId = null, clientName = null)
        viewModel.onPromptChange("Create 6-day planche routine")
        viewModel.generatePlan()
        // Second click while already generating must be ignored
        viewModel.generatePlan()
        // Recomposition calling initialize with same clientId must NOT wipe generating state
        viewModel.initialize(clientId = null, clientName = null)

        assertEquals(PlannerStep.GENERATING, viewModel.uiState.value.step)
        assertTrue(viewModel.uiState.value.isGenerating)

        gate.complete(Result.success(samplePreview))
        advanceUntilIdle()

        coVerify(exactly = 1) { generateUseCase(any(), any(), any(), any(), any(), any()) }
        assertEquals(PlannerStep.PREVIEW, viewModel.uiState.value.step)
        assertNotNull(viewModel.uiState.value.pendingIdempotencyKey)
    }

    @Test
    fun generatePlan_success_movesToPreviewStep() = runTest {
        coEvery {
            generateUseCase(any(), any(), any(), any(), any(), any())
        } returns Result.success(samplePreview)

        viewModel.onPromptChange("Create a 6-day planche routine")
        viewModel.generatePlan()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PlannerStep.PREVIEW, state.step)
        assertFalse(state.isGenerating)
        assertEquals("6-Day Planche Routine", state.planPreview?.planName)
        assertNull(state.errorMessage)
    }

    @Test
    fun modifyPlan_success_updatesPlanPreview() = runTest {
        coEvery {
            generateUseCase(any(), any(), any(), any(), any(), any())
        } returns Result.success(samplePreview)

        val updatedPreview = samplePreview.copy(planName = "Modified 6-Day Planche Routine")
        coEvery {
            modifyUseCase(prompt = any(), currentPlan = any(), clientId = any())
        } returns Result.success(updatedPreview)

        viewModel.onPromptChange("6-Day Planche Routine")
        viewModel.generatePlan()
        advanceUntilIdle()

        viewModel.onModifyPromptChange("Add more rest days")
        viewModel.modifyPlan()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PlannerStep.PREVIEW, state.step)
        assertEquals("Modified 6-Day Planche Routine", state.planPreview?.planName)
        assertEquals("", state.modifyPrompt)
    }

    @Test
    fun requestCommit_evenWhenPreviewMetadataStale_checksLiveActivePlanAndOpensDialog() = runTest {
        val stalePreviewNoConflict = samplePreview.copy(
            metadata = samplePreview.metadata.copy(hasConflicts = false, existingActivePlanName = null)
        )
        coEvery {
            generateUseCase(any(), any(), any(), any(), any(), any())
        } returns Result.success(stalePreviewNoConflict)
        coEvery { getWorkoutPlansUseCase.getLatestActivePlan(null) } returns Result.success(
            UserWorkoutPlan(id = 77L, name = "Newly Activated Plan", splitType = WorkoutSplitType.UPPER_LOWER, isActive = true)
        )

        viewModel.onPromptChange("Planche routine")
        viewModel.generatePlan()
        advanceUntilIdle()

        viewModel.requestCommit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showConflictDialog)
        assertEquals(77L, state.conflictPlanId)
        assertEquals("Newly Activated Plan", state.conflictPlanName)
    }

    @Test
    fun confirmConflictAndCommit_passesExpectedPlanIdAndMovesToSuccess() = runTest {
        coEvery {
            generateUseCase(any(), any(), any(), any(), any(), any())
        } returns Result.success(samplePreview)

        val savedPlan = UserWorkoutPlan(
            id = 200L,
            name = "6-Day Planche Routine",
            splitType = WorkoutSplitType.CUSTOM,
            isActive = true
        )
        coEvery {
            commitUseCase(
                plan = any(),
                idempotencyKey = any(),
                startDate = any(),
                replaceExisting = true,
                clientId = null,
                expectedActivePlanId = 55L
            )
        } returns Result.success(savedPlan)

        viewModel.onPromptChange("Plan with conflict")
        viewModel.generatePlan()
        advanceUntilIdle()

        viewModel.requestCommit()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showConflictDialog)

        viewModel.confirmConflictAndCommit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PlannerStep.SUCCESS, state.step)
        assertEquals(200L, state.committedPlan?.id)
        assertFalse(state.isCommitting)
        assertFalse(state.showConflictDialog)
    }

    @Test
    fun commitWithoutReplacing_savesPlanAsInactiveKeepingCurrentActivePlan() = runTest {
        coEvery {
            generateUseCase(any(), any(), any(), any(), any(), any())
        } returns Result.success(samplePreview)

        val savedDraft = UserWorkoutPlan(
            id = 201L,
            name = "6-Day Planche Routine",
            splitType = WorkoutSplitType.CUSTOM,
            isActive = false
        )
        coEvery {
            commitUseCase(
                plan = any(),
                idempotencyKey = any(),
                startDate = any(),
                replaceExisting = false,
                clientId = null,
                expectedActivePlanId = null
            )
        } returns Result.success(savedDraft)

        viewModel.onPromptChange("Draft plan")
        viewModel.generatePlan()
        advanceUntilIdle()

        viewModel.requestCommit()
        advanceUntilIdle()
        viewModel.commitWithoutReplacing()
        advanceUntilIdle()

        assertEquals(PlannerStep.SUCCESS, viewModel.uiState.value.step)
        assertEquals(201L, viewModel.uiState.value.committedPlan?.id)
    }

    @Test
    fun commit_whenBackendReturns409Conflict_refreshesActivePlanAndAllowsRetry() = runTest {
        val noConflictPreview = samplePreview.copy(
            metadata = samplePreview.metadata.copy(hasConflicts = false, existingActivePlanName = null)
        )
        coEvery {
            generateUseCase(any(), any(), any(), any(), any(), any())
        } returns Result.success(noConflictPreview)

        // Initially no active plan, then another device activates plan 88 before commit executes
        coEvery { getWorkoutPlansUseCase.getLatestActivePlan(null) } returnsMany listOf(
            Result.success(null),
            Result.success(UserWorkoutPlan(id = 88L, name = "Concurrent Plan", splitType = WorkoutSplitType.PPL, isActive = true))
        )

        val err409 = HttpException(
            Response.error<Any>(
                409,
                """{"status":409,"error":"Conflict","message":"Your active workout plan changed."}"""
                    .toResponseBody("application/json".toMediaTypeOrNull())
            )
        )
        val savedAfterConfirm = UserWorkoutPlan(id = 210L, name = "6-Day Planche Routine", splitType = WorkoutSplitType.CUSTOM, isActive = true)
        coEvery {
            commitUseCase(any(), any(), any(), true, null, 0L)
        } returns Result.failure(err409)
        coEvery {
            commitUseCase(any(), any(), any(), true, null, 88L)
        } returns Result.success(savedAfterConfirm)

        viewModel.onPromptChange("Plan")
        viewModel.generatePlan()
        advanceUntilIdle()
        val keyBeforeCommit = viewModel.uiState.value.pendingIdempotencyKey

        viewModel.requestCommit()
        advanceUntilIdle()

        // Backend 409 triggered fresh state fetch & opened conflict dialog for plan 88 while retaining idempotencyKey
        val conflictState = viewModel.uiState.value
        assertFalse(conflictState.isCommitting)
        assertEquals(PlannerStep.PREVIEW, conflictState.step)
        assertTrue(conflictState.showConflictDialog)
        assertEquals(88L, conflictState.conflictPlanId)
        assertEquals("Concurrent Plan", conflictState.conflictPlanName)
        assertEquals(keyBeforeCommit, conflictState.pendingIdempotencyKey)

        // Confirm retry succeeds seamlessly without app restart
        viewModel.confirmConflictAndCommit()
        advanceUntilIdle()

        assertEquals(PlannerStep.SUCCESS, viewModel.uiState.value.step)
        assertEquals(210L, viewModel.uiState.value.committedPlan?.id)
    }

    @Test
    fun generatePlan_recordsChatMessagesAndSetsCompletedStatus() = runTest {
        coEvery {
            generateUseCase(any(), any(), any(), any(), any(), any())
        } returns Result.success(samplePreview)

        viewModel.onPromptChange("Build upper body strength")
        viewModel.generatePlan()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GenerationStatus.COMPLETED, state.generationStatus)
        assertEquals(2, state.chatMessages.size)
        assertEquals(MessageRole.USER, state.chatMessages[0].role)
        assertEquals("Build upper body strength", state.chatMessages[0].text)
        assertEquals(MessageRole.AI, state.chatMessages[1].role)
        assertTrue(state.chatMessages[1].text.contains("ready"))
    }

    @Test
    fun cancelGeneration_setsCancelledStatusAndResetsGenerating() = runTest {
        val gate = CompletableDeferred<Result<AiWorkoutPlanPreview>>()
        coEvery {
            generateUseCase(any(), any(), any(), any(), any(), any())
        } coAnswers { gate.await() }

        viewModel.onPromptChange("Build core strength")
        viewModel.generatePlan()

        assertTrue(viewModel.uiState.value.isGenerating)
        viewModel.cancelGeneration()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertEquals(GenerationStatus.CANCELLED, state.generationStatus)
        assertEquals(PlannerStep.INPUT, state.step)
    }
}
