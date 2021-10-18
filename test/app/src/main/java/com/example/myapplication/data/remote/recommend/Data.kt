package com.example.myapplication.data.remote.recommend

import com.example.myapplication.data.remote.responses.Song

data class Data(
    val image_url: String,
    val items: List<Song>,
    val total: Int
)