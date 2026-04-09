package com.bsi.sfachatbot.data.repository

import com.bsi.sfachatbot.data.local.ChatDao
import com.bsi.sfachatbot.data.local.entity.ChatMessageEntity
import com.bsi.sfachatbot.data.remote.ApiService
import com.bsi.sfachatbot.data.remote.dto.ChatRequest
import com.bsi.sfachatbot.data.remote.dto.ChatResponse
import com.bsi.sfachatbot.model.ChatMessage
import com.bsi.sfachatbot.model.TableData
import com.bsi.sfachatbot.util.NetworkResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val apiService: ApiService,
    private val chatDao: ChatDao
) {
    private val gson = Gson()

    /** Observe all messages from Room (single source of truth). */
    val allMessages: Flow<List<ChatMessage>> =
        chatDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Send a question to the backend:
     * 1. Save user message to Room immediately (appears in chat).
     * 2. Call API.
     * 3. Save system response to Room.
     */
    suspend fun sendQuestion(question: String): NetworkResult<ChatMessage> {
        chatDao.insertMessage(
            ChatMessageEntity(content = question, isUser = true)
        )

        return try {
            val response = apiService.sendQuery(ChatRequest(question))

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val (answerText, tableData) = buildTableData(body)
                val tableJson = tableData?.let { gson.toJson(it) }
                val systemMessage = ChatMessageEntity(
                    content = answerText,
                    isUser = false,
                    sql = body.generatedSql,
                    tableJson = tableJson
                )
                chatDao.insertMessage(systemMessage)
                NetworkResult.Success(systemMessage.toDomain())
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                val errorEntity = ChatMessageEntity(
                    content = "Error: $errorMsg",
                    isUser = false
                )
                chatDao.insertMessage(errorEntity)
                NetworkResult.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Could not reach server"
            val errorEntity = ChatMessageEntity(
                content = "Network error: $errorMsg",
                isUser = false
            )
            chatDao.insertMessage(errorEntity)
            NetworkResult.Error(errorMsg)
        }
    }

    suspend fun clearHistory() {
        chatDao.clearAll()
    }

    private fun buildTableData(response: ChatResponse): Pair<String, TableData?> {
        if (response.status == "error") {
            return Pair(response.error ?: "An unknown error occurred.", null)
        }
        val results = response.results
        if (results.isNullOrEmpty()) {
            return Pair("No results found for your query.", null)
        }
        val columns = response.columns ?: results.first().keys.toList()
        val rows = results.map { row -> columns.map { col -> row[col]?.toString() ?: "-" } }
        return Pair("", TableData(columns = columns, rows = rows, rowCount = response.rowCount))
    }

    private fun ChatMessageEntity.toDomain(): ChatMessage {
        val tableData: TableData? = tableJson?.let {
            val type = object : TypeToken<TableData>() {}.type
            gson.fromJson(it, type)
        }
        return ChatMessage(id, content, isUser, sql, tableData, timestamp)
    }
}
