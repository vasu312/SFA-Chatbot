package com.bsi.sfachatbot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsi.sfachatbot.data.local.entity.ConversationEntity
import com.bsi.sfachatbot.data.remote.dto.SummaryResponse
import com.bsi.sfachatbot.data.repository.ChatRepository
import com.bsi.sfachatbot.model.ChatMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    // The ID of the currently active conversation (-1L = not yet initialised)
    private val _currentConversationId = MutableStateFlow(-1L)
    val currentConversationId: StateFlow<Long> = _currentConversationId.asStateFlow()

    // All conversations, ordered by most-recently updated
    val conversations: StateFlow<List<ConversationEntity>> =
        repository.getAllConversations().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Messages for the active conversation — switches automatically when the ID changes
    val messages: StateFlow<List<ChatMessage>> = _currentConversationId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList())
            else repository.getMessagesForConversation(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Loading state for in-flight API calls
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Dashboard summary stats
    private val _summary = MutableStateFlow<SummaryResponse?>(null)
    val summary: StateFlow<SummaryResponse?> = _summary.asStateFlow()

    init {
        viewModelScope.launch {
            // Ensure at least one conversation exists; switch to the most recent one
            if (repository.getConversationCount() == 0) {
                val id = repository.createConversation("New Chat")
                _currentConversationId.value = id
            } else {
                val recent = repository.getMostRecentConversation()
                if (recent != null) _currentConversationId.value = recent.id
            }
        }
        viewModelScope.launch {
            _summary.value = repository.fetchSummary()
        }
    }

    fun sendQuestion(question: String) {
        val convId = _currentConversationId.value
        if (question.isBlank() || _isLoading.value || convId == -1L) return

        viewModelScope.launch {
            _isLoading.value = true

            // Auto-title: set conversation title from first question
            val messageCount = repository.getMessageCount(convId)
            if (messageCount == 0) {
                val autoTitle = question.trim().take(40)
                val conv = conversations.value.find { it.id == convId }
                if (conv != null) {
                    repository.updateConversationTitle(conv, autoTitle)
                }
            }

            repository.sendQuestion(question.trim(), convId)
            _isLoading.value = false
        }
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val id = repository.createConversation("New Chat")
            _currentConversationId.value = id
        }
    }

    fun switchConversation(id: Long) {
        if (id != _currentConversationId.value) {
            _currentConversationId.value = id
        }
    }

    fun updateConversationTitle(conversation: ConversationEntity, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.updateConversationTitle(conversation, newTitle.trim())
        }
    }

    fun deleteConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            val isActive = conversation.id == _currentConversationId.value
            repository.deleteConversation(conversation)

            if (isActive) {
                // Switch to another conversation or create a fresh one
                val remaining = conversations.value.filter { it.id != conversation.id }
                if (remaining.isNotEmpty()) {
                    _currentConversationId.value = remaining.first().id
                } else {
                    val id = repository.createConversation("New Chat")
                    _currentConversationId.value = id
                }
            }
        }
    }

    fun refreshSummary() {
        viewModelScope.launch {
            _summary.value = repository.fetchSummary()
        }
    }
}
