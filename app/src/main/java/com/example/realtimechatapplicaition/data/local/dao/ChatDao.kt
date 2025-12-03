package com.example.realtimechatapplicaition.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.realtimechatapplicaition.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTimestamp = :timestamp WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, timestamp: Long)

    @Query("UPDATE chats SET unreadCount = :count WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: String, count: Int)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()
}
