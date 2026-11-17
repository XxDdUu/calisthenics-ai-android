package com.calisthenics.ai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.calisthenics.ai.data.local.GenderSessionStore
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.datasource.RemoteAuthDataSource
import com.calisthenics.ai.data.remote.dto.AdminUserDto
import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import com.calisthenics.ai.data.remote.dto.ClientProfileDto
import com.calisthenics.ai.data.remote.dto.CoachProfileDto
import com.calisthenics.ai.data.remote.dto.UserProfileResponseDto
import com.calisthenics.ai.domain.model.Gender
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GenderDataLayerTest {

    @Test
    fun `Gender fromApi parses valid values case-insensitively and rejects unknown values`() {
        assertEquals(Gender.MALE, Gender.fromApi("MALE"))
        assertEquals(Gender.MALE, Gender.fromApi(" male "))
        assertEquals(Gender.FEMALE, Gender.fromApi("FEMALE"))
        assertEquals(Gender.FEMALE, Gender.fromApi("female"))
        assertNull(Gender.fromApi(null))
        assertNull(Gender.fromApi(""))
        assertNull(Gender.fromApi("OTHER"))
    }

    @Test
    fun `all profile and auth DTO mappers map gender consistently`() {
        val authToken = AuthResponseDto(
            accessToken = "acc",
            refreshToken = "ref",
            userId = 5L,
            email = "a@b.com",
            fullName = "Athlete",
            role = "ROLE_USER",
            gender = "MALE"
        ).toDomain()
        assertEquals(Gender.MALE, authToken.gender)

        val userProfile = UserProfileResponseDto(
            id = 5L,
            email = "a@b.com",
            fullName = "Athlete",
            experienceLevel = "INTERMEDIATE",
            role = "ROLE_USER",
            gender = "FEMALE"
        ).toDomain()
        assertEquals(Gender.FEMALE, userProfile.gender)

        val clientProfile = ClientProfileDto(
            id = 5L,
            userId = 5L,
            fullName = "Athlete",
            gender = "MALE"
        ).toDomain()
        assertEquals(Gender.MALE, clientProfile.gender)

        val coachProfile = CoachProfileDto(
            id = 5L,
            userId = 5L,
            fullName = "Coach",
            gender = "FEMALE"
        ).toDomain()
        assertEquals(Gender.FEMALE, coachProfile.gender)

        val adminUser = AdminUserDto(
            id = 5L,
            email = "a@b.com",
            fullName = "Admin User",
            role = "ROLE_USER",
            hasCoachProfile = false,
            hasClientProfile = true,
            createdAt = null,
            gender = "MALE"
        ).toDomain()
        assertEquals(Gender.MALE, adminUser.gender)
    }

    @Test
    fun `updateGender persists cached gender and publishes to GenderSessionStore only on success`() = runTest {
        val remoteAuthDataSource = mockk<RemoteAuthDataSource>()
        val tokenManager = mockk<TokenManager>(relaxed = true)
        val genderSessionStore = GenderSessionStore()
        val onboardingSessionStore = com.calisthenics.ai.data.local.OnboardingSessionStore()
        val repository = AuthRepositoryImpl(remoteAuthDataSource, tokenManager, genderSessionStore, onboardingSessionStore)

        coEvery { remoteAuthDataSource.updateGender("MALE") } returns UserProfileResponseDto(
            id = 7L,
            email = "test@example.com",
            fullName = "Test",
            experienceLevel = "INTERMEDIATE",
            role = "ROLE_USER",
            gender = "MALE"
        )

        val successResult = repository.updateGender(Gender.MALE)
        assertTrue(successResult.isSuccess)
        assertEquals(Gender.MALE, successResult.getOrNull()?.gender)
        verify(exactly = 1) { tokenManager.saveAccountGender(7L, "MALE") }
        assertEquals(Gender.MALE, genderSessionStore.state.value?.gender)
        assertEquals(true, genderSessionStore.state.value?.isBackendConfirmed)

        // Failure case: must NOT save or publish
        coEvery { remoteAuthDataSource.updateGender("FEMALE") } throws IOException("Offline")
        val failureResult = repository.updateGender(Gender.FEMALE)
        assertTrue(failureResult.isFailure)
        verify(exactly = 0) { tokenManager.saveAccountGender(7L, "FEMALE") }
        assertEquals(Gender.MALE, genderSessionStore.state.value?.gender)
    }

    @Test
    fun `login Google OAuth and getCurrentUser sync gender state across TokenManager and GenderSessionStore`() = runTest {
        val remoteAuthDataSource = mockk<RemoteAuthDataSource>()
        val tokenManager = mockk<TokenManager>(relaxed = true)
        val genderSessionStore = GenderSessionStore()
        val onboardingSessionStore = com.calisthenics.ai.data.local.OnboardingSessionStore()
        val repository = AuthRepositoryImpl(remoteAuthDataSource, tokenManager, genderSessionStore, onboardingSessionStore)

        coEvery { remoteAuthDataSource.login(any()) } returns AuthResponseDto(
            accessToken = "acc",
            refreshToken = "ref",
            userId = 20L,
            email = "login@example.com",
            fullName = "Login User",
            role = "ROLE_USER",
            gender = null
        )
        val loginRes = repository.login("login@example.com", "secret")
        assertTrue(loginRes.isSuccess)
        assertNull(loginRes.getOrNull()?.gender)
        verify { tokenManager.saveAccountGender(20L, null) }
        assertEquals(20L, genderSessionStore.state.value?.userId)
        assertNull(genderSessionStore.state.value?.gender)
        assertEquals(true, genderSessionStore.state.value?.isBackendConfirmed)

        coEvery { remoteAuthDataSource.authenticateWithGoogle("google-token") } returns AuthResponseDto(
            accessToken = "acc2",
            refreshToken = "ref2",
            userId = 21L,
            email = "google@example.com",
            fullName = "Google User",
            role = "ROLE_USER",
            gender = "FEMALE"
        )
        val googleRes = repository.loginWithGoogle("google-token")
        assertTrue(googleRes.isSuccess)
        assertEquals(Gender.FEMALE, googleRes.getOrNull()?.gender)
        verify { tokenManager.saveAccountGender(21L, "FEMALE") }
        assertEquals(Gender.FEMALE, genderSessionStore.state.value?.gender)

        coEvery { remoteAuthDataSource.authenticateWithFirebase("firebase-id-token") } returns AuthResponseDto(
            accessToken = "acc3",
            refreshToken = "ref3",
            userId = 22L,
            email = "firebase@example.com",
            fullName = "Firebase User",
            role = "ROLE_USER",
            gender = "MALE",
            onboardingStatus = "DISMISSED"
        )
        val firebaseRes = repository.loginWithFirebase("firebase-id-token")
        assertTrue(firebaseRes.isSuccess)
        assertEquals(Gender.MALE, firebaseRes.getOrNull()?.gender)
        verify { tokenManager.saveAuthToken("acc3", "ref3", 22L, "ROLE_USER", "firebase@example.com", "Firebase User") }
        verify { tokenManager.saveAccountGender(22L, "MALE") }
        assertEquals(Gender.MALE, genderSessionStore.state.value?.gender)

        repository.clearAuth()
        assertNull(genderSessionStore.state.value)
    }

    @Test
    fun `TokenManager stores normalizes and clears per-account gender`() {
        val backingMap = mutableMapOf<String, String?>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val context = mockk<Context>(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            backingMap[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(any()) } answers {
            backingMap.remove(firstArg())
            editor
        }
        every { prefs.getString(any(), any()) } answers {
            backingMap[firstArg()] ?: secondArg()
        }

        val tokenManager = TokenManager(context)
        tokenManager.saveAccountGender(5L, "male")
        assertEquals("MALE", tokenManager.getAccountGender(5L))

        tokenManager.saveAccountGender(5L, "INVALID")
        assertNull(tokenManager.getAccountGender(5L))

        tokenManager.saveAccountGender(5L, "FEMALE")
        assertEquals("FEMALE", tokenManager.getAccountGender(5L))
        tokenManager.removeAccount(5L)
        assertNull(tokenManager.getAccountGender(5L))
    }
}
