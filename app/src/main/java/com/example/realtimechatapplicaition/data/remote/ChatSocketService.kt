package com.example.realtimechatapplicaition.data.remote

import android.util.Log
import com.example.realtimechatapplicaition.data.model.JoinedRoomResponse
import com.example.realtimechatapplicaition.data.model.NewMessageResponse
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI

class ChatSocketService {

    companion object {
        private const val TAG = "ChatSocketService"
        private const val BASE_URL = "https://chatroom-q7wb.onrender.com"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private var socket: Socket? = null
    private var currentUsername: String? = null

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // Events
    private val _joinedRoom = MutableSharedFlow<JoinedRoomResponse>()
    val joinedRoom: SharedFlow<JoinedRoomResponse> = _joinedRoom

    private val _newMessage = MutableSharedFlow<NewMessageResponse>()
    val newMessage: SharedFlow<NewMessageResponse> = _newMessage

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error

    fun connect(username: String) {
        if (_connectionState.value is ConnectionState.Connected) {
            Log.d(TAG, "Already connected")
            return
        }

        currentUsername = username
        _connectionState.value = ConnectionState.Connecting

        try {
            Log.d(TAG, "Connecting to Socket.IO: $BASE_URL with username: $username")

            val options = IO.Options().apply {
                query = "username=$username"
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 20000
            }

            socket = IO.socket(URI.create(BASE_URL), options)

            setupSocketListeners()

            socket?.connect()
            Log.d(TAG, "Socket connect() called")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating socket: ${e.message}", e)
            scope.launch {
                _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                _error.emit(e.message ?: "Connection failed")
            }
        }
    }

    private fun setupSocketListeners() {
        socket?.apply {
            // Connection events
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket.IO connected")
                scope.launch {
                    _connectionState.value = ConnectionState.Connected
                }
            }

            on(Socket.EVENT_DISCONNECT) { args ->
                val reason = args.firstOrNull()?.toString() ?: "Unknown"
                Log.d(TAG, "Socket.IO disconnected: $reason")
                scope.launch {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()?.toString() ?: "Unknown error"
                Log.e(TAG, "Socket.IO connection error: $error")
                scope.launch {
                    _connectionState.value = ConnectionState.Error(error)
                    _error.emit("Connection error: $error")
                }
            }

            // Custom events
            on("joined_room") { args ->
                Log.d(TAG, "Received joined_room event: ${args.contentToString()}")
                handleJoinedRoom(args)
            }

            on("new_message") { args ->
                Log.d(TAG, "Received new_message event: ${args.contentToString()}")
                handleNewMessage(args)
            }

            on("error") { args ->
                Log.e(TAG, "Received error event: ${args.contentToString()}")
                handleError(args)
            }
        }
    }

    private fun handleJoinedRoom(args: Array<Any>) {
        scope.launch {
            try {
                val data = args.firstOrNull()
                Log.d(TAG, "Parsing joined_room data: $data")

                val jsonStr = when (data) {
                    is JSONObject -> data.toString()
                    is String -> data
                    else -> data?.toString() ?: "{}"
                }

                val response = gson.fromJson(jsonStr, JoinedRoomResponse::class.java)
                Log.d(TAG, "Joined room: ${response.roomId}")
                _joinedRoom.emit(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing joined_room: ${e.message}", e)
            }
        }
    }

    private fun handleNewMessage(args: Array<Any>) {
        scope.launch {
            try {
                val data = args.firstOrNull()
                Log.d(TAG, "Parsing new_message data: $data")

                val jsonStr = when (data) {
                    is JSONObject -> data.toString()
                    is String -> data
                    else -> data?.toString() ?: "{}"
                }

                val response = gson.fromJson(jsonStr, NewMessageResponse::class.java)
                Log.d(TAG, "New message from ${response.senderUsername}: ${response.text}")
                _newMessage.emit(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing new_message: ${e.message}", e)
            }
        }
    }

    private fun handleError(args: Array<Any>) {
        scope.launch {
            try {
                val data = args.firstOrNull()
                Log.d(TAG, "Parsing error data: $data")

                val errorMessage = when (data) {
                    is JSONObject -> data.optString("message", data.toString())
                    is String -> data
                    else -> data?.toString() ?: "Unknown error"
                }

                Log.e(TAG, "Socket error: $errorMessage")
                _error.emit(errorMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing error event: ${e.message}", e)
                _error.emit("Unknown error occurred")
            }
        }
    }

    fun joinRoom(roomId: String) {
        if (_connectionState.value !is ConnectionState.Connected) {
            Log.e(TAG, "Cannot join room: Not connected")
            scope.launch {
                _error.emit("Not connected to server")
            }
            return
        }

        try {
            val data = JSONObject().apply {
                put("room_id", roomId)
            }

            Log.d(TAG, "Emitting join_room event: $data")
            socket?.emit("join_room", data)
        } catch (e: Exception) {
            Log.e(TAG, "Error joining room: ${e.message}", e)
            scope.launch {
                _error.emit("Failed to join room: ${e.message}")
            }
        }
    }

    fun sendMessage(roomId: String, text: String) {
        if (_connectionState.value !is ConnectionState.Connected) {
            Log.e(TAG, "Cannot send message: Not connected")
            scope.launch {
                _error.emit("Not connected to server")
            }
            return
        }

        try {
            val data = JSONObject().apply {
                put("room_id", roomId)
                put("text", text)
            }

            Log.d(TAG, "Emitting send_message event: $data")
            socket?.emit("send_message", data)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            scope.launch {
                _error.emit("Failed to send message: ${e.message}")
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting Socket.IO")
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun isConnected(): Boolean = _connectionState.value is ConnectionState.Connected
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
