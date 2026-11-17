package com.calisthenics.ai.ui.chat.ai

import androidx.lifecycle.SavedStateHandle
import com.calisthenics.ai.domain.model.AiActionCard
import com.calisthenics.ai.domain.model.AiConversationDetail
import com.calisthenics.ai.domain.model.AiConversationSummary
import com.calisthenics.ai.domain.model.AiMessage
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.repository.AiChatRepository
import com.calisthenics.ai.domain.repository.ProfileRepository
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
class AiChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAiChatRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAiChatRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        conversationId: Long? = null,
        profileRepository: ProfileRepository? = null
    ): AiChatViewModel {
        val map = mutableMapOf<String, Any>()
        if (conversationId != null) {
            map["conversationId"] = conversationId
        }
        val savedStateHandle = SavedStateHandle(map)
        return AiChatViewModel(
            aiChatRepository = fakeRepository,
            profileRepository = profileRepository,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun loadConversationWithIdPopulatesMessages() = runTest {
        fakeRepository.conversationDetails[10L] = AiConversationDetail(
            id = 10L,
            title = "Planche Training",
            messages = listOf(
                AiMessage(1L, 10L, "user", "How to planche?", null, null, null, null),
                AiMessage(2L, 10L, "assistant", "Start with planche leans.", "GENERAL_CHAT", null, null, null)
            ),
            createdAt = "2026-11-12T00:00:00Z",
            updatedAt = "2026-11-12T00:00:00Z"
        )

        val vm = createViewModel(conversationId = 10L)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(10L, state.currentConversationId)
        assertEquals("Planche Training", state.conversationTitle)
        assertEquals(2, state.messages.size)
        assertEquals("user", state.messages[0].role)
        assertEquals("assistant", state.messages[1].role)
    }

    @Test
    fun sendMessageOptimisticallyAppendsThenReceivesAssistantReplyWithCard() = runTest {
        fakeRepository.conversationDetails[10L] = AiConversationDetail(
            id = 10L,
            title = "PR Tracking",
            messages = emptyList(),
            createdAt = null,
            updatedAt = null
        )

        val vm = createViewModel(conversationId = 10L)
        advanceUntilIdle()

        fakeRepository.nextSendMessageResult = Result.success(
            AiMessage(
                id = 102L,
                conversationId = 10L,
                role = "assistant",
                content = "Recorded your PR!",
                intent = "PR_RECORD",
                metadata = null,
                actionCard = AiActionCard.PrRecorded(
                    prId = 55L,
                    exerciseName = "Pull-up",
                    recordType = "MAX_REPS",
                    value = 20.0,
                    unit = "reps",
                    formattedValue = "20 reps",
                    previousValue = 18.0,
                    improvementPercentage = 11.1
                ),
                createdAt = null
            )
        )

        vm.sendMessage("I just did 20 pull-ups")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSending)
        assertEquals(2, state.messages.size)
        assertEquals("user", state.messages[0].role)
        assertEquals("I just did 20 pull-ups", state.messages[0].content)
        assertEquals("assistant", state.messages[1].role)
        assertEquals("Recorded your PR!", state.messages[1].content)
        assertNotNull(state.messages[1].actionCard)
        assertTrue(state.messages[1].actionCard is AiActionCard.PrRecorded)
    }

    @Test
    fun sendMessageFailureShowsError() = runTest {
        val vm = createViewModel(conversationId = 10L)
        advanceUntilIdle()

        fakeRepository.nextSendMessageResult = Result.failure(Exception("Network unreachable"))

        vm.sendMessage("Hello?")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSending)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Network unreachable"))
    }

    @Test
    fun startNewConversationResetsCurrentMessages() = runTest {
        val vm = createViewModel(conversationId = 10L)
        advanceUntilIdle()

        vm.startNewConversation()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(999L, state.currentConversationId)
        assertEquals("New Conversation", state.conversationTitle)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun deleteConversationRemovesFromList() = runTest {
        fakeRepository.conversations.add(
            AiConversationSummary(
                id = 10L,
                title = "Old Routine",
                lastMessagePreview = "Goodbye",
                lastMessageRole = "assistant",
                lastInteractionAt = null,
                createdAt = null,
                messageCount = 2
            )
        )

        val vm = createViewModel(conversationId = 10L)
        advanceUntilIdle()

        vm.deleteConversation(10L)
        advanceUntilIdle()

        assertTrue(fakeRepository.deletedConversationIds.contains(10L))
    }

    @Test
    fun sendMessageWithProfileUpdateSuccessRefreshesProfile() = runTest {
        var profileRefreshed = false
        val mockProfileRepo = object : ProfileRepository {
            override suspend fun getMyClientProfile(): Result<ClientProfile> {
                profileRefreshed = true
                return Result.success(
                    ClientProfile(
                        id = 1L,
                        fullName = "Athlete",
                        weightKg = 76.0
                    )
                )
            }
            override suspend fun getMyCoachProfile() = Result.failure<com.calisthenics.ai.domain.model.CoachProfile>(NotImplementedError())
            override suspend fun updateMyCoachProfile(request: com.calisthenics.ai.data.remote.dto.CoachProfileRequestDto) = Result.failure<com.calisthenics.ai.domain.model.CoachProfile>(NotImplementedError())
            override suspend fun getPublicCoachProfile(id: Long) = Result.failure<com.calisthenics.ai.domain.model.CoachProfile>(NotImplementedError())
            override suspend fun updateMyClientProfile(request: com.calisthenics.ai.data.remote.dto.ClientProfileRequestDto) = Result.failure<ClientProfile>(NotImplementedError())
            override suspend fun getClientProfile(id: Long) = Result.failure<ClientProfile>(NotImplementedError())
            override suspend fun getClientOverview(id: Long) = Result.failure<com.calisthenics.ai.domain.model.ClientTrainingOverview>(NotImplementedError())
            override suspend fun getMyClientOverview() = Result.failure<com.calisthenics.ai.domain.model.ClientTrainingOverview>(NotImplementedError())
            override suspend fun uploadAvatar(file: java.io.File) = Result.failure<String>(NotImplementedError())
            override suspend fun getProfileMedia(userId: Long, profileType: com.calisthenics.ai.domain.model.ProfileType) = Result.failure<List<com.calisthenics.ai.domain.model.ProfileMediaItem>>(NotImplementedError())
            override suspend fun uploadProfileMedia(profileType: com.calisthenics.ai.domain.model.ProfileType, file: java.io.File, thumbnail: java.io.File?, mimeType: String) = Result.failure<com.calisthenics.ai.domain.model.ProfileMediaItem>(NotImplementedError())
            override suspend fun deleteProfileMedia(id: Long) = Result.failure<Unit>(NotImplementedError())
            override suspend fun reorderProfileMedia(profileType: com.calisthenics.ai.domain.model.ProfileType, mediaIds: List<Long>) = Result.failure<List<com.calisthenics.ai.domain.model.ProfileMediaItem>>(NotImplementedError())
            override suspend fun setProfileMediaCover(id: Long) = Result.failure<List<com.calisthenics.ai.domain.model.ProfileMediaItem>>(NotImplementedError())
        }

        val vm = createViewModel(conversationId = 10L, profileRepository = mockProfileRepo)
        advanceUntilIdle()

        fakeRepository.nextSendMessageResult = Result.success(
            AiMessage(
                id = 200L,
                conversationId = 10L,
                role = "assistant",
                content = "Cân nặng đã cập nhật thành 76 kg.",
                intent = "PROFILE_UPDATE",
                metadata = null,
                actionCard = AiActionCard.ProfileUpdate(
                    success = true,
                    updatedFields = mapOf("weightKg" to "76.0"),
                    weightKg = 76.0
                ),
                createdAt = null
            )
        )

        vm.sendMessage("Cập nhật cân nặng của tôi thành 76kg.")
        advanceUntilIdle()

        assertTrue(profileRefreshed)
        val state = vm.uiState.value
        assertEquals(2, state.messages.size)
        assertTrue(state.messages[1].actionCard is AiActionCard.ProfileUpdate)
    }
}

