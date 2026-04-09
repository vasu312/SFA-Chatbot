package com.bsi.sfachatbot.ui.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.bsi.sfachatbot.SfaChatbotApplication
import com.bsi.sfachatbot.ui.theme.SfaChatbotTheme

class ChatActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory((application as SfaChatbotApplication).chatRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SfaChatbotTheme {
                ChatScreen(viewModel)
            }
        }
    }
}
