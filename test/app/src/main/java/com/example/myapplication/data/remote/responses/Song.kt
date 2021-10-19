package com.example.myapplication.data.remote.responses

import com.example.myapplication.utils.Contains.TYPE_ONLINE


//data class Song(
//    var album: Album? = null,
//    var artist: ArtistX? = null,
//    var artists: List<ArtistXX>? = null,
//    val artists_names: String,
//    val code: String? = null,
//    val content_owner: Int? = null,
//    val duration: Int,
//    val id: String,
//    val isWorldWide: Boolean? = null,
//    val isoffical: Boolean? = null,
//    val link: String? = null,
//    val lyric: String? = null,
//    val mv_link: String? = null,
//    val name: String? = null,
//    val order: Any? = null,
//    val performer: String? = null,
//    val playlist_id: String? = null,
//    val position: Int? = null,
//    val rank_num: Any? = null,
//    val rank_status: String? =null,
//    val thumbnail: String,
//    val title: String,
//    val total: Int? = null,
//    val type: String? = null,
//    var favorit: Boolean = false
//)

data class Song(
    val artists_names: String,
    val duration: Int,
    val id: String,
    var thumbnail: String?=null,
    val title: String,
    var favorit: Boolean = false,
    var image: ByteArray = byteArrayOf()
)
//var song = Song(artists_names = "name",duration = 100,id = "23",thumbnail = "link")