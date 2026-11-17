package com.calisthenics.ai.ui.media

import com.calisthenics.ai.BuildConfig

object MediaUrlUtils {
    /**
     * Resolves and normalizes any media URL:
     * - Prepends BuildConfig.API_BASE_URL if relative.
     * - Enforces HTTPS if base URL is HTTPS or if the host is `sky-enterprise.id.vn`,
     *   preventing Android NetworkSecurityConfig CleartextTraffic errors.
     * - Appends token if provided and not yet present.
     */
    fun resolveMediaUrl(rawUrl: String?, token: String? = null): String? {
        if (rawUrl.isNullOrBlank()) return null
        val baseUrl = try {
            BuildConfig.API_BASE_URL.trimEnd('/')
        } catch (_: Throwable) {
            "https://api.sky-enterprise.id.vn"
        }
        var absoluteUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "$baseUrl/${rawUrl.trimStart('/')}"
        }

        if ((baseUrl.startsWith("https://") || absoluteUrl.contains("sky-enterprise.id.vn")) &&
            absoluteUrl.startsWith("http://")
        ) {
            absoluteUrl = "https://" + absoluteUrl.substring("http://".length)
        }

        return if (!token.isNullOrBlank() && !absoluteUrl.contains("token=")) {
            if (absoluteUrl.contains("?")) "$absoluteUrl&token=$token" else "$absoluteUrl?token=$token"
        } else {
            absoluteUrl
        }
    }
}
