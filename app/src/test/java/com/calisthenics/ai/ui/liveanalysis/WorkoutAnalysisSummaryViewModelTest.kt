package com.calisthenics.ai.ui.liveanalysis

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.domain.model.AiPerformanceSummary
import com.calisthenics.ai.domain.model.DetectedFormError
import com.calisthenics.ai.domain.model.ErrorSeverity
import com.calisthenics.ai.domain.model.ExerciseFormMetric
import com.calisthenics.ai.domain.model.LiveRepRecord
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutAnalysisSummaryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: LiveAnalysisRepository
    private lateinit var snapshotCache: LiveSessionSnapshotCache

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        snapshotCache = LiveSessionSnapshotCache()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_withSnapshot_rendersImmediatelyAndFetchesAiOnce() = runTest {
        val snapshot = samplePushUpSession(id = "push-1", aiStatus = "PENDING")
        snapshotCache.put(snapshot)

        val completedAi = sampleAiSummary()
        coEvery { repository.getSessionDetails("push-1") } returns Result.success(snapshot)
        coEvery { repository.generateSessionAiSummary("push-1") } returns Result.success(completedAi)

        val vm = WorkoutAnalysisSummaryViewModel(
            repository = repository,
            snapshotCache = snapshotCache,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "push-1"))
        )

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.session)
        assertEquals("PUSH_UP", state.session?.exerciseType)
        assertEquals(2, state.session?.reps?.size)
        assertEquals("COMPLETED", state.aiStatus)
        assertEquals(completedAi, state.aiSummary)
        assertEquals("COMPLETED", snapshotCache.get("push-1")?.aiSummaryStatus)

        coVerify(exactly = 1) { repository.generateSessionAiSummary("push-1") }
    }

    @Test
    fun init_whenHistoryAlreadyHasCompletedAi_doesNotCallGeminiAgain() = runTest {
        val completedAi = sampleAiSummary()
        val persistedHistory = samplePushUpSession(
            id = "hist-1",
            aiStatus = "COMPLETED",
            aiRecommendations = completedAi
        )
        coEvery { repository.getSessionDetails("hist-1") } returns Result.success(persistedHistory)

        val vm = WorkoutAnalysisSummaryViewModel(
            repository = repository,
            snapshotCache = snapshotCache,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "hist-1"))
        )

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("COMPLETED", state.aiStatus)
        assertEquals(completedAi, state.aiSummary)
        assertEquals(persistedHistory, state.session)

        coVerify(exactly = 1) { repository.getSessionDetails("hist-1") }
        coVerify(exactly = 0) { repository.generateSessionAiSummary(any()) }
    }

    @Test
    fun init_whenGeminiFails_preservesStatsAndMarksUnavailable() = runTest {
        val squatSession = LiveSessionRecord(
            id = "squat-1",
            exerciseType = "SQUAT",
            status = "COMPLETED",
            totalReps = 5,
            correctReps = 4,
            incorrectReps = 1,
            overallScore = 84.5,
            averageScore = 84.5,
            analysisConfidence = 0.91,
            durationSeconds = 42.0,
            exerciseMetrics = listOf(
                ExerciseFormMetric("squat_depth", "Squat Depth", 88.0, "°", 86.0, 0.9, "VALID"),
                ExerciseFormMetric("balance", "Balance", null, "%", null, 0.0, "UNAVAILABLE", "Ankles occluded")
            ),
            detectedErrors = listOf(
                DetectedFormError("KNEE_VALGUS", "Knee Cave", 1, ErrorSeverity.MEDIUM, listOf(12000L), listOf(3), "Push knees outward")
            ),
            aiSummaryStatus = "PENDING"
        )
        coEvery { repository.getSessionDetails("squat-1") } returns Result.success(squatSession)
        coEvery { repository.generateSessionAiSummary("squat-1") } returns Result.failure(RuntimeException("Gemini unavailable"))

        val vm = WorkoutAnalysisSummaryViewModel(
            repository = repository,
            snapshotCache = snapshotCache,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "squat-1"))
        )

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isAiLoading)
        assertNull(state.error)
        assertEquals(84.5, state.session?.overallScore ?: 0.0, 0.01)
        assertEquals(5, state.session?.totalReps)
        assertEquals("UNAVAILABLE", state.aiStatus)
        assertNull(state.aiSummary)
    }

    @Test
    fun init_waitsForInFlightCompletionBeforeQueryingBackend() = runTest {
        val stdDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(stdDispatcher)

        val snapshot = samplePushUpSession(id = "inflight-1", aiStatus = "PENDING")
        snapshotCache.put(snapshot)
        snapshotCache.markCompletionStarted("inflight-1")

        coEvery { repository.getSessionDetails("inflight-1") } returns Result.success(snapshot)
        coEvery { repository.generateSessionAiSummary("inflight-1") } returns Result.success(sampleAiSummary())

        val vm = WorkoutAnalysisSummaryViewModel(
            repository = repository,
            snapshotCache = snapshotCache,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "inflight-1"))
        )
        advanceUntilIdle()

        // Summary must already be visible immediately from cache while backend completion is still running
        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.session)
        coVerify(exactly = 0) { repository.getSessionDetails("inflight-1") }

        snapshotCache.markCompletionFinished("inflight-1", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.getSessionDetails("inflight-1") }
        coVerify(exactly = 1) { repository.generateSessionAiSummary("inflight-1") }
        assertEquals("COMPLETED", vm.uiState.value.aiStatus)
    }

    @Test
    fun selectRep_updatesAndClearsSelectedRep() = runTest {
        val session = samplePushUpSession(id = "rep-inspect-1", aiStatus = "COMPLETED", aiRecommendations = sampleAiSummary())
        coEvery { repository.getSessionDetails("rep-inspect-1") } returns Result.success(session)

        val vm = WorkoutAnalysisSummaryViewModel(
            repository = repository,
            snapshotCache = snapshotCache,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "rep-inspect-1"))
        )

        val rep2 = session.reps[1]
        vm.selectRep(rep2)
        assertEquals(rep2, vm.uiState.value.selectedRep)

        vm.selectRep(null)
        assertNull(vm.uiState.value.selectedRep)
    }

    @Test
    fun zeroRepsAndHoldShareText_doNotFabricateMissingScores() = runTest {
        val zeroRepSession = LiveSessionRecord(
            id = "zero-1",
            exerciseType = "PLANCHE",
            status = "COMPLETED",
            totalReps = 0,
            correctReps = 0,
            incorrectReps = 0,
            overallScore = null,
            averageScore = null,
            analysisConfidence = null,
            peakHoldDurationSeconds = null,
            durationSeconds = 9.0,
            exerciseMetrics = listOf(
                ExerciseFormMetric("scapular_positioning", "Scapular Positioning", null, "", null, 0.0, "UNAVAILABLE", "Insufficient data")
            ),
            reps = emptyList(),
            aiSummaryStatus = "UNAVAILABLE"
        )
        coEvery { repository.getSessionDetails("zero-1") } returns Result.success(zeroRepSession)
        coEvery { repository.generateSessionAiSummary("zero-1") } returns Result.success(
            AiPerformanceSummary(status = "UNAVAILABLE")
        )

        val vm = WorkoutAnalysisSummaryViewModel(
            repository = repository,
            snapshotCache = snapshotCache,
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "zero-1"))
        )

        val shareText = vm.buildShareSummaryText(zeroRepSession)
        assertTrue(shareText.contains("Overall Form Score: Unavailable"))
        assertTrue(shareText.contains("Total Reps: 0"))
        assertFalse(shareText.contains("Scapular Positioning"))

        val frontLeverHold = zeroRepSession.copy(
            id = "fl-1",
            exerciseType = "FRONT_LEVER",
            overallScore = 88.5,
            peakHoldDurationSeconds = 11.2,
            exerciseMetrics = listOf(
                ExerciseFormMetric("body_horizontal_alignment", "Body Horizontal Alignment", 174.0, "°", 89.0, 0.92, "VALID")
            ),
            detectedErrors = listOf(
                DetectedFormError("HIP_DROP", "Hip Drop", 2, ErrorSeverity.MEDIUM, listOf(4000L, 8000L), emptyList(), "Engage glutes")
            ),
            aiRecommendations = sampleAiSummary(),
            aiSummaryStatus = "COMPLETED"
        )
        val holdShareText = vm.buildShareSummaryText(frontLeverHold)
        assertTrue(holdShareText.contains("Peak Hold: 11.2s"))
        assertTrue(holdShareText.contains("Body Horizontal Alignment: 89% (174.0°)"))
        assertTrue(holdShareText.contains("Hip Drop: 2x [MEDIUM]"))
        assertTrue(holdShareText.contains("AI Coach Assessment:"))
    }

    private fun samplePushUpSession(
        id: String,
        aiStatus: String,
        aiRecommendations: AiPerformanceSummary? = null
    ) = LiveSessionRecord(
        id = id,
        exerciseType = "PUSH_UP",
        status = "COMPLETED",
        totalReps = 2,
        correctReps = 2,
        incorrectReps = 0,
        overallScore = 88.0,
        averageScore = 88.0,
        analysisConfidence = 0.93,
        durationSeconds = 18.0,
        exerciseMetrics = listOf(
            ExerciseFormMetric("body_alignment", "Body Alignment", 172.0, "°", 88.0, 0.94, "VALID"),
            ExerciseFormMetric("tempo_consistency", "Tempo Consistency", 3.1, "s/rep", 92.0, 0.90, "VALID")
        ),
        detectedErrors = listOf(
            DetectedFormError("HIP_SAG", "Hip Sagging", 1, ErrorSeverity.MEDIUM, listOf(3500L), listOf(1), "Brace core")
        ),
        aiSummary = aiRecommendations?.overallAssessment,
        aiRecommendations = aiRecommendations,
        aiSummaryStatus = aiStatus,
        reps = listOf(
            LiveRepRecord(
                repNumber = 1,
                score = 84.0,
                minElbowAngle = 92.0,
                minJointAngle = 92.0,
                depth = "GOOD",
                completionStatus = "SUCCESS",
                durationMs = 2400L,
                timestampOffsetMs = 3500L,
                isValid = true,
                detectedErrors = listOf("Hip Sagging")
            ),
            LiveRepRecord(
                repNumber = 2,
                score = 92.0,
                minElbowAngle = 88.0,
                minJointAngle = 88.0,
                depth = "GOOD",
                completionStatus = "SUCCESS",
                durationMs = 2200L,
                timestampOffsetMs = 6000L,
                isValid = true
            )
        )
    )

    private fun sampleAiSummary() = AiPerformanceSummary(
        status = "COMPLETED",
        overallAssessment = "Strong push-up mechanics with steady tempo across both repetitions.",
        strengths = listOf("Consistent chest-to-floor depth"),
        weaknesses = listOf("Minor hip sag on rep 1"),
        recommendations = listOf(
            "Brace abs before descending",
            "Keep glutes engaged throughout the press",
            "Maintain 2-second lowering tempo"
        ),
        nextSessionFocus = "Aim for 3x8 strict push-ups maintaining straight line",
        modelUsed = "gemini-flash-lite-latest"
    )
}
