package com.example.realtimechatapplicaition.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.realtimechatapplicaition.data.model.Chat
import com.example.realtimechatapplicaition.data.model.Message
import com.example.realtimechatapplicaition.data.model.MessageStatus
import com.example.realtimechatapplicaition.data.remote.ApiService
import com.example.realtimechatapplicaition.data.remote.ChatSocketService
import com.example.realtimechatapplicaition.data.remote.ConnectionState
import com.example.realtimechatapplicaition.data.repository.ChatRepository
import com.example.realtimechatapplicaition.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class ChatDetailViewModel(
    private val chatId: String,
    private val chatRepository: ChatRepository,
    private val socketService: ChatSocketService,
    private val apiService: ApiService,
    private val username: String,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    companion object {
        private const val TAG = "ChatDetailViewModel"
    }

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState

    private val _events = MutableSharedFlow<ChatDetailEvent>()
    val events: SharedFlow<ChatDetailEvent> = _events

    private var isRoomJoined = false
    private var hasShownOfflineToast = false
    private var hasSyncedFromServer = false

    init {
        Log.d(TAG, "Init - chatId: $chatId, username: $username")
        chatRepository.selectChat(chatId)
        observeCurrentChat()
        observeMessages()
        observeSocketEvents()
        observeNetworkState()
        loadMessagesFromDbAndSync()
        connectAndJoinRoom()
    }

    private fun observeCurrentChat() {
        viewModelScope.launch {
            chatRepository.getChatById(chatId)?.let { chat ->
                _uiState.value = _uiState.value.copy(chat = chat)
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.getMessagesForChat(chatId).collect { messages ->
                Log.d(TAG, "Messages updated from DB: ${messages.size}")
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                Log.d(TAG, "Network state changed: isOnline=$isOnline")
                _uiState.value = _uiState.value.copy(isOffline = !isOnline)

                if (isOnline) {
                    hasShownOfflineToast = false
                    // Reconnect socket if not connected
                    if (!socketService.isConnected()) {
                        Log.d(TAG, "Network online, reconnecting socket...")
                        socketService.connect(username)
                    }
                }
            }
        }
    }

    private fun loadMessagesFromDbAndSync() {
        viewModelScope.launch {
            Log.d(TAG, "Loading messages from DB first for room: $chatId")
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Messages will be loaded automatically via the Flow in observeMessages()
            // Now sync from server if online
            if (networkMonitor.isCurrentlyConnected() && !hasSyncedFromServer) {
                syncMessagesFromServer()
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun syncMessagesFromServer() {
        Log.d(TAG, "Syncing messages from server for room: $chatId")

        val result = apiService.getRoomMessages(chatId, username)
        result.onSuccess { messageResponses ->
            Log.d(TAG, "Loaded ${messageResponses.size} messages from server")
            hasSyncedFromServer = true

            val serverMessages = messageResponses.map { response ->
                val isFromCurrentUser = response.senderUsername == username
                Message(
                    id = response.id,
                    chatId = response.roomId,
                    content = response.text,
                    timestamp = parseTimestamp(response.createdAt),
                    isFromUser = isFromCurrentUser,
                    status = MessageStatus.DELIVERED,
                    senderUsername = response.senderUsername
                )
            }

            // Save to database (will merge with existing)
            chatRepository.syncMessagesFromServer(chatId, serverMessages)

            if (messageResponses.isNotEmpty()) {
                val lastMsg = messageResponses.last()
                chatRepository.updateLastMessage(
                    chatId,
                    lastMsg.text,
                    parseTimestamp(lastMsg.createdAt)
                )
            }

        }.onFailure { exception ->
            Log.e(TAG, "Failed to sync messages from server: ${exception.message}")
            // Don't show error - we still have local messages
        }
    }

    private fun retryUnsentMessages() {
        viewModelScope.launch {
            val unsentMessages = chatRepository.getUnsentMessagesForChat(chatId)
            Log.d(TAG, "Retrying ${unsentMessages.size} unsent messages")

            for (message in unsentMessages) {
                if (socketService.isConnected() && isRoomJoined) {
                    socketService.sendMessage(chatId, message.content)
                    // The message will be marked as sent when we receive it back from server
                    Log.d(TAG, "Retried sending message: ${message.id}")
                }
            }
        }
    }

    private fun observeSocketEvents() {
        // Connection state
        viewModelScope.launch {
            socketService.connectionState.collect { state ->
                Log.d(TAG, "Connection state changed: $state")

                _uiState.value = _uiState.value.copy(
                    isConnecting = state is ConnectionState.Connecting,
                    isConnected = state is ConnectionState.Connected
                )

                when (state) {
                    is ConnectionState.Connected -> {
                        Log.d(TAG, "Connected, joining room: $chatId")
                        // Always try to join room when connected (reconnection case)
                        isRoomJoined = false
                        socketService.joinRoom(chatId)
                    }
                    is ConnectionState.Disconnected -> {
                        Log.d(TAG, "Disconnected from socket")
                        isRoomJoined = false
                    }
                    is ConnectionState.Error -> {
                        Log.e(TAG, "Connection error: ${state.message}")
                        isRoomJoined = false
                        // Don't show error popup for connection issues when offline
                        if (networkMonitor.isCurrentlyConnected()) {
                            _events.emit(ChatDetailEvent.ShowError(state.message))
                        }
                    }
                    else -> {}
                }
            }
        }

        // Joined room
        viewModelScope.launch {
            socketService.joinedRoom.collect { response ->
                Log.d(TAG, "Joined room response: ${response.roomId}")
                if (response.roomId == chatId) {
                    isRoomJoined = true
                    _uiState.value = _uiState.value.copy(isRoomJoined = true)
                    Log.d(TAG, "Successfully joined room: $chatId")

                    // Retry unsent messages after joining room
                    retryUnsentMessages()
                }
            }
        }

        // Incoming messages
        viewModelScope.launch {
            socketService.newMessage.collect { response ->
                Log.d(TAG, "New message received - room: ${response.roomId}, sender: ${response.senderUsername}")

                if (response.roomId == chatId) {
                    val isFromCurrentUser = response.senderUsername == username
                    val messageTimestamp = parseTimestamp(response.createdAt)

                    if (isFromCurrentUser) {
                        // This is our own message echoed back from server
                        // Find and update any matching unsent message instead of creating duplicate
                        val unsentMessages = chatRepository.getUnsentMessagesForChat(chatId)
                        val matchingMessage = unsentMessages.find { it.content == response.text }
                        if (matchingMessage != null) {
                            // Mark existing local message as sent - don't create new one
                            chatRepository.markMessageAsSent(matchingMessage.id, response.id)
                            Log.d(TAG, "Marked local message as sent: ${matchingMessage.id}")
                        } else {
                            // No matching unsent message, this might be from another device
                            // Only save if it doesn't exist
                            chatRepository.saveMessageFromServer(
                                id = response.id,
                                chatId = response.roomId,
                                content = response.text,
                                timestamp = messageTimestamp,
                                isFromUser = isFromCurrentUser,
                                senderUsername = response.senderUsername
                            )
                        }
                    } else {
                        // Message from other user - save to database
                        chatRepository.saveMessageFromServer(
                            id = response.id,
                            chatId = response.roomId,
                            content = response.text,
                            timestamp = messageTimestamp,
                            isFromUser = isFromCurrentUser,
                            senderUsername = response.senderUsername
                        )
                    }

                    _events.emit(ChatDetailEvent.MessageSent)
                    chatRepository.updateLastMessage(chatId, response.text, messageTimestamp)
                }
            }
        }

        // Errors - only show if online
        viewModelScope.launch {
            socketService.error.collect { errorMessage ->
                Log.e(TAG, "Socket error: $errorMessage")
                if (networkMonitor.isCurrentlyConnected()) {
                    _events.emit(ChatDetailEvent.ShowError(errorMessage))
                }
            }
        }
    }

    private fun connectAndJoinRoom() {
        Log.d(TAG, "connectAndJoinRoom - isConnected: ${socketService.isConnected()}")
        if (!socketService.isConnected()) {
            Log.d(TAG, "Connecting to socket with username: $username")
            socketService.connect(username)
        } else {
            socketService.joinRoom(chatId)
        }
    }

    fun sendMessage(content: String) {
        Log.d(TAG, "sendMessage called with: $content")

        if (content.isBlank()) return

        val trimmed = content.trim()

        if (!trimmed.startsWith("@AI")) {
            viewModelScope.launch {
                _events.emit(ChatDetailEvent.ShowError("Message must start with @AI"))
            }
            return
        }

        viewModelScope.launch {
            // Always save message to database first with isSentToServer = false
            val localMessage = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                content = trimmed,
                timestamp = System.currentTimeMillis(),
                isFromUser = true,
                status = MessageStatus.QUEUED,
                senderUsername = username
            )

            // Save to database immediately
            chatRepository.saveMessage(localMessage)
            Log.d(TAG, "Message saved to DB: ${localMessage.id}")

            // Check if we can send now
            val isOffline = !networkMonitor.isCurrentlyConnected()
            val isDisconnected = !socketService.isConnected()

            if (isOffline || isDisconnected || !isRoomJoined) {
                // Show offline toast only once
                if (!hasShownOfflineToast) {
                    hasShownOfflineToast = true
                    _events.emit(ChatDetailEvent.ShowOfflineToast)
                }
                Log.d(TAG, "Message queued for later: ${localMessage.id}")
            } else {
                // Send via socket
                socketService.sendMessage(chatId, trimmed)
                Log.d(TAG, "Message sent via socket: ${localMessage.id}")
            }

            _events.emit(ChatDetailEvent.MessageSent)
        }
    }

    fun retryMessage(message: Message) {
        viewModelScope.launch {
            if (socketService.isConnected() && isRoomJoined) {
                socketService.sendMessage(chatId, message.content)
                Log.d(TAG, "Retrying message: ${message.id}")
            } else {
                _events.emit(ChatDetailEvent.ShowError("Not connected to server"))
            }
        }
    }

    private fun parseTimestamp(createdAt: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(createdAt)?.time ?: System.currentTimeMillis()
        } catch (e1: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(createdAt)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    format.parse(createdAt)?.time ?: System.currentTimeMillis()
                } catch (e3: Exception) {
                    Log.e(TAG, "Failed to parse timestamp: $createdAt", e3)
                    System.currentTimeMillis()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.clearCurrentChat()
    }

    class Factory(
        private val chatId: String,
        private val chatRepository: ChatRepository,
        private val socketService: ChatSocketService,
        private val apiService: ApiService,
        private val username: String,
        private val networkMonitor: NetworkMonitor
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatDetailViewModel(
                chatId,
                chatRepository,
                socketService,
                apiService,
                username,
                networkMonitor
            ) as T
        }
    }
}

data class ChatDetailUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isRoomJoined: Boolean = false,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false
)

sealed class ChatDetailEvent {
    data object MessageSent : ChatDetailEvent()
    data object ShowOfflineToast : ChatDetailEvent()
    data class ShowError(val message: String) : ChatDetailEvent()
}
