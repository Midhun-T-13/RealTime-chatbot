package com.example.realtimechatapplicaition.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromUser: Boolean = true,
    val senderUsername: String = "",
    val isSentToServer: Boolean = false,
    val serverMessageId: String? = null
)
