package com.calisthenics.ai.ui.report

import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.MAX_REPORT_EVIDENCE
import com.calisthenics.ai.domain.model.ReportReason
import com.calisthenics.ai.domain.model.UserReportReceipt
import com.calisthenics.ai.domain.model.UserReportStatus
import com.calisthenics.ai.domain.repository.ReportRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ReportUserViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: ReportRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun confirmAndSubmit_succeedsWithUploadedEvidence() = runTest {
        coEvery { repository.uploadEvidence(any()) } returns Result.success("/api/v1/storage/files/proof.jpg")
        coEvery { repository.reportUser(22L, any()) } returns Result.success(
            UserReportReceipt(100L, UserReportStatus.PENDING, "2026-11-13T06:00:00Z")
        )

        val vm = ReportUserViewModel(repository)
        vm.start(22L)
        vm.requestConfirm()
        assertFalse(vm.uiState.value.showConfirm)

        vm.setReason(ReportReason.SPAM_OR_SCAM)
        vm.setDescription("Unsolicited links")
        vm.addEvidence(File("/tmp/proof.jpg"))
        advanceUntilIdle()

        assertEquals(listOf("/api/v1/storage/files/proof.jpg"), vm.uiState.value.uploadedUrls)
        vm.requestConfirm()
        assertTrue(vm.uiState.value.showConfirm)

        vm.submit()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.submitted)
        coVerify(exactly = 1) {
            repository.reportUser(
                22L,
                match { it.reason == ReportReason.SPAM_OR_SCAM && it.evidenceUrls == listOf("/api/v1/storage/files/proof.jpg") }
            )
        }
    }

    @Test
    fun duplicateAndRateLimitErrors_mapToUserFacingStrings() = runTest {
        coEvery { repository.reportUser(22L, any()) } returnsMany listOf(
            Result.failure(httpError(409)),
            Result.failure(httpError(429))
        )

        val vm = ReportUserViewModel(repository)
        vm.start(22L)
        vm.setReason(ReportReason.HARASSMENT)
        vm.submit()
        advanceUntilIdle()
        assertEquals(R.string.report_error_duplicate, vm.uiState.value.error?.res)

        vm.submit()
        advanceUntilIdle()
        assertEquals(R.string.report_error_too_many, vm.uiState.value.error?.res)
    }

    @Test
    fun evidenceCap_preventsExceedingMaxCount() = runTest {
        coEvery { repository.uploadEvidence(any()) } returns Result.success("/api/v1/storage/files/x.jpg")
        val vm = ReportUserViewModel(repository)
        vm.start(33L)

        repeat(MAX_REPORT_EVIDENCE + 2) { idx ->
            vm.addEvidence(File("/tmp/e$idx.jpg"))
        }
        advanceUntilIdle()

        assertEquals(MAX_REPORT_EVIDENCE, vm.uiState.value.evidence.size)
        assertFalse(vm.uiState.value.canAddEvidence)
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody("application/json".toMediaType())))
}
