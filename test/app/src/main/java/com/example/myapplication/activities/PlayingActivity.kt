package com.example.myapplication.activities

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf

import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.data.local.SongDatabase
import com.example.myapplication.data.local.models.SongFavourite
import com.example.myapplication.data.remote.SongApi
import com.example.myapplication.data.remote.info.Infor
import com.example.myapplication.data.remote.recommend.RecommendResponses
import com.example.myapplication.data.remote.responses.Song
import com.example.myapplication.databinding.ActivityPlayingBinding
import com.example.myapplication.fragmment.MyPlaylistFragment
import com.example.myapplication.fragmment.RecommendFragment
import com.example.myapplication.service.MusicService
import com.example.myapplication.utils.Contains
import com.example.myapplication.utils.Contains.ACTION_CANCEL
import com.example.myapplication.utils.Contains.ACTION_CHANGE_SONG
import com.example.myapplication.utils.Contains.ACTION_PAUSE
import com.example.myapplication.utils.Contains.ACTION_PLAY
import com.example.myapplication.utils.FragmentAction
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class PlayingActivity : AppCompatActivity(), FragmentAction {
    private var musicService: MusicService? = null
    private var isBound = false
    private var fromUser = false
    private lateinit var binding: ActivityPlayingBinding
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
        binding = ActivityPlayingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindService()
        initViews()
    }

    private fun getInfoSong() {
        binding.tvInfor.visibility = View.INVISIBLE
        val inforApi = SongApi.create()
        val infor = inforApi.getInfo("audio", musicService!!.cursong!!.id).enqueue(
            object : Callback<Infor> {
                override fun onResponse(call: Call<Infor>, response: Response<Infor>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        body?.let {
                            val listGenre = body.data.genres
                            if (listGenre.isNotEmpty()) {
                                var text = ""
                                listGenre.forEach {
                                    text = text + " " + it.name
                                }
                                binding.tvInfor.text = "<$text>"
                                binding.tvInfor.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<Infor>, t: Throwable) {
                    binding.tvInfor.text = " UNKNOWN"
                }
            }
        )
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
        binding.tvTitle.isSelected = true
        binding.tvSinger.isSelected = true
        binding.tvProgressChange.isVisible = false
        listenSeekBarChange()
        updateUiWhenChangeSong()
        // Button
//        val btnPrev = findViewById<ImageView>(R.id.btn_prev)
//        val btnNext = findViewById<ImageView>(R.id.btn_next)
//        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        binding.btnBack.setOnClickListener {
            super.onBackPressed()
        }
        binding.btnPause.setOnClickListener {
            musicService?.let {
                it.togglePlayPause()
                val intent = Intent(this, MusicService::class.java)
                startService(intent)
            }
        }
        binding.btnNext.setOnClickListener {
            musicService?.nextSong()
        }
        binding.btnPrev.setOnClickListener {
            musicService?.prevSong()
        }
        binding.btnRepeat.setOnClickListener {
            musicService?.setRepeat()
            changeRepeatState()
        }
        binding.btnShuffle.setOnClickListener {
            musicService?.setShuffle()
            changeShuffleState()
        }
        binding.ivFavourite.setOnClickListener {
            musicService?.cursong?.let {
                addFavorite(it)
            }
        }
        binding.addFragment.setOnClickListener {
            val bundle = bundleOf(
                "recommend" to true
            )
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_container, RecommendFragment::class.java, bundle)
            transaction.addToBackStack(null)
            transaction.commit()
            supportFragmentManager.executePendingTransactions()
            setRecommendSong()
        }
        binding.addToPlaylist.setOnClickListener {
            musicService?.cursong?.let {
                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(
                    R.id.fragment_container,
                    MyPlaylistFragment::class.java,
                    bundleOf("song" to it)
                )
                transaction.addToBackStack(null)
                transaction.commit()
                supportFragmentManager.executePendingTransactions()
            }
        }
    }


    private fun changeRepeatState() {
        if (musicService!!.repeat) binding.btnRepeat.setImageResource(R.drawable.ic_repeat_on)
        else binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
    }

    private fun changeShuffleState() {
        if (musicService!!.shuffle) binding.btnShuffle.setImageResource(R.drawable.ic_shuffle_on)
        else binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
    }

    private fun changeTogglePausePlayUi(value: Int) {
        if (value == ACTION_PAUSE) binding.btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        else binding.btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        musicService?.let {
            if (it.internetConnected == false && it.namePlaylist != "OFFLINE") {
                showSnack("No Internet ")
            }
        }
    }

    private fun updateSeekBar(value: Int, fromUser: Boolean) {
        if (!fromUser) {
            binding.tvCurrentDuration .text = Contains.durationString(value)
            binding.progressHorizontal.progress = value
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
                        binding.ivFavourite.setImageResource(R.drawable.ic_baseline_heart_broken_24)
                    }
                } else {
                    db.getDao().insert(songFavourite)
                    withContext(Dispatchers.Main) {
                        showSnack("Add Favourite List")
                        binding.ivFavourite.setImageResource(R.drawable.ic_heart_checked)
                    }
                }
            }
        }
    }

    private fun listenSeekBarChange() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        binding.progressHorizontal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var newPos = 0
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (fromUser) newPos = p1
                binding.tvProgressChange.text = Contains.durationString(newPos)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                fromUser = true
                binding.tvProgressChange.isVisible = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                musicService?.seekTo(newPos)
                fromUser = false
                binding.tvProgressChange.isVisible = false
            }
        })
    }

    private fun updateUiWhenChangeSong() {
        musicService?.let { service ->
            curSong = service.cursong
            curSong?.let { curSong ->
                //content change
                binding.tvTitle.text = curSong.title
                binding.tvSinger.text = curSong.artists_names
                binding.tvDuration.text = Contains.durationString(curSong.duration)
                binding.tvCurrentDuration .text =
                    Contains.durationString(service.getMediaCurrentPos() / 1000)
                binding.progressHorizontal.max = (curSong.duration)

                //image change
                if (curSong.thumbnail != null) { //online
                    val imgUrl = curSong.thumbnail
                    Glide.with(applicationContext).load(imgUrl).circleCrop().into(binding.ivContent)
                } else if (curSong.image.isNotEmpty()) { // offline
                    binding.ivContent.setImageBitmap(
                        BitmapFactory.decodeByteArray(curSong.image, 0, curSong.image.size)
                    )
                } else binding.ivContent.setImageResource(R.drawable.ic_baseline_music_note_24)

                // ofline mode - hide view
                //get category when song isn't LocalSong
                if (curSong.isOffline == false) {
                    getInfoSong()
                }
                if (curSong.isOffline) binding.ivFavourite.visibility = View.GONE
                else binding.ivFavourite.visibility = View.VISIBLE

                //favourite change
                //check existed database
                val id = curSong.id
                val db = SongDatabase.getInstance(applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    val isExists = db.getDao().isExist(id)
                    withContext(Dispatchers.Main) {
                        if (isExists) binding.ivFavourite.setImageResource(R.drawable.ic_heart_checked)
                        else binding.ivFavourite.setImageResource(R.drawable.ic_baseline_heart_broken_24)
                    }
                }
            }
        }
    }

    private fun setRecommendSong() {
        val isOffline = musicService?.cursong?.isOffline ?: true
        if (!isOffline) {
            loadRecommendSong()
        } else {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container) as? RecommendFragment
            fragment?.receiverStateLoad(3, null, "Offline Song, hasn't recommend Song")

            // showSnack("offline song")
        }
    }

    private fun loadRecommendSong() {
        val songId = musicService?.cursong?.id
        val fragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as? RecommendFragment
        val recommendResponses = SongApi.create().getRecommend("audio", songId!!)
        fragment?.receiverStateLoad(1, null, "fetching")
        recommendResponses.enqueue(object : Callback<RecommendResponses> {
            override fun onResponse(
                call: Call<RecommendResponses>,
                response: Response<RecommendResponses>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        val newlist = it.data.items.toMutableList()
                        newlist.let {
                            fragment?.receiverStateLoad(2, newlist, "suucess")
                        }
                    }
                }
            }

            override fun onFailure(call: Call<RecommendResponses>, t: Throwable) {
                showSnack("Error call API")
            }
        })
    }

    override fun setRecommendSong(song: Song) {

    }

    override fun clickDownload(song: Song) {
        downloadSong(song)
    }

    override fun setNewPlaylistOnFragment(newlistSong: MutableList<Song>, name: String) {
        musicService?.let {
            it.setPlaylist(newlistSong)
            it.setNewSong(newlistSong[0].id)
            it.playSong()
            it.sendToActivity(ACTION_CHANGE_SONG)
            val intentService = Intent(this, MusicService::class.java)
            startService(intentService)
        }
    }

    override fun setNewSongOnFragment(newSong: Song, newlistSong: MutableList<Song>, name: String) {
        musicService?.let {
            it.setPlaylist(newlistSong)
            it.setNewSong(newSong.id)
            it.playSong()
            it.sendToActivity(ACTION_CHANGE_SONG)
            val intentService = Intent(this, MusicService::class.java)
            startService(intentService)
        }
    }

    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            true
        }
    }

    private fun downloadSong(song: Song) {
        if (isStoragePermissionGranted()) {

            val downloadManager =
                getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse("http://api.mp3.zing.vn/api/streaming/audio/${song.id}/128")
            val fileName = song.title
            val appFile = File("/storage/emulated/0/Download/" + fileName + ".mp3")
            if (appFile.canRead()) {
                showSnack("File Already Exists...")
            } else {
                showSnack("Waiting Download...")
                val request = DownloadManager.Request(uri)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setTitle(fileName)
                request.setDescription("downloading..")
                request.setAllowedOverRoaming(true)
                request.setDestinationInExternalPublicDir(
                    (Environment.DIRECTORY_DOWNLOADS),
                    fileName + ".mp3"
                )
                val downloadId = downloadManager.enqueue(request)
            }
        }
    }

    private fun showSnack(mess: String) {
        Snackbar.make(findViewById(R.id.root_layout), mess, Snackbar.LENGTH_LONG).show()
    }
}
