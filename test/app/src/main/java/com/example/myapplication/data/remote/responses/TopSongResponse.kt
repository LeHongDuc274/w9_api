package com.example.myapplication.data.remote.responses

data class TopSongResponse(
    val `data`: Data,
    val err: Int,
    val msg: String,
    val timestamp: Long
)