package com.example.myapplication.data.local.relations

import androidx.room.Entity

@Entity(primaryKeys = ["playlistName","uid"])
data class SongPlaylistCrossRef(
    val playlistName : String, // name play list
    val uid: String  // id Song
)
