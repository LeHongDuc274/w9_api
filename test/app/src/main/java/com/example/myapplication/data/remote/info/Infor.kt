package com.example.myapplication.data.remote.info

import java.io.Serializable

data class Infor(
    val `data`: Data,
    val err: Int,
    val msg: String,
    val timestamp: Long
): Serializable