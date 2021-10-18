package com.example.myapplication.activities

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapter.SongAdapter
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.data.remote.responses.TopSongResponse
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CANCEL
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.ACTION_PAUSE
import com.example.myapplication.utils.Contains.ACTION_PLAY
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private var response: TopSongResponse? = null
    private var listSong = listOf<Song>()
    private var adapter = SongAdapter()
    private var musicService: MusicService? = null
    private var isBound = false
    lateinit var tvContent: TextView
    lateinit var btnPause: ImageView
    val broadcast = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == "fromNotifyToActivity") {
                    val value = it.getIntExtra("fromNotifyToActivity", -1)
                    when (value) {
                        ACTION_CHANGE_SONG -> {
                            changeContent()
                            changeTogglePausePlayUi(ACTION_PLAY)
                        }
                        ACTION_PAUSE -> changeTogglePausePlayUi(value)
                        ACTION_PLAY -> changeTogglePausePlayUi(value)
                        ACTION_CANCEL -> changeTogglePausePlayUi(ACTION_PAUSE)
                    }
                }
            }
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter("fromNotifyToActivity")
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcast,
            filter
        )
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcast)
    }

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
        initControlBottomBar()
    }

    override fun onStart() {
        super.onStart()
        registerReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) unbindService(connection)
        unregisterReceiver()
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
//            if (!listSong.isEmpty() && isBound) {
//                musicService?.setPlaylist(listSong)
//                changeContent()
//            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }

    private fun changePausePlayBtn() {
        musicService?.let {
            if (it.isPlaying()) {
                btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
            } else btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }
    }

    private fun initRv() {
        val rv = findViewById<RecyclerView>(R.id.rv_top_song)
        rv.adapter = adapter
        rv.layoutManager =
            LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        adapter.setItemClick { song ->
            val id = song.id
           // Toast.makeText(this,song.favorit.toString(),Toast.LENGTH_LONG).show()
            musicService?.setNewSong(id)
            musicService?.playSong()
            val intentService = Intent(this, MusicService::class.java)
            startService(intentService)
            changeContent()
            changeTogglePausePlayUi(ACTION_PLAY)
            val intent = Intent(this, PlayingActivity::class.java)
            startActivity(intent)
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
                                musicService?.let {
                                    it.setPlaylist(listSong)
                                    if(it.cursong==null){
                                        it.setNewSong(listSong[0].id)
                                    }

                                }



                                changeContent()
                                changePausePlayBtn()
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    fetch.text = "timeout - retry"
                    fetch.setOnClickListener {
                        getTopSong(songApi)
                    }
                }
            }
        }
    }

    private fun changeTogglePausePlayUi(value: Int) {
        if (value == ACTION_PAUSE) {
            btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        } else {
            btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        }
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

    private fun initControlBottomBar() {
        btnPause = findViewById<ImageView>(R.id.btn_pause)
        val layout = findViewById<RelativeLayout>(R.id.ll_layout)

        btnPause.setOnClickListener {
            val intent = Intent(this, MusicService::class.java)
            startService(intent)
            musicService?.togglePlayPause()
        }

        layout.setOnClickListener {
            if (isBound && listSong.isNotEmpty()) {
                val intent = Intent(this, PlayingActivity::class.java)
                startActivity(intent)
            }
        }
    }
}