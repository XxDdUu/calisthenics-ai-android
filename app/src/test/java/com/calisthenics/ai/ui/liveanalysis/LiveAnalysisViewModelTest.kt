package com.calisthenics.ai.ui.liveanalysis

import com.calisthenics.ai.domain.model.LiveAnalysisResult
import com.calisthenics.ai.domain.model.SupportedExercise
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveAnalysisViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: LiveAnalysisRepository = mockk()
    private val snapshotCache = LiveSessionSnapshotCache()

    private fun createViewModel() = LiveAnalysisViewModel(repository, snapshotCache)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.getSupportedExercises() } returns Result.success(emptyList())
        coEvery { repository.createSession(any(), any()) } returns Result.success(
            com.calisthenics.ai.domain.model.LiveSessionRecord(
                id = "test-session",
                exerciseType = "SQUAT",
                status = "IN_PROGRESS"
            )
        )
        coEvery { repository.recordRep(any(), any()) } returns Result.success(
            com.calisthenics.ai.domain.model.LiveSessionRecord(
                id = "test-session",
                exerciseType = "SQUAT",
                status = "IN_PROGRESS"
            )
        )
        coEvery {
            repository.completeSession(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns Result.success(
            com.calisthenics.ai.domain.model.LiveSessionRecord(
                id = "test-session",
                exerciseType = "SQUAT",
                status = "COMPLETED"
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState contains all 10 calisthenics exercises`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val exercises = viewModel.uiState.value.availableExercises
        assertEquals(10, exercises.size)

        val codes = exercises.map { it.code }
        val expectedCodes = listOf(
            "PUSH_UP", "SQUAT", "PULL_UP", "DIPS", "HANDSTAND",
            "PLANCHE", "FRONT_LEVER", "PIKE_PUSH_UP", "L_SIT", "MUSCLE_UP"
        )
        assertEquals(expectedCodes, codes)
    }

    @Test
    fun `selectExercise changes active exercise and resets session state`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val initialSessionId = viewModel.uiState.value.sessionId
        val pushUpOption = viewModel.uiState.value.availableExercises.first { it.code == "PUSH_UP" }

        viewModel.setDropdownExpanded(true)
        assertTrue(viewModel.uiState.value.isExerciseDropdownExpanded)

        viewModel.selectExercise(pushUpOption)

        val state = viewModel.uiState.value
        assertEquals("PUSH_UP", state.exerciseCode)
        assertFalse(state.isExerciseDropdownExpanded)
        assertNotEquals(initialSessionId, state.sessionId)
        assertNull(state.liveResult)
        assertNull(state.error)
        assertEquals("Push-up", state.currentExerciseOption.displayName)
        assertTrue(state.currentExerciseOption.sideView)
    }

    @Test
    fun `pull up option has front or side camera recommendation and sideView is false`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val pullUpOption = viewModel.uiState.value.availableExercises.first { it.code == "PULL_UP" }
        viewModel.selectExercise(pullUpOption)

        val state = viewModel.uiState.value
        assertEquals("PULL_UP", state.currentExerciseOption.code)
        assertFalse(state.currentExerciseOption.sideView)
        assertEquals("REPETITION", state.currentExerciseOption.category)
    }

    @Test
    fun `toggleRunning updates isRunning flag`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRunning)
        viewModel.toggleRunning()
        assertTrue(viewModel.uiState.value.isRunning)
        viewModel.toggleRunning()
        assertFalse(viewModel.uiState.value.isRunning)
    }

    @Test
    fun `loadCatalog merges remote exercise metadata`() = runTest(dispatcher) {
        val remoteCatalog = listOf(
            SupportedExercise(
                exercise = "push_up",
                name = "Standard Push-up",
                type = "repetition",
                cameraView = "Side-on 90-degree angle",
                variations = listOf("standard", "diamond")
            )
        )
        coEvery { repository.getSupportedExercises() } returns Result.success(remoteCatalog)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val pushUp = viewModel.uiState.value.availableExercises.first { it.code == "PUSH_UP" }
        assertEquals("Standard Push-up", pushUp.displayName)
        assertEquals("Side-on 90-degree angle", pushUp.recommendedView)
    }

    @Test
    fun `onFrameCaptured handles 401 Unauthorized with meaningful session expired message`() = runTest(dispatcher) {
        val dummyFile = java.io.File.createTempFile("test-frame", ".jpg")
        val httpException = retrofit2.HttpException(
            retrofit2.Response.error<Any>(
                401,
                "{\"status\":401}".toResponseBody("application/json".toMediaTypeOrNull())
            )
        )
        coEvery { repository.analyzeFrame(any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.failure(httpException)

        val viewModel = createViewModel()
        viewModel.ioDispatcher = dispatcher
        viewModel.timeProvider = { 1000L }
        advanceUntilIdle()

        viewModel.onFrameCaptured(
            imageFile = dummyFile,
            frameSequence = 1L,
            timestampMs = 1000L,
            sideView = true,
            exerciseCode = "PLANCHE"
        )
        advanceUntilIdle()

        assertEquals("Session expired or unauthorized. Please re-authenticate.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onFrameCaptured updates liveResult and ignores older out-of-order frame`() = runTest(dispatcher) {
        val dummyFile1 = java.io.File.createTempFile("test-frame-1", ".jpg")
        val dummyFile2 = java.io.File.createTempFile("test-frame-2", ".jpg")

        val resultSeq1 = LiveAnalysisResult(status = "VALID", exercise = "PLANCHE", score = 70.0)
        val resultSeq2 = LiveAnalysisResult(status = "VALID", exercise = "PLANCHE", score = 90.0)

        val viewModel = createViewModel()
        viewModel.ioDispatcher = dispatcher
        var currentTime = 1000L
        viewModel.timeProvider = { currentTime }
        advanceUntilIdle()

        // Seq 2 completes first
        coEvery { repository.analyzeFrame(any(), any(), any(), eq(2L), any(), any(), any(), any()) } returns Result.success(resultSeq2)
        viewModel.onFrameCaptured(
            imageFile = dummyFile2,
            frameSequence = 2L,
            timestampMs = 2000L,
            sideView = true,
            exerciseCode = "PLANCHE"
        )
        advanceUntilIdle()

        assertEquals(90.0, viewModel.uiState.value.liveResult?.score)

        // Advance simulated time past throttle window
        currentTime += 500L

        // Seq 1 completes after Seq 2 (delayed / out of order)
        coEvery { repository.analyzeFrame(any(), any(), any(), eq(1L), any(), any(), any(), any()) } returns Result.success(resultSeq1)
        viewModel.onFrameCaptured(
            imageFile = dummyFile1,
            frameSequence = 1L,
            timestampMs = 1000L,
            sideView = true,
            exerciseCode = "PLANCHE"
        )
        advanceUntilIdle()

        // Score remains 90.0 from Seq 2, not overwritten by delayed Seq 1
        assertEquals(90.0, viewModel.uiState.value.liveResult?.score)
    }

    @Test
    fun `starting and stopping session invokes repository createSession and completeSession and triggers summary navigation`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.ioDispatcher = dispatcher
        advanceUntilIdle()

        viewModel.setRunning(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRunning)

        val sessionId = viewModel.uiState.value.sessionId
        io.mockk.coVerify { repository.createSession(eq("PLANCHE"), eq(sessionId)) }

        viewModel.setRunning(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRunning)
        assertEquals(sessionId, viewModel.uiState.value.navigateToSummarySessionId)
        assertNotNull(snapshotCache.get(sessionId))

        io.mockk.coVerify {
            repository.completeSession(
                eq(sessionId),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `when frame analysis returns completed reps, recordRep is called for each rep`() = runTest(dispatcher) {
        val dummyFile = java.io.File.createTempFile("test-frame-rep", ".jpg")
        val analysis = LiveAnalysisResult(
            status = "VALID",
            exercise = "PUSH_UP",
            repCount = 2,
            score = 85.0,
            elbowAngle = 90.0,
            minElbowAngle = 88.0,
            depth = "FULL"
        )
        coEvery { repository.analyzeFrame(any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.success(analysis)

        val viewModel = createViewModel()
        viewModel.ioDispatcher = dispatcher
        advanceUntilIdle()

        viewModel.onFrameCaptured(
            imageFile = dummyFile,
            frameSequence = 1L,
            timestampMs = 1000L,
            sideView = true,
            exerciseCode = "PUSH_UP"
        )
        advanceUntilIdle()

        io.mockk.coVerify(exactly = 2) { repository.recordRep(eq(viewModel.uiState.value.sessionId), any()) }
    }
}
