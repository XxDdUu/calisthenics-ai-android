package com.calisthenics.ai.domain.usecase

import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.repository.ExerciseRepository
import javax.inject.Inject

class GetProgressionsUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    suspend operator fun invoke(parentExerciseCode: String): Result<List<Exercise>> {
        return repository.getExercises().map { exercises ->
            when (parentExerciseCode.uppercase()) {
                "PLANCHE" -> exercises.filter { 
                    it.code.contains("PLANCHE", ignoreCase = true) 
                }
                "FRONT_LEVER" -> exercises.filter { 
                    it.code.contains("LEVER", ignoreCase = true) 
                }
                "HANDSTAND" -> exercises.filter { 
                    it.code.contains("HANDSTAND", ignoreCase = true) 
                }
                else -> exercises.filter { 
                    it.code.equals(parentExerciseCode, ignoreCase = true) ||
                    it.name.contains(parentExerciseCode, ignoreCase = true)
                }.ifEmpty { exercises }
            }
        }
    }
}
