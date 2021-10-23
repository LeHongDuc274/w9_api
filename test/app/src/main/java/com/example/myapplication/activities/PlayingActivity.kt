package com.example.myapplication.activities

import android.content.*
import android.graphics.BitmapFactory
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.*

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.local.SongDatabase
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.fragmment.MyPlaylistFragment
import com.example.myapplication.fragmment.RecommendFragment
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CANCEL
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.ACTION_PAUSE
import com.example.myapplication.utils.Contains.ACTION_PLAY
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*

class PlayingActivity : AppCompatActivity() {
    private var musicService: MusicService? = null
    private var isBound = false
    private var fromUser = false

    lateinit var tvSinger: TextView
    lateinit var tvTitle: TextView
    lateinit var tvDuration: TextView
    lateinit var btnRepeat: ImageView
    lateinit var btnShuffle: ImageView
    lateinit var tvProgressChange: TextView
    lateinit var progressBar: SeekBar
    lateinit var tvCurDuration: TextView
    lateinit var ivContent: ImageView
    lateinit var btnPause: FloatingActionButton
    lateinit var ivAddPlaylist: ImageView
    lateinit var ivAddFragment: ImageView
    lateinit var tvRecommend: TextView
    lateinit var ivFavourite: ImageView

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
                            updateUiWhenChangeSong()
                            changeTogglePausePlayUi(ACTION_PLAY)
                        }
                        (ACTION_PAUSE) -> {
                            changeTogglePausePlayUi(value)
                        }
                        (ACTION_PLAY) -> {
                            changeTogglePausePlayUi(value)
                        }
                        ACTION_CANCEL -> changeTogglePausePlayUi(ACTION_PAUSE)
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
        tvTitle.isSelected = true
        tvSinger = findViewById<TextView>(R.id.tv_singer)
        tvSinger.isSelected = true
        tvCurDuration = findViewById<TextView>(R.id.tv_current_duration)
        tvDuration = findViewById<TextView>(R.id.tv_duration)
        progressBar = findViewById<SeekBar>(R.id.progress_horizontal)
        btnRepeat = findViewById(R.id.btn_repeat)
        btnShuffle = findViewById(R.id.btn_shuffle)
        tvProgressChange = findViewById(R.id.tv_progress_change)
        tvProgressChange.isVisible = false
        ivContent = findViewById(R.id.iv_content)
        ivAddPlaylist = findViewById(R.id.add_to_playlist)
        ivAddFragment = findViewById(R.id.add_fragment)
        tvRecommend = findViewById(R.id.tv_recommed)
        ivFavourite = findViewById(R.id.iv_favourite)
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
        ivFavourite.setOnClickListener {
            musicService?.cursong?.let {
                addFavorite(it)
            }
        }
        ivAddFragment.setOnClickListener {
            loadFragment(RecommendFragment(musicService!!, this))
        }
        ivAddPlaylist.setOnClickListener {
            val id = musicService?.cursong?.id
            id?.let {
                loadFragment(MyPlaylistFragment(true,id,musicService= musicService!!))
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        musicService?.let {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_container, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    private fun changeRepeatState() {
        if (musicService!!.repeat) btnRepeat.setImageResource(R.drawable.ic_repeat_on)
        else btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
    }

    private fun changeShuffleState() {
        if (musicService!!.shuffle) btnShuffle.setImageResource(R.drawable.ic_shuffle_on)
        else btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
    }

    private fun changeTogglePausePlayUi(value: Int) {
        if (value == ACTION_PAUSE) btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        else btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        musicService?.let {
            if (it.internetConnected == false && it.namePlaylist != "OFFLINE") {
                showSnack("No Internet ")
            }
        }
    }

    private fun updateSeekBar(value: Int, fromUser: Boolean) {
        if (!fromUser) {
            tvCurDuration.text = Contains.durationString(value)
            progressBar.progress = value
        }
    }

    private fun addFavorite(song: Song) {
        val songFavourite =
            song.thumbnail?.let {
                SongFavourite(
                    song.artists_names,
                    song.duration,
                    song.id,
                    it,
                    song.title
                )
            }
        val db = SongDatabase.getInstance(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            if (songFavourite != null) {
                val id = songFavourite.id
                val isExists = db.getDao().isExist(id)
                if (isExists) {
                    db.getDao().deleteById(id)
                    withContext(Dispatchers.Main) {
                        showSnack("Remove Favourite List")
                        ivFavourite.setImageResource(R.drawable.ic_baseline_heart_broken_24)
                    }
                } else {
                    db.getDao().insert(songFavourite)
                    withContext(Dispatchers.Main) {
                        showSnack("Add Favourite List")
                        ivFavourite.setImageResource(R.drawable.ic_heart_checked)
                    }
                }
            }
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
                //content change
                tvTitle.text = curSong.title
                tvSinger.text = curSong.artists_names
                tvDuration.text = Contains.durationString(curSong.duration)
                tvCurDuration.text =
                    Contains.durationString(service.getMediaCurrentPos() / 1000)
                progressBar.max = (curSong.duration)
                //image change
                if (curSong.thumbnail != null) {
                    val imgUrl = curSong.thumbnail
                    Glide.with(applicationContext).load(imgUrl).circleCrop().into(ivContent)
                } else if (curSong.image.isNotEmpty()) {
                    ivContent.setImageBitmap(
                        BitmapFactory.decodeByteArray(curSong.image, 0, curSong.image.size)
                    )
                } else ivContent.setImageResource(R.drawable.ic_baseline_music_note_24)
                // ofline mode - hide view
                if (curSong.isOffline) ivFavourite.visibility = View.GONE
                else ivFavourite.visibility = View.VISIBLE
                //favourite change
                //check existed database
                val id = curSong.id
                val db = SongDatabase.getInstance(applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    val isExists = db.getDao().isExist(id)
                    withContext(Dispatchers.Main) {
                        if (isExists) ivFavourite.setImageResource(R.drawable.ic_heart_checked)
                        else ivFavourite.setImageResource(R.drawable.ic_baseline_heart_broken_24)
                    }
                }
            }
        }
    }

    private fun showSnack(mess: String) {
        Snackbar.make(findViewById(R.id.root_layout), mess, Snackbar.LENGTH_LONG).show()
    }
}
