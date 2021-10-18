package com.example.myapplication.data.remote.responses

data class Data(
    val customied: List<Any>,
    val peak_score: Int,
    val song: List<Song>,
    val songHis: SongHis
)