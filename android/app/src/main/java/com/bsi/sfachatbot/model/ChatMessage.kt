package com.bsi.sfachatbot.model

data class ChatMessage(
    val id: Long = 0,
    val content: String,
    val isUser: Boolean,
    val sql: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
