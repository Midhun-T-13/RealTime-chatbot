package com.example.realtimechatapplicaition.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.realtimechatapplicaition.data.local.dao.ChatDao
import com.example.realtimechatapplicaition.data.local.dao.MessageDao
import com.example.realtimechatapplicaition.data.local.entity.ChatEntity
import com.example.realtimechatapplicaition.data.local.entity.MessageEntity

@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        private const val DATABASE_NAME = "chat_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
