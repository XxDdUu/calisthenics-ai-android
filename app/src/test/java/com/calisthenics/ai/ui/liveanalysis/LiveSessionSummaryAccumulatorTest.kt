package com.calisthenics.ai.ui.liveanalysis

import com.calisthenics.ai.domain.model.ErrorSeverity
import com.calisthenics.ai.domain.model.LiveAnalysisResult
import com.calisthenics.ai.domain.model.LiveFeedback
import com.calisthenics.ai.domain.model.LiveScoringMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSessionSummaryAccumulatorTest {

    @Test
    fun pushUpSummary_computesRealMetricsAndDeduplicatesErrorsWithinSingleRep() {
        val acc = LiveSessionSummaryAccumulator(exerciseCode = "PUSH_UP", sessionStartTimeMs = 1000L)

        // Frame 1 & Frame 2 inside Rep #1 both emit HIP_SAG continuously -> must count ONCE for Rep #1
        acc.recordFrame(
            analysis = LiveAnalysisResult(
                status = "VALID",
                exercise = "PUSH_UP",
                poseDetected = true,
                confidence = 0.94,
                score = 78.0,
                repCount = 0,
                scoringMetrics = listOf(
                    LiveScoringMetric("body_alignment", 162.0, 72.0, 0.35, 0.95, "VALID"),
                    LiveScoringMetric("elbow_movement", 8.0, 84.0, 0.30, 0.90, "VALID"),
                    LiveScoringMetric("range_of_motion", 96.0, 86.0, 0.35, 0.92, "VALID"),
                    LiveScoringMetric("movement_control", 0.0, 0.0, 0.0, 0.0, "INSUFFICIENT_DATA")
                ),
                feedback = listOf(LiveFeedback("HIP_SAG", "Keep your hips up.", "WARNING"))
            ),
            nowMs = 2000L,
            isHoldExercise = false
        )

        acc.recordFrame(
            analysis = LiveAnalysisResult(
                status = "VALID",
                exercise = "PUSH_UP",
                poseDetected = true,
                confidence = 0.92,
                score = 82.0,
                repCount = 1,
                minElbowAngle = 92.0,
                depth = "GOOD",
                scoringMetrics = listOf(
                    LiveScoringMetric("body_alignment", 170.0, 82.0, 0.35, 0.95, "VALID"),
                    LiveScoringMetric("elbow_movement", 6.0, 88.0, 0.30, 0.90, "VALID"),
                    LiveScoringMetric("range_of_motion", 92.0, 96.0, 0.35, 0.92, "VALID")
                ),
                feedback = listOf(LiveFeedback("HIP_SAG", "Keep your hips up.", "ERROR"))
            ),
            nowMs = 3500L,
            isHoldExercise = false
        )

        // Rep #2 cleanly executed without errors
        acc.recordFrame(
            analysis = LiveAnalysisResult(
                status = "VALID",
                exercise = "PUSH_UP",
                poseDetected = true,
                confidence = 0.96,
                score = 94.0,
                repCount = 2,
                minElbowAngle = 88.0,
                depth = "GOOD",
                scoringMetrics = listOf(
                    LiveScoringMetric("body_alignment", 178.0, 96.0, 0.35, 0.95, "VALID"),
                    LiveScoringMetric("elbow_movement", 3.0, 94.0, 0.30, 0.90, "VALID"),
                    LiveScoringMetric("range_of_motion", 88.0, 100.0, 0.35, 0.92, "VALID")
                )
            ),
            nowMs = 6000L,
            isHoldExercise = false
        )

        val summary = acc.buildSessionSnapshot("push-session", durationSeconds = 5.0, isHoldExercise = false)

        assertEquals(2, summary.totalReps)
        assertEquals(2, summary.correctReps)
        assertEquals(0, summary.incorrectReps)
        assertEquals(2, summary.reps.size)
        assertNotNull(summary.analysisConfidence)

        // Error HIP_SAG appeared in 2 frames of rep #1 -> counted ONCE total and escalated to HIGH!
        assertEquals(1, summary.detectedErrors.size)
        val hipSag = summary.detectedErrors.first()
        assertEquals("HIP_SAG", hipSag.code)
        assertEquals("Hip Sagging", hipSag.errorName)
        assertEquals(1, hipSag.occurrences)
        assertEquals(ErrorSeverity.HIGH, hipSag.severity)
        assertEquals(listOf(1), hipSag.repNumbers)

        // Push-up metrics only (no squat/planche leakage), tempo + rep consistency computed from >=2 reps
        val metricNames = summary.exerciseMetrics.map { it.name }
        assertEquals(
            listOf("body_alignment", "elbow_positioning", "range_of_motion", "tempo_consistency", "rep_consistency"),
            metricNames
        )
        assertTrue(summary.exerciseMetrics.all { it.status == "VALID" && it.score != null })
    }

    @Test
    fun squatSummary_usesSquatMetricsAndNeverAppliesPushUpMetrics() {
        val acc = LiveSessionSummaryAccumulator(exerciseCode = "SQUAT", sessionStartTimeMs = 1000L)

        acc.recordFrame(
            analysis = LiveAnalysisResult(
                status = "VALID",
                exercise = "SQUAT",
                poseDetected = true,
                confidence = 0.91,
                score = 89.0,
                repCount = 1,
                minKneeAngle = 86.0,
                depth = "GOOD",
                scoringMetrics = listOf(
                    LiveScoringMetric("squat_depth", 86.0, 96.0, 0.35, 0.95, "VALID"),
                    LiveScoringMetric("knee_alignment", 4.0, 92.0, 0.25, 0.90, "VALID"),
                    LiveScoringMetric("hip_positioning", 75.0, 88.0, 0.15, 0.88, "VALID"),
                    LiveScoringMetric("torso_position", 22.0, 94.0, 0.25, 0.92, "VALID"),
                    LiveScoringMetric("balance", 5.0, 90.0, 0.10, 0.80, "VALID")
                )
            ),
            nowMs = 3000L,
            isHoldExercise = false
        )

        val summary = acc.buildSessionSnapshot("squat-1", durationSeconds = 2.0, isHoldExercise = false)
        val metricNames = summary.exerciseMetrics.map { it.name }
        assertEquals(
            listOf("squat_depth", "knee_alignment", "hip_positioning", "torso_angle", "balance"),
            metricNames
        )
        assertFalse(metricNames.contains("elbow_positioning"))
        assertFalse(metricNames.contains("shoulder_lean"))
    }

    @Test
    fun plancheSummary_marksUnavailableScapularMetricInsteadOfFabricating() {
        val acc = LiveSessionSummaryAccumulator(exerciseCode = "PLANCHE", sessionStartTimeMs = 1000L)

        acc.recordFrame(
            analysis = LiveAnalysisResult(
                status = "VALID",
                exercise = "PLANCHE",
                poseDetected = true,
                confidence = 0.88,
                score = 92.0,
                isValidHold = true,
                holdDurationSec = 4.5,
                scoringMetrics = listOf(
                    LiveScoringMetric("shoulder_lean", 44.0, 100.0, 0.30, 0.92, "VALID"),
                    LiveScoringMetric("hip_elevation", 174.0, 96.0, 0.20, 0.90, "VALID"),
                    LiveScoringMetric("arm_lockout", 175.0, 100.0, 0.30, 0.95, "VALID"),
                    LiveScoringMetric("scapular_protraction", 0.0, 0.0, 0.0, 0.0, "INSUFFICIENT_DATA"),
                    LiveScoringMetric("body_line", 4.0, 95.0, 0.20, 0.90, "VALID")
                )
            ),
            nowMs = 5500L,
            isHoldExercise = true
        )

        val summary = acc.buildSessionSnapshot("planche-1", durationSeconds = 4.5, isHoldExercise = true)

        assertEquals(92.0, summary.overallScore)
        assertEquals(4.5, summary.peakHoldDurationSeconds)
        val scapular = summary.exerciseMetrics.first { it.name == "scapular_positioning" }
        assertEquals("UNAVAILABLE", scapular.status)
        assertNull(scapular.score)
        assertNotNull(scapular.unavailableReason)
    }

    @Test
    fun frontLeverSummary_computesHoldConsistencyAcrossValidHoldFrames() {
        val acc = LiveSessionSummaryAccumulator(exerciseCode = "FRONT_LEVER", sessionStartTimeMs = 1000L)

        listOf(84.0, 88.0, 86.0).forEachIndexed { idx, frameScore ->
            acc.recordFrame(
                analysis = LiveAnalysisResult(
                    status = "VALID",
                    exercise = "FRONT_LEVER",
                    poseDetected = true,
                    confidence = 0.90,
                    score = frameScore,
                    isValidHold = true,
                    holdDurationSec = (idx + 1) * 2.0,
                    scoringMetrics = listOf(
                        LiveScoringMetric("horizontal_tilt", 5.0, 85.0, 0.20, 0.90, "VALID"),
                        LiveScoringMetric("body_line", 175.0, 87.5, 0.25, 0.92, "VALID"),
                        LiveScoringMetric("shoulder_angle", 42.0, 95.5, 0.25, 0.91, "VALID"),
                        LiveScoringMetric("elbow_extension", 177.0, 100.0, 0.30, 0.94, "VALID"),
                        LiveScoringMetric("stability", 0.0, 0.0, 0.0, 0.0, "INSUFFICIENT_DATA")
                    )
                ),
                nowMs = 1000L + (idx + 1) * 1000L,
                isHoldExercise = true
            )
        }

        val summary = acc.buildSessionSnapshot("fl-1", durationSeconds = 6.0, isHoldExercise = true)

        assertEquals(86.0, summary.overallScore)
        assertEquals(6.0, summary.peakHoldDurationSeconds)
        val holdConsistency = summary.exerciseMetrics.first { it.name == "hold_consistency" }
        assertEquals("VALID", holdConsistency.status)
        assertTrue((holdConsistency.score ?: 0.0) > 85.0)
    }

    @Test
    fun zeroRepsAndMissingLandmarks_neverFabricatesScoresOrMetrics() {
        val acc = LiveSessionSummaryAccumulator(exerciseCode = "PUSH_UP", sessionStartTimeMs = 1000L)

        acc.recordFrame(
            analysis = LiveAnalysisResult(
                status = "NO_PERSON_DETECTED",
                exercise = "PUSH_UP",
                poseDetected = false,
                confidence = 0.0,
                score = null,
                repCount = 0,
                scoringMetrics = emptyList(),
                feedback = listOf(LiveFeedback("INCOMPLETE_POSE", "Step fully into view", "WARNING"))
            ),
            nowMs = 2000L,
            isHoldExercise = false
        )

        val summary = acc.buildSessionSnapshot("empty-1", durationSeconds = 3.0, isHoldExercise = false)

        assertEquals(0, summary.totalReps)
        assertNull(summary.overallScore)
        assertNull(summary.averageScore)
        assertNull(summary.analysisConfidence)
        assertNull(summary.formConsistencyPercent)
        assertNull(summary.movementQuality)
        assertTrue(summary.exerciseMetrics.all { it.status == "UNAVAILABLE" && it.score == null })
        assertTrue(summary.detectedErrors.isEmpty())
    }
}
