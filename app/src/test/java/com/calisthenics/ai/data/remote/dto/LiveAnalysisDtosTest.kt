package com.calisthenics.ai.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveAnalysisDtosTest {

    @Test
    fun `live response maps normalized landmark coordinates and timing`() {
        val response = Gson().fromJson(
            """
                {
                  "landmarks": [
                    {
                      "name": "left_shoulder",
                      "x": 120.0,
                      "y": 80.0,
                      "x_norm": 0.25,
                      "y_norm": 0.4,
                      "confidence": 0.9,
                      "visible": true
                    }
                  ],
                  "timing": {
                    "preprocessing_ms": 3.2,
                    "inference_ms": 45.6,
                    "analysis_ms": 1.1,
                    "total_ms": 49.9
                  }
                }
            """.trimIndent(),
            LiveAnalysisResponseDto::class.java
        )

        assertEquals(0.25, response.landmarks.single().xNorm!!, 0.0)
        assertEquals(0.4, response.landmarks.single().yNorm!!, 0.0)
        assertEquals(3.2, response.timing!!.preprocessingMs!!, 0.0)
        assertEquals(45.6, response.timing.inferenceMs!!, 0.0)
        assertEquals(49.9, response.timing.totalMs!!, 0.0)
    }

    @Test
    fun `live response maps squat repetition telemetry fields`() {
        val response = Gson().fromJson(
            """
                {
                  "status": "VALID",
                  "exercise": "SQUAT",
                  "currentState": "BOTTOM",
                  "kneeAngle": 92.5,
                  "minKneeAngle": 88.0,
                  "depth": "GOOD",
                  "isTracking": true,
                  "repCount": 3
                }
            """.trimIndent(),
            LiveAnalysisResponseDto::class.java
        )

        assertEquals("BOTTOM", response.currentState)
        assertEquals(92.5, response.kneeAngle!!, 0.0)
        assertEquals(88.0, response.minKneeAngle!!, 0.0)
        assertEquals("GOOD", response.depth)
        assertEquals(true, response.isTracking)
        assertEquals(3, response.repCount)
    }

    @Test
    fun `live response maps push-up repetition telemetry fields`() {
        val response = Gson().fromJson(
            """
                {
                  "status": "VALID",
                  "exercise": "PUSH_UP",
                  "current_state": "BOTTOM",
                  "elbow_angle": 85.0,
                  "min_elbow_angle": 82.3,
                  "body_alignment": 12.4,
                  "depth": "FULL",
                  "is_tracking": true,
                  "rep_count": 5
                }
            """.trimIndent(),
            LiveAnalysisResponseDto::class.java
        )

        assertEquals("BOTTOM", response.currentState)
        assertEquals(85.0, response.elbowAngle!!, 0.0)
        assertEquals(82.3, response.minElbowAngle!!, 0.0)
        assertEquals(12.4, response.bodyAlignment!!, 0.0)
        assertEquals("FULL", response.depth)
        assertEquals(true, response.isTracking)
        assertEquals(5, response.repCount)
    }

    @Test
    fun `session response maps joint angles`() {
        val response = Gson().fromJson(
            """
                {
                  "id": "test-session-123",
                  "exerciseType": "PUSH_UP",
                  "totalReps": 10,
                  "averageJointAngle": 89.5,
                  "deepestJointAngle": 78.2,
                  "reps": [
                    {
                      "repNumber": 1,
                      "minimumJointAngle": 78.2,
                      "formScore": 88.0,
                      "depthRating": "FULL"
                    }
                  ]
                }
            """.trimIndent(),
            AnalysisSessionResponseDto::class.java
        )

        assertEquals("test-session-123", response.id)
        assertEquals("PUSH_UP", response.exerciseType)
        assertEquals(89.5, response.averageJointAngle!!, 0.0)
        assertEquals(78.2, response.deepestJointAngle!!, 0.0)
        assertEquals(1, response.reps!!.size)
        assertEquals(78.2, response.reps!![0].minimumJointAngle!!, 0.0)
    }
}
