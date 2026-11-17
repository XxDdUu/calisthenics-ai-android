package com.calisthenics.ai.ui.notifications

import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.remote.dto.NotificationDto
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val analyticsRepository: AnalyticsRepository = mockk()
    private lateinit var sessionManager: AccountSessionManager

    private val sampleToday = LocalDate.of(2026, 11, 14)

    private val workoutNotif = NotificationDto(
        id = 1L,
        recipientUserId = 10L,
        type = "WORKOUT_ASSIGNED",
        notificationType = "WORKOUT_ASSIGNED",
        relatedEntityType = "WORKOUT_PLAN",
        relatedEntityId = 42L,
        deepLink = "workout_plan_details/42",
        title = "New Workout Assigned",
        message = "Coach assigned Hypertrophy PPL.",
        isRead = false,
        createdAt = "2026-11-14T10:00:00Z"
    )

    private val programNotif = NotificationDto(
        id = 2L,
        recipientUserId = 10L,
        type = "PROGRAM_AVAILABLE",
        notificationType = "PROGRAM_AVAILABLE",
        relatedEntityType = "GETTING_STARTED_PROGRAM",
        relatedEntityId = 7L,
        deepLink = "getting_started/7",
        title = "New Program Available",
        message = "A new 3 Days Getting Started program is ready for you.",
        isRead = false,
        createdAt = "2026-11-13T15:30:00Z"
    )

    private val coachRequestNotif = NotificationDto(
        id = 3L,
        recipientUserId = 10L,
        type = "COACH_CLIENT_REQUEST",
        notificationType = "COACH_CLIENT_REQUEST",
        relatedEntityType = "COACH_CLIENT_RELATIONSHIP",
        relatedEntityId = 88L,
        deepLink = "coach",
        title = "Coaching Request Accepted",
        message = "Coach Maya accepted your coaching request.",
        isRead = true,
        createdAt = "2026-11-10T09:00:00Z"
    )

    private val announcementNotif = NotificationDto(
        id = 4L,
        recipientUserId = 10L,
        type = "ADMIN_ANNOUNCEMENT",
        notificationType = "ADMIN_ANNOUNCEMENT",
        relatedEntityType = "ANNOUNCEMENT",
        deepLink = "notifications",
        title = "Platform Announcement",
        message = "New platform updates are available.",
        isRead = false,
        createdAt = "2026-11-14T08:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionManager = AccountSessionManager()
        sessionManager.updateTestSession(10L, "ROLE_USER")
        every { analyticsRepository.notificationUpdates } returns emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsNotificationsAndUnreadCountOnStart_andGroupsByDate() = runTest {
        coEvery { analyticsRepository.getNotifications(null, 0, NotificationsViewModel.PAGE_SIZE) } returns
            Result.success(listOf(workoutNotif, programNotif, coachRequestNotif))
        coEvery { analyticsRepository.getUnreadNotificationsCount() } returns Result.success(2L)

        val viewModel = NotificationsViewModel(analyticsRepository, sessionManager)
        viewModel.todayProvider = { sampleToday }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(3, state.notifications.size)
        assertEquals(2L, state.unreadCount)

        val grouped = viewModel.groupedNotifications(ZoneOffset.UTC)
        assertEquals(listOf(workoutNotif), grouped[NotificationDateGroup.TODAY])
        assertEquals(listOf(programNotif), grouped[NotificationDateGroup.YESTERDAY])
        assertEquals(listOf(coachRequestNotif), grouped[NotificationDateGroup.EARLIER])
    }

    @Test
    fun switchingFilters_fetchesUnreadOnlyWhenUnreadSelected() = runTest {
        coEvery { analyticsRepository.getNotifications(null, 0, NotificationsViewModel.PAGE_SIZE) } returns
            Result.success(listOf(workoutNotif, coachRequestNotif))
        coEvery { analyticsRepository.getNotifications(true, 0, NotificationsViewModel.PAGE_SIZE) } returns
            Result.success(listOf(workoutNotif))
        coEvery { analyticsRepository.getUnreadNotificationsCount() } returns Result.success(1L)

        val viewModel = NotificationsViewModel(analyticsRepository, sessionManager)
        advanceUntilIdle()

        viewModel.setFilter(NotificationFilter.UNREAD)
        advanceUntilIdle()

        assertEquals(NotificationFilter.UNREAD, viewModel.uiState.value.filter)
        assertEquals(listOf(workoutNotif), viewModel.uiState.value.notifications)
        coVerify { analyticsRepository.getNotifications(true, 0, NotificationsViewModel.PAGE_SIZE) }
    }

    @Test
    fun markAsRead_updatesItemReadStateAndDecrementsUnreadCount() = runTest {
        coEvery { analyticsRepository.getNotifications(null, 0, NotificationsViewModel.PAGE_SIZE) } returns
            Result.success(listOf(workoutNotif, programNotif))
        coEvery { analyticsRepository.getUnreadNotificationsCount() } returns Result.success(2L)
        coEvery { analyticsRepository.markNotificationAsRead(1L) } returns Result.success(Unit)

        val viewModel = NotificationsViewModel(analyticsRepository, sessionManager)
        advanceUntilIdle()

        viewModel.markAsRead(1L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.first { it.id == 1L }.isRead)
        assertEquals(1L, viewModel.uiState.value.unreadCount)
        coVerify(exactly = 1) { analyticsRepository.markNotificationAsRead(1L) }
    }

    @Test
    fun markAllAsRead_setsAllReadAndResetsUnreadCountToZero() = runTest {
        coEvery { analyticsRepository.getNotifications(null, 0, NotificationsViewModel.PAGE_SIZE) } returns
            Result.success(listOf(workoutNotif, programNotif))
        coEvery { analyticsRepository.getUnreadNotificationsCount() } returns Result.success(2L)
        coEvery { analyticsRepository.markAllNotificationsAsRead() } returns Result.success(Unit)

        val viewModel = NotificationsViewModel(analyticsRepository, sessionManager)
        advanceUntilIdle()

        viewModel.markAllAsRead()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notifications.all { it.isRead })
        assertEquals(0L, viewModel.uiState.value.unreadCount)
        coVerify(exactly = 1) { analyticsRepository.markAllNotificationsAsRead() }
    }

    @Test
    fun loadMoreNotifications_appendsNextPageWhenHasMoreIsTrue() = runTest {
        val page0 = (1L..20L).map { idx ->
            workoutNotif.copy(id = idx, title = "Item $idx")
        }
        val page1 = listOf(programNotif.copy(id = 21L))
        coEvery { analyticsRepository.getNotifications(null, 0, NotificationsViewModel.PAGE_SIZE) } returns
            Result.success(page0)
        coEvery { analyticsRepository.getNotifications(null, 1, NotificationsViewModel.PAGE_SIZE) } returns
            Result.success(page1)
        coEvery { analyticsRepository.getUnreadNotificationsCount() } returns Result.success(21L)

        val viewModel = NotificationsViewModel(analyticsRepository, sessionManager)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasMore)
        assertEquals(20, viewModel.uiState.value.notifications.size)

        viewModel.loadMoreNotifications()
        advanceUntilIdle()

        assertEquals(21, viewModel.uiState.value.notifications.size)
        assertFalse(viewModel.uiState.value.hasMore)
        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun tappingNotifications_marksUnreadAsRead_andNavigatesToCorrectDestinations() = runTest {
        coEvery { analyticsRepository.getNotifications(null, 0, NotificationsViewModel.PAGE_SIZE) } returns
            Result.success(listOf(workoutNotif, programNotif, coachRequestNotif, announcementNotif))
        coEvery { analyticsRepository.getUnreadNotificationsCount() } returns Result.success(3L)
        coEvery { analyticsRepository.markNotificationAsRead(any()) } returns Result.success(Unit)

        val viewModel = NotificationsViewModel(analyticsRepository, sessionManager)
        advanceUntilIdle()

        val targets = mutableListOf<NotificationNavigationTarget>()
        viewModel.onNotificationTapped(workoutNotif) { targets.add(it) }
        viewModel.onNotificationTapped(programNotif) { targets.add(it) }
        viewModel.onNotificationTapped(coachRequestNotif) { targets.add(it) }
        viewModel.onNotificationTapped(announcementNotif) { targets.add(it) }
        advanceUntilIdle()

        assertEquals(NotificationNavigationTarget.WorkoutDetails(42L), targets[0])
        assertEquals(NotificationNavigationTarget.ProgramAvailable(7L), targets[1])
        assertEquals(NotificationNavigationTarget.CoachRelationships, targets[2])
        assertTrue(targets[3] is NotificationNavigationTarget.AnnouncementDetail)
        assertNotNull(viewModel.uiState.value.selectedAnnouncement)

        coVerify(exactly = 1) { analyticsRepository.markNotificationAsRead(1L) }
        coVerify(exactly = 1) { analyticsRepository.markNotificationAsRead(2L) }
        coVerify(exactly = 0) { analyticsRepository.markNotificationAsRead(3L) } // already read
        coVerify(exactly = 1) { analyticsRepository.markNotificationAsRead(4L) }

        viewModel.dismissAnnouncementDetail()
        assertNull(viewModel.uiState.value.selectedAnnouncement)
    }

    @Test
    fun sessionSwitch_clearsPreviousUsersNotificationsAndReloads() = runTest {
        val user10Notif = workoutNotif.copy(id = 10L, recipientUserId = 10L)
        val user20Notif = programNotif.copy(id = 20L, recipientUserId = 20L)

        coEvery { analyticsRepository.getNotifications(null, 0, NotificationsViewModel.PAGE_SIZE) } returnsMany listOf(
            Result.success(listOf(user10Notif)),
            Result.success(listOf(user20Notif))
        )
        coEvery { analyticsRepository.getUnreadNotificationsCount() } returnsMany listOf(
            Result.success(1L),
            Result.success(1L)
        )

        val viewModel = NotificationsViewModel(analyticsRepository, sessionManager)
        advanceUntilIdle()
        assertEquals(listOf(user10Notif), viewModel.uiState.value.notifications)

        sessionManager.updateTestSession(20L, "ROLE_USER")
        advanceUntilIdle()

        assertEquals(listOf(user20Notif), viewModel.uiState.value.notifications)
    }
}
