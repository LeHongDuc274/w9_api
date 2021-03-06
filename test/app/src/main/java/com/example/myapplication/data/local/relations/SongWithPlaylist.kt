package com.example.myapplication.data.local.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.myapplication.data.local.models.Playlist
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.local.models.SongInPlaylist

data class SongWithPlaylist(
    @Embedded val song : SongInPlaylist,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistName",
        associateBy = Junction(SongPlaylistCrossRef::class)
    )
    val playlists : MutableList<Playlist>
)
