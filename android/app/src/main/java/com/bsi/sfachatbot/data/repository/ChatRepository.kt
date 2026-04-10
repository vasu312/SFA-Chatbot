package com.bsi.sfachatbot.data.repository

import com.bsi.sfachatbot.data.local.ChatDao
import com.bsi.sfachatbot.data.local.ConversationDao
import com.bsi.sfachatbot.data.local.entity.ChatMessageEntity
import com.bsi.sfachatbot.data.local.entity.ConversationEntity
import com.bsi.sfachatbot.data.remote.ApiService
import com.bsi.sfachatbot.data.remote.dto.ChatRequest
import com.bsi.sfachatbot.data.remote.dto.ChatResponse
import com.bsi.sfachatbot.data.remote.dto.SummaryResponse
import com.bsi.sfachatbot.model.ChatMessage
import com.bsi.sfachatbot.model.TableData
import com.bsi.sfachatbot.util.NetworkResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ChatRepository(
    private val apiService: ApiService,
    private val chatDao: ChatDao,
    private val conversationDao: ConversationDao
) {
    private val gson = Gson()

    // --- Conversation operations ---

    fun getAllConversations(): Flow<List<ConversationEntity>> =
        conversationDao.getAllConversations()

    suspend fun getConversationCount(): Int = conversationDao.getCount()

    suspend fun getMostRecentConversation(): ConversationEntity? =
        conversationDao.getMostRecent()

    suspend fun createConversation(title: String = "New Chat"): Long =
        conversationDao.insert(ConversationEntity(title = title))

    suspend fun updateConversationTitle(conversation: ConversationEntity, newTitle: String) {
        conversationDao.update(conversation.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(conversation: ConversationEntity) {
        chatDao.deleteByConversation(conversation.id)
        conversationDao.delete(conversation)
    }

    // --- Message operations ---

    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>> =
        chatDao.getByConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getMessageCount(conversationId: Long): Int =
        chatDao.getMessageCount(conversationId)

    suspend fun sendQuestion(question: String, conversationId: Long): NetworkResult<ChatMessage> {
        chatDao.insertMessage(
            ChatMessageEntity(conversationId = conversationId, content = question, isUser = true)
        )

        return try {
            val response = apiService.sendQuery(ChatRequest(question))

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val (answerText, tableData) = buildTableData(body)
                val tableJson = tableData?.let { gson.toJson(it) }
                val systemMessage = ChatMessageEntity(
                    conversationId = conversationId,
                    content = answerText,
                    isUser = false,
                    sql = body.generatedSql,
                    tableJson = tableJson
                )
                chatDao.insertMessage(systemMessage)
                NetworkResult.Success(systemMessage.toDomain())
            } else {
                val errorEntity = ChatMessageEntity(
                    conversationId = conversationId,
                    content = "Server error. Please try again later.",
                    isUser = false
                )
                chatDao.insertMessage(errorEntity)
                NetworkResult.Error("Server error", response.code())
            }
        } catch (e: UnknownHostException) {
            saveErrorMessage(conversationId, "No internet connection. Please check your network.")
            NetworkResult.Error(e.localizedMessage ?: "No internet")
        } catch (e: SocketTimeoutException) {
            saveErrorMessage(conversationId, "Request timed out. Please try again.")
            NetworkResult.Error(e.localizedMessage ?: "Timeout")
        } catch (e: IOException) {
            saveErrorMessage(conversationId, "Network error. Please check your connection.")
            NetworkResult.Error(e.localizedMessage ?: "IO error")
        } catch (e: Exception) {
            saveErrorMessage(conversationId, "Something went wrong. Please try again.")
            NetworkResult.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun fetchSummary(): SummaryResponse? {
        return try {
            val response = apiService.getSummary()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearConversation(conversationId: Long) {
        chatDao.deleteByConversation(conversationId)
    }

    private suspend fun saveErrorMessage(conversationId: Long, message: String) {
        chatDao.insertMessage(
            ChatMessageEntity(conversationId = conversationId, content = message, isUser = false)
        )
    }

    private fun mapApiError(error: String?): String {
        if (error == null) return "Something went wrong. Please try again."
        return when {
            error.contains("Only SELECT", ignoreCase = true) ->
                "I can only answer read-only questions about your data."
            error.contains("Failed to generate SQL", ignoreCase = true) ->
                "I couldn't understand your question. Please try rephrasing it."
            error.contains("SQL execution failed", ignoreCase = true) ->
                "I had trouble running that query. Try rephrasing your question."
            error.contains("Could not generate", ignoreCase = true) ->
                "I couldn't understand your question. Please try rephrasing it."
            else -> "Something went wrong. Please try again."
        }
    }

    private fun buildTableData(response: ChatResponse): Pair<String, TableData?> {
        if (response.status == "error") {
            return Pair(mapApiError(response.error), null)
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
