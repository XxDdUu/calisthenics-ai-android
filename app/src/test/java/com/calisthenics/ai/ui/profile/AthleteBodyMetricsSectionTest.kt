package com.calisthenics.ai.ui.profile

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.dto.ClientProfileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AthleteBodyMetricsSectionTest {

    @Test
    fun `formatHeight formats whole numbers without decimals`() {
        assertEquals("177 cm", BodyMetricFormatter.formatHeight(177.0))
    }

    @Test
    fun `formatHeight formats fractional values with one decimal place`() {
        assertEquals("177.5 cm", BodyMetricFormatter.formatHeight(177.5))
    }

    @Test
    fun `formatHeight returns fallback string when height is null`() {
        assertEquals("Not provided", BodyMetricFormatter.formatHeight(null, "Not provided"))
    }

    @Test
    fun `formatWeight formats whole numbers without decimals`() {
        assertEquals("76 kg", BodyMetricFormatter.formatWeight(76.0))
    }

    @Test
    fun `formatWeight formats fractional values with one decimal place`() {
        assertEquals("76.8 kg", BodyMetricFormatter.formatWeight(76.8))
    }

    @Test
    fun `formatWeight returns fallback string when weight is null`() {
        assertEquals("Not provided", BodyMetricFormatter.formatWeight(null, "Not provided"))
    }

    @Test
    fun `isBothMissing returns true when both height and weight are null`() {
        assertTrue(BodyMetricFormatter.isBothMissing(null, null))
    }

    @Test
    fun `isBothMissing returns false when only height is provided`() {
        assertFalse(BodyMetricFormatter.isBothMissing(180.0, null))
    }

    @Test
    fun `isBothMissing returns false when only weight is provided`() {
        assertFalse(BodyMetricFormatter.isBothMissing(null, 75.0))
    }

    @Test
    fun `isBothMissing returns false when both are provided`() {
        assertFalse(BodyMetricFormatter.isBothMissing(180.0, 75.0))
    }

    @Test
    fun `ClientProfileDto maps height and weight to domain model correctly`() {
        val dto = ClientProfileDto(
            id = 10L,
            userId = 10L,
            fullName = "Alex Athlete",
            displayName = "Alex",
            heightCm = 175.5,
            weightKg = 72.0
        )

        val domain = dto.toDomain()
        assertEquals(175.5, domain.heightCm)
        assertEquals(72.0, domain.weightKg)
    }

    @Test
    fun `ClientProfileDto maps null height and weight to domain model correctly`() {
        val dto = ClientProfileDto(
            id = 11L,
            userId = 11L,
            fullName = "Sam Athlete",
            heightCm = null,
            weightKg = null
        )

        val domain = dto.toDomain()
        assertEquals(null, domain.heightCm)
        assertEquals(null, domain.weightKg)
    }
}
