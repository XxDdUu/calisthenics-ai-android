package com.calisthenics.ai.ui.home

import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.model.ExerciseMediaType
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutMediaResolverTest {

    @Test
    fun `prefers media belonging to selected workout`() {
        val own = media("own.jpg")
        val resolved = resolveWorkoutMedia(day(own), media("category.jpg"), media("fallback.jpg"))

        assertEquals(own, resolved.media)
        assertEquals(media("category.jpg"), resolved.fallbackMedia)
        assertEquals(media("fallback.jpg"), resolved.globalFallback)
    }

    @Test
    fun `prefers video over image within the same workout`() {
        val image = media("own.jpg")
        val video = ExerciseMedia(ExerciseMediaType.VIDEO, "own.mp4", displayOrder = 1)
        val resolved = resolveWorkoutMedia(day(image, video), null, null)

        assertEquals(video, resolved.media)
    }

    @Test
    fun `falls back from category media to global media`() {
        val global = media("fallback.jpg")
        val resolved = resolveWorkoutMedia(day(), null, global)

        assertEquals(null, resolved.media)
        assertEquals(null, resolved.fallbackMedia)
        assertEquals(global, resolved.globalFallback)
        assertTrue(resolved.usesGlobalFallback)
    }

    private fun day(vararg media: ExerciseMedia) = UserWorkoutPlanDay(
        id = 1,
        dayOfWeek = WorkoutDayOfWeek.MONDAY,
        sessionType = WorkoutSessionType.PUSH,
        exercises = listOf(
            UserWorkoutPlanExercise(
                id = 1,
                exercise = Exercise(
                    id = 1,
                    name = "Push-up",
                    code = "PUSH_UP",
                    category = ExerciseCategory.PUSH,
                    difficulty = DifficultyLevel.BEGINNER,
                    description = "",
                    targetMuscles = "",
                    instructions = "",
                    mediaItems = media.toList()
                ),
                sets = 3
            )
        )
    )

    private fun media(url: String) = ExerciseMedia(ExerciseMediaType.IMAGE, url)
}
