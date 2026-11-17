package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AdminUserReportDto
import com.calisthenics.ai.data.remote.dto.CreateUserReportRequestDto
import com.calisthenics.ai.data.remote.dto.PreviousUserReportDto
import com.calisthenics.ai.data.remote.dto.ReportUserRefDto
import com.calisthenics.ai.data.remote.dto.UserReportActionDto
import com.calisthenics.ai.data.remote.dto.UserReportCreatedDto
import com.calisthenics.ai.data.remote.dto.UserReportSummaryDto
import com.calisthenics.ai.domain.model.AdminUserReport
import com.calisthenics.ai.domain.model.ModerationAction
import com.calisthenics.ai.domain.model.PreviousUserReport
import com.calisthenics.ai.domain.model.ReportReason
import com.calisthenics.ai.domain.model.ReportUserRef
import com.calisthenics.ai.domain.model.UserReportAction
import com.calisthenics.ai.domain.model.UserReportActionType
import com.calisthenics.ai.domain.model.UserReportDraft
import com.calisthenics.ai.domain.model.UserReportReceipt
import com.calisthenics.ai.domain.model.UserReportStatus
import com.calisthenics.ai.domain.model.UserReportSummary

fun UserReportDraft.toRequestDto(): CreateUserReportRequestDto = CreateUserReportRequestDto(
    reason = requireNotNull(reason) { "Report reason is required" }.name,
    description = description.trim().takeIf { it.isNotEmpty() },
    evidenceUrls = evidenceUrls.takeIf { it.isNotEmpty() }
)

fun UserReportCreatedDto.toDomain() = UserReportReceipt(
    id = id,
    status = UserReportStatus.fromApi(status),
    createdAt = createdAt
)

fun UserReportSummaryDto.toDomain() = UserReportSummary(
    total = total ?: 0L,
    pending = pending ?: 0L,
    underReview = underReview ?: 0L,
    resolved = resolved ?: 0L,
    dismissed = dismissed ?: 0L
)

fun ReportUserRefDto?.toDomain() = ReportUserRef(
    id = this?.id,
    fullName = this?.fullName.orEmpty(),
    email = this?.email
)

fun PreviousUserReportDto.toDomain() = PreviousUserReport(
    id = id,
    reason = ReportReason.fromApi(reason),
    status = UserReportStatus.fromApi(status),
    createdAt = createdAt
)

fun UserReportActionDto.toDomain() = UserReportAction(
    id = id,
    type = UserReportActionType.fromApi(type),
    fromStatus = fromStatus?.let(UserReportStatus::fromApi),
    toStatus = toStatus?.let(UserReportStatus::fromApi),
    moderationAction = ModerationAction.fromApi(moderationAction),
    suspendedUntil = suspendedUntil,
    note = note,
    adminId = adminId,
    adminName = adminName,
    createdAt = createdAt
)

fun AdminUserReportDto.toDomain() = AdminUserReport(
    id = id,
    reason = ReportReason.fromApi(reason),
    status = UserReportStatus.fromApi(status),
    reporter = reporter.toDomain(),
    reportedUser = reportedUser.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    description = description,
    evidenceUrls = evidenceUrls.orEmpty(),
    handledByAdminId = handledByAdminId,
    reviewedAt = reviewedAt,
    resolvedAt = resolvedAt,
    previousReports = previousReports.orEmpty().map { it.toDomain() },
    actions = actions.orEmpty().map { it.toDomain() }
)
