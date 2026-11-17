package com.calisthenics.ai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.local.database.CachedWorkoutPlanDayEntity
import com.calisthenics.ai.data.local.database.CachedWorkoutPlanEntity
import com.calisthenics.ai.data.local.database.CachedWorkoutPlanExerciseEntity
import com.calisthenics.ai.data.local.database.WorkoutScheduleDao
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.data.remote.datasource.RemoteWorkoutPlanDataSource
import com.calisthenics.ai.data.remote.dto.WorkoutPlanDayResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanResponseDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutMetadataDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutPlanPreviewDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutSessionPreviewDto
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutPlanDayConfiguration
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSource
import com.calisthenics.ai.domain.model.WorkoutSplitType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkoutPlanRepositoryImplTest {

    private val context: Context = mockk(relaxed = true)
    private val preferences: SharedPreferences = mockk(relaxed = true)
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)
    private val remoteDataSource: RemoteWorkoutPlanDataSource = mockk()
    private val dao: WorkoutScheduleDao = mockk(relaxed = true)
    private val tokenManager: TokenManager = mockk(relaxed = true)
    private val connectivityMonitor: ConnectivityMonitor = mockk()

    private lateinit var repository: WorkoutPlanRepositoryImpl

    @Before
    fun setUp() {
        every { context.getSharedPreferences(any(), any()) } returns preferences
        every { preferences.edit() } returns editor
        every { editor.commit() } returns true
        every { tokenManager.getUserId() } returns 1L
        every { preferences.getLong(any(), -1L) } returns -1L

        repository = WorkoutPlanRepositoryImpl(
            context = context,
            remoteDataSource = remoteDataSource,
            workoutScheduleDao = dao,
            tokenManager = tokenManager,
            connectivityMonitor = connectivityMonitor
        )
    }

    @Test
    fun getPlans_online_fetchesFromRemoteAndCachesToRoom() = runTest {
        every { connectivityMonitor.isConnected() } returns true
        val remotePlan = WorkoutPlanResponseDto(
            id = 101L,
            name = "Push Pull Legs",
            splitType = "PPL",
            days = listOf(
                WorkoutPlanDayResponseDto(
                    id = 1L,
                    dayOfWeek = "MONDAY",
                    sessionType = "PUSH",
                    exercises = emptyList()
                )
            )
        )
        coEvery { remoteDataSource.getWorkoutPlans() } returns listOf(remotePlan)

        val result = repository.getPlans()

        assertTrue(result.isSuccess)
        val plans = result.getOrThrow()
        assertEquals(1, plans.size)
        assertEquals("Push Pull Legs", plans[0].name)
        coVerify { dao.syncPersonalPlans(1L, any()) }
    }

    @Test
    fun getPlans_offline_fallsBackToRoomDatabaseWithoutRemoteCall() = runTest {
        every { connectivityMonitor.isConnected() } returns false
        val cachedPlan = CachedWorkoutPlanEntity(
            ownerUserId = 1L,
            id = 202L,
            name = "Offline Plan",
            splitType = "PPL",
            isCoachAssigned = false,
            coachName = null,
            assignmentId = null,
            isActive = true,
            updatedAt = 1000L
        )
        val cachedDay = CachedWorkoutPlanDayEntity(
            ownerUserId = 1L,
            id = 21L,
            planId = 202L,
            dayOfWeek = "MONDAY",
            sessionType = "PUSH"
        )
        coEvery { dao.getAllPlans(1L) } returns listOf(cachedPlan)
        coEvery { dao.getDaysForPlan(1L, 202L) } returns listOf(cachedDay)
        coEvery { dao.getExercisesForPlan(1L, 202L) } returns emptyList()

        val result = repository.getPlans()

        assertTrue(result.isSuccess)
        val plans = result.getOrThrow()
        assertEquals(1, plans.size)
        assertEquals("Offline Plan", plans[0].name)
        assertEquals(202L, plans[0].id)
        coVerify(exactly = 0) { remoteDataSource.getWorkoutPlans() }
    }

    @Test
    fun resolveTodayWorkout_offline_prioritizesCoachAssignedPlanOverPersonalPlan() = runTest {
        every { connectivityMonitor.isConnected() } returns false
        val personalPlan = CachedWorkoutPlanEntity(
            ownerUserId = 1L,
            id = 1L,
            name = "Personal Plan",
            splitType = "PPL",
            isCoachAssigned = false,
            coachName = null,
            assignmentId = null,
            isActive = true,
            updatedAt = 1000L
        )
        val coachPlan = CachedWorkoutPlanEntity(
            ownerUserId = 1L,
            id = 2L,
            name = "Coach Plan",
            splitType = "FULL_BODY",
            isCoachAssigned = true,
            coachName = "Coach Alex",
            assignmentId = 88L,
            isActive = false,
            updatedAt = 1000L
        )
        coEvery { dao.getAllPlans(1L) } returns listOf(personalPlan, coachPlan)
        coEvery { dao.getDaysForPlan(1L, any()) } returns emptyList()
        coEvery { dao.getExercisesForPlan(1L, any()) } returns emptyList()

        val result = repository.resolveTodayWorkout("2026-11-05", activePlanId = 1L)

        assertTrue(result.isSuccess)
        val resolution = result.getOrThrow()
        assertEquals(WorkoutSource.COACH, resolution.source)
        assertEquals(2L, resolution.selectedPlanId)
        assertTrue(resolution.activeCoach)
    }

    @Test
    fun createPlan_offline_blocksMutationWithClearMessage() = runTest {
        every { connectivityMonitor.isConnected() } returns false

        val result = repository.createPlan(
            name = "New Plan",
            splitType = WorkoutSplitType.PPL,
            days = listOf(WorkoutDayOfWeek.MONDAY to WorkoutSessionType.PUSH)
        )

        assertTrue(result.isFailure)
        assertEquals("Cannot create workout plans while offline.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { remoteDataSource.createWorkoutPlan(any()) }
    }

    @Test
    fun deletePlan_offline_blocksMutationWithClearMessage() = runTest {
        every { connectivityMonitor.isConnected() } returns false

        val result = repository.deletePlan(101L)

        assertTrue(result.isFailure)
        assertEquals("Cannot delete workout plans while offline.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { remoteDataSource.deleteWorkoutPlan(any()) }
    }

    @Test
    fun deletePlan_online_deletesRemoteInvalidatesRoomClearsActivePlanAndSyncsRemaining() = runTest {
        every { connectivityMonitor.isConnected() } returns true
        every { tokenManager.currentSessionSnapshot() } returns com.calisthenics.ai.data.local.AccountSession(
            activeUserId = 1L,
            generation = 1L
        )
        every { preferences.getLong("user_1.active_plan_id", -1L) } returns 5L
        every { editor.remove(any()) } returns editor
        coEvery { remoteDataSource.deleteWorkoutPlan(5L) } returns Unit
        val remainingDto = WorkoutPlanResponseDto(
            id = 8L,
            name = "Remaining Plan",
            splitType = "PPL",
            days = emptyList()
        )
        coEvery { remoteDataSource.getWorkoutPlans() } returns listOf(remainingDto)

        val result = repository.deletePlan(5L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { remoteDataSource.deleteWorkoutPlan(5L) }
        coVerify(exactly = 1) { dao.deletePlanAndChildren(1L, 5L) }
        io.mockk.verify { editor.remove("user_1.active_plan_id") }
        coVerify(exactly = 1) { dao.syncPersonalPlans(1L, match { it.size == 1 && it[0].id == 8L }) }
    }

    @Test
    fun updatePlanDays_sendsCustomNameFocusAndRestDayAndCachesTheFullSchedule() = runTest {
        every { connectivityMonitor.isConnected() } returns true
        val existingDays = WorkoutDayOfWeek.entries.mapIndexed { index, day ->
            WorkoutPlanDayResponseDto(
                id = index + 1L,
                dayOfWeek = day.name,
                sessionType = if (day == WorkoutDayOfWeek.SUNDAY) "REST" else "PUSH"
            )
        }
        coEvery { remoteDataSource.getWorkoutPlanById(42L) } returns WorkoutPlanResponseDto(
            id = 42L,
            name = "My split",
            splitType = "CUSTOM",
            days = existingDays
        )
        coEvery { remoteDataSource.getWorkoutPlanDayExercises(42L, any()) } returns emptyList()

        val configurations = WorkoutDayOfWeek.entries.map { day ->
            when (day) {
                WorkoutDayOfWeek.MONDAY -> WorkoutPlanDayConfiguration(
                    dayOfWeek = day,
                    workoutType = null,
                    name = "  Planche + Push  ",
                    description = "  Planche progression  ",
                    isRestDay = false
                )
                WorkoutDayOfWeek.SUNDAY -> WorkoutPlanDayConfiguration(
                    dayOfWeek = day,
                    workoutType = WorkoutSessionType.REST,
                    name = null,
                    description = null,
                    isRestDay = true
                )
                else -> WorkoutPlanDayConfiguration(
                    dayOfWeek = day,
                    workoutType = WorkoutSessionType.PUSH,
                    name = "Push",
                    description = null,
                    isRestDay = false
                )
            }
        }
        val request = slot<UpdateWorkoutPlanRequestDto>()
        val updatedResponse = WorkoutPlanResponseDto(
            id = 42L,
            name = "My split",
            splitType = "CUSTOM",
            days = existingDays.map { day ->
                when (day.dayOfWeek) {
                    "MONDAY" -> day.copy(
                        sessionType = null,
                        name = "Planche + Push",
                        description = "Planche progression"
                    )
                    "SUNDAY" -> day.copy(sessionType = "REST", isRestDay = true)
                    else -> day
                }
            }
        )
        coEvery { remoteDataSource.updateWorkoutPlan(42L, capture(request)) } returns updatedResponse

        val result = repository.updatePlanDays(42L, configurations)

        assertTrue(result.isSuccess)
        assertEquals(WorkoutSplitType.CUSTOM, result.getOrThrow().splitType)
        assertEquals(WorkoutSessionType.UNSPECIFIED, result.getOrThrow().days.first().sessionType)
        val mondayRequest = request.captured.days.first { it.dayOfWeek == "MONDAY" }
        assertEquals(null, mondayRequest.sessionType)
        assertEquals("Planche + Push", mondayRequest.name)
        assertEquals("Planche progression", mondayRequest.description)
        val sundayRequest = request.captured.days.first { it.dayOfWeek == "SUNDAY" }
        assertTrue(sundayRequest.isRestDay)
        assertTrue(sundayRequest.sessionType == null)
        coVerify {
            dao.saveFullPlan(
                any(),
                match { days -> days.size == 7 && days.first { it.dayOfWeek == "MONDAY" }.description == "Planche progression" },
                any()
            )
        }
    }

    @Test
    fun generateAiWorkoutPlan_online_callsRemoteDataSource() = runTest {
        every { connectivityMonitor.isConnected() } returns true
        val previewDto = AiWorkoutPlanPreviewDto(
            planName = "AI Planche Program",
            splitType = "CUSTOM",
            sessions = emptyList(),
            metadata = AiWorkoutMetadataDto(modelUsed = "gemini-2.5-flash-lite")
        )
        coEvery { remoteDataSource.generateAiWorkoutPlan(any()) } returns previewDto

        val result = repository.generateAiWorkoutPlan(prompt = "Create 6-day planche routine")

        assertTrue(result.isSuccess)
        assertEquals("AI Planche Program", result.getOrThrow().planName)
        coVerify { remoteDataSource.generateAiWorkoutPlan(match { it.prompt == "Create 6-day planche routine" }) }
    }

    @Test
    fun modifyAiWorkoutPlan_online_callsRemoteDataSource() = runTest {
        every { connectivityMonitor.isConnected() } returns true
        val previewDto = AiWorkoutPlanPreviewDto(
            planName = "Modified Plan",
            splitType = "CUSTOM",
            sessions = emptyList(),
            metadata = AiWorkoutMetadataDto(modelUsed = "gemini-2.5-flash-lite")
        )
        coEvery { remoteDataSource.modifyAiWorkoutPlan(any()) } returns previewDto

        val result = repository.modifyAiWorkoutPlan(
            prompt = "Replace legs with mobility",
            currentPlan = previewDto.toDomain()
        )

        assertTrue(result.isSuccess)
        assertEquals("Modified Plan", result.getOrThrow().planName)
        coVerify { remoteDataSource.modifyAiWorkoutPlan(match { it.prompt == "Replace legs with mobility" }) }
    }

    @Test
    fun commitAiWorkoutPlan_online_savesAndSetsActivePlan() = runTest {
        every { connectivityMonitor.isConnected() } returns true
        val previewDto = AiWorkoutPlanPreviewDto(
            planName = "Committed Plan",
            splitType = "CUSTOM",
            sessions = emptyList(),
            metadata = AiWorkoutMetadataDto(modelUsed = "gemini-2.5-flash-lite")
        )
        val committedResponse = WorkoutPlanResponseDto(
            id = 777L,
            name = "Committed Plan",
            splitType = "CUSTOM",
            days = emptyList()
        )
        coEvery { remoteDataSource.commitAiWorkoutPlan(any()) } returns committedResponse
        coEvery { remoteDataSource.getWorkoutPlanById(777L) } returns committedResponse

        val result = repository.commitAiWorkoutPlan(
            plan = previewDto.toDomain(),
            replaceExisting = true
        )

        assertTrue(result.isSuccess)
        assertEquals(777L, result.getOrThrow().id)
        coVerify { remoteDataSource.commitAiWorkoutPlan(any()) }
    }

    @Test
    fun getMyAssignments_and_getMyCoachAssignments_excludeCancelledWhileKeepingInactive() = runTest {
        val dtoPlan = WorkoutPlanResponseDto(
            id = 55L,
            name = "Coach Plan",
            splitType = "PPL",
            days = emptyList()
        )
        val dtos = listOf(
            com.calisthenics.ai.data.remote.dto.WorkoutPlanAssignmentDto(
                id = 1L,
                relationshipId = 10L,
                coachName = "Coach",
                clientName = "Client",
                status = "ACTIVE",
                startDate = "2026-10-01",
                createdAt = "2026-10-01T00:00:00Z",
                workoutPlan = dtoPlan
            ),
            com.calisthenics.ai.data.remote.dto.WorkoutPlanAssignmentDto(
                id = 2L,
                relationshipId = 10L,
                coachName = "Coach",
                clientName = "Client",
                status = "INACTIVE",
                startDate = "2026-10-01",
                createdAt = "2026-10-01T00:00:00Z",
                workoutPlan = dtoPlan
            ),
            com.calisthenics.ai.data.remote.dto.WorkoutPlanAssignmentDto(
                id = 3L,
                relationshipId = 9L,
                coachName = "Coach",
                clientName = "Client",
                status = "CANCELLED",
                startDate = "2026-09-01",
                createdAt = "2026-09-01T00:00:00Z",
                workoutPlan = dtoPlan
            ),
            com.calisthenics.ai.data.remote.dto.WorkoutPlanAssignmentDto(
                id = 4L,
                relationshipId = 8L,
                coachName = "Coach",
                clientName = "Client",
                status = "ENDED",
                startDate = "2026-08-01",
                createdAt = "2026-08-01T00:00:00Z",
                workoutPlan = dtoPlan
            )
        )
        coEvery { remoteDataSource.getMyAssignments() } returns dtos
        coEvery { remoteDataSource.getMyCoachAssignments() } returns dtos

        val clientAssignments = repository.getMyAssignments().getOrThrow()
        val coachAssignments = repository.getMyCoachAssignments().getOrThrow()

        assertEquals(listOf(1L, 2L), clientAssignments.map { it.id })
        assertEquals(listOf(1L, 2L), coachAssignments.map { it.id })
    }

    @Test
    fun getPlanById_preservesCoachAssignedMetadata_andEvictsRoomCacheOn403() = runTest {
        every { connectivityMonitor.isConnected() } returns true
        every { tokenManager.currentSessionSnapshot() } returns com.calisthenics.ai.data.local.AccountSession(
            activeUserId = 1L,
            generation = 0L
        )
        coEvery { dao.getPlanById(1L, 88L) } returns CachedWorkoutPlanEntity(
            ownerUserId = 1L,
            id = 88L,
            name = "Coach Plan",
            splitType = "PPL",
            isCoachAssigned = true,
            coachName = "Coach Alex",
            assignmentId = 44L,
            isActive = true
        )
        coEvery { remoteDataSource.getWorkoutPlanById(88L) } returns WorkoutPlanResponseDto(
            id = 88L,
            name = "Coach Plan Updated",
            splitType = "PPL",
            days = emptyList(),
            isActive = false
        )
        val savedPlanSlot = slot<CachedWorkoutPlanEntity>()
        coEvery { dao.saveFullPlan(capture(savedPlanSlot), any(), any()) } returns Unit

        val loaded = repository.getPlanById(88L).getOrThrow()

        assertEquals(88L, loaded.id)
        assertTrue(savedPlanSlot.captured.isCoachAssigned)
        assertEquals("Coach Alex", savedPlanSlot.captured.coachName)
        assertEquals(44L, savedPlanSlot.captured.assignmentId)
        assertTrue(savedPlanSlot.captured.isActive)

        val forbidden = retrofit2.HttpException(
            retrofit2.Response.error<Any>(
                403,
                okhttp3.ResponseBody.create(null, "")
            )
        )
        coEvery { remoteDataSource.getWorkoutPlanById(88L) } throws forbidden

        val revokedResult = repository.getPlanById(88L)

        assertTrue(revokedResult.isFailure)
        coVerify(exactly = 1) { dao.deletePlanAndChildren(1L, 88L) }
    }
}
