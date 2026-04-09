package com.bsi.sfachatbot.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val status: String,
    val question: String,
    @SerializedName("generated_sql") val generatedSql: String?,
    val results: List<Map<String, Any>>?,
    @SerializedName("row_count") val rowCount: Int,
    val columns: List<String>?,
    val error: String?
)
