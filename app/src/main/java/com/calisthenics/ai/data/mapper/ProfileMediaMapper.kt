package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.ProfileMediaDto
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileMediaType
import com.calisthenics.ai.domain.model.ProfileType
import com.calisthenics.ai.ui.media.MediaUrlUtils

fun ProfileMediaDto.toDomain(): ProfileMediaItem {
    val pType = try {
        ProfileType.valueOf(profileType.uppercase())
    } catch (_: Exception) {
        if (profileType.equals("CLIENT", ignoreCase = true)) ProfileType.ATHLETE else ProfileType.COACH
    }
    val mType = try {
        ProfileMediaType.valueOf(mediaType.uppercase())
    } catch (_: Exception) {
        ProfileMediaType.IMAGE
    }
    return ProfileMediaItem(
        id = id,
        userId = userId,
        profileType = pType,
        mediaType = mType,
        url = MediaUrlUtils.resolveMediaUrl(url) ?: url,
        thumbnailUrl = MediaUrlUtils.resolveMediaUrl(thumbnailUrl) ?: thumbnailUrl,
        displayOrder = displayOrder,
        createdAt = createdAt
    )
}
