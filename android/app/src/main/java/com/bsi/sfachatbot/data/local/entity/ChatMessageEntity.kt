package com.bsi.sfachatbot.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    @ColumnInfo(name = "is_user")
    val isUser: Boolean,
    val sql: String? = null,
    @ColumnInfo(name = "table_json")
    val tableJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
