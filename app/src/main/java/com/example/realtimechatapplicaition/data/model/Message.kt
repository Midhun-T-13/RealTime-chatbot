package com.example.realtimechatapplicaition.data.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromUser: Boolean = true,
    val status: MessageStatus = MessageStatus.SENDING,
    val senderUsername: String = ""
)

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    QUEUED
}
