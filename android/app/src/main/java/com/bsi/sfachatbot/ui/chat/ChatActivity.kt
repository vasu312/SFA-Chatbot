package com.bsi.sfachatbot.ui.chat

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bsi.sfachatbot.R
import com.bsi.sfachatbot.SfaChatbotApplication
import com.bsi.sfachatbot.databinding.ActivityChatBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter

    private val viewModel: ChatViewModel by viewModels {
        val app = application as SfaChatbotApplication
        ChatViewModelFactory(app.chatRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupInputHandling()
        setupToolbar()
        observeState()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupInputHandling() {
        binding.btnSend.setOnClickListener { sendMessage() }

        binding.editMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear -> {
                    viewModel.clearHistory()
                    true
                }
                else -> false
            }
        }
    }

    private fun sendMessage() {
        val text = binding.editMessage.text?.toString() ?: return
        if (text.isBlank()) return
        viewModel.sendQuestion(text)
        binding.editMessage.text?.clear()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collectLatest { messages ->
                        chatAdapter.submitList(messages) {
                            if (messages.isNotEmpty()) {
                                binding.recyclerChat.smoothScrollToPosition(
                                    messages.size - 1
                                )
                            }
                        }
                    }
                }
                launch {
                    viewModel.isLoading.collectLatest { loading ->
                        binding.progressBar.isVisible = loading
                        binding.btnSend.isEnabled = !loading
                    }
                }
            }
        }
    }
}
