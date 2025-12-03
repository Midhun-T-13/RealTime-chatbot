package com.example.realtimechatapplicaition.data.remote

import android.util.Log
import com.example.realtimechatapplicaition.data.model.CreateRoomRequest
import com.example.realtimechatapplicaition.data.model.NewMessageResponse
import com.example.realtimechatapplicaition.data.model.Room
import com.example.realtimechatapplicaition.data.model.User
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiService {

    companion object {
        private const val TAG = "ApiService"
        private const val BASE_URL = "https://chatroom-q7wb.onrender.com/"
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val userApi = retrofit.create(UserApi::class.java)

    suspend fun getUser(username: String): Result<User> {
        return try {
            Log.d(TAG, "Attempting to get user with username: $username")

            val response = userApi.getUser(username)

            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    Log.d(TAG, "API Success - User ID: ${user.id}, Username: ${user.username}")
                    Result.success(user)
                } else {
                    Log.e(TAG, "API Error - Empty response body")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API Error - Code: ${response.code()}, Message: ${response.message()}")
                Log.e(TAG, "Error Body: $errorBody")
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createRoom(username: String, roomName: String): Result<Room> {
        return try {
            Log.d(TAG, "Attempting to create room with name: $roomName for user: $username")

            val request = CreateRoomRequest(
                name = roomName,
                participantUsernames = listOf(username)
            )

            val response = userApi.createRoom(username, request)

            if (response.isSuccessful) {
                val room = response.body()
                if (room != null) {
                    Log.d(TAG, "API Success - Room ID: ${room.id}, Name: ${room.name}")
                    Result.success(room)
                } else {
                    Log.e(TAG, "API Error - Empty response body")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API Error - Code: ${response.code()}, Message: ${response.message()}")
                Log.e(TAG, "Error Body: $errorBody")
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(roomId: String, username: String): Result<Unit> {
        return try {
            Log.d(TAG, "Attempting to delete room with ID: $roomId for user: $username")

            val response = userApi.deleteRoom(roomId, username)

            if (response.isSuccessful) {
                Log.d(TAG, "API Success - Room deleted: ${response.body()?.message}")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API Error - Code: ${response.code()}, Message: ${response.message()}")
                Log.e(TAG, "Error Body: $errorBody")
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRoomMessages(roomId: String, username: String, limit: Int = 50): Result<List<NewMessageResponse>> {
        return try {
            Log.d(TAG, "Attempting to get messages for room: $roomId, user: $username, limit: $limit")

            val response = userApi.getRoomMessages(roomId, username, limit)

            if (response.isSuccessful) {
                val messages = response.body() ?: emptyList()
                Log.d(TAG, "API Success - Got ${messages.size} messages")
                Result.success(messages)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API Error - Code: ${response.code()}, Message: ${response.message()}")
                Log.e(TAG, "Error Body: $errorBody")
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
