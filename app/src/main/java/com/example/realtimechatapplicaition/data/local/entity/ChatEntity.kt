package com.example.realtimechatapplicaition.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
