package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.AddReportNoteRequestDto
import com.calisthenics.ai.data.remote.dto.AdminPageDto
import com.calisthenics.ai.data.remote.dto.AdminUserReportDto
import com.calisthenics.ai.data.remote.dto.CreateUserReportRequestDto
import com.calisthenics.ai.data.remote.dto.FileUploadResponseDto
import com.calisthenics.ai.data.remote.dto.ReportActionRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateReportStatusRequestDto
import com.calisthenics.ai.data.remote.dto.UserReportCreatedDto
import com.calisthenics.ai.data.remote.dto.UserReportSummaryDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class RemoteReportDataSource @Inject constructor(
    private val api: CalisthenicsApi
) {
    suspend fun uploadImage(file: File): FileUploadResponseDto {
        val body = file.asRequestBody("image/jpeg".toMediaType())
        return api.uploadFile(MultipartBody.Part.createFormData("file", file.name, body))
    }

    suspend fun reportUser(userId: Long, request: CreateUserReportRequestDto): UserReportCreatedDto =
        api.reportUser(userId, request)

    suspend fun getSummary(): UserReportSummaryDto = api.getAdminUserReportSummary()

    suspend fun getReports(
        status: String?,
        reportedUserId: Long?,
        page: Int,
        size: Int
    ): AdminPageDto<AdminUserReportDto> = api.getAdminUserReports(status, reportedUserId, page, size)

    suspend fun getReport(id: Long): AdminUserReportDto = api.getAdminUserReport(id)

    suspend fun updateStatus(id: Long, status: String, note: String?): AdminUserReportDto =
        api.updateAdminUserReportStatus(id, UpdateReportStatusRequestDto(status, note))

    suspend fun addNote(id: Long, note: String): AdminUserReportDto =
        api.addAdminUserReportNote(id, AddReportNoteRequestDto(note))

    suspend fun addAction(id: Long, action: String, suspendedUntil: String?, note: String?): AdminUserReportDto =
        api.addAdminUserReportAction(id, ReportActionRequestDto(action, suspendedUntil, note))
}
