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
}
