package com.example.myapplication.activities

import android.content.*
import android.graphics.BitmapFactory
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.myapplication.R
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CANCEL
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.ACTION_PAUSE
import com.example.myapplication.utils.Contains.ACTION_PLAY
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlayingActivity : AppCompatActivity() {
    private var musicService: MusicService? = null
    private var isBound = false
    private var fromUser = false

    lateinit var tvSinger: TextView
    lateinit var tvTitle: TextView
    lateinit var tvDuration: TextView
    lateinit var btnRepeat: ImageButton
    lateinit var btnShuffle: ImageButton
    lateinit var tvProgressChange: TextView
    lateinit var progressBar: SeekBar
    lateinit var volumBar: SeekBar
    lateinit var tvCurDuration: TextView
    lateinit var ivContent: ImageView
    lateinit var btnPause: FloatingActionButton
    lateinit var ivVolum: ImageView
    private var curSong: Song? = null
    val broadcast = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.let {
                if (it.action == "updatePosition") {
                    val value = it.getIntExtra("value", 0)
                    updateSeekBar(value, fromUser)
                }
                if (it.action == "fromNotifyToActivity") {
                    val value = it.getIntExtra("fromNotifyToActivity", 0)
                    when (value) {
                        ACTION_CHANGE_SONG -> {
                            Log.e("change", "change")
                            updateUiWhenChangeSong()
                            changeTogglePausePlayUi(ACTION_PLAY)
                        }
                        (ACTION_PAUSE) -> {
                            changeTogglePausePlayUi(value)
                        }
                        (ACTION_PLAY) -> {
                            changeTogglePausePlayUi(value)
                        }
                        ACTION_CANCEL ->changeTogglePausePlayUi(ACTION_PAUSE)

                    }
                }
            }
        }
    }


    private fun registerReceiver() {
        val filter = IntentFilter("fromNotifyToActivity")
        filter.addAction("updatePosition")
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
        setContentView(R.layout.activity_playing)
        bindService()
        initViews()
    }

    override fun onStart() {
        super.onStart()
        registerReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        unregisterReceiver()
    }

    val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as MusicService.Mybind
            musicService = binder.getInstance()
            isBound = true
            musicService?.let {
                changeShuffleState()
                changeRepeatState()
                updateUiWhenChangeSong()
                it.updateDuration()
                updateSeekBar(it.getMediaCurrentPos(), false)
                if (it.isPlaying()) changeTogglePausePlayUi(ACTION_PLAY) else changeTogglePausePlayUi(
                    ACTION_PAUSE
                )
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }

    private fun bindService() {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun initViews() {
        tvTitle = findViewById<TextView>(R.id.tv_title)
        tvSinger = findViewById<TextView>(R.id.tv_singer)
        tvCurDuration = findViewById<TextView>(R.id.tv_current_duration)
        tvDuration = findViewById<TextView>(R.id.tv_duration)
        progressBar = findViewById<SeekBar>(R.id.progress_horizontal)
        volumBar = findViewById(R.id.volum)
        btnRepeat = findViewById(R.id.btn_repeat)
        btnShuffle = findViewById(R.id.btn_shuffle)
        tvProgressChange = findViewById(R.id.tv_progress_change)
        tvProgressChange.isVisible = false
        ivContent = findViewById(R.id.iv_content)
        ivVolum = findViewById(R.id.iv_volum)
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        volumBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            ivVolum.setImageResource(R.drawable.ic_baseline_volume_off_24)
        } else ivVolum.setImageResource(R.drawable.ic_baseline_volume_up_24)
        volumBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        listenSeekBarChange()
        updateUiWhenChangeSong()
        // Button
        btnPause = findViewById<FloatingActionButton>(R.id.btn_pause)

        val btnPrev = findViewById<ImageView>(R.id.btn_prev)
        val btnNext = findViewById<ImageView>(R.id.btn_next)
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        btnBack.setOnClickListener {
            super.onBackPressed()
        }
        btnPause.setOnClickListener {
            musicService?.let {
                it.togglePlayPause()
                val intent = Intent(this, MusicService::class.java)
                startService(intent)
            }
        }
        btnNext.setOnClickListener {
            musicService?.nextSong()
        }
        btnPrev.setOnClickListener {
            musicService?.prevSong()
        }
        btnRepeat.setOnClickListener {
            musicService?.setRepeat()
            changeRepeatState()
        }
        btnShuffle.setOnClickListener {
            musicService?.setShuffle()
            changeShuffleState()
        }
    }

    private fun changeRepeatState() {
        if (musicService!!.repeat) {
            btnRepeat.setImageResource(R.drawable.ic_repeat_on)
        } else btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
    }

    private fun changeShuffleState() {
        if (musicService!!.shuffle) {
            btnShuffle.setImageResource(R.drawable.ic_shuffle_on)
        } else btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
    }


    private fun changeTogglePausePlayUi(value: Int) {
        if (value == ACTION_PAUSE) {
            btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        } else {
            btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        }
    }

    private fun updateSeekBar(value: Int, fromUser: Boolean) {
        if (!fromUser) {
            tvCurDuration.text = Contains.durationString(value)
            progressBar.progress = value
        }
    }

    private fun listenSeekBarChange() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var newPos = 0
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (fromUser) newPos = p1
                tvProgressChange.text = Contains.durationString(newPos)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                fromUser = true
                tvProgressChange.isVisible = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                musicService?.seekTo(newPos)
                fromUser = false
                tvProgressChange.isVisible = false
            }

        })
    }

    private fun updateUiWhenChangeSong() {
        musicService?.let { service ->
            curSong = service.cursong
            curSong?.let { curSong ->
                Log.e("cur", curSong.toString())
                tvTitle.text = curSong.title
                tvSinger.text = curSong.artists_names
                tvDuration.text = Contains.durationString(curSong.duration)
                tvCurDuration.text =
                    Contains.durationString(service.getMediaCurrentPos() / 1000)
                progressBar.max = (curSong.duration)
            }
        }
//        val byteArray = musicService.cursong.byteArray
//        if (byteArray.isEmpty()) {
//            ivContent.setImageResource(R.drawable.ic_baseline_music_note_24)
//        } else {
//            ivContent.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
//        }
        // togglePausePlay
    }
}