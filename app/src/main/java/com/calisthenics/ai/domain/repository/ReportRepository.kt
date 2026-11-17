package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminUserReport
import com.calisthenics.ai.domain.model.ModerationAction
import com.calisthenics.ai.domain.model.UserReportDraft
import com.calisthenics.ai.domain.model.UserReportReceipt
import com.calisthenics.ai.domain.model.UserReportStatus
import com.calisthenics.ai.domain.model.UserReportSummary
import java.io.File

interface ReportRepository {
    /** Uploads an image through the shared storage endpoint and returns its download URL. */
    suspend fun uploadEvidence(file: File): Result<String>
    suspend fun reportUser(userId: Long, draft: UserReportDraft): Result<UserReportReceipt>

    suspend fun getSummary(): Result<UserReportSummary>
    suspend fun getReports(
        status: UserReportStatus?,
        reportedUserId: Long?,
        page: Int,
        size: Int
    ): Result<AdminPage<AdminUserReport>>
    suspend fun getReport(id: Long): Result<AdminUserReport>
    suspend fun updateStatus(id: Long, status: UserReportStatus, note: String?): Result<AdminUserReport>
    suspend fun addNote(id: Long, note: String): Result<AdminUserReport>
    suspend fun addAction(
        id: Long,
        action: ModerationAction,
        suspendedUntil: String?,
        note: String?
    ): Result<AdminUserReport>
}
