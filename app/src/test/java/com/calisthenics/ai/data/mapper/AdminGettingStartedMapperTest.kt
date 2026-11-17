package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AdminAiChatResponseDto
import com.calisthenics.ai.data.remote.dto.AdminAiMessageDto
import com.calisthenics.ai.data.remote.dto.AdminConfirmationDecisionResponseDto
import com.calisthenics.ai.data.remote.dto.AdminPendingConfirmationDto
import com.calisthenics.ai.data.remote.dto.AdminToolExecutionRecordDto
import com.calisthenics.ai.data.remote.dto.AdminUserDto
import com.calisthenics.ai.data.remote.dto.GettingStartedCampaignAnalyticsDto
import com.calisthenics.ai.data.remote.dto.GettingStartedCampaignSummaryDto
import com.calisthenics.ai.data.remote.dto.GettingStartedProgramPreviewDto
import com.calisthenics.ai.data.remote.dto.GettingStartedProgramResponseDto
import com.calisthenics.ai.domain.model.TargetAudience
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminGettingStartedMapperTest {

    @Test
    fun `maps getting started preview analytics campaigns and pending confirmation from chat response dto`() {
        val programDto = GettingStartedProgramResponseDto(
            id = 42L,
            title = "Getting Started – 3 Days",
            description = "Beginner full-body campaign",
            targetAudience = "LAST_14_DAYS",
            published = false,
            version = 2
        )
        val previewDto = GettingStartedProgramPreviewDto(
            program = programDto,
            valid = true,
            validationErrors = emptyList(),
            targetAudience = "LAST_14_DAYS",
            audienceLabel = "New Members – Last 14 Days",
            eligibleAthleteCount = 12L,
            totalAthleteCount = 50L,
            estimatedAudienceSize = 12L
        )
        val campaignDto = GettingStartedCampaignSummaryDto(
            programId = 41L,
            title = "Active Campaign",
            published = true,
            version = 1,
            targetAudience = "ALL_ATHLETES",
            audienceLabel = "All Athletes",
            eligibleAthleteCount = 50L,
            enrollments = 20L
        )
        val analyticsDto = GettingStartedCampaignAnalyticsDto(
            programId = 41L,
            programTitle = "Active Campaign",
            published = true,
            version = 1,
            targetAudience = "ALL_ATHLETES",
            audienceLabel = "All Athletes",
            eligibleAthleteCount = 50L,
            modalImpressions = 40L,
            modalDismissals = 10L,
            programDetailViews = 25L,
            enrollments = 20L,
            day1Completions = 16L,
            day2Completions = 12L,
            day3Completions = 8L,
            day1CompletionRate = 80.0,
            day2CompletionRate = 60.0,
            day3CompletionRate = 40.0,
            completedEnrollments = 8L,
            overallCompletionRate = 40.0
        )
        val pendingDto = AdminPendingConfirmationDto(
            confirmationToken = "tok-123",
            actionType = "PUBLISH_PROGRAM",
            status = "PENDING",
            programId = 42L,
            programTitle = "Getting Started – 3 Days",
            targetAudience = "LAST_14_DAYS",
            audienceLabel = "New Members – Last 14 Days",
            eligibleAthleteCount = 12L,
            summary = "Confirm publish to 12 athletes"
        )
        val chatDto = AdminAiChatResponseDto(
            conversationId = 99L,
            conversationTitle = "Campaign Launch",
            assistantMessage = AdminAiMessageDto(id = 2L, conversationId = 99L, role = "ASSISTANT", content = "Ready for confirmation"),
            intent = "CAMPAIGN_PUBLISH_CONFIRMATION",
            gettingStartedProgram = programDto,
            gettingStartedPreview = previewDto,
            activeCampaigns = listOf(campaignDto),
            campaignAnalytics = analyticsDto,
            pendingConfirmation = pendingDto
        )

        val mapped = chatDto.toDomain()

        assertEquals(42L, mapped.gettingStartedProgram?.id)
        assertEquals(TargetAudience.LAST_14_DAYS, mapped.gettingStartedProgram?.targetAudience)
        assertTrue(mapped.gettingStartedPreview?.valid == true)
        assertEquals(12L, mapped.gettingStartedPreview?.eligibleAthleteCount)
        assertEquals(1, mapped.activeCampaigns.size)
        assertEquals(40.0, mapped.campaignAnalytics?.overallCompletionRate ?: 0.0, 0.001)
        assertNotNull(mapped.pendingConfirmation)
        assertEquals("tok-123", mapped.pendingConfirmation?.confirmationToken)
    }

    @Test
    fun `maps confirmation decision response dto`() {
        val decisionDto = AdminConfirmationDecisionResponseDto(
            confirmationToken = "tok-123",
            actionType = "PUBLISH_PROGRAM",
            status = "CONFIRMED",
            conversationId = 99L,
            reply = "Published successfully.",
            execution = AdminToolExecutionRecordDto(toolName = "publishProgram", status = "EXECUTED"),
            gettingStartedProgram = GettingStartedProgramResponseDto(
                id = 42L,
                title = "Getting Started – 3 Days",
                published = true,
                targetAudience = "LAST_14_DAYS",
                version = 2
            ),
            activeCampaigns = listOf(
                GettingStartedCampaignSummaryDto(
                    programId = 42L,
                    title = "Getting Started – 3 Days",
                    published = true,
                    version = 2,
                    targetAudience = "LAST_14_DAYS"
                )
            )
        )

        val mapped = decisionDto.toDomain()

        assertEquals("CONFIRMED", mapped.status)
        assertEquals("EXECUTED", mapped.execution?.status)
        assertEquals("publishProgram", mapped.execution?.toolName)
        assertTrue(mapped.gettingStartedProgram?.published == true)
        assertEquals(1, mapped.activeCampaigns.size)
    }

    @Test
    fun `maps pro management pending confirmation and decision user fields with null program fields`() {
        val pendingProDto = AdminPendingConfirmationDto(
            confirmationToken = "pro-tok-77",
            actionType = "GRANT_PRO",
            status = "PENDING",
            programId = null,
            programTitle = null,
            targetUserId = 42L,
            targetUserName = "Alex Athlete",
            targetUserEmail = "alex@example.com",
            targetUserCurrentTier = "FREE",
            targetUserMembershipSource = "Default",
            targetUserPaidPro = false,
            targetUserAdminGrantedPro = false,
            reason = "Beta access",
            summary = "Confirm granting PRO"
        )
        val grantedUserDto = AdminUserDto(
            id = 42L,
            email = "alex@example.com",
            fullName = "Alex Athlete",
            role = "ROLE_USER",
            hasCoachProfile = false,
            hasClientProfile = true,
            createdAt = "2026-11-17T00:00:00Z",
            membershipTier = "PRO",
            membershipSource = "Admin Grant",
            unlimited = true,
            paidPro = false,
            adminGrantedPro = true,
            adminProStatus = "ACTIVE"
        )
        val chatDto = AdminAiChatResponseDto(
            conversationId = 101L,
            assistantMessage = AdminAiMessageDto(id = 5L, conversationId = 101L, role = "ASSISTANT", content = "Confirm PRO?"),
            intent = "CONFIRMATION_REQUIRED",
            pendingConfirmation = pendingProDto,
            updatedProUser = null
        )
        val decisionDto = AdminConfirmationDecisionResponseDto(
            confirmationToken = "pro-tok-77",
            actionType = "GRANT_PRO",
            status = "CONFIRMED",
            conversationId = 101L,
            reply = "PRO Access Granted",
            updatedUser = grantedUserDto
        )

        val mappedChat = chatDto.toDomain()
        val mappedDecision = decisionDto.toDomain()

        assertTrue(mappedChat.pendingConfirmation?.isProAction == true)
        assertEquals(42L, mappedChat.pendingConfirmation?.targetUserId)
        assertEquals("alex@example.com", mappedChat.pendingConfirmation?.targetUserEmail)
        assertEquals("Beta access", mappedChat.pendingConfirmation?.reason)
        assertEquals(true, mappedDecision.updatedUser?.adminGrantedPro)
        assertEquals(true, mappedDecision.updatedUser?.unlimited)
    }
}
