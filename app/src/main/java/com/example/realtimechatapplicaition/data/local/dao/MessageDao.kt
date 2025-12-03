package com.example.realtimechatapplicaition.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.realtimechatapplicaition.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChatOnce(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE isSentToServer = 0 ORDER BY timestamp ASC")
    suspend fun getUnsentMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isSentToServer = 0 ORDER BY timestamp ASC")
    suspend fun getUnsentMessagesForChat(chatId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET isSentToServer = 1, serverMessageId = :serverMessageId WHERE id = :localMessageId")
    suspend fun markMessageAsSent(localMessageId: String, serverMessageId: String)

    @Query("UPDATE messages SET isSentToServer = 1 WHERE id = :messageId")
    suspend fun markMessageAsSent(messageId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE id = :messageId OR serverMessageId = :messageId)")
    suspend fun messageExists(messageId: String): Boolean

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND content = :content AND senderUsername = :senderUsername LIMIT 1")
    suspend fun findMessageByContentAndSender(chatId: String, content: String, senderUsername: String): MessageEntity?
}
