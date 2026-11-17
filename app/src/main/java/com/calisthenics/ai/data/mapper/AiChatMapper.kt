package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AiConversationDetailDto
import com.calisthenics.ai.data.remote.dto.AiConversationSummaryDto
import com.calisthenics.ai.data.remote.dto.AiMessageDto
import com.calisthenics.ai.domain.model.AiActionCard
import com.calisthenics.ai.domain.model.AiConversationDetail
import com.calisthenics.ai.domain.model.AiConversationSummary
import com.calisthenics.ai.domain.model.AiMessage
import org.json.JSONObject

fun AiConversationSummaryDto.toDomain(): AiConversationSummary {
    return AiConversationSummary(
        id = id,
        title = title.ifBlank { "Conversation #$id" },
        lastMessagePreview = lastMessagePreview?.ifBlank { "No messages yet" } ?: "No messages yet",
        lastMessageRole = lastMessageRole,
        lastInteractionAt = lastInteractionAt,
        createdAt = createdAt,
        messageCount = messageCount
    )
}

fun AiConversationDetailDto.toDomain(): AiConversationDetail {
    return AiConversationDetail(
        id = id,
        title = title.ifBlank { "Conversation #$id" },
        messages = messages.map { it.toDomain() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun AiMessageDto.toDomain(): AiMessage {
    val parsedCard = parseActionCard(metadata)
    return AiMessage(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        intent = intent,
        metadata = metadata,
        actionCard = parsedCard,
        createdAt = createdAt
    )
}

private fun parseActionCard(metadata: String?): AiActionCard? {
    if (metadata.isNullOrBlank()) return null
    return try {
        val root = JSONObject(metadata)
        val type = root.optString("type")
        when (type) {
            "PR_RECORDED" -> {
                val pr = root.optJSONObject("pr") ?: return null
                AiActionCard.PrRecorded(
                    prId = pr.optLong("id"),
                    exerciseName = pr.optString("exerciseName", "Personal Record"),
                    recordType = pr.optString("recordType", "MAX_REPS"),
                    value = pr.optDouble("value", pr.optDouble("recordValue", 0.0)),
                    unit = pr.optString("unit", ""),
                    formattedValue = pr.optString("formattedValue").takeIf { it.isNotBlank() },
                    previousValue = if (pr.has("previousRecordValue") && !pr.isNull("previousRecordValue")) pr.optDouble("previousRecordValue") else null,
                    improvementPercentage = if (pr.has("improvementPercentage") && !pr.isNull("improvementPercentage")) pr.optDouble("improvementPercentage") else null
                )
            }
            "WORKOUT_PLAN_PREVIEW" -> {
                val plan = root.optJSONObject("plan") ?: return null
                AiActionCard.WorkoutPlan(
                    planId = if (plan.has("id") && !plan.isNull("id")) plan.optLong("id") else null,
                    planName = plan.optString("planName", "Calisthenics Plan"),
                    description = plan.optString("description").takeIf { it.isNotBlank() },
                    durationWeeks = plan.optInt("durationWeeks", 4),
                    splitType = plan.optString("splitType").takeIf { it.isNotBlank() }
                )
            }
            "PROGRESS_SUMMARY" -> {
                AiActionCard.ProgressSummary(
                    totalPrs = root.optInt("totalPrs", 0),
                    completedSessionsCount = root.optInt("completedSessionsCount", 0),
                    uniqueExercisesCount = root.optLong("uniqueExercisesWithPr", 0L)
                )
            }
            "PROFILE_UPDATED" -> {
                val success = root.optBoolean("success", true)
                val isAmbiguous = root.optBoolean("isAmbiguous", false)
                val updatedFieldsObj = root.optJSONObject("updatedFields")
                val updatedFieldsMap = mutableMapOf<String, String>()
                if (updatedFieldsObj != null) {
                    val keys = updatedFieldsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        updatedFieldsMap[key] = updatedFieldsObj.optString(key)
                    }
                }
                val currentProfile = root.optJSONObject("currentProfile")
                AiActionCard.ProfileUpdate(
                    success = success,
                    isAmbiguous = isAmbiguous,
                    updatedFields = updatedFieldsMap,
                    displayName = currentProfile?.optString("displayName")?.takeIf { it.isNotBlank() },
                    heightCm = if (currentProfile?.has("heightCm") == true && !currentProfile.isNull("heightCm")) currentProfile.optDouble("heightCm") else null,
                    weightKg = if (currentProfile?.has("weightKg") == true && !currentProfile.isNull("weightKg")) currentProfile.optDouble("weightKg") else null,
                    experienceLevel = currentProfile?.optString("experienceLevel")?.takeIf { it.isNotBlank() },
                    primaryGoals = currentProfile?.optString("primaryGoals")?.takeIf { it.isNotBlank() },
                    bio = currentProfile?.optString("bio")?.takeIf { it.isNotBlank() }
                )
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
