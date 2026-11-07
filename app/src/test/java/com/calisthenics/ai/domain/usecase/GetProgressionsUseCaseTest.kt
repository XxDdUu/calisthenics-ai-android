package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProgressionsUseCaseTest {

    private val repository: ExerciseRepository = mockk()
    private val useCase = GetProgressionsUseCase(repository)

    @Test
    fun invoke_filtersExercisesByParentCode() = runTest {
        val exercises = listOf(
            Exercise(1, "Tuck Planche", "TUCK_PLANCHE", ExerciseCategory.SKILL, DifficultyLevel.INTERMEDIATE, "", "", ""),
            Exercise(2, "Advanced Tuck Planche", "ADV_TUCK_PLANCHE", ExerciseCategory.SKILL, DifficultyLevel.ADVANCED, "", "", ""),
            Exercise(3, "Straddle Planche", "STRADDLE_PLANCHE", ExerciseCategory.SKILL, DifficultyLevel.ADVANCED, "", "", ""),
            Exercise(4, "Full Planche", "FULL_PLANCHE", ExerciseCategory.SKILL, DifficultyLevel.ELITE, "", "", ""),
            Exercise(5, "Front Lever", "FRONT_LEVER", ExerciseCategory.SKILL, DifficultyLevel.ADVANCED, "", "", "")
        )

        coEvery { repository.getExercises() } returns Result.success(exercises)

        val result = useCase("PLANCHE")

        assertTrue(result.isSuccess)
        val filtered = result.getOrThrow()
        assertEquals(4, filtered.size)
        assertTrue(filtered.all { it.code.contains("PLANCHE") })
    }
}
