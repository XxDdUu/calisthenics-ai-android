package com.calisthenics.ai.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserUsageDtoTest {

    private val gson = Gson()

    @Test
    fun `deserialize and map full json to domain`() {
        val json = """
            {
              "membership": {
                "tier": "PRO",
                "status": "ACTIVE",
                "expiresAt": "2026-12-16T23:59:59+07:00",
                "source": "Subscription"
              },
              "videoAnalysis": {
                "used": 2,
                "limit": 10,
                "remaining": 8,
                "unlimited": false,
                "resetAt": "2026-11-17T00:00:00+07:00",
                "timezone": "Asia/Ho_Chi_Minh"
              },
              "liveAnalysis": {
                "available": true,
                "unlimited": true
              }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, UserUsageResponseDto::class.java)
        assertNotNull(dto)

        val domain = dto.toDomain()
        assertEquals("PRO", domain.membership.tier)
        assertEquals("ACTIVE", domain.membership.status)
        assertEquals("2026-12-16T23:59:59+07:00", domain.membership.expiresAt)
        assertEquals("Subscription", domain.membership.source)
        assertTrue(domain.membership.isPro)
        assertFalse(domain.membership.isAdmin)

        assertEquals(2, domain.videoAnalysis.used)
        assertEquals(10, domain.videoAnalysis.limit)
        assertEquals(8, domain.videoAnalysis.remaining)
        assertFalse(domain.videoAnalysis.unlimited)
        assertEquals("2026-11-17T00:00:00+07:00", domain.videoAnalysis.resetAt)
        assertEquals("Asia/Ho_Chi_Minh", domain.videoAnalysis.timezone)
        assertEquals(0.2f, domain.videoAnalysis.progress, 0.001f)

        assertTrue(domain.liveAnalysis.available)
        assertTrue(domain.liveAnalysis.unlimited)
    }

    @Test
    fun `deserialize snake_case and alternate field names`() {
        val json = """
            {
              "membership": {
                "tier": "ADMIN",
                "status": "ACTIVE",
                "expires_at": null,
                "source": "ADMIN Role"
              },
              "video_analysis": {
                "used": 42,
                "limit": null,
                "remaining": null,
                "is_unlimited": true,
                "resets_at": null,
                "timezone": "UTC"
              },
              "live_analysis": {
                "is_available": true,
                "is_unlimited": true
              }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, UserUsageResponseDto::class.java)
        val domain = dto.toDomain()

        assertEquals("ADMIN", domain.membership.tier)
        assertTrue(domain.membership.isAdmin)
        assertTrue(domain.membership.isPro)
        assertEquals("ADMIN Role", domain.membership.source)

        assertEquals(42, domain.videoAnalysis.used)
        assertNull(domain.videoAnalysis.limit)
        assertNull(domain.videoAnalysis.remaining)
        assertTrue(domain.videoAnalysis.unlimited)
        assertEquals("UTC", domain.videoAnalysis.timezone)
        assertEquals(0f, domain.videoAnalysis.progress, 0.001f)

        assertTrue(domain.liveAnalysis.available)
        assertTrue(domain.liveAnalysis.unlimited)
    }

    @Test
    fun `default values when fields are empty or null`() {
        val dto = UserUsageResponseDto()
        val domain = dto.toDomain()

        assertEquals("FREE", domain.membership.tier)
        assertEquals("ACTIVE", domain.membership.status)
        assertFalse(domain.membership.isPro)
        assertFalse(domain.membership.isAdmin)
        assertNull(domain.membership.source)

        assertEquals(0, domain.videoAnalysis.used)
        assertEquals(1, domain.videoAnalysis.limit)
        assertEquals(1, domain.videoAnalysis.remaining)
        assertFalse(domain.videoAnalysis.unlimited)
        assertEquals("Asia/Ho_Chi_Minh", domain.videoAnalysis.timezone)
        assertEquals(0f, domain.videoAnalysis.progress, 0.001f)

        assertFalse(domain.liveAnalysis.available)
        assertFalse(domain.liveAnalysis.unlimited)
    }

    @Test
    fun `video analysis progress calculation boundary tests`() {
        // 0%
        val zero = VideoAnalysisUsageDto(used = 0, limit = 5).toDomain()
        assertEquals(0f, zero.progress, 0.001f)

        // 50%
        val half = VideoAnalysisUsageDto(used = 5, limit = 10).toDomain()
        assertEquals(0.5f, half.progress, 0.001f)

        // 100%
        val full = VideoAnalysisUsageDto(used = 10, limit = 10).toDomain()
        assertEquals(1.0f, full.progress, 0.001f)

        // Over limit coerced to 1.0f
        val over = VideoAnalysisUsageDto(used = 15, limit = 10).toDomain()
        assertEquals(1.0f, over.progress, 0.001f)

        // Unlimited returns 0f
        val unlimited = VideoAnalysisUsageDto(used = 15, unlimited = true).toDomain()
        assertEquals(0f, unlimited.progress, 0.001f)

        // Null limit returns 0f
        val nullLimit = VideoAnalysisUsageDto(used = 5, limit = null).toDomain()
        assertEquals(0f, nullLimit.progress, 0.001f)
    }
}
