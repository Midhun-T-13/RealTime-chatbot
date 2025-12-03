package com.example.realtimechatapplicaition.data.model

import com.google.gson.annotations.SerializedName

data class Room(
    val id: String,
    val name: String,
    @SerializedName("owner_username")
    val ownerUsername: String,
    @SerializedName("participant_usernames")
    val participantUsernames: List<String>,
    @SerializedName("created_at")
    val createdAt: String
)

data class CreateRoomRequest(
    val name: String,
    @SerializedName("participant_usernames")
    val participantUsernames: List<String>
)

data class DeleteRoomResponse(
    val message: String
)
