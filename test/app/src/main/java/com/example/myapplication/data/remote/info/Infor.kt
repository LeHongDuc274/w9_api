package com.example.myapplication.data.remote.info

data class Infor(
    val `data`: Data,
    val err: Int,
    val msg: String,
    val timestamp: Long
)