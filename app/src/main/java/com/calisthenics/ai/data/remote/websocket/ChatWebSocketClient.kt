package com.calisthenics.ai.data.remote.websocket

import android.util.Log
import com.calisthenics.ai.BuildConfig
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.remote.dto.ChatMessageDto
import com.calisthenics.ai.data.remote.dto.UserPresenceDto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class WebSocketState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

@Singleton
class ChatWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionState = MutableStateFlow(WebSocketState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ChatMessageDto>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ChatMessageDto> = _incomingMessages.asSharedFlow()

    private val _presenceUpdates = MutableSharedFlow<UserPresenceDto>(extraBufferCapacity = 64)
    val presenceUpdates: SharedFlow<UserPresenceDto> = _presenceUpdates.asSharedFlow()

    private var webSocket: WebSocket? = null
    private val isExplicitDisconnect = AtomicBoolean(false)
    private val subIdGenerator = AtomicInteger(1)

    // Maps destination -> subscriptionId
    private val activeSubscriptions = ConcurrentHashMap<String, String>()
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null

    fun connect() {
        if (_connectionState.value == WebSocketState.CONNECTED ||
            _connectionState.value == WebSocketState.CONNECTING
        ) {
            return
        }

        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            _connectionState.value = WebSocketState.DISCONNECTED
            return
        }

        isExplicitDisconnect.set(false)
        _connectionState.value = WebSocketState.CONNECTING

        val wsUrl = buildWsUrl(token)
        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener(token))
    }

    fun disconnect() {
        isExplicitDisconnect.set(true)
        reconnectJob?.cancel()
        reconnectJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionState.value = WebSocketState.DISCONNECTED
        activeSubscriptions.clear()
    }

    fun subscribeToConversation(conversationId: Long) {
        val destination = "/topic/conversations/$conversationId"
        subscribe(destination)
    }

    fun unsubscribeFromConversation(conversationId: Long) {
        val destination = "/topic/conversations/$conversationId"
        unsubscribe(destination)
    }

    fun subscribeToUserQueue() {
        subscribe("/user/queue/messages")
    }

    fun subscribeToPresence() {
        subscribe("/user/queue/presence")
    }

    private fun subscribe(destination: String) {
        if (activeSubscriptions.containsKey(destination)) return
        val subId = "sub-" + subIdGenerator.getAndIncrement()
        activeSubscriptions[destination] = subId

        if (_connectionState.value == WebSocketState.CONNECTED) {
            sendStompFrame(
                StompFrame(
                    command = "SUBSCRIBE",
                    headers = mapOf(
                        "id" to subId,
                        "destination" to destination
                    )
                )
            )
        }
    }

    private fun unsubscribe(destination: String) {
        val subId = activeSubscriptions.remove(destination) ?: return
        if (_connectionState.value == WebSocketState.CONNECTED) {
            sendStompFrame(
                StompFrame(
                    command = "UNSUBSCRIBE",
                    headers = mapOf("id" to subId)
                )
            )
        }
    }

    private fun sendStompFrame(frame: StompFrame) {
        webSocket?.send(frame.toWireString())
    }

    private fun resubscribeAll() {
        activeSubscriptions.forEach { (dest, id) ->
            sendStompFrame(
                StompFrame(
                    command = "SUBSCRIBE",
                    headers = mapOf(
                        "id" to id,
                        "destination" to dest
                    )
                )
            )
        }
    }

    private fun createWebSocketListener(token: String): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Send STOMP CONNECT frame
                val connectFrame = StompFrame(
                    command = "CONNECT",
                    headers = mapOf(
                        "accept-version" to "1.1,1.2",
                        "heart-beat" to "10000,10000",
                        "Authorization" to "Bearer $token"
                    )
                )
                webSocket.send(connectFrame.toWireString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val frame = StompFrame.parse(text) ?: return

                when (frame.command) {
                    "CONNECTED" -> {
                        _connectionState.value = WebSocketState.CONNECTED
                        resubscribeAll()
                        startPresenceHeartbeat()
                    }
                    "MESSAGE" -> {
                        val body = frame.body
                        if (body.isNotBlank()) {
                            val presenceSubscriptionId = activeSubscriptions["/user/queue/presence"]
                            if (frame.headers["subscription"] == presenceSubscriptionId) {
                                runCatching {
                                    val presenceDto = gson.fromJson(body, UserPresenceDto::class.java)
                                    if (presenceDto != null) {
                                        _presenceUpdates.tryEmit(presenceDto)
                                    }
                                }.onFailure { e ->
                                    Log.e("ChatWebSocketClient", "Failed to deserialize presence update", e)
                                }
                            } else {
                                runCatching {
                                    val messageDto = gson.fromJson(body, ChatMessageDto::class.java)
                                    if (messageDto != null) {
                                        _incomingMessages.tryEmit(messageDto)
                                    }
                                }.onFailure { e ->
                                    Log.e("ChatWebSocketClient", "Failed to deserialize STOMP message", e)
                                }
                            }
                        }
                    }
                    "ERROR" -> {
                        Log.w("ChatWebSocketClient", "STOMP error frame received")
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                heartbeatJob?.cancel()
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                heartbeatJob?.cancel()
                _connectionState.value = WebSocketState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                heartbeatJob?.cancel()
                _connectionState.value = WebSocketState.ERROR
                scheduleReconnect()
            }
        }
    }

    private fun startPresenceHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value == WebSocketState.CONNECTED) {
                sendStompFrame(
                    StompFrame(
                        command = "SEND",
                        headers = mapOf(
                            "destination" to "/app/presence.heartbeat",
                            "content-length" to "0"
                        )
                    )
                )
                delay(20_000L)
            }
        }
    }

    private fun scheduleReconnect() {
        if (isExplicitDisconnect.get()) return
        if (tokenManager.getAccessToken().isNullOrBlank()) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            if (!isExplicitDisconnect.get() && _connectionState.value != WebSocketState.CONNECTED) {
                connect()
            }
        }
    }

    private fun buildWsUrl(token: String): String {
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        val wsBase = when {
            base.startsWith("https://") -> "wss://" + base.substring(8)
            base.startsWith("http://") -> "ws://" + base.substring(7)
            else -> "ws://$base"
        }
        return "$wsBase/ws?token=$token"
    }
}
