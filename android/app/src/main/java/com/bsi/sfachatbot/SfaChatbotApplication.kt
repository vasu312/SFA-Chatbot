package com.bsi.sfachatbot

import android.app.Application
import com.bsi.sfachatbot.data.local.ChatDatabase
import com.bsi.sfachatbot.data.remote.ApiClient
import com.bsi.sfachatbot.data.repository.ChatRepository

class SfaChatbotApplication : Application() {

    lateinit var chatRepository: ChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val apiService = ApiClient.create()
        val database = ChatDatabase.getInstance(this)
        chatRepository = ChatRepository(apiService, database.chatDao())
    }
}
