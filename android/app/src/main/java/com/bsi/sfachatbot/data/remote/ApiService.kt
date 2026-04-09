package com.bsi.sfachatbot.data.remote

import com.bsi.sfachatbot.data.remote.dto.ChatRequest
import com.bsi.sfachatbot.data.remote.dto.ChatResponse
import com.bsi.sfachatbot.data.remote.dto.SummaryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("api/v1/chat")
    suspend fun sendQuery(@Body request: ChatRequest): Response<ChatResponse>

    @GET("api/v1/health")
    suspend fun healthCheck(): Response<Map<String, Any>>

    @GET("api/v1/summary")
    suspend fun getSummary(): Response<SummaryResponse>
}
