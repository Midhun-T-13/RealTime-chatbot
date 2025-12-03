package com.example.realtimechatapplicaition

import android.app.Application
import com.example.realtimechatapplicaition.data.local.AppDatabase
import com.example.realtimechatapplicaition.data.model.User
import com.example.realtimechatapplicaition.data.remote.ApiService
import com.example.realtimechatapplicaition.data.remote.ChatSocketService
import com.example.realtimechatapplicaition.data.repository.ChatRepository
import com.example.realtimechatapplicaition.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatApplication : Application() {

    lateinit var apiService: ApiService
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var socketService: ChatSocketService
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var database: AppDatabase
        private set

    var currentUser: User? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize database
        database = AppDatabase.getInstance(this)

        // Clear all database tables on app launch
        CoroutineScope(Dispatchers.IO).launch {
            database.clearAllTables()
        }

        // Initialize dependencies
        apiService = ApiService()
        chatRepository = ChatRepository(database.chatDao(), database.messageDao())
        socketService = ChatSocketService()
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
    }

    override fun onTerminate() {
        super.onTerminate()
        networkMonitor.stopMonitoring()
    }

    companion object {
        lateinit var instance: ChatApplication
            private set
    }
}
