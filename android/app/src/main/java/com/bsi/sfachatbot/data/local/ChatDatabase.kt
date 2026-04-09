package com.bsi.sfachatbot.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bsi.sfachatbot.data.local.entity.ChatMessageEntity

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "sfa_chatbot.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
