package com.example.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.data.remote.responses.TopSongResponse
import com.example.myapplication.service.MusicService
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private var response: TopSongResponse? = null
    private var listSong = listOf<Song>()
    private var adapter = SongAdapter()
    private var musicService: MusicService? = null
    private var isBound = false
    lateinit var tvContent: TextView
    lateinit var btnPause: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        val songApi = SongApi.create()
        initRv()
        bindService()
        getTopSong(songApi)
    }

    private fun initViews() {
        tvContent = findViewById(R.id.tv_infor)
        btnPause = findViewById(R.id.btn_pause)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) unbindService(connection)
    }

    private fun bindService() {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as MusicService.Mybind
            musicService = binder.getInstance()
            isBound = true
            if (!listSong.isEmpty()) {
                musicService?.setPlaylist(listSong)
                if (musicService?.isPlaying() ?: false == false) {
                    musicService?.setNewSong(listSong[0].id)
                }
                changeContent()
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }

    private fun initRv() {
        val rv = findViewById<RecyclerView>(R.id.rv_top_song)
        rv.adapter = adapter
        rv.layoutManager =
            LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        adapter.setItemClick { id ->
            musicService?.setNewSong(id)
            musicService?.playSong()
            changePausePlayBtn()
        }
    }

    private fun getTopSong(songApi: SongApi) {
        val fetch = findViewById<TextView>(R.id.fetch)
        fetch.visibility = View.VISIBLE
        fetch.text = "fetching"
        CoroutineScope(Dispatchers.IO).launch {
           try {
               withTimeout(3000) {
                   response = songApi.getTopSong()
                   withContext(Dispatchers.Main) {
                       response?.let {
                           listSong = it.data.song
                           adapter.setData(listSong)
                           fetch.visibility = View.GONE
                           if (!listSong.isEmpty()) {
                               musicService?.setPlaylist(listSong)
                               if (musicService?.isPlaying() ?: false == false) {
                                   musicService?.setNewSong(listSong[0].id)
                               }
                               changeContent()
                           }
                       }
                   }
               }
           } catch (e:TimeoutCancellationException){
               withContext(Dispatchers.Main) {
                   fetch.text = "timeout - retry"
                   fetch.setOnClickListener {
                       getTopSong(songApi)
                   }
               }
           }
        }
    }


private fun changePausePlayBtn() {
    if (musicService?.isPlaying() ?: false == true) {
        btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
    } else btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
}

private fun changeContent() {
    tvContent.text = musicService?.cursong?.title
//        val byteArray = musicService.cursong.byteArray
//        if (byteArray.isEmpty()) {
//            ivSong.setImageResource(R.drawable.ic_baseline_music_note_24)
//        } else {
//            ivSong.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
//        }
}
}