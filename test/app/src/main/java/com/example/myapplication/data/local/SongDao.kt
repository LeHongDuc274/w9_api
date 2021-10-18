package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.myapplication.data.remote.responses.Song

@Dao
interface SongDao {
    @Query("select * from song_favourite")
    suspend fun getAllSong(): List<SongFavourite>

    @Insert
    suspend fun insert(song: SongFavourite)

    @Delete
    suspend fun delete(song: SongFavourite)

}