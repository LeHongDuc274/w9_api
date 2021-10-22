package com.example.myapplication.data.local

import androidx.room.*
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.local.relations.PlaylistWithSong
import com.example.myapplication.data.local.relations.SongPlaylistCrossRef
import com.example.myapplication.data.local.relations.SongWithPlaylist

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

    @Query("select * from playlist")
    suspend fun getAllPlaylist(): MutableList<Playlist>

    @Insert
    suspend fun insertNewPlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query(value = "select exists(select * from playlist where playlistName = :playlistName)")
    suspend fun isPlaylistExits(playlistName : String) : Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongPlaylistCrossRef(crossRef: SongPlaylistCrossRef)

    @Transaction
    @Query("SELECT * FROM playlist WHERE playlistName = :playlistName")
    suspend fun getSongOfPlaylist(playlistName: String): MutableList<PlaylistWithSong>

    @Transaction
    @Query("SELECT * FROM song_favourite WHERE id = :id")
    suspend fun getPlaylistOfSong(id: String): MutableList<SongWithPlaylist>

}