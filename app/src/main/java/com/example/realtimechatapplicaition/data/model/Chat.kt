package com.example.realtimechatapplicaition.data.model

import java.util.UUID

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)
