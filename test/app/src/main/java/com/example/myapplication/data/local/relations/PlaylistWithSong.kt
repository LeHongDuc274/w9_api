package com.example.myapplication.data.local.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.local.models.SongInPlaylist

data class PlaylistWithSong(
    @Embedded val playlist : Playlist,
    @Relation(
        parentColumn = "playlistName",
        entityColumn = "uid",
        associateBy = Junction(SongPlaylistCrossRef::class)
    )
    val songs : MutableList<SongInPlaylist>
)
