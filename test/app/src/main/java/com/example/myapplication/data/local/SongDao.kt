package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.myapplication.data.remote.responses.Song

@Dao
interface SongDao {
    @Query("select * from song_favourite")
    suspend fun getAllSong(): MutableList<SongFavourite>

    @Insert
    suspend fun insert(song: SongFavourite)

    @Delete
    suspend fun delete(song: SongFavourite)

    @Query("delete from song_favourite where id= :id")
    suspend fun deleteById(id:String)

    @Query(value = "select exists(select * from song_favourite where id = :id)")
    suspend fun isExist(id:String) : Boolean

}