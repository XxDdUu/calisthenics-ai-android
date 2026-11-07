package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.ExerciseMediaResponseDto
import com.calisthenics.ai.data.remote.dto.ExerciseResponseDto
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.model.ExerciseMediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseMapperTest {

    @Test
    fun `maps empty media to an empty list`() {
        assertEquals(emptyList<ExerciseMedia>(), exerciseDto().toDomain().mediaItems)
    }

    @Test
    fun `maps legacy single image and video urls`() {
        val exercise = exerciseDto(
            imageUrl = "https://cdn.example.com/image.jpg",
            videoUrl = "https://cdn.example.com/video.mp4"
        ).toDomain()

        assertEquals(2, exercise.mediaItems.size)
        assertEquals(ExerciseMediaType.IMAGE, exercise.mediaItems[0].type)
        assertEquals(ExerciseMediaType.VIDEO, exercise.mediaItems[1].type)
    }

    @Test
    fun `maps multiple media items in configured order`() {
        val exercise = exerciseDto(
            mediaItems = listOf(
                ExerciseMediaResponseDto("VIDEO", "https://cdn.example.com/second.mp4", displayOrder = 2),
                ExerciseMediaResponseDto("IMAGE", "https://cdn.example.com/first.jpg", displayOrder = 1),
                ExerciseMediaResponseDto("VIDEO", "https://cdn.example.com/first.mp4", displayOrder = 0)
            )
        ).toDomain()

        assertEquals(
            listOf(
                "https://cdn.example.com/first.mp4",
                "https://cdn.example.com/first.jpg",
                "https://cdn.example.com/second.mp4"
            ),
            exercise.mediaItems.map { it.url }
        )
    }

    @Test
    fun `does not duplicate legacy urls included in media collection`() {
        val exercise = exerciseDto(
            imageUrl = "https://cdn.example.com/image.jpg",
            mediaItems = listOf(
                ExerciseMediaResponseDto("IMAGE", "https://cdn.example.com/image.jpg")
            )
        ).toDomain()

        assertEquals(1, exercise.mediaItems.size)
    }

    @Test
    fun `upgrades legacy http media urls because cleartext traffic is disabled`() {
        val exercise = exerciseDto(
            videoUrl = "http://api.example.com/video.mp4",
            mediaItems = listOf(
                ExerciseMediaResponseDto(
                    "IMAGE",
                    "http://api.example.com/image.jpg",
                    thumbnailUrl = "http://api.example.com/thumb.jpg"
                )
            )
        ).toDomain()

        assertEquals(
            listOf(
                "https://api.example.com/image.jpg",
                "https://api.example.com/video.mp4"
            ),
            exercise.mediaItems.map { it.url }
        )
        assertEquals("https://api.example.com/thumb.jpg", exercise.mediaItems[0].thumbnailUrl)
    }

    private fun exerciseDto(
        imageUrl: String? = null,
        videoUrl: String? = null,
        mediaItems: List<ExerciseMediaResponseDto>? = null
    ) = ExerciseResponseDto(
        id = 1,
        name = "Push-up",
        code = "PUSH_UP",
        category = "PUSH",
        difficulty = "BEGINNER",
        description = null,
        targetMuscles = null,
        instructions = null,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        mediaItems = mediaItems
    )
}
