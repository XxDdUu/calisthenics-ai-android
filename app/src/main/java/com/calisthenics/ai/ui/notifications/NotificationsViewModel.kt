package com.calisthenics.ai.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.data.remote.api.NetworkErrorParser
import com.calisthenics.ai.data.remote.dto.NotificationDto
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import javax.inject.Inject

enum class NotificationFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread")
}

enum class NotificationDateGroup(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    EARLIER("Earlier")
}

sealed interface NotificationNavigationTarget {
    data class WorkoutDetails(val planId: Long) : NotificationNavigationTarget
    data class ProgramAvailable(val programId: Long) : NotificationNavigationTarget
    data object CoachRelationships : NotificationNavigationTarget
    data class AnnouncementDetail(val notification: NotificationDto) : NotificationNavigationTarget
    data object PersonalRecords : NotificationNavigationTarget
    data object None : NotificationNavigationTarget
}

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMarkingAll: Boolean = false,
    val filter: NotificationFilter = NotificationFilter.ALL,
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount: Long = 0L,
    val error: String? = null,
    val paginationError: String? = null,
    val hasMore: Boolean = false,
    val currentPage: Int = 0,
    val selectedAnnouncement: NotificationDto? = null
) {
    val groupedNotifications: Map<NotificationDateGroup, List<NotificationDto>>
        get() = NotificationsViewModel.groupNotificationsByDate(notifications)
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 20

        fun parseCreatedDateTime(
            createdAt: String,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): ZonedDateTime? {
            val raw = createdAt.trim()
            if (raw.isEmpty()) return null
            return runCatching { ZonedDateTime.parse(raw).withZoneSameInstant(zoneId) }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(zoneId) }.getOrNull()
                ?: runCatching { Instant.parse(raw).atZone(zoneId) }.getOrNull()
                ?: runCatching { LocalDateTime.parse(raw).atZone(zoneId) }.getOrNull()
                ?: runCatching { LocalDate.parse(raw).atStartOfDay(zoneId) }.getOrNull()
        }

        fun resolveDateGroup(
            createdAt: String,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): NotificationDateGroup {
            val localDate = parseCreatedDateTime(createdAt, zoneId)?.toLocalDate()
                ?: return NotificationDateGroup.EARLIER
            return when {
                localDate.isEqual(today) -> NotificationDateGroup.TODAY
                localDate.isEqual(today.minusDays(1)) -> NotificationDateGroup.YESTERDAY
                else -> NotificationDateGroup.EARLIER
            }
        }

        fun groupNotificationsByDate(
            items: List<NotificationDto>,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Map<NotificationDateGroup, List<NotificationDto>> {
            if (items.isEmpty()) return emptyMap()
            val grouped = linkedMapOf<NotificationDateGroup, MutableList<NotificationDto>>()
            for (group in NotificationDateGroup.entries) {
                grouped[group] = mutableListOf()
            }
            for (item in items) {
                grouped.getValue(resolveDateGroup(item.createdAt, today, zoneId)).add(item)
            }
            return grouped.filterValues { it.isNotEmpty() }
        }

        fun resolveNavigationTarget(notification: NotificationDto): NotificationNavigationTarget {
            val normalizedType = notification.resolvedType.trim().uppercase(Locale.US)
            val deepLink = notification.deepLink.orEmpty().trim()
            val entityId = notification.resolvedEntityId

            return when (normalizedType) {
                "WORKOUT_ASSIGNED" -> {
                    val planId = entityId?.takeIf { it > 0L }
                        ?: parseTrailingLongId(deepLink, "workout_plan_details/")
                    if (planId != null && planId > 0L) {
                        NotificationNavigationTarget.WorkoutDetails(planId)
                    } else {
                        NotificationNavigationTarget.None
                    }
                }
                "PROGRAM_AVAILABLE" -> {
                    val programId = entityId?.takeIf { it > 0L }
                        ?: parseTrailingLongId(deepLink, "getting_started/")
                        ?: 0L
                    NotificationNavigationTarget.ProgramAvailable(programId)
                }
                "COACH_CLIENT_REQUEST" -> NotificationNavigationTarget.CoachRelationships
                "ADMIN_ANNOUNCEMENT" -> NotificationNavigationTarget.AnnouncementDetail(notification)
                "PR_SHARED", "COACH_FEEDBACK" -> NotificationNavigationTarget.PersonalRecords
                else -> resolveFallbackDeepLink(deepLink, notification)
            }
        }

        private fun resolveFallbackDeepLink(
            deepLink: String,
            notification: NotificationDto
        ): NotificationNavigationTarget {
            if (deepLink.startsWith("workout_plan_details/")) {
                val planId = parseTrailingLongId(deepLink, "workout_plan_details/")
                if (planId != null && planId > 0L) {
                    return NotificationNavigationTarget.WorkoutDetails(planId)
                }
            }
            if (deepLink.startsWith("getting_started")) {
                val programId = parseTrailingLongId(deepLink, "getting_started/") ?: 0L
                return NotificationNavigationTarget.ProgramAvailable(programId)
            }
            if (deepLink.equals("coach", ignoreCase = true)) {
                return NotificationNavigationTarget.CoachRelationships
            }
            if (deepLink.equals("personal_records", ignoreCase = true)) {
                return NotificationNavigationTarget.PersonalRecords
            }
            if (notification.resolvedType.contains("ANNOUNCEMENT", ignoreCase = true)) {
                return NotificationNavigationTarget.AnnouncementDetail(notification)
            }
            return NotificationNavigationTarget.None
        }

        private fun parseTrailingLongId(deepLink: String, prefix: String): Long? {
            if (!deepLink.startsWith(prefix)) return null
            return deepLink.removePrefix(prefix)
                .substringBefore("/")
                .substringBefore("?")
                .toLongOrNull()
        }
    }

    internal var todayProvider: () -> LocalDate = { LocalDate.now() }

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = { _uiState.value = NotificationsUiState() },
        onSessionActive = { loadNotifications(resetPage = true) }
    )

    init {
        sessionGuard.startObserving()
        loadNotifications(resetPage = true)
    }

    fun groupedNotifications(zoneId: ZoneId = ZoneId.systemDefault()): Map<NotificationDateGroup, List<NotificationDto>> {
        return groupNotificationsByDate(_uiState.value.notifications, todayProvider(), zoneId)
    }

    fun setFilter(filter: NotificationFilter) {
        if (_uiState.value.filter == filter && _uiState.value.error == null) return
        _uiState.update {
            it.copy(
                filter = filter,
                error = null,
                paginationError = null
            )
        }
        loadNotifications(resetPage = true)
    }

    fun refresh() {
        loadNotifications(resetPage = true, isPullRefresh = true)
    }

    fun loadNotifications(resetPage: Boolean = true, isPullRefresh: Boolean = false) {
        val currentState = _uiState.value
        val filter = currentState.filter
        val unreadOnly = filter == NotificationFilter.UNREAD
        if (resetPage && currentState.isLoading && !isPullRefresh) return

        sessionGuard.launchScoped { captured ->
            _uiState.update {
                it.copy(
                    isLoading = resetPage && !isPullRefresh && it.notifications.isEmpty(),
                    isRefreshing = isPullRefresh,
                    error = if (resetPage) null else it.error,
                    paginationError = null
                )
            }

            val itemsResult = analyticsRepository.getNotifications(
                unreadOnly = if (unreadOnly) true else null,
                page = 0,
                size = PAGE_SIZE
            )
            val countResult = analyticsRepository.getUnreadNotificationsCount()

            if (!sessionGuard.isCurrentSession(captured)) return@launchScoped

            itemsResult
                .onSuccess { fetched ->
                    val unreadCount = countResult.getOrNull() ?: if (unreadOnly) {
                        fetched.size.toLong()
                    } else {
                        fetched.count { !it.isRead }.toLong()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = fetched,
                            unreadCount = unreadCount.coerceAtLeast(0L),
                            hasMore = fetched.size >= PAGE_SIZE,
                            currentPage = 0,
                            error = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = NetworkErrorParser.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    fun loadMoreNotifications() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        val nextPage = state.currentPage + 1
        val unreadOnly = if (state.filter == NotificationFilter.UNREAD) true else null

        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoadingMore = true, paginationError = null) }
            analyticsRepository.getNotifications(
                unreadOnly = unreadOnly,
                page = nextPage,
                size = PAGE_SIZE
            )
                .onSuccess { pageItems ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { current ->
                        val merged = (current.notifications + pageItems).distinctBy { it.id }
                        current.copy(
                            isLoadingMore = false,
                            notifications = merged,
                            currentPage = nextPage,
                            hasMore = pageItems.size >= PAGE_SIZE,
                            paginationError = null
                        )
                    }
                }
                .onFailure { throwable ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            paginationError = NetworkErrorParser.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    fun markAsRead(notificationId: Long) {
        val existing = _uiState.value.notifications.firstOrNull { it.id == notificationId }
        val wasUnread = existing?.isRead == false
        val previousNotifications = _uiState.value.notifications
        val previousUnreadCount = _uiState.value.unreadCount

        if (wasUnread) {
            _uiState.update { state ->
                val updatedList = if (state.filter == NotificationFilter.UNREAD) {
                    state.notifications.filterNot { it.id == notificationId }
                } else {
                    state.notifications.map { item ->
                        if (item.id == notificationId) item.copy(isRead = true) else item
                    }
                }
                state.copy(
                    notifications = updatedList,
                    unreadCount = (state.unreadCount - 1L).coerceAtLeast(0L)
                )
            }
        }

        sessionGuard.launchScoped { captured ->
            analyticsRepository.markNotificationAsRead(notificationId)
                .onFailure { throwable ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    if (wasUnread) {
                        _uiState.update {
                            it.copy(
                                notifications = previousNotifications,
                                unreadCount = previousUnreadCount,
                                error = NetworkErrorParser.toUserMessage(throwable)
                            )
                        }
                    }
                }
        }
    }

    fun markAllAsRead() {
        val current = _uiState.value
        if (current.isMarkingAll) return
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isMarkingAll = true, error = null) }
            analyticsRepository.markAllNotificationsAsRead()
                .onSuccess {
                    if (!sessionGuard.isCurrentSession(captured)) return@onSuccess
                    _uiState.update { state ->
                        val updatedList = if (state.filter == NotificationFilter.UNREAD) {
                            emptyList()
                        } else {
                            state.notifications.map { it.copy(isRead = true) }
                        }
                        state.copy(
                            isMarkingAll = false,
                            notifications = updatedList,
                            unreadCount = 0L,
                            hasMore = if (state.filter == NotificationFilter.UNREAD) false else state.hasMore
                        )
                    }
                }
                .onFailure { throwable ->
                    if (!sessionGuard.isCurrentSession(captured)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isMarkingAll = false,
                            error = NetworkErrorParser.toUserMessage(throwable)
                        )
                    }
                }
        }
    }

    fun onNotificationTapped(
        notification: NotificationDto,
        onNavigate: (NotificationNavigationTarget) -> Unit
    ) {
        if (!notification.isRead) {
            markAsRead(notification.id)
        }
        val target = resolveNavigationTarget(notification)
        if (target is NotificationNavigationTarget.AnnouncementDetail) {
            _uiState.update {
                val readItem = target.notification.copy(isRead = true)
                it.copy(selectedAnnouncement = readItem)
            }
        }
        onNavigate(target)
    }

    fun dismissAnnouncementDetail() {
        _uiState.update { it.copy(selectedAnnouncement = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, paginationError = null) }
    }
}
