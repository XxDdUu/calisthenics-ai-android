package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.ProfileMediaDto
import com.calisthenics.ai.domain.model.ProfileMediaType
import com.calisthenics.ai.domain.model.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileMediaMapperTest {

    @Test
    fun toDomain_mapsCoachImageCorrectly() {
        val dto = ProfileMediaDto(
            id = 101L,
            userId = 42L,
            profileType = "COACH",
            mediaType = "IMAGE",
            url = "https://cdn.example.com/coach.jpg",
            thumbnailUrl = "https://cdn.example.com/thumb.jpg",
            displayOrder = 0,
            createdAt = "2026-11-10T08:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals(101L, domain.id)
        assertEquals(42L, domain.userId)
        assertEquals(ProfileType.COACH, domain.profileType)
        assertEquals(ProfileMediaType.IMAGE, domain.mediaType)
        assertEquals("https://cdn.example.com/coach.jpg", domain.url)
        assertEquals("https://cdn.example.com/thumb.jpg", domain.thumbnailUrl)
        assertEquals(0, domain.displayOrder)
        assertEquals("2026-11-10T08:00:00Z", domain.createdAt)
    }

    @Test
    fun toDomain_mapsClientToAthleteVideoCorrectly() {
        val dto = ProfileMediaDto(
            id = 102L,
            userId = 43L,
            profileType = "CLIENT",
            mediaType = "VIDEO",
            url = "https://cdn.example.com/planche.mp4",
            thumbnailUrl = null,
            displayOrder = 2
        )

        val domain = dto.toDomain()

        assertEquals(102L, domain.id)
        assertEquals(ProfileType.ATHLETE, domain.profileType)
        assertEquals(ProfileMediaType.VIDEO, domain.mediaType)
        assertEquals("https://cdn.example.com/planche.mp4", domain.url)
        assertNull(domain.thumbnailUrl)
        assertEquals(2, domain.displayOrder)
    }

    @Test
    fun toDomain_upgradesHttpToHttpsForSkyEnterprise() {
        val dto = ProfileMediaDto(
            id = 103L,
            userId = 44L,
            profileType = "COACH",
            mediaType = "VIDEO",
            url = "http://api.sky-enterprise.id.vn/api/v1/storage/exercise-media/video.mp4",
            thumbnailUrl = "http://api.sky-enterprise.id.vn/api/v1/storage/exercise-media/thumb.jpg",
            displayOrder = 1
        )

        val domain = dto.toDomain()

        assertEquals("https://api.sky-enterprise.id.vn/api/v1/storage/exercise-media/video.mp4", domain.url)
        assertEquals("https://api.sky-enterprise.id.vn/api/v1/storage/exercise-media/thumb.jpg", domain.thumbnailUrl)
    }
}
