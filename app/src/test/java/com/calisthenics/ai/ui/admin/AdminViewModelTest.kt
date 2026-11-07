package com.calisthenics.ai.ui.admin

import com.calisthenics.ai.domain.model.AdminExercise
import com.calisthenics.ai.domain.model.AdminExerciseDeleteResult
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.repository.AdminRepository
import com.calisthenics.ai.domain.repository.AuthRepository
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val adminRepository: AdminRepository = mockk()
    private val authRepository: AuthRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesUnauthorizedHttpStatusesForNavigationHandling() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)

        listOf(401, 403).forEach { statusCode ->
            coEvery { adminRepository.getDashboard() } returns Result.failure(httpError(statusCode))

            viewModel.loadDashboard()
            advanceUntilIdle()

            assertEquals(statusCode, viewModel.uiState.value.authorizationErrorCode)
        }
    }

    @Test
    fun loadExercises_Success_UpdatesUiState() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val exercise = testExercise(1L, "Handstand", true)
        val page = AdminPage(listOf(exercise), 0, 20, 1, 1, false)

        coEvery {
            adminRepository.getExercises(0, 20, "hand", null, null, null, "name", "ASC")
        } returns Result.success(page)

        viewModel.loadExercises(page = 0, search = "hand")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.exercisePage?.content?.size)
        assertEquals("Handstand", viewModel.uiState.value.exercisePage?.content?.first()?.name)
    }

    @Test
    fun createExercise_Success_LoadsExercisesAndInvokesCallback() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val exercise = testExercise(5L, "L-Sit", true)
        var callbackInvoked = false

        coEvery {
            adminRepository.createExercise(
                name = "L-Sit",
                code = null,
                category = "CORE",
                difficulty = "BEGINNER",
                description = null,
                targetMuscles = "Abs",
                instructions = null,
                imageUrl = null,
                videoUrl = null,
                equipment = "None",
                active = true
            )
        } returns Result.success(exercise)

        coEvery {
            adminRepository.getExercises(0, 20, any(), any(), any(), any(), any(), any())
        } returns Result.success(AdminPage(listOf(exercise), 0, 20, 1, 1, false))

        viewModel.createExercise(
            name = "L-Sit",
            code = null,
            category = "CORE",
            difficulty = "BEGINNER",
            description = null,
            targetMuscles = "Abs",
            instructions = null,
            imageUrl = null,
            videoUrl = null,
            equipment = "None",
            active = true,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("Exercise created successfully.", viewModel.uiState.value.message)
    }

    @Test
    fun updateExerciseStatus_Success_UpdatesItemInPage() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val activeEx = testExercise(10L, "Planche", true)
        val inactiveEx = activeEx.copy(active = false)

        val initialPage = AdminPage(listOf(activeEx), 0, 20, 1, 1, false)
        coEvery {
            adminRepository.getExercises(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(initialPage)

        viewModel.loadExercises()
        advanceUntilIdle()

        coEvery { adminRepository.updateExerciseStatus(10L, false) } returns Result.success(inactiveEx)

        viewModel.updateExerciseStatus(10L, false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.exercisePage?.content?.first()?.active ?: true)
    }

    @Test
    fun deleteExercise_Success_RemovesFromPage() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val ex1 = testExercise(20L, "Old Exercise", true)
        val initialPage = AdminPage(listOf(ex1), 0, 20, 1, 1, false)

        coEvery {
            adminRepository.getExercises(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(initialPage)

        viewModel.loadExercises()
        advanceUntilIdle()

        coEvery {
            adminRepository.deleteExercise(20L, false)
        } returns Result.success(AdminExerciseDeleteResult(true, false, "Exercise deleted."))

        var successInvoked = false
        viewModel.deleteExercise(
            exerciseId = 20L,
            softDelete = false,
            onSuccess = { successInvoked = true },
            onConflict = {}
        )
        advanceUntilIdle()

        assertTrue(successInvoked)
        assertEquals(0, viewModel.uiState.value.exercisePage?.content?.size)
    }

    @Test
    fun deleteExercise_Conflict_InvokesOnConflict() = runTest {
        val viewModel = AdminViewModel(adminRepository, authRepository)
        val conflictBody = "{\"message\":\"Cannot delete in-use exercise\",\"status\":409}".toResponseBody("application/json".toMediaType())
        val http409 = HttpException(Response.error<Unit>(409, conflictBody))

        coEvery {
            adminRepository.deleteExercise(30L, false)
        } returns Result.failure(http409)

        var conflictReceived: String? = null
        viewModel.deleteExercise(
            exerciseId = 30L,
            softDelete = false,
            onSuccess = {},
            onConflict = { conflictReceived = it }
        )
        advanceUntilIdle()

        assertNotNull(conflictReceived)
        assertTrue(conflictReceived!!.contains("Cannot delete in-use exercise"))
    }

    private fun testExercise(id: Long, name: String, active: Boolean) = AdminExercise(
        id = id,
        name = name,
        code = name.uppercase().replace(" ", "_"),
        category = "SKILL",
        difficulty = "ADVANCED",
        description = "Test description",
        targetMuscles = "Chest",
        instructions = "Step 1",
        imageUrl = null,
        videoUrl = null,
        equipment = "None",
        active = active,
        createdAt = "2026-11-04T12:00:00Z",
        planReferencesCount = 0L,
        sessionReferencesCount = 0L,
        canDelete = true
    )

    private fun httpError(statusCode: Int): HttpException {
        val body = "denied".toResponseBody("text/plain".toMediaType())
        return HttpException(Response.error<Unit>(statusCode, body))
    }
}
