package com.example.myapplication.data.remote.recommend

data class RecommendResponses(
    val `data`: Data,
    val err: Int,
    val msg: String,
    val timestamp: Long
)