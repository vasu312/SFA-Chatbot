package com.bsi.sfachatbot.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SummaryStats(
    @SerializedName("order_count") val orderCount: Int,
    @SerializedName("order_value") val orderValue: Double,
    @SerializedName("total_visits") val totalVisits: Int,
    @SerializedName("lines_sold") val linesSold: Int
)

data class SummaryResponse(
    val day: SummaryStats,
    val month: SummaryStats,
    @SerializedName("reference_date") val referenceDate: String
)