private class FakeAiChatRepository : AiChatRepository {
    val conversations = mutableListOf<AiConversationSummary>()
    val conversationDetails = mutableMapOf<Long, AiConversationDetail>()
    val deletedConversationIds = mutableListOf<Long>()
    var nextSendMessageResult: Result<AiMessage>? = null

    override suspend fun getConversations(): Result<List<AiConversationSummary>> {
        return Result.success(conversations)
    }

    override suspend fun getLatestConversationSummary(): Result<AiConversationSummary?> {
        return Result.success(conversations.firstOrNull())
    }

    override suspend fun createConversation(title: String?): Result<AiConversationSummary> {
        val summary = AiConversationSummary(
            id = 999L,
            title = title ?: "New Conversation",
            lastMessagePreview = "No messages yet",
            lastMessageRole = null,
            lastInteractionAt = null,
            createdAt = null,
            messageCount = 0
        )
        conversations.add(summary)
        return Result.success(summary)
    }

    override suspend fun getConversation(id: Long): Result<AiConversationDetail> {
        val detail = conversationDetails[id] ?: AiConversationDetail(
            id = id,
            title = "Conversation #$id",
            messages = emptyList(),
            createdAt = null,
            updatedAt = null
        )
        return Result.success(detail)
    }

    override suspend fun deleteConversation(id: Long): Result<Unit> {
        deletedConversationIds.add(id)
        conversations.removeAll { it.id == id }
        conversationDetails.remove(id)
        return Result.success(Unit)
    }

    override suspend fun sendMessage(
        conversationId: Long,
        message: String,
        timezone: String?
    ): Result<AiMessage> {
        return nextSendMessageResult ?: Result.success(
            AiMessage(
                id = 1000L,
                conversationId = conversationId,
                role = "assistant",
                content = "Default assistant response",
                intent = "GENERAL_CHAT",
                metadata = null,
                createdAt = null
            )
        )
    }

    override suspend fun sendQuickMessage(message: String, timezone: String?): Result<AiMessage> {
        return sendMessage(10L, message, timezone)
    }
}
