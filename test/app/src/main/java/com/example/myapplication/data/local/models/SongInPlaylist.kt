package com.example.myapplication.data.local.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "song_in_list")
data class SongInPlaylist(
    @ColumnInfo(name = "artists_names") val artists_names: String,
    @ColumnInfo(name = "duration") val duration: Int,
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "uid") val id: String,
    @ColumnInfo(name = "thumbnail") val thumbnail: String?,
    @ColumnInfo(name = "title") var title: String
)
