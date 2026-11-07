package com.calisthenics.ai.ui.chat

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ChatTimeUtils {

    fun formatConversationTime(timestampStr: String?): String {
        if (timestampStr.isNullOrBlank()) return ""
        val dateTime = parseIsoDateTime(timestampStr) ?: return ""
        val messageDate = dateTime.toLocalDate()
        val today = LocalDate.now()

        return when {
            messageDate.isEqual(today) -> {
                dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
            }
            messageDate.isEqual(today.minusDays(1)) -> {
                "Yesterday"
            }
            messageDate.isAfter(today.minusDays(7)) -> {
                dateTime.format(DateTimeFormatter.ofPattern("EEE"))
            }
            else -> {
                dateTime.format(DateTimeFormatter.ofPattern("MMM d"))
            }
        }
    }

    fun formatMessageTime(timestampStr: String?): String {
        if (timestampStr.isNullOrBlank()) return ""
        val dateTime = parseIsoDateTime(timestampStr) ?: return ""
        return dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    fun getMessageDateHeader(timestampStr: String?): String {
        if (timestampStr.isNullOrBlank()) return "Older"
        val dateTime = parseIsoDateTime(timestampStr) ?: return "Older"
        val messageDate = dateTime.toLocalDate()
        val today = LocalDate.now()

        return when {
            messageDate.isEqual(today) -> "Today"
            messageDate.isEqual(today.minusDays(1)) -> "Yesterday"
            messageDate.year == today.year -> dateTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
            else -> dateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        }
    }

    private fun parseIsoDateTime(timestampStr: String): LocalDateTime? {
        return try {
            ZonedDateTime.parse(timestampStr).toLocalDateTime()
        } catch (e: DateTimeParseException) {
            try {
                LocalDateTime.parse(timestampStr)
            } catch (e2: DateTimeParseException) {
                null
            }
        }
    }
}
