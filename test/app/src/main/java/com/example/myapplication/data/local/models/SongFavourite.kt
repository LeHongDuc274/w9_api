package com.example.myapplication.data.local.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "song_favourite"
   // indices = [Index(value = ["id"], unique = true)]
)
data class SongFavourite(
    @ColumnInfo(name = "artists_names") val artists_names: String,
    @ColumnInfo(name = "duration") val duration: Int,
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "thumbnail") val thumbnail: String,
    @ColumnInfo(name = "title") var title: String
) : Serializable {
}