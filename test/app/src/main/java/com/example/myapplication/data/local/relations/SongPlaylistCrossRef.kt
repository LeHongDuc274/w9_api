package com.example.myapplication.data.local.relations

import androidx.room.Entity

@Entity(primaryKeys = ["playlistName","id"])
data class SongPlaylistCrossRef(
    val playlistName : String, // name play list
    val id: String  // id Song
)
