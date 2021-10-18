package com.example.myapplication.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import com.example.myapplication.data.remote.responses.Song

class MusicService : Service() {
    private var mediaPlayer = MediaPlayer()

    private var playlist = listOf<Song>()
    private val listSongPos = listOf<Int>()
    lateinit var cursong: Song
    private var songPos = 0
    var shuffle = false
    var repeat = false

    inner class Mybind() : Binder() {
        fun getInstance(): MusicService {
            return this@MusicService
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        return Mybind()
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    fun isPlaying() = mediaPlayer.isPlaying
    fun setPlaylist(list: List<Song>) {
        playlist = list
    }

    fun setNewSong(newId: String) {
        cursong = playlist.find {
            it.id == newId
        } ?: cursong
        mediaPlayer.reset()
        val uri = Uri.parse("http://api.mp3.zing.vn/api/streaming/audio/${cursong.id}/128")
        mediaPlayer = MediaPlayer.create(applicationContext, uri)
        mediaPlayer.setOnCompletionListener {
            nextSong()
        }
    }
    fun playSong(){
        mediaPlayer.start()
    }

    private fun nextSong() {

    }
}