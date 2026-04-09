package com.bsi.sfachatbot.model

data class TableData(
    val columns: List<String>,
    val rows: List<List<String>>,
    val rowCount: Int
)

data class ChatMessage(
    val id: Long = 0,
    val content: String,
    val isUser: Boolean,
    val sql: String? = null,
    val tableData: TableData? = null,
    val timestamp: Long = System.currentTimeMillis()
)
