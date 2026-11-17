package com.calisthenics.ai.ui.coach

import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachConnectionOverview
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import com.calisthenics.ai.domain.repository.ChatRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoachConnectionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCoachClientRepository
    private lateinit var viewModel: CoachConnectionsViewModel
    private val analyticsRepository: AnalyticsRepository = mockk {
        coEvery { getCoachSharedPRs(any()) } returns Result.success(emptyList())
        coEvery { getClientSharedPRs() } returns Result.success(emptyList())
        coEvery { getClientFeedbacks() } returns Result.success(emptyList())
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCoachClientRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsProfilesAndRelationshipsFromRepository() = runTest {
        createViewModel(Result.success(overview()))

        assertEquals(listOf(coach), viewModel.uiState.value.coaches)
        assertEquals(listOf(activeRelationship), viewModel.uiState.value.relationships)
        assertEquals(listOf(activeRelationship), viewModel.uiState.value.myCoaches)
        assertTrue(viewModel.uiState.value.myClients.isEmpty())
        assertEquals(10L, viewModel.uiState.value.currentUserId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun separatesAcceptedCoachAndClientListsForCurrentUser() = runTest {
        val clientRelationship = activeRelationship.copy(
            id = 13L,
            coachId = 10L,
            coachName = "Current user",
            clientId = 30L,
            clientName = "Connected client"
        )
        createViewModel(
            Result.success(
                overview(relationships = listOf(activeRelationship, clientRelationship))
            )
        )

        assertEquals(listOf(activeRelationship), viewModel.uiState.value.myCoaches)
        assertEquals(listOf(clientRelationship), viewModel.uiState.value.myClients)
    }

    @Test
    fun emptyOverviewIsAValidLoadedState() = runTest {
        createViewModel(Result.success(CoachConnectionOverview(10L, emptyList(), emptyList())))

        assertTrue(viewModel.uiState.value.hasLoaded)
        assertTrue(viewModel.uiState.value.coaches.isEmpty())
        assertTrue(viewModel.uiState.value.relationships.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun reportsLoadFailuresWithoutDisplayingBackendDetails() = runTest {
        createViewModel(Result.failure(IllegalStateException("internal server detail")))

        assertEquals("Something went wrong. Please try again.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.error.orEmpty().contains("internal"))
    }

    @Test
    fun sendsConnectionAndUpdatesOnlyAfterBackendSuccess() = runTest {
        createViewModel(Result.success(overview(coaches = listOf(coach), relationships = emptyList())))

        val pending = activeRelationship.copy(id = 12L, status = "PENDING", requestedBy = 10L)
        repository.requestResult = Result.success(pending)
        viewModel.requestConnection(coach.id)
        assertTrue(viewModel.uiState.value.relationships.isEmpty())
        advanceUntilIdle()

        assertEquals(listOf(pending), viewModel.uiState.value.relationships)
        assertEquals(1, repository.requestCalls)
        assertEquals("Connection request sent.", viewModel.uiState.value.feedback)
    }

    @Test
    fun preventsDuplicateConnectionRequestsWhileSubmissionIsQueued() = runTest {
        createViewModel(Result.success(overview(coaches = listOf(coach), relationships = emptyList())))

        viewModel.requestConnection(coach.id)
        viewModel.requestConnection(coach.id)
        advanceUntilIdle()

        assertEquals(1, repository.requestCalls)
    }

    @Test
    fun acceptsRejectsAndCancelsByReplacingConfirmedRelationship() = runTest {
        createViewModel(Result.success(overview(relationships = listOf(pendingRelationship))))

        repository.acceptResult = Result.success(pendingRelationship.copy(status = "ACTIVE"))
        viewModel.acceptRequest(pendingRelationship.id)
        advanceUntilIdle()
        assertEquals("ACTIVE", viewModel.uiState.value.relationships.single().status)

        repository.rejectResult = Result.success(pendingRelationship.copy(status = "REJECTED"))
        viewModel.rejectRequest(pendingRelationship.id)
        advanceUntilIdle()
        assertEquals("REJECTED", viewModel.uiState.value.relationships.single().status)

        repository.cancelResult = Result.success(pendingRelationship.copy(status = "CANCELLED"))
        viewModel.cancelRequest(pendingRelationship.id)
        advanceUntilIdle()
        assertEquals("CANCELLED", viewModel.uiState.value.relationships.single().status)
    }

    @Test
    fun incomingRequestCountIncludesUniquePendingRequestsFromOtherUsersOnly() = runTest {
        val incoming = pendingRelationship.copy(id = 14L, requestedBy = 20L)
        val resolved = incoming.copy(id = 16L, status = "REJECTED")
        val relationships = listOf(
            incoming,
            incoming.copy(),
            pendingRelationship,
            resolved,
            activeRelationship
        )
        createViewModel(Result.success(overview(relationships = relationships)))

        assertEquals(1, viewModel.uiState.value.incomingRequestCount)
        assertEquals(listOf(incoming), viewModel.uiState.value.incomingRequests)

        repository.acceptResult = Result.success(incoming.copy(status = "ACTIVE"))
        viewModel.acceptRequest(incoming.id)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.incomingRequestCount)
    }

    @Test
    fun refreshUpdatesIncomingRequestCountFromTheLatestOverview() = runTest {
        createViewModel(Result.success(overview(relationships = emptyList())))
        assertEquals(0, viewModel.uiState.value.incomingRequestCount)

        val incoming = pendingRelationship.copy(id = 14L, requestedBy = 20L)
        repository.overview = Result.success(overview(relationships = listOf(incoming)))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.incomingRequestCount)
    }

    @Test
    fun refreshAllRefreshesEntireOverview() = runTest {
        createViewModel(Result.success(overview(coaches = emptyList())))
        assertTrue(viewModel.uiState.value.coaches.isEmpty())

        val newCoach = coach.copy(id = 99L, fullName = "New Coach")
        repository.overview = Result.success(overview(coaches = listOf(newCoach)))
        viewModel.refreshAll()
        advanceUntilIdle()

        assertEquals(listOf(newCoach), viewModel.uiState.value.coaches)
    }

    @Test
    fun refreshDiscoverUpdatesCoachesWithoutRefetchingOverview() = runTest {
        createViewModel(Result.success(overview()))
        assertEquals(listOf(coach), viewModel.uiState.value.coaches)

        val updatedCoach = coach.copy(fullName = "Updated Coach")
        repository.coachesResult = Result.success(listOf(updatedCoach))

        viewModel.refreshDiscover()
        advanceUntilIdle()

        assertEquals(1, repository.loadCoachesCalls)
        assertEquals(listOf(updatedCoach), viewModel.uiState.value.coaches)
        assertFalse(viewModel.uiState.value.isSectionRefreshing(CoachSection.DISCOVER))
    }

    @Test
    fun refreshMyCoachesAndOtherSectionsUpdateRelationships() = runTest {
        createViewModel(Result.success(overview()))
        assertEquals(1, viewModel.uiState.value.relationships.size)

        val newClientRel = activeRelationship.copy(
            id = 25L,
            coachId = 10L,
            clientId = 55L,
            status = "ACTIVE"
        )
        repository.relationshipsResult = Result.success(listOf(activeRelationship, newClientRel))

        viewModel.refreshMyClients()
        advanceUntilIdle()

        assertEquals(1, repository.loadRelationshipsCalls)
        assertEquals(listOf(newClientRel), viewModel.uiState.value.myClients)
        assertFalse(viewModel.uiState.value.isSectionRefreshing(CoachSection.MY_CLIENTS))

        viewModel.refreshSection(CoachSection.SENT)
        advanceUntilIdle()
        assertEquals(2, repository.loadRelationshipsCalls)
    }

    @Test
    fun sectionRefreshFallsBackToOverviewRefreshWhenNotYetLoaded() = runTest {
        repository.overview = Result.success(overview())
        viewModel = CoachConnectionsViewModel(
            repository,
            mockk<ChatRepository>(relaxed = true),
            analyticsRepository
        )

        viewModel.refreshDiscover()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasLoaded)
        assertEquals(listOf(coach), viewModel.uiState.value.coaches)
    }

    @Test
    fun preservesCurrentRelationshipWhenActionFails() = runTest {
        createViewModel(Result.success(overview(relationships = listOf(pendingRelationship))))
        repository.acceptResult = Result.failure(IllegalStateException("conflict details"))

        viewModel.acceptRequest(pendingRelationship.id)
        advanceUntilIdle()

        assertEquals("PENDING", viewModel.uiState.value.relationships.single().status)
        assertEquals("Something went wrong. Please try again.", viewModel.uiState.value.feedback)
    }

    @Test
    fun clientTerminatesActiveCoachRelationshipRemovesFromMyCoachesAndUpdatesState() = runTest {
        createViewModel(Result.success(overview(relationships = listOf(activeRelationship))))
        assertEquals(1, viewModel.uiState.value.myCoaches.size)

        repository.terminateResult = Result.success(activeRelationship.copy(status = "TERMINATED"))
        viewModel.terminateRelationship(activeRelationship.id)
        advanceUntilIdle()

        assertEquals(1, repository.terminateCalls)
        assertTrue(viewModel.uiState.value.myCoaches.isEmpty())
        assertEquals("TERMINATED", viewModel.uiState.value.relationships.single().status)
        assertEquals("Relationship ended.", viewModel.uiState.value.feedback)
        assertTrue(viewModel.uiState.value.busyRelationshipIds.isEmpty())
    }

    @Test
    fun coachTerminatesActiveClientRelationshipRemovesFromMyClientsAndUpdatesState() = runTest {
        val clientRelationship = activeRelationship.copy(
            id = 15L,
            coachId = 10L,
            coachName = "Current user (Coach)",
            clientId = 50L,
            clientName = "Client 50"
        )
        createViewModel(Result.success(overview(relationships = listOf(clientRelationship))))
        assertEquals(1, viewModel.uiState.value.myClients.size)

        repository.terminateResult = Result.success(clientRelationship.copy(status = "TERMINATED"))
        viewModel.terminateRelationship(clientRelationship.id)
        advanceUntilIdle()

        assertEquals(1, repository.terminateCalls)
        assertTrue(viewModel.uiState.value.myClients.isEmpty())
        assertEquals("TERMINATED", viewModel.uiState.value.relationships.single().status)
        assertEquals("Relationship ended.", viewModel.uiState.value.feedback)
    }

    @Test
    fun duplicateTerminationCallsIgnoredWhenBusy() = runTest {
        createViewModel(Result.success(overview(relationships = listOf(activeRelationship))))

        viewModel.terminateRelationship(activeRelationship.id)
        viewModel.terminateRelationship(activeRelationship.id)
        advanceUntilIdle()

        assertEquals(1, repository.terminateCalls)
    }

    @Test
    fun handlesTerminationErrorGracefully() = runTest {
        createViewModel(Result.success(overview(relationships = listOf(activeRelationship))))
        repository.terminateResult = Result.failure(IllegalStateException("Server error"))

        viewModel.terminateRelationship(activeRelationship.id)
        advanceUntilIdle()

        assertEquals(1, repository.terminateCalls)
        assertEquals(1, viewModel.uiState.value.myCoaches.size)
        assertEquals("Something went wrong. Please try again.", viewModel.uiState.value.feedback)
        assertTrue(viewModel.uiState.value.busyRelationshipIds.isEmpty())
    }

    @Test
    fun terminatingRelationshipRemovesItsAssignmentsAndFiltersCancelledFromOptions() = runTest {
        val coachRel = activeRelationship.copy(
            id = 15L,
            coachId = 10L,
            coachName = "Current user (Coach)",
            clientId = 50L,
            clientName = "Client 50"
        )
        val otherRel = coachRel.copy(id = 16L, clientId = 51L, clientName = "Client 51")
        val plan = com.calisthenics.ai.domain.model.UserWorkoutPlan(
            id = 1L,
            name = "Plan",
            splitType = com.calisthenics.ai.domain.model.WorkoutSplitType.PPL,
            isActive = true
        )
        val assignmentForTerminatedRel = com.calisthenics.ai.domain.model.WorkoutPlanAssignment(
            id = 101L,
            coachName = "Current user (Coach)",
            startDate = "2026-10-01",
            createdAt = "2026-10-01T00:00:00Z",
            workoutPlan = plan,
            status = com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus.ACTIVE,
            relationshipId = 15L
        )
        val assignmentForOtherRel = assignmentForTerminatedRel.copy(
            id = 102L,
            relationshipId = 16L
        )
        val alreadyCancelledAssignment = assignmentForTerminatedRel.copy(
            id = 103L,
            relationshipId = 16L,
            status = com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus.CANCELLED
        )
        val workoutPlanRepo: com.calisthenics.ai.domain.repository.WorkoutPlanRepository = mockk(relaxed = true)
        coEvery { workoutPlanRepo.getPlans() } returns Result.success(listOf(plan))
        coEvery { workoutPlanRepo.getMyCoachAssignments() } returnsMany listOf(
            Result.success(listOf(assignmentForTerminatedRel, assignmentForOtherRel, alreadyCancelledAssignment)),
            Result.success(listOf(assignmentForOtherRel))
        )

        repository.overview = Result.success(overview(relationships = listOf(coachRel, otherRel)))
        viewModel = CoachConnectionsViewModel(
            repository = repository,
            chatRepository = mockk<ChatRepository>(relaxed = true),
            analyticsRepository = analyticsRepository,
            workoutPlanRepository = workoutPlanRepo
        )
        advanceUntilIdle()

        viewModel.loadAssignmentOptions()
        advanceUntilIdle()
        assertEquals(listOf(101L, 102L), viewModel.uiState.value.coachAssignments.map { it.id })

        repository.terminateResult = Result.success(coachRel.copy(status = "TERMINATED"))
        viewModel.terminateRelationship(15L)
        advanceUntilIdle()

        assertEquals(listOf(102L), viewModel.uiState.value.coachAssignments.map { it.id })
        assertEquals(listOf(otherRel), viewModel.uiState.value.myClients)
    }

    @Test
    fun prFeedbackLoadsCoachSharedPrsAndFiltersOutUnrelatedCoachRecords() = runTest {
        val myCoachShare = com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto(
            id = 501L,
            personalRecord = com.calisthenics.ai.data.remote.dto.PersonalRecordDto(
                id = 901L,
                exerciseName = "Weighted Pull-up",
                recordType = "MAX_WEIGHT",
                recordValue = 40.0,
                unit = "kg",
                achievedAt = "2026-11-10T10:00:00Z"
            ),
            clientUserId = 30L,
            clientName = "Client 30",
            coachUserId = 10L, // Current user is Coach 10
            coachName = "Current Coach",
            sharedAt = "2026-11-10T11:00:00Z",
            isRead = false
        )
        val otherCoachShare = myCoachShare.copy(
            id = 502L,
            coachUserId = 999L, // Shared with a different coach
            coachName = "Other Coach"
        )

        coEvery { analyticsRepository.getCoachSharedPRs(any()) } returns Result.success(
            listOf(myCoachShare, otherCoachShare)
        )
        coEvery { analyticsRepository.getClientSharedPRs() } returns Result.success(emptyList())

        createViewModel(Result.success(overview()))

        // Only the PR shared with currentUserId (10L) is displayed
        assertEquals(listOf(myCoachShare), viewModel.uiState.value.clientFeedbacks)
        assertEquals(1, viewModel.uiState.value.unreadFeedbackCount)

        // Marking as read updates state
        val readShare = myCoachShare.copy(isRead = true)
        coEvery { analyticsRepository.markPrShareAsRead(501L) } returns Result.success(readShare)
        viewModel.markPrShareAsRead(501L)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.clientFeedbacks.single().isRead)

        // Submitting feedback updates state
        val reviewedShare = readShare.copy(coachFeedback = "Solid pull!", rating = 5, status = "REVIEWED")
        coEvery { analyticsRepository.addCoachFeedback(501L, "Solid pull!", 5) } returns Result.success(reviewedShare)
        viewModel.submitCoachPrFeedback(501L, "Solid pull!", 5)
        advanceUntilIdle()
        assertEquals("Solid pull!", viewModel.uiState.value.clientFeedbacks.single().coachFeedback)
        assertEquals(5, viewModel.uiState.value.clientFeedbacks.single().rating)
    }

    private fun overview(
        coaches: List<CoachProfile> = listOf(coach),
        relationships: List<CoachClientRelationship> = listOf(activeRelationship)
    ) = CoachConnectionOverview(10L, coaches, relationships)

    private suspend fun TestScope.createViewModel(result: Result<CoachConnectionOverview>) {
        repository.overview = result
        viewModel = CoachConnectionsViewModel(
            repository,
            mockk<ChatRepository>(relaxed = true),
            analyticsRepository
        )
        advanceUntilIdle()
    }

    private class FakeCoachClientRepository : CoachClientRepository {
        var overview = Result.success(
            CoachConnectionOverview(10L, listOf(coach), listOf(activeRelationship))
        )
        var coachesResult = Result.success(listOf(coach))
        var relationshipsResult = Result.success(listOf(activeRelationship))
        var requestResult = Result.success(pendingRelationship)
        var acceptResult = Result.success(pendingRelationship.copy(status = "ACTIVE"))
        var rejectResult = Result.success(pendingRelationship.copy(status = "REJECTED"))
        var cancelResult = Result.success(pendingRelationship.copy(status = "CANCELLED"))
        var terminateResult = Result.success(activeRelationship.copy(status = "TERMINATED"))
        var requestCalls = 0
        var loadCoachesCalls = 0
        var loadRelationshipsCalls = 0
        var terminateCalls = 0

        override suspend fun loadOverview() = overview
        override suspend fun loadCoaches(): Result<List<CoachProfile>> {
            loadCoachesCalls++
            return coachesResult
        }
        override suspend fun loadRelationships(): Result<List<CoachClientRelationship>> {
            loadRelationshipsCalls++
            return relationshipsResult
        }
        override suspend fun getCoachProfile(id: Long) = Result.success(coach)
        override suspend fun requestConnection(coachId: Long): Result<CoachClientRelationship> {
            requestCalls++
            return requestResult
        }
        override suspend fun acceptRequest(relationshipId: Long) = acceptResult
        override suspend fun rejectRequest(relationshipId: Long) = rejectResult
        override suspend fun cancelRequest(relationshipId: Long) = cancelResult
        override suspend fun terminateRelationship(relationshipId: Long): Result<CoachClientRelationship> {
            terminateCalls++
            return terminateResult
        }
    }

    private companion object {
        val coach = CoachProfile(20L, "Coach", null, "ADVANCED")
        val activeRelationship = CoachClientRelationship(
            id = 11L,
            coachId = 20L,
            coachName = "Coach",
            coachAvatarUrl = null,
            coachExperienceLevel = "ADVANCED",
            clientId = 10L,
            clientName = "Client",
            clientAvatarUrl = null,
            clientExperienceLevel = "BEGINNER",
            requestedBy = 10L,
            status = "ACTIVE",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-02T00:00:00Z"
        )
        val pendingRelationship = activeRelationship.copy(id = 12L, status = "PENDING")
    }
}
