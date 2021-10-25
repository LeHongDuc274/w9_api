package com.example.myapplication.utils

import com.example.myapplication.data.remote.responses.Song

interface FragmentAction {
    fun setNewSongOnFragment(newSong: Song,newlistSong: MutableList<Song>,name:String = "ONLINE")
    fun setNewPlaylistOnFragment(newlistSong: MutableList<Song>,name: String = "ONLINE")
    fun clickDownload(song: Song)
    fun setRecommendSong(song: Song)
    //fun playlistClickOnFragment()
}