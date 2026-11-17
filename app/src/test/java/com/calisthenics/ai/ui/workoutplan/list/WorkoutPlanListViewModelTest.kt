package com.calisthenics.ai.ui.workoutplan.list

import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.domain.usecase.CreateWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.DeleteWorkoutPlanUseCase
import com.calisthenics.ai.domain.usecase.GetWorkoutPlansUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutPlanListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: WorkoutPlanRepository = mockk(relaxed = true)
    private val coachClientRepository: CoachClientRepository = mockk(relaxed = true)

    private lateinit var getWorkoutPlansUseCase: GetWorkoutPlansUseCase
    private lateinit var createWorkoutPlanUseCase: CreateWorkoutPlanUseCase
    private lateinit var deleteWorkoutPlanUseCase: DeleteWorkoutPlanUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.getPlans() } returns Result.success(emptyList())
        coEvery { repository.getMyAssignments() } returns Result.success(emptyList())
        coEvery { repository.getMyCoachAssignments() } returns Result.success(emptyList())
        coEvery { repository.getClientPersonalPlans(any()) } returns Result.success(emptyList())
        coEvery { repository.assignPlanToClient(any(), any()) } returns Result.success(Unit)
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(emptyList())
        getWorkoutPlansUseCase = GetWorkoutPlansUseCase(repository)
        createWorkoutPlanUseCase = CreateWorkoutPlanUseCase(repository)
        deleteWorkoutPlanUseCase = DeleteWorkoutPlanUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadPlans_success_populatesPlansInState() = runTest {
        val samplePlans = listOf(
            UserWorkoutPlan(id = 1L, name = "Hypertrophy PPL", splitType = WorkoutSplitType.PPL),
            UserWorkoutPlan(id = 2L, name = "Upper Lower Strength", splitType = WorkoutSplitType.UPPER_LOWER)
        )
        coEvery { repository.getPlans() } returns Result.success(samplePlans)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.plans.size)
        assertEquals("Hypertrophy PPL", state.plans[0].name)
    }

    @Test
    fun loadPlans_empty_showsEmptyState() = runTest {
        coEvery { repository.getPlans() } returns Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.plans.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun loadPlans_error_setsErrorMessage() = runTest {
        coEvery { repository.getPlans() } returns Result.failure(RuntimeException("Network error"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.plans.isEmpty())
        assertEquals("Network error", state.error)
    }

    @Test
    fun submitCreatePlan_withValidName_callsRepositoryAndReloads() = runTest {
        val newPlan = UserWorkoutPlan(id = 10L, name = "Full Body AI", splitType = WorkoutSplitType.FULL_BODY)
        coEvery { repository.getPlans() } returns Result.success(emptyList())
        coEvery { repository.createPlan(any(), any(), any()) } returns Result.success(newPlan)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openCreateDialog()
        viewModel.updateCreatePlanName("Full Body AI")
        viewModel.updateCreateSplitType(WorkoutSplitType.FULL_BODY)
        viewModel.submitCreatePlan()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCreateDialogOpen)
        coVerify(exactly = 1) { repository.createPlan("Full Body AI", WorkoutSplitType.FULL_BODY, any()) }
    }

    @Test
    fun submitCreatePlan_withBlankName_setsValidationErrorWithoutCallingRepo() = runTest {
        coEvery { repository.getPlans() } returns Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openCreateDialog()
        viewModel.updateCreatePlanName("   ")
        viewModel.submitCreatePlan()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCreateDialogOpen)
        assertEquals("Plan name cannot be empty", viewModel.uiState.value.createError)
        coVerify(exactly = 0) { repository.createPlan(any(), any(), any()) }
    }

    @Test
    fun executeDeletePlan_deletesAndReloads() = runTest {
        val plan = UserWorkoutPlan(id = 5L, name = "Old Plan", splitType = WorkoutSplitType.PPL)
        coEvery { repository.getPlans() } returns Result.success(listOf(plan))
        coEvery { repository.deletePlan(5L) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.confirmDeletePlan(plan)
        assertEquals(plan, viewModel.uiState.value.planToDelete)

        viewModel.executeDeletePlan()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.planToDelete)
        coVerify(exactly = 1) { repository.deletePlan(5L) }
    }

    @Test
    fun submitCreatePlan_whenOffline_blocksCreation() = runTest {
        val viewModel = createViewModel(isOnline = false)
        advanceUntilIdle()

        viewModel.openCreateDialog()
        assertEquals("Cannot create workout plans while offline.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isCreateDialogOpen)
    }

    @Test
    fun executeDeletePlan_whenOffline_blocksDeletion() = runTest {
        val plan = UserWorkoutPlan(id = 5L, name = "Offline Plan", splitType = WorkoutSplitType.PPL)
        val viewModel = createViewModel(isOnline = false)
        advanceUntilIdle()

        viewModel.confirmDeletePlan(plan)
        assertEquals("Cannot delete workout plans while offline.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.planToDelete)
    }

    @Test
    fun assignmentsExcludeCancelledWhileRetainingInactive_andCacheInvalidationRefreshes() = runTest {
        val invalidations = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        io.mockk.every { repository.planCacheInvalidations } returns invalidations
        val samplePlan = UserWorkoutPlan(id = 7L, name = "Coach Plan", splitType = WorkoutSplitType.PPL)
        val activeAssignment = com.calisthenics.ai.domain.model.WorkoutPlanAssignment(
            id = 1L,
            coachName = "Coach",
            startDate = "2026-10-01",
            createdAt = "2026-10-01T00:00:00Z",
            workoutPlan = samplePlan,
            status = com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus.ACTIVE,
            relationshipId = 11L
        )
        val inactiveAssignment = activeAssignment.copy(
            id = 2L,
            status = com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus.INACTIVE
        )
        val cancelledAssignment = activeAssignment.copy(
            id = 3L,
            status = com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus.CANCELLED
        )
        coEvery { repository.getMyAssignments() } returnsMany listOf(
            Result.success(listOf(activeAssignment, inactiveAssignment, cancelledAssignment)),
            Result.success(emptyList())
        )
        coEvery { repository.getMyCoachAssignments() } returns Result.success(
            listOf(activeAssignment, inactiveAssignment, cancelledAssignment)
        )
        coEvery {
            repository.updateAssignmentStatus(2L, com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus.ACTIVE)
        } returns Result.success(inactiveAssignment.copy(status = com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus.ACTIVE))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.assignments.map { it.id })
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.coachAssignments.map { it.id })

        // Requesting toggle on CANCELLED is ignored; INACTIVE still reactivates
        viewModel.requestAssignmentStatusChange(cancelledAssignment)
        assertNull(viewModel.uiState.value.assignmentToDeactivate)
        coVerify(exactly = 0) { repository.updateAssignmentStatus(3L, any()) }

        viewModel.requestAssignmentStatusChange(inactiveAssignment)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            repository.updateAssignmentStatus(2L, com.calisthenics.ai.domain.model.WorkoutPlanAssignmentStatus.ACTIVE)
        }

        // Invalidation triggers reload so terminated assignments disappear immediately
        invalidations.emit(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.assignments.isEmpty())
    }

    private fun createViewModel(
        isOnline: Boolean = true,
        userId: Long? = null,
        gettingStartedRepository: com.calisthenics.ai.domain.repository.GettingStartedRepository =
            com.calisthenics.ai.domain.repository.NoOpGettingStartedRepository,
        exerciseRepository: com.calisthenics.ai.domain.repository.ExerciseRepository =
            com.calisthenics.ai.domain.repository.NoOpExerciseRepository
    ): WorkoutPlanListViewModel {
        val connectivityMonitor: com.calisthenics.ai.data.network.ConnectivityMonitor = mockk {
            io.mockk.every { isConnected() } returns isOnline
            io.mockk.every { this@mockk.isOnline } returns kotlinx.coroutines.flow.flowOf(isOnline)
        }
        val tokenManager: TokenManager = mockk(relaxed = true) {
            io.mockk.every { getUserId() } returns userId
        }
        return WorkoutPlanListViewModel(
            getWorkoutPlansUseCase = getWorkoutPlansUseCase,
            createWorkoutPlanUseCase = createWorkoutPlanUseCase,
            deleteWorkoutPlanUseCase = deleteWorkoutPlanUseCase,
            connectivityMonitor = connectivityMonitor,
            coachClientRepository = coachClientRepository,
            tokenManager = tokenManager,
            gettingStartedRepository = gettingStartedRepository,
            exerciseRepository = exerciseRepository
        )
    }

    @Test
    fun assignPlanToClient_withConnectedClient_assignsSuccessfully() = runTest {
        val rel = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Athlete A", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(rel))
        coEvery { repository.assignPlanToClient(10L, 100L) } returns Result.success(Unit)

        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        viewModel.selectClient(20L)
        advanceUntilIdle()
        assertEquals(20L, viewModel.uiState.value.selectedClientId)

        var onSuccessCalled = false
        viewModel.assignPlanToClient(20L, 100L, onSuccess = { onSuccessCalled = true })
        advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertEquals("Plan assigned to Athlete A.", viewModel.uiState.value.assignmentMessage)
        coVerify(exactly = 1) { repository.assignPlanToClient(10L, 100L) }
    }

    @Test
    fun selectClientA_thenClientB_assignsWorkoutToClientB() = runTest {
        val relA = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Athlete A", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        val relB = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 11L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 21L, clientName = "Athlete B", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(relA, relB))
        coEvery { repository.assignPlanToClient(11L, 100L) } returns Result.success(Unit)

        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        // Select Client A
        viewModel.selectClient(20L)
        advanceUntilIdle()
        assertEquals(20L, viewModel.uiState.value.selectedClientId)

        // Select Client B
        viewModel.selectClient(21L)
        advanceUntilIdle()
        assertEquals(21L, viewModel.uiState.value.selectedClientId)

        // Assign to selected client
        var onSuccessCalled = false
        viewModel.assignPlanToSelectedClient(100L, onSuccess = { onSuccessCalled = true })
        advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertEquals("Plan assigned to Athlete B.", viewModel.uiState.value.assignmentMessage)
        coVerify(exactly = 1) { repository.assignPlanToClient(11L, 100L) }
        coVerify(exactly = 0) { repository.assignPlanToClient(10L, any()) }
    }

    @Test
    fun selectedClientState_isRetainedAcrossRecompositionOrSheetDismissal() = runTest {
        val rel = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Athlete A", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(rel))

        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        viewModel.selectClient(20L)
        advanceUntilIdle()
        assertEquals(20L, viewModel.uiState.value.selectedClientId)

        // Closing sheet or navigating back and returning:
        viewModel.clearAssignmentMessage()
        assertEquals(20L, viewModel.uiState.value.selectedClientId)
    }

    @Test
    fun assignPlan_whenNoClientSelected_displaysValidationMessage() = runTest {
        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedClientId)
        viewModel.assignPlanToSelectedClient(100L)
        advanceUntilIdle()

        assertEquals("Select a connected client first.", viewModel.uiState.value.assignmentMessage)
        coVerify(exactly = 0) { repository.assignPlanToClient(any(), any()) }

        viewModel.assignPlanToClient(null, 100L)
        assertEquals("Select a connected client first.", viewModel.uiState.value.assignmentMessage)
    }

    @Test
    fun assignPlan_whenClientIsDisconnected_isRejectedWithValidationMessage() = runTest {
        val terminatedRel = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Terminated Athlete", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "TERMINATED", createdAt = null, updatedAt = null
        )
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(terminatedRel))

        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.clients.isEmpty())

        viewModel.assignPlanToClient(20L, 100L)
        advanceUntilIdle()

        assertEquals("Select a connected client first.", viewModel.uiState.value.assignmentMessage)
        coVerify(exactly = 0) { repository.assignPlanToClient(any(), any()) }
    }

    @Test
    fun assignPlan_whenClientConnectedToAnotherCoach_isRejected() = runTest {
        val otherCoachRel = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 999L, coachName = "Other Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Athlete", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 999L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(otherCoachRel))

        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.clients.isEmpty())

        viewModel.assignPlanToClient(20L, 100L)
        advanceUntilIdle()

        assertEquals("Select a connected client first.", viewModel.uiState.value.assignmentMessage)
        coVerify(exactly = 0) { repository.assignPlanToClient(any(), any()) }
    }

    @Test
    fun assignPlan_whenNetworkFails_displaysParsedErrorMessage() = runTest {
        val rel = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Athlete A", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(rel))
        coEvery { repository.assignPlanToClient(10L, 100L) } returns Result.failure(
            retrofit2.HttpException(
                retrofit2.Response.error<Any>(
                    401,
                    okhttp3.ResponseBody.create(null, "{\"message\":\"Session expired\"}")
                )
            )
        )

        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        viewModel.assignPlanToClient(20L, 100L)
        advanceUntilIdle()

        assertEquals("Session expired. Please log in again.", viewModel.uiState.value.assignmentMessage)
    }

    @Test
    fun assignPlanToClient_withRelationshipId_resolvesAndAssignsSuccessfully() = runTest {
        val rel = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Athlete A", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(rel))
        coEvery { repository.assignPlanToClient(10L, 100L) } returns Result.success(Unit)

        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        var onSuccessCalled = false
        viewModel.assignPlanToClient(10L, 100L, onSuccess = { onSuccessCalled = true })
        advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertEquals("Plan assigned to Athlete A.", viewModel.uiState.value.assignmentMessage)
        coVerify(exactly = 1) { repository.assignPlanToClient(10L, 100L) }
    }

    @Test
    fun gettingStartedAddedToPlan_coexistsWithPersonalPlans_andDisappearsWhenCompleted() = runTest {
        val personalPlan = UserWorkoutPlan(id = 1L, name = "Existing PPL", splitType = WorkoutSplitType.PPL)
        coEvery { repository.getPlans() } returns Result.success(listOf(personalPlan))

        val updates = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 4)
        val gsRepo = mockk<com.calisthenics.ai.domain.repository.GettingStartedRepository>()
        io.mockk.every { gsRepo.enrollmentUpdates } returns updates
        val activeProgram = com.calisthenics.ai.domain.model.GettingStartedProgram(
            id = 10L,
            title = "Getting Started – 3 Days",
            published = true,
            days = listOf(
                com.calisthenics.ai.domain.model.GettingStartedDay(
                    id = 1L,
                    dayNumber = 1,
                    title = "Day 1",
                    status = com.calisthenics.ai.domain.model.GettingStartedDayStatus.NOT_STARTED
                )
            ),
            enrollment = com.calisthenics.ai.domain.model.GettingStartedEnrollment(
                id = 50L,
                programId = 10L,
                status = "IN_PROGRESS",
                addedToWorkoutPlan = true,
                completedDaysCount = 0,
                totalDays = 3,
                isCompleted = false
            )
        )
        val completedProgram = activeProgram.copy(
            enrollment = activeProgram.enrollment?.copy(
                status = "COMPLETED",
                completedDaysCount = 3,
                isCompleted = true
            )
        )
        coEvery { gsRepo.getPublishedProgram() } returnsMany listOf(
            Result.success(activeProgram),
            Result.success(completedProgram)
        )
        val session = com.calisthenics.ai.domain.model.WorkoutSession(
            id = 900L,
            workoutPlanId = null,
            workoutPlanDayId = null,
            assignmentId = null,
            source = "GETTING_STARTED",
            title = "Day 1",
            workoutDate = "2026-11-14",
            status = "IN_PROGRESS",
            startedAt = null,
            lastSavedAt = null,
            completedAt = null,
            durationSeconds = 0L,
            completedExercises = 0,
            skippedExercises = 0,
            unfinishedExercises = 1,
            completedSets = 0,
            totalReps = 0,
            totalHoldDurationSeconds = 0,
            exercises = emptyList()
        )
        coEvery { gsRepo.startDaySession(1, null) } returns Result.success(session)

        val viewModel = createViewModel(gettingStartedRepository = gsRepo)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.plans.size)
        assertEquals(10L, viewModel.uiState.value.activeGettingStartedProgram?.id)

        var started: com.calisthenics.ai.domain.model.WorkoutSession? = null
        viewModel.startGettingStartedDay(1) { started = it }
        advanceUntilIdle()
        assertEquals(900L, started?.id)
        assertEquals(
            com.calisthenics.ai.domain.model.GettingStartedDayStatus.IN_PROGRESS,
            viewModel.uiState.value.activeGettingStartedProgram?.days?.first()?.status
        )

        updates.emit(Unit)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeGettingStartedProgram)
        assertEquals(1, viewModel.uiState.value.plans.size)
    }

    @Test
    fun gettingStartedJustStart_notAddedToWorkoutPlan_isNotShownInWorkoutPlansList() = runTest {
        val gsRepo = mockk<com.calisthenics.ai.domain.repository.GettingStartedRepository>()
        io.mockk.every { gsRepo.enrollmentUpdates } returns kotlinx.coroutines.flow.emptyFlow()
        coEvery { gsRepo.getPublishedProgram() } returns Result.success(
            com.calisthenics.ai.domain.model.GettingStartedProgram(
                id = 10L,
                title = "Getting Started – 3 Days",
                published = true,
                enrollment = com.calisthenics.ai.domain.model.GettingStartedEnrollment(
                    id = 50L,
                    programId = 10L,
                    status = "IN_PROGRESS",
                    addedToWorkoutPlan = false,
                    isCompleted = false
                )
            )
        )

        val viewModel = createViewModel(gettingStartedRepository = gsRepo)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeGettingStartedProgram)
    }

    @Test
    fun assignPlanWithDetailsToClient_passesScheduleAndNotes() = runTest {
        val rel = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Athlete A", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(rel))
        coEvery { repository.assignPlanToClient(10L, 100L, "2026-11-15", "2026-11-30", "Slow eccentrics") } returns
            Result.success(Unit)

        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        var succeeded = false
        viewModel.assignPlanWithDetailsToClient(
            clientId = 20L,
            planId = 100L,
            startDate = "2026-11-15",
            endDate = "2026-11-30",
            notes = "Slow eccentrics",
            onSuccess = { succeeded = true }
        )
        advanceUntilIdle()

        assertTrue(succeeded)
        coVerify(exactly = 1) {
            repository.assignPlanToClient(10L, 100L, "2026-11-15", "2026-11-30", "Slow eccentrics")
        }
    }

    @Test
    fun assignPlanWithDetailsToClient_rejectsDeadlineBeforeStartDate() = runTest {
        val rel = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Athlete A", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(rel))

        val viewModel = createViewModel(userId = 1L)
        advanceUntilIdle()

        viewModel.assignPlanWithDetailsToClient(
            clientId = 20L,
            planId = 100L,
            startDate = "2026-11-20",
            endDate = "2026-11-10",
            notes = null
        )
        advanceUntilIdle()

        assertEquals("Deadline cannot be before the start date.", viewModel.uiState.value.assignmentMessage)
        coVerify(exactly = 0) { repository.assignPlanToClient(any(), any(), any(), any(), any()) }
    }

    @Test
    fun createCustomWorkoutAndAssignToClient_createsPlanAddsExercisesAndAssigns() = runTest {
        val rel = com.calisthenics.ai.domain.model.CoachClientRelationship(
            id = 10L, coachId = 1L, coachName = "Coach", coachAvatarUrl = null, coachExperienceLevel = null,
            clientId = 20L, clientName = "Athlete A", clientAvatarUrl = null, clientExperienceLevel = null,
            requestedBy = 1L, status = "ACTIVE", createdAt = null, updatedAt = null
        )
        val pushUp = com.calisthenics.ai.domain.model.Exercise(
            id = 1L,
            name = "Push-up",
            code = "push_up",
            category = com.calisthenics.ai.domain.model.ExerciseCategory.PUSH,
            difficulty = com.calisthenics.ai.domain.model.DifficultyLevel.BEGINNER,
            description = "Push-ups",
            targetMuscles = "Chest",
            instructions = "Lower chest"
        )
        val plank = com.calisthenics.ai.domain.model.Exercise(
            id = 2L,
            name = "Plank",
            code = "plank",
            category = com.calisthenics.ai.domain.model.ExerciseCategory.ISOMETRIC,
            difficulty = com.calisthenics.ai.domain.model.DifficultyLevel.BEGINNER,
            description = "Core hold",
            targetMuscles = "Core",
            instructions = "Hold straight line"
        )
        val mondayDay = com.calisthenics.ai.domain.model.UserWorkoutPlanDay(
            id = 501L,
            dayOfWeek = WorkoutDayOfWeek.MONDAY,
            sessionType = WorkoutSessionType.FULL_BODY,
            isRestDay = false
        )
        val createdPlan = UserWorkoutPlan(
            id = 300L,
            name = "Custom Coach Block",
            splitType = WorkoutSplitType.CUSTOM,
            days = listOf(mondayDay)
        )
        val exerciseRepo = mockk<com.calisthenics.ai.domain.repository.ExerciseRepository> {
            coEvery { getExercises() } returns Result.success(listOf(pushUp, plank))
        }

        coEvery { coachClientRepository.loadRelationships() } returns Result.success(listOf(rel))
        coEvery { repository.createPlan("Custom Coach Block", WorkoutSplitType.CUSTOM, any()) } returns
            Result.success(createdPlan)
        coEvery {
            repository.addExercise(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(
            com.calisthenics.ai.domain.model.UserWorkoutPlanExercise(id = 1L, exercise = pushUp, sets = 4, reps = 12)
        )
        coEvery {
            repository.assignPlanToClient(10L, 300L, "2026-11-15", "2026-12-01", "Focus on control")
        } returns Result.success(Unit)

        val viewModel = createViewModel(userId = 1L, exerciseRepository = exerciseRepo)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.availableExercises.size)

        var succeeded = false
        viewModel.createCustomWorkoutAndAssignToClient(
            clientId = 20L,
            workoutName = "Custom Coach Block",
            sessionType = WorkoutSessionType.FULL_BODY,
            targetDay = WorkoutDayOfWeek.MONDAY,
            configuredExercises = listOf(
                ConfiguredAssignmentExercise(pushUp, sets = 4, reps = 12, restSeconds = 90, notes = "Full ROM"),
                ConfiguredAssignmentExercise(plank, sets = 3, reps = null, durationSeconds = 45, restSeconds = 60, notes = "")
            ),
            startDate = "2026-11-15",
            endDate = "2026-12-01",
            notes = "Focus on control",
            onSuccess = { succeeded = true }
        )
        advanceUntilIdle()

        assertTrue(succeeded)
        coVerify(exactly = 1) {
            repository.addExercise(300L, 501L, 1L, 4, 12, null, 90, 1, "Full ROM")
            repository.addExercise(300L, 501L, 2L, 3, null, 45, 60, 2, null)
            repository.assignPlanToClient(10L, 300L, "2026-11-15", "2026-12-01", "Focus on control")
        }
    }
}
