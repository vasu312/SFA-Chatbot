package com.bsi.sfachatbot.ui.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsi.sfachatbot.SfaChatbotApplication
import com.bsi.sfachatbot.ui.components.DrawerContent
import com.bsi.sfachatbot.ui.dashboard.DashboardScreen
import com.bsi.sfachatbot.ui.theme.SfaChatbotTheme
import kotlinx.coroutines.launch

sealed class Screen {
    object Chat : Screen()
    object Dashboard : Screen()
}

class ChatActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory((application as SfaChatbotApplication).chatRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SfaChatbotTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

                val conversations by viewModel.conversations.collectAsStateWithLifecycle()
                val currentConversationId by viewModel.currentConversationId.collectAsStateWithLifecycle()
                val summary by viewModel.summary.collectAsStateWithLifecycle()

                // Back handler:
                //   - Drawer open → close drawer
                //   - Chat screen → go back to Dashboard (home)
                //   - Dashboard → no intercept; system back exits the app
                BackHandler(enabled = drawerState.isOpen || currentScreen is Screen.Chat) {
                    when {
                        drawerState.isOpen -> scope.launch { drawerState.close() }
                        currentScreen is Screen.Chat -> currentScreen = Screen.Dashboard
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            conversations = conversations,
                            currentConversationId = currentConversationId,
                            isDashboardActive = currentScreen is Screen.Dashboard,
                            onDashboardClick = {
                                scope.launch { drawerState.close() }
                                currentScreen = Screen.Dashboard
                            },
                            onNewChatClick = {
                                viewModel.createNewConversation()
                                scope.launch { drawerState.close() }
                                currentScreen = Screen.Chat
                            },
                            onConversationClick = { id ->
                                viewModel.switchConversation(id)
                                scope.launch { drawerState.close() }
                                currentScreen = Screen.Chat
                            },
                            onRenameConversation = { conv, newTitle ->
                                viewModel.updateConversationTitle(conv, newTitle)
                            },
                            onDeleteConversation = { conv ->
                                viewModel.deleteConversation(conv)
                            }
                        )
                    }
                ) {
                    when (currentScreen) {
                        is Screen.Chat -> ChatScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                        is Screen.Dashboard -> DashboardScreen(
                            summary = summary,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onRefresh = { viewModel.refreshSummary() }
                        )
                    }
                }
            }
        }
    }
}
