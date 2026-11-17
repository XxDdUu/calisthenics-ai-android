package com.calisthenics.ai.ui.analysis

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.CameraQuality
import com.calisthenics.ai.domain.model.IssueSeverity
import com.calisthenics.ai.domain.model.KeypointFrameModel
import com.calisthenics.ai.domain.model.KeypointItemModel
import com.calisthenics.ai.domain.model.MovementMetric
import com.calisthenics.ai.domain.model.RepBreakdown
import com.calisthenics.ai.domain.model.StructuredIssue
import com.calisthenics.ai.domain.model.VideoKeypointsDataModel
import com.calisthenics.ai.domain.usecase.GetAnalysisByIdUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisResultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAnalysisByIdUseCase: GetAnalysisByIdUseCase = mockk()
    private val tokenManager: TokenManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { tokenManager.getAccessToken() } returns "mock-jwt-token"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createFakeAnalysisResult(id: Long = 42L): AnalysisResult {
        val frame1 = KeypointFrameModel(
            frameIndex = 0,
            timestampMs = 0L,
            keypoints = listOf(
                KeypointItemModel(name = "nose", x = 0.5f, y = 0.2f, confidence = 0.95f),
                KeypointItemModel(name = "left_shoulder", x = 0.45f, y = 0.3f, confidence = 0.9f)
            )
        )
        val frame2 = KeypointFrameModel(
            frameIndex = 1,
            timestampMs = 500L,
            keypoints = listOf(
                KeypointItemModel(name = "nose", x = 0.52f, y = 0.21f, confidence = 0.96f),
                KeypointItemModel(name = "left_shoulder", x = 0.46f, y = 0.31f, confidence = 0.91f)
            )
        )
        val frame3 = KeypointFrameModel(
            frameIndex = 2,
            timestampMs = 1000L,
            keypoints = listOf(
                KeypointItemModel(name = "nose", x = 0.54f, y = 0.22f, confidence = 0.94f),
                KeypointItemModel(name = "left_shoulder", x = 0.47f, y = 0.32f, confidence = 0.89f)
            )
        )

        val keypointsData = VideoKeypointsDataModel(
            analysisId = id,
            format = "COCO_17",
            coordinateSystem = "NORMALIZED",
            totalFrames = 3,
            frames = listOf(frame1, frame2, frame3)
        )

        return AnalysisResult(
            id = id,
            userId = 1L,
            exerciseId = 10L,
            exerciseName = "Planche",
            mediaUrl = "/api/v1/video-analysis/$id/annotated",
            mediaType = "VIDEO",
            overallScore = 88.0,
            holdDurationSeconds = 3.5,
            formFeedbackList = listOf("Good shoulder lean", "Maintain hip lockout"),
            elbowAngle = 178.0,
            bodyLineAngle = 175.0,
            shoulderLeanAngle = 45.0,
            rawKeypointsJson = null,
            createdAt = "2026-11-07T12:00:00Z",
            videoMetadata = null,
            componentScores = null,
            stabilityMetrics = null,
            timeline = emptyList(),
            originalVideoUrl = "/api/v1/video-analysis/$id/original",
            annotatedVideoUrl = "/api/v1/video-analysis/$id/annotated",
            status = "COMPLETED",
            keypointsUrl = "/api/v1/video-analysis/$id/keypoints",
            keypointsData = keypointsData
        )
    }

    @Test
    fun `loadAnalysisResult success populates state with video URLs and keypoints`() = runTest(testDispatcher) {
        val fakeResult = createFakeAnalysisResult(42L)
        coEvery { getAnalysisByIdUseCase(42L) } returns Result.success(fakeResult)

        val savedStateHandle = SavedStateHandle(mapOf("analysisId" to "42"))
        val viewModel = AnalysisResultViewModel(
            savedStateHandle = savedStateHandle,
            getAnalysisByIdUseCase = getAnalysisByIdUseCase,
            tokenManager = tokenManager
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.result)
        assertEquals(42L, state.result?.id)
        assertEquals("mock-jwt-token", state.authToken)
        assertEquals(VideoDisplayMode.ANNOTATED, state.selectedVideoMode)
        assertNotNull(state.currentKeypointFrame)
        assertEquals(0, state.currentKeypointFrame?.frameIndex)
    }

    @Test
    fun `setVideoMode switches between ORIGINAL and ANNOTATED`() = runTest(testDispatcher) {
        val fakeResult = createFakeAnalysisResult(42L)
        coEvery { getAnalysisByIdUseCase(42L) } returns Result.success(fakeResult)

        val savedStateHandle = SavedStateHandle(mapOf("analysisId" to "42"))
        val viewModel = AnalysisResultViewModel(
            savedStateHandle = savedStateHandle,
            getAnalysisByIdUseCase = getAnalysisByIdUseCase,
            tokenManager = tokenManager
        )
        advanceUntilIdle()

        viewModel.setVideoMode(VideoDisplayMode.ORIGINAL)
        assertEquals(VideoDisplayMode.ORIGINAL, viewModel.uiState.value.selectedVideoMode)

        viewModel.setVideoMode(VideoDisplayMode.ANNOTATED)
        assertEquals(VideoDisplayMode.ANNOTATED, viewModel.uiState.value.selectedVideoMode)
    }

    @Test
    fun `updatePlaybackPosition synchronizes closest frame keypoints`() = runTest(testDispatcher) {
        val fakeResult = createFakeAnalysisResult(42L)
        coEvery { getAnalysisByIdUseCase(42L) } returns Result.success(fakeResult)

        val savedStateHandle = SavedStateHandle(mapOf("analysisId" to "42"))
        val viewModel = AnalysisResultViewModel(
            savedStateHandle = savedStateHandle,
            getAnalysisByIdUseCase = getAnalysisByIdUseCase,
            tokenManager = tokenManager
        )
        advanceUntilIdle()

        // Seek to 480ms -> closest is frame 1 (timestamp 500ms)
        viewModel.updatePlaybackPosition(480L)
        assertEquals(480L, viewModel.uiState.value.currentPlaybackPositionMs)
        assertEquals(1, viewModel.uiState.value.currentKeypointFrame?.frameIndex)
        assertEquals(500L, viewModel.uiState.value.currentKeypointFrame?.timestampMs)

        // Seek to 950ms -> closest is frame 2 (timestamp 1000ms)
        viewModel.updatePlaybackPosition(950L)
        assertEquals(950L, viewModel.uiState.value.currentPlaybackPositionMs)
        assertEquals(2, viewModel.uiState.value.currentKeypointFrame?.frameIndex)
        assertEquals(1000L, viewModel.uiState.value.currentKeypointFrame?.timestampMs)
    }

    @Test
    fun `requestSeek and clearSeekRequest manage seek requests properly`() = runTest(testDispatcher) {
        val fakeResult = createFakeAnalysisResult(42L)
        coEvery { getAnalysisByIdUseCase(42L) } returns Result.success(fakeResult)

        val savedStateHandle = SavedStateHandle(mapOf("analysisId" to "42"))
        val viewModel = AnalysisResultViewModel(
            savedStateHandle = savedStateHandle,
            getAnalysisByIdUseCase = getAnalysisByIdUseCase,
            tokenManager = tokenManager
        )
        advanceUntilIdle()

        assertNull(viewModel.seekRequestMs.value)

        viewModel.requestSeek(750L)
        assertEquals(750L, viewModel.seekRequestMs.value)

        viewModel.clearSeekRequest()
        assertNull(viewModel.seekRequestMs.value)
    }

    @Test
    fun `toggleSkeletonOnOriginal toggles skeleton visibility flag`() = runTest(testDispatcher) {
        val fakeResult = createFakeAnalysisResult(42L)
        coEvery { getAnalysisByIdUseCase(42L) } returns Result.success(fakeResult)

        val savedStateHandle = SavedStateHandle(mapOf("analysisId" to "42"))
        val viewModel = AnalysisResultViewModel(
            savedStateHandle = savedStateHandle,
            getAnalysisByIdUseCase = getAnalysisByIdUseCase,
            tokenManager = tokenManager
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSkeletonOnOriginal)

        viewModel.toggleSkeletonOnOriginal()
        assertFalse(viewModel.uiState.value.showSkeletonOnOriginal)

        viewModel.toggleSkeletonOnOriginal()
        assertTrue(viewModel.uiState.value.showSkeletonOnOriginal)
    }

    @Test
    fun `v1 fields populate rep breakdown, movement metrics, issues, camera quality, and track activeRepNumber`() = runTest(testDispatcher) {
        val v1Result = createFakeAnalysisResult(99L).copy(
            exerciseCode = "SQUAT",
            exerciseName = "Squat",
            repCount = 2,
            validReps = 1,
            incompleteReps = 1,
            reps = listOf(
                RepBreakdown(
                    repNumber = 1,
                    valid = true,
                    status = "VALID",
                    formScore = 91.0,
                    startTimestampMs = 400L,
                    endTimestampMs = 1900L,
                    durationMs = 1500L,
                    minAngleDeg = 82.0,
                    romDeg = 94.0,
                    depthClassification = "PARALLEL"
                ),
                RepBreakdown(
                    repNumber = 2,
                    valid = false,
                    status = "INCOMPLETE",
                    formScore = 58.0,
                    startTimestampMs = 2200L,
                    endTimestampMs = 3600L,
                    durationMs = 1400L,
                    minAngleDeg = 112.0,
                    romDeg = 64.0,
                    depthClassification = "SHALLOW",
                    issues = listOf("Insufficient depth")
                )
            ),
            movementMetrics = mapOf(
                "knee_depth" to MovementMetric(
                    key = "knee_depth",
                    name = "Knee Depth",
                    value = 82.0,
                    unit = "°",
                    targetRange = "<= 95°",
                    score = 92.0,
                    status = "GOOD"
                ),
                "hip_shift" to MovementMetric(
                    key = "hip_shift",
                    name = "Hip Shift",
                    value = null,
                    unit = "%",
                    status = "UNAVAILABLE",
                    unavailableReason = "Landmarks occluded"
                )
            ),
            issues = listOf(
                StructuredIssue(
                    code = "SHALLOW_DEPTH",
                    title = "Shallow Squat Depth",
                    message = "Hip crease stayed above knee level on rep #2",
                    severity = IssueSeverity.HIGH,
                    affectedReps = listOf(2),
                    recommendation = "Lower until thighs are at least parallel"
                )
            ),
            recommendations = listOf("Keep chest proud and brace core before descending"),
            cameraQuality = CameraQuality(
                overallQuality = "GOOD",
                viewAngle = "SIDE_VIEW",
                fullBodyVisibleRatio = 0.96,
                averageConfidence = 0.91
            )
        )
        coEvery { getAnalysisByIdUseCase(99L) } returns Result.success(v1Result)

        val viewModel = AnalysisResultViewModel(
            savedStateHandle = SavedStateHandle(mapOf("analysisId" to "99")),
            getAnalysisByIdUseCase = getAnalysisByIdUseCase,
            tokenManager = tokenManager
        )
        advanceUntilIdle()

        val loaded = viewModel.uiState.value.result
        assertNotNull(loaded)
        assertEquals(2, loaded?.repCount)
        assertEquals(1, loaded?.validReps)
        assertEquals(1, loaded?.incompleteReps)
        assertTrue(loaded?.movementMetrics?.get("hip_shift")?.isUnavailable == true)
        assertFalse(loaded?.movementMetrics?.get("knee_depth")?.isUnavailable == true)
        assertEquals("GOOD", loaded?.cameraQuality?.overallQuality)
        assertEquals(IssueSeverity.HIGH, loaded?.issues?.firstOrNull()?.severity)

        viewModel.updatePlaybackPosition(1000L)
        assertEquals(1, viewModel.uiState.value.activeRepNumber)

        viewModel.updatePlaybackPosition(2500L)
        assertEquals(2, viewModel.uiState.value.activeRepNumber)

        viewModel.updatePlaybackPosition(200L)
        assertNull(viewModel.uiState.value.activeRepNumber)
    }
}
