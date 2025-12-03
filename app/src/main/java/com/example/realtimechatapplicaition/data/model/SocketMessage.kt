package com.example.realtimechatapplicaition.data.model

import com.google.gson.annotations.SerializedName

// Response models
data class JoinedRoomResponse(
    @SerializedName("room_id")
    val roomId: String
)

data class NewMessageResponse(
    val id: String,
    @SerializedName("room_id")
    val roomId: String,
    @SerializedName("sender_username")
    val senderUsername: String,
    val text: String,
    @SerializedName("created_at")
    val createdAt: String
)
