package com.calisthenics.ai.ui.common

import androidx.annotation.StringRes
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import retrofit2.HttpException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A user-facing error: either a localized string resource ([res]) for well-known HTTP cases
 * or a raw [message] (server / network parser text).
 */
data class FeatureError(@StringRes val res: Int? = null, val message: String? = null)

/**
 * Maps a failure to a [FeatureError], using the provided resources for specific HTTP codes and
 * falling back to [NetworkErrorParser] for everything else.
 */
fun Throwable.toFeatureError(
    @StringRes badRequest: Int? = null,
    @StringRes forbidden: Int? = null,
    @StringRes notFound: Int? = null,
    @StringRes conflict: Int? = null,
    @StringRes tooManyRequests: Int? = null
): FeatureError {
    val code = (this as? HttpException)?.code()
    val res = when (code) {
        400 -> badRequest
        403 -> forbidden
        404 -> notFound
        409 -> conflict
        429 -> tooManyRequests
        else -> null
    }
    return if (res != null) FeatureError(res = res) else FeatureError(message = NetworkErrorParser.toUserMessage(this))
}

/** Formats an ISO-8601 instant for display; returns the raw value if it cannot be parsed. */
fun String?.toDisplayDateTime(zone: ZoneId = ZoneId.systemDefault()): String {
    if (this.isNullOrBlank()) return ""
    return runCatching {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(zone)
            .format(Instant.parse(this))
    }.getOrElse { this }
}

fun String?.toDisplayDate(zone: ZoneId = ZoneId.systemDefault()): String {
    if (this.isNullOrBlank()) return ""
    return runCatching {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(zone).format(Instant.parse(this))
    }.getOrElse { this.take(10) }
}
