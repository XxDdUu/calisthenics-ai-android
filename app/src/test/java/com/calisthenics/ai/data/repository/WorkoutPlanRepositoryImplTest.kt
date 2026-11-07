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
        coVerify { dao.insertPlans(any()) }
    }

    @Test
    fun getPlans_offline_fallsBackToRoomDatabaseWithoutRemoteCall() = runTest {
        every { connectivityMonitor.isConnected() } returns false
        val cachedPlan = CachedWorkoutPlanEntity(
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
            id = 21L,
            planId = 202L,
            dayOfWeek = "MONDAY",
            sessionType = "PUSH"
        )
        coEvery { dao.getAllPlans() } returns listOf(cachedPlan)
        coEvery { dao.getDaysForPlan(202L) } returns listOf(cachedDay)
        coEvery { dao.getExercisesForPlan(202L) } returns emptyList()

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
            id = 2L,
            name = "Coach Plan",
            splitType = "FULL_BODY",
            isCoachAssigned = true,
            coachName = "Coach Alex",
            assignmentId = 88L,
            isActive = false,
            updatedAt = 1000L
        )
        coEvery { dao.getAllPlans() } returns listOf(personalPlan, coachPlan)
        coEvery { dao.getDaysForPlan(any()) } returns emptyList()
        coEvery { dao.getExercisesForPlan(any()) } returns emptyList()

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
}
