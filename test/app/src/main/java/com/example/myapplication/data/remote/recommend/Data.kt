package com.example.myapplication.data.remote.recommend

import com.example.myapplication.data.remote.responses.Song
import java.io.Serializable

data class Data(
    val items: List<Song>,
): Serializable