package com.example.realtimechatapplicaition.data.remote

import com.example.realtimechatapplicaition.data.model.CreateRoomRequest
import com.example.realtimechatapplicaition.data.model.DeleteRoomResponse
import com.example.realtimechatapplicaition.data.model.NewMessageResponse
import com.example.realtimechatapplicaition.data.model.Room
import com.example.realtimechatapplicaition.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {

    @GET("users/me")
    suspend fun getUser(
        @Header("X-Username") username: String
    ): Response<User>

    @POST("rooms")
    suspend fun createRoom(
        @Header("X-Username") username: String,
        @Body request: CreateRoomRequest
    ): Response<Room>

    @DELETE("rooms/{roomId}")
    suspend fun deleteRoom(
        @Path("roomId") roomId: String,
        @Header("X-Username") username: String
    ): Response<DeleteRoomResponse>

    @GET("rooms/{roomId}/messages")
    suspend fun getRoomMessages(
        @Path("roomId") roomId: String,
        @Header("X-Username") username: String,
        @Query("limit") limit: Int = 50
    ): Response<List<NewMessageResponse>>
}
