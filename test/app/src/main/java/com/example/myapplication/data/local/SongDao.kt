package com.example.myapplication.data.local

import androidx.room.*
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.local.models.SongInPlaylist
import com.example.myapplication.data.local.relations.PlaylistWithSong
import com.example.myapplication.data.local.relations.SongPlaylistCrossRef
import com.example.myapplication.data.local.relations.SongWithPlaylist

@Dao
interface SongDao {
    // get top 100
    @Query("select * from song_favourite")
    suspend fun getAllSong(): MutableList<SongFavourite>

    // favourite
    @Insert
    suspend fun insert(song: SongFavourite)
    @Delete
    suspend fun delete(song: SongFavourite)
    @Query("delete from song_favourite where id= :id")
    suspend fun deleteById(id:String)
    @Query(value = "select exists(select * from song_favourite where id = :id)")
    suspend fun isExist(id:String) : Boolean


    //my playlist
    @Query(value = "select exists(select * from SongPlaylistCrossRef where uid = :id and playlistName= :name)")
    suspend fun isExistCrossRef(id:String,name:String) : Boolean

    @Query("delete from SongPlaylistCrossRef where uid = :id and playlistName= :name ")
    suspend fun deleteCrossRef(id: String,name: String)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongInPlaylist(song:SongInPlaylist)

    @Transaction
    @Query("SELECT * FROM playlist WHERE playlistName = :name") // WHERE playlistName = :playlistName
    suspend fun getSongOfPlaylist(name:String): PlaylistWithSong

//    @Transaction
//    @Query("SELECT * FROM SongInPlaylist WHERE id = :id")
//    suspend fun getPlaylistOfSong(id: String): MutableList<SongWithPlaylist>

}