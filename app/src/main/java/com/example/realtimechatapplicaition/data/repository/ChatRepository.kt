package com.example.realtimechatapplicaition.data.repository

import android.util.Log
import com.example.realtimechatapplicaition.data.local.dao.ChatDao
import com.example.realtimechatapplicaition.data.local.dao.MessageDao
import com.example.realtimechatapplicaition.data.local.entity.ChatEntity
import com.example.realtimechatapplicaition.data.local.entity.MessageEntity
import com.example.realtimechatapplicaition.data.model.Chat
import com.example.realtimechatapplicaition.data.model.Message
import com.example.realtimechatapplicaition.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    companion object {
        private const val TAG = "ChatRepository"
    }

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId

    // Get all chats as Flow
    val chats: Flow<List<Chat>> = chatDao.getAllChats().map { entities ->
        entities.map { it.toChat() }
    }

    // Get current chat as Flow
    fun getCurrentChat(): Flow<Chat?> {
        val chatId = _currentChatId.value ?: return kotlinx.coroutines.flow.flowOf(null)
        return chatDao.getChatByIdFlow(chatId).map { it?.toChat() }
    }

    // Get messages for a chat as Flow
    fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId).map { entities ->
            entities.map { it.toMessage() }
        }
    }

    suspend fun createChat(title: String, roomId: String): Chat {
        val chatEntity = ChatEntity(
            id = roomId,
            title = title,
            lastMessage = "No messages yet",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        chatDao.insertChat(chatEntity)
        Log.d(TAG, "Created chat: $roomId with title: $title")
        return chatEntity.toChat()
    }

    suspend fun getChatById(chatId: String): Chat? {
        return chatDao.getChatById(chatId)?.toChat()
    }

    fun selectChat(chatId: String) {
        _currentChatId.value = chatId
    }

    fun clearCurrentChat() {
        _currentChatId.value = null
    }

    suspend fun deleteChat(chatId: String) {
        chatDao.deleteChat(chatId)
        if (_currentChatId.value == chatId) {
            _currentChatId.value = null
        }
        Log.d(TAG, "Deleted chat: $chatId")
    }

    suspend fun updateLastMessage(chatId: String, lastMessage: String, timestamp: Long) {
        chatDao.updateLastMessage(chatId, lastMessage, timestamp)
    }

    suspend fun markChatAsRead(chatId: String) {
        chatDao.updateUnreadCount(chatId, 0)
    }

    // Message operations

    suspend fun saveMessage(message: Message): MessageEntity {
        val entity = MessageEntity(
            id = message.id,
            chatId = message.chatId,
            content = message.content,
            timestamp = message.timestamp,
            isFromUser = message.isFromUser,
            senderUsername = message.senderUsername,
            isSentToServer = message.status == MessageStatus.DELIVERED || message.status == MessageStatus.SENT,
            serverMessageId = null
        )
        messageDao.insertMessage(entity)
        Log.d(TAG, "Saved message: ${message.id} isSentToServer: ${entity.isSentToServer}")
        return entity
    }

    suspend fun saveMessageFromServer(
        id: String,
        chatId: String,
        content: String,
        timestamp: Long,
        isFromUser: Boolean,
        senderUsername: String
    ) {
        // Check if message already exists
        if (messageDao.messageExists(id)) {
            Log.d(TAG, "Message already exists: $id")
            return
        }

        val entity = MessageEntity(
            id = id,
            chatId = chatId,
            content = content,
            timestamp = timestamp,
            isFromUser = isFromUser,
            senderUsername = senderUsername,
            isSentToServer = true,
            serverMessageId = id
        )
        messageDao.insertMessage(entity)
        Log.d(TAG, "Saved server message: $id")
    }

    suspend fun markMessageAsSent(localMessageId: String, serverMessageId: String? = null) {
        if (serverMessageId != null) {
            messageDao.markMessageAsSent(localMessageId, serverMessageId)
        } else {
            messageDao.markMessageAsSent(localMessageId)
        }
        Log.d(TAG, "Marked message as sent: $localMessageId")
    }

    suspend fun getUnsentMessages(): List<Message> {
        return messageDao.getUnsentMessages().map { it.toMessage() }
    }

    suspend fun getUnsentMessagesForChat(chatId: String): List<Message> {
        return messageDao.getUnsentMessagesForChat(chatId).map { it.toMessage() }
    }

    suspend fun getMessagesForChatOnce(chatId: String): List<Message> {
        return messageDao.getMessagesForChatOnce(chatId).map { it.toMessage() }
    }

    suspend fun syncMessagesFromServer(chatId: String, serverMessages: List<Message>) {
        var insertedCount = 0
        for (message in serverMessages) {
            // Check if message already exists (by server id or as serverMessageId in local message)
            if (!messageDao.messageExists(message.id)) {
                // Also check if we have a local message with matching content that was sent
                // This handles the case where local message has different id than server message
                val existingByContent = messageDao.findMessageByContentAndSender(
                    chatId = chatId,
                    content = message.content,
                    senderUsername = message.senderUsername
                )

                if (existingByContent == null) {
                    val entity = MessageEntity(
                        id = message.id,
                        chatId = message.chatId,
                        content = message.content,
                        timestamp = message.timestamp,
                        isFromUser = message.isFromUser,
                        senderUsername = message.senderUsername,
                        isSentToServer = true,
                        serverMessageId = message.id
                    )
                    messageDao.insertMessage(entity)
                    insertedCount++
                } else {
                    // Update existing message with server id
                    messageDao.markMessageAsSent(existingByContent.id, message.id)
                    Log.d(TAG, "Updated existing message ${existingByContent.id} with server id ${message.id}")
                }
            }
        }
        Log.d(TAG, "Synced $insertedCount new messages from server for chat: $chatId")
    }

    // Extension functions for conversion
    private fun ChatEntity.toChat(): Chat {
        return Chat(
            id = id,
            title = title,
            lastMessage = lastMessage,
            lastMessageTimestamp = lastMessageTimestamp,
            unreadCount = unreadCount
        )
    }

    private fun MessageEntity.toMessage(): Message {
        return Message(
            id = id,
            chatId = chatId,
            content = content,
            timestamp = timestamp,
            isFromUser = isFromUser,
            status = if (isSentToServer) MessageStatus.DELIVERED else MessageStatus.QUEUED,
            senderUsername = senderUsername
        )
    }
}
