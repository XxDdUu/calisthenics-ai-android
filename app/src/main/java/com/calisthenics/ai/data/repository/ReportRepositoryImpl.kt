package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.mapper.toRequestDto
import com.calisthenics.ai.data.remote.datasource.RemoteReportDataSource
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.AdminUserReport
import com.calisthenics.ai.domain.model.ModerationAction
import com.calisthenics.ai.domain.model.UserReportDraft
import com.calisthenics.ai.domain.model.UserReportReceipt
import com.calisthenics.ai.domain.model.UserReportStatus
import com.calisthenics.ai.domain.model.UserReportSummary
import com.calisthenics.ai.domain.repository.ReportRepository
import java.io.File
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val remote: RemoteReportDataSource
) : ReportRepository {

    override suspend fun uploadEvidence(file: File): Result<String> = runCatching {
        remote.uploadImage(file).fileDownloadUri
    }

    override suspend fun reportUser(userId: Long, draft: UserReportDraft): Result<UserReportReceipt> = runCatching {
        remote.reportUser(userId, draft.toRequestDto()).toDomain()
    }

    override suspend fun getSummary(): Result<UserReportSummary> = runCatching {
        remote.getSummary().toDomain()
    }

    override suspend fun getReports(
        status: UserReportStatus?,
        reportedUserId: Long?,
        page: Int,
        size: Int
    ): Result<AdminPage<AdminUserReport>> = runCatching {
        remote.getReports(status?.name, reportedUserId, page, size).toDomain { it.toDomain() }
    }

    override suspend fun getReport(id: Long): Result<AdminUserReport> = runCatching {
        remote.getReport(id).toDomain()
    }

    override suspend fun updateStatus(id: Long, status: UserReportStatus, note: String?): Result<AdminUserReport> =
        runCatching {
            remote.updateStatus(id, status.name, note?.trim()?.takeIf { it.isNotEmpty() }).toDomain()
        }

    override suspend fun addNote(id: Long, note: String): Result<AdminUserReport> = runCatching {
        remote.addNote(id, note.trim()).toDomain()
    }

    override suspend fun addAction(
        id: Long,
        action: ModerationAction,
        suspendedUntil: String?,
        note: String?
    ): Result<AdminUserReport> = runCatching {
        remote.addAction(id, action.name, suspendedUntil, note?.trim()?.takeIf { it.isNotEmpty() }).toDomain()
    }
}
