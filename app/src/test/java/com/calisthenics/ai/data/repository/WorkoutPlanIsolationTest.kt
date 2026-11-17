package com.calisthenics.ai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.calisthenics.ai.data.local.AccountSession
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.local.database.AppDatabase
import com.calisthenics.ai.data.local.database.CachedWorkoutPlanDayEntity
import com.calisthenics.ai.data.local.database.CachedWorkoutPlanEntity
import com.calisthenics.ai.data.local.database.CachedWorkoutPlanExerciseEntity
import com.calisthenics.ai.data.local.database.WorkoutScheduleDao
import com.calisthenics.ai.data.network.ConnectivityMonitor
import com.calisthenics.ai.data.remote.datasource.RemoteWorkoutPlanDataSource
import com.calisthenics.ai.data.remote.dto.TodayWorkoutCandidateDto
import com.calisthenics.ai.data.remote.dto.TodayWorkoutResolutionResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanDayResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanResponseDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutPlanIsolationTest {

    private lateinit var prefs: LocalSharedPrefs
    private lateinit var tokenManager: TokenManager
    private lateinit var remoteDataSource: RemoteWorkoutPlanDataSource
    private lateinit var fakeDao: InMemoryWorkoutScheduleDao
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var repository: WorkoutPlanRepositoryImpl

    @Before
    fun setUp() {
        prefs = LocalSharedPrefs()
        val context = mockk<Context>(relaxed = true) {
            every { getSharedPreferences(any(), any()) } returns prefs
            every { applicationContext } returns this
        }
        tokenManager = TokenManager(context)
        remoteDataSource = mockk()
        fakeDao = InMemoryWorkoutScheduleDao()
        connectivityMonitor = mockk()

        repository = WorkoutPlanRepositoryImpl(
            context = context,
            tokenManager = tokenManager,
            remoteDataSource = remoteDataSource,
            workoutScheduleDao = fakeDao,
            connectivityMonitor = connectivityMonitor
        )
    }

    @Test
    fun `Scenario D - offline workout plans and today resolution strictly isolate Account A, Account B, and legacy unowned rows`() = runTest {
        every { connectivityMonitor.isConnected() } returns false

        // Simulate legacy pre-migration rows under LEGACY_UNOWNED_USER_ID (-1) plus Account A & Account B caches.
        fakeDao.seedLegacyPlan(
            CachedWorkoutPlanEntity(
                ownerUserId = AppDatabase.LEGACY_UNOWNED_USER_ID,
                id = 999L,
                name = "Legacy Unowned Plan",
                splitType = "PPL",
                isActive = true
            )
        )

        tokenManager.saveAuthToken(jwtFor(1L), "ref-a", userId = 1L, role = "ROLE_USER")
        fakeDao.insertPlan(
            CachedWorkoutPlanEntity(ownerUserId = 1L, id = 10L, name = "Account A Plan", splitType = "PPL", isActive = true)
        )
        tokenManager.saveAuthToken(jwtFor(2L), "ref-b", userId = 2L, role = "ROLE_USER")
        fakeDao.insertPlan(
            CachedWorkoutPlanEntity(ownerUserId = 2L, id = 20L, name = "Account B Plan", splitType = "FULL_BODY", isActive = true)
        )

        // Active user is B (2L): must see ONLY B's plan offline
        val bPlans = repository.getPlans().getOrThrow()
        assertEquals(listOf("Account B Plan"), bPlans.map { it.name })
        val bToday = repository.resolveTodayWorkout("2026-11-10", null).getOrThrow()
        assertEquals(listOf("Account B Plan"), bToday.candidates.map { it.plan.name })
        assertEquals(20L, bToday.selectedPlanId)

        // Switch offline back to A (1L): must see ONLY A's plan offline
        assertTrue(tokenManager.switchToAccount(1L))
        val aPlans = repository.getPlans().getOrThrow()
        assertEquals(listOf("Account A Plan"), aPlans.map { it.name })
        val aToday = repository.resolveTodayWorkout("2026-11-10", null).getOrThrow()
        assertEquals(listOf("Account A Plan"), aToday.candidates.map { it.plan.name })
        assertEquals(10L, aToday.selectedPlanId)

        // Deactivate session: logged out user sees zero plans offline (never legacy -1 or previous user)
        tokenManager.deactivateActiveSession()
        assertTrue(repository.getPlans().getOrThrow().isEmpty())
        assertTrue(repository.resolveTodayWorkout("2026-11-10", null).getOrThrow().candidates.isEmpty())
    }

    @Test
    fun `coach and athlete can cache same planId independently without collision`() = runTest {
        every { connectivityMonitor.isConnected() } returns false

        tokenManager.saveAuthToken(jwtFor(10L), "ref-10", userId = 10L, role = "ROLE_USER")
        fakeDao.saveFullPlan(
            plan = CachedWorkoutPlanEntity(ownerUserId = 10L, id = 42L, name = "Coach Owned Plan 42", splitType = "CUSTOM", isCoachAssigned = false, isActive = true),
            days = listOf(CachedWorkoutPlanDayEntity(ownerUserId = 10L, id = 100L, planId = 42L, dayOfWeek = "MONDAY", sessionType = "PUSH")),
            exercises = emptyList()
        )

        tokenManager.saveAuthToken(jwtFor(20L), "ref-20", userId = 20L, role = "ROLE_USER")
        fakeDao.saveFullPlan(
            plan = CachedWorkoutPlanEntity(ownerUserId = 20L, id = 42L, name = "Assigned Plan 42", splitType = "CUSTOM", isCoachAssigned = true, coachName = "Coach 10", assignmentId = 77L, isActive = true),
            days = listOf(CachedWorkoutPlanDayEntity(ownerUserId = 20L, id = 100L, planId = 42L, dayOfWeek = "MONDAY", sessionType = "PUSH")),
            exercises = emptyList()
        )

        val athletePlan = repository.getPlanById(42L).getOrThrow()
        assertEquals("Assigned Plan 42", athletePlan.name)

        tokenManager.switchToAccount(10L)
        val coachPlan = repository.getPlanById(42L).getOrThrow()
        assertEquals("Coach Owned Plan 42", coachPlan.name)
    }

    @Test
    fun `observeCachedPlans automatically switches reactive Room stream when active account changes`() = runTest {
        tokenManager.saveAuthToken(jwtFor(1L), "ref-1", userId = 1L, role = "ROLE_USER")
        fakeDao.insertPlan(CachedWorkoutPlanEntity(ownerUserId = 1L, id = 101L, name = "User 1 Plan", splitType = "PPL"))
        fakeDao.insertPlan(CachedWorkoutPlanEntity(ownerUserId = 2L, id = 202L, name = "User 2 Plan", splitType = "UPPER_LOWER"))

        val observed1 = repository.observeCachedPlans().first()
        assertEquals(listOf("User 1 Plan"), observed1.map { it.name })

        tokenManager.saveAuthToken(jwtFor(2L), "ref-2", userId = 2L, role = "ROLE_USER")
        val observed2 = repository.observeCachedPlans().first()
        assertEquals(listOf("User 2 Plan"), observed2.map { it.name })
    }

    @Test
    fun `Scenario C - delayed getPlans and resolveTodayWorkout responses from Account A never pollute Account B Room cache`() = runTest {
        every { connectivityMonitor.isConnected() } returns true
        val delayedUserAPlans = CompletableDeferred<List<WorkoutPlanResponseDto>>()
        val delayedUserAToday = CompletableDeferred<TodayWorkoutResolutionResponseDto>()
        coEvery { remoteDataSource.getWorkoutPlans() } coAnswers { delayedUserAPlans.await() }
        coEvery { remoteDataSource.resolveTodayWorkout("2026-11-10", any()) } coAnswers { delayedUserAToday.await() }

        tokenManager.saveAuthToken(jwtFor(1L), "ref-a", userId = 1L, role = "ROLE_USER")

        val jobA1 = launch { repository.getPlans() }
        val jobA2 = launch { repository.resolveTodayWorkout("2026-11-10", null) }
        advanceUntilIdle()

        // Switch immediately to B before A's responses return
        tokenManager.saveAuthToken(jwtFor(2L), "ref-b", userId = 2L, role = "ROLE_USER")

        // Delayed Account A responses finish while Account B is active
        delayedUserAPlans.complete(
            listOf(WorkoutPlanResponseDto(id = 555L, name = "Stale User A Plan", splitType = "PPL", days = emptyList()))
        )
        delayedUserAToday.complete(
            TodayWorkoutResolutionResponseDto(
                workoutDate = "2026-11-10",
                activeCoach = false,
                source = "PERSONAL",
                selectionRequired = false,
                selectedPlanId = 555L,
                candidates = listOf(
                    TodayWorkoutCandidateDto(
                        workoutPlan = WorkoutPlanResponseDto(
                            id = 555L,
                            name = "Stale User A Today",
                            splitType = "PPL",
                            days = listOf(WorkoutPlanDayResponseDto(id = 1L, dayOfWeek = "TUESDAY", sessionType = "PUSH"))
                        ),
                        today = WorkoutPlanDayResponseDto(id = 1L, dayOfWeek = "TUESDAY", sessionType = "PUSH"),
                        assignmentId = null,
                        coachName = null
                    )
                )
            )
        )
        advanceUntilIdle()
        jobA1.join()
        jobA2.join()

        // Neither User 2's cache nor User 1's stale late write corrupted User 2
        assertTrue(fakeDao.getAllPlans(2L).isEmpty())
    }

    private fun jwtFor(userId: Long): String {
        val futureExp = (System.currentTimeMillis() / 1000) + 3600
        val payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"sub\":\"$userId\",\"exp\":$futureExp}".toByteArray())
        return "hdr.$payload.sig"
    }

    private class InMemoryWorkoutScheduleDao : WorkoutScheduleDao {
        private val plans = mutableListOf<CachedWorkoutPlanEntity>()
        private val days = mutableListOf<CachedWorkoutPlanDayEntity>()
        private val exercises = mutableListOf<CachedWorkoutPlanExerciseEntity>()
        private val trigger = MutableStateFlow(0)

        fun seedLegacyPlan(plan: CachedWorkoutPlanEntity) {
            plans.removeAll { it.ownerUserId == plan.ownerUserId && it.id == plan.id }
            plans.add(plan)
            trigger.value++
        }

        override fun getAllPlansFlow(ownerUserId: Long): Flow<List<CachedWorkoutPlanEntity>> =
            trigger.map { plans.filter { it.ownerUserId == ownerUserId } }

        override suspend fun getAllPlans(ownerUserId: Long): List<CachedWorkoutPlanEntity> =
            plans.filter { it.ownerUserId == ownerUserId }

        override fun getPlanByIdFlow(ownerUserId: Long, planId: Long): Flow<CachedWorkoutPlanEntity?> =
            trigger.map { plans.firstOrNull { it.ownerUserId == ownerUserId && it.id == planId } }

        override suspend fun getPlanById(ownerUserId: Long, planId: Long): CachedWorkoutPlanEntity? =
            plans.firstOrNull { it.ownerUserId == ownerUserId && it.id == planId }

        override suspend fun getCoachAssignedPlan(ownerUserId: Long): CachedWorkoutPlanEntity? =
            plans.firstOrNull { it.ownerUserId == ownerUserId && it.isCoachAssigned }

        override suspend fun getActivePlan(ownerUserId: Long): CachedWorkoutPlanEntity? =
            plans.firstOrNull { it.ownerUserId == ownerUserId && it.isActive }

        override fun getDaysForPlanFlow(ownerUserId: Long, planId: Long): Flow<List<CachedWorkoutPlanDayEntity>> =
            trigger.map { days.filter { it.ownerUserId == ownerUserId && it.planId == planId } }

        override suspend fun getDaysForPlan(ownerUserId: Long, planId: Long): List<CachedWorkoutPlanDayEntity> =
            days.filter { it.ownerUserId == ownerUserId && it.planId == planId }

        override fun getExercisesForDayFlow(ownerUserId: Long, dayId: Long): Flow<List<CachedWorkoutPlanExerciseEntity>> =
            trigger.map { exercises.filter { it.ownerUserId == ownerUserId && it.dayId == dayId } }

        override suspend fun getExercisesForDay(ownerUserId: Long, dayId: Long): List<CachedWorkoutPlanExerciseEntity> =
            exercises.filter { it.ownerUserId == ownerUserId && it.dayId == dayId }

        override fun getExercisesForPlanFlow(ownerUserId: Long, planId: Long): Flow<List<CachedWorkoutPlanExerciseEntity>> =
            trigger.map { exercises.filter { it.ownerUserId == ownerUserId && it.planId == planId } }

        override suspend fun getExercisesForPlan(ownerUserId: Long, planId: Long): List<CachedWorkoutPlanExerciseEntity> =
            exercises.filter { it.ownerUserId == ownerUserId && it.planId == planId }

        override suspend fun insertPlans(plans: List<CachedWorkoutPlanEntity>) {
            plans.forEach { p ->
                this.plans.removeAll { it.ownerUserId == p.ownerUserId && it.id == p.id }
                this.plans.add(p)
            }
            trigger.value++
        }

        override suspend fun insertPlan(plan: CachedWorkoutPlanEntity) {
            insertPlans(listOf(plan))
        }

        override suspend fun insertDays(days: List<CachedWorkoutPlanDayEntity>) {
            days.forEach { d ->
                this.days.removeAll { it.ownerUserId == d.ownerUserId && it.planId == d.planId && it.dayOfWeek == d.dayOfWeek }
                this.days.add(d)
            }
            trigger.value++
        }

        override suspend fun insertExercises(exercises: List<CachedWorkoutPlanExerciseEntity>) {
            exercises.forEach { e ->
                this.exercises.removeAll { it.ownerUserId == e.ownerUserId && it.planId == e.planId && it.dayId == e.dayId && it.id == e.id }
                this.exercises.add(e)
            }
            trigger.value++
        }

        override suspend fun deleteDaysForPlan(ownerUserId: Long, planId: Long) {
            days.removeAll { it.ownerUserId == ownerUserId && it.planId == planId }
            trigger.value++
        }

        override suspend fun deleteExercisesForPlan(ownerUserId: Long, planId: Long) {
            exercises.removeAll { it.ownerUserId == ownerUserId && it.planId == planId }
            trigger.value++
        }

        override suspend fun deletePlan(ownerUserId: Long, planId: Long) {
            plans.removeAll { it.ownerUserId == ownerUserId && it.id == planId }
            trigger.value++
        }

        override suspend fun deleteCoachAssignedExercisesForUser(ownerUserId: Long) {
            val coachPlanIds = plans.filter { it.ownerUserId == ownerUserId && it.isCoachAssigned }.map { it.id }.toSet()
            exercises.removeAll { it.ownerUserId == ownerUserId && it.planId in coachPlanIds }
            trigger.value++
        }

        override suspend fun deleteCoachAssignedDaysForUser(ownerUserId: Long) {
            val coachPlanIds = plans.filter { it.ownerUserId == ownerUserId && it.isCoachAssigned }.map { it.id }.toSet()
            days.removeAll { it.ownerUserId == ownerUserId && it.planId in coachPlanIds }
            trigger.value++
        }

        override suspend fun deleteCoachAssignedPlansForUser(ownerUserId: Long) {
            plans.removeAll { it.ownerUserId == ownerUserId && it.isCoachAssigned }
            trigger.value++
        }

        override suspend fun deletePersonalPlansForUser(ownerUserId: Long) {
            plans.removeAll { it.ownerUserId == ownerUserId && !it.isCoachAssigned }
            trigger.value++
        }

        override suspend fun deletePersonalPlansNotIn(ownerUserId: Long, retainedPlanIds: List<Long>) {
            plans.removeAll { it.ownerUserId == ownerUserId && !it.isCoachAssigned && it.id !in retainedPlanIds }
            trigger.value++
        }

        override suspend fun deleteOrphanedDaysForUser(ownerUserId: Long) {
            val validPlanIds = plans.filter { it.ownerUserId == ownerUserId }.map { it.id }.toSet()
            days.removeAll { it.ownerUserId == ownerUserId && it.planId !in validPlanIds }
            trigger.value++
        }

        override suspend fun deleteOrphanedExercisesForUser(ownerUserId: Long) {
            val validPlanIds = plans.filter { it.ownerUserId == ownerUserId }.map { it.id }.toSet()
            exercises.removeAll { it.ownerUserId == ownerUserId && it.planId !in validPlanIds }
            trigger.value++
        }

        override suspend fun setActivePlanId(ownerUserId: Long, activePlanId: Long) {
            for (i in plans.indices) {
                val p = plans[i]
                if (p.ownerUserId == ownerUserId) {
                    plans[i] = p.copy(isActive = (p.id == activePlanId))
                }
            }
            trigger.value++
        }

        override suspend fun clearExercisesForUser(ownerUserId: Long) {
            exercises.removeAll { it.ownerUserId == ownerUserId }
            trigger.value++
        }

        override suspend fun clearDaysForUser(ownerUserId: Long) {
            days.removeAll { it.ownerUserId == ownerUserId }
            trigger.value++
        }

        override suspend fun clearPlansForUser(ownerUserId: Long) {
            plans.removeAll { it.ownerUserId == ownerUserId }
            trigger.value++
        }

        override suspend fun clearExercises() {
            exercises.clear()
            trigger.value++
        }

        override suspend fun clearDays() {
            days.clear()
            trigger.value++
        }

        override suspend fun clearPlans() {
            plans.clear()
            trigger.value++
        }
    }

    private class LocalSharedPrefs : SharedPreferences {
        private val map = linkedMapOf<String, Any?>()
        override fun getAll(): MutableMap<String, *> = LinkedHashMap(map)
        override fun getString(key: String?, defValue: String?): String? = if (key != null && map.containsKey(key)) map[key] as? String else defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (map[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = key != null && map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val staged = linkedMapOf<String, Any?>()
            private var clearReq = false
            private val removals = linkedSetOf<String>()
            override fun putString(key: String?, value: String?) = apply { if (key != null) staged[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { if (key != null) staged[key] = values?.toSet() }
            override fun putInt(key: String?, value: Int) = apply { if (key != null) staged[key] = value }
            override fun putLong(key: String?, value: Long) = apply { if (key != null) staged[key] = value }
            override fun putFloat(key: String?, value: Float) = apply { if (key != null) staged[key] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) staged[key] = value }
            override fun remove(key: String?) = apply { if (key != null) removals.add(key) }
            override fun clear() = apply { clearReq = true }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearReq) map.clear()
                removals.forEach { map.remove(it) }
                map.putAll(staged)
            }
        }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }
}
