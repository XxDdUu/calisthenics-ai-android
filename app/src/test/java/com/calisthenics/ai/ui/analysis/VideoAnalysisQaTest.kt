package com.calisthenics.ai.ui.analysis

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.data.remote.dto.VideoAnalysisResponseDto
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.CameraQuality
import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.model.IssueSeverity
import com.calisthenics.ai.domain.model.KeypointFrameModel
import com.calisthenics.ai.domain.model.KeypointItemModel
import com.calisthenics.ai.domain.model.MovementMetric
import com.calisthenics.ai.domain.model.QuotaEntitlement
import com.calisthenics.ai.domain.model.RepBreakdown
import com.calisthenics.ai.domain.model.StructuredIssue
import com.calisthenics.ai.domain.model.VideoKeypointsDataModel
import com.calisthenics.ai.domain.repository.AnalysisRepository
import com.calisthenics.ai.domain.usecase.AnalyzeImageUseCase
import com.calisthenics.ai.domain.usecase.AnalyzeVideoUseCase
import com.calisthenics.ai.domain.usecase.GetAnalysisByIdUseCase
import com.calisthenics.ai.domain.usecase.GetExercisesUseCase
import com.calisthenics.ai.ui.exercise.ExerciseSelectionViewModel
import com.calisthenics.ai.ui.exercise.UpgradeDialogReason
import com.calisthenics.ai.ui.video.VideoSelectionViewModel
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class VideoAnalysisQaTest {

    private val testDispatcher = StandardTestDispatcher()
    private val gson = Gson()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. DTO-to-Domain Mapping for SQUAT, PUSH_UP, PULL_UP v1.0 JSON Responses
    // =========================================================================

    @Test
    fun `v1 SQUAT JSON maps reps, UNAVAILABLE movementMetrics, LOW-MEDIUM-HIGH issues, recommendations, cameraQuality, and quota`() {
        val squatJson = """
            {
              "id": 101,
              "userId": 7,
              "exerciseId": 12,
              "exerciseCode": "SQUAT",
              "exerciseName": "Bodyweight Squat",
              "schemaVersion": "1.0",
              "status": "COMPLETED",
              "overallScore": 84.5,
              "formAccuracy": 86.0,
              "stabilityScore": 82.0,
              "alignmentScore": 85.0,
              "repCount": 3,
              "validReps": 2,
              "incompleteReps": 1,
              "durationMs": 7200,
              "width": 640,
              "height": 480,
              "fps": 30.0,
              "originalVideoUrl": "/api/v1/video-analysis/101/original",
              "annotatedVideoUrl": "/api/v1/video-analysis/101/annotated",
              "keypointsUrl": "/api/v1/video-analysis/101/keypoints",
              "reps": [
                {
                  "repNumber": 1,
                  "isValid": true,
                  "status": "VALID",
                  "formScore": 91.0,
                  "startTimestampMs": 400,
                  "endTimestampMs": 2200,
                  "durationMs": 1800,
                  "minAngleDeg": 86.5,
                  "romDeg": 88.5,
                  "depthClassification": "GOOD",
                  "issues": [],
                  "feedback": "Clean depth and upright chest."
                },
                {
                  "repNumber": 2,
                  "isValid": false,
                  "status": "INCOMPLETE",
                  "formScore": 61.0,
                  "startTimestampMs": 2600,
                  "endTimestampMs": 4300,
                  "durationMs": 1700,
                  "minAngleDeg": 124.0,
                  "romDeg": 51.0,
                  "depthClassification": "SHALLOW",
                  "issues": ["SHALLOW_DEPTH"],
                  "feedback": "Descend until thighs reach parallel."
                },
                {
                  "repNumber": 3,
                  "isValid": true,
                  "status": "VALID",
                  "formScore": 89.5,
                  "startTimestampMs": 4700,
                  "endTimestampMs": 6600,
                  "durationMs": 1900,
                  "minAngleDeg": 88.0,
                  "romDeg": 87.0,
                  "depthClassification": "GOOD",
                  "issues": [],
                  "feedback": "Strong recovery."
                }
              ],
              "movementMetrics": {
                "squat_depth": {
                  "name": "squat_depth",
                  "label": "Squat Depth",
                  "value": 87.3,
                  "unit": "deg",
                  "targetRange": "<= 100 deg",
                  "score": 90.0,
                  "confidence": 0.94,
                  "status": "VALID"
                },
                "knee_alignment": {
                  "name": "knee_alignment",
                  "label": "Knee Alignment",
                  "value": null,
                  "unit": "ratio",
                  "targetRange": ">= 0.82",
                  "score": null,
                  "confidence": 0.0,
                  "status": "UNAVAILABLE",
                  "reason": "Contralateral knee and ankle occluded in side view"
                }
              },
              "issues": [
                {
                  "code": "MINOR_TEMPO_VARIANCE",
                  "title": "Minor Tempo Variance",
                  "message": "Descent speed varied slightly on rep 1.",
                  "severity": "LOW",
                  "affectedReps": [1],
                  "recommendation": "Use a steady 2-second descent."
                },
                {
                  "code": "SHALLOW_DEPTH",
                  "title": "Shallow Squat Depth",
                  "message": "Rep 2 stopped above parallel.",
                  "severity": "MEDIUM",
                  "affectedReps": [2],
                  "measuredValue": 124.0,
                  "targetRange": "<= 100 deg",
                  "recommendation": "Sink hips slightly lower at bottom."
                },
                {
                  "code": "EXCESSIVE_FORWARD_LEAN",
                  "title": "Excessive Forward Lean",
                  "message": "Torso leaned past 45 degrees.",
                  "severity": "HIGH",
                  "affectedReps": [2, 3],
                  "recommendation": "Keep chest proud and brace core."
                }
              ],
              "recommendations": [
                "Sink hips to parallel on every rep.",
                "Maintain an upright torso throughout ascent."
              ],
              "cameraQuality": {
                "overallQuality": "FAIR",
                "viewAngle": "SIDE",
                "fullBodyVisibleRatio": 0.88,
                "averageConfidence": 0.91,
                "warnings": ["Far-side leg partially occluded; knee valgus unavailable."],
                "sideViewDetected": true
              },
              "quota": {
                "userId": 7,
                "membershipTier": "FREE",
                "dailyLimit": 1,
                "usedToday": 1,
                "remainingToday": 0,
                "liveAnalysisUnlocked": false,
                "timezone": "Asia/Ho_Chi_Minh",
                "quotaDate": "2026-11-16",
                "resetsAt": "2026-11-17T00:00:00+07:00"
              }
            }
        """.trimIndent()

        val domain = gson.fromJson(squatJson, VideoAnalysisResponseDto::class.java).toDomain()

        assertEquals("1.0", domain.schemaVersion)
        assertEquals("SQUAT", domain.exerciseCode)
        assertEquals(3, domain.repCount)
        assertEquals(2, domain.validReps)
        assertEquals(1, domain.incompleteReps)
        assertEquals(3, domain.reps.size)
        assertTrue(domain.reps[0].valid)
        assertFalse(domain.reps[1].valid)
        assertEquals("SHALLOW", domain.reps[1].depthClassification)
        assertEquals(2600L, domain.reps[1].startTimestampMs)

        val depthMetric = domain.movementMetrics["squat_depth"]
        assertNotNull(depthMetric)
        assertFalse(depthMetric!!.isUnavailable)
        assertEquals(87.3, depthMetric.value!!, 0.01)
        assertEquals(90.0, depthMetric.score!!, 0.01)

        val kneeAlign = domain.movementMetrics["knee_alignment"]
        assertNotNull(kneeAlign)
        assertEquals("UNAVAILABLE", kneeAlign!!.status)
        assertTrue(kneeAlign.isUnavailable)
        assertNull(kneeAlign.value)
        assertNull(kneeAlign.score)

        assertEquals(3, domain.issues.size)
        assertEquals(IssueSeverity.LOW, domain.issues[0].severity)
        assertEquals(IssueSeverity.MEDIUM, domain.issues[1].severity)
        assertEquals(IssueSeverity.HIGH, domain.issues[2].severity)
        assertEquals(listOf(2, 3), domain.issues[2].affectedReps)

        assertEquals(2, domain.recommendations.size)
        assertNotNull(domain.cameraQuality)
        assertEquals("FAIR", domain.cameraQuality?.overallQuality)
        assertEquals("SIDE", domain.cameraQuality?.viewAngle)
        assertEquals(true, domain.cameraQuality?.sideViewDetected)

        assertNotNull(domain.quota)
        assertEquals("FREE", domain.quota?.membershipTier)
        assertEquals(1, domain.quota?.dailyLimit)
        assertEquals(1, domain.quota?.usedToday)
        assertEquals(0, domain.quota?.remainingToday)
        assertFalse(domain.quota!!.liveAnalysisUnlocked)
        assertEquals("Asia/Ho_Chi_Minh", domain.quota?.timezone)
    }

    @Test
    fun `v1 PUSH_UP JSON maps reps, UNAVAILABLE body_alignment, issues, recommendations, cameraQuality, and PRO quota`() {
        val pushUpJson = """
            {
              "id": 102,
              "userId": 8,
              "exerciseCode": "PUSH_UP",
              "exerciseName": "Push-Up",
              "schemaVersion": "1.0",
              "status": "COMPLETED",
              "overallScore": 79.0,
              "repCount": 2,
              "validReps": 1,
              "incompleteReps": 1,
              "durationMs": 5000,
              "reps": [
                {
                  "repNumber": 1,
                  "isValid": true,
                  "status": "VALID",
                  "formScore": 88.0,
                  "startTimestampMs": 300,
                  "endTimestampMs": 2100,
                  "durationMs": 1800,
                  "minAngleDeg": 85.0,
                  "romDeg": 90.0,
                  "depthClassification": "GOOD"
                },
                {
                  "repNumber": 2,
                  "isValid": false,
                  "status": "INCOMPLETE",
                  "formScore": 64.0,
                  "startTimestampMs": 2500,
                  "endTimestampMs": 4200,
                  "durationMs": 1700,
                  "minAngleDeg": 115.0,
                  "romDeg": 60.0,
                  "depthClassification": "SHALLOW"
                }
              ],
              "movementMetrics": {
                "range_of_motion": {
                  "name": "range_of_motion",
                  "value": 85.0,
                  "unit": "deg",
                  "score": 92.0,
                  "status": "VALID"
                },
                "body_alignment": {
                  "name": "body_alignment",
                  "value": null,
                  "unit": "deg",
                  "score": null,
                  "status": "UNAVAILABLE",
                  "reason": "Ankles occluded outside frame"
                }
              },
              "issues": [
                {
                  "code": "HIP_SAG",
                  "title": "Hip Sag",
                  "message": "Hips dropped below shoulder-ankle line.",
                  "severity": "HIGH",
                  "affectedReps": [1, 2],
                  "recommendation": "Squeeze glutes and brace abs."
                },
                {
                  "code": "SHALLOW_DEPTH",
                  "title": "Shallow Push-up",
                  "message": "Chest did not reach target depth on rep 2.",
                  "severity": "MEDIUM",
                  "affectedReps": [2]
                }
              ],
              "recommendations": ["Keep ankles inside frame for body-line check."],
              "cameraQuality": {
                "overallQuality": "POOR",
                "viewAngle": "SIDE",
                "fullBodyVisibleRatio": 0.30,
                "averageConfidence": 0.72,
                "warnings": ["Ankles cut off on left edge"]
              },
              "quota": {
                "userId": 8,
                "membershipTier": "PRO",
                "dailyLimit": 10,
                "usedToday": 2,
                "remainingToday": 8,
                "liveAnalysisUnlocked": true,
                "timezone": "Asia/Ho_Chi_Minh"
              }
            }
        """.trimIndent()

        val domain = gson.fromJson(pushUpJson, VideoAnalysisResponseDto::class.java).toDomain()

        assertEquals("PUSH_UP", domain.exerciseCode)
        assertEquals(2, domain.repCount)
        assertEquals(1, domain.validReps)
        assertEquals(1, domain.incompleteReps)
        assertTrue(domain.movementMetrics["body_alignment"]!!.isUnavailable)
        assertNull(domain.movementMetrics["body_alignment"]!!.value)
        assertNull(domain.movementMetrics["body_alignment"]!!.score)
        assertEquals(IssueSeverity.HIGH, domain.issues[0].severity)
        assertEquals(IssueSeverity.MEDIUM, domain.issues[1].severity)
        assertEquals("POOR", domain.cameraQuality?.overallQuality)
        assertTrue(domain.quota!!.isPro)
        assertTrue(domain.quota!!.liveAnalysisUnlocked)
        assertEquals(8, domain.quota?.remainingToday)
    }

    @Test
    fun `v1 PULL_UP JSON maps reps, UNAVAILABLE body_swing and chin_to_bar, issues, recommendations, cameraQuality, and quota`() {
        val pullUpJson = """
            {
              "id": 103,
              "userId": 9,
              "exerciseCode": "PULL_UP",
              "exerciseName": "Pull-Up",
              "schemaVersion": "1.0",
              "status": "COMPLETED",
              "overallScore": 76.0,
              "repCount": 2,
              "validReps": 1,
              "incompleteReps": 1,
              "durationMs": 5600,
              "reps": [
                {
                  "repNumber": 1,
                  "isValid": true,
                  "status": "VALID",
                  "formScore": 84.0,
                  "startTimestampMs": 500,
                  "endTimestampMs": 2600,
                  "durationMs": 2100,
                  "minAngleDeg": 58.0
                },
                {
                  "repNumber": 2,
                  "isValid": false,
                  "status": "INCOMPLETE",
                  "formScore": 60.0,
                  "startTimestampMs": 3000,
                  "endTimestampMs": 5100,
                  "durationMs": 2100,
                  "minAngleDeg": 112.0
                }
              ],
              "movementMetrics": {
                "range_of_motion": {
                  "name": "range_of_motion",
                  "value": 58.0,
                  "unit": "deg",
                  "score": 88.0,
                  "status": "VALID"
                },
                "body_swing": {
                  "name": "body_swing",
                  "value": null,
                  "unit": "ratio",
                  "score": null,
                  "status": "UNAVAILABLE"
                },
                "chin_to_bar": {
                  "name": "chin_to_bar",
                  "value": null,
                  "unit": "%",
                  "score": null,
                  "status": "UNAVAILABLE"
                }
              },
              "issues": [
                {
                  "code": "KIPPING_BODY_SWING",
                  "title": "Kipping / Body Swing",
                  "message": "Excessive horizontal sway detected.",
                  "severity": "HIGH",
                  "affectedReps": [1, 2],
                  "recommendation": "Engage hollow-body tension before pulling."
                },
                {
                  "code": "INCOMPLETE_PULL",
                  "title": "Incomplete Pull",
                  "message": "Chin did not clear wrist height.",
                  "severity": "LOW",
                  "affectedReps": [2]
                }
              ],
              "recommendations": ["Frame head-to-ankles to track body swing and chin clearance."],
              "cameraQuality": {
                "overallQuality": "GOOD",
                "viewAngle": "FRONT",
                "fullBodyVisibleRatio": 0.92,
                "averageConfidence": 0.94,
                "warnings": []
              },
              "quota": {
                "userId": 9,
                "membershipTier": "FREE",
                "dailyLimit": 1,
                "usedToday": 1,
                "remainingToday": 0,
                "liveAnalysisUnlocked": false,
                "timezone": "Asia/Ho_Chi_Minh"
              }
            }
        """.trimIndent()

        val domain = gson.fromJson(pullUpJson, VideoAnalysisResponseDto::class.java).toDomain()

        assertEquals("PULL_UP", domain.exerciseCode)
        assertEquals(2, domain.repCount)
        assertTrue(domain.movementMetrics["body_swing"]!!.isUnavailable)
        assertNull(domain.movementMetrics["body_swing"]!!.value)
        assertNull(domain.movementMetrics["body_swing"]!!.score)
        assertTrue(domain.movementMetrics["chin_to_bar"]!!.isUnavailable)
        assertNull(domain.movementMetrics["chin_to_bar"]!!.value)
        assertNull(domain.movementMetrics["chin_to_bar"]!!.score)
        assertEquals(IssueSeverity.HIGH, domain.issues[0].severity)
        assertEquals(IssueSeverity.LOW, domain.issues[1].severity)
        assertEquals("GOOD", domain.cameraQuality?.overallQuality)
        assertEquals("FRONT", domain.cameraQuality?.viewAngle)
        assertEquals(1, domain.recommendations.size)
        assertEquals("FREE", domain.quota?.membershipTier)
    }

    // =========================================================================
    // 2. ExerciseSelectionViewModel: Quota Display & FREE vs PRO Live Lock
    // =========================================================================

    @Test
    fun `ExerciseSelectionViewModel displays FREE quota with Live Analysis locked and unlocks after PRO upgrade`() =
        runTest(testDispatcher) {
            val getExercisesUseCase: GetExercisesUseCase = mockk()
            val analysisRepository: AnalysisRepository = mockk()

            val exercises = listOf(
                Exercise(1L, "Squat", "SQUAT", ExerciseCategory.LEGS, DifficultyLevel.BEGINNER, "", "", ""),
                Exercise(2L, "Push-up", "PUSH_UP", ExerciseCategory.PUSH, DifficultyLevel.BEGINNER, "", "", ""),
                Exercise(3L, "Pull-up", "PULL_UP", ExerciseCategory.PULL, DifficultyLevel.INTERMEDIATE, "", "", "")
            )
            val freeQuota = QuotaEntitlement(
                userId = 1L,
                membershipTier = "FREE",
                dailyLimit = 1,
                usedToday = 1,
                remainingToday = 0,
                liveAnalysisUnlocked = false,
                timezone = "Asia/Ho_Chi_Minh",
                quotaDate = "2026-11-16",
                resetsAt = "2026-11-17T00:00:00+07:00"
            )
            val proQuota = QuotaEntitlement(
                userId = 1L,
                membershipTier = "PRO",
                dailyLimit = 10,
                usedToday = 1,
                remainingToday = 9,
                liveAnalysisUnlocked = true,
                timezone = "Asia/Ho_Chi_Minh",
                quotaDate = "2026-11-16",
                resetsAt = "2026-11-17T00:00:00+07:00"
            )

            coEvery { getExercisesUseCase() } returns Result.success(exercises)
            coEvery { analysisRepository.getVideoAnalysisQuota() } returns Result.success(freeQuota)
            coEvery { analysisRepository.getVideoAnalysisHistory() } returns Result.success(emptyList())
            coEvery { analysisRepository.updateMembershipTier("PRO") } returns Result.success(proQuota)

            val viewModel = ExerciseSelectionViewModel(getExercisesUseCase, analysisRepository)
            advanceUntilIdle()

            val freeState = viewModel.uiState.value
            assertFalse(freeState.isLoading)
            assertEquals("FREE", freeState.quota?.membershipTier)
            assertEquals(1, freeState.quota?.dailyLimit)
            assertEquals(1, freeState.quota?.usedToday)
            assertEquals(0, freeState.quota?.remainingToday)
            assertTrue(freeState.quota!!.isExhausted)
            assertFalse(freeState.quota!!.liveAnalysisUnlocked)
            assertFalse(freeState.quota!!.isPro)

            // Tapping locked Live Analysis opens upgrade dialog with LIVE_ANALYSIS_LOCKED reason
            viewModel.openUpgradeDialog(UpgradeDialogReason.LIVE_ANALYSIS_LOCKED)
            assertTrue(viewModel.uiState.value.showUpgradeDialog)
            assertEquals(UpgradeDialogReason.LIVE_ANALYSIS_LOCKED, viewModel.uiState.value.upgradeDialogReason)

            // Upgrading to PRO unlocks Live Analysis & increases quota to 10/day
            viewModel.updateMembershipTier("PRO")
            advanceUntilIdle()

            val proState = viewModel.uiState.value
            assertFalse(proState.showUpgradeDialog)
            assertTrue(proState.quota!!.isPro)
            assertTrue(proState.quota!!.liveAnalysisUnlocked)
            assertEquals(10, proState.quota?.dailyLimit)
            assertEquals(9, proState.quota?.remainingToday)
        }

    // =========================================================================
    // 3. VideoSelectionViewModel: exerciseType + Stable IdempotencyKey + 429/403
    // =========================================================================

    @Test
    fun `VideoSelectionViewModel reuses stable idempotencyKey across retries and passes exerciseType`() =
        runTest(testDispatcher) {
            val analyzeVideoUseCase: AnalyzeVideoUseCase = mockk()
            val analyzeImageUseCase: AnalyzeImageUseCase = mockk()
            val analysisRepository: AnalysisRepository = mockk()
            val context: Context = mockk(relaxed = true)
            val contentResolver: ContentResolver = mockk(relaxed = true)
            val uri: Uri = mockk(relaxed = true)
            val tempDir: File = Files.createTempDirectory("qa_video_sel_").toFile()

            try {
                every { context.cacheDir } returns tempDir
                every { context.contentResolver } returns contentResolver
                every { uri.scheme } returns "file"
                every { uri.path } returns "/sdcard/Movies/squat_clip.mp4"
                every { contentResolver.openInputStream(uri) } answers {
                    ByteArrayInputStream("synthetic-video-payload".toByteArray())
                }

                coEvery { analysisRepository.getVideoAnalysisQuota() } returns Result.success(
                    QuotaEntitlement(membershipTier = "FREE", dailyLimit = 1, usedToday = 0, remainingToday = 1)
                )

                val capturedKeys = mutableListOf<String?>()
                val capturedTypes = mutableListOf<String?>()

                val completedResult = AnalysisResult(
                    id = 555L,
                    userId = 1L,
                    exerciseId = 12L,
                    exerciseName = "Squat",
                    mediaUrl = "/api/v1/video-analysis/555/annotated",
                    mediaType = "VIDEO",
                    overallScore = 88.0,
                    holdDurationSeconds = null,
                    formFeedbackList = listOf("Great depth"),
                    elbowAngle = null,
                    bodyLineAngle = null,
                    shoulderLeanAngle = null,
                    rawKeypointsJson = null,
                    createdAt = "2026-11-16T10:00:00Z",
                    videoMetadata = null,
                    componentScores = null,
                    stabilityMetrics = null,
                    timeline = emptyList(),
                    status = "COMPLETED",
                    exerciseCode = "SQUAT"
                )

                var attempt = 0
                coEvery {
                    analyzeVideoUseCase(
                        videoFile = any(),
                        exerciseId = any(),
                        exerciseType = captureNullable(capturedTypes),
                        idempotencyKey = captureNullable(capturedKeys),
                        sideView = any()
                    )
                } answers {
                    attempt++
                    if (attempt == 1) {
                        Result.failure(IOException("Temporary network drop"))
                    } else {
                        Result.success(completedResult)
                    }
                }

                val viewModel = VideoSelectionViewModel(
                    savedStateHandle = SavedStateHandle(
                        mapOf(
                            "exerciseId" to "12",
                            "exerciseCode" to "SQUAT",
                            "progressionName" to "Squat"
                        )
                    ),
                    analyzeVideoUseCase = analyzeVideoUseCase,
                    analyzeImageUseCase = analyzeImageUseCase,
                    analysisRepository = analysisRepository
                )
                advanceUntilIdle()

                viewModel.onVideoSelected(context, uri)
                val initialKey = viewModel.uiState.value.idempotencyKey
                assertNotNull(initialKey)
                assertTrue(initialKey!!.isNotBlank())

                // 1st upload attempt fails transiently
                var navigatedId: Long? = null
                viewModel.analyzeVideo(context) { navigatedId = it }
                advanceUntilIdle()

                assertNull(navigatedId)
                assertNotNull(viewModel.uiState.value.error)

                // Retry same video without re-selecting -> MUST reuse exact same idempotencyKey
                viewModel.analyzeVideo(context) { navigatedId = it }
                advanceUntilIdle()

                assertEquals(555L, navigatedId)
                assertEquals(2, capturedKeys.size)
                assertEquals(initialKey, capturedKeys[0])
                assertEquals(initialKey, capturedKeys[1])
                assertEquals(listOf("SQUAT", "SQUAT"), capturedTypes)

                // Selecting a new video allocates a fresh idempotencyKey
                viewModel.onVideoSelected(context, uri)
                assertNotEquals(initialKey, viewModel.uiState.value.idempotencyKey)
            } finally {
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `VideoSelectionViewModel handles HTTP 429 quota exceeded and HTTP 403 forbidden responses`() =
        runTest(testDispatcher) {
            val analyzeVideoUseCase: AnalyzeVideoUseCase = mockk()
            val analyzeImageUseCase: AnalyzeImageUseCase = mockk()
            val analysisRepository: AnalysisRepository = mockk()
            val context: Context = mockk(relaxed = true)
            val contentResolver: ContentResolver = mockk(relaxed = true)
            val uri: Uri = mockk(relaxed = true)
            val tempDir: File = Files.createTempDirectory("qa_video_err_").toFile()

            try {
                every { context.cacheDir } returns tempDir
                every { context.contentResolver } returns contentResolver
                every { uri.scheme } returns "file"
                every { uri.path } returns "/sdcard/Movies/pushup.mp4"
                every { contentResolver.openInputStream(uri) } answers {
                    ByteArrayInputStream("video-bytes".toByteArray())
                }

                coEvery { analysisRepository.getVideoAnalysisQuota() } returns Result.success(
                    QuotaEntitlement(
                        membershipTier = "FREE",
                        dailyLimit = 1,
                        usedToday = 1,
                        remainingToday = 0,
                        timezone = "Asia/Ho_Chi_Minh",
                        resetsAt = "2026-11-17T00:00:00+07:00"
                    )
                )

                val viewModel = VideoSelectionViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("exerciseCode" to "PUSH_UP")),
                    analyzeVideoUseCase = analyzeVideoUseCase,
                    analyzeImageUseCase = analyzeImageUseCase,
                    analysisRepository = analysisRepository
                )
                advanceUntilIdle()
                viewModel.onVideoSelected(context, uri)

                // HTTP 429 QUOTA_EXCEEDED
                val err429Body = """
                    {
                      "status": 429,
                      "code": "QUOTA_EXCEEDED",
                      "message": "Daily video analysis quota reached (1/1).",
                      "membershipTier": "FREE",
                      "dailyLimit": 1,
                      "usedToday": 1,
                      "remainingToday": 0,
                      "timezone": "Asia/Ho_Chi_Minh",
                      "resetsAt": "2026-11-17T00:00:00+07:00"
                    }
                """.trimIndent().toResponseBody("application/json".toMediaTypeOrNull())
                val http429 = NetworkErrorParser.toPreservedException(
                    HttpException(Response.error<Any>(429, err429Body))
                )
                coEvery { analyzeVideoUseCase(any(), any(), any(), any(), any()) } returns Result.failure(http429)

                viewModel.analyzeVideo(context) {}
                advanceUntilIdle()

                val state429 = viewModel.uiState.value
                assertTrue(state429.isQuotaExhaustedError)
                assertFalse(state429.isForbiddenError)
                assertEquals(0, state429.quota?.remainingToday)
                assertTrue(state429.quotaResetHint?.contains("2026-11-17T00:00:00+07:00") == true)
                assertTrue(state429.error?.contains("quota", ignoreCase = true) == true)

                // HTTP 403 PRO_MEMBERSHIP_REQUIRED
                val err403Body = """
                    {
                      "status": 403,
                      "code": "PRO_MEMBERSHIP_REQUIRED",
                      "message": "PRO membership is required for this feature."
                    }
                """.trimIndent().toResponseBody("application/json".toMediaTypeOrNull())
                val http403 = NetworkErrorParser.toPreservedException(
                    HttpException(Response.error<Any>(403, err403Body))
                )
                coEvery { analyzeVideoUseCase(any(), any(), any(), any(), any()) } returns Result.failure(http403)

                viewModel.analyzeVideo(context) {}
                advanceUntilIdle()

                val state403 = viewModel.uiState.value
                assertTrue(state403.isForbiddenError)
                assertFalse(state403.isQuotaExhaustedError)
                assertEquals("PRO membership is required for this feature.", state403.error)

                coVerify(exactly = 2) { analyzeVideoUseCase(any(), any(), eq("PUSH_UP"), any(), any()) }
            } finally {
                tempDir.deleteRecursively()
            }
        }

    // =========================================================================
    // 4. AnalysisResultViewModel: Rep Breakdown Loading & Seek-to-Timestamp
    // =========================================================================

    @Test
    fun `AnalysisResultViewModel loads rep breakdown and seeks to selected rep timestamp`() =
        runTest(testDispatcher) {
            val getAnalysisByIdUseCase: GetAnalysisByIdUseCase = mockk()
            val tokenManager: TokenManager = mockk(relaxed = true)
            every { tokenManager.getAccessToken() } returns "qa-jwt"

            val reps = listOf(
                RepBreakdown(
                    repNumber = 1,
                    valid = true,
                    status = "VALID",
                    formScore = 92.0,
                    startTimestampMs = 400L,
                    endTimestampMs = 1900L,
                    durationMs = 1500L,
                    minAngleDeg = 85.0,
                    romDeg = 90.0,
                    depthClassification = "GOOD"
                ),
                RepBreakdown(
                    repNumber = 2,
                    valid = false,
                    status = "INCOMPLETE",
                    formScore = 62.0,
                    startTimestampMs = 2400L,
                    endTimestampMs = 4100L,
                    durationMs = 1700L,
                    minAngleDeg = 122.0,
                    romDeg = 53.0,
                    depthClassification = "SHALLOW",
                    issues = listOf("SHALLOW_DEPTH")
                )
            )
            val keypoints = VideoKeypointsDataModel(
                analysisId = 99L,
                totalFrames = 3,
                frames = listOf(
                    KeypointFrameModel(0, 400L, listOf(KeypointItemModel("nose", 0.5f, 0.2f, 0.95f))),
                    KeypointFrameModel(1, 2400L, listOf(KeypointItemModel("nose", 0.5f, 0.25f, 0.96f))),
                    KeypointFrameModel(2, 4000L, listOf(KeypointItemModel("nose", 0.5f, 0.22f, 0.94f)))
                )
            )
            val fakeResult = AnalysisResult(
                id = 99L,
                userId = 1L,
                exerciseId = 12L,
                exerciseName = "Bodyweight Squat",
                exerciseCode = "SQUAT",
                schemaVersion = "1.0",
                mediaUrl = "/api/v1/video-analysis/99/annotated",
                mediaType = "VIDEO",
                overallScore = 79.0,
                holdDurationSeconds = null,
                formFeedbackList = listOf("Sink deeper on rep 2"),
                elbowAngle = null,
                bodyLineAngle = null,
                shoulderLeanAngle = null,
                rawKeypointsJson = null,
                createdAt = "2026-11-16T10:00:00Z",
                videoMetadata = null,
                componentScores = null,
                stabilityMetrics = null,
                timeline = emptyList(),
                status = "COMPLETED",
                repCount = 2,
                validReps = 1,
                incompleteReps = 1,
                reps = reps,
                movementMetrics = mapOf(
                    "knee_alignment" to MovementMetric(
                        key = "knee_alignment",
                        name = "Knee Alignment",
                        value = null,
                        score = null,
                        status = "UNAVAILABLE"
                    )
                ),
                issues = listOf(
                    StructuredIssue(
                        code = "SHALLOW_DEPTH",
                        title = "Shallow Depth",
                        message = "Rep 2 was shallow.",
                        severity = IssueSeverity.MEDIUM,
                        affectedReps = listOf(2)
                    )
                ),
                recommendations = listOf("Reach parallel thighs."),
                cameraQuality = CameraQuality(overallQuality = "GOOD", viewAngle = "SIDE"),
                keypointsData = keypoints
            )

            coEvery { getAnalysisByIdUseCase(99L) } returns Result.success(fakeResult)

            val viewModel = AnalysisResultViewModel(
                savedStateHandle = SavedStateHandle(mapOf("analysisId" to "99")),
                getAnalysisByIdUseCase = getAnalysisByIdUseCase,
                tokenManager = tokenManager
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, state.result?.repCount)
            assertEquals(1, state.result?.validReps)
            assertEquals(1, state.result?.incompleteReps)
            assertEquals(2, state.result?.reps?.size)
            assertTrue(state.result?.movementMetrics?.get("knee_alignment")?.isUnavailable == true)

            // User taps Rep 2 breakdown row -> seeks to 2400ms
            val rep2StartMs = state.result!!.reps[1].startTimestampMs
            viewModel.requestSeek(rep2StartMs)
            assertEquals(2400L, viewModel.seekRequestMs.value)

            viewModel.updatePlaybackPosition(2400L)
            assertEquals(2, viewModel.uiState.value.activeRepNumber)
            assertEquals(1, viewModel.uiState.value.currentKeypointFrame?.frameIndex)

            viewModel.clearSeekRequest()
            assertNull(viewModel.seekRequestMs.value)
        }

    @Test
    fun qa_adminUnlimitedQuota_modelAndDto_properties() {
        val dto = com.calisthenics.ai.data.remote.dto.QuotaEntitlementDto(
            userId = 77L,
            membershipTier = "ADMIN",
            dailyLimit = null,
            usedToday = 15,
            remainingToday = null,
            liveAnalysisUnlocked = true,
            timezone = "Asia/Ho_Chi_Minh",
            quotaDate = "2026-11-16",
            resetsAt = "2026-11-17T00:00:00+07:00",
            unlimited = true
        )

        val domain = dto.toDomain()
        assertTrue(domain.isAdmin)
        assertTrue(domain.isPro)
        assertTrue(domain.isUnlimited)
        assertFalse(domain.isExhausted)
        assertNull(domain.dailyLimit)
        assertNull(domain.remainingToday)
        assertEquals(15, domain.usedToday)
        assertTrue(domain.liveAnalysisUnlocked)
        assertEquals("ADMIN", domain.membershipTier)
    }

    @Test
    fun qa_userProfile_adminRole_mapsToAdminWithAllProEntitlements() {
        val dto = com.calisthenics.ai.data.remote.dto.UserProfileResponseDto(
            id = 1L,
            email = "admin@sky.app",
            fullName = "Super Admin",
            experienceLevel = "ADVANCED",
            role = "ROLE_ADMIN",
            gender = "MALE",
            onboardingStatus = "COMPLETED",
            membershipTier = "ADMIN",
            dailyQuotaLimit = null,
            dailyQuotaUsed = 12,
            dailyQuotaRemaining = null,
            liveAnalysisUnlocked = true
        )

        val profile = dto.toDomain()
        assertTrue(profile.isAdmin)
        assertTrue(profile.isPro)
        assertEquals("ADMIN", profile.membershipTier)
        assertNull(profile.dailyQuotaLimit)
        assertNull(profile.dailyQuotaRemaining)
        assertEquals(12, profile.dailyQuotaUsed)
        assertTrue(profile.liveAnalysisUnlocked)
    }

    @Test
    fun qa_exerciseSelectionViewModel_loadsAdminQuota_unlockedState() = runTest {
        val mockRepo = mockk<AnalysisRepository>()
        val mockGetExercises = mockk<GetExercisesUseCase>()

        val adminQuota = QuotaEntitlement(
            userId = 42L,
            membershipTier = "ADMIN",
            dailyLimit = null,
            usedToday = 12,
            remainingToday = null,
            liveAnalysisUnlocked = true,
            timezone = "Asia/Ho_Chi_Minh",
            quotaDate = "2026-11-16",
            isUnlimited = true
        )

        coEvery { mockRepo.getVideoAnalysisQuota() } returns Result.success(adminQuota)
        coEvery { mockRepo.getVideoAnalysisHistory() } returns Result.success(emptyList())
        coEvery { mockGetExercises() } returns Result.success(emptyList())

        val viewModel = ExerciseSelectionViewModel(
            getExercisesUseCase = mockGetExercises,
            analysisRepository = mockRepo
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.quota)
        assertTrue(state.quota!!.isAdmin)
        assertTrue(state.quota!!.isPro)
        assertTrue(state.quota!!.isUnlimited)
        assertFalse(state.quota!!.isExhausted)
        assertEquals(12, state.quota!!.usedToday)
        assertTrue(state.quota!!.liveAnalysisUnlocked)
    }

    @Test
    fun qa_threeTiers_quotaEntitlementComparison() {
        // FREE Tier: 1 upload/day, live locked
        val free = QuotaEntitlement(membershipTier = "FREE", dailyLimit = 1, usedToday = 1, remainingToday = 0, liveAnalysisUnlocked = false)
        assertFalse(free.isAdmin)
        assertFalse(free.isPro)
        assertTrue(free.isExhausted)
        assertFalse(free.liveAnalysisUnlocked)

        // PRO Tier: 10 uploads/day, live unlocked
        val pro = QuotaEntitlement(membershipTier = "PRO", dailyLimit = 10, usedToday = 5, remainingToday = 5, liveAnalysisUnlocked = true)
        assertFalse(pro.isAdmin)
        assertTrue(pro.isPro)
        assertFalse(pro.isExhausted)
        assertTrue(pro.liveAnalysisUnlocked)

        // ADMIN Tier: Unlimited uploads, live unlocked, never exhausted
        val admin = QuotaEntitlement(membershipTier = "ADMIN", dailyLimit = null, usedToday = 25, remainingToday = null, liveAnalysisUnlocked = true, isUnlimited = true)
        assertTrue(admin.isAdmin)
        assertTrue(admin.isPro)
        assertFalse(admin.isExhausted)
        assertTrue(admin.liveAnalysisUnlocked)
    }
}
