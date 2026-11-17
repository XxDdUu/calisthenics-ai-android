package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.remote.datasource.RemoteAnalysisDataSource
import com.calisthenics.ai.data.remote.dto.MembershipUsageDto
import com.calisthenics.ai.data.remote.dto.QuotaEntitlementDto
import com.calisthenics.ai.data.remote.dto.UserUsageResponseDto
import com.calisthenics.ai.data.remote.dto.VideoAnalysisResponseDto
import com.calisthenics.ai.data.remote.dto.VideoAnalysisUsageDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

class AnalysisRepositoryUsageTest {

    private val remoteDataSource: RemoteAnalysisDataSource = mockk(relaxed = true)
    private lateinit var repository: AnalysisRepositoryImpl

    @Before
    fun setup() {
        repository = AnalysisRepositoryImpl(remoteDataSource)
    }

    @Test
    fun `getUserUsage succeeds and caches data in memory`() = runTest {
        val dto = UserUsageResponseDto(
            membership = MembershipUsageDto(tier = "PRO", status = "ACTIVE"),
            videoAnalysis = VideoAnalysisUsageDto(used = 1, limit = 10, remaining = 9)
        )
        coEvery { remoteDataSource.getUserUsage() } returns dto

        assertNull(repository.getCachedUserUsage())

        val result = repository.getUserUsage()
        assertTrue(result.isSuccess)

        val usage = result.getOrNull()
        assertNotNull(usage)
        assertEquals("PRO", usage?.membership?.tier)
        assertEquals(1, usage?.videoAnalysis?.used)

        // Cached value is stored
        val cached = repository.getCachedUserUsage()
        assertNotNull(cached)
        assertEquals(usage, cached)
    }

    @Test
    fun `getUserUsage returns failure when remote call throws`() = runTest {
        coEvery { remoteDataSource.getUserUsage() } throws IOException("Network down")

        val result = repository.getUserUsage()
        assertTrue(result.isFailure)
        assertNull(repository.getCachedUserUsage())
    }

    @Test
    fun `invalidateUserUsageCache clears cache and emits update event`() = runTest {
        val dto = UserUsageResponseDto(
            membership = MembershipUsageDto(tier = "FREE", status = "ACTIVE")
        )
        coEvery { remoteDataSource.getUserUsage() } returns dto
        repository.getUserUsage()
        assertNotNull(repository.getCachedUserUsage())

        repository.invalidateUserUsageCache()
        assertNull(repository.getCachedUserUsage())
    }

    @Test
    fun `uploadVideoForAnalysis invalidates usage cache on success`() = runTest {
        val dto = UserUsageResponseDto(
            membership = MembershipUsageDto(tier = "FREE", status = "ACTIVE")
        )
        coEvery { remoteDataSource.getUserUsage() } returns dto
        repository.getUserUsage()
        assertNotNull(repository.getCachedUserUsage())

        val mockFile = mockk<File>(relaxed = true)
        coEvery {
            remoteDataSource.uploadVideoForAnalysis(
                videoFile = any(),
                exerciseId = any(),
                exerciseType = any(),
                idempotencyKey = any(),
                sideView = any()
            )
        } returns VideoAnalysisResponseDto(id = 999L, userId = 1L, status = "PENDING")

        val uploadResult = repository.uploadVideoForAnalysis(
            videoFile = mockFile,
            exerciseId = 1L
        )
        assertTrue(uploadResult.isSuccess)
        assertNull(repository.getCachedUserUsage())
    }

    @Test
    fun `updateMembershipTier invalidates usage cache on success`() = runTest {
        val dto = UserUsageResponseDto(
            membership = MembershipUsageDto(tier = "FREE", status = "ACTIVE")
        )
        coEvery { remoteDataSource.getUserUsage() } returns dto
        repository.getUserUsage()
        assertNotNull(repository.getCachedUserUsage())

        coEvery { remoteDataSource.updateMembershipTier("PRO") } returns QuotaEntitlementDto(membershipTier = "PRO")

        val updateResult = repository.updateMembershipTier("PRO")
        assertTrue(updateResult.isSuccess)
        assertNull(repository.getCachedUserUsage())
    }
}
