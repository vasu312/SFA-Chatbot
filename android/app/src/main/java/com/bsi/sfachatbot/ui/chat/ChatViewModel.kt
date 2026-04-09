package com.bsi.sfachatbot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsi.sfachatbot.data.repository.ChatRepository
import com.bsi.sfachatbot.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    /** All chat messages, observed from Room via Flow -> StateFlow. */
    val messages: StateFlow<List<ChatMessage>> =
        repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Loading state for the send operation. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendQuestion(question: String) {
        if (question.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            repository.sendQuestion(question.trim())
            _isLoading.value = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
