package com.example.realtimechatapplicaition.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.realtimechatapplicaition.data.model.Chat
import com.example.realtimechatapplicaition.data.remote.ApiService
import com.example.realtimechatapplicaition.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val apiService: ApiService,
    private val username: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState

    private val _events = MutableSharedFlow<ChatListEvent>()
    val events: SharedFlow<ChatListEvent> = _events

    init {
        observeChats()
    }

    private fun observeChats() {
        viewModelScope.launch {
            chatRepository.chats.collect { chats ->
                _uiState.value = _uiState.value.copy(
                    chats = chats,
                    isEmpty = chats.isEmpty()
                )
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val chatNumber = _uiState.value.chats.size + 1
            val roomName = "Chat $chatNumber"

            val result = apiService.createRoom(username, roomName)

            result.onSuccess { room ->
                val chat = chatRepository.createChat(room.name, room.id)
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.emit(ChatListEvent.NavigateToChat(chat.id))
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.emit(ChatListEvent.ShowError(exception.message ?: "Failed to create room"))
            }
        }
    }

    fun selectChat(chatId: String) {
        chatRepository.selectChat(chatId)
        viewModelScope.launch {
            _events.emit(ChatListEvent.NavigateToChat(chatId))
        }
    }

    fun deleteChat(position: Int) {
        val chat = _uiState.value.chats.getOrNull(position) ?: return

        viewModelScope.launch {
            val result = apiService.deleteRoom(chat.id, username)

            result.onSuccess {
                chatRepository.deleteChat(chat.id)
                _events.emit(ChatListEvent.ShowSuccess("Room deleted successfully"))
            }.onFailure { exception ->
                _events.emit(ChatListEvent.ShowError(exception.message ?: "Failed to delete room"))
            }
        }
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val apiService: ApiService,
        private val username: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatListViewModel(chatRepository, apiService, username) as T
        }
    }
}

data class ChatListUiState(
    val chats: List<Chat> = emptyList(),
    val isEmpty: Boolean = true,
    val isLoading: Boolean = false
)

sealed class ChatListEvent {
    data class NavigateToChat(val chatId: String) : ChatListEvent()
    data class ShowError(val message: String) : ChatListEvent()
    data class ShowSuccess(val message: String) : ChatListEvent()
}
