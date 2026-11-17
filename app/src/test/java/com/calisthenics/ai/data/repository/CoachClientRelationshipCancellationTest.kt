package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.CoachClientRelationshipDto
import com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachClientRelationshipCancellationTest {

    @Test
    fun assignmentStatus_distinguishesCancelledFromInactiveAndRejectsUnknownAsEnded() {
        assertEquals(WorkoutPlanAssignmentStatus.ACTIVE, WorkoutPlanAssignmentStatus.fromApi("ACTIVE"))
        assertEquals(WorkoutPlanAssignmentStatus.INACTIVE, WorkoutPlanAssignmentStatus.fromApi("INACTIVE"))
        assertEquals(WorkoutPlanAssignmentStatus.CANCELLED, WorkoutPlanAssignmentStatus.fromApi("CANCELLED"))
        assertEquals(WorkoutPlanAssignmentStatus.CANCELLED, WorkoutPlanAssignmentStatus.fromApi("CANCELED"))
        assertEquals(WorkoutPlanAssignmentStatus.ENDED, WorkoutPlanAssignmentStatus.fromApi("SOMETHING_NEW"))
        assertEquals(WorkoutPlanAssignmentStatus.ACTIVE, WorkoutPlanAssignmentStatus.fromApi(null))

        assertTrue(WorkoutPlanAssignmentStatus.CANCELLED.isCancelledOrEnded)
        assertTrue(WorkoutPlanAssignmentStatus.ENDED.isCancelledOrEnded)
        assertTrue(WorkoutPlanAssignmentStatus.REVOKED.isCancelledOrEnded)
        assertFalse(WorkoutPlanAssignmentStatus.INACTIVE.isCancelledOrEnded)
        assertFalse(WorkoutPlanAssignmentStatus.ACTIVE.isCancelledOrEnded)
    }

    @Test
    fun terminateRelationship_invalidatesCoachAssignedPlansCache() = runTest {
        val api: CalisthenicsApi = mockk()
        val workoutPlanRepository: WorkoutPlanRepository = mockk(relaxed = true)
        val repo = CoachClientRepositoryImpl(api, workoutPlanRepository)

        coEvery { api.terminateCoachConnection(42L) } returns CoachClientRelationshipDto(
            id = 42L,
            coachId = 1L,
            coachName = "Coach",
            clientId = 2L,
            clientName = "Client",
            requestedBy = 1L,
            status = "TERMINATED"
        )

        val result = repo.terminateRelationship(42L)

        assertTrue(result.isSuccess)
        assertEquals("TERMINATED", result.getOrThrow().status)
        coVerify(exactly = 1) { workoutPlanRepository.invalidateCoachAssignedPlans() }
    }
}
