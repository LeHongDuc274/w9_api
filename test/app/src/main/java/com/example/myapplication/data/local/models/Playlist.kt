package com.example.myapplication.data.local.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = false)
    val playlistName : String
    )
