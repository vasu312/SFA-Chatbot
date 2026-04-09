package com.bsi.sfachatbot.data.repository

import com.bsi.sfachatbot.data.local.ChatDao
import com.bsi.sfachatbot.data.local.entity.ChatMessageEntity
import com.bsi.sfachatbot.data.remote.ApiService
import com.bsi.sfachatbot.data.remote.dto.ChatRequest
import com.bsi.sfachatbot.model.ChatMessage
import com.bsi.sfachatbot.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val apiService: ApiService,
    private val chatDao: ChatDao
) {

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
        // Save user message locally
        chatDao.insertMessage(
            ChatMessageEntity(content = question, isUser = true)
        )

        return try {
            val response = apiService.sendQuery(ChatRequest(question))

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val answerText = formatResponse(body)
                val systemMessage = ChatMessageEntity(
                    content = answerText,
                    isUser = false,
                    sql = body.generatedSql
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

    /**
     * Format the API response into a readable chat message.
     */
    private fun formatResponse(
        response: com.bsi.sfachatbot.data.remote.dto.ChatResponse
    ): String {
        if (response.status == "error") {
            return response.error ?: "An unknown error occurred."
        }

        val results = response.results
        if (results.isNullOrEmpty()) {
            return "No results found for your query."
        }

        val columns = response.columns ?: results.first().keys.toList()
        val sb = StringBuilder()

        // Format as a readable table
        results.forEachIndexed { index, row ->
            if (results.size > 1) {
                sb.appendLine("--- Row ${index + 1} ---")
            }
            for (col in columns) {
                val value = row[col] ?: "-"
                sb.appendLine("$col: $value")
            }
            if (index < results.size - 1) sb.appendLine()
        }

        sb.appendLine()
        sb.append("(${response.rowCount} row(s) returned)")

        return sb.toString().trim()
    }

    private fun ChatMessageEntity.toDomain() =
        ChatMessage(id, content, isUser, sql, timestamp)
}
