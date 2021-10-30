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
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf

import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
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
import com.example.myapplication.viewmodels.PlayingViewModel
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
    lateinit var playingVm: PlayingViewModel
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
                            musicService?.isPlaying()?.let { it1 -> playingVm.setPlaybackState(it1) }
                        }
                        else -> musicService?.isPlaying()?.let { it1 -> playingVm.setPlaybackState(it1) }
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
        registerReceiver()
        playingVm = ViewModelProvider(
            this,
            PlayingViewModel.PlayingViewmodelFactory(this.application)
        )[PlayingViewModel::class.java]
        binding = ActivityPlayingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindService()
        initViews()

    }

    override fun onStart() {
        super.onStart()
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
                observeCurSong()
                observePlaylist()
                it.updateDuration()
                updateSeekBar(it.getMediaCurrentPos(), false)

                it.isPlaying().let {
                    playingVm.setPlaybackState(it)
                }
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
        obverseInfor()
        listenSeekBarChange()
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
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_container, RecommendFragment::class.java, null)
            transaction.addToBackStack(null)
            transaction.commit()
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

    private fun obverseInfor() {
        playingVm.infor.observe(this, {
            binding.tvInfor.text = it
        })
    }


    private fun changeRepeatState() {
        if (musicService!!.repeat) binding.btnRepeat.setImageResource(R.drawable.ic_repeat_on)
        else binding.btnRepeat.setImageResource(R.drawable.ic_baseline_repeat_24)
    }

    private fun changeShuffleState() {
        if (musicService!!.shuffle) binding.btnShuffle.setImageResource(R.drawable.ic_shuffle_on)
        else binding.btnShuffle.setImageResource(R.drawable.ic_baseline_shuffle_24)
    }

//    private fun changeTogglePausePlayUi(value: Int) {
//        if (value == ACTION_PAUSE) binding.btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
//        else binding.btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
//        musicService?.let {
//            if (it.internetConnected == false && it.namePlaylist != "OFFLINE") {
//                showSnack("No Internet ")
//            }
//        }
//    }

    private fun updateSeekBar(value: Int, fromUser: Boolean) {
        if (!fromUser) {
            binding.tvCurrentDuration.text = Contains.durationString(value)
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
        binding.progressHorizontal.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
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

    fun observeCurSong() {
        playingVm.curSong.observe(this, { curSong ->
            musicService?.setNewSong(curSong)
            musicService?.playSong()
            val intent = Intent(this, MusicService::class.java)
            startService(intent)
            binding.tvTitle.text = curSong.title
            binding.tvSinger.text = curSong.artists_names
            binding.tvDuration.text = Contains.durationString(curSong.duration)
            binding.tvCurrentDuration.text =
                Contains.durationString(musicService!!.getMediaCurrentPos() / 1000)
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
            playingVm.getInfor(curSong)
            if (curSong.isOffline) binding.ivFavourite.visibility = View.GONE
            else binding.ivFavourite.visibility = View.VISIBLE
            //favourite change
            //check existed database
            playingVm.checkIsFavou(curSong)
        })
        playingVm.isFavoriteSong.observe(this, {
            if (it) binding.ivFavourite.setImageResource(R.drawable.ic_heart_checked)
            else binding.ivFavourite.setImageResource(R.drawable.ic_baseline_heart_broken_24)
        })
    }

    private fun updateUiWhenChangeSong() {
        musicService?.let { service ->
            curSong = service.cursong
            curSong?.let { curSong ->
                playingVm.setNewSong(curSong)
            }
        }
        playingVm.isPlaying.observe(this,{
            if (!it) binding.btnPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            else binding.btnPause.setImageResource(R.drawable.ic_baseline_pause_24)
        })
    }

    private fun observePlaylist() {
        playingVm.curList.observe(this, {
            if (!it.isNullOrEmpty()) {
                musicService?.setPlaylist(it, playingVm.namePlaylist.value!!)
            }
        })
    }

    private fun setRecommendSong() {
        val isOffline = playingVm.curSong.value?.isOffline ?: true
    }


    override fun setRecommendSong(song: Song) {

    }

    override fun clickDownload(song: Song) {
        downloadSong(song)
    }

    override fun setNewPlaylistOnFragment(newlistSong: MutableList<Song>, name: String) {
        musicService?.let {
            it.setPlaylist(newlistSong)
            it.setNewSong(newlistSong[0])
            it.playSong()
            it.sendToActivity(ACTION_CHANGE_SONG)
            val intentService = Intent(this, MusicService::class.java)
            startService(intentService)
        }
    }

    override fun setNewSongOnFragment(newSong: Song, newlistSong: MutableList<Song>, name: String) {
        musicService?.let {
            it.setPlaylist(newlistSong)
            it.setNewSong(newSong)
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
